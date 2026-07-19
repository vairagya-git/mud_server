package com.rama.mudstock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.rama.mudstock.config.ApplicationProperties;

@Service
public class MassiveRestOptionSnapshotService extends AbstractMassiveRestService {

    private final ApplicationProperties applicationProperties;

    private static final Logger log = LoggerFactory.getLogger(MassiveRestOptionSnapshotService.class);

    public MassiveRestOptionSnapshotService(MassiveRestApiCallLimiter apiCallLimiter,
                                            ApplicationProperties applicationProperties) {
        super(apiCallLimiter, applicationProperties);
        this.applicationProperties = applicationProperties;
    }

    public String fetchOptionSnapshot(String ticker, String strikePrice, String expirationDate) {
        String optionSnapshotPattern = applicationProperties.getMassive().getOptionSnapshot();
        String path = String.format(optionSnapshotPattern, ticker, strikePrice, expirationDate, massiveApiKey());
        String url = buildMassiveUrl(path);
        log.info("fetchOptionSnapshot start: ticker={}, strikePrice={}, expirationDate={}, url={}",
            ticker,
            strikePrice,
            expirationDate,
            url);

        try {
            String response = executeGet(
                path,
                "Failed to fetch option snapshot from Massive API (client error)",
                "Failed to fetch option snapshot from Massive API",
                true);
            log.info("fetchOptionSnapshot success: ticker={}, strikePrice={}, expirationDate={}, responseLength={}",
                ticker,
                strikePrice,
                expirationDate,
                response == null ? 0 : response.length());
            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            log.info("fetchOptionSnapshot 404: client-error: ticker={}, strikePrice={}, expirationDate={}, url={}, status={}, responseBody={}",
                ticker,
                strikePrice,
                expirationDate,
                url,
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString());
            throw ex;
        } catch (HttpClientErrorException ex) {
            log.info("fetchOptionSnapshot client-error: ticker={}, strikePrice={}, expirationDate={}, url={}, status={}, responseBody={}",
                ticker,
                strikePrice,
                expirationDate,
                url,
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.info("fetchOptionSnapshot failed: ticker={}, strikePrice={}, expirationDate={}, url={}",
                ticker,
                strikePrice,
                expirationDate,
                url,
                ex);
            throw ex;
        }
    }
}