package com.rama.mudstock.scheduler.earnings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.model.earnings.EarningsDate;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.model.yfinance.YFinanceTickerResponse;
import com.rama.mudstock.repository.earnings.EarningsDateRepository;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.service.YFinanceService;
import com.rama.mudstock.util.MudDateUtil;
import com.rama.mudstock.util.WatchlistUtil;

/**
 * Weekly cronjob that, for each stock in the configured watchlists, calls the
 * yfinance Python service and persists upcoming earnings dates.
 */
@Component
@Profile("cronjob")
public class UpcomingEarningCronjob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(UpcomingEarningCronjob.class);

    private final WatchlistRepository watchlistRepository;
    private final EarningsDateRepository earningsDateRepository;
    private final YFinanceService yFinanceService;
    private final SystemConfigService systemConfigService;

    public UpcomingEarningCronjob(WatchlistRepository watchlistRepository,
                                  EarningsDateRepository earningsDateRepository,
                                  YFinanceService yFinanceService,
                                  SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.watchlistRepository = watchlistRepository;
        this.earningsDateRepository = earningsDateRepository;
        this.yFinanceService = yFinanceService;
        this.systemConfigService = systemConfigService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void run() {
        var watchlistCfg = CronjobConfigEnum.WATCHLIST_CODES;
        String purpose = CronjobConfigEnum.Purpose.WEEKLY_UPCOMING_EARNING_CRONJOB.value();

        boolean enabled = isEnabled(purpose);

        if (!enabled) {
            log.info("UpcomingEarningCronjob: disabled by system_config (purpose={}, code={})",
                purpose,
                enabledCode());
            return;
        }

        if (!shouldExecuteSinceLastUpdated("UpcomingEarningCronjob", null, purpose, LISBON)) {
            return;
        }

        List<String> watchlistCodeList = systemConfigService
            .findByPurposeAndCode(
                purpose,
                watchlistCfg.code())
            .filter(List.class::isInstance)
            .map(v -> ((List<?>) v).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList())
            .orElse(List.of());

        String watchlistCodes = String.join(",", watchlistCodeList);

        log.info("UpcomingEarningCronjob: starting for watchlists [{}]", watchlistCodes);
        Map<String, Stock> uniqueStocks = WatchlistUtil.collectUniqueStocksByTicker(
                watchlistCodes,
                watchlistRepository,
                log,
                "UpcomingEarningCronjob");

        log.info("UpcomingEarningCronjob: collected {} unique stock(s) from configured watchlists", uniqueStocks.size());
        for (Stock stock : uniqueStocks.values()) {
            try {
                processStock(stock);
            } catch (Exception ex) {
                log.error("UpcomingEarningCronjob: error processing stock {}", stock.getTicker(), ex);
            }
        }
        updateLastUpdatedNowUtc(purpose, lastUpdatedCode());
        log.info("UpcomingEarningCronjob: finished");
    }

    private void processStock(Stock stock) {
        YFinanceTickerResponse response = yFinanceService.getTicker(stock.getTicker());
        if (response == null) {
            log.warn("UpcomingEarningCronjob: no response for ticker {}", stock.getTicker());
            return;
        }

        List<LocalDate> earningsDates = extractEarningsDates(response);
        if (earningsDates.isEmpty()) {
            log.info("UpcomingEarningCronjob: no earnings dates found for {}", stock.getTicker());
            return;
        }

        for (LocalDate date : earningsDates) {
            if (earningsDateRepository.existsByStockIdAndEarningsDate(stock.getId(), date)) {
                log.debug("UpcomingEarningCronjob: earnings date {} already exists for {}, skipping", date, stock.getTicker());
                continue;
            }
            EarningsDate ed = new EarningsDate();
            ed.setStockId(stock.getId());
            ed.setQuarter(computeQuarter(date));
            ed.setReleaseTime(EarningsDate.ReleaseTime.AFTER_MARKET);
            ed.setStatus(EarningsDate.Status.UPCOMING);
            ed.setEarningsDate(date);
            earningsDateRepository.save(ed);
            log.info("UpcomingEarningCronjob: saved {} earnings date {} ({})", stock.getTicker(), date, ed.getQuarter());
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
     * ("2026-07-15 00:00:00+00:00"). Takes the first 10 characters (ISO prefix)
     * and delegates to {@link MudDateUtil#parseFlexible(String)}.
     */
    private LocalDate parseDateSafely(String s) {
        if (s == null || s.length() < 10) return null;
        LocalDate date = MudDateUtil.parseFlexible(s.substring(0, 10));
        if (date == null) log.warn("UpcomingEarningCronjob: could not parse date string '{}'", s);
        return date;
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
