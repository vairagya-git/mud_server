package com.rama.mudstock.scheduler.analyst;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.facade.AnalystRatingFacade;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.model.stockwatchlist.Watchlist;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;

@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "dailyAnalystRatingUpdate", name = "enabled", havingValue = "true")
public class DailyAnalystRatingUpdateCronjob {

    private static final Logger log = LoggerFactory.getLogger(DailyAnalystRatingUpdateCronjob.class);

    private final AnalystRatingFacade analystRatingFacade;
    private final WatchlistRepository watchlistRepository;

    @Value("${dailyAnalystRatingUpdate.watchlist-code}")
    private String watchlistCode;

    public DailyAnalystRatingUpdateCronjob(AnalystRatingFacade analystRatingFacade,
                                           WatchlistRepository watchlistRepository) {
        this.analystRatingFacade = analystRatingFacade;
        this.watchlistRepository = watchlistRepository;
    }

    @Scheduled(cron = "${dailyAnalystRatingUpdate.cron}")
    public void run() {
        log.info("DailyAnalystRatingUpdateCronjob: starting for watchlist-code={}", watchlistCode);

        Optional<Watchlist> watchlistOpt = watchlistRepository.findByCodeWithStocks(watchlistCode);
        if (watchlistOpt.isEmpty()) {
            log.warn("DailyAnalystRatingUpdateCronjob: watchlist not found for code={}", watchlistCode);
            return;
        }

        Set<Stock> stocks = watchlistOpt.get().getStocks();
        if (stocks == null || stocks.isEmpty()) {
            log.warn("DailyAnalystRatingUpdateCronjob: no stocks in watchlist code={}", watchlistCode);
            return;
        }

        log.info("DailyAnalystRatingUpdateCronjob: processing {} stock(s)", stocks.size());
        int totalSaved = 0;
        for (Stock stock : stocks) {
            String ticker = stock.getTicker();
            if (ticker == null || ticker.isBlank()) {
                log.warn("DailyAnalystRatingUpdateCronjob: skipping stock id={} with blank ticker", stock.getId());
                continue;
            }
            try {
                int saved = analystRatingFacade.fetchAndSaveForTicker(ticker);
                log.info("DailyAnalystRatingUpdateCronjob: ticker={} saved={} rating(s)", ticker, saved);
                totalSaved += saved;
            } catch (Exception ex) {
                log.error("DailyAnalystRatingUpdateCronjob: error processing ticker={}: {}", ticker, ex.getMessage());
            }
        }
        log.info("DailyAnalystRatingUpdateCronjob: done — total ratings saved={}", totalSaved);
    }
}
