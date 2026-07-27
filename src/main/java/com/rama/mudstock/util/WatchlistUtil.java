package com.rama.mudstock.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;

/**
 * Utility methods for watchlist-based stock collection shared across schedulers.
 *
 * <p>All methods are {@code static} — no instantiation needed.</p>
 */
public final class WatchlistUtil {

    private WatchlistUtil() {}

    /**
     * Splits a comma-separated list of watchlist codes, fetches each watchlist,
    * and returns a deduplicated list of stocks ordered by first encounter.
     * Stocks with a blank ticker are silently skipped.
     * Missing watchlists emit a WARN log but do not fail.
     *
     * @param commaSeparatedCodes comma-separated watchlist codes (e.g. {@code "MOVING_STOCK,TECH_WATCH"})
     * @param repository          {@link WatchlistRepository} used to fetch each watchlist with its stocks
     * @param log                 caller's logger (used for WARN messages on missing watchlists)
     * @param callerName          simple name of the caller, used in log messages
     * @return insertion-ordered list of unique {@link Stock} entries, duplicates across watchlists removed
     */
    public static List<Stock> collectUniqueStocksByTicker(
            String commaSeparatedCodes,
            WatchlistRepository repository,
            Logger log,
            String callerName) {

        Map<String, Stock> uniqueStocks = new LinkedHashMap<>();
        if (commaSeparatedCodes == null || commaSeparatedCodes.isBlank()) {
            return List.of();
        }
        for (String code : commaSeparatedCodes.split(",")) {
            String trimmed = code.trim();
            if (trimmed.isEmpty()) continue;
            repository.findByCodeWithStocks(trimmed)
                .ifPresentOrElse(
                    w -> collectFromWatchlist(w.getStocks(), uniqueStocks),
                    () -> log.warn("{}: watchlist not found: {}", callerName, trimmed)
                );
        }
            return List.copyOf(uniqueStocks.values());
    }

    private static void collectFromWatchlist(Collection<Stock> stocks, Map<String, Stock> uniqueStocks) {
        if (stocks == null) return;
        for (Stock s : stocks) {
            String ticker = s.getTicker();
            if (ticker != null && !ticker.isBlank()) {
                uniqueStocks.putIfAbsent(ticker.trim().toUpperCase(), s);
            }
        }
    }
}
