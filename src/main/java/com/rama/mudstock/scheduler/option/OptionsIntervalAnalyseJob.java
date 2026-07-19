package com.rama.mudstock.scheduler.option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
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

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void analyseOptionContracts() {
        String purpose = CronjobConfigEnum.Purpose.OPTIONS_INTERVAL_ANALYSE_DAILY_JOB.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        try {
            int processed = optionsIntervalAnalyseFacade.analyseDaily();
            log.info("{}: processed {} option contract(s)", purpose, processed);
            updateLastUpdatedNowUtc(purpose);
        } catch (Exception ex) {
            log.error("{}: option contract analysis failed", purpose, ex);
        }
    }
}