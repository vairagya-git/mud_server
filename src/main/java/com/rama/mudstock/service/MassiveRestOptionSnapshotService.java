package com.rama.mudstock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        log.info("Constructed option-snapshot URL: {}", buildMassiveUrl(path));
        return executeGet(
            path,
            "Failed to fetch option snapshot from Massive API (client error)",
            "Failed to fetch option snapshot from Massive API",
            true);
    }
}