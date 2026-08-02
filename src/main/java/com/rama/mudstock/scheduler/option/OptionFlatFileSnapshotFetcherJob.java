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

    private final MarketCalendarService marketCalendarService;
    private final OptionFlatFileSnapshotFetcherFacade optionFlatFileSnapshotFetcherFacade;
    private final Logger log = LoggerFactory.getLogger(OptionFlatFileSnapshotFetcherJob.class);

    public OptionFlatFileSnapshotFetcherJob(OptionFlatFileSnapshotFetcherFacade optionFlatFileSnapshotFetcherFacade,
                                            MarketCalendarService marketCalendarService,
                                            SystemConfigService systemConfigService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.OPTION_FLAT_FILE_SNAPSHOT_FETCHER_JOB.value(), marketCalendarService);
        this.optionFlatFileSnapshotFetcherFacade = optionFlatFileSnapshotFetcherFacade;
        this.marketCalendarService = marketCalendarService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void fetchSnapshots() {

        // backfillJulyOnce(); // Uncomment this line to run the backfill for July 2026

        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        try {
            LocalDate targetDate = resolveValidTargetDate(getPurpose());
            if (targetDate == null) {
                return;
            }

            long snapshotVersion = Instant.now().toEpochMilli();
            int inserted = optionFlatFileSnapshotFetcherFacade.fetchAndStoreSnapshots(snapshotVersion, targetDate, isForceExecuteEnabled(getPurpose()));
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

    /**
     * One-time backfill: fetch and store flat-file snapshots for each day
     * from 2025-07-01 to 2025-07-31 (inclusive), with a 3-minute delay after each day.
     */
    public void backfillJulyOnce() {
        LocalDate start = LocalDate.of(2026, 6, 24);
        LocalDate end = LocalDate.of(2026, 6, 30);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (marketCalendarService.isMarketClosed(date)) {
                log.info("{}: market closed on date={}, skipping backfill for this date", getPurpose(), date);
                continue;
            }
            try {
                long snapshotVersion = Instant.now().toEpochMilli();
                int inserted = optionFlatFileSnapshotFetcherFacade.fetchAndStoreSnapshots(snapshotVersion, date, isForceExecuteEnabled(getPurpose()));
                log.info("{}: backfill inserted {} option_snapshot row(s) for date={}, snapshotVersion={}",
                    getPurpose(), inserted, date, snapshotVersion);
            } catch (Exception ex) {
                log.error("{}: backfill failed for date={}", getPurpose(), date, ex);
            }

            try {
                Thread.sleep(1 * 60 * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("{}: backfill delay interrupted, stopping backfill loop", getPurpose());
                break;
            }
        }
    }
}
