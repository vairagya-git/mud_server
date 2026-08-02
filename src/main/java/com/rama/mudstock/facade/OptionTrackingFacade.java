package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rama.mudstock.model.option.OptionTrackingSnapshotRow;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.service.OptionDataContractService;
import com.rama.mudstock.service.OptionTrackingSnapshotService;
import com.rama.mudstock.service.WatchlistStockService;

@Service
public class OptionTrackingFacade {

    private final WatchlistStockService watchlistStockService;
    private final OptionDataContractService optionDataContractService;
    private final OptionTrackingSnapshotService optionTrackingSnapshotService;

    public OptionTrackingFacade(WatchlistStockService watchlistStockService,
                                OptionDataContractService optionDataContractService,
                                OptionTrackingSnapshotService optionTrackingSnapshotService) {
        this.watchlistStockService = watchlistStockService;
        this.optionDataContractService = optionDataContractService;
        this.optionTrackingSnapshotService = optionTrackingSnapshotService;
    }

    public List<Stock> getTrackableStocks() {
        return watchlistStockService.getAllSystemWatchlistStocks();
    }

    public Map<LocalDate, Long> getExpirationDatesForTicker(String ticker) {
        return optionDataContractService.findExpirationDatesForTicker(ticker);
    }

    public Map<String, Long> getContractTickerIdsForIntervalAnalyse(Long optionsIntervalAnalyseId) {
        return optionDataContractService.findContractTickerIdsForIntervalAnalyse(optionsIntervalAnalyseId);
    }

    public List<OptionTrackingSnapshotRow> getTrackingSnapshotRows(List<Long> contractIds) {
        return optionTrackingSnapshotService.getTrackingSnapshotRows(contractIds);
    }
}
