package com.rama.mudstock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AlphaVantageStockService {

    @Value("${alphavantage.function}")
    private String function;

    @Value("${alphavantage.apikey}")
    private String apiKey;

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    public String fetchDailyTimeSeries(String ticker) {
        RestTemplate rest = new RestTemplate();
        String url = String.format("%s?function=%s&symbol=%s&apikey=%s", BASE_URL, function, ticker, apiKey);
        try {
            return rest.getForObject(url, String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch time series from Alpha Vantage", e);
        }
    }
}
