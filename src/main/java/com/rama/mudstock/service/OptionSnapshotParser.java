package com.rama.mudstock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OptionSnapshotParser {

    private static final Logger log = LoggerFactory.getLogger(OptionSnapshotParser.class);

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

    public OptionSnapshotData parseSnapshotData(String responseBody, String expectedContractTicker) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode payload = root;

        if (root.has("results") && root.get("results").isObject()) {
            payload = root.get("results");
        } else if (root.has("results") && root.get("results").isArray()) {
            payload = findMatchingSnapshot(root.get("results"), expectedContractTicker);
        }

        JsonNode lastQuote = payload.path("last_quote");
        JsonNode lastTrade = payload.path("last_trade");
        JsonNode underlyingAsset = payload.path("underlying_asset");
        JsonNode greeks = payload.path("greeks");
        JsonNode day = payload.path("day");

        return new OptionSnapshotData(
            decimal(payload, "break_even_price"),
            decimal(payload, "implied_volatility"),
            integer(payload, "open_interest"),

            decimal(underlyingAsset, "price"),
            decimal(underlyingAsset, "change_to_break_even"),
            text(underlyingAsset, "timeframe"),
            longValue(underlyingAsset, "last_updated"),

            decimal(lastQuote, "bid"),
            decimal(lastQuote, "ask"),
            decimal(lastQuote, "midpoint"),
            integer(lastQuote, "bid_size"),
            integer(lastQuote, "ask_size"),
            text(lastQuote, "timeframe"),
            longValue(lastQuote, "last_updated"),

            decimal(lastTrade, "price"),
            integer(lastTrade, "size"),
            longValue(lastTrade, "sip_timestamp"),

            decimal(greeks, "delta"),
            decimal(greeks, "gamma"),
            decimal(greeks, "theta"),
            decimal(greeks, "vega"),

            integer(day, "volume"));
    }

    private JsonNode findMatchingSnapshot(JsonNode results, String expectedContractTicker) {
        if (results == null || !results.isArray()) {
            return results;
        }

        if (expectedContractTicker != null && !expectedContractTicker.isBlank()) {
            List<String> availableTickers = new ArrayList<>();
            for (JsonNode result : results) {
                String ticker = result.path("details").path("ticker").asText(null);
                if (ticker != null && !ticker.isBlank()) {
                    availableTickers.add(ticker);
                }
                if (expectedContractTicker.equalsIgnoreCase(ticker)) {
                    return result;
                }
            }

            log.warn("OptionSnapshotParser: no matching details.ticker found for expectedContractTicker={}; availableTickers={}",
                expectedContractTicker,
                availableTickers);
        }

        return results.isEmpty() ? results : results.get(0);
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

    private BigDecimal decimal(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.hasNonNull(field)) {
            return null;
        }
        return new BigDecimal(node.get(field).asText());
    }

    private Integer integer(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asInt();
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long longValue(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asLong();
    }

    public record OptionContractData(String contractType,
                                     String exerciseStyle,
                                     LocalDate expirationDate,
                                     BigDecimal strikePrice,
                                     int sharesPerContract,
                                     String contractTicker) {
    }

    public record OptionSnapshotData(BigDecimal breakEvenPrice,
                                     BigDecimal impliedVolatility,
                                     Integer openInterest,
                                     BigDecimal underlyingPrice,
                                     BigDecimal changeToBreakEven,
                                     String underlyingTimeframe,
                                     Long underlyingLastUpdated,
                                     BigDecimal bid,
                                     BigDecimal ask,
                                     BigDecimal midpoint,
                                     Integer bidSize,
                                     Integer askSize,
                                     String quoteTimeframe,
                                     Long quoteLastUpdated,
                                     BigDecimal lastTradePrice,
                                     Integer lastTradeSize,
                                     Long tradeSipTimestamp,
                                     BigDecimal delta,
                                     BigDecimal gamma,
                                     BigDecimal theta,
                                     BigDecimal vega,
                                     Integer dayVolume) {
    }
}