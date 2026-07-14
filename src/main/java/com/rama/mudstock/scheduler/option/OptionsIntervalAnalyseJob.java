package com.rama.mudstock.scheduler.option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.facade.OptionsIntervalAnalyseFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Daily cronjob for options interval analysis.
 * It processes only CREATE_CONTRACT entries from options_interval_analyse.
 */
@Component
@Profile("cronjob")
public class OptionsIntervalAnalyseJob extends AbstractCronjob {

    private final OptionsIntervalAnalyseFacade optionsIntervalAnalyseFacade;
    private final Logger log = LoggerFactory.getLogger(OptionsIntervalAnalyseJob.class);

    public OptionsIntervalAnalyseJob(OptionsIntervalAnalyseFacade optionsIntervalAnalyseFacade,
                                     SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.optionsIntervalAnalyseFacade = optionsIntervalAnalyseFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void analyseOptionContracts() {
        String purpose = "OptionsIntervalAnalyseDailyJob";

        if (!isEnabled(purpose)) {
            log.info("OptionsIntervalAnalyseJob: disabled by system_config (purpose={}, code={})", purpose, enabledCode());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(purpose, LISBON)) {
            return;
        }

        try {
            int processed = optionsIntervalAnalyseFacade.analyseDaily();
            log.info("OptionsIntervalAnalyseJob: processed {} option contract(s)", processed);
            updateLastUpdatedNowUtc(purpose);
        } catch (Exception ex) {
            log.error("OptionsIntervalAnalyseJob: option contract analysis failed", ex);
        }
    }
}