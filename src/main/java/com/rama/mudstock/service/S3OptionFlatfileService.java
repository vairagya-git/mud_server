package com.rama.mudstock.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rama.mudstock.config.ApplicationProperties;
import com.rama.mudstock.config.ApplicationConfig;
import java.math.BigDecimal;
import com.rama.mudstock.model.option.TickerOptionSnapshotData;
import com.rama.mudstock.model.option.TickerStockSnapshotData;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class S3OptionFlatfileService {

    private static final String STOCK_MINUTE_AGG = "us_stocks_sip/minute_aggs_v1";
    private static final DateTimeFormatter YYYY = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MM = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter PORTUGAL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ISO_DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ApplicationProperties.S3Flatfiles properties;

    public S3OptionFlatfileService(ApplicationProperties applicationProperties) {
        this.properties = applicationProperties.getS3Flatfiles();
    }

    public Map<String, List<TickerOptionSnapshotData>> loadOptionRowsDaysData(LocalDate date) {
        String key = buildObjectKey(date, properties.getMinuteAgg());
        Map<String, List<TickerOptionSnapshotData>> rowsByTicker = new LinkedHashMap<>();

        AwsBasicCredentials creds = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        S3Configuration s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        try (S3Client client = S3Client.builder()
            .endpointOverride(URI.create(properties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.US_EAST_1)
            .serviceConfiguration(s3Configuration)
            .build()) {

            Path localPath = ensureLocalCopy(client, date, properties.getMinuteAgg(), key);
            try (InputStream rawInput = openInputStream(client, key, localPath);
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
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read s3 object '" + key + "': " + ex.getMessage(), ex);
        }

        return rowsByTicker;
    }

    public Map<String, Map<Long, TickerStockSnapshotData>> loadStockRowsDaysData(LocalDate date) {
        String key = buildObjectKey(date, STOCK_MINUTE_AGG);
        Map<String, Map<Long, TickerStockSnapshotData>> rowsByTicker = new LinkedHashMap<>();

        AwsBasicCredentials creds = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        S3Configuration s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        try (S3Client client = S3Client.builder()
            .endpointOverride(URI.create(properties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.US_EAST_1)
            .serviceConfiguration(s3Configuration)
            .build()) {

            Path localPath = ensureLocalCopy(client, date, STOCK_MINUTE_AGG, key);
            try (InputStream rawInput = openInputStream(client, key, localPath);
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
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read s3 object '" + key + "': " + ex.getMessage(), ex);
        }

        return rowsByTicker;
    }

    public Map<String, Object> fetchCsvOptRows(String fileLocation, String sortBy, String sortDirection, int limit) {
        S3Location location = resolveLocation(fileLocation);

        List<Map<String, Object>> records = new ArrayList<>();

        AwsBasicCredentials creds = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        S3Configuration s3Configuration = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

        try (S3Client client = S3Client.builder()
            .endpointOverride(URI.create(properties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.US_EAST_1)
            .serviceConfiguration(s3Configuration)
            .build();
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                 new GZIPInputStream(client.getObject(GetObjectRequest.builder()
                     .bucket(location.bucket())
                     .key(location.key())
                     .build())),
                 StandardCharsets.UTF_8))) {

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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bucket", location.bucket());
        result.put("endpoint", properties.getEndpoint());
        result.put("fileLocation", location.original());
        result.put("objectKey", location.key());
        result.put("sortBy", normalizeSortBy(sortBy));
        result.put("sortDirection", normalizeSortDirection(sortDirection));
        result.put("recordCount", records.size());
        result.put("records", records);
        return result;
    }

    public String getConfiguredTestTicker() {
        return normalizeTicker(properties.getTestTicker());
    }

    public LocalDate getConfiguredTestDay() {
        return properties.getTestDay() == null ? LocalDate.now().minusDays(1) : properties.getTestDay();
    }

    public Map<String, Object> fetchBucketUpdateTimestamp(LocalDate date) {
        String key = buildObjectKey(date);

        AwsBasicCredentials creds = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        S3Configuration s3Configuration = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

        try (S3Client client = S3Client.builder()
            .endpointOverride(URI.create(properties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.US_EAST_1)
            .serviceConfiguration(s3Configuration)
            .build()) {

            var headResponse = client.headObject(HeadObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bucket", properties.getBucket());
            result.put("endpoint", properties.getEndpoint());
            result.put("objectKey", key);
            result.put("lastModified", headResponse.lastModified() == null ? null : headResponse.lastModified().toString());
            result.put("contentLength", headResponse.contentLength());
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read S3 object metadata for '" + key + "': " + ex.getMessage(), ex);
        }
    }

    private String buildObjectKey(LocalDate date) {
        return buildObjectKey(date, properties.getMinuteAgg());
    }

    private String buildObjectKey(LocalDate date, String minuteAggPrefix) {
        String pattern = properties.getFileLocationPattern();
        String path = pattern
            .replace("YYYY", date.format(YYYY))
            .replace("MM", date.format(MM))
            .replace("DD", date.format(DD));
        return trimSlashes(minuteAggPrefix) + "/" + trimSlashes(path);
    }

    private InputStream openInputStream(S3Client client, String key, Path localPath) throws Exception {
        if (localPath != null) {
            return Files.newInputStream(localPath);
        }
        return client.getObject(GetObjectRequest.builder()
            .bucket(properties.getBucket())
            .key(key)
            .build());
    }

    private Path ensureLocalCopy(S3Client client,
                                 LocalDate date,
                                 String minuteAggPrefix,
                                 String key) {
        String root = properties.getLocalFilePath();
        if (!StringUtils.hasText(root)) {
            return null;
        }

        try {
            Path dateDir = Path.of(root.trim(), date.format(ISO_DAY));
            Files.createDirectories(dateDir);

            String safePrefix = trimSlashes(minuteAggPrefix).replace('/', '_');
            Path localFile = dateDir.resolve(safePrefix + "-" + date.format(ISO_DAY) + ".csv.gz");
            if (Files.exists(localFile) && Files.size(localFile) > 0L) {
                return localFile;
            }

            Path tempFile = dateDir.resolve(localFile.getFileName().toString() + ".part");
            try (ResponseInputStream<GetObjectResponse> remote = client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build())) {
                Files.copy(remote, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, localFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return localFile;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to cache s3 object locally for key '" + key + "': " + ex.getMessage(), ex);
        }
    }

    private static String trimSlashes(String value) {
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

    private static String normalizeTicker(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isTickerMatch(String rowTicker, String targetTicker) {
        if (targetTicker.isEmpty()) {
            return false;
        }
        String normalizedRow = rowTicker == null ? "" : rowTicker.trim().toUpperCase(Locale.ROOT);
        return normalizedRow.equals(targetTicker) || normalizedRow.startsWith("O:" + targetTicker);
    }

    private S3Location resolveLocation(String fileLocation) {
        String raw = fileLocation == null ? "" : fileLocation.trim();
        if (raw.startsWith("s3://")) {
            String remainder = raw.substring(5);
            int slashIndex = remainder.indexOf('/');
            if (slashIndex < 0) {
                throw new IllegalArgumentException("Invalid s3 file location: " + raw);
            }
            String bucket = trimSlashes(remainder.substring(0, slashIndex));
            String key = trimSlashes(remainder.substring(slashIndex + 1));
            return new S3Location(bucket, key, raw);
        }

        return new S3Location(trimSlashes(properties.getBucket()), trimSlashes(raw), raw);
    }

    private String toPortugalTime(String rawWindowStart) {
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
            return java.time.LocalDateTime.parse(value).atZone(ApplicationConfig.LISBON).format(PORTUGAL_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        return value;
    }

    private Comparator<Map<String, Object>> buildCsvOptComparator(String sortBy, String sortDirection) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        boolean descending = "desc".equalsIgnoreCase(normalizeSortDirection(sortDirection));

        Comparator<Map<String, Object>> comparator = switch (normalizedSortBy) {
            case "ticker" -> Comparator.comparing(row -> textValue(row, "ticker"), String.CASE_INSENSITIVE_ORDER);
            case "volume", "transactions" -> Comparator.comparingLong(row -> longValue(row, normalizedSortBy));
            case "open", "close", "high", "low" -> Comparator.comparingDouble(row -> doubleValue(row, normalizedSortBy));
            case "portugal_time" -> Comparator.comparing(row -> textValue(row, "portugal_time"), String.CASE_INSENSITIVE_ORDER);
            case "window_start" -> Comparator.comparingLong(row -> windowStartEpoch(row));
            default -> Comparator.comparing(row -> textValue(row, "window_start"), String.CASE_INSENSITIVE_ORDER);
        };

        if (descending) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(row -> textValue(row, "ticker"), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(row -> textValue(row, "window_start"), String.CASE_INSENSITIVE_ORDER);
    }

    private static String normalizeSortBy(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ticker", "volume", "open", "close", "high", "low", "window_start", "portugal_time", "transactions" -> normalized;
            default -> "window_start";
        };
    }

    private static String normalizeSortDirection(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "desc".equals(normalized) ? "desc" : "asc";
    }

    private static String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private static long longValue(Map<String, Object> row, String key) {
        try {
            return Long.parseLong(textValue(row, key).replaceAll("[^0-9-]", ""));
        } catch (Exception ex) {
            return Long.MIN_VALUE;
        }
    }

    private static double doubleValue(Map<String, Object> row, String key) {
        try {
            return Double.parseDouble(textValue(row, key).replaceAll("[^0-9.\\-]", ""));
        } catch (Exception ex) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    private static long windowStartEpoch(Map<String, Object> row) {
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

    private static Integer parseInteger(String value) {
        try { return value == null || value.isBlank() ? null : Integer.valueOf(value.trim()); }
        catch (Exception ex) { return null; }
    }

    private static Long parseLong(String value) {
        try { return value == null || value.isBlank() ? null : Long.valueOf(value.trim()); }
        catch (Exception ex) { return null; }
    }

    private static BigDecimal parseBigDecimal(String value) {
        try { return value == null || value.isBlank() ? null : new BigDecimal(value.trim()); }
        catch (Exception ex) { return null; }
    }

    private record S3Location(String bucket, String key, String original) { }
}
