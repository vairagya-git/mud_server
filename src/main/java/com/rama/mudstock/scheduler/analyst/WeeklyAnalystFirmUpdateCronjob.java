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

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void run() {
        String purpose = CronjobConfigEnum.Purpose.WEEKLY_ANALYST_FIRM_UPDATE_CRONJOB.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        log.info("{}: starting weekly analyst firm sync", purpose);
        try {
            int updated = benzingaFirmService.fetchAndSaveSmart();
            updateLastUpdatedNowUtc(purpose);
            log.info("{}: completed - {} firm(s) inserted/updated", purpose, updated);
        } catch (Exception ex) {
            log.error("{}: error during firm sync", purpose, ex);
        }
    }
}