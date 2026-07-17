package com.rama.mudstock.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.service.SystemConfigService;

public abstract class AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(AbstractCronjob.class);

    public static final String LISBON_ZONE = "Europe/Lisbon";
    protected static final ZoneId LISBON = ZoneId.of(LISBON_ZONE);

    private final SystemConfigService systemConfigService;

    protected AbstractCronjob(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    protected String enabledCode() {
        return CronjobConfigEnum.ENABLED.code();
    }

    protected String executionCode() {
        return CronjobConfigEnum.EXECUTION.code();
    }

    protected String forceExecuteCode() {
        return CronjobConfigEnum.FORCE_EXECUTE.code();
    }

    protected String minuteHourlyFrequencyCode() {
        return CronjobConfigEnum.MINUTE_HOURLY_FREQUENCY.code();
    }

    protected String lastUpdatedCode() {
        return CronjobConfigEnum.LAST_UPDATED.code();
    }

    protected String cutOffTimeCode() {
        return CronjobConfigEnum.CUTOFF_TIME.code();
    }

    protected String cutOffTimeFormat() {
        return CronjobConfigEnum.CUTOFF_TIME.format();
    }

    protected String startTimeCode() {
        return CronjobConfigEnum.START_TIME.code();
    }

    protected String endTimeCode() {
        return CronjobConfigEnum.END_TIME.code();
    }

    protected String timeFormat() {
        return "HH:mm";
    }

    protected boolean isEnabled(String purpose) {
        return isEnabled(purpose, enabledCode());
    }

    protected String resolveLastUpdated(String purpose) {
        return resolveLastUpdated(purpose, lastUpdatedCode());
    }

    protected LocalDate resolveNextProcessingDate(String purpose, ZoneId zoneId) {
        return resolveNextProcessingDateFromLastUpdated(resolveLastUpdated(purpose), zoneId);
    }

    protected boolean shouldExecuteSinceLastUpdated(String logPrefix,
                                                    LocalDate processingDate,
                                                    String purpose,
                                                    ZoneId zoneId) {
        if (isForceExecuteEnabled(purpose)) {
            log.info("{}: forceExecute is enabled (purpose={}, code={}); bypassing schedule checks",
                logPrefix,
                purpose,
                forceExecuteCode());
            return true;
        }

        String executionCode = executionCode();
        String execution = resolveExecutionMode(purpose, executionCode);
        String lastUpdated = resolveLastUpdated(purpose, lastUpdatedCode());
        LocalDate effectiveProcessingDate = processingDate != null
            ? processingDate
            : resolveNextProcessingDateFromLastUpdated(lastUpdated, zoneId);

        switch (execution) {
            case "hourly":
                return shouldExecuteAtFrequency("hours", purpose, minuteHourlyFrequencyCode(), lastUpdated, zoneId, ChronoUnit.HOURS);
            case "minutes":
                return shouldExecuteAtFrequency("minutes", purpose, minuteHourlyFrequencyCode(), lastUpdated, zoneId, ChronoUnit.MINUTES);
            case "daily":
                return shouldExecuteDailyByLastUpdatedAndCutoff(
                    logPrefix,
                    effectiveProcessingDate,
                    purpose,
                    cutOffTimeCode(),
                    cutOffTimeFormat(),
                    zoneId);
            default:
                log.warn("AbstractCronjob: unsupported execution mode '{}' (purpose={}, code={})",
                    execution,
                    purpose,
                    executionCode);
                return false;
        }
    }

    protected boolean isWithinExecutionWindow(String logPrefix,
                                              String purpose,
                                              ZoneId zoneId) {
        return isWithinExecutionWindow(
            logPrefix,
            purpose,
            startTimeCode(),
            endTimeCode(),
            timeFormat(),
            zoneId);
    }

    protected boolean isEnabled(String purpose, String enabledCode) {
        return systemConfigService
            .findByPurposeAndCode(purpose, enabledCode)
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(Boolean.FALSE);
    }

    protected String resolveExecutionMode(String purpose, String executionCode) {
        String raw = systemConfigService
            .findByPurposeAndCode(purpose, executionCode)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse("");

        String normalized = raw.trim().toLowerCase();
        if (normalized.isBlank()) {
            log.warn("AbstractCronjob: missing execution mode in system_config (purpose={}, code={})", purpose, executionCode);
        }
        return normalized;
    }

    protected String resolveLastUpdated(String purpose, String lastUpdatedCode) {
        return systemConfigService
            .findByPurposeAndCode(purpose, lastUpdatedCode)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .orElse("");
    }

    protected String resolveStringValue(String purpose, String code) {
        return systemConfigService
            .findByPurposeAndCode(purpose, code)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .orElse("");
    }

    protected Integer resolveIntegerValue(String purpose, String code) {
        return systemConfigService
            .findByPurposeAndCode(purpose, code)
            .map(value -> {
                if (value instanceof Integer intValue) {
                    return intValue;
                }
                if (value instanceof Long longValue) {
                    return Math.toIntExact(longValue);
                }
                if (value instanceof Number numberValue) {
                    return numberValue.intValue();
                }
                if (value instanceof String strValue) {
                    try {
                        return Integer.parseInt(strValue.trim());
                    } catch (Exception ignored) {
                        return null;
                    }
                }
                return null;
            })
            .orElse(null);
    }

    protected LocalDate resolveLastUpdatedDate(String rawLastUpdated, ZoneId zoneId) {
        if (rawLastUpdated == null || rawLastUpdated.isBlank()) {
            return null;
        }

        String value = rawLastUpdated.trim();
        try {
            return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(value).atZone(zoneId).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(zoneId).toLocalDate();
        } catch (Exception ignored) {
        }
        return null;
    }

    protected LocalDate resolveNextProcessingDateFromLastUpdated(String rawLastUpdated, ZoneId zoneId) {
        LocalDate lastUpdatedDate = resolveLastUpdatedDate(rawLastUpdated, zoneId);
        if (lastUpdatedDate == null) {
            return LocalDate.now(zoneId);
        }
        return lastUpdatedDate.plusDays(1);
    }

    protected ZonedDateTime resolveLastUpdatedDateTime(String rawLastUpdated, ZoneId zoneId) {
        if (rawLastUpdated == null || rawLastUpdated.isBlank()) {
            return null;
        }

        String value = rawLastUpdated.trim();
        try {
            return Instant.parse(value).atZone(zoneId);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(zoneId);
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(zoneId);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).atZone(zoneId);
        } catch (Exception ignored) {
        }

        LocalDate fallbackDate = resolveLastUpdatedDate(value, zoneId);
        if (fallbackDate != null) {
            return fallbackDate.atStartOfDay(zoneId);
        }
        return null;
    }

    private boolean shouldExecuteDailyByLastUpdatedAndCutoff(String logPrefix,
                                                             LocalDate processingDate,
                                                             String purpose,
                                                             String cutOffTimeCode,
                                                             String cutOffTimeFormat,
                                                             ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);

        if (processingDate.isAfter(today)) {
            log.info("{}: processing date {} is after today {}; skipping", logPrefix, processingDate, today);
            return false;
        }
        if (processingDate.isBefore(today)) {
            return true;
        }

        return isOnOrAfterCutoff(logPrefix, purpose, cutOffTimeCode, cutOffTimeFormat, zoneId);
    }

    private boolean shouldExecuteAtFrequency(String unitName,
                                             String purpose,
                                             String minuteHourlyFrequencyCode,
                                             String rawLastUpdated,
                                             ZoneId zoneId,
                                             ChronoUnit unit) {
        Integer frequency = resolveIntegerValue(purpose, minuteHourlyFrequencyCode);
        if (frequency == null || frequency <= 0) {
            log.warn("AbstractCronjob: invalid {} frequency (purpose={}, code={}, value={})",
                unitName,
                purpose,
                minuteHourlyFrequencyCode,
                frequency);
            return false;
        }

        ZonedDateTime lastUpdatedAt = resolveLastUpdatedDateTime(rawLastUpdated, zoneId);
        if (lastUpdatedAt == null) {
            return true;
        }

        ZonedDateTime nextExecutionAt = lastUpdatedAt.plus(frequency, unit);
        return !nextExecutionAt.isAfter(ZonedDateTime.now(zoneId));
    }

    private boolean isOnOrAfterCutoff(String logPrefix,
                                      String purpose,
                                      String cutOffTimeCode,
                                      String cutOffTimeFormat,
                                      ZoneId zoneId) {
        String rawCutOffTime = resolveStringValue(purpose, cutOffTimeCode);
        if (rawCutOffTime.isBlank()) {
            log.warn("{}: missing cutoff time config (purpose={}, code={})", logPrefix, purpose, cutOffTimeCode);
            return false;
        }

        String format = (cutOffTimeFormat == null || cutOffTimeFormat.isBlank()) ? "HH:mm" : cutOffTimeFormat.trim();
        try {
            LocalTime cutOffTime = LocalTime.parse(rawCutOffTime, DateTimeFormatter.ofPattern(format));
            LocalTime now = LocalTime.now(zoneId);
            boolean allowed = !now.isBefore(cutOffTime);
            if (!allowed) {
                log.info("{}: before cutoff now={} cutoff={}", logPrefix, now, cutOffTime);
            }
            return allowed;
        } catch (Exception ex) {
            log.warn("{}: invalid cutoff time value='{}' format='{}'", logPrefix, rawCutOffTime, format, ex);
            return false;
        }
    }

    protected boolean isWithinExecutionWindow(String logPrefix,
                                              String purpose,
                                              String startTimeCode,
                                              String endTimeCode,
                                              String timeFormat,
                                              ZoneId zoneId) {
        if (isForceExecuteEnabled(purpose)) {
            log.info("{}: forceExecute is enabled (purpose={}, code={}); bypassing execution window check",
                logPrefix,
                purpose,
                forceExecuteCode());
            return true;
        }

        String startRaw = resolveStringValue(purpose, startTimeCode);
        String endRaw = resolveStringValue(purpose, endTimeCode);

        if (startRaw.isBlank() || endRaw.isBlank()) {
            log.warn("{}: missing execution window config (purpose={}, startCode={}, endCode={})",
                logPrefix,
                purpose,
                startTimeCode,
                endTimeCode);
            return false;
        }

        String format = (timeFormat == null || timeFormat.isBlank()) ? "HH:mm" : timeFormat.trim();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalTime startTime = LocalTime.parse(startRaw, formatter);
            LocalTime endTime = LocalTime.parse(endRaw, formatter);
            LocalTime now = LocalTime.now(zoneId);

            boolean withinWindow;
            if (endTime.isBefore(startTime)) {
                // Supports windows that cross midnight, e.g. 23:00 to 05:00.
                withinWindow = !now.isBefore(startTime) || !now.isAfter(endTime);
            } else {
                withinWindow = !now.isBefore(startTime) && !now.isAfter(endTime);
            }

            if (!withinWindow) {
                log.info("{}: outside execution window now={} start={} end={}", logPrefix, now, startTime, endTime);
            }
            return withinWindow;
        } catch (Exception ex) {
            log.warn("{}: invalid execution window config startTime='{}' endTime='{}' format='{}'",
                logPrefix,
                startRaw,
                endRaw,
                format,
                ex);
            return false;
        }
    }

    protected void updateLastUpdatedNowUtc(String purpose, String lastUpdatedCode) {
        String nowUtc = Instant.now().toString();
        boolean updated = systemConfigService.updateValue(purpose, lastUpdatedCode, nowUtc);
        if (!updated) {
            log.warn("AbstractCronjob: failed to update lastUpdated config (purpose={}, code={})", purpose, lastUpdatedCode);
        }
    }

    protected void updateLastUpdatedForProcessingDate(String purpose,
                                                      String lastUpdatedCode,
                                                      LocalDate processingDate,
                                                      ZoneId zoneId) {
        String processingDateValue = processingDate.toString();

        boolean updated = systemConfigService.updateValue(purpose, lastUpdatedCode, processingDateValue);
        if (!updated) {
            log.warn("AbstractCronjob: failed to update lastUpdated for processingDate={} (purpose={}, code={})",
                processingDate,
                purpose,
                lastUpdatedCode);
        }
    }

    protected boolean isForceExecuteEnabled(String purpose) {
        return systemConfigService
            .findByPurposeAndCode(purpose, forceExecuteCode())
            .map(value -> {
                if (value instanceof Boolean boolValue) {
                    return boolValue;
                }
                if (value instanceof String strValue) {
                    return Boolean.parseBoolean(strValue.trim());
                }
                return Boolean.FALSE;
            })
            .orElse(Boolean.FALSE);
    }
}