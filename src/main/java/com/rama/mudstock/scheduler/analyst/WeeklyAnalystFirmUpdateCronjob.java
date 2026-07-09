package com.rama.mudstock.scheduler.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.BenzingaFirmService;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Weekly cronjob that fetches analyst firm data from the Benzinga endpoint
 * and performs a smart upsert — only updating rows whose {@code last_updated}
 * date differs from what is already stored in the database.
 *
 * <p>Cron configuration is read from {@code system_config}:</p>
 * <pre>
 * purpose = WeeklyAnalystFirmUpdateCronjob
 * code = cronExpression
 * </pre>
 */
@Component
@Profile("cronjob")
public class WeeklyAnalystFirmUpdateCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyAnalystFirmUpdateCronjob.class);

    private final BenzingaFirmService benzingaFirmService;

    public WeeklyAnalystFirmUpdateCronjob(BenzingaFirmService benzingaFirmService,
                                          SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.benzingaFirmService = benzingaFirmService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void run() {
        var enabledCfg = SystemConfigEnum.WeeklyAnalystFirmUpdateCronjob.ENABLED;
        var cronCfg = SystemConfigEnum.WeeklyAnalystFirmUpdateCronjob.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.WeeklyAnalystFirmUpdateCronjob.LAST_UPDATED;
        String purpose = enabledCfg.purpose();

        boolean enabled = isEnabled(purpose, enabledCfg.code());

        if (!enabled) {
            log.info("WeeklyAnalystFirmUpdateCronjob: disabled by system_config (purpose={}, code={})",
                purpose,
                enabledCfg.code());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            LISBON)) {
            return;
        }

        log.info("WeeklyAnalystFirmUpdateCronjob: starting weekly analyst firm sync");
        try {
            int updated = benzingaFirmService.fetchAndSaveSmart();
            updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
            log.info("WeeklyAnalystFirmUpdateCronjob: completed — {} firm(s) inserted/updated", updated);
        } catch (Exception ex) {
            log.error("WeeklyAnalystFirmUpdateCronjob: error during firm sync", ex);
        }
    }
}