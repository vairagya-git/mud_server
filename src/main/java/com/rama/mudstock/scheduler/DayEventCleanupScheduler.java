package com.rama.mudstock.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.service.EveryDayEventService;

/**
 * Periodically removes redundant auto-generated every-day day_event_master rows: when a date has both
 * a genuine master and the auto every-day master (code ending in the everyday-watchlist-code), the auto
 * one and its day_event_map / day_event_entry rows are deleted.
 */
@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "dayEventCleanupDuplilcate", name = "enabled", havingValue = "true")
public class DayEventCleanupScheduler {
    private final EveryDayEventService everyDayEventService;
    private final Logger log = LoggerFactory.getLogger(DayEventCleanupScheduler.class);

    public DayEventCleanupScheduler(EveryDayEventService everyDayEventService) {
        this.everyDayEventService = everyDayEventService;
    }

    // cron expression configured in application-cronjob.yml: dayEventCleanupDuplilcate.cron
    @Scheduled(cron = "${dayEventCleanupDuplilcate.cron}")
    public void cleanupRedundantMasters() {
        log.info("DayEventCleanupScheduler: scanning for redundant every-day masters");
        try {
            int removed = everyDayEventService.cleanupRedundantMasters();
            log.info("DayEventCleanupScheduler: removed {} redundant master(s)", removed);
        } catch (Exception ex) {
            log.error("DayEventCleanupScheduler: error during cleanup", ex);
        }
    }
}
