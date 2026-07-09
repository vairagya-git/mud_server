package com.rama.mudstock.facade;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.daystock.DayStockMovementEntryRepository;
import com.rama.mudstock.repository.daystock.DayStockMovementMapRepository;
import com.rama.mudstock.service.DayStockMovementAggregateParser;
import com.rama.mudstock.service.MarketCalendarService;
import com.rama.mudstock.service.MassiveRestStockService;
import com.rama.mudstock.util.MudDateUtil;

@Service
public class DayStockMovementFacade {

    private static final Logger log = LoggerFactory.getLogger(DayStockMovementFacade.class);

    private final MassiveRestStockService massiveRestStockService;
    private final DayStockMovementMapRepository mappingRepository;
    private final DayStockMovementEntryRepository dayStockMovementEntryRepository;
    private final MarketCalendarService marketCalendarService;
    private final DayStockMovementAggregateParser aggregateParser;

    public DayStockMovementFacade(MassiveRestStockService massiveRestStockService,
                                  DayStockMovementMapRepository mappingRepository,
                                  DayStockMovementEntryRepository dayStockMovementEntryRepository,
                                  MarketCalendarService marketCalendarService,
                                  DayStockMovementAggregateParser aggregateParser) {
        this.massiveRestStockService = massiveRestStockService;
        this.mappingRepository = mappingRepository;
        this.dayStockMovementEntryRepository = dayStockMovementEntryRepository;
        this.marketCalendarService = marketCalendarService;
        this.aggregateParser = aggregateParser;
    }

    public void fetchAggregatesForNewMappings(String rawCutOffTime, String rawCutOffTimeFormat, ZoneId zoneId) {
        List<Map<String, Object>> mappings = mappingRepository.listMappingsByStatus("new");
        if (mappings == null || mappings.isEmpty()) {
            log.info("No day-event mappings with status 'new' found.");
            return;
        }

        for (Map<String, Object> mapping : mappings) {
            if (!shouldProcessMapping(mapping, rawCutOffTime, rawCutOffTimeFormat, zoneId)) {
                continue;
            }
            processMapping(mapping);
        }
    }

    private boolean shouldProcessMapping(Map<String, Object> mapping,
                                         String rawCutOffTime,
                                         String rawCutOffTimeFormat,
                                         ZoneId zoneId) {
        LocalDate eventDate = toLocalDate(mapping.get("date"));
        if (eventDate == null) {
            log.warn("Skipping mapping with missing eventDate: {}", mapping);
            return false;
        }

        LocalDate currentDate = LocalDate.now(zoneId);
        if (!eventDate.isEqual(currentDate)) {
            return true;
        }

        Optional<LocalTime> cutOffTime = parseCutOffTime(rawCutOffTime, rawCutOffTimeFormat);
        if (cutOffTime.isEmpty()) {
            return true;
        }

        LocalTime now = LocalTime.now(zoneId);
        if (!now.isAfter(cutOffTime.get())) {
            log.info("Skipping current-day mapping until after cutoff time (eventDate={}, cutoffTime={}, mapping={})",
                eventDate, cutOffTime.get(), mapping);
            return false;
        }

        return true;
    }

    private Optional<LocalTime> parseCutOffTime(String rawCutOffTime, String rawCutOffTimeFormat) {
        if (rawCutOffTime == null || rawCutOffTime.isBlank()) {
            return Optional.empty();
        }

        String format = rawCutOffTimeFormat == null || rawCutOffTimeFormat.isBlank() ? "HH:mm" : rawCutOffTimeFormat.trim();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return Optional.of(LocalTime.parse(rawCutOffTime.trim(), formatter));
        } catch (DateTimeException | IllegalArgumentException ex) {
            log.warn("Invalid cutoff time configuration (value={}, format={})", rawCutOffTime, rawCutOffTimeFormat, ex);
            return Optional.empty();
        }
    }

    private void processMapping(Map<String, Object> mapping) {
        try {
            String ticker = (String) mapping.get("ticker");
            LocalDate eventDate = toLocalDate(mapping.get("date"));
            if (ticker == null || eventDate == null) {
                log.warn("Skipping mapping with missing ticker or eventDate: {}", mapping);
                return;
            }

            Long mappingId = toLong(mapping.get("map_id"));
            if (marketCalendarService.isMarketClosed(eventDate)) {
                if (mappingId != null) {
                    mappingRepository.updateStatus(mappingId, "MARKET_CLOSED");
                    log.info("Marked mapping id={} as MARKET_CLOSED for date={} (weekend or holiday)", mappingId, eventDate);
                }
                return;
            }

            LocalDate previousMarketDate = previousMarketDay(eventDate);
            String responseBody = massiveRestStockService.fetchTickerAggregate(ticker, previousMarketDate, eventDate);
            int resultsCount = aggregateParser.extractResultsCount(responseBody);
            log.info("Ticker aggregate for {} from {} to {}: resultsCount={}, payload={}",
                ticker, previousMarketDate, eventDate, resultsCount, responseBody);

            if (resultsCount == 1) {
                LocalDate earlierMarketDate = previousMarketDay(previousMarketDate);
                String retryResponseBody = massiveRestStockService.fetchTickerAggregate(ticker, earlierMarketDate, eventDate);
                int retryResultsCount = aggregateParser.extractResultsCount(retryResponseBody);
                log.info("(Retry) Ticker aggregate for {} from {} to {}: resultsCount={}, payload={}",
                    ticker, earlierMarketDate, eventDate, retryResultsCount, retryResponseBody);
                if (retryResultsCount > 0) {
                    responseBody = retryResponseBody;
                }
            }

            Optional<DayStockMovementAggregateParser.AggregateSnapshot> snapshot =
                aggregateParser.parseAggregate(responseBody, eventDate, previousMarketDate);

            if (snapshot.isEmpty()) {
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

    private LocalDate toLocalDate(Object rawValue) {
        if (rawValue instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (rawValue instanceof LocalDate localDate) {
            return localDate;
        }
        if (rawValue != null) {
            return MudDateUtil.parseIso(rawValue.toString());
        }
        return null;
    }

    private Long toLong(Object rawValue) {
        return rawValue instanceof Number number ? number.longValue() : null;
    }

    private LocalDate previousMarketDay(LocalDate date) {
        LocalDate current = date.minusDays(1);
        while (marketCalendarService.isMarketClosed(current)) {
            current = current.minusDays(1);
        }
        return current;
    }
}