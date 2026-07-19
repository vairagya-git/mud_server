package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
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
public class DayStockMovementCleanupCronjob extends AbstractCronjob {
    private final DayStockMovementService dayStockMovementService;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementCleanupCronjob.class);

    public DayStockMovementCleanupCronjob(DayStockMovementService dayStockMovementService,
                                          SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.dayStockMovementService = dayStockMovementService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void cleanupRedundantKeys() {
        String purpose = CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_CLEANUP.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        log.info("{}: scanning for redundant every-day movement keys", purpose);
        try {
            int removed = dayStockMovementService.cleanupRedundantMasters();
            updateLastUpdatedNowUtc(purpose);
            log.info("{}: removed {} redundant key(s)", purpose, removed);
        } catch (Exception ex) {
            log.error("{}: error during cleanup", purpose, ex);
        }
    }
}
