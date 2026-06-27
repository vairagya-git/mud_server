package com.rama.mudstock.scheduler.analyst;

import java.time.LocalDate;
import java.util.Map;
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
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.WatchlistUtil;

@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "dailyAnalystRatingUpdate", name = "enabled", havingValue = "true")
public class DailyAnalystRatingUpdateCronjob {

    private static final Logger log = LoggerFactory.getLogger(DailyAnalystRatingUpdateCronjob.class);

    /** Key used to look up and update the rating date in {@code system_config}. */
    private static final String RATING_DATE_CODE = "benzinga-analyst-rating-date";

    private final AnalystRatingFacade analystRatingFacade;
    private final WatchlistRepository watchlistRepository;
    private final SystemConfigService systemConfigService;

    @Value("${dailyAnalystRatingUpdate.watchlist-codes}")
    private String watchlistCodes;

    public DailyAnalystRatingUpdateCronjob(AnalystRatingFacade analystRatingFacade,
                                           WatchlistRepository watchlistRepository,
                                           SystemConfigService systemConfigService) {
        this.analystRatingFacade = analystRatingFacade;
        this.watchlistRepository = watchlistRepository;
        this.systemConfigService = systemConfigService;
    }

    @Scheduled(cron = "${dailyAnalystRatingUpdate.cron}")
    public void run() {
        log.info("DailyAnalystRatingUpdateCronjob: starting for watchlist-codes=[{}]", watchlistCodes);

        Optional<Object> ratingDateOpt = systemConfigService.findByCode(RATING_DATE_CODE);
        if (ratingDateOpt.isEmpty()) {
            log.warn("DailyAnalystRatingUpdateCronjob: system_config code='{}' not found, skipping", RATING_DATE_CODE);
            return;
        }
        LocalDate ratingDate = (LocalDate) ratingDateOpt.get();
        String ratingDateStr = ratingDate.toString();
        log.info("DailyAnalystRatingUpdateCronjob: using rating date={}", ratingDateStr);

        Map<String, Stock> uniqueStocks = WatchlistUtil.collectUniqueStocksByTicker(
                watchlistCodes, watchlistRepository, log, "DailyAnalystRatingUpdateCronjob");

        if (uniqueStocks.isEmpty()) {
            log.warn("DailyAnalystRatingUpdateCronjob: no stocks found across watchlist-codes=[{}]", watchlistCodes);
            return;
        }

        log.info("DailyAnalystRatingUpdateCronjob: processing {} unique stock(s)", uniqueStocks.size());
        int totalSaved = 0;
        for (Stock stock : uniqueStocks.values()) {
            String ticker = stock.getTicker();
            try {
                int saved = analystRatingFacade.fetchAndSaveForTicker(ticker, ratingDateStr);
                log.info("DailyAnalystRatingUpdateCronjob: ticker={} saved={} rating(s)", ticker, saved);
                totalSaved += saved;
            } catch (Exception ex) {
                log.error("DailyAnalystRatingUpdateCronjob: error processing ticker={}: {}", ticker, ex.getMessage());
            }
        }

        log.info("DailyAnalystRatingUpdateCronjob: done — total ratings saved={}", totalSaved);

        String yesterday = LocalDate.now().minusDays(1).toString();
        boolean updated = systemConfigService.updateValue(RATING_DATE_CODE, yesterday);
        if (updated) {
            log.info("DailyAnalystRatingUpdateCronjob: updated system_config '{}' to {}", RATING_DATE_CODE, yesterday);
        }
    }
}

