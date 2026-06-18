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

import com.rama.mudstock.service.EveryDayEventService;

@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "everyDayEvent", name = "enabled", havingValue = "true")
public class EveryDayWatchlistEventScheduler {
    private final EveryDayEventService everyDayEventService;
    private final Logger log = LoggerFactory.getLogger(EveryDayWatchlistEventScheduler.class);

    @Value("${everyDayEvent.cron:}")
    private String everyDayEventCron;

    public EveryDayWatchlistEventScheduler(EveryDayEventService everyDayEventService) {
        this.everyDayEventService = everyDayEventService;
    }

    // Cron configured in application-cronjob.yml: everyDayEvent.cron
    // zone ensures 22:00 is interpreted as Portugal local time (WET/WEST) on any host
    @Scheduled(cron = "${everyDayEvent.cron}", zone = "Europe/Lisbon")
    public void runEveryDayEvent() {
        log.info("EveryDayWatchlistEventScheduler: running scheduled job for today");
        everyDayEventService.populateForDate(LocalDate.now());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (everyDayEventCron == null || everyDayEventCron.isBlank()) {
            log.warn("EveryDayWatchlistEventScheduler: everyDayEvent.cron is not set or empty. @Scheduled may not be active.");
        } else {
            log.info("EveryDayWatchlistEventScheduler initialized with cron='{}'", everyDayEventCron);
        }
    }
}
