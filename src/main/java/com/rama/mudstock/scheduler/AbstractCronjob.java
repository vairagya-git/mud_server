package com.rama.mudstock.scheduler;

import java.time.Instant;
import java.time.ZoneId;

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
}