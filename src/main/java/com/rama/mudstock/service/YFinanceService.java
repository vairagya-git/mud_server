package com.rama.mudstock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rama.mudstock.model.yfinance.YFinanceTickerResponse;

/**
 * Calls the local Python yfinance REST service to retrieve ticker data
 * (news and earnings calendar) for a given stock symbol.
 *
 * Configuration (application-cronjob.yml):
 *   python.yfinance-url: http://localhost:9001/
 *   python.ticker:       ticker/%s
 */
@Service
public class YFinanceService {

    private static final Logger log = LoggerFactory.getLogger(YFinanceService.class);

    @Value("${python.yfinance-url}")
    private String baseUrl;

    @Value("${python.ticker}")
    private String tickerPath;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches ticker info from the yfinance Python service.
     *
     * @param ticker stock symbol, e.g. "SNDK"
     * @return parsed response, or {@code null} if the call fails
     */
    public YFinanceTickerResponse getTicker(String ticker) {
        String url = baseUrl + String.format(tickerPath, ticker);
        log.debug("YFinanceService: GET {}", url);
        try {
            YFinanceTickerResponse response = restTemplate.getForObject(url, YFinanceTickerResponse.class);
            if (response == null) {
                log.warn("YFinanceService: empty response for ticker {}", ticker);
            }
            return response;
        } catch (RestClientException ex) {
            log.error("YFinanceService: failed to fetch ticker {}: {}", ticker, ex.getMessage());
            return null;
        }
    }
}
