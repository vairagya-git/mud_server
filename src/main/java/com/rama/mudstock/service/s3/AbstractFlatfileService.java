package com.rama.mudstock.service.s3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.springframework.util.StringUtils;

import com.rama.mudstock.config.ApplicationConfig;
import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.model.option.TickerOptionSnapshotData;
import com.rama.mudstock.model.option.TickerStockSnapshotData;

public abstract class AbstractFlatfileService {

    private static final DateTimeFormatter YYYY = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MM = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter PORTUGAL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    protected final ApplicationProperties.S3Flatfiles properties;

    protected AbstractFlatfileService(ApplicationProperties applicationProperties) {
        this.properties = applicationProperties.getS3Flatfiles();
    }

    public Map<String, List<TickerOptionSnapshotData>> loadOptionRowsDaysData(LocalDate date) {
        String key = buildObjectKey(date, properties.getMinuteAgg());
        Map<String, List<TickerOptionSnapshotData>> rowsByTicker = new LinkedHashMap<>();

        try (InputStream rawInput = openDailyInputStream(date, properties.getMinuteAgg(), key);
               BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(rawInput), StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    continue;
                }

                String rowTicker = parts[0].trim();
                Long windowStart = parseLong(parts[6]);
                if (windowStart == null) {
                    continue;
                }

                TickerOptionSnapshotData row = new TickerOptionSnapshotData(
                    rowTicker,
                    parseInteger(parts[1]),
                    parseBigDecimal(parts[2]),
                    parseBigDecimal(parts[3]),
                    parseBigDecimal(parts[4]),
                    parseBigDecimal(parts[5]),
                    windowStart,
                    parseInteger(parts[7]));

                rowsByTicker.computeIfAbsent(rowTicker, k -> new ArrayList<>()).add(row);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read " + sourceName() + " object '" + key + "': " + ex.getMessage(), ex);
        }

