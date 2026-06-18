package com.rama.mudstock.scheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.model.EarningsDate;
import com.rama.mudstock.model.YFinanceTickerResponse;
import com.rama.mudstock.model.Watchlist;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.repository.EarningsDateRepository;
import com.rama.mudstock.repository.WatchlistRepository;
import com.rama.mudstock.service.YFinanceService;

/**
 * Weekly cronjob that, for each stock in the configured watchlists, calls the
 * yfinance Python service and persists upcoming earnings dates.
 *
 * Cron and watchlist config come from application-cronjob.yml:
 *   weeklyUpcomingEarning.cron
 *   weeklyUpcomingEarning.watchlists  (comma-separated watchlist codes)
 */
@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "weeklyUpcomingEarning", name = "enabled", havingValue = "true")
public class WeeklyUpcomingEarningCronjob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyUpcomingEarningCronjob.class);

    private final WatchlistRepository watchlistRepository;
    private final EarningsDateRepository earningsDateRepository;
    private final YFinanceService yFinanceService;

    @Value("${weeklyUpcomingEarning.watchlists}")
    private String watchlistCodes;

    public WeeklyUpcomingEarningCronjob(WatchlistRepository watchlistRepository,
                                 EarningsDateRepository earningsDateRepository,
                                 YFinanceService yFinanceService) {
        this.watchlistRepository = watchlistRepository;
        this.earningsDateRepository = earningsDateRepository;
        this.yFinanceService = yFinanceService;
    }

    @Scheduled(cron = "${weeklyUpcomingEarning.cron}", zone = "Europe/Lisbon")
    public void run() {
        log.info("WeeklyUpcomingEarningCronjob: starting for watchlists [{}]", watchlistCodes);
        Map<String, Stock> uniqueStocks = new LinkedHashMap<>();
        for (String code : watchlistCodes.split(",")) {
            String trimmed = code.trim();
            watchlistRepository.findByCodeWithStocks(trimmed)
                .ifPresentOrElse(
                    w -> collectUniqueStocks(w, uniqueStocks),
                    () -> log.warn("WeeklyUpcomingEarningCronjob: watchlist not found: {}", trimmed)
                );
        }

        log.info("WeeklyUpcomingEarningCronjob: collected {} unique stock(s) from configured watchlists", uniqueStocks.size());
        for (Stock stock : uniqueStocks.values()) {
            try {
                processStock(stock);
            } catch (Exception ex) {
                log.error("WeeklyUpcomingEarningCronjob: error processing stock {}", stock.getTicker(), ex);
            }
        }
        log.info("WeeklyUpcomingEarningCronjob: finished");
    }

    private void collectUniqueStocks(Watchlist watchlist, Map<String, Stock> uniqueStocks) {
        log.info("WeeklyUpcomingEarningCronjob: processing watchlist '{}'", watchlist.getCode());
        for (Stock stock : watchlist.getStocks()) {
            String key = stock.getId() != null ? String.valueOf(stock.getId()) : stock.getTicker();
            uniqueStocks.putIfAbsent(key, stock);
        }
    }

    private void processStock(Stock stock) {
        YFinanceTickerResponse response = yFinanceService.getTicker(stock.getTicker());
        if (response == null) {
            log.warn("WeeklyUpcomingEarningCronjob: no response for ticker {}", stock.getTicker());
            return;
        }

        List<LocalDate> earningsDates = extractEarningsDates(response);
        if (earningsDates.isEmpty()) {
            log.info("WeeklyUpcomingEarningCronjob: no earnings dates found for {}", stock.getTicker());
            return;
        }

        for (LocalDate date : earningsDates) {
            if (earningsDateRepository.existsByStockIdAndEarningsDate(stock.getId(), date)) {
                log.debug("WeeklyUpcomingEarningCronjob: earnings date {} already exists for {}, skipping", date, stock.getTicker());
                continue;
            }
            EarningsDate ed = new EarningsDate();
            ed.setStockId(stock.getId());
            ed.setQuarter(computeQuarter(date));
            ed.setReleaseTime(EarningsDate.ReleaseTime.AFTER_MARKET);
            ed.setStatus(EarningsDate.Status.UPCOMING);
            ed.setEarningsDate(date);
            earningsDateRepository.save(ed);
            log.info("WeeklyUpcomingEarningCronjob: saved {} earnings date {} ({})", stock.getTicker(), date, ed.getQuarter());
        }
    }

    /**
     * Extracts earnings dates from the "Earnings Date" key in the calendar map.
     * The value may be a single date string or a JSON array (deserialized as List).
     */
    @SuppressWarnings("unchecked")
    private List<LocalDate> extractEarningsDates(YFinanceTickerResponse response) {
        List<LocalDate> result = new ArrayList<>();
        if (response.getCalendar() == null) return result;

        Object raw = response.getCalendar().get("Earnings Date");
        if (raw == null) return result;

        List<String> dateStrings;
        if (raw instanceof List) {
            dateStrings = (List<String>) raw;
        } else {
            dateStrings = List.of(raw.toString());
        }

        for (String s : dateStrings) {
            LocalDate date = parseDateSafely(s);
            if (date != null) result.add(date);
        }
        return result;
    }

    /**
     * Parses a date string that may be ISO-8601 ("2026-07-15"), a datetime
     * ("2026-07-15T00:00:00"), or a datetime with timezone offset
     * ("2026-07-15 00:00:00+00:00"). Takes the first 10 characters.
     */
    private LocalDate parseDateSafely(String s) {
        if (s == null || s.length() < 10) return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception ex) {
            log.warn("WeeklyUpcomingEarningCronjob: could not parse date string '{}'", s);
            return null;
        }
    }

    /**
     * Returns quarter string in the format Q{1-4}-{year} based on the earnings date.
     * Jan-Mar → Q1, Apr-Jun → Q2, Jul-Sep → Q3, Oct-Dec → Q4.
     */
    private static String computeQuarter(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return "Q" + quarter + "-" + date.getYear();
    }
}
