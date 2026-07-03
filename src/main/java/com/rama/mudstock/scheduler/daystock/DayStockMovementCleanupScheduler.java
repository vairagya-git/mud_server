package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.service.CronJobConfigSupport;
import com.rama.mudstock.service.DayStockMovementService;

/**
 * Periodically removes redundant auto-generated every-day day_stock_movement_key rows: when a date
 * has both a genuine key and the auto every-day key (code ending in a configured watchlist-code),
 * the auto one and its day_stock_movement_map / day_stock_movement_entry rows are deleted.
 */
@Component
@Profile("cronjob")
public class DayStockMovementCleanupScheduler {
    private final DayStockMovementService dayStockMovementService;
    private final CronJobConfigSupport cronJobConfigSupport;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementCleanupScheduler.class);

    public DayStockMovementCleanupScheduler(DayStockMovementService dayStockMovementService,
                                            CronJobConfigSupport cronJobConfigSupport) {
        this.dayStockMovementService = dayStockMovementService;
        this.cronJobConfigSupport = cronJobConfigSupport;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = "Europe/Lisbon")
    public void cleanupRedundantKeys() {
        var enabledCfg = SystemConfigEnum.DayStockMovementCleanup.ENABLED;
        var cronCfg = SystemConfigEnum.DayStockMovementCleanup.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.DayStockMovementCleanup.LAST_UPDATED;
        String purpose = enabledCfg.purpose();

        boolean enabled = cronJobConfigSupport.isEnabled(purpose, enabledCfg.code());

        if (!enabled) {
            log.info("DayStockMovementCleanupScheduler: disabled by system_config (purpose={}, code={})",
                purpose, enabledCfg.code());
            return;
        }

        if (!cronJobConfigSupport.shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            java.time.ZoneId.of("Europe/Lisbon"))) {
            return;
        }

        log.info("DayStockMovementCleanupScheduler: scanning for redundant every-day movement keys");
        try {
            int removed = dayStockMovementService.cleanupRedundantMasters();
            cronJobConfigSupport.updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
            log.info("DayStockMovementCleanupScheduler: removed {} redundant key(s)", removed);
        } catch (Exception ex) {
            log.error("DayStockMovementCleanupScheduler: error during cleanup", ex);
        }
    }
}
