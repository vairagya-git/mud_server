package com.rama.mudstock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractMassiveRestService {

    @Value("${massive.base-url}")
    private String baseUrl;

    @Value("${massive.apikey}")
    private String apiKey;

    private final MassiveRestApiCallLimiter apiCallLimiter;

    protected AbstractMassiveRestService(MassiveRestApiCallLimiter apiCallLimiter) {
        this.apiCallLimiter = apiCallLimiter;
    }

    protected String massiveApiKey() {
        return apiKey;
    }

    protected String buildMassiveUrl(String path) {
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