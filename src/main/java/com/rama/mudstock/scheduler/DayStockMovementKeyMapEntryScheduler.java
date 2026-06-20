package com.rama.mudstock.scheduler;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.service.DayStockMovementService;
import com.rama.mudstock.service.MarketCalendarService;

@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "dayStockMovementKeyMapEntry", name = "enabled", havingValue = "true")
public class DayStockMovementKeyMapEntryScheduler {
    private final DayStockMovementService dayStockMovementService;
    private final MarketCalendarService marketCalendarService;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementKeyMapEntryScheduler.class);

    @Value("${dayStockMovementKeyMapEntry.cron:}")
    private String dayStockMovementKeyMapEntryCron;

    public DayStockMovementKeyMapEntryScheduler(DayStockMovementService dayStockMovementService,
                                                MarketCalendarService marketCalendarService) {
        this.dayStockMovementService = dayStockMovementService;
        this.marketCalendarService = marketCalendarService;
    }

    // Cron configured in application-cronjob.yml: dayStockMovementKeyMapEntry.cron
    // zone ensures 22:00 is interpreted as Portugal local time (WET/WEST) on any host
    @Scheduled(cron = "${dayStockMovementKeyMapEntry.cron}", zone = "Europe/Lisbon")
    public void runDayStockMovementKeyMapEntry() {
        LocalDate today = LocalDate.now();
        if (marketCalendarService.isMarketClosed(today)) {
            log.info("DayStockMovementKeyMapEntryScheduler: market is closed on {} (weekend or holiday), skipping", today);
            return;
        }
        log.info("DayStockMovementKeyMapEntryScheduler: creating day_stock_movement_key and mappings for today");
        dayStockMovementService.populateForDate(today);
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
