package com.rama.mudstock.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rama.mudstock.config.ApplicationConfig;
import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.TypeConverstionUtil;

public abstract class AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(AbstractCronjob.class);
    private static final boolean TEMP_LOG = true;

    private final SystemConfigService systemConfigService;
    private String currentPurpose;
    private Map<String, Object> currentPurposeConfig = new HashMap<>();

    protected AbstractCronjob(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * Loads all typed system_config values for a purpose into an in-memory map for this run.
     */
    protected void loadPurposeConfig(String purpose) {
        this.currentPurpose = purpose;
        this.currentPurposeConfig = new HashMap<>(systemConfigService.findAllByPurpose(purpose));
    }

    protected Object getConfigValue(String code) {
        return currentPurposeConfig.get(code);
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

    private boolean hasItBeenExecutedToday(String purpose,
                                           String rawLastUpdated) {
        LocalDate today = LocalDate.now(ApplicationConfig.LISBON);
        LocalDate lastUpdatedDate = null;
        if (rawLastUpdated != null && !rawLastUpdated.isBlank()) {
            String value = rawLastUpdated.trim();
            try {
                // Expected DB format: 2026-07-18T10:27:09.432373Z
                lastUpdatedDate = Instant.parse(value).atZone(ApplicationConfig.LISBON).toLocalDate();
            } catch (Exception ignored) {
            }
            if (lastUpdatedDate == null) {
                try {
                    lastUpdatedDate = LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
                } catch (Exception ignored) {
                }
            }
        }

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
        if (purpose.equals(currentPurpose)) {
            currentPurposeConfig.put(lastUpdatedCode, nowUtc);
        }
    }

    protected boolean isForceExecuteEnabled(String purpose) {
        return Boolean.TRUE.equals(TypeConverstionUtil.toBoolean(getConfigValue(CronjobConfigEnum.FORCE_EXECUTE.code())));
    }

    private boolean hasSystemConfigProperty(String purpose, CronjobConfigEnum config) {
        return currentPurposeConfig.containsKey(config.code());
    }

    /**
      * Dynamically derives execution mode from available schedule details in system_config.
      * Priority: dailyCutOffTime property exists -> DAILY_CUT_OFF,
    * startTime property exists -> START_END_TIME, otherwise NONE.
     */
    protected CronjobConfigEnum.ExecutionMode fetchExecutionModeByDetails(String purpose) {
        if (hasSystemConfigProperty(purpose, CronjobConfigEnum.DAILY_CUTT_OFF_TIME)) {
            return CronjobConfigEnum.ExecutionMode.DAILY_CUT_OFF;
        }
        if (hasSystemConfigProperty(purpose, CronjobConfigEnum.START_TIME)) {
            return CronjobConfigEnum.ExecutionMode.START_END_TIME;
        }
        return CronjobConfigEnum.ExecutionMode.NONE;
    }

    /**
     * Unified scheduler gate.
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
        loadPurposeConfig(purpose);

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

            String lastUpdated = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.LAST_UPDATED.code()));
            return !hasItBeenExecutedToday(purpose, lastUpdated);
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
//Changed For Git