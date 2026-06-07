package com.rama.mudstock.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rama.mudstock.model.DatePeriod;
import com.rama.mudstock.repository.EarningsDateEntryRepository;

@Service
public class DatePeriodFetcher {

    private final MassiveRestStockService massiveService;
    private final EarningsDateEntryRepository entryRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(DatePeriodFetcher.class);

    public DatePeriodFetcher(MassiveRestStockService massiveService, EarningsDateEntryRepository entryRepository) {
        this.massiveService = massiveService;
        this.entryRepository = entryRepository;
    }

    /**
     * Fetch open/close data for all DatePeriod values relative to the provided earningsDate.
     * Returns a map of DatePeriod -> API response body (as string). Caller may inspect and persist.
     */
    public Map<DatePeriod, String> fetchAllForTickerAndEarningsDate(String ticker, LocalDate earningsDate, Long earningsDateId, Long stockId) {
        Map<DatePeriod, String> result = new EnumMap<>(DatePeriod.class);
        for (DatePeriod dp : DatePeriod.values()) {
            LocalDate target = earningsDate.plusDays(dp.getDaysOffset());
            // original target (before weekend-adjustment) used to determine availability
            LocalDate originalTarget = target;
            // adjust weekends per rules:
            // - OneWeekBefore: if target is Sat/Sun -> move back to previous Friday
            // - OneWeekAfter, TwoWeekAfter: if target is Sat/Sun -> move forward to next Monday
            target = adjustForWeekend(dp, target);
            // If the original target falls on a weekend, mark status = NULL and skip fetching
            switch (originalTarget.getDayOfWeek()) {
                case SATURDAY:
                case SUNDAY:
                    log.info("Original target {} for {} (period {}) is weekend — marking status done and skipping", originalTarget, ticker, dp);
                    try {
                        entryRepository.setEntryStatusToDone(earningsDateId, stockId, dp.getDbValue());
                    } catch (Exception ux) {
                        log.error("Failed to set status done for {} {} {}: {}", earningsDateId, stockId, dp, ux.getMessage());
                    }
                    continue;
            }
            LocalDate now = LocalDate.now();
            if (!now.isAfter(target)) {
                log.info("Target date {} for {} (period {}) not reached (now={}), skipping remaining periods for this ticker", target, ticker, dp, now);
                break;
            }
            try {
                // Only fetch if the entry is still marked as 'new'
                String currentStatus = entryRepository.getEntryStatus(earningsDateId, stockId, dp.getDbValue());
                if (currentStatus == null || !"new".equalsIgnoreCase(currentStatus)) {
                    log.info("Skipping fetch for {} {} {} because status is not 'new' (status={})", earningsDateId, stockId, dp, currentStatus);
                    continue;
                }

                String body = null;
                try {
                    body = massiveService.fetchOpenClose(ticker, target);
                    result.put(dp, body);
                    log.info("Fetched {} for {} on {} (len={})", dp, ticker, target, body == null ? 0 : body.length());
                } catch (org.springframework.web.client.HttpClientErrorException hce) {
                    if (hce.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND) {
                        // Market closed or no data for this date — delete the initially created entry and skip
                        log.info("No data (404) for {} on {} — deleting initial entry and skipping", ticker, target);
                        try {
                            int deleted = entryRepository.deleteEntryForEarningsDate(earningsDateId, stockId, dp.getDbValue());
                            if (deleted == 0) {
                                log.info("No earnings_date_entry row found to delete for {} {} {}", earningsDateId, stockId, dp);
                            }
                        } catch (Exception ux) {
                            log.error("Failed to delete entry for {} {} {}: {}", earningsDateId, stockId, dp, ux.getMessage());
                        }
                        continue;
                    }
                    throw hce;
                }

                if (body != null) {
                    try {
                        JsonNode node = mapper.readTree(body);
                        BigDecimal open = node.hasNonNull("open") ? new BigDecimal(node.get("open").asText()) : null;
                        BigDecimal high = node.hasNonNull("high") ? new BigDecimal(node.get("high").asText()) : null;
                        BigDecimal low = node.hasNonNull("low") ? new BigDecimal(node.get("low").asText()) : null;
                        BigDecimal close = node.hasNonNull("close") ? new BigDecimal(node.get("close").asText()) : null;
                        BigDecimal volume = node.hasNonNull("volume") ? new BigDecimal(node.get("volume").asText()) : null;
                        String from = node.hasNonNull("from") ? node.get("from").asText() : null;

                        int updated = entryRepository.updateEntryForEarningsDate(earningsDateId, stockId, dp.getDbValue(), from, open, high, low, close, volume);
                        if (updated == 0) {
                            log.info("No row updated for {} {} {} — it may have been processed already", earningsDateId, stockId, dp);
                        }
                    } catch (Exception jex) {
                        log.error("Failed to parse/fill entry for {} on {}: {}", ticker, target, jex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to fetch {} for {} on {}: {}", dp, ticker, target, ex.getMessage());
                result.put(dp, null);
            }
        }
        return result;
    }

    private LocalDate adjustForWeekend(DatePeriod dp, LocalDate date) {
        switch (dp) {
            case OneWeekBefore:
                switch (date.getDayOfWeek()) {
                    case SATURDAY:
                        return date.minusDays(1);
                    case SUNDAY:
                        return date.minusDays(2);
                    default:
                        return date;
                }
            case OneWeekAfter:
            case TwoWeekAfter:
                switch (date.getDayOfWeek()) {
                    case SATURDAY:
                        return date.plusDays(2);
                    case SUNDAY:
                        return date.plusDays(1);
                    default:
                        return date;
                }
            default:
                return date;
        }
    }
}
