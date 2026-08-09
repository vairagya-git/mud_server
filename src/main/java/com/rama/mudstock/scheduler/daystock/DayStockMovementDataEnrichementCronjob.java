package com.rama.mudstock.scheduler.daystock;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.DayStockMovementDataEnrichementService;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class DayStockMovementDataEnrichementCronjob extends AbstractCronjob {

    private final Logger log = LoggerFactory.getLogger(DayStockMovementDataEnrichementCronjob.class);
    private final WatchlistRepository watchlistRepository;
    private final DayStockMovementDataEnrichementService dayStockMovementDataEnrichementService;

    public DayStockMovementDataEnrichementCronjob(WatchlistRepository watchlistRepository,
                                                  DayStockMovementDataEnrichementService dayStockMovementDataEnrichementService,
                                                  MarketCalendarService marketCalendarService,
                                                  SystemConfigService systemConfigService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_DATA_ENRICHEMENT.value(), marketCalendarService);
        this.watchlistRepository = watchlistRepository;
        this.dayStockMovementDataEnrichementService = dayStockMovementDataEnrichementService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void enrichDayStockMovementData() {
        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        List<String> watchlistCodes = resolveConfiguredWatchlistCodes(getPurpose(), CronjobConfigEnum.WATCHLIST_CODES.code());
        if (watchlistCodes.isEmpty()) {
            log.warn("{}: watchlist-codes is empty; skipping stub execution", getPurpose());
            return;
        }

        String watchlistCodesCsv = String.join(",", watchlistCodes);
        var uniqueStocks = collectUniqueStocksByTicker(getPurpose(), watchlistCodesCsv, watchlistRepository);
        if (uniqueStocks.isEmpty()) {
            log.warn("{}: no stocks found across watchlist-codes=[{}]", getPurpose(), watchlistCodesCsv);
            return;
        }

        LocalDate targetDate = resolveValidTargetDate(getPurpose());
        if (targetDate == null) {
            return;
        }

        List<Long> stockIds = uniqueStocks.stream()
            .map(com.rama.mudstock.model.stockwatchlist.Stock::getId)
            .filter(java.util.Objects::nonNull)
            .toList();

        if (stockIds.isEmpty()) {
            log.warn("{}: resolved stocks have no ids; skipping enrichment", getPurpose());
            return;
        }

        int updated = dayStockMovementDataEnrichementService.enrichPriceMatchDateTimes(stockIds, targetDate);

        log.info("{}: enriched snapshot/flatfile high-low datetime fields for date={} watchlistStocks={} updatedRowCount={}",
            getPurpose(),
            targetDate,
            uniqueStocks.size(),
            updated);

        updateLastUpdatedNowUtc(getPurpose());
        updateDailyDateToNextEligible(getPurpose(), targetDate);
    }
}
