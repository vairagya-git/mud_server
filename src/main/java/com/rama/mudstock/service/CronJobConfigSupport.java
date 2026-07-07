package com.rama.mudstock.service;

import java.time.Instant;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.util.CronScheduleUtil;

@Service
public class CronJobConfigSupport {

    private static final Logger log = LoggerFactory.getLogger(CronJobConfigSupport.class);

    private final SystemConfigService systemConfigService;

    public CronJobConfigSupport(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    public boolean isEnabled(String purpose, String enabledCode) {
        return systemConfigService
            .findByPurposeAndCode(purpose, enabledCode)
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(Boolean.FALSE);
    }

    public String resolveCronExpression(String purpose, String cronCode) {
        String raw = systemConfigService
            .findByPurposeAndCode(purpose, cronCode)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse("");

        String normalized = CronScheduleUtil.normalizeCronExpression(raw);
        if (normalized.isBlank()) {
            log.warn("CronJobConfigSupport: missing cron expression in system_config (purpose={}, code={})", purpose, cronCode);
        }
        return normalized;
    }

    public String resolveLastUpdated(String purpose, String lastUpdatedCode) {
        return systemConfigService
            .findByPurposeAndCode(purpose, lastUpdatedCode)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .orElse("");
    }

    public String resolveStringValue(String purpose, String code) {
        return systemConfigService
            .findByPurposeAndCode(purpose, code)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .orElse("");
    }

    public boolean shouldExecuteSinceLastUpdated(String purpose,
                                                 String cronCode,
                                                 String lastUpdatedCode,
                                                 ZoneId zoneId) {
        String cronExpression = resolveCronExpression(purpose, cronCode);
        String lastUpdated = resolveLastUpdated(purpose, lastUpdatedCode);
        return CronScheduleUtil.shouldExecuteSinceLastUpdated(cronExpression, lastUpdated, zoneId);
    }

    public boolean shouldExecuteSinceLastUpdated(String purpose,
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

    public void updateLastUpdatedNowUtc(String purpose, String lastUpdatedCode) {
        String nowUtc = Instant.now().toString();
        boolean updated = systemConfigService.updateValue(purpose, lastUpdatedCode, nowUtc);
        if (!updated) {
            log.warn("CronJobConfigSupport: failed to update lastUpdated config (purpose={}, code={})", purpose, lastUpdatedCode);
        }
    }
}
