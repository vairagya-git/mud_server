package com.rama.mudstock.scheduler.trade;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.optiontrade.OptionTradeStrategySnapshotLiveRefinementFacade;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.SystemConfigService;

/**
 * Periodic cronjob mirroring OptionAPISnapshotFetcherJob scheduling structure.
 */
@Component
@Profile("cronjob")
public class OptionTradeStrategySnapshotRefinementJob extends AbstractOptionStrategySnapshotRefinementJob {

    private final MarketCalendarService marketCalendarService;
    private final OptionTradeStrategySnapshotLiveRefinementFacade optionTradeStrategySnapshotLiveRefinementFacade;
    private final Logger log = LoggerFactory.getLogger(OptionTradeStrategySnapshotRefinementJob.class);

    public OptionTradeStrategySnapshotRefinementJob(OptionTradeStrategySnapshotLiveRefinementFacade optionTradeStrategySnapshotLiveRefinementFacade,
                                                    SystemConfigService systemConfigService,
                                                    MarketCalendarService marketCalendarService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.OPTION_STRATEGY_SNAPSHOT_REFINEMENT_JOB.value());
        this.optionTradeStrategySnapshotLiveRefinementFacade = optionTradeStrategySnapshotLiveRefinementFacade;
        this.marketCalendarService = marketCalendarService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void refineSnapshots() {
        loadAndLogSharedConfig(log);

        if (!shouldExecuteBySchedule(getPurpose()) || marketCalendarService.isMarketClosed(LocalDate.now())) {
            log.info("{}: market is closed or outside trading hours", getPurpose());
            return;
        }

        try {
            int inserted = optionTradeStrategySnapshotLiveRefinementFacade.enrichTradeStrategySnapshot(
                com.rama.mudstock.repository.option.OptionTradeRepository.TradeMode.LIVE,
                optionSnapshotInterval(),
                lastFetchedSnapshotTime(),
                lastFetchedFlatFileTime(),
                lastFetchedManualEntryTime());
            log.info("{}: inserted {} option_strategy_snapshot row(s)", getPurpose(), inserted);
            updateLastUpdatedNowUtc(getPurpose());
        } catch (Exception ex) {
            log.error("{}: snapshot fetch failed", getPurpose(), ex);
        }
    }
}