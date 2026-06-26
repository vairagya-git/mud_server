package com.rama.mudstock.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rama.mudstock.model.analyst.BenzingaAnalystRatingResponse;
import com.rama.mudstock.model.analyst.FirmAnalyst;
import com.rama.mudstock.model.analyst.Firm;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.analyst.FirmAnalystRepository;
import com.rama.mudstock.repository.analyst.FirmAnalystStockRatingRepository;
import com.rama.mudstock.repository.analyst.FirmRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;
import com.rama.mudstock.service.BenzingaFirmService;
import com.rama.mudstock.util.MudDateUtil;

/**
 * Facade that fetches analyst stock ratings from the Benzinga API and persists
 * them to the {@code firm_analyst_stock_rating} table.
 *
 * <p>Resolution order before inserting a rating row:</p>
 * <ol>
 *   <li>{@code firm_analyst_id} — looked up by {@code benzinga_analyst_id}; if missing,
 *       the analyst is fetched and saved via {@link FirmAnalystFacade#fetchAndSave}.</li>
 *   <li>{@code firm_id} — looked up by {@code benzinga_firm_id}; if missing,
 *       the firm is fetched and saved via {@link BenzingaFirmService#fetchByFirmId}.</li>
 *   <li>{@code stock_id} — looked up by ticker; if not found, the row is skipped.</li>
 * </ol>
 */
@Service
public class AnalystRatingFacade {

    private static final Logger log = LoggerFactory.getLogger(AnalystRatingFacade.class);

    private final BenzingaFirmService benzingaFirmService;
    private final FirmAnalystFacade firmAnalystFacade;
    private final FirmRepository firmRepository;
    private final FirmAnalystRepository firmAnalystRepository;
    private final StockRepository stockRepository;
    private final FirmAnalystStockRatingRepository ratingRepository;

    public AnalystRatingFacade(BenzingaFirmService benzingaFirmService,
                               FirmAnalystFacade firmAnalystFacade,
                               FirmRepository firmRepository,
                               FirmAnalystRepository firmAnalystRepository,
                               StockRepository stockRepository,
                               FirmAnalystStockRatingRepository ratingRepository) {
        this.benzingaFirmService = benzingaFirmService;
        this.firmAnalystFacade = firmAnalystFacade;
        this.firmRepository = firmRepository;
        this.firmAnalystRepository = firmAnalystRepository;
        this.stockRepository = stockRepository;
        this.ratingRepository = ratingRepository;
    }

    /**
     * Fetches analyst ratings for the given ticker and persists each result.
     *
     * @param ticker the stock ticker to fetch ratings for
     * @return number of ratings successfully upserted
     */
    public int fetchAndSaveForTicker(String ticker) {
        List<BenzingaAnalystRatingResponse> ratings = benzingaFirmService.fetchAnalystRatings(ticker);
        int saved = 0;
        for (BenzingaAnalystRatingResponse rating : ratings) {
            try {
                save(rating);
                saved++;
            } catch (Exception ex) {
                log.error("AnalystRatingFacade: error saving rating for ticker={} date={}: {}",
                        ticker, rating.getDate(), ex.getMessage());
            }
        }
        return saved;
    }

    /**
     * Persists a single {@link BenzingaAnalystRatingResponse}, resolving all foreign keys first.
     */
    public void save(BenzingaAnalystRatingResponse rating) {
        Long firmAnalystId = resolveFirmAnalystId(rating.getBenzingaAnalystId());
        if (firmAnalystId == null) {
            log.warn("AnalystRatingFacade: skipping rating — could not resolve firm_analyst for benzingaAnalystId={}",
                    rating.getBenzingaAnalystId());
            return;
        }

        Long firmId = resolveFirmId(rating.getBenzingaFirmId());
        if (firmId == null) {
            log.warn("AnalystRatingFacade: skipping rating — could not resolve firm for benzingaFirmId={}",
                    rating.getBenzingaFirmId());
            return;
        }

        Long stockId = resolveStockId(rating.getTicker());
        if (stockId == null) {
            log.warn("AnalystRatingFacade: skipping rating — ticker={} not found in stock table",
                    rating.getTicker());
            return;
        }

        LocalDate lastUpdated = parseDate(rating.getLastUpdated());
        LocalDate date = parseDate(rating.getDate());

        if (date == null) {
            log.warn("AnalystRatingFacade: skipping rating — date is null for ticker={} analystId={}",
                    rating.getTicker(), rating.getBenzingaAnalystId());
            return;
        }

        ratingRepository.upsert(
                firmAnalystId,
                firmId,
                stockId,
                rating.getRatingAction(),
                rating.getPriceTargetAction(),
                rating.getRating(),
                rating.getPreviousRating(),
                toBigDecimal(rating.getPriceTarget()),
                toBigDecimal(rating.getPreviousPriceTarget()),
                toBigDecimal(rating.getPricePercentChange()),
                toBigDecimal(rating.getAdjustedPriceTarget()),
                toBigDecimal(rating.getPreviousAdjustedPriceTarget()),
                rating.getImportance(),
                lastUpdated,
                date,
                rating.getBenzingaCalendarUrl(),
                rating.getBenzingaNewsUrl());

        log.info("AnalystRatingFacade: upserted rating ticker={} analyst={} date={}",
                rating.getTicker(), rating.getBenzingaAnalystId(), date);
    }

    // -----------------------------------------------------------------------
    // Resolution helpers
    // -----------------------------------------------------------------------

    private Long resolveFirmAnalystId(String benzingaAnalystId) {
        if (benzingaAnalystId == null || benzingaAnalystId.isBlank()) return null;

        Optional<FirmAnalyst> existing = firmAnalystRepository
                .findByBenzingaAnalystId(benzingaAnalystId);
        if (existing.isPresent()) return existing.get().getId();

        log.info("AnalystRatingFacade: analyst not found for benzingaAnalystId={}, fetching from API", benzingaAnalystId);
        firmAnalystFacade.fetchAndSave(benzingaAnalystId);

        return firmAnalystRepository.findByBenzingaAnalystId(benzingaAnalystId)
                .map(FirmAnalyst::getId)
                .orElse(null);
    }

    private Long resolveFirmId(String benzingaFirmId) {
        if (benzingaFirmId == null || benzingaFirmId.isBlank()) return null;

        Optional<Firm> existing = firmRepository.findByBenzingaFirmId(benzingaFirmId);
        if (existing.isPresent()) return existing.get().getId();

        log.info("AnalystRatingFacade: firm not found for benzingaFirmId={}, fetching from API", benzingaFirmId);
        benzingaFirmService.fetchByFirmId(benzingaFirmId);

        return firmRepository.findByBenzingaFirmId(benzingaFirmId)
                .map(Firm::getId)
                .orElse(null);
    }

    private Long resolveStockId(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        return stockRepository.findByTicker(ticker).map(Stock::getId).orElse(null);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        try {
            return MudDateUtil.parseIso(datePart);
        } catch (Exception ex) {
            log.warn("AnalystRatingFacade: could not parse date='{}', storing null", raw);
            return null;
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
