package com.rama.mudstock.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rama.mudstock.util.MudDateUtil;

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

    private final MassiveRestApiCallLimiter apiCallLimiter;
    private static final Logger log = LoggerFactory.getLogger(MassiveRestStockService.class);

    public MassiveRestStockService(MassiveRestApiCallLimiter apiCallLimiter) {
        this.apiCallLimiter = apiCallLimiter;
    }

    public String fetchOpenClose(String ticker, LocalDate date) {
        try {
            apiCallLimiter.acquireOrWait();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for API rate limit", ie);
        }
        String dateStr = MudDateUtil.toIsoString(date);
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
        String startStr = MudDateUtil.toIsoString(start);
        String endStr = MudDateUtil.toIsoString(end);
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

    // convenience overload
    public String fetchOpenClose(String ticker, String dateIso) {
        return fetchOpenClose(ticker, MudDateUtil.parseIso(dateIso));
    }

    private String joinBaseAndPath(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) return base + path.substring(1);
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

}

