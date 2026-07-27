package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.earnings.EarningsDate;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.daystock.DayStockMovementEntryRepository;
import com.rama.mudstock.repository.earnings.EarningsDateRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;
import com.rama.mudstock.service.DayStockMovementAggregateParser;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.MassiveRestStockService;
import com.rama.mudstock.util.TypeConverstionUtil;

@Service
public class DayStockMovementFacade {

    private static final Logger log = LoggerFactory.getLogger(DayStockMovementFacade.class);

    private final MassiveRestStockService massiveRestStockService;
    private final DayStockMovementEntryRepository dayStockMovementEntryRepository;
    private final EarningsDateRepository earningsDateRepository;
    private final StockRepository stockRepository;
    private final MarketCalendarService marketCalendarService;
    private final DayStockMovementAggregateParser aggregateParser;

    public DayStockMovementFacade(MassiveRestStockService massiveRestStockService,
                                  DayStockMovementEntryRepository dayStockMovementEntryRepository,
                                  EarningsDateRepository earningsDateRepository,
                                  StockRepository stockRepository,
                                  MarketCalendarService marketCalendarService,
                                  DayStockMovementAggregateParser aggregateParser) {
        this.massiveRestStockService = massiveRestStockService;
        this.dayStockMovementEntryRepository = dayStockMovementEntryRepository;
        this.earningsDateRepository = earningsDateRepository;
        this.stockRepository = stockRepository;
        this.marketCalendarService = marketCalendarService;
        this.aggregateParser = aggregateParser;
    }

    public void fetchAggregatesForPastEarningsWindow() {
        List<Map<String, Object>> pastEntries = earningsDateRepository.listPastWindowCandidatesWithTicker();
        if (pastEntries == null || pastEntries.isEmpty()) {
            log.info("No earnings_date rows with status 'PAST' or 'PROCESSING' found.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate maxProcessDate = today.minusDays(1);

        for (Map<String, Object> entry : pastEntries) {
            Long earningsDateId = TypeConverstionUtil.toLong(entry.get("id"));
            String ticker = TypeConverstionUtil.toString(entry.get("ticker"));
            LocalDate earningsDate = TypeConverstionUtil.toLocalDate(entry.get("earnings_date"));
            LocalDate processedTill = TypeConverstionUtil.toLocalDate(entry.get("processed_till"));
            String statusRaw = TypeConverstionUtil.toString(entry.get("status"));
            Integer noOfDays = TypeConverstionUtil.toInteger(entry.get("no_of_days"));
            int windowDays = (noOfDays == null || noOfDays <= 0) ? 10 : noOfDays;

            if (earningsDateId == null || ticker == null || ticker.isBlank() || earningsDate == null) {
                log.warn("Skipping earnings_date row due to missing ticker/date: {}", entry);
                continue;
            }

            LocalDate windowStart = earningsDate.minusDays(windowDays);
            LocalDate windowEnd = earningsDate.plusDays(windowDays);
            LocalDate effectiveEnd = windowEnd.isAfter(maxProcessDate) ? maxProcessDate : windowEnd;

            LocalDate startDate = processedTill == null ? windowStart : processedTill.plusDays(1);
            if (startDate.isBefore(windowStart)) {
                startDate = windowStart;
            }

            if (startDate.isAfter(windowEnd)) {
                updateStatusIfChanged(earningsDateId, statusRaw, EarningsDate.Status.PROCESSED);
                log.info("PAST window already fully processed for earningsDateId={} ticker={} (processedTill={}, windowEnd={})",
                    earningsDateId, ticker, processedTill, windowEnd);
                continue;
            }

            LocalDate latestProcessed = processedTill;
            for (LocalDate eventDate = startDate; !eventDate.isAfter(effectiveEnd); eventDate = eventDate.plusDays(1)) {
                processEarningsDateWindow(ticker.trim().toUpperCase(), eventDate, earningsDate, earningsDateId, windowDays);
                latestProcessed = eventDate;
            }

            if (latestProcessed != null && !latestProcessed.equals(processedTill)) {
                earningsDateRepository.updateProcessedTill(earningsDateId, latestProcessed);
                log.info("Updated earnings_date id={} processed_till={} (ticker={}, baseEarningsDate={})",
                    earningsDateId, latestProcessed, ticker, earningsDate);
            }

            boolean isComplete = latestProcessed != null && !latestProcessed.isBefore(windowEnd);
            EarningsDate.Status targetStatus = isComplete ? EarningsDate.Status.PROCESSED : EarningsDate.Status.PROCESSING;
            updateStatusIfChanged(earningsDateId, statusRaw, targetStatus);
        }
    }

    private void updateStatusIfChanged(Long earningsDateId, String currentStatusRaw, EarningsDate.Status targetStatus) {
        EarningsDate.Status currentStatus = null;
        if (currentStatusRaw != null) {
            try {
                currentStatus = EarningsDate.Status.valueOf(currentStatusRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown earnings_date status '{}' for id={}; forcing status update to {}",
                    currentStatusRaw, earningsDateId, targetStatus);
            }
        }

        if (currentStatus != targetStatus) {
            earningsDateRepository.updateStatus(earningsDateId, targetStatus);
            log.info("Updated earnings_date id={} status {} -> {}", earningsDateId, currentStatus, targetStatus);
        }
    }

    public void fetchAggregatesForWatchlist(List<Stock> allowedStocks, LocalDate targetDate) {
        if (allowedStocks == null || allowedStocks.isEmpty()) {
            log.info("No allowed stocks configured for day-stock movement watchlist fetch.");
            return;
        }

        log.info("Fetching day-stock movement aggregates for {} stock(s) on date={}", allowedStocks.size(), targetDate);
        for (Stock stock : allowedStocks) {
            processWatchlistStock(stock, targetDate);
        }
    }

    private void processWatchlistStock(Stock stock, LocalDate eventDate) {
        try {
            if (stock == null || stock.getTicker() == null || stock.getTicker().isBlank() || eventDate == null) {
                log.warn("Skipping stock with missing ticker or eventDate: {}", stock);
                return;
            }

            Optional<DayStockMovementAggregateParser.AggregateSnapshot> snapshot =
                fetchAggregateSnapshotForEventDate(stock.getTicker().trim().toUpperCase(), eventDate, "watchlist");

            if (snapshot.isEmpty()) {
                log.warn("Could not find both previous and current day bars in aggregate results for stock {} on date {}", stock.getTicker(), eventDate);
                return;
            }

            DayStockMovementAggregateParser.AggregateSnapshot data = snapshot.get();
            dayStockMovementEntryRepository.upsertDayStockMovementEntry(
                stock.getId(),
                null,
                data.dayStockMovementDate(),
                null,
                data.preDayClose(),
                data.curDayOpen(),
                data.curDayClose(),
                data.curDayHigh(),
                data.curDayLow(),
                data.curDayVolWeight(),
                data.curDayVolume(),
                data.changePercent(),
                data.dayOpeningChangePercent());
            log.info("Saved day_stock_movement_entry for stock={} eventDate={}", stock.getTicker(), eventDate);
        } catch (Exception ex) {
            log.error("Failed to fetch aggregate for stock {} on date {}", stock != null ? stock.getTicker() : null, eventDate, ex);
        }
    }

    private LocalDate previousMarketDay(LocalDate date) {
        LocalDate current = date.minusDays(1);
        while (marketCalendarService.isMarketClosed(current)) {
            current = current.minusDays(1);
        }
        return current;
    }

    private void processEarningsDateWindow(String ticker, LocalDate eventDate, LocalDate baseEarningsDate, Long earningsDateId, int windowDays) {
        try {
            Optional<DayStockMovementAggregateParser.AggregateSnapshot> snapshot =
                fetchAggregateSnapshotForEventDate(ticker, eventDate, "past-earnings-window");

            if (snapshot.isEmpty()) {
                log.warn("No aggregate snapshot found for ticker={} eventDate={} (base={}, windowDays={})",
                    ticker, eventDate, baseEarningsDate, windowDays);
                return;
            }

            DayStockMovementAggregateParser.AggregateSnapshot data = snapshot.get();
            Long stockId = stockRepository.findByTicker(ticker)
                .map(stock -> stock.getId())
                .orElse(null);

            if (stockId == null) {
                log.warn("Missing stock_id for ticker={} eventDate={} (base={}, windowDays={})",
                    ticker, eventDate, baseEarningsDate, windowDays);
                return;
            }

            boolean earnings = true;
            dayStockMovementEntryRepository.insertEarningsEntry(
                stockId,
                data.dayStockMovementDate(),
                earningsDateId,
                data.preDayClose(),
                data.curDayOpen(),
                data.curDayClose(),
                data.curDayHigh(),
                data.curDayLow(),
                data.curDayVolWeight(),
                data.curDayVolume(),
                data.changePercent(),
                data.dayOpeningChangePercent(),
                earnings);

            log.info("Computed aggregate snapshot for ticker={} eventDate={} (base={}, windowDays={}): changePercent={} dayOpeningChangePercent={}",
                ticker,
                eventDate,
                baseEarningsDate,
                windowDays,
                data.changePercent(),
                data.dayOpeningChangePercent());
        } catch (Exception ex) {
            log.error("Failed processing PAST earnings window ticker={} eventDate={} (base={}, windowDays={})",
                ticker, eventDate, baseEarningsDate, windowDays, ex);
        }
    }

    private Optional<DayStockMovementAggregateParser.AggregateSnapshot> fetchAggregateSnapshotForEventDate(String ticker,
                                                                                                            LocalDate eventDate,
                                                                                                            String context) throws Exception {
        if (marketCalendarService.isMarketClosed(eventDate)) {
            log.debug("Skipping closed market day [{}] ticker={} eventDate={}", context, ticker, eventDate);
            return Optional.empty();
        }

        LocalDate previousMarketDate = previousMarketDay(eventDate);
        String responseBody = massiveRestStockService.fetchTickerAggregate(ticker, previousMarketDate, eventDate);
        int resultsCount = aggregateParser.extractResultsCount(responseBody);
        log.info("Ticker aggregate [{}] for {} from {} to {}: resultsCount={}, payload={}",
            context, ticker, previousMarketDate, eventDate, resultsCount, responseBody);

        if (resultsCount == 1) {
            LocalDate earlierMarketDate = previousMarketDay(previousMarketDate);
            String retryResponseBody = massiveRestStockService.fetchTickerAggregate(ticker, earlierMarketDate, eventDate);
            int retryResultsCount = aggregateParser.extractResultsCount(retryResponseBody);
            log.info("(Retry) Ticker aggregate [{}] for {} from {} to {}: resultsCount={}, payload={}",
                context, ticker, earlierMarketDate, eventDate, retryResultsCount, retryResponseBody);
            if (retryResultsCount > 0) {
                responseBody = retryResponseBody;
            }
        }

        return aggregateParser.parseAggregate(responseBody, eventDate, previousMarketDate);
    }
}

//Changed For Git