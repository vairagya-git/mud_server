package com.rama.mudstock.scheduler.option;

import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.OptionFlatFileSnapshotFetcherFacade;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Periodic cronjob scaffold for flat-file option snapshot ingestion.
 */
@Component
@Profile("cronjob")
public class OptionFlatFileSnapshotFetcherJob extends AbstractCronjob {

    private final OptionFlatFileSnapshotFetcherFacade optionFlatFileSnapshotFetcherFacade;
    private final Logger log = LoggerFactory.getLogger(OptionFlatFileSnapshotFetcherJob.class);

    public OptionFlatFileSnapshotFetcherJob(OptionFlatFileSnapshotFetcherFacade optionFlatFileSnapshotFetcherFacade,
                                            MarketCalendarService marketCalendarService,
                                            SystemConfigService systemConfigService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.OPTION_FLAT_FILE_SNAPSHOT_FETCHER_JOB.value(), marketCalendarService);
        this.optionFlatFileSnapshotFetcherFacade = optionFlatFileSnapshotFetcherFacade;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void fetchSnapshots() {
        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        try {
            LocalDate targetDate = resolveTargetDate(getPurpose());
            long snapshotVersion = Instant.now().toEpochMilli();
            int inserted = optionFlatFileSnapshotFetcherFacade.fetchAndStoreSnapshots(snapshotVersion, targetDate);
            log.info("{}: inserted {} option_snapshot row(s) from flat file, targetDate={}, snapshotVersion={}",
                getPurpose(),
                inserted,
                targetDate,
                snapshotVersion);
            updateLastUpdatedNowUtc(getPurpose());
            updateDailyDateToNextEligible(getPurpose(), targetDate);
        } catch (Exception ex) {
            log.error("{}: flat-file snapshot fetch failed", getPurpose(), ex);
        }
    }
}
