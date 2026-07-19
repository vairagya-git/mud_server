package com.rama.mudstock.scheduler.analyst;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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
import com.rama.mudstock.util.WatchlistUtil;

@Component
@Profile("cronjob")
public class DailyAnalystRatingCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(DailyAnalystRatingCronjob.class);

    private final AnalystRatingFacade analystRatingFacade;
    private final WatchlistRepository watchlistRepository;
    private final SystemConfigService systemConfigService;

    public DailyAnalystRatingCronjob(AnalystRatingFacade analystRatingFacade,
                                     WatchlistRepository watchlistRepository,
                                     SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.analystRatingFacade = analystRatingFacade;
        this.watchlistRepository = watchlistRepository;
        this.systemConfigService = systemConfigService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void run() {
        String purpose = CronjobConfigEnum.Purpose.DAILY_ANALYST_RATING_CRONJOB.value();
        String watchlistCode = CronjobConfigEnum.WATCHLIST_CODES.code();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        List<String> watchlistCodeList = systemConfigService
            .findByPurposeAndCode(
                purpose,
                watchlistCode)
            .filter(List.class::isInstance)
            .map(v -> ((List<?>) v).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList())
            .orElse(List.of());

        String watchlistCodes = String.join(",", watchlistCodeList);
        log.info("{}: starting for watchlist-codes=[{}]", purpose, watchlistCodes);

        String lastUpdatedRaw = resolveStringValue(purpose, lastUpdatedCode());
        LocalDate ratingDate = resolveRatingDateFromLastUpdated(lastUpdatedRaw);
        String ratingDateStr = ratingDate.toString();
        log.info("{}: using rating date={}", purpose, ratingDateStr);

        Map<String, Stock> uniqueStocks = WatchlistUtil.collectUniqueStocksByTicker(
            watchlistCodes, watchlistRepository, log, purpose);

        if (uniqueStocks.isEmpty()) {
            log.warn("{}: no stocks found across watchlist-codes=[{}]", purpose, watchlistCodes);
            return;
        }

        log.info("{}: processing {} unique stock(s)", purpose, uniqueStocks.size());
        int totalSaved = 0;
        for (Stock stock : uniqueStocks.values()) {
            String ticker = stock.getTicker();
            try {
                int saved = analystRatingFacade.fetchAndSaveForTicker(ticker, ratingDateStr);
                log.info("{}: ticker={} saved={} rating(s)", purpose, ticker, saved);
                totalSaved += saved;
            } catch (Exception ex) {
                log.error("{}: error processing ticker={}: {}", purpose, ticker, ex.getMessage());
            }
        }

        log.info("{}: done - total ratings saved={}", purpose, totalSaved);
        updateLastUpdatedNowUtc(purpose);
    }

    private LocalDate resolveRatingDateFromLastUpdated(String rawLastUpdated) {
        if (rawLastUpdated == null || rawLastUpdated.isBlank()) {
            return LocalDate.now(com.rama.mudstock.config.ApplicationConfig.LISBON).minusDays(1);
        }

        String value = rawLastUpdated.trim();
        try {
            return Instant.parse(value).atZone(com.rama.mudstock.config.ApplicationConfig.LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(com.rama.mudstock.config.ApplicationConfig.LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(com.rama.mudstock.config.ApplicationConfig.LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
        } catch (Exception ignored) {
            return LocalDate.now(com.rama.mudstock.config.ApplicationConfig.LISBON).minusDays(1);
        }
    }
}
