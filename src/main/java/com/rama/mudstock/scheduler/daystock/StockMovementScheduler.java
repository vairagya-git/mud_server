package com.rama.mudstock.scheduler.daystock;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.facade.DayStockMovementFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class StockMovementScheduler extends AbstractCronjob {
    private final DayStockMovementFacade dayStockMovementFacade;
    private final Logger log = LoggerFactory.getLogger(StockMovementScheduler.class);

    public StockMovementScheduler(DayStockMovementFacade dayStockMovementFacade,
                                  SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.dayStockMovementFacade = dayStockMovementFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void pollDayStockMovementMappings() {
        String purpose = "DayStockMovementData";

        boolean enabled = isEnabled(purpose);

        if (!enabled) {
            log.info("StockMovementScheduler: disabled by system_config (purpose={}, code={})",
                purpose, enabledCode());
            return;
        }

        LocalDate processingDate = resolveNextProcessingDate(purpose, LISBON);
        if (!shouldProcessDate("StockMovementScheduler", processingDate, purpose, LISBON)) {
            return;
        }

        log.info("StockMovementScheduler: polling for NEW day-stock-movement mappings and fetching aggregates");
        try {
            String cutOffTime = resolveStringValue(purpose, cutOffTimeCode());
            dayStockMovementFacade.fetchAggregatesForNewMappings(cutOffTime, cutOffTimeFormat(), LISBON);
            updateLastUpdatedForProcessingDate(purpose, processingDate, LISBON);
        } catch (Exception ex) {
            log.error("StockMovementScheduler: error while fetching aggregates", ex);
        }
    }
}
