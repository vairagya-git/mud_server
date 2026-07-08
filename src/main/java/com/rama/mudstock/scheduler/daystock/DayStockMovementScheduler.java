package com.rama.mudstock.scheduler.daystock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.facade.DayStockMovementFacade;
import com.rama.mudstock.service.CronJobConfigSupport;

@Component
@Profile("cronjob")
public class DayStockMovementScheduler {
    private final DayStockMovementFacade dayStockMovementFacade;
    private final CronJobConfigSupport cronJobConfigSupport;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementScheduler.class);

    public DayStockMovementScheduler(DayStockMovementFacade dayStockMovementFacade,
                                     CronJobConfigSupport cronJobConfigSupport) {
        this.dayStockMovementFacade = dayStockMovementFacade;
        this.cronJobConfigSupport = cronJobConfigSupport;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = "Europe/Lisbon")
    public void pollDayStockMovementMappings() {
        var enabledCfg = SystemConfigEnum.DayStockMovementData.ENABLED;
        var cronCfg = SystemConfigEnum.DayStockMovementData.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.DayStockMovementData.LAST_UPDATED;
        var cutOffTimeCfg = SystemConfigEnum.DayStockMovementData.CUTOFF_TIME;
        String purpose = enabledCfg.purpose();

        boolean enabled = cronJobConfigSupport.isEnabled(purpose, enabledCfg.code());

        if (!enabled) {
            log.info("DayStockMovementScheduler: disabled by system_config (purpose={}, code={})",
                purpose, enabledCfg.code());
            return;
        }

        if (!cronJobConfigSupport.shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            cutOffTimeCfg.code(),
            cutOffTimeCfg.format(),
            java.time.ZoneId.of("Europe/Lisbon"))) {
            return;
        }

        log.info("DayStockMovementScheduler: polling for NEW day-stock-movement mappings and fetching aggregates");
        try {
            dayStockMovementFacade.fetchAggregatesForNewMappings();
            cronJobConfigSupport.updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
        } catch (Exception ex) {
            log.error("DayStockMovementScheduler: error while fetching aggregates", ex);
        }
    }
}
