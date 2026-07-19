package com.rama.mudstock.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.model.analyst.BenzingaAnalystListResponse;
import com.rama.mudstock.model.analyst.BenzingaAnalystRatingListResponse;
import com.rama.mudstock.model.analyst.BenzingaAnalystRatingResponse;
import com.rama.mudstock.model.analyst.BenzingaAnalystResponse;
import com.rama.mudstock.model.analyst.BenzingaFirmListResponse;
import com.rama.mudstock.model.analyst.BenzingaFirmResponse;
import com.rama.mudstock.model.analyst.Firm;
import com.rama.mudstock.repository.analyst.FirmRepository;
import com.rama.mudstock.util.MudDateUtil;

/**
 * Fetches analyst firm data from the configured Benzinga endpoint and
 * persists / upserts the results into the {@code firm} table.
 *
 * <p>Smart update logic: a firm row is only written when it does not exist yet,
 * or when the {@code last_updated} date returned by the API differs from the
 * value already stored in the database.</p>
 *
 * <p>Configuration (application.yml — under {@code massive:}):</p>
 * <pre>
 * massive:
 *   base-url: https://api.example.com/
 *   benzinga-firm: /benzinga/v1/firms?limit=1000&amp;sort=name.asc&amp;apiKey=%s
 *   apikey: YOUR_KEY
 * </pre>
 */
@Service
public class BenzingaFirmService {

    private static final Logger log = LoggerFactory.getLogger(BenzingaFirmService.class);

    private final FirmRepository firmRepository;
    private final RestTemplate restTemplate;
    private final ApplicationProperties applicationProperties;

    public BenzingaFirmService(FirmRepository firmRepository,
                               ApplicationProperties applicationProperties) {
        this.firmRepository = firmRepository;
        this.restTemplate = new RestTemplate();
        this.applicationProperties = applicationProperties;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Fetches the firm list from the Benzinga endpoint and upserts each entry
     * into the {@code firm} table.
     *
     * @return number of firms actually inserted or updated
     */
    public int fetchAndSave() {
        return doFetch(false);
    }

    /**
     * Fetches the firm list and performs a <em>smart</em> update:
     * a row is only written when it does not exist yet, or when the
     * {@code last_updated} date from the API differs from the stored value.
     *
     * @return number of firms inserted or updated
     */
    public int fetchAndSaveSmart() {
        return doFetch(true);
    }

    /**
     * Fetches a single firm by its Benzinga firm ID from the configured
     * {@code massive.benzinga-firm-id} endpoint and upserts it into the database.
     *
     * <p>Endpoint pattern: {@code /benzinga/v1/firms?benzinga_id=%s&limit=1000&sort=name.asc&apiKey=%s}</p>
     *
     * @param benzingaFirmId the Benzinga firm ID to look up (e.g. {@code 66c57995cf364000017832c6})
     * @return the first matching {@link BenzingaFirmResponse} from the API, or {@code null} if not found or error
     */
    public BenzingaFirmResponse fetchByFirmId(String benzingaFirmId) {
        if (benzingaFirmId == null || benzingaFirmId.isBlank()) {
            log.warn("BenzingaFirmService.fetchByFirmId: benzingaFirmId is blank, skipping");
            return null;
        }
        String url = buildFirmByIdUrl(benzingaFirmId.trim());
        log.info("BenzingaFirmService.fetchByFirmId: fetching firm {} from {}", benzingaFirmId, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        BenzingaFirmListResponse wrapper;
        try {
            ResponseEntity<BenzingaFirmListResponse> resp = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, BenzingaFirmListResponse.class);
            wrapper = resp.getBody();
        } catch (Exception ex) {
            log.error("BenzingaFirmService.fetchByFirmId: failed to fetch firm {} from {}", benzingaFirmId, url, ex);
            return null;
        }

        if (wrapper == null || wrapper.getResults() == null || wrapper.getResults().isEmpty()) {
            log.warn("BenzingaFirmService.fetchByFirmId: no results returned for benzinga_id={}", benzingaFirmId);
            return null;
        }

        BenzingaFirmResponse dto = wrapper.getResults().get(0);
        log.info("BenzingaFirmService.fetchByFirmId: found firm name='{}' for benzinga_id={}", dto.getName(), benzingaFirmId);

        // Upsert the returned firm into the database
        try {
            firmRepository.upsert(
                    dto.getBenzingaId().trim(),
                    dto.getName().trim(),
                    dto.getCurrency() == null ? null : dto.getCurrency().trim(),
                    parseLastUpdated(dto.getLastUpdated()));
            log.info("BenzingaFirmService.fetchByFirmId: upserted firm benzinga_id={}", benzingaFirmId);
        } catch (Exception ex) {
            log.error("BenzingaFirmService.fetchByFirmId: error upserting firm benzinga_id={}", benzingaFirmId, ex);
        }

        return dto;
    }

    /**
     * Fetches a single analyst by its Benzinga analyst ID from the configured
     * {@code massive.benzinga-analyst} endpoint.
     *
     * @param benzingaId the Benzinga analyst ID to look up
     * @return the matching {@link BenzingaAnalystResponse}, or {@code null} if not found or error
     */
    public BenzingaAnalystResponse fetchAnalystById(String benzingaId) {
        if (benzingaId == null || benzingaId.isBlank()) {
            log.warn("BenzingaFirmService.fetchAnalystById: benzingaId is blank, skipping");
            return null;
        }
        String baseUrl = applicationProperties.getMassive().getBaseUrl();
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String path = String.format(applicationProperties.getMassive().getBenzingaAnalyst(),
            benzingaId.trim(),
            applicationProperties.getMassive().getApikey());
        if (path.startsWith("/")) path = path.substring(1);
        String url = base + path;
        log.info("BenzingaFirmService.fetchAnalystById: fetching analyst {} from {}", benzingaId, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        BenzingaAnalystListResponse wrapper;
        try {
            ResponseEntity<BenzingaAnalystListResponse> resp = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, BenzingaAnalystListResponse.class);
            wrapper = resp.getBody();
        } catch (Exception ex) {
            log.error("BenzingaFirmService.fetchAnalystById: failed to fetch analyst {} from {}", benzingaId, url, ex);
            return null;
        }

        if (wrapper == null || wrapper.getResults() == null || wrapper.getResults().isEmpty()) {
            log.warn("BenzingaFirmService.fetchAnalystById: no results returned for benzinga_id={}", benzingaId);
            return null;
        }

        BenzingaAnalystResponse analyst = wrapper.getResults().get(0);
        log.info("BenzingaFirmService.fetchAnalystById: found analyst name='{}' for benzinga_id={}", analyst.getFullName(), benzingaId);
        return analyst;
    }

    /**
     * Fetches analyst ratings for a given ticker from the configured
     * {@code massive.benzinga-analyst-rating} endpoint.
     *
     * @param ticker the stock ticker to query ratings for
     * @return list of rating results, or empty list on error
     */
    public java.util.List<BenzingaAnalystRatingResponse> fetchAnalystRatings(String ticker, String ratingDate) {
        if (ticker == null || ticker.isBlank()) {
            log.warn("BenzingaFirmService.fetchAnalystRatings: ticker is blank, skipping");
            return java.util.Collections.emptyList();
        }
        if (ratingDate == null || ratingDate.isBlank()) {
            log.warn("BenzingaFirmService.fetchAnalystRatings: ratingDate is blank, skipping");
            return java.util.Collections.emptyList();
        }
        String baseUrl = applicationProperties.getMassive().getBaseUrl();
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String path = String.format(applicationProperties.getMassive().getBenzingaAnalystRating(),
            ticker.trim(),
            ratingDate.trim(),
            applicationProperties.getMassive().getApikey());
        if (path.startsWith("/")) path = path.substring(1);
        String url = base + path;
        log.info("BenzingaFirmService.fetchAnalystRatings: fetching ratings for ticker={} from {}", ticker, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        BenzingaAnalystRatingListResponse wrapper;
        try {
            ResponseEntity<BenzingaAnalystRatingListResponse> resp = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, BenzingaAnalystRatingListResponse.class);
            wrapper = resp.getBody();
        } catch (Exception ex) {
            log.error("BenzingaFirmService.fetchAnalystRatings: failed to fetch ratings for ticker={}", ticker, ex);
            return java.util.Collections.emptyList();
        }

        if (wrapper == null || wrapper.getResults() == null) {
            log.warn("BenzingaFirmService.fetchAnalystRatings: no results returned for ticker={}", ticker);
            return java.util.Collections.emptyList();
        }

        log.info("BenzingaFirmService.fetchAnalystRatings: received {} rating(s) for ticker={}", wrapper.getResults().size(), ticker);
        return wrapper.getResults();
    }

    /**
     * Returns all firms currently stored in the database.
     */
    public List<Firm> listAll() {
        return firmRepository.findAll();
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private int doFetch(boolean smartUpdate) {
        String firmUrl = buildFirmUrl();
        log.info("BenzingaFirmService: fetching firms from {} (smartUpdate={})", firmUrl, smartUpdate);

        // Send Accept: */* to match WireMock / proxy stubs that require a wildcard Accept header.
        // RestTemplate's default typed getForObject sends "application/json, application/*+json"
        // which can cause stub mismatches in local mock environments.
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // The API wraps the firm array in { "status": "OK", "results": [...] }
        BenzingaFirmListResponse wrapper;
        try {
            ResponseEntity<BenzingaFirmListResponse> resp = restTemplate.exchange(
                    firmUrl, HttpMethod.GET, requestEntity, BenzingaFirmListResponse.class);
            wrapper = resp.getBody();
        } catch (Exception ex) {
            log.error("BenzingaFirmService: failed to fetch firms from {}", firmUrl, ex);
            return 0;
        }

        if (wrapper == null || wrapper.getResults() == null || wrapper.getResults().isEmpty()) {
            log.warn("BenzingaFirmService: received empty or null results from {}", firmUrl);
            return 0;
        }

        List<BenzingaFirmResponse> firms = wrapper.getResults();
        log.info("BenzingaFirmService: received {} firm(s) from API", firms.size());

        int saved = 0;
        int skipped = 0;
        int unchanged = 0;

        for (BenzingaFirmResponse dto : firms) {
            if (dto.getBenzingaId() == null || dto.getBenzingaId().isBlank()
                    || dto.getName() == null || dto.getName().isBlank()) {
                log.warn("BenzingaFirmService: skipping entry with missing benzinga_id or name");
                skipped++;
                continue;
            }

            LocalDate apiLastUpdated = parseLastUpdated(dto.getLastUpdated());

            if (smartUpdate) {
                Optional<Firm> existing = firmRepository.findByBenzingaFirmId(dto.getBenzingaId().trim());
                if (existing.isPresent()) {
                    LocalDate dbLastUpdated = existing.get().getLastUpdated();
                    if (isLastUpdatedSame(dbLastUpdated, apiLastUpdated)) {
                        log.debug("BenzingaFirmService: skipping unchanged firm benzinga_id={} last_updated={}",
                                dto.getBenzingaId(), apiLastUpdated);
                        unchanged++;
                        continue;
                    }
                    log.debug("BenzingaFirmService: updating firm benzinga_id={} last_updated {} → {}",
                            dto.getBenzingaId(), dbLastUpdated, apiLastUpdated);
                }
            }

            try {
                firmRepository.upsert(
                        dto.getBenzingaId().trim(),
                        dto.getName().trim(),
                        dto.getCurrency() == null ? null : dto.getCurrency().trim(),
                        apiLastUpdated);
                saved++;
            } catch (Exception ex) {
                log.error("BenzingaFirmService: error upserting firm benzinga_id={} name={}",
                        dto.getBenzingaId(), dto.getName(), ex);
                skipped++;
            }
        }

        log.info("BenzingaFirmService: done — saved/updated={}, unchanged={}, skipped={}",
                saved, unchanged, skipped);
        return saved;
    }

    /**
     * Returns {@code true} when both dates are considered the same value
     * (handles the case where both are {@code null}).
     */
    private boolean isLastUpdatedSame(LocalDate dbDate, LocalDate apiDate) {
        if (dbDate == null && apiDate == null) return true;
        if (dbDate == null || apiDate == null) return false;
        return dbDate.equals(apiDate);
    }

    /**
     * Builds the full URL for the firms list endpoint.
     * The path pattern from config may contain a {@code %s} placeholder for the API key.
     */
    private String buildFirmUrl() {
        String baseUrl = applicationProperties.getMassive().getBaseUrl();
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String path = String.format(applicationProperties.getMassive().getBenzingaFirm(),
            applicationProperties.getMassive().getApikey());
        if (path.startsWith("/")) path = path.substring(1);
        return base + path;
    }

    /**
     * Builds the full URL for the single-firm-by-ID endpoint.
     * The path pattern expects two {@code %s} placeholders: first the benzinga_id, then the API key.
     * Example pattern: {@code /benzinga/v1/firms?benzinga_id=%s&limit=1000&sort=name.asc&apiKey=%s}
     */
    private String buildFirmByIdUrl(String benzingaFirmId) {
        String baseUrl = applicationProperties.getMassive().getBaseUrl();
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String path = String.format(applicationProperties.getMassive().getBenzingaFirmId(),
            benzingaFirmId,
            applicationProperties.getMassive().getApikey());
        if (path.startsWith("/")) path = path.substring(1);
        return base + path;
    }

    /**
     * Parses the {@code last_updated} datetime string from the API response.
     * Only the date portion ({@code yyyy-MM-dd}) is extracted and stored.
     *
     * @param raw the raw string from the API (may be {@code null})
     * @return parsed {@link LocalDate}, or {@code null} if blank / unparseable
     */
    private LocalDate parseLastUpdated(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        try {
            return MudDateUtil.parseIso(datePart);
        } catch (Exception ex) {
            log.warn("BenzingaFirmService: could not parse last_updated='{}', storing null", raw);
            return null;
        }
    }
}