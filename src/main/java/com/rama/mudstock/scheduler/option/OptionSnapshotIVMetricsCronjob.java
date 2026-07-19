package com.rama.mudstock.scheduler.option;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.OptionSnapshotIVMetricsFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class OptionSnapshotIVMetricsCronjob extends AbstractCronjob {

    private final OptionSnapshotIVMetricsFacade optionSnapshotIVMetricsFacade;
    private final Logger log = LoggerFactory.getLogger(OptionSnapshotIVMetricsCronjob.class);

    public OptionSnapshotIVMetricsCronjob(OptionSnapshotIVMetricsFacade optionSnapshotIVMetricsFacade,
                                          SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.optionSnapshotIVMetricsFacade = optionSnapshotIVMetricsFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void calculateMetrics() {
        String purpose = CronjobConfigEnum.Purpose.OPTION_SNAPSHOT_IV_METRICS.value();

        LocalDate metricsDate = LocalDate.now(com.rama.mudstock.config.ApplicationConfig.LISBON);
        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        try {
            int rows = optionSnapshotIVMetricsFacade.calculateForDate(metricsDate);
            log.info("{}: completed ivDate={} rows={}", purpose, metricsDate, rows);
            updateLastUpdatedNowUtc(purpose);
        } catch (Exception ex) {
            log.error("{}: failed to calculate IV metrics", purpose, ex);
        }
    }
}