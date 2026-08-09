package com.rama.mudstock.scheduler.daystock;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.config.ApplicationConfig;
import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.DayStockMovementFacade;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.TypeConverstionUtil;

@Component
@Profile("cronjob")
public class DayStockMovementCronjob extends AbstractCronjob {
    private final DayStockMovementFacade dayStockMovementFacade;
    private final WatchlistRepository watchlistRepository;
    private final Logger log = LoggerFactory.getLogger(DayStockMovementCronjob.class);

    public DayStockMovementCronjob(DayStockMovementFacade dayStockMovementFacade,
                                   WatchlistRepository watchlistRepository,
                                   MarketCalendarService marketCalendarService,
                                   SystemConfigService systemConfigService) {
        super(systemConfigService, CronjobConfigEnum.Purpose.DAY_STOCK_MOVEMENT_DATA.value(), marketCalendarService);
        this.dayStockMovementFacade = dayStockMovementFacade;
        this.watchlistRepository = watchlistRepository;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void pollDayStockMovementMappings() {
        String watchlistCodes = String.join(",", resolveConfiguredWatchlistCodes(getPurpose(), CronjobConfigEnum.WATCHLIST_CODES.code()));

        if (!shouldExecuteBySchedule(getPurpose())) {
            return;
        }

        List<Stock> uniqueStocks = collectUniqueStocksByTicker(getPurpose(), watchlistCodes, watchlistRepository);
        if (uniqueStocks.isEmpty()) {
            log.warn("{}: no stocks found across watchlist-codes=[{}]", getPurpose(), watchlistCodes);
            return;
        }

        handleForceExecuteHistoryPull(uniqueStocks);

        LocalDate targetDate = resolveValidTargetDate(getPurpose());
        if (targetDate == null) {
            return;
        }

        log.info("{}: polling for watchlist-based day-stock-movement aggregates for {} unique stock(s) on date={} from watchlist-codes=[{}]",
            getPurpose(),
            uniqueStocks.size(),
            targetDate,
            watchlistCodes);
        try {
            dayStockMovementFacade.fetchAggregatesForWatchlist(uniqueStocks, targetDate);
            updateLastUpdatedNowUtc(getPurpose());
            updateDailyDateToNextEligible(getPurpose(), targetDate);
        } catch (Exception ex) {
            log.error("{}: error while fetching aggregates", getPurpose(), ex);
        }
    }

    private void runForceExecuteHistoryPull(List<Stock> uniqueStocks,
                                            List<String> pullStockHistoryTickers) {
        List<Stock> filteredStocks = uniqueStocks.stream()
            .filter(stock -> stock != null
                && stock.getTicker() != null
                && pullStockHistoryTickers.contains(stock.getTicker().trim().toUpperCase()))
            .toList();

        if (filteredStocks.isEmpty()) {
            log.warn("{}: pullStockHistory is configured but none of the tickers matched watchlist stocks. configuredTickers={}",
                getPurpose(),
                pullStockHistoryTickers);
            return;
        }

        Integer configuredDays = TypeConverstionUtil.toInteger(getConfigValue(CronjobConfigEnum.PULL_STOCK_HISTORY_DAYS.code()));
        int historyDays = configuredDays == null || configuredDays < 0 ? 0 : configuredDays;

        LocalDate endDate = LocalDate.now(ApplicationConfig.LISBON);
        LocalDate startDate = endDate.minusDays(historyDays);

        log.info("{}: forceExecute history pull enabled for {} stock(s), days={}, startDate={}, endDate={}, configuredTickers={}",
            getPurpose(),
            filteredStocks.size(),
            historyDays,
            startDate,
            endDate,
            pullStockHistoryTickers);

        try {
            for (LocalDate targetDate = startDate; !targetDate.isAfter(endDate); targetDate = targetDate.plusDays(1)) {
                dayStockMovementFacade.fetchAggregatesForWatchlist(filteredStocks, targetDate);
            }
        } catch (Exception ex) {
            log.error("{}: error while running forceExecute history pull", getPurpose(), ex);
        }
    }

    private boolean handleForceExecuteHistoryPull(List<Stock> uniqueStocks) {
        boolean forceExecuteEnabled = isForceExecuteEnabled(getPurpose());
        List<String> pullStockHistoryTickers = resolvePullStockHistoryTickers();
        if (!forceExecuteEnabled || pullStockHistoryTickers.isEmpty()) {
            return false;
        }
        runForceExecuteHistoryPull(uniqueStocks, pullStockHistoryTickers);
        return true;
    }

    private List<String> resolvePullStockHistoryTickers() {
        Object configured = getConfigValue(CronjobConfigEnum.PULL_STOCK_HISTORY.code());
        if (!(configured instanceof List<?> configuredList)) {
            return List.of();
        }

        Set<String> tickers = new LinkedHashSet<>();
        for (Object value : configuredList) {
            String ticker = TypeConverstionUtil.toString(value).toUpperCase();
            if (!ticker.isBlank()) {
                tickers.add(ticker);
            }
        }
        return List.copyOf(tickers);
    }
}