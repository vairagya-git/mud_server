package com.rama.mudstock.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class MassiveRestStockService {

    @Value("${massive.base-url}")
    private String baseUrl;

    @Value("${massive.apikey}")
    private String apiKey;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MassiveRestApiCallLimiter apiCallLimiter;

    @Autowired
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
        String dateStr = date.format(DATE_FMT);
        RestTemplate rest = new RestTemplate();
        // Expected URL: {baseUrl}/{ticker}/{date}?adjusted=true&apiKey={apiKey}
        String url = String.format("%s/%s/%s?adjusted=true&apiKey=%s", baseUrl, ticker, dateStr, apiKey);
        try {
            return rest.getForObject(url, String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch open/close from Massive API", e);
        }
    }

    // convenience overload
    public String fetchOpenClose(String ticker, String dateIso) {
        return fetchOpenClose(ticker, LocalDate.parse(dateIso, DATE_FMT));
    }
}
