package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.analyst.BenzingaAnalystResponse;
import com.rama.mudstock.model.analyst.BenzingaFirmResponse;
import com.rama.mudstock.model.analyst.Firm;
import com.rama.mudstock.repository.analyst.FirmAnalystRepository;
import com.rama.mudstock.repository.analyst.FirmRepository;
import com.rama.mudstock.service.BenzingaFirmService;
import com.rama.mudstock.util.DataConversionUtil;
import com.rama.mudstock.util.MudDateUtil;

/**
 * Facade that fetches analyst data from the Benzinga API and persists it to the
 * {@code firm_analyst} table.
 *
 * <p>If the referenced firm ({@code benzinga_firm_id}) does not yet exist in the
 * {@code firm} table, it is fetched via {@link BenzingaFirmService#fetchByFirmId}
 * and inserted before the analyst row is written.</p>
 */
@Service
public class FirmAnalystFacade {

    private static final Logger log = LoggerFactory.getLogger(FirmAnalystFacade.class);

    private final BenzingaFirmService benzingaFirmService;
    private final FirmRepository firmRepository;
    private final FirmAnalystRepository firmAnalystRepository;

    public FirmAnalystFacade(BenzingaFirmService benzingaFirmService,
                             FirmRepository firmRepository,
                             FirmAnalystRepository firmAnalystRepository) {
        this.benzingaFirmService = benzingaFirmService;
        this.firmRepository = firmRepository;
        this.firmAnalystRepository = firmAnalystRepository;
    }

    /**
     * Fetches the analyst with the given Benzinga analyst ID from the API and
     * upserts the result into {@code firm_analyst}.
     *
     * <p>If the analyst's firm is not yet in the {@code firm} table it is
     * fetched and persisted first.</p>
     *
     * @param benzingaAnalystId the Benzinga analyst ID (e.g. {@code 5f634eb692f07400010e2bcc})
     * @return the persisted analyst response, or {@code null} if the analyst could not be fetched
     */
    public BenzingaAnalystResponse fetchAndSave(String benzingaAnalystId) {
        BenzingaAnalystResponse analyst = benzingaFirmService.fetchAnalystById(benzingaAnalystId);
        if (analyst == null) {
            log.warn("FirmAnalystFacade: no analyst returned for benzingaAnalystId={}", benzingaAnalystId);
            return null;
        }
        save(analyst);
        return analyst;
    }

    /**
     * Persists an already-fetched {@link BenzingaAnalystResponse} into {@code firm_analyst}.
     * Ensures the referenced firm exists first.
     *
     * @param analyst the analyst DTO to persist
     */
    public void save(BenzingaAnalystResponse analyst) {
        Long firmId = resolveFirmId(analyst.getBenzingaFirmId());
        if (firmId == null) {
            log.error("FirmAnalystFacade: cannot persist analyst={} — firm could not be resolved for benzingaFirmId={}",
                    analyst.getBenzingaId(), analyst.getBenzingaFirmId());
            return;
        }

        LocalDate lastUpdated = parseDate(analyst.getLastUpdated());

        firmAnalystRepository.upsert(
                firmId,
                analyst.getBenzingaId(),
                analyst.getBenzingaFirmId(),
                analyst.getFullName(),
                lastUpdated,
                DataConversionUtil.toBigDecimal(analyst.getOverallAvgReturn()),
                DataConversionUtil.toBigDecimal(analyst.getOverallAvgReturnPercentile()),
                DataConversionUtil.toBigDecimal(analyst.getOverallSuccessRate()),
                DataConversionUtil.toBigDecimal(analyst.getSmartScore()),
                DataConversionUtil.toBigDecimal(analyst.getTotalRatings()),
                DataConversionUtil.toBigDecimal(analyst.getTotalRatingsPercentile()));

        log.info("FirmAnalystFacade: upserted firm_analyst benzingaAnalystId={} firmId={}",
                analyst.getBenzingaId(), firmId);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    /**
     * Returns the {@code firm.id} for the given {@code benzingaFirmId}.
     * If the firm is not yet in the database, fetches it from the API first.
     */
    private Long resolveFirmId(String benzingaFirmId) {
        if (benzingaFirmId == null || benzingaFirmId.isBlank()) {
            log.warn("FirmAnalystFacade: benzingaFirmId is blank, cannot resolve firm");
            return null;
        }

        Optional<Firm> existing = firmRepository.findByBenzingaFirmId(benzingaFirmId);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        log.info("FirmAnalystFacade: firm not found for benzingaFirmId={}, fetching from API", benzingaFirmId);
        BenzingaFirmResponse firmResponse = benzingaFirmService.fetchByFirmId(benzingaFirmId);
        if (firmResponse == null) {
            log.error("FirmAnalystFacade: API returned no firm for benzingaFirmId={}", benzingaFirmId);
            return null;
        }

        // fetchByFirmId already upserts the firm row; look it up now
        return firmRepository.findByBenzingaFirmId(benzingaFirmId)
                .map(Firm::getId)
                .orElse(null);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        try {
            return MudDateUtil.parseIso(datePart);
        } catch (Exception ex) {
            log.warn("FirmAnalystFacade: could not parse date='{}', storing null", raw);
            return null;
        }
    }

}
