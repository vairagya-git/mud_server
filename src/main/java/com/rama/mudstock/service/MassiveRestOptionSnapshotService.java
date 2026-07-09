package com.rama.mudstock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MassiveRestOptionSnapshotService extends AbstractMassiveRestService {

    @Value("${massive.option-snapshot:}")
    private String optionSnapshotPattern;

    private static final Logger log = LoggerFactory.getLogger(MassiveRestOptionSnapshotService.class);

    public MassiveRestOptionSnapshotService(MassiveRestApiCallLimiter apiCallLimiter) {
        super(apiCallLimiter);
    }

    public String fetchOptionSnapshot(String ticker, String strikePrice, String expirationDate) {
        String path = String.format(optionSnapshotPattern, ticker, strikePrice, expirationDate, massiveApiKey());
        log.info("Constructed option-snapshot URL: {}", buildMassiveUrl(path));
        return executeGet(
            path,
            "Failed to fetch option snapshot from Massive API (client error)",
            "Failed to fetch option snapshot from Massive API",
            true);
    }
}