package com.rama.mudstock.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.CronScheduleUtil;

public abstract class AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(AbstractCronjob.class);

    public static final String LISBON_ZONE = "Europe/Lisbon";
    protected static final ZoneId LISBON = ZoneId.of(LISBON_ZONE);

    private final SystemConfigService systemConfigService;

    protected AbstractCronjob(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    protected boolean isEnabled(String purpose, String enabledCode) {
        return systemConfigService
            .findByPurposeAndCode(purpose, enabledCode)
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(Boolean.FALSE);
    }

    protected String resolveCronExpression(String purpose, String cronCode) {
        String raw = systemConfigService
            .findByPurposeAndCode(purpose, cronCode)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse("");

        String normalized = CronScheduleUtil.normalizeCronExpression(raw);
        if (normalized.isBlank()) {
            log.warn("AbstractCronjob: missing cron expression in system_config (purpose={}, code={})", purpose, cronCode);
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
        try {
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    protected LocalDate resolveNextProcessingDate(String rawLastUpdated, ZoneId zoneId) {
        LocalDate lastUpdatedDate = resolveLastUpdatedDate(rawLastUpdated, zoneId);
        if (lastUpdatedDate == null) {
            return LocalDate.now(zoneId);
        }
        return lastUpdatedDate.plusDays(1);
    }

    protected boolean shouldProcessDate(String logPrefix,
                                        LocalDate processingDate,
                                        String purpose,
                                        String cronCode,
                                        String lastUpdatedCode,
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

        if (!shouldExecuteSinceLastUpdated(purpose, cronCode, lastUpdatedCode, zoneId)) {
            return false;
        }

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

    protected boolean shouldExecuteSinceLastUpdated(String purpose,
                                                    String cronCode,
                                                    String lastUpdatedCode,
                                                    ZoneId zoneId) {
        String cronExpression = resolveCronExpression(purpose, cronCode);
        String lastUpdated = resolveLastUpdated(purpose, lastUpdatedCode);
        return CronScheduleUtil.shouldExecuteSinceLastUpdated(cronExpression, lastUpdated, zoneId);
    }

    protected boolean shouldExecuteSinceLastUpdated(String purpose,
                                                    String cronCode,
                                                    String lastUpdatedCode,
                                                    String cutOffTimeCode,
                                                    String cutOffTimeFormat,
                                                    ZoneId zoneId) {
        String cronExpression = resolveCronExpression(purpose, cronCode);
        String lastUpdated = resolveLastUpdated(purpose, lastUpdatedCode);
        String cutOffTime = resolveStringValue(purpose, cutOffTimeCode);
        return CronScheduleUtil.shouldExecuteSinceLastUpdated(cronExpression, lastUpdated, cutOffTime, cutOffTimeFormat, zoneId);
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
}