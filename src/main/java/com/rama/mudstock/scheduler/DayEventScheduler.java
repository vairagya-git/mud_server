package com.rama.mudstock.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.service.MassiveRestStockService;

@Component
public class DayEventScheduler {
    private final MassiveRestStockService massiveService;
    private final Logger log = LoggerFactory.getLogger(DayEventScheduler.class);

    public DayEventScheduler(MassiveRestStockService massiveService) {
        this.massiveService = massiveService;
    }

    // cron expression configured in application.yml: dayevent.cron
    @Scheduled(cron = "${dayevent.cron}")
    public void pollDayEventMappings() {
        log.info("DayEventScheduler: polling for NEW day-event mappings and fetching aggregates");
        try {
            massiveService.fetchAggregatesForNewMappings();
        } catch (Exception ex) {
            log.error("DayEventScheduler: error while fetching aggregates", ex);
        }
    }
}
