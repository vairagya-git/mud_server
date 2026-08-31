package com.rama.mudstock.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.model.option.OptionContract;
import com.rama.mudstock.model.option.OptionsInternalAnalyseEntity;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionIntervalAnalyseRepository;
import com.rama.mudstock.repository.option.OptionSnapshotFlatfileRepository;
import com.rama.mudstock.repository.option.OptionSnapshotIVMetricRepository;
import com.rama.mudstock.repository.option.OptionSnapshotRepository;
import com.rama.mudstock.service.app.ApplicationFilterService;

@Service
public class OptionContractAnalysisFacade {

    private final OptionIntervalAnalyseRepository optionIntervalAnalyseRepository;
    private final OptionContractRepository optionContractRepository;
    private final OptionSnapshotRepository optionSnapshotRepository;
    private final OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository;
    private final OptionSnapshotIVMetricRepository optionSnapshotIVMetricRepository;
    private final ApplicationProperties applicationProperties;
    private final ApplicationFilterService applicationFilterService;

    public OptionContractAnalysisFacade(OptionIntervalAnalyseRepository optionIntervalAnalyseRepository,
                                        OptionContractRepository optionContractRepository,
                                        OptionSnapshotRepository optionSnapshotRepository,
                                        OptionSnapshotFlatfileRepository optionSnapshotFlatfileRepository,
                                        OptionSnapshotIVMetricRepository optionSnapshotIVMetricRepository,
                                        ApplicationProperties applicationProperties,
                                        ApplicationFilterService applicationFilterService) {
        this.optionIntervalAnalyseRepository = optionIntervalAnalyseRepository;
        this.optionContractRepository = optionContractRepository;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.optionSnapshotFlatfileRepository = optionSnapshotFlatfileRepository;
        this.optionSnapshotIVMetricRepository = optionSnapshotIVMetricRepository;
        this.applicationProperties = applicationProperties;
        this.applicationFilterService = applicationFilterService;
    }

    public List<OptionsInternalAnalyseEntity> listAnalyseEntries() {
        return optionIntervalAnalyseRepository.getOptionsInternalAnalyseByStatus(null);
    }

    public int createAnalyseEntry(Long stockId,
                                  String contractType,
                                  String source,
                                  String status,
                                  LocalDate expirationDate,
                                  BigDecimal strikeFrom,
                                  BigDecimal strikeTo,
                                  BigDecimal interval) {
        return optionIntervalAnalyseRepository.insert(
            stockId,
            contractType,
            source,
            status,
            expirationDate,
            strikeFrom,
            strikeTo,
            interval);
    }

    public Map<String, Object> findAnalyseEntryById(Long id) {
        return optionIntervalAnalyseRepository.findByIdWithTicker(id);
    }

    public int updateAnalyseEntry(Long id,
                                  Long stockId,
                                  String contractType,
                                  String status,
                                  LocalDate expirationDate,
                                  BigDecimal strikeFrom,
                                  BigDecimal strikeTo,
                                  BigDecimal interval) {
        return optionIntervalAnalyseRepository.updateById(
            id,
            stockId,
            contractType,
            status,
            expirationDate,
            strikeFrom,
            strikeTo,
            interval);
    }

    public int updateAnalyseStatus(Long id, String status) {
        return optionIntervalAnalyseRepository.updateStatusById(id, status);
    }

    public List<OptionContract> listContracts() {
        return applicationFilterService.listOptionAnalysisContracts();
    }

    public OptionContractFilterContainer loadOptionContractFilterContainer() {
        List<Stock> contractTickers = applicationFilterService.listUniqueContractTickers();
        List<String> contractSources = applicationFilterService.listOptionAnalysisContractSources();
        List<OptionContract> contractExpirations = optionContractRepository.getOptionContractExpirationForStock(null);
        long snapshotRefreshIntervalMs = applicationProperties.getSnapshotRefreshMs();

        return new OptionContractFilterContainer(
            contractTickers,
            contractSources,
            contractExpirations,
            snapshotRefreshIntervalMs);
    }

    public List<Stock> listUniqueContractTickers() {
        return applicationFilterService.listUniqueContractTickers();
    }

    public List<String> listContractSources() {
        return applicationFilterService.listOptionAnalysisContractSources();
    }

    public List<OptionContract> listSnapshotContracts() {
        return applicationFilterService.listOptionAnalysisActiveContracts();
    }

    public List<OptionContract> listContractExpirations() {
        return optionContractRepository.getOptionContractExpirationForStock(null);
    }

    public long snapshotRefreshIntervalMs() {
        return applicationProperties.getSnapshotRefreshMs();
    }

    public List<Map<String, Object>> listMetrics() {
        return optionSnapshotIVMetricRepository.listAllWithTickerAndContract();
    }

    public List<OptionContract> listStrikeOptions(Long stockId, LocalDate expirationDate) {
        return optionContractRepository.getOptionContractStrikeForStockAndExpiration(stockId, expirationDate);
    }

    public List<OptionContract> listContractsWithSnapshotFlatFileForStrike(Long stockId,
                                                                           LocalDate expirationDate,
                                                                           BigDecimal strikePrice) {
        List<OptionContract> contracts = optionContractRepository.getOptionContractsForStockExpirationStrike(stockId, expirationDate, strikePrice);
        for (OptionContract contract : contracts) {
            if (contract.getId() == null) {
                continue;
            }
            contract.setOptionSnapshots(optionSnapshotRepository.listOptionSnapshotsByContractId(contract.getId()));
            contract.setOptionFlatFiles(optionSnapshotFlatfileRepository.listByContractId(contract.getId()));
        }
        return contracts;
    }

    public List<OptionContract> listContractsForContractSelection(Long stockId,
                                                                  LocalDate expirationDate,
                                                                  BigDecimal strikePrice,
                                                                  String contractType,
                                                                  String source) {
        String[] contractTypeFilters = applicationFilterService.resolveContractTypeFilters(contractType);
        String[] sourceFilters = applicationFilterService.resolveContractSourceFilters(source);
        return optionContractRepository.getOptionContractsForSelection(stockId, expirationDate, strikePrice, contractTypeFilters, sourceFilters);
    }

    public List<Stock> listOptionAnalysisStocksSorted() {
        return applicationFilterService.listOptionAnalysisStocksSorted();
    }

    public record OptionContractFilterContainer(List<Stock> contractTickers,
                                                List<String> contractSources,
                                                List<OptionContract> contractExpirations,
                                                long snapshotRefreshIntervalMs) {
    }
}