        return rowsByTicker;
    }

    public Map<String, Map<Long, TickerStockSnapshotData>> loadStockRowsDaysData(LocalDate date) {
        String stockMinuteAgg = properties.getStockMinuteAgg();
        if (!StringUtils.hasText(stockMinuteAgg)) {
            throw new IllegalStateException("s3-flatfiles.stock-minute-agg is required");
        }
        String key = buildObjectKey(date, stockMinuteAgg);
        Map<String, Map<Long, TickerStockSnapshotData>> rowsByTicker = new LinkedHashMap<>();

        try (InputStream rawInput = openDailyInputStream(date, stockMinuteAgg, key);
               BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(rawInput), StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    continue;
                }

                String rowTicker = parts[0].trim();
                Long windowStart = parseLong(parts[6]);
                if (windowStart == null) {
                    continue;
                }

                TickerStockSnapshotData row = new TickerStockSnapshotData(
                    rowTicker,
                    parseInteger(parts[1]),
                    parseBigDecimal(parts[2]),
                    parseBigDecimal(parts[3]),
                    parseBigDecimal(parts[4]),
                    parseBigDecimal(parts[5]),
                    windowStart,
                    parseInteger(parts[7]));

                rowsByTicker
                    .computeIfAbsent(rowTicker, k -> new LinkedHashMap<>())
                    .put(windowStart, row);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read " + sourceName() + " object '" + key + "': " + ex.getMessage(), ex);
        }

        return rowsByTicker;
    }

    public Map<String, Object> fetchCsvOptRows(String fileLocation, String sortBy, String sortDirection, int limit) {
        FlatfileLocation location = resolveLocation(fileLocation);
        List<Map<String, Object>> records = new ArrayList<>();

        try (InputStream rawInput = openLocationInputStream(location);
               BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(rawInput), StandardCharsets.UTF_8))) {

            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ticker", parts[0].trim());
                row.put("volume", parts[1]);
                row.put("open", parts[2]);
                row.put("close", parts[3]);
                row.put("high", parts[4]);
                row.put("low", parts[5]);
                row.put("window_start", parts[6]);
                row.put("portugal_time", toPortugalTime(parts[6]));
                row.put("transactions", parts[7]);
                records.add(row);
            }

            records.sort(buildCsvOptComparator(sortBy, sortDirection));
            if (records.size() > limit) {
                records = new ArrayList<>(records.subList(0, limit));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read csv opt file '" + location.original() + "': " + ex.getMessage(), ex);
        }

        Map<String, Object> result = new LinkedHashMap<>(buildLocationInfo(location));
        result.put("fileLocation", location.original());
        result.put("objectKey", location.key());
        result.put("sortBy", normalizeSortBy(sortBy));
        result.put("sortDirection", normalizeSortDirection(sortDirection));
        result.put("recordCount", records.size());
        result.put("records", records);
        return result;
    }

    public Map<String, Object> fetchBucketUpdateTimestamp(LocalDate date) {
        String key = buildObjectKey(date);
        return buildUpdateTimestampResult(key);
    }

    protected String buildObjectKey(LocalDate date) {
        return buildObjectKey(date, properties.getMinuteAgg());
    }

    protected String buildObjectKey(LocalDate date, String minuteAggPrefix) {
        String pattern = properties.getFileLocationPattern();
        String path = pattern
            .replace("YYYY", date.format(YYYY))
            .replace("MM", date.format(MM))
            .replace("DD", date.format(DD));
        return trimSlashes(minuteAggPrefix) + "/" + trimSlashes(path);
    }

    protected FlatfileLocation resolveLocation(String fileLocation) {
        String raw = fileLocation == null ? "" : fileLocation.trim();
        if (raw.startsWith("s3://")) {
            String remainder = raw.substring(5);
            int slashIndex = remainder.indexOf('/');
            if (slashIndex < 0) {
                throw new IllegalArgumentException("Invalid s3 file location: " + raw);
            }
            String bucket = trimSlashes(remainder.substring(0, slashIndex));
            String key = trimSlashes(remainder.substring(slashIndex + 1));
            return new FlatfileLocation(bucket, key, raw);
        }

        return new FlatfileLocation(trimSlashes(defaultBucket()), trimSlashes(raw), raw);
    }

    protected static String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    protected static String normalizeTicker(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    protected String toPortugalTime(String rawWindowStart) {
        String value = rawWindowStart == null ? "" : rawWindowStart.trim();
        if (value.isEmpty()) {
            return "";
        }

        try {
            long parsed = Long.parseLong(value);
            Instant instant = value.length() > 10 ? Instant.ofEpochMilli(parsed) : Instant.ofEpochSecond(parsed);
            return instant.atZone(ApplicationConfig.LISBON).format(PORTUGAL_TIME_FORMAT);
        } catch (Exception ignored) {
        }

        try {
            return Instant.parse(value).atZone(ApplicationConfig.LISBON).format(PORTUGAL_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ApplicationConfig.LISBON).format(PORTUGAL_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(ApplicationConfig.LISBON).format(PORTUGAL_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value).atZone(ApplicationConfig.LISBON).format(PORTUGAL_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        return value;
    }

    protected Comparator<Map<String, Object>> buildCsvOptComparator(String sortBy, String sortDirection) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        boolean descending = "desc".equalsIgnoreCase(normalizeSortDirection(sortDirection));

        Comparator<Map<String, Object>> comparator = switch (normalizedSortBy) {
            case "ticker" -> Comparator.comparing(row -> textValue(row, "ticker"), String.CASE_INSENSITIVE_ORDER);
            case "volume", "transactions" -> Comparator.comparingLong(row -> longValue(row, normalizedSortBy));
            case "open", "close", "high", "low" -> Comparator.comparingDouble(row -> doubleValue(row, normalizedSortBy));
            case "portugal_time" -> Comparator.comparing(row -> textValue(row, "portugal_time"), String.CASE_INSENSITIVE_ORDER);
            case "window_start" -> Comparator.comparingLong(AbstractFlatfileService::windowStartEpoch);
            default -> Comparator.comparing(row -> textValue(row, "window_start"), String.CASE_INSENSITIVE_ORDER);
        };

        if (descending) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(row -> textValue(row, "ticker"), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(row -> textValue(row, "window_start"), String.CASE_INSENSITIVE_ORDER);
    }

    protected static String normalizeSortBy(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ticker", "volume", "open", "close", "high", "low", "window_start", "portugal_time", "transactions" -> normalized;
            default -> "window_start";
        };
    }

    protected static String normalizeSortDirection(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "desc".equals(normalized) ? "desc" : "asc";
    }

    protected static String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    protected static long longValue(Map<String, Object> row, String key) {
        try {
            return Long.parseLong(textValue(row, key).replaceAll("[^0-9-]", ""));
        } catch (Exception ex) {
            return Long.MIN_VALUE;
        }
    }

    protected static double doubleValue(Map<String, Object> row, String key) {
        try {
            return Double.parseDouble(textValue(row, key).replaceAll("[^0-9.\\-]", ""));
        } catch (Exception ex) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    protected static long windowStartEpoch(Map<String, Object> row) {
        String value = textValue(row, "window_start").trim();
        if (value.isEmpty()) {
            return Long.MIN_VALUE;
        }
        try {
            return value.length() > 10 ? Long.parseLong(value) : Long.parseLong(value) * 1000L;
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).atZone(ApplicationConfig.LISBON).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        return Long.MIN_VALUE;
    }

    protected static Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    protected static Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    protected static BigDecimal parseBigDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    protected String localRootOrThrow() {
        String root = properties.getLocalDirectory();
        if (!StringUtils.hasText(root)) {
            root = properties.getLocalFilePath();
        }
        if (!StringUtils.hasText(root)) {
            throw new IllegalStateException("s3-flatfiles.local-directory is required for local read mode");
        }
        return root.trim();
    }

    protected abstract String sourceName();

    protected abstract String defaultBucket();

    protected abstract InputStream openDailyInputStream(LocalDate date, String minuteAggPrefix, String key) throws Exception;

    protected abstract InputStream openLocationInputStream(FlatfileLocation location) throws Exception;

    protected abstract Map<String, Object> buildLocationInfo(FlatfileLocation location);

    protected abstract Map<String, Object> buildUpdateTimestampResult(String key);

    protected record FlatfileLocation(String bucket, String key, String original) {
    }
}
