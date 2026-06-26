package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.service.MassiveRestStockService;

@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "dayStockMovementData", name = "enabled", havingValue = "true")
public class DayStockMovementScheduler {
    private final MassiveRestStockService massiveService;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementScheduler.class);

    public DayStockMovementScheduler(MassiveRestStockService massiveService) {
        this.massiveService = massiveService;
    }

    // cron expression configured in application-cronjob.yml: dayStockMovementData.cron
    @Scheduled(cron = "${dayStockMovementData.cron}")
    public void pollDayStockMovementMappings() {
        log.info("DayStockMovementScheduler: polling for NEW day-stock-movement mappings and fetching aggregates");
        try {
            massiveService.fetchAggregatesForNewMappings();
        } catch (Exception ex) {
            log.error("DayStockMovementScheduler: error while fetching aggregates", ex);
        }
    }
}
