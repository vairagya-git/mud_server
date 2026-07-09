package com.rama.mudstock.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rama.mudstock.util.MudDateUtil;

@Service
public class MassiveRestStockService extends AbstractMassiveRestService {

    @Value("${massive.open-close:}")
    private String openClosePattern;

    @Value("${massive.ticker-aggregate:}")
    private String tickerAggregatePattern;

    private static final Logger log = LoggerFactory.getLogger(MassiveRestStockService.class);

    public MassiveRestStockService(MassiveRestApiCallLimiter apiCallLimiter) {
        super(apiCallLimiter);
    }

    public String fetchOpenClose(String ticker, LocalDate date) {
        String dateStr = MudDateUtil.toIsoString(date);
        String path = String.format(openClosePattern, ticker, dateStr, massiveApiKey());
        return executeGet(
            path,
            "Failed to fetch open/close from Massive API (client error)",
            "Failed to fetch open/close from Massive API",
            true);
    }

    /**
     * Fetch ticker aggregate data for the given date range using the configured pattern
     */
    public String fetchTickerAggregate(String ticker, LocalDate start, LocalDate end) {
        String startStr = MudDateUtil.toIsoString(start);
        String endStr = MudDateUtil.toIsoString(end);
        String path = String.format(tickerAggregatePattern, ticker, startStr, endStr, massiveApiKey());
        log.info("Constructed ticker-aggregate URL: {}", buildMassiveUrl(path));
        return executeGet(
            path,
            "Failed to fetch ticker aggregate from Massive API (client error)",
            "Failed to fetch ticker aggregate from Massive API",
            true);
    }

    // convenience overload
    public String fetchOpenClose(String ticker, String dateIso) {
        return fetchOpenClose(ticker, MudDateUtil.parseIso(dateIso));
    }

}

