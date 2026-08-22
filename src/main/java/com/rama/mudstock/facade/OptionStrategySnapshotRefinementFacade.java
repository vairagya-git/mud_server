package com.rama.mudstock.facade;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionTradeRepository;

@Service
public class OptionStrategySnapshotRefinementFacade {

    private final Logger log = LoggerFactory.getLogger(OptionStrategySnapshotRefinementFacade.class);

    private final OptionAPISnapshotFetcherFacade optionSnapshotFetcherFacade;
    private final OptionTradeRepository optionTradeRepository;

    public OptionStrategySnapshotRefinementFacade(OptionAPISnapshotFetcherFacade optionSnapshotFetcherFacade,
                                                  OptionTradeRepository optionTradeRepository) {
        this.optionSnapshotFetcherFacade = optionSnapshotFetcherFacade;
        this.optionTradeRepository = optionTradeRepository;
    }

    public int refineStrategySnapshot(boolean historyData,
                                      Integer optionSnapshotInterval,
                                      String lastFetchedSnapshotTime,
                                      String lastFetchedFlatFileTime,
                                      String lastFetchedManualEntryTime) {
        if (historyData) {
            int historicOpenTradeCount = optionTradeRepository.listOpenHistoricLiveTrades().size();
            log.info("refineStrategySnapshot: OPEN LIVE trades with with_historic_data=1 count={}", historicOpenTradeCount);
            if (historicOpenTradeCount == 0) {
                return 0;
            }
        }

        long snapshotVersion = Instant.now().toEpochMilli();
        int inserted = optionSnapshotFetcherFacade.fetchAndStoreSnapshots(snapshotVersion);
        log.info("refineStrategySnapshot: historyData={}, optionSnapshotInterval={}, lastFetchedSnapshotTime={}, lastFetchedFlatFileTime={}, lastFetchedManualEntryTime={}, inserted {} option_snapshot row(s), snapshotVersion={}",
            historyData,
            optionSnapshotInterval,
            lastFetchedSnapshotTime,
            lastFetchedFlatFileTime,
            lastFetchedManualEntryTime,
            inserted,
            snapshotVersion);
        return inserted;
    }
}
