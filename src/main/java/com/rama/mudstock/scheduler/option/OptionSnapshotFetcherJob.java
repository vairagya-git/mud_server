package com.rama.mudstock.scheduler.option;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.facade.OptionSnapshotFetcherFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Periodic cronjob that fetches option snapshots for ACTIVE option contracts
 * and stores them in option_snapshot.
 */
@Component
@Profile("cronjob")
public class OptionSnapshotFetcherJob extends AbstractCronjob {

    private final OptionSnapshotFetcherFacade optionSnapshotFetcherFacade;
    private final Logger log = LoggerFactory.getLogger(OptionSnapshotFetcherJob.class);

    public OptionSnapshotFetcherJob(OptionSnapshotFetcherFacade optionSnapshotFetcherFacade,
                                    SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.optionSnapshotFetcherFacade = optionSnapshotFetcherFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void fetchSnapshots() {
        var enabledCfg = SystemConfigEnum.OptionSnapshotFetcherJob.ENABLED;
        var startTimeCfg = SystemConfigEnum.OptionSnapshotFetcherJob.START_TIME;
        var endTimeCfg = SystemConfigEnum.OptionSnapshotFetcherJob.END_TIME;
        var cronCfg = SystemConfigEnum.OptionSnapshotFetcherJob.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.OptionSnapshotFetcherJob.LAST_UPDATED;
        String purpose = enabledCfg.purpose();

        if (!isEnabled(purpose, enabledCfg.code())) {
            log.info("OptionSnapshotFetcherJob: disabled by system_config (purpose={}, code={})", purpose, enabledCfg.code());
            return;
        }

        if (!isWithinExecutionWindow(purpose, startTimeCfg.code(), endTimeCfg.code())) {
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
            int inserted = optionSnapshotFetcherFacade.fetchAndStoreSnapshots();
            log.info("OptionSnapshotFetcherJob: inserted {} option_snapshot row(s)", inserted);
            updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
        } catch (Exception ex) {
            log.error("OptionSnapshotFetcherJob: snapshot fetch failed", ex);
        }
    }

    private boolean isWithinExecutionWindow(String purpose, String startTimeCode, String endTimeCode) {
        String startRaw = resolveStringValue(purpose, startTimeCode);
        String endRaw = resolveStringValue(purpose, endTimeCode);

        if (startRaw.isBlank() || endRaw.isBlank()) {
            log.warn("OptionSnapshotFetcherJob: missing execution window config (purpose={}, startCode={}, endCode={})",
                purpose, startTimeCode, endTimeCode);
            return false;
        }

        try {
            LocalTime startTime = LocalTime.parse(startRaw);
            LocalTime endTime = LocalTime.parse(endRaw);
            LocalTime now = LocalTime.now(LISBON);

            boolean withinWindow = !now.isBefore(startTime) && !now.isAfter(endTime);

            if (!withinWindow) {
                log.info("OptionSnapshotFetcherJob: outside execution window now={} start={} end={}", now, startTime, endTime);
            }
            return withinWindow;
        } catch (DateTimeParseException ex) {
            log.warn("OptionSnapshotFetcherJob: invalid execution window config startTime='{}' endTime='{}'", startRaw, endRaw, ex);
            return false;
        }
    }
}