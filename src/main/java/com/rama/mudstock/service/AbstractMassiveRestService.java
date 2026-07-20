package com.rama.mudstock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rama.mudstock.config.ApplicationProperties;

public abstract class AbstractMassiveRestService {

    private static final Logger log = LoggerFactory.getLogger(AbstractMassiveRestService.class);

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
        String redactedUrl = redactApiKey(url);
        log.info("Massive executeGet request start: url={}, propagateNotFound={}", redactedUrl, propagateNotFound);
        try {
            String response = rest.getForObject(url, String.class);
            log.info("Massive executeGet request success: url={}, responseLength={}",
                redactedUrl,
                response == null ? 0 : response.length());
            return response;
        } catch (HttpClientErrorException hce) {
            log.warn("Massive executeGet client error: url={}, status={}, responseBody={}",
                redactedUrl,
                hce.getStatusCode().value(),
                abbreviate(hce.getResponseBodyAsString()));
            if (propagateNotFound && hce.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                log.warn("Massive executeGet rethrowing 404 as HttpClientErrorException.NotFound: url={}, status={}",
                    redactedUrl,
                    hce.getStatusCode().value());
                throw hce;
            }
            log.warn("Massive executeGet wrapping client error into RuntimeException: url={}, status={}, propagateNotFound={}",
                redactedUrl,
                hce.getStatusCode().value(),
                propagateNotFound);
            throw new RuntimeException(clientErrorMessage, hce);
        } catch (RestClientException ex) {
            log.error("Massive executeGet rest-client error: url={}, message={}", redactedUrl, ex.getMessage(), ex);
            throw new RuntimeException(genericErrorMessage, ex);
        }
    }

    private String redactApiKey(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        return url.replaceAll("([?&]apiKey=)[^&]+", "$1***");
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() > 500 ? value.substring(0, 500) + "..." : value;
    }
}