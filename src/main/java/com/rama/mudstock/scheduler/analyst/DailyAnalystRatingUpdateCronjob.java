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

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.facade.AnalystRatingFacade;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.WatchlistUtil;

@Component
@Profile("cronjob")
public class DailyAnalystRatingUpdateCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(DailyAnalystRatingUpdateCronjob.class);

    private final AnalystRatingFacade analystRatingFacade;
    private final WatchlistRepository watchlistRepository;
    private final SystemConfigService systemConfigService;

    public DailyAnalystRatingUpdateCronjob(AnalystRatingFacade analystRatingFacade,
                                           WatchlistRepository watchlistRepository,
                                           SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.analystRatingFacade = analystRatingFacade;
        this.watchlistRepository = watchlistRepository;
        this.systemConfigService = systemConfigService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void run() {
        var enabledCfg = SystemConfigEnum.DailyAnalystRatingCronjob.ENABLED;
        var cronCfg = SystemConfigEnum.DailyAnalystRatingCronjob.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.DailyAnalystRatingCronjob.LAST_UPDATED;
        var watchlistCfg = SystemConfigEnum.DailyAnalystRatingCronjob.WATCHLIST_CODES;
        String purpose = enabledCfg.purpose();

        boolean enabled = isEnabled(purpose, enabledCfg.code());

        if (!enabled) {
            log.info("DailyAnalystRatingUpdateCronjob: disabled by system_config (purpose={}, code={})",
                purpose,
                enabledCfg.code());
            return;
        }

        if (!shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            LISBON)) {
            return;
        }

        List<String> watchlistCodeList = systemConfigService
            .findByPurposeAndCode(
                watchlistCfg.purpose(),
                watchlistCfg.code())
            .filter(List.class::isInstance)
            .map(v -> ((List<?>) v).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList())
            .orElse(List.of());

        String watchlistCodes = String.join(",", watchlistCodeList);
        log.info("DailyAnalystRatingUpdateCronjob: starting for watchlist-codes=[{}]", watchlistCodes);

        String lastUpdatedRaw = resolveLastUpdated(purpose, lastUpdatedCfg.code());
        LocalDate ratingDate = resolveRatingDateFromLastUpdated(lastUpdatedRaw);
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
        updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
    }

    private LocalDate resolveRatingDateFromLastUpdated(String rawLastUpdated) {
        if (rawLastUpdated == null || rawLastUpdated.isBlank()) {
            return LocalDate.now(LISBON).minusDays(1);
        }

        String value = rawLastUpdated.trim();
        try {
            return Instant.parse(value).atZone(LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
        } catch (Exception ignored) {
            return LocalDate.now(LISBON).minusDays(1);
        }
    }
}

