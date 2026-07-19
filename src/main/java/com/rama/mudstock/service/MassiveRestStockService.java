package com.rama.mudstock.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.util.MudDateUtil;

@Service
public class MassiveRestStockService extends AbstractMassiveRestService {

    private final ApplicationProperties applicationProperties;

    private static final Logger log = LoggerFactory.getLogger(MassiveRestStockService.class);

    public MassiveRestStockService(MassiveRestApiCallLimiter apiCallLimiter,
                                   ApplicationProperties applicationProperties) {
        super(apiCallLimiter, applicationProperties);
        this.applicationProperties = applicationProperties;
    }

    public String fetchOpenClose(String ticker, LocalDate date) {
        String dateStr = MudDateUtil.toIsoString(date);
        String openClosePattern = applicationProperties.getMassive().getOpenClose();
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
        String tickerAggregatePattern = applicationProperties.getMassive().getTickerAggregate();
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

