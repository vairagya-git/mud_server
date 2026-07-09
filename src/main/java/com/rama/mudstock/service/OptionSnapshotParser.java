package com.rama.mudstock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OptionSnapshotParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<OptionContractData> parseContracts(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        List<OptionContractData> contracts = new ArrayList<>();

        if (root.has("details")) {
            addContract(root.get("details"), contracts);
        }

        if (root.has("results")) {
            JsonNode results = root.get("results");
            if (results.isArray()) {
                for (JsonNode result : results) {
                    addContract(result.has("details") ? result.get("details") : result, contracts);
                }
            } else if (results.isObject()) {
                addContract(results.has("details") ? results.get("details") : results, contracts);
            }
        }

        return contracts;
    }

    private void addContract(JsonNode detailsNode, List<OptionContractData> contracts) {
        if (detailsNode == null || detailsNode.isMissingNode() || !detailsNode.isObject()) {
            return;
        }

        String contractType = normalizeUpper(detailsNode.path("contract_type").asText(null));
        String expirationDateRaw = detailsNode.path("expiration_date").asText(null);
        String contractTicker = detailsNode.path("ticker").asText(null);

        if (contractType == null || expirationDateRaw == null || contractTicker == null || contractTicker.isBlank()) {
            return;
        }

        contracts.add(new OptionContractData(
            contractType,
            blankToNull(detailsNode.path("exercise_style").asText(null)),
            LocalDate.parse(expirationDateRaw),
            detailsNode.hasNonNull("strike_price") ? new BigDecimal(detailsNode.get("strike_price").asText()) : BigDecimal.ZERO,
            detailsNode.path("shares_per_contract").asInt(100),
            contractTicker));
    }

    private String normalizeUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record OptionContractData(String contractType,
                                     String exerciseStyle,
                                     LocalDate expirationDate,
                                     BigDecimal strikePrice,
                                     int sharesPerContract,
                                     String contractTicker) {
    }
}