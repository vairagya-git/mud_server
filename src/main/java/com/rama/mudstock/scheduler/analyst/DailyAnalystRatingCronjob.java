package com.rama.mudstock.scheduler.analyst;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.AnalystRatingFacade;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class DailyAnalystRatingCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(DailyAnalystRatingCronjob.class);

    private final AnalystRatingFacade analystRatingFacade;
    private final WatchlistRepository watchlistRepository;

    public DailyAnalystRatingCronjob(AnalystRatingFacade analystRatingFacade,
                                     WatchlistRepository watchlistRepository,
                                     SystemConfigService systemConfigService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.DAILY_ANALYST_RATING_CRONJOB.value());
        this.analystRatingFacade = analystRatingFacade;
        this.watchlistRepository = watchlistRepository;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void run() {
        String watchlistCode = CronjobConfigEnum.WATCHLIST_CODES.code();

        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        var watchlistCodeList = resolveConfiguredWatchlistCodes(getPurpose(), watchlistCode);

        String watchlistCodes = String.join(",", watchlistCodeList);
        log.info("{}: starting for watchlist-codes=[{}]", getPurpose(), watchlistCodes);

        LocalDate ratingDate = resolveExecutionDate(getPurpose());
        if (ratingDate == null) {
            log.error("{}: ratingDate is required (forceExecute enabled but date not configured), skipping run", getPurpose());
            return;
        }
        String ratingDateStr = ratingDate.toString();
        log.info("{}: using rating date={}", getPurpose(), ratingDateStr);

        List<Stock> uniqueStocks = collectUniqueStocksByTicker(getPurpose(), watchlistCodes, watchlistRepository);

        if (uniqueStocks.isEmpty()) {
            log.warn("{}: no stocks found across watchlist-codes=[{}]", getPurpose(), watchlistCodes);
            return;
        }

        log.info("{}: processing {} unique stock(s)", getPurpose(), uniqueStocks.size());
        int totalSaved = 0;
        for (Stock stock : uniqueStocks) {
            String ticker = stock.getTicker();
            try {
                int saved = analystRatingFacade.fetchAndSaveForTicker(ticker, ratingDateStr);
                log.info("{}: ticker={} saved={} rating(s)", getPurpose(), ticker, saved);
                totalSaved += saved;
            } catch (Exception ex) {
                log.error("{}: error processing ticker={}: {}", getPurpose(), ticker, ex.getMessage());
            }
        }

        log.info("{}: done - total ratings saved={}", getPurpose(), totalSaved);
        updateLastUpdatedNowUtc(getPurpose());
    }
}
