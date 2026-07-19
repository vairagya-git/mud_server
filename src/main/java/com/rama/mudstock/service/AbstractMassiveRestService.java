package com.rama.mudstock.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rama.mudstock.config.ApplicationProperties;

public abstract class AbstractMassiveRestService {

    private final ApplicationProperties applicationProperties;

    private final MassiveRestApiCallLimiter apiCallLimiter;

    protected AbstractMassiveRestService(MassiveRestApiCallLimiter apiCallLimiter,
                                         ApplicationProperties applicationProperties) {
        this.apiCallLimiter = apiCallLimiter;
        this.applicationProperties = applicationProperties;
    }

    protected String massiveApiKey() {
        return applicationProperties.getMassive().getApikey();
    }

    protected String buildMassiveUrl(String path) {
        String baseUrl = applicationProperties.getMassive().getBaseUrl();
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    protected String executeGet(String path,
                                String clientErrorMessage,
                                String genericErrorMessage,
                                boolean propagateNotFound) {
        try {
            apiCallLimiter.acquireOrWait();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for API rate limit", ie);
        }

        RestTemplate rest = new RestTemplate();
        String url = buildMassiveUrl(path);
        try {
            return rest.getForObject(url, String.class);
        } catch (HttpClientErrorException hce) {
            if (propagateNotFound && hce.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw hce;
            }
            throw new RuntimeException(clientErrorMessage, hce);
        } catch (RestClientException ex) {
            throw new RuntimeException(genericErrorMessage, ex);
        }
    }
}