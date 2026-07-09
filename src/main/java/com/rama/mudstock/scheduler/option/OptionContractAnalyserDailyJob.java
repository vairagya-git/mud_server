package com.rama.mudstock.scheduler.option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.facade.OptionContractAnalyserFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class OptionContractAnalyserDailyJob extends AbstractCronjob {

    private final OptionContractAnalyserFacade optionContractAnalyserFacade;
    private final Logger log = LoggerFactory.getLogger(OptionContractAnalyserDailyJob.class);

    public OptionContractAnalyserDailyJob(OptionContractAnalyserFacade optionContractAnalyserFacade,
                                          SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.optionContractAnalyserFacade = optionContractAnalyserFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void analyseOptionContracts() {
        var enabledCfg = SystemConfigEnum.OptionContractAnalyserDailyJob.ENABLED;
        var cronCfg = SystemConfigEnum.OptionContractAnalyserDailyJob.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.OptionContractAnalyserDailyJob.LAST_UPDATED;
        String purpose = enabledCfg.purpose();

        if (!isEnabled(purpose, enabledCfg.code())) {
            log.info("OptionContractAnalyserDailyJob: disabled by system_config (purpose={}, code={})", purpose, enabledCfg.code());
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
            int processed = optionContractAnalyserFacade.analyseDaily();
            log.info("OptionContractAnalyserDailyJob: processed {} option contract(s)", processed);
            updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
        } catch (Exception ex) {
            log.error("OptionContractAnalyserDailyJob: option contract analysis failed", ex);
        }
    }
}