package com.rama.mudstock.scheduler.option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.facade.OptionsIntervalAnalyseFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Daily cronjob for options interval analysis.
 * It processes only CREATE_CONTRACT entries from options_interval_analyse.
 */
@Component
@Profile("cronjob")
public class OptionsIntervalAnalyseDailyJob extends AbstractCronjob {

    private final OptionsIntervalAnalyseFacade optionsIntervalAnalyseFacade;
    private final Logger log = LoggerFactory.getLogger(OptionsIntervalAnalyseDailyJob.class);

    public OptionsIntervalAnalyseDailyJob(OptionsIntervalAnalyseFacade optionsIntervalAnalyseFacade,
                                          SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.optionsIntervalAnalyseFacade = optionsIntervalAnalyseFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void analyseOptionContracts() {
        var enabledCfg = SystemConfigEnum.OptionsIntervalAnalyseDailyJob.ENABLED;
        var cronCfg = SystemConfigEnum.OptionsIntervalAnalyseDailyJob.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.OptionsIntervalAnalyseDailyJob.LAST_UPDATED;
        String purpose = enabledCfg.purpose();

        if (!isEnabled(purpose, enabledCfg.code())) {
            log.info("OptionsIntervalAnalyseDailyJob: disabled by system_config (purpose={}, code={})", purpose, enabledCfg.code());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            LISBON)) {
            return;
        }

        try {
            int processed = optionsIntervalAnalyseFacade.analyseDaily();
            log.info("OptionsIntervalAnalyseDailyJob: processed {} option contract(s)", processed);
            updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
        } catch (Exception ex) {
            log.error("OptionsIntervalAnalyseDailyJob: option contract analysis failed", ex);
        }
    }
}