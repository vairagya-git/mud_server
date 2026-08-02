package com.rama.mudstock.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.enums.SystemConfigEnum;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.util.WatchlistUtil;

@Service
public class WatchlistStockService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistStockService.class);

    private final SystemConfigService systemConfigService;
    private final WatchlistRepository watchlistRepository;

    public WatchlistStockService(SystemConfigService systemConfigService,
                                 WatchlistRepository watchlistRepository) {
        this.systemConfigService = systemConfigService;
        this.watchlistRepository = watchlistRepository;
    }

    /**
     * Returns unique stocks (ticker + id) for the watchlist codes configured
     * under SystemConfigEnum.SYSTEM_WATCHLIST_CODES.
     */
    public List<Stock> getAllSystemWatchlistStocks() {
        List<String> watchlistCodeList = systemConfigService
            .findByPurposeAndCode(
                SystemConfigEnum.SYSTEM_WATCHLIST_CODES.purpose(),
                SystemConfigEnum.SYSTEM_WATCHLIST_CODES.code())
            .filter(List.class::isInstance)
            .map(v -> ((List<?>) v).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList())
            .orElse(List.of());

        String watchlistCodes = String.join(",", watchlistCodeList);
        return WatchlistUtil.collectUniqueStocksByTicker(
            watchlistCodes,
            watchlistRepository,
            log,
            SystemConfigEnum.SYSTEM_WATCHLIST_CODES.purpose());
    }
}
