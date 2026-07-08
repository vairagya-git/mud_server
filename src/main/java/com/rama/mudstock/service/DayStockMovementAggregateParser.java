package com.rama.mudstock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DayStockMovementAggregateParser {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public int extractResultsCount(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return extractResultsCount(root);
    }

    public Optional<AggregateSnapshot> parseAggregate(String responseBody,
                                                      LocalDate eventDate,
                                                      LocalDate previousMarketDate) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!root.has("results") || !root.get("results").isArray()) {
            return Optional.empty();
        }

        JsonNode previousDayNode = null;
        JsonNode currentDayNode = null;
        for (JsonNode node : root.get("results")) {
            if (!node.has("t")) {
                continue;
            }
            long timestamp = node.get("t").asLong();
            LocalDate resultDate = Instant.ofEpochMilli(timestamp).atZone(UTC).toLocalDate();
            if (resultDate.equals(eventDate)) {
                currentDayNode = node;
            } else if (resultDate.equals(previousMarketDate)) {
                previousDayNode = node;
            }
        }

        if (currentDayNode == null || previousDayNode == null) {
            return Optional.empty();
        }

        double preDayClose = previousDayNode.has("c") ? previousDayNode.get("c").asDouble() : 0.0;
        double curDayOpen = currentDayNode.has("o") ? currentDayNode.get("o").asDouble() : 0.0;
        double curDayClose = currentDayNode.has("c") ? currentDayNode.get("c").asDouble() : 0.0;
        double curDayHigh = currentDayNode.has("h") ? currentDayNode.get("h").asDouble() : 0.0;
        double curDayLow = currentDayNode.has("l") ? currentDayNode.get("l").asDouble() : 0.0;
        double curDayVolWeight = currentDayNode.has("vw") ? currentDayNode.get("vw").asDouble() : 0.0;
        long curDayVolume = currentDayNode.has("v") ? currentDayNode.get("v").asLong() : 0L;

        Double changePercent = null;
        Double dayOpeningChangePercent = null;
        if (preDayClose != 0.0) {
            changePercent = toPercent(curDayClose, preDayClose);
            dayOpeningChangePercent = toPercent(curDayOpen, preDayClose);
        }

        return Optional.of(new AggregateSnapshot(
            preDayClose,
            curDayOpen,
            curDayClose,
            curDayHigh,
            curDayLow,
            curDayVolWeight,
            curDayVolume,
            changePercent,
            dayOpeningChangePercent));
    }

    private int extractResultsCount(JsonNode root) {
        if (root.has("resultsCount")) {
            return root.get("resultsCount").asInt();
        }
        if (root.has("results") && root.get("results").isArray()) {
            return root.get("results").size();
        }
        return 0;
    }

    private Double toPercent(double currentValue, double previousValue) {
        BigDecimal raw = BigDecimal.valueOf(currentValue)
            .subtract(BigDecimal.valueOf(previousValue))
            .divide(BigDecimal.valueOf(previousValue), 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        return raw.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record AggregateSnapshot(double preDayClose,
                                    double curDayOpen,
                                    double curDayClose,
                                    double curDayHigh,
                                    double curDayLow,
                                    double curDayVolWeight,
                                    long curDayVolume,
                                    Double changePercent,
                                    Double dayOpeningChangePercent) {
    }
}