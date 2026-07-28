package com.rama.mudstock.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.repository.daystock.DayStockMovementEntryRepository;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;

/**
 * Shared logic for the "every day event" feature: for a given date, create a single
 * day_stock_movement_key entry for the configured watchlist-codes (from system_config) and
 * map every stock in that watchlist into day_stock_movement_map.
 *
 * Used by both the scheduled job (for today) and the manual date form.
 */
@Service
public class DayStockMovementService {
    private final Logger log = LoggerFactory.getLogger(DayStockMovementService.class);

    private final WatchlistRepository watchlistRepo;
    private final DayStockMovementEntryRepository entryRepository;
    private final SystemConfigService systemConfigService;

    public DayStockMovementService(WatchlistRepository watchlistRepo, DayStockMovementEntryRepository entryRepository,
                                   SystemConfigService systemConfigService) {
        this.watchlistRepo = watchlistRepo;
        this.entryRepository = entryRepository;
        this.systemConfigService = systemConfigService;
    }

    public String getWatchlistCode() {
        return String.join(",", getWatchlistCodes());
    }

    public List<String> getWatchlistCodes() {
        var watchlistCfg = CronjobConfigEnum.WATCHLIST_CODES;
        String purpose = CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_DATA.value();
        return systemConfigService
            .findByPurposeAndCode(purpose, watchlistCfg.code())
                .filter(List.class::isInstance)
                .map(v -> ((List<?>) v).stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .map(String::toUpperCase)
                        .distinct()
                        .toList())
                .orElse(Collections.emptyList());
    }

    public List<Map<String, Object>> listEntriesWithMeta() {
        return entryRepository.listAllEntriesWithMeta();
    }

    public List<String> listDistinctEntryTickers() {
        return entryRepository.listDistinctEntryTickers();
    }

    @Transactional
    public void refreshWatchlistData() {
        List<String> watchlistCodes = getWatchlistCodes();
        if (watchlistCodes.isEmpty()) {
            log.warn("watchlist-codes is not configured in system_config; skipping day-stock movement refresh");
            return;
        }

        for (String watchlistCode : watchlistCodes) {
            watchlistRepo.findByCode(watchlistCode).ifPresent(watchlist -> {
                log.info("Day stock movement refresh checked watchlist '{}' with {} stock(s)",
                        watchlistCode, watchlist.getStocks().size());
            });
        }
    }
}
