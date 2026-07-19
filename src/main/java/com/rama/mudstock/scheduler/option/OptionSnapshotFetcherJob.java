package com.rama.mudstock.scheduler.option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
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

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void fetchSnapshots() {
        String purpose = CronjobConfigEnum.Purpose.OPTION_SNAPSHOT_FETCHER_JOB.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        try {
            int inserted = optionSnapshotFetcherFacade.fetchAndStoreSnapshots();
            log.info("{}: inserted {} option_snapshot row(s)", purpose, inserted);
            updateLastUpdatedNowUtc(purpose);
        } catch (Exception ex) {
            log.error("{}: snapshot fetch failed", purpose, ex);
        }
    }
}