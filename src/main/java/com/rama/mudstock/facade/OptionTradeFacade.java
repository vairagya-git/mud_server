package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionContractRepository;
import com.rama.mudstock.repository.option.OptionStrategyRepository;

@Service
public class OptionTradeFacade {

    private final OptionStrategyRepository optionStrategyRepository;
    private final OptionContractRepository optionContractRepository;

    public OptionTradeFacade(OptionStrategyRepository optionStrategyRepository,
                             OptionContractRepository optionContractRepository) {
        this.optionStrategyRepository = optionStrategyRepository;
        this.optionContractRepository = optionContractRepository;
    }

    public OptionTradeFilterData loadFilterData() {
        List<Map<String, Object>> strategyDefinitions = optionStrategyRepository.listActiveStrategyDefinitions();
        List<Map<String, Object>> strategyDefinitionLegs = optionStrategyRepository.listActiveStrategyDefinitionLegs();
        List<String> tickers = optionContractRepository.listDistinctTickersByStatus(OptionContractRepository.STATUS_ACTIVE);
        List<LocalDate> expirationDates = optionContractRepository
            .listDistinctExpirationDatesByStatus(OptionContractRepository.STATUS_ACTIVE);
        List<Map<String, Object>> contracts = optionContractRepository.listActiveContractsForSimulator();

        return new OptionTradeFilterData(strategyDefinitions, strategyDefinitionLegs, tickers, expirationDates, contracts);
    }

    public record OptionTradeFilterData(List<Map<String, Object>> strategyDefinitions,
                                        List<Map<String, Object>> strategyDefinitionLegs,
                                        List<String> tickers,
                                        List<LocalDate> expirationDates,
                                        List<Map<String, Object>> contracts) {
    }
}
