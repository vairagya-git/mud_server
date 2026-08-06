package com.rama.mudstock.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.rama.mudstock.service.ApplicationFilterService;

@Service
public class OptionTradeFacade {

    private final ApplicationFilterService applicationFilterService;

    public OptionTradeFacade(ApplicationFilterService applicationFilterService) {
        this.applicationFilterService = applicationFilterService;
    }

    public OptionTradeFilterData loadFilterData() {
        ApplicationFilterService.OptionTradeFilters filters = applicationFilterService.optionTradeFilters();

        return new OptionTradeFilterData(
            filters.strategyDefinitions(),
            filters.strategyDefinitionLegs(),
            filters.tickers(),
            filters.expirationDates(),
            filters.contracts());
    }

    public record OptionTradeFilterData(List<Map<String, Object>> strategyDefinitions,
                                        List<Map<String, Object>> strategyDefinitionLegs,
                                        List<String> tickers,
                                        List<LocalDate> expirationDates,
                                        List<Map<String, Object>> contracts) {
    }
}
