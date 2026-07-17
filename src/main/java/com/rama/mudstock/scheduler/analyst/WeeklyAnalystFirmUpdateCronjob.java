package com.rama.mudstock.scheduler.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.BenzingaFirmService;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Weekly cronjob that fetches analyst firm data from the Benzinga endpoint
 * and performs a smart upsert — only updating rows whose {@code last_updated}
 * date differs from what is already stored in the database.
 *
 * <p>Execution configuration is read from {@code system_config}:</p>
 * <pre>
 * purpose = WeeklyAnalystFirmUpdateCronjob
 * code = execution / minuteHourlyFrequency
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
        String purpose = CronjobConfigEnum.Purpose.WEEKLY_ANALYST_FIRM_UPDATE_CRONJOB.value();

        boolean enabled = isEnabled(purpose);

        if (!enabled) {
            log.info("WeeklyAnalystFirmUpdateCronjob: disabled by system_config (purpose={}, code={})",
                purpose,
                enabledCode());
            return;
        }

        if (!shouldExecuteSinceLastUpdated("WeeklyAnalystFirmUpdateCronjob", null, purpose, LISBON)) {
            return;
        }

        log.info("WeeklyAnalystFirmUpdateCronjob: starting weekly analyst firm sync");
        try {
            int updated = benzingaFirmService.fetchAndSaveSmart();
            updateLastUpdatedNowUtc(purpose, lastUpdatedCode());
            log.info("WeeklyAnalystFirmUpdateCronjob: completed — {} firm(s) inserted/updated", updated);
        } catch (Exception ex) {
            log.error("WeeklyAnalystFirmUpdateCronjob: error during firm sync", ex);
        }
    }
}