package com.rama.mudstock.scheduler.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.service.BenzingaFirmService;

/**
 * Weekly cronjob that fetches analyst firm data from the Benzinga endpoint
 * and performs a smart upsert — only updating rows whose {@code last_updated}
 * date differs from what is already stored in the database.
 *
 * <p>Cron configuration in {@code application-cronjob.yml}:</p>
 * <pre>
 * weeklyAnalystFirmUpdate:
 *   enabled: true
 *   cron: "0 0 21 * * SUN"   # every Sunday at 21:00 Portugal time
 * </pre>
 */
@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "weeklyAnalystFirmUpdate", name = "enabled", havingValue = "true")
public class WeeklyAnalystFirmUpdateCronjob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyAnalystFirmUpdateCronjob.class);

    private final BenzingaFirmService benzingaFirmService;

    public WeeklyAnalystFirmUpdateCronjob(BenzingaFirmService benzingaFirmService) {
        this.benzingaFirmService = benzingaFirmService;
    }

    /**
     * Runs on the configured cron schedule (default: every Sunday at 21:00 Lisbon time).
     * Fetches the full firm list from the Benzinga endpoint and performs a
     * smart upsert — skipping any firm whose {@code last_updated} date has not changed.
     */
    @Scheduled(cron = "${weeklyAnalystFirmUpdate.cron}", zone = "Europe/Lisbon")
    public void run() {
        log.info("WeeklyAnalystFirmUpdateCronjob: starting weekly analyst firm sync");
        try {
            int updated = benzingaFirmService.fetchAndSaveSmart();
            log.info("WeeklyAnalystFirmUpdateCronjob: completed — {} firm(s) inserted/updated", updated);
        } catch (Exception ex) {
            log.error("WeeklyAnalystFirmUpdateCronjob: error during firm sync", ex);
        }
    }
}