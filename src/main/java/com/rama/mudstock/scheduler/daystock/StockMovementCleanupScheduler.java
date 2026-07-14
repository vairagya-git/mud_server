package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.DayStockMovementService;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Periodically removes redundant auto-generated every-day day_stock_movement_key rows: when a date
 * has both a genuine key and the auto every-day key (code ending in a configured watchlist-code),
 * the auto one and its day_stock_movement_map / day_stock_movement_entry rows are deleted.
 */
@Component
@Profile("cronjob")
public class StockMovementCleanupScheduler extends AbstractCronjob {
    private final DayStockMovementService dayStockMovementService;
    private final Logger log = LoggerFactory.getLogger(StockMovementCleanupScheduler.class);

    public StockMovementCleanupScheduler(DayStockMovementService dayStockMovementService,
                                         SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.dayStockMovementService = dayStockMovementService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void cleanupRedundantKeys() {
        String purpose = "DayStockMovementCleanup";

        boolean enabled = isEnabled(purpose);

        if (!enabled) {
            log.info("StockMovementCleanupScheduler: disabled by system_config (purpose={}, code={})",
                purpose, enabledCode());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(purpose, LISBON)) {
            return;
        }

        log.info("StockMovementCleanupScheduler: scanning for redundant every-day movement keys");
        try {
            int removed = dayStockMovementService.cleanupRedundantMasters();
            updateLastUpdatedNowUtc(purpose);
            log.info("StockMovementCleanupScheduler: removed {} redundant key(s)", removed);
        } catch (Exception ex) {
            log.error("StockMovementCleanupScheduler: error during cleanup", ex);
        }
    }
}
