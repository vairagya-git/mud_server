package com.rama.mudstock.scheduler.earnings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.DayStockMovementFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class EarningsDetailCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(EarningsDetailCronjob.class);
    private final DayStockMovementFacade dayStockMovementFacade;

    public EarningsDetailCronjob(SystemConfigService systemConfigService,
                                 DayStockMovementFacade dayStockMovementFacade) {
        super(systemConfigService, CronjobConfigEnum.Purpose.EARNINGS_DETAIL_CRONJOB.value());
        this.dayStockMovementFacade = dayStockMovementFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void run() {
        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        try {
            log.info("{}: execution started", getPurpose());
            dayStockMovementFacade.fetchAggregatesForPastEarningsWindow();
            updateLastUpdatedNowUtc(getPurpose());
            log.info("{}: execution finished", getPurpose());
        } catch (Exception ex) {
            log.error("{}: execution failed", getPurpose(), ex);
        }
    }
}
