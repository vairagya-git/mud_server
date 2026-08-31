package com.rama.mudstock.service.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rama.mudstock.repository.option.OptionTradeRepository;

@Service
public class ApplicationMetaDataService {

    public List<String> listOptionTradeModes() {
        return Arrays.stream(OptionTradeRepository.TradeMode.values())
            .map(Enum::name)
            .toList();
    }

    public OptionTradeRepository.TradeMode resolveOptionTradeMode(String tradeMode) {
        String normalizedTradeMode = tradeMode == null ? "" : tradeMode.trim().toUpperCase();
        if (normalizedTradeMode.isBlank()) {
            throw new IllegalArgumentException("trade_mode is required");
        }

        try {
            return OptionTradeRepository.TradeMode.valueOf(normalizedTradeMode);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("trade_mode must be one of: " + String.join(", ", listOptionTradeModes()));
        }
    }
}
