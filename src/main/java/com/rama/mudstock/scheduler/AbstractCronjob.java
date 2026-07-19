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
    protected boolean isWithinExecutionWindow(String purpose) {
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
            if (endTime.isBefore(startTime)) {
                // Supports windows that cross midnight, e.g. 23:00 to 05:00.
                withinWindow = !now.isBefore(startTime) || !now.isAfter(endTime);
            } else {
                withinWindow = !now.isBefore(startTime) && !now.isAfter(endTime);
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

    private boolean shouldExecuteDailyByLastUpdatedAndCutoff(String purpose,
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

        if (lastUpdatedDate != null && lastUpdatedDate.isEqual(today)) {
            log.info("{}: already executed today {}; skipping", purpose, today);
            return false;
        }

        if (lastUpdatedDate != null && lastUpdatedDate.isAfter(today)) {
            log.info("{}: lastUpdated {} is after today {}; skipping", purpose, lastUpdatedDate, today);
            return false;
        }

        return isOnOrAfterCutoff(purpose);
    }

    private boolean isOnOrAfterCutoff(String purpose) {
        String cutOffTimeCode = CronjobConfigEnum.CUTOFF_TIME.code();
        String cutOffTimeFormat = CronjobConfigEnum.CUTOFF_TIME.format();

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
     * Priority: cutOffTime property exists -> CUT_OFF,
     * startTime property exists -> BETWEEN_TIME, otherwise NONE.
     */
    protected CronjobConfigEnum.ExecutionMode fetchExecutionModeByDetails(String purpose) {
        if (hasSystemConfigProperty(purpose, CronjobConfigEnum.CUTOFF_TIME)) {
            return CronjobConfigEnum.ExecutionMode.CUT_OFF;
        }
        if (hasSystemConfigProperty(purpose, CronjobConfigEnum.START_TIME)) {
            return CronjobConfigEnum.ExecutionMode.BETWEEN_TIME;
        }
        return CronjobConfigEnum.ExecutionMode.NONE;
    }

    /**
     * Unified scheduler gate.
     *
     * Decision order:
     * 1) forceExecute=true -> allow immediately.
     * 2) enabled=false -> block.
     * 3) execution=hourly/minutes -> derive extra mode from config details:
     *    - CUT_OFF: current time on/after cutoff
     *    - BETWEEN_TIME: current time inside configured window
     *    - NONE: no extra time gate
     * 4) execution=daily -> enforce: not already executed today AND cutoff passed.
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
            String lastUpdated = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.LAST_UPDATED.code()));
            return shouldExecuteDailyByLastUpdatedAndCutoff(purpose, lastUpdated);
        }

        if (execution == CronjobConfigEnum.Execution.HOURLY
            || execution == CronjobConfigEnum.Execution.MINUTES) {
            return switch (executionMode) {
                case CUT_OFF -> isOnOrAfterCutoff(purpose);
                case BETWEEN_TIME -> isWithinExecutionWindow(purpose);
                case NONE -> true;
            };
        }

        log.warn("{}: unsupported execution '{}' for schedule evaluation", purpose, execution);
        return false;
    }


}