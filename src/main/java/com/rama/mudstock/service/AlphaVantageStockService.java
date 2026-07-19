package com.rama.mudstock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rama.mudstock.config.ApplicationProperties;

@Service
public class AlphaVantageStockService {

    private final ApplicationProperties applicationProperties;

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    public AlphaVantageStockService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String fetchDailyTimeSeries(String ticker) {
        RestTemplate rest = new RestTemplate();
        String function = applicationProperties.getAlphavantage().getFunction();
        String apiKey = applicationProperties.getAlphavantage().getApikey();
        String url = String.format("%s?function=%s&symbol=%s&apikey=%s", BASE_URL, function, ticker, apiKey);
        try {
            return rest.getForObject(url, String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch time series from Alpha Vantage", e);
        }
    }
}
