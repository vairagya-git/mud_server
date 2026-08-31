package com.rama.mudstock.facade.optiontrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionSnapshotFlatfileRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.repository.option.OptionStrategyRepository;
import com.rama.mudstock.repository.option.OptionTradeRepository;

@Service
public class OptionTradeStrategySnapshotLiveRefinementFacade extends AbstractOptionTradeStrategySnapshotRefinementFacade {

    private final Logger log = LoggerFactory.getLogger(OptionTradeStrategySnapshotLiveRefinementFacade.class);

    public OptionTradeStrategySnapshotLiveRefinementFacade(OptionTradeRepository optionTradeRepository,
                                                           OptionStrategyRepository optionStrategyRepository,
                                                           OptionSnapshotRepository optionSnapshotRepository,
                                                           OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository) {
        super(optionTradeRepository, optionStrategyRepository, optionSnapshotRepository, optionSnapshotFlatfileRepository);
    }

    @Override
    public int enrichTradeStrategySnapshot(OptionTradeRepository.TradeMode tradeMode,
                                           Integer optionSnapshotInterval,
                                           String lastFetchedSnapshotTime,
                                           String lastFetchedFlatFileTime,
                                           String lastFetchedManualEntryTime) {
        log.info("OptionTradeStrategySnapshotLiveRefinementFacade is a stub. tradeMode={}, interval={}", tradeMode, optionSnapshotInterval);
        return 0;
    }
}
