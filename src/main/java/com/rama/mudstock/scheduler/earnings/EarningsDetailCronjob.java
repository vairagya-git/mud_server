package com.rama.mudstock.scheduler.earnings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class EarningsDetailCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(EarningsDetailCronjob.class);

    public EarningsDetailCronjob(SystemConfigService systemConfigService) {
        super(systemConfigService);
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void run() {
        String purpose = CronjobConfigEnum.Purpose.EARNINGS_DETAIL_CRONJOB.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        try {
            log.info("{}: stub execution started", purpose);
            // TODO: Implement earnings detail population logic.
            updateLastUpdatedNowUtc(purpose);
            log.info("{}: stub execution finished", purpose);
        } catch (Exception ex) {
            log.error("{}: stub execution failed", purpose, ex);
        }
    }
}
