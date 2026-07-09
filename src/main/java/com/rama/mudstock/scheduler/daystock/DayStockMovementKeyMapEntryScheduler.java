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

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.DayStockMovementService;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class DayStockMovementKeyMapEntryScheduler extends AbstractCronjob {

    private final DayStockMovementService dayStockMovementService;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementKeyMapEntryScheduler.class);

    @Value("${dayStockMovementKeyMapEntry.cron:}")
    private String dayStockMovementKeyMapEntryCron;

    public DayStockMovementKeyMapEntryScheduler(DayStockMovementService dayStockMovementService,
                                                SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.dayStockMovementService = dayStockMovementService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void runDayStockMovementKeyMapEntry() {
        var enabledCfg = SystemConfigEnum.DayStockMovementKeyMapEntry.ENABLED;
        var cronCfg = SystemConfigEnum.DayStockMovementKeyMapEntry.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.DayStockMovementKeyMapEntry.LAST_UPDATED;
        String purpose = enabledCfg.purpose();

        boolean enabled = isEnabled(purpose, enabledCfg.code());

        if (!enabled) {
            log.info("DayStockMovementKeyMapEntryScheduler: disabled by system_config (purpose={}, code={})",
                purpose, enabledCfg.code());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            LISBON)) {
            return;
        }

        LocalDate today = LocalDate.now(LISBON);

        DayStockMovementService.KeyPreparationResult preparation =
            dayStockMovementService.prepareDayStockMovementKeys(today);
        if (preparation.marketClosed()) {
            log.info("DayStockMovementKeyMapEntryScheduler: market is closed on {} (weekend or holiday), skipping", today);
            return;
        }

        DayStockMovementService.Result mappingResult =
            dayStockMovementService.createMappingsForPreparedKeys(today, preparation);

        if (!mappingResult.watchlistFound()) {
            log.info("DayStockMovementKeyMapEntryScheduler: no watchlist found/configured for day-stock mapping; skipping lastUpdated update");
            return;
        }

        updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (dayStockMovementKeyMapEntryCron == null || dayStockMovementKeyMapEntryCron.isBlank()) {
            log.warn("DayStockMovementKeyMapEntryScheduler: dayStockMovementKeyMapEntry.cron is not set or empty. @Scheduled may not be active.");
        } else {
            log.info("DayStockMovementKeyMapEntryScheduler initialized with cron='{}'", dayStockMovementKeyMapEntryCron);
        }
    }
}
