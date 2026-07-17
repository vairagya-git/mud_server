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

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void calculateMetrics() {
        String purpose = CronjobConfigEnum.Purpose.OPTION_SNAPSHOT_IV_METRICS.value();

        if (!isEnabled(purpose)) {
            log.info("OptionSnapshotIVMetricsCronjob: disabled by system_config (purpose={}, code={})", purpose, enabledCode());
            return;
        }

        LocalDate metricsDate = resolveNextProcessingDate(purpose, LISBON);
        if (!shouldExecuteSinceLastUpdated("OptionSnapshotIVMetricsCronjob", metricsDate, purpose, LISBON)) {
            return;
        }

        try {
            int rows = optionSnapshotIVMetricsFacade.calculateForDate(metricsDate);
            log.info("OptionSnapshotIVMetricsCronjob: completed ivDate={} rows={}", metricsDate, rows);
            updateLastUpdatedForProcessingDate(purpose, lastUpdatedCode(), metricsDate, LISBON);
        } catch (Exception ex) {
            log.error("OptionSnapshotIVMetricsCronjob: failed to calculate IV metrics", ex);
        }
    }
}