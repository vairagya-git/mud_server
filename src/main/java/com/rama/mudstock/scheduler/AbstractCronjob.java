package com.rama.mudstock.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rama.mudstock.config.ApplicationConfig;
import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.TypeConverstionUtil;
import com.rama.mudstock.util.WatchlistUtil;

public abstract class AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(AbstractCronjob.class);
    private static final boolean TEMP_LOG = true;

    private final SystemConfigService systemConfigService;
    private final MarketCalendarService marketCalendarService;
    private String currentPurpose;

    protected AbstractCronjob(SystemConfigService systemConfigService, String purpose) {
        this(systemConfigService, purpose, null);
    }

    protected AbstractCronjob(SystemConfigService systemConfigService,
                              String purpose,
                              MarketCalendarService marketCalendarService) {
        this.systemConfigService = systemConfigService;
        this.marketCalendarService = marketCalendarService;
        loadPurposeConfig(purpose);
    }

    /**
     * Sets the active purpose context for this cronjob instance.
     */
    protected void loadPurposeConfig(String purpose) {
        this.currentPurpose = purpose;
    }

    protected String getPurpose() {
        return currentPurpose;
    }

    protected Object getConfigValue(String code) {
        if (currentPurpose == null || currentPurpose.isBlank() || code == null || code.isBlank()) {
            return null;
        }
        return systemConfigService.findByPurposeAndCode(currentPurpose, code).orElse(null);
    }

    protected List<String> resolveConfiguredWatchlistCodes(String purpose, String code) {
        return systemConfigService
            .findByPurposeAndCode(purpose, code)
            .filter(List.class::isInstance)
            .map(v -> ((List<?>) v).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList())
            .orElse(List.of());
    }

    protected List<com.rama.mudstock.model.stockwatchlist.Stock> collectUniqueStocksByTicker(String purpose,
                                                                                            String watchlistCodes,
                                                                                            WatchlistRepository watchlistRepository) {
        return WatchlistUtil.collectUniqueStocksByTicker(watchlistCodes, watchlistRepository, log, purpose);
    }

    protected boolean isEnabled(String purpose) {
        return Boolean.TRUE.equals(TypeConverstionUtil.toBoolean(getConfigValue(CronjobConfigEnum.ENABLED.code())));
    }

    /**
     * Resolves configured execution type (daily/hourly/minutes) from system_config.
     */
    private CronjobConfigEnum.Execution resolveExecution(String purpose) {
        String executionCode = CronjobConfigEnum.EXECUTION.code();
        String execution = TypeConverstionUtil.toString(getConfigValue(executionCode)).toLowerCase();
        if (execution.isBlank()) {
            log.warn("AbstractCronjob: missing execution mode in system_config (purpose={}, code={})", purpose, executionCode);
        }
        CronjobConfigEnum.Execution executionMode = CronjobConfigEnum.Execution.fromValue(execution);
        if (executionMode == null) {
            log.warn("AbstractCronjob: unsupported execution mode '{}' (purpose={}, code={})",
                execution,
                purpose,
                executionCode);
            return null;
        }
        return executionMode;
    }

    /**
     * Evaluates [startTime, endTime] window in Lisbon timezone.
     * Supports windows that cross midnight.
     */
    protected boolean isWithinStartEndWindow(String purpose) {
        String startRaw = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.START_TIME.code()));
        String endRaw = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.END_TIME.code()));

        if (startRaw.isBlank() || endRaw.isBlank()) {
            log.warn("{}: missing execution window config (purpose={}, startCode={}, endCode={})",
                purpose,
                purpose,
                CronjobConfigEnum.START_TIME.code(),
                CronjobConfigEnum.END_TIME.code());
            return false;
        }

        String rawTimeFormat = ApplicationConfig.TIME_FORMAT_HH_MM;
        String format = (rawTimeFormat == null || rawTimeFormat.isBlank())
            ? ApplicationConfig.TIME_FORMAT_HH_MM
            : rawTimeFormat.trim();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalTime startTime = LocalTime.parse(startRaw, formatter);
            LocalTime endTime = LocalTime.parse(endRaw, formatter);
            LocalTime now = LocalTime.now(ApplicationConfig.LISBON);

            boolean withinWindow;
            String comparisonMode;
            if (endTime.isBefore(startTime)) {
                // Supports windows that cross midnight, e.g. 23:00 to 05:00.
                withinWindow = !now.isBefore(startTime) || !now.isAfter(endTime);
                comparisonMode = "CROSS_MIDNIGHT";
            } else {
                withinWindow = !now.isBefore(startTime) && !now.isAfter(endTime);
                comparisonMode = "SAME_DAY";
            }

            if (TEMP_LOG) {
                log.info(
                    "TEMP_LOG {} START_END_TIME check: now_lisbon={} start={} end={} comparison_mode={} within_window={} now_utc={}",
                    purpose,
                    now,
                    startTime,
                    endTime,
                    comparisonMode,
                    withinWindow,
                    Instant.now());
            }

            if (!withinWindow) {
                log.info("{}: outside execution window now={} start={} end={}", purpose, now, startTime, endTime);
            }
            return withinWindow;
        } catch (Exception ex) {
            log.warn("{}: invalid execution window config startTime='{}' endTime='{}' format='{}'",
                purpose,
                startRaw,
                endRaw,
                format,
                ex);
            return false;
        }
    }

    private boolean hasItBeenExecutedToday(String purpose) {
        LocalDate today = LocalDate.now(ApplicationConfig.LISBON);
        LocalDate lastUpdatedDate = resolveDateFromConfig(purpose, CronjobConfigEnum.LAST_UPDATED.code());

        if (lastUpdatedDate != null) {
            if (lastUpdatedDate.isEqual(today)) {
                log.info("{}: already executed today {}; skipping", purpose, today);
                return true;
            }

            if (lastUpdatedDate.isAfter(today)) {
                log.info("{}: lastUpdated {} is after today {}; skipping", purpose, lastUpdatedDate, today);
                return true;
            }
        }

        return false;
    }

    private boolean isOnOrAfterDailyCutOff(String purpose) {
        String cutOffTimeCode = CronjobConfigEnum.DAILY_CUTT_OFF_TIME.code();
        String cutOffTimeFormat = CronjobConfigEnum.DAILY_CUTT_OFF_TIME.format();

        String rawCutOffTime = TypeConverstionUtil.toString(getConfigValue(cutOffTimeCode));
        if (rawCutOffTime.isBlank()) {
            log.warn("{}: missing cutoff time config (purpose={}, code={})", purpose, purpose, cutOffTimeCode);
            return false;
        }

        String format = (cutOffTimeFormat == null || cutOffTimeFormat.isBlank())
            ? ApplicationConfig.TIME_FORMAT_HH_MM
            : cutOffTimeFormat.trim();
        try {
            LocalTime cutOffTime = LocalTime.parse(rawCutOffTime, DateTimeFormatter.ofPattern(format));
            LocalTime now = LocalTime.now(ApplicationConfig.LISBON);
            boolean allowed = !now.isBefore(cutOffTime);
            if (!allowed) {
                log.info("{}: before cutoff now={} cutoff={}", purpose, now, cutOffTime);
            }
            return allowed;
        } catch (Exception ex) {
            log.warn("{}: invalid cutoff time value='{}' format='{}'", purpose, rawCutOffTime, format, ex);
            return false;
        }
    }

    private boolean hasMinuteHourlyIntervalElapsed(String purpose,
                                                   CronjobConfigEnum.Execution execution) {
        Integer frequency = TypeConverstionUtil.toInteger(
            getConfigValue(CronjobConfigEnum.MINUTE_HOURLY_FREQUENCY.code()));
        if (frequency == null || frequency <= 0) {
            log.warn("{}: invalid or missing {} for execution={}",
                purpose,
                CronjobConfigEnum.MINUTE_HOURLY_FREQUENCY.code(),
                execution);
            return false;
        }

        String rawLastUpdated = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.LAST_UPDATED.code()));
        if (rawLastUpdated.isBlank()) {
            return true;
        }

        Instant lastUpdated;
        try {
            lastUpdated = Instant.parse(rawLastUpdated);
        } catch (Exception ex) {
            log.warn("{}: invalid lastUpdated value='{}'; allowing execution", purpose, rawLastUpdated, ex);
            return true;
        }

        long intervalSeconds = execution == CronjobConfigEnum.Execution.MINUTES
            ? frequency.longValue() * 60L
            : frequency.longValue() * 3600L;
        Instant nextEligibleAt = lastUpdated.plusSeconds(intervalSeconds);
        boolean intervalElapsed = !Instant.now().isBefore(nextEligibleAt);

        if (!intervalElapsed) {
            log.info("{}: interval not elapsed yet (execution={}, frequency={}, lastUpdated={}, nextEligibleAt={})",
                purpose,
                execution,
                frequency,
                lastUpdated,
                nextEligibleAt);
        }
        return intervalElapsed;
    }

    protected void updateLastUpdatedNowUtc(String purpose) {
        String nowUtc = Instant.now().toString();
        String lastUpdatedCode = CronjobConfigEnum.LAST_UPDATED.code();
        boolean updated = systemConfigService.updateValue(purpose, lastUpdatedCode, nowUtc);
        if (!updated) {
            log.warn("AbstractCronjob: failed to update lastUpdated config (purpose={}, code={})", purpose, lastUpdatedCode);
            return;
        }
    }

    protected LocalDate resolveNextEligibleDate(LocalDate fromDate) {
        LocalDate baseDate = fromDate == null ? LocalDate.now(ApplicationConfig.LISBON) : fromDate;
        LocalDate candidate = baseDate.plusDays(1);
        if (marketCalendarService == null) {
            log.warn("{}: MarketCalendarService not configured in AbstractCronjob; using next calendar day {}",
                getPurpose(),
                candidate);
            return candidate;
        }
        while (marketCalendarService.isMarketClosed(candidate)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    protected void updateDailyDateToNextEligible(String purpose,
                                                 LocalDate currentTargetDate) {
        String dailyDateCode = CronjobConfigEnum.DAILY_DATE.code();
        LocalDate nextEligibleDate = resolveNextEligibleDate(currentTargetDate);
        String nextEligibleDateText = nextEligibleDate.toString();

        boolean updated = systemConfigService.updateValue(purpose, dailyDateCode, nextEligibleDateText);
        if (!updated) {
            log.warn("AbstractCronjob: failed to update dailyDate config (purpose={}, code={}, value={})",
                purpose,
                dailyDateCode,
                nextEligibleDateText);
            return;
        }

        log.info("{}: updated {} to next eligible market date {}", purpose, dailyDateCode, nextEligibleDateText);
    }

    protected boolean isForceExecuteEnabled(String purpose) {
        return Boolean.TRUE.equals(TypeConverstionUtil.toBoolean(getConfigValue(CronjobConfigEnum.FORCE_EXECUTE.code())));
    }

    private LocalDate resolveDateFromConfig(String purpose, String configCode) {
        String rawDate = TypeConverstionUtil.toString(getConfigValue(configCode));
        if (rawDate == null || rawDate.isBlank()) {
            log.warn("{}: {} is not configured (empty/missing); date not available", purpose, configCode);
            return null;
        }

        String value = rawDate.trim();
        try {
            return Instant.parse(value).atZone(ApplicationConfig.LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ApplicationConfig.LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(ApplicationConfig.LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
        } catch (Exception ignored) {
            log.warn("{}: unable to parse {}='{}'; date not available", purpose, configCode, rawDate);
            return null;
        }
    }

    /**
     * Resolves execution target date from force execute config.
     * If forceExecute=false, uses dailyDate.
     * If forceExecute=true, uses forceExecuteDailyDate.
     * Returns null if not configured/parseable; caller must handle this and skip execution.
     */
    protected LocalDate resolveTargetDate(String purpose) {
        boolean forceExecuteEnabled = isForceExecuteEnabled(purpose);
        String configCode = forceExecuteEnabled
            ? CronjobConfigEnum.FORCE_EXECUTE_DAILY_DATE.code()
            : CronjobConfigEnum.DAILY_DATE.code();

        LocalDate targetDate = resolveDateFromConfig(purpose, configCode);
        if (TEMP_LOG) {
            log.info("TEMP_LOG {} resolveTargetDate: forceExecute={} configCode={} resolvedTargetDate={}",
                purpose,
                forceExecuteEnabled,
                configCode,
                targetDate);
        }
        if (targetDate == null) {
            log.error("{}: target date is not configured (code={}); cronjob must skip execution", purpose, configCode);
        }
        return targetDate;
    }

    /**
     * Resolves the execution/reference date for daily data-fetch jobs.
     * If forceExecute is enabled, uses the configured target date (forceExecuteDailyDate).
     * Otherwise, uses the current Lisbon date.
     */
    protected LocalDate resolveExecutionDate(String purpose) {
        if (isForceExecuteEnabled(purpose)) {
            return resolveTargetDate(purpose);
        }
        return LocalDate.now(ApplicationConfig.LISBON);
    }

    /**
     * Resolves and validates the target date for execution.
     * Returns null (after logging) if the date is not configured or the market is closed on that date.
     * Callers must check for null and skip execution accordingly.
     */
    protected LocalDate resolveValidTargetDate(String purpose) {
        LocalDate targetDate = resolveTargetDate(purpose);
        if (targetDate == null) {
            log.error("{}: targetDate is required, skipping execution", purpose);
            return null;
        }

        if (marketCalendarService != null && marketCalendarService.isMarketClosed(targetDate)) {
            log.info("{}: market is closed on targetDate={}, skipping execution", purpose, targetDate);
            return null;
        }

        return targetDate;
    }

    private boolean hasSystemConfigProperty(CronjobConfigEnum config) {
        if (currentPurpose == null || currentPurpose.isBlank()) {
            return false;
        }
        return systemConfigService.findByPurposeAndCode(currentPurpose, config.code()).isPresent();
    }

    /**
      * Dynamically derives execution mode from available schedule details in system_config.
      * Priority: dailyCutOffTime property exists -> DAILY_CUT_OFF,
    * startTime property exists -> START_END_TIME, otherwise NONE.
     */
    protected CronjobConfigEnum.ExecutionMode fetchExecutionModeByDetails(String purpose) {
        if (hasSystemConfigProperty(CronjobConfigEnum.DAILY_CUTT_OFF_TIME)) {
            return CronjobConfigEnum.ExecutionMode.DAILY_CUT_OFF;
        }
        if (hasSystemConfigProperty(CronjobConfigEnum.START_TIME)) {
            return CronjobConfigEnum.ExecutionMode.START_END_TIME;
        }
        return CronjobConfigEnum.ExecutionMode.NONE;
    }

    /**
     * Unified scheduler gate.––
     *
     * Decision order:
     * 1) forceExecute=true -> allow immediately.
     * 2) enabled=false -> block.
      * 3) execution=daily -> enforce: current time on/after dailyCutOffTime
      *    and it has not been executed today.
    * 4) execution=hourly/minutes -> START_END_TIME gate (if configured),
    *    then minuteHourlyFrequency interval check against lastUpdated.
     */
    protected boolean shouldExecuteBySchedule(String purpose) {
        if (!purpose.equals(currentPurpose)) {
            log.warn("{}: constructor-loaded purpose '{}' does not match runtime purpose '{}'; skipping execution",
                purpose,
                currentPurpose,
                purpose);
            return false;
        }

        boolean forceExecuteEnabled = isForceExecuteEnabled(purpose);
        if (forceExecuteEnabled) {
            log.info("{}: forceExecute is enabled (code={}); bypassing schedule checks",
                purpose,
                CronjobConfigEnum.FORCE_EXECUTE.code());
            return true;
        }

        if (!isEnabled(purpose)) {
            log.info("{}: disabled by system_config (code={})", purpose, CronjobConfigEnum.ENABLED.code());
            return false;
        }

        CronjobConfigEnum.ExecutionMode executionMode = fetchExecutionModeByDetails(purpose);

        CronjobConfigEnum.Execution execution = resolveExecution(purpose);
        if (execution == null) {
            return false;
        }

        if (execution == CronjobConfigEnum.Execution.DAILY) {
            if (!isOnOrAfterDailyCutOff(purpose)) {
                return false;
            }
            return !hasItBeenExecutedToday(purpose);
        }

        if (execution == CronjobConfigEnum.Execution.HOURLY
            || execution == CronjobConfigEnum.Execution.MINUTES) {
            return switch (executionMode) {
                case START_END_TIME -> isWithinStartEndWindow(purpose)
                    && hasMinuteHourlyIntervalElapsed(purpose, execution);
                case NONE -> hasMinuteHourlyIntervalElapsed(purpose, execution);
                case DAILY_CUT_OFF -> {
                    String message = String.format(
                        "%s: invalid execution mode %s for execution=%s; expected START_END_TIME or NONE",
                        purpose,
                        executionMode,
                        execution);
                    log.error(message);
                    throw new IllegalStateException(message);
                }
            };
        }

        log.warn("{}: unsupported execution '{}' for schedule evaluation", purpose, execution);
        return false;
    }
}