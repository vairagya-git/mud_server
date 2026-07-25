package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.earnings.EarningsDate;
import com.rama.mudstock.repository.daystock.DayStockMovementEntryRepository;
import com.rama.mudstock.repository.daystock.DayStockMovementMapRepository;
import com.rama.mudstock.repository.earnings.EarningsDateRepository;
import com.rama.mudstock.service.DayStockMovementAggregateParser;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.MassiveRestStockService;
import com.rama.mudstock.util.TypeConverstionUtil;

@Service
public class DayStockMovementFacade {

    private static final Logger log = LoggerFactory.getLogger(DayStockMovementFacade.class);

    private final MassiveRestStockService massiveRestStockService;
    private final DayStockMovementMapRepository mappingRepository;
    private final DayStockMovementEntryRepository dayStockMovementEntryRepository;
    private final EarningsDateRepository earningsDateRepository;
    private final MarketCalendarService marketCalendarService;
    private final DayStockMovementAggregateParser aggregateParser;

    public DayStockMovementFacade(MassiveRestStockService massiveRestStockService,
                                  DayStockMovementMapRepository mappingRepository,
                                  DayStockMovementEntryRepository dayStockMovementEntryRepository,
                                  EarningsDateRepository earningsDateRepository,
                                  MarketCalendarService marketCalendarService,
                                  DayStockMovementAggregateParser aggregateParser) {
        this.massiveRestStockService = massiveRestStockService;
        this.mappingRepository = mappingRepository;
        this.dayStockMovementEntryRepository = dayStockMovementEntryRepository;
        this.earningsDateRepository = earningsDateRepository;
        this.marketCalendarService = marketCalendarService;
        this.aggregateParser = aggregateParser;
    }

    public void fetchAggregatesForPastEarningsWindow() {
        List<Map<String, Object>> pastEntries = earningsDateRepository.listByStatusWithTicker(EarningsDate.Status.PAST);
        if (pastEntries == null || pastEntries.isEmpty()) {
            log.info("No earnings_date rows with status 'PAST' found.");
            return;
        }

        for (Map<String, Object> entry : pastEntries) {
            String ticker = TypeConverstionUtil.toString(entry.get("ticker"));
            LocalDate earningsDate = TypeConverstionUtil.toLocalDate(entry.get("earnings_date"));
            Integer noOfDays = TypeConverstionUtil.toInteger(entry.get("no_of_days"));
            int windowDays = (noOfDays == null || noOfDays <= 0) ? 10 : noOfDays;

            if (ticker == null || ticker.isBlank() || earningsDate == null) {
                log.warn("Skipping earnings_date row due to missing ticker/date: {}", entry);
                continue;
            }

            for (int offset = -windowDays; offset <= windowDays; offset++) {
                LocalDate eventDate = earningsDate.plusDays(offset);
                processEarningsDateWindow(ticker.trim().toUpperCase(), eventDate, earningsDate, windowDays);
            }
        }
    }

    public void fetchAggregatesForNewMappings() {
        List<Map<String, Object>> mappings = mappingRepository.listMappingsByStatus("new");
        if (mappings == null || mappings.isEmpty()) {
            log.info("No day-event mappings with status 'new' found.");
            return;
        }

        for (Map<String, Object> mapping : mappings) {
            processMapping(mapping);
        }
    }

    private void processMapping(Map<String, Object> mapping) {
        try {
            String ticker = (String) mapping.get("ticker");
            LocalDate eventDate = TypeConverstionUtil.toLocalDate(mapping.get("date"));
            if (ticker == null || eventDate == null) {
                log.warn("Skipping mapping with missing ticker or eventDate: {}", mapping);
                return;
            }

            Long mappingId = TypeConverstionUtil.toLong(mapping.get("map_id"));
            Optional<DayStockMovementAggregateParser.AggregateSnapshot> snapshot =
                fetchAggregateSnapshotForEventDate(ticker, eventDate, "mapping");

            if (snapshot.isEmpty()) {
                if (mappingId != null && marketCalendarService.isMarketClosed(eventDate)) {
                    mappingRepository.updateStatus(mappingId, "MARKET_CLOSED");
                    log.info("Marked mapping id={} as MARKET_CLOSED for date={} (weekend or holiday)", mappingId, eventDate);
                }
                log.warn("Could not find both previous and current day bars in aggregate results for mapping {}", mapping);
                return;
            }

            if (mappingId == null) {
                log.warn("Missing map_id to save day_stock_movement_entry for mapping {}", mapping);
                return;
            }

            DayStockMovementAggregateParser.AggregateSnapshot data = snapshot.get();
            dayStockMovementEntryRepository.upsertDayStockMovementEntry(
                mappingId,
                data.preDayClose(),
                data.curDayOpen(),
                data.curDayClose(),
                data.curDayHigh(),
                data.curDayLow(),
                data.curDayVolWeight(),
                data.curDayVolume(),
                data.changePercent(),
                data.dayOpeningChangePercent());
            log.info("Saved day_stock_movement_entry for mappingId={} eventDate={}", mappingId, eventDate);

            try {
                int updated = mappingRepository.updateStatus(mappingId, "processed");
                if (updated > 0) {
                    log.info("Marked day_stock_movement_map id={} as processed", mappingId);
                } else {
                    log.warn("No day_stock_movement_map row updated for id={}", mappingId);
                }
            } catch (Exception ex) {
                log.error("Failed to update day_stock_movement_map status for id={}", mappingId, ex);
            }
        } catch (Exception ex) {
            log.error("Failed to fetch aggregate for mapping {}", mapping, ex);
        }
    }

    private LocalDate previousMarketDay(LocalDate date) {
        LocalDate current = date.minusDays(1);
        while (marketCalendarService.isMarketClosed(current)) {
            current = current.minusDays(1);
        }
        return current;
    }

    private void processEarningsDateWindow(String ticker, LocalDate eventDate, LocalDate baseEarningsDate, int windowDays) {
        try {
            Optional<DayStockMovementAggregateParser.AggregateSnapshot> snapshot =
                fetchAggregateSnapshotForEventDate(ticker, eventDate, "past-earnings-window");

            if (snapshot.isEmpty()) {
                log.warn("No aggregate snapshot found for ticker={} eventDate={} (base={}, windowDays={})",
                    ticker, eventDate, baseEarningsDate, windowDays);
                return;
            }

            DayStockMovementAggregateParser.AggregateSnapshot data = snapshot.get();
            boolean earnings = true;
            dayStockMovementEntryRepository.insertEarningsEntry(
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