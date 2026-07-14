package com.rama.mudstock.scheduler.daystock;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.DayStockMovementService;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class StockMovementKeyMapEntryScheduler extends AbstractCronjob {

    private final DayStockMovementService dayStockMovementService;
    private final Logger log = LoggerFactory.getLogger(StockMovementKeyMapEntryScheduler.class);

    @Value("${dayStockMovementKeyMapEntry.cron:}")
    private String dayStockMovementKeyMapEntryCron;

    public StockMovementKeyMapEntryScheduler(DayStockMovementService dayStockMovementService,
                                             SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.dayStockMovementService = dayStockMovementService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void runDayStockMovementKeyMapEntry() {
        String purpose = "DayStockMovementKeyMapEntry";

        boolean enabled = isEnabled(purpose);

        if (!enabled) {
            log.info("StockMovementKeyMapEntryScheduler: disabled by system_config (purpose={}, code={})",
                purpose, enabledCode());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(purpose, LISBON)) {
            return;
        }

        LocalDate today = LocalDate.now(LISBON);

        DayStockMovementService.KeyPreparationResult preparation =
            dayStockMovementService.prepareDayStockMovementKeys(today);
        if (preparation.marketClosed()) {
            log.info("StockMovementKeyMapEntryScheduler: market is closed on {} (weekend or holiday), skipping", today);
            return;
        }

        DayStockMovementService.Result mappingResult =
            dayStockMovementService.createMappingsForPreparedKeys(today, preparation);

        if (!mappingResult.watchlistFound()) {
            log.info("StockMovementKeyMapEntryScheduler: no watchlist found/configured for day-stock mapping; skipping lastUpdated update");
            return;
        }

        updateLastUpdatedNowUtc(purpose);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (dayStockMovementKeyMapEntryCron == null || dayStockMovementKeyMapEntryCron.isBlank()) {
            log.warn("StockMovementKeyMapEntryScheduler: dayStockMovementKeyMapEntry.cron is not set or empty. @Scheduled may not be active.");
        } else {
            log.info("StockMovementKeyMapEntryScheduler initialized with cron='{}'", dayStockMovementKeyMapEntryCron);
        }
    }
}
