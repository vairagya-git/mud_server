package com.rama.mudstock.scheduler.analyst;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.facade.AnalystRatingFacade;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.WatchlistUtil;

@Component
@Profile("cronjob")
public class DailyAnalystRatingUpdateCronjob {

    private static final Logger log = LoggerFactory.getLogger(DailyAnalystRatingUpdateCronjob.class);

    private final AnalystRatingFacade analystRatingFacade;
    private final WatchlistRepository watchlistRepository;
    private final SystemConfigService systemConfigService;

    public DailyAnalystRatingUpdateCronjob(AnalystRatingFacade analystRatingFacade,
                                           WatchlistRepository watchlistRepository,
                                           SystemConfigService systemConfigService) {
        this.analystRatingFacade = analystRatingFacade;
        this.watchlistRepository = watchlistRepository;
        this.systemConfigService = systemConfigService;
    }

    @Scheduled(cron = "${dailyAnalystRatingUpdate.cron}")
    public void run() {
        boolean enabled = systemConfigService
            .findByPurposeAndCode(
                SystemConfigEnum.DAILY_ANALYST_RATING_ENABLED.getPurpose(),
                SystemConfigEnum.DAILY_ANALYST_RATING_ENABLED.getCode())
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(Boolean.FALSE);

        if (!enabled) {
            log.info("DailyAnalystRatingUpdateCronjob: disabled by system_config (purpose={}, code={})",
                SystemConfigEnum.DAILY_ANALYST_RATING_ENABLED.getPurpose(),
                SystemConfigEnum.DAILY_ANALYST_RATING_ENABLED.getCode());
            return;
        }

        List<String> watchlistCodeList = systemConfigService
            .findByPurposeAndCode(
                SystemConfigEnum.DAILY_ANALYST_RATING_WATCHLIST_CODES.getPurpose(),
                SystemConfigEnum.DAILY_ANALYST_RATING_WATCHLIST_CODES.getCode())
            .filter(List.class::isInstance)
            .map(v -> ((List<?>) v).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList())
            .orElse(List.of());

        String watchlistCodes = String.join(",", watchlistCodeList);
        log.info("DailyAnalystRatingUpdateCronjob: starting for watchlist-codes=[{}]", watchlistCodes);

        Optional<Object> ratingDateOpt = systemConfigService.findByPurposeAndCode(
            SystemConfigEnum.DAILY_ANALYST_RATING_DATE.getPurpose(),
            SystemConfigEnum.DAILY_ANALYST_RATING_DATE.getCode());
        if (ratingDateOpt.isEmpty()) {
            log.warn("DailyAnalystRatingUpdateCronjob: system_config not found for purpose='{}', code='{}'; skipping",
                SystemConfigEnum.DAILY_ANALYST_RATING_DATE.getPurpose(),
                SystemConfigEnum.DAILY_ANALYST_RATING_DATE.getCode());
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
        boolean updated = systemConfigService.updateValue(SystemConfigEnum.DAILY_ANALYST_RATING_DATE.getCode(), yesterday);
        if (updated) {
            log.info("DailyAnalystRatingUpdateCronjob: updated system_config '{}' to {}",
                    SystemConfigEnum.DAILY_ANALYST_RATING_DATE.getCode(), yesterday);
        }
    }
}

