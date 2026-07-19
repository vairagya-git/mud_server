package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.DayStockMovementFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.TypeConverstionUtil;

@Component
@Profile("cronjob")
public class DayStockMovementCronjob extends AbstractCronjob {
    private final DayStockMovementFacade dayStockMovementFacade;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementCronjob.class);

    public DayStockMovementCronjob(DayStockMovementFacade dayStockMovementFacade,
                                   SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.dayStockMovementFacade = dayStockMovementFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void pollDayStockMovementMappings() {
        String purpose = CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_DATA.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        log.info("{}: polling for NEW day-stock-movement mappings and fetching aggregates", purpose);
        try {
            String cutOffTime = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.CUTOFF_TIME.code()));
            dayStockMovementFacade.fetchAggregatesForNewMappings(
                cutOffTime,
                CronjobConfigEnum.CUTOFF_TIME.format(),
                com.rama.mudstock.config.ApplicationConfig.LISBON);
            updateLastUpdatedNowUtc(purpose);
        } catch (Exception ex) {
            log.error("{}: error while fetching aggregates", purpose, ex);
        }
    }
}
