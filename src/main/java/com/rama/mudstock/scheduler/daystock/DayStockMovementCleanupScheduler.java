package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.service.DayStockMovementService;

/**
 * Periodically removes redundant auto-generated every-day day_stock_movement_key rows: when a date
 * has both a genuine key and the auto every-day key (code ending in the everyday-watchlist-code),
 * the auto one and its day_stock_movement_map / day_stock_movement_entry rows are deleted.
 */
@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "dayStockMovementCleanup", name = "enabled", havingValue = "true")
public class DayStockMovementCleanupScheduler {
    private final DayStockMovementService dayStockMovementService;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementCleanupScheduler.class);

    public DayStockMovementCleanupScheduler(DayStockMovementService dayStockMovementService) {
        this.dayStockMovementService = dayStockMovementService;
    }

    // cron expression configured in application-cronjob.yml: dayStockMovementCleanup.cron
    @Scheduled(cron = "${dayStockMovementCleanup.cron}")
    public void cleanupRedundantKeys() {
        log.info("DayStockMovementCleanupScheduler: scanning for redundant every-day movement keys");
        try {
            int removed = dayStockMovementService.cleanupRedundantMasters();
            log.info("DayStockMovementCleanupScheduler: removed {} redundant key(s)", removed);
        } catch (Exception ex) {
            log.error("DayStockMovementCleanupScheduler: error during cleanup", ex);
        }
    }
}
