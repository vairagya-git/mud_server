package com.rama.mudstock.scheduler.option;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.OptionAPISnapshotFetcherFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Periodic cronjob that fetches option snapshots for ACTIVE option contracts
 * and stores them in option_snapshot.
 */
@Component
@Profile("cronjob")
public class OptionAPISnapshotFetcherJob extends AbstractCronjob {

    private final OptionAPISnapshotFetcherFacade optionSnapshotFetcherFacade;
    private final Logger log = LoggerFactory.getLogger(OptionAPISnapshotFetcherJob.class);

    public OptionAPISnapshotFetcherJob(OptionAPISnapshotFetcherFacade optionSnapshotFetcherFacade,
                                       SystemConfigService systemConfigService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.OPTION_API_SNAPSHOT_FETCHER_JOB.value());
        this.optionSnapshotFetcherFacade = optionSnapshotFetcherFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void fetchSnapshots() {
        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        try {
            long snapshotVersion = Instant.now().toEpochMilli();
            int inserted = optionSnapshotFetcherFacade.fetchAndStoreSnapshots(snapshotVersion);
            log.info("{}: inserted {} option_snapshot row(s), snapshotVersion={}", getPurpose(), inserted, snapshotVersion);
            updateLastUpdatedNowUtc(getPurpose());
        } catch (Exception ex) {
            log.error("{}: snapshot fetch failed", getPurpose(), ex);
        }
    }
}

//Changed For Git