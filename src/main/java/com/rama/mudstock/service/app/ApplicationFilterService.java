package com.rama.mudstock.service.app;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rama.mudstock.enums.OptionStrategyEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;
import com.rama.mudstock.model.option.OptionContract;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.analyst.FirmAnalystQueryRepository;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionStrategyRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;
import com.rama.mudstock.service.DayStockMovementService;
import com.rama.mudstock.service.OptionDataContractService;
import com.rama.mudstock.service.WatchlistStockService;

@Service
public class ApplicationFilterService {

    private final StockRepository stockRepository;
    private final WatchlistStockService watchlistStockService;
    private final OptionContractRepository optionContractRepository;
    private final OptionStrategyRepository optionStrategyRepository;
    private final DayStockMovementService dayStockMovementService;
    private final OptionDataContractService optionDataContractService;
    private final FirmAnalystQueryRepository firmAnalystQueryRepository;

    public ApplicationFilterService(StockRepository stockRepository,
                                    WatchlistStockService watchlistStockService,
                                    OptionContractRepository optionContractRepository,
                                    OptionStrategyRepository optionStrategyRepository,
                                    DayStockMovementService dayStockMovementService,
                                    OptionDataContractService optionDataContractService,
                                    FirmAnalystQueryRepository firmAnalystQueryRepository) {
        this.stockRepository = stockRepository;
        this.watchlistStockService = watchlistStockService;
        this.optionContractRepository = optionContractRepository;
        this.optionStrategyRepository = optionStrategyRepository;
        this.dayStockMovementService = dayStockMovementService;
        this.optionDataContractService = optionDataContractService;
        this.firmAnalystQueryRepository = firmAnalystQueryRepository;
    }

    public List<Stock> listOptionAnalysisStocksSorted() {
        List<Stock> stocks = stockRepository.findAll();
        stocks.sort(Comparator.comparing(s -> s.getTicker() == null ? "" : s.getTicker(), String.CASE_INSENSITIVE_ORDER));
        return stocks;
    }

    public List<OptionContract> listOptionAnalysisContracts() {
        return optionContractRepository.getOptionContractsByStatus(List.of(), false, null);
    }

    public List<Stock> listUniqueContractTickers() {
        return optionContractRepository.listDistinctStocksByStatus();
    }

    public List<String> listOptionAnalysisContractSources() {
        return List.of(
            OptionSourceEnum.API.name(),
            OptionSourceEnum.FLAT_FILE.name(),
            OptionSourceEnum.BOTH.name());
    }

    public List<OptionContract> listOptionAnalysisActiveContracts() {
        return optionContractRepository.getOptionContractsByStatus(List.of(), false, null);
    }

    public String[] resolveContractTypeFilters(String contractType) {
        String normalized = contractType == null ? "" : contractType.trim().toUpperCase();
        if (normalized.isBlank() || "BOTH".equals(normalized)) {
            return new String[] { "PUT", "CALL" };
        }
        if ("PUT".equals(normalized) || "CALL".equals(normalized)) {
            return new String[] { normalized };
        }
        return new String[] { "PUT", "CALL" };
    }

    public String[] resolveContractSourceFilters(String source) {
        String normalized = source == null ? "" : source.trim().toUpperCase();
        if (normalized.isBlank() || "BOTH".equals(normalized)) {
            return new String[] { "API", "FLAT_FILE", "BOTH" };
        }
        if ("API".equals(normalized) || "FLAT_FILE".equals(normalized)) {
            return new String[] { normalized };
        }
        return new String[] { "API", "FLAT_FILE", "BOTH" };
    }

    public List<Stock> listOptionTrackingStocksSorted() {
        List<Stock> stocks = watchlistStockService.getAllSystemWatchlistStocks();
        stocks.sort(Comparator.comparing(s -> s.getTicker() == null ? "" : s.getTicker(), String.CASE_INSENSITIVE_ORDER));
        return stocks;
    }

    public Map<LocalDate, Long> listOptionTrackingExpirations(String ticker) {
        return optionDataContractService.findExpirationDatesForTicker(ticker);
    }

    public Map<String, Long> listOptionTrackingContractTickerIds(Long optionsIntervalAnalyseId) {
        return optionDataContractService.findContractTickerIdsForIntervalAnalyse(optionsIntervalAnalyseId);
    }

    public List<String> listDayStockMovementTickers() {
        return dayStockMovementService.listDistinctEntryTickers();
    }

    public AnalystRatingFilters analystRatingFilters() {
        return new AnalystRatingFilters(
            firmAnalystQueryRepository.listDistinctRatingTickers(),
            firmAnalystQueryRepository.listDistinctRatingAnalysts(),
            firmAnalystQueryRepository.listDistinctRatingFirms());
    }

    public List<Stock> listOptionStrategyStocksSorted() {
        List<Stock> stocks = stockRepository.findAll();
        stocks.sort(Comparator.comparing(s -> s.getTicker() == null ? "" : s.getTicker(), String.CASE_INSENSITIVE_ORDER));
        return stocks;
    }

    public List<Map<String, Object>> listOptionStrategyNameOptions() {
        return optionStrategyRepository.listStrategyNameOptions();
    }

    public OptionStrategyEnum.StrategyType[] listOptionStrategyTypes() {
        return OptionStrategyEnum.StrategyType.values();
    }

    public OptionStrategyEnum.StrategyMode[] listOptionStrategyModes() {
        return OptionStrategyEnum.StrategyMode.values();
    }

    public OptionStrategyEnum.Type[] listOptionStrategyEntryTypes() {
        return OptionStrategyEnum.Type.values();
    }

    public OptionStrategyEnum.StrategyStatus[] listOptionStrategyStatuses() {
        return OptionStrategyEnum.StrategyStatus.values();
    }

    public OptionTradeFilters optionTradeFilters(String contractStatus) {
        List<Map<String, Object>> strategyDefinitions = optionStrategyRepository.listActiveStrategyDefinitions();
        List<Map<String, Object>> strategyDefinitionLegs = optionStrategyRepository.listActiveStrategyDefinitionLegs();
        List<String> tickers = optionContractRepository.listDistinctTickersByStatus(contractStatus);
        List<LocalDate> expirationDates = optionContractRepository
            .listDistinctExpirationDatesByStatus(contractStatus);

        return new OptionTradeFilters(strategyDefinitions, strategyDefinitionLegs, tickers, expirationDates, null);
    }

    public record AnalystRatingFilters(List<String> tickers,
                                       List<String> analysts,
                                       List<String> firms) {
    }

    public record OptionTradeFilters(List<Map<String, Object>> strategyDefinitions,
                                     List<Map<String, Object>> strategyDefinitionLegs,
                                     List<String> tickers,
                                     List<LocalDate> expirationDates,
                                     Object contracts) {
    }
}
