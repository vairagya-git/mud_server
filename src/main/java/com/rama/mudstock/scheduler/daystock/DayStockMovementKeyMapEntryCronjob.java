package com.rama.mudstock.scheduler.daystock;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.DayStockMovementService;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class DayStockMovementKeyMapEntryCronjob extends AbstractCronjob {

    private final DayStockMovementService dayStockMovementService;
    private final ApplicationProperties applicationProperties;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementKeyMapEntryCronjob.class);

    public DayStockMovementKeyMapEntryCronjob(DayStockMovementService dayStockMovementService,
                                              SystemConfigService systemConfigService,
                                              ApplicationProperties applicationProperties) {
        super(systemConfigService);
        this.dayStockMovementService = dayStockMovementService;
        this.applicationProperties = applicationProperties;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void runDayStockMovementKeyMapEntry() {
        String purpose = CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_KEY_MAP_ENTRY.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        LocalDate today = LocalDate.now(com.rama.mudstock.config.ApplicationConfig.LISBON);

        DayStockMovementService.KeyPreparationResult preparation =
            dayStockMovementService.prepareDayStockMovementKeys(today);
        if (preparation.marketClosed()) {
            log.info("{}: market is closed on {} (weekend or holiday), skipping", purpose, today);
            return;
        }

        DayStockMovementService.Result mappingResult =
            dayStockMovementService.createMappingsForPreparedKeys(today, preparation);

        if (!mappingResult.watchlistFound()) {
            log.info("{}: no watchlist found/configured for day-stock mapping; skipping lastUpdated update", purpose);
            return;
        }

        updateLastUpdatedNowUtc(purpose);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String purpose = CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_KEY_MAP_ENTRY.value();
        String dayStockMovementKeyMapEntryCron = applicationProperties.getDayStockMovementKeyMapEntry().getCron();
        if (dayStockMovementKeyMapEntryCron == null || dayStockMovementKeyMapEntryCron.isBlank()) {
            log.warn("{}: dayStockMovementKeyMapEntry.cron is not set or empty. @Scheduled may not be active.", purpose);
        } else {
            log.info("{}: initialized with cron='{}'", purpose, dayStockMovementKeyMapEntryCron);
        }
    }
}
