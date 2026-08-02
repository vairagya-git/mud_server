package com.rama.mudstock.scheduler.daystock;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.facade.DayStockMovementFacade;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.SystemConfigService;

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
}
//Changed For Git