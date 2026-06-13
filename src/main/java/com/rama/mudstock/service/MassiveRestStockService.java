package com.rama.mudstock.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MassiveRestStockService {

    @Value("${massive.base-url}")
    private String baseUrl;

    @Value("${massive.apikey}")
    private String apiKey;

    @Value("${massive.open-close:}")
    private String openClosePattern;

    @Value("${massive.ticker-aggregate:}")
    private String tickerAggregatePattern;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MassiveRestApiCallLimiter apiCallLimiter;
    private final com.rama.mudstock.repository.DayEventMappingRepository mappingRepository;
    private final com.rama.mudstock.repository.DayEventEntryRepository dayEventEntryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(MassiveRestStockService.class);

    @Autowired
    public MassiveRestStockService(MassiveRestApiCallLimiter apiCallLimiter,
                                   com.rama.mudstock.repository.DayEventMappingRepository mappingRepository,
                                   com.rama.mudstock.repository.DayEventEntryRepository dayEventEntryRepository) {
        this.apiCallLimiter = apiCallLimiter;
        this.mappingRepository = mappingRepository;
        this.dayEventEntryRepository = dayEventEntryRepository;
    }

    public String fetchOpenClose(String ticker, LocalDate date) {
        try {
            apiCallLimiter.acquireOrWait();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for API rate limit", ie);
        }
        String dateStr = date.format(DATE_FMT);
        RestTemplate rest = new RestTemplate();
        String url = joinBaseAndPath(baseUrl, String.format(openClosePattern, ticker, dateStr, apiKey));
        try {
            return rest.getForObject(url, String.class);
        } catch (HttpClientErrorException hce) {
            // Let caller handle 404 (market closed) specifically; rethrow for higher-level handling
            if (hce.getStatusCode() == HttpStatus.NOT_FOUND) throw hce;
            throw new RuntimeException("Failed to fetch open/close from Massive API (client error)", hce);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch open/close from Massive API", e);
        }
    }

    /**
     * Fetch ticker aggregate data for the given date range using the configured pattern
     */
    public String fetchTickerAggregate(String ticker, LocalDate start, LocalDate end) {
        try {
            apiCallLimiter.acquireOrWait();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for API rate limit", ie);
        }
        String startStr = start.format(DATE_FMT);
        String endStr = end.format(DATE_FMT);
        RestTemplate rest = new RestTemplate();
        String url = joinBaseAndPath(baseUrl, String.format(tickerAggregatePattern, ticker, startStr, endStr, apiKey));
        try {
            log.info("Constructed ticker-aggregate URL: {}", url);
            String resp = rest.getForObject(url, String.class);
            return resp;
        } catch (HttpClientErrorException hce) {
            if (hce.getStatusCode() == HttpStatus.NOT_FOUND) throw hce;
            throw new RuntimeException("Failed to fetch ticker aggregate from Massive API (client error)", hce);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch ticker aggregate from Massive API", e);
        }
    }

    /**
     * Process all day-event mappings with status 'new': fetch aggregate data and log details.
     */
    public void fetchAggregatesForNewMappings() {
        List<Map<String,Object>> mappings = mappingRepository.listMappingsByStatus("new");
        if (mappings == null || mappings.isEmpty()) {
            log.info("No day-event mappings with status 'new' found.");
            return;
        }
        for (Map<String,Object> m : mappings) {
            try {
                String ticker = (String) m.get("ticker");
                Object eo = m.get("event_date");
                LocalDate eventDate = null;
                if (eo instanceof java.sql.Date) eventDate = ((java.sql.Date) eo).toLocalDate();
                else if (eo instanceof java.time.LocalDate) eventDate = (java.time.LocalDate) eo;
                else if (eo != null) eventDate = LocalDate.parse(eo.toString(), DATE_FMT);
                if (ticker == null || eventDate == null) {
                    log.warn("Skipping mapping with missing ticker or eventDate: {}", m);
                    continue;
                }
                LocalDate start = previousMarketDay(eventDate.minusDays(0));
                String resp = fetchTickerAggregate(ticker, start, eventDate);
                // parse resultsCount
                JsonNode root = objectMapper.readTree(resp);
                int resultsCount = 0;
                if (root.has("resultsCount")) resultsCount = root.get("resultsCount").asInt();
                else if (root.has("results") && root.get("results").isArray()) resultsCount = root.get("results").size();
                log.info("Ticker aggregate for {} from {} to {}: resultsCount={}, payload={}", ticker, start, eventDate, resultsCount, resp);
                if (resultsCount == 1) {
                    // try one more day earlier
                    LocalDate earlier = previousMarketDay(start.minusDays(1));
                    String resp2 = fetchTickerAggregate(ticker, earlier, eventDate);
                    JsonNode root2 = objectMapper.readTree(resp2);
                    int resultsCount2 = 0;
                    if (root2.has("resultsCount")) resultsCount2 = root2.get("resultsCount").asInt();
                    else if (root2.has("results") && root2.get("results").isArray()) resultsCount2 = root2.get("results").size();
                    log.info("(Retry) Ticker aggregate for {} from {} to {}: resultsCount={}, payload={}", ticker, earlier, eventDate, resultsCount2, resp2);
                    // if retry succeeded, prefer root2 as the data source
                    if (resultsCount2 > 0) {
                        root = root2;
                    }
                }
                // extract data from results array: find entry for eventDate and previous market day
                if (root.has("results") && root.get("results").isArray()) {
                    JsonNode results = root.get("results");
                    LocalDate prevDate = previousMarketDay(eventDate);
                    JsonNode prevNode = null;
                    JsonNode curNode = null;
                    for (JsonNode n : results) {
                        if (!n.has("t")) continue;
                        long t = n.get("t").asLong();
                        LocalDate d = java.time.Instant.ofEpochMilli(t).atZone(java.time.ZoneId.of("UTC")).toLocalDate();
                        if (d.equals(eventDate)) curNode = n;
                        else if (d.equals(prevDate)) prevNode = n;
                    }
                    if (curNode != null && prevNode != null) {
                        double preDayClose = prevNode.has("c") ? prevNode.get("c").asDouble() : 0.0;
                        double curDayOpen = curNode.has("o") ? curNode.get("o").asDouble() : 0.0;
                        double curDayClose = curNode.has("c") ? curNode.get("c").asDouble() : 0.0;
                        double curDayHigh = curNode.has("h") ? curNode.get("h").asDouble() : 0.0;
                        double curDayLow = curNode.has("l") ? curNode.get("l").asDouble() : 0.0;
                        double curDayVolWeight = curNode.has("vw") ? curNode.get("vw").asDouble() : 0.0;
                        long curDayVolume = curNode.has("v") ? curNode.get("v").asLong() : 0L;
                        Double changePercent = null;
                        Double dayOpeningChangePercent = null;
                        if (preDayClose != 0.0) {
                            java.math.BigDecimal raw = java.math.BigDecimal.valueOf(curDayClose).subtract(java.math.BigDecimal.valueOf(preDayClose))
                                    .divide(java.math.BigDecimal.valueOf(preDayClose), 8, java.math.RoundingMode.HALF_UP)
                                    .multiply(java.math.BigDecimal.valueOf(100));
                            changePercent = raw.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();

                            // ((curDayOpen - preDayClose) / preDayClose) * 100
                            java.math.BigDecimal rawOpening = java.math.BigDecimal.valueOf(curDayOpen).subtract(java.math.BigDecimal.valueOf(preDayClose))
                                    .divide(java.math.BigDecimal.valueOf(preDayClose), 8, java.math.RoundingMode.HALF_UP)
                                    .multiply(java.math.BigDecimal.valueOf(100));
                            dayOpeningChangePercent = rawOpening.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
                        }
                        // save to DB; mapping contains day_event_master_id and stock_id and day_event_map_id
                        Long dayEventMapId = m.get("day_event_map_id") instanceof Number ? ((Number) m.get("day_event_map_id")).longValue() : null;
                        Long stockId = m.get("stock_id") instanceof Number ? ((Number) m.get("stock_id")).longValue() : null;
                        Long dayEventMasterId = m.get("day_event_master_id") instanceof Number ? ((Number) m.get("day_event_master_id")).longValue() : null;
                        if (dayEventMapId != null) {
                            dayEventEntryRepository.upsertDayEventEntry(dayEventMapId, preDayClose, curDayOpen, curDayClose, curDayHigh, curDayLow, curDayVolWeight, curDayVolume, changePercent, dayOpeningChangePercent);
                            log.info("Saved day_event_entry for mappingId={} eventDate={}", dayEventMapId, eventDate);
                            try {
                                int updated = mappingRepository.updateStatus(dayEventMapId, "processed");
                                if (updated > 0) log.info("Marked day_event_map id={} as processed", dayEventMapId);
                                else log.warn("No day_event_map row updated for id={}", dayEventMapId);
                            } catch (Exception e) {
                                log.error("Failed to update day_event_map status for id={}", dayEventMapId, e);
                            }
                        } else {
                            log.warn("Missing day_event_map_id to save day_event_entry for mapping {}", m);
                        }
                    } else {
                        log.warn("Could not find both previous and current day bars in aggregate results for mapping {}", m);
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to fetch aggregate for mapping {}", m, ex);
            }
        }
    }

    private LocalDate previousMarketDay(LocalDate d) {
        LocalDate cur = d.minusDays(1);
        while (cur.getDayOfWeek() == DayOfWeek.SATURDAY || cur.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cur = cur.minusDays(1);
        }
        return cur;
    }

    // convenience overload
    public String fetchOpenClose(String ticker, String dateIso) {
        return fetchOpenClose(ticker, LocalDate.parse(dateIso, DATE_FMT));
    }

    private String joinBaseAndPath(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) return base + path.substring(1);
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

}

