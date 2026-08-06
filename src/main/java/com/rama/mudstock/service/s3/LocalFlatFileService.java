package com.rama.mudstock.service.s3;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rama.mudstock.config.ApplicationProperties;

@Service
@ConditionalOnProperty(prefix = "s3-flatfiles", name = "read-local", havingValue = "true")
public class LocalFlatFileService extends AbstractFlatfileService {

    private static final DateTimeFormatter YYYY = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MM = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd");

    public LocalFlatFileService(ApplicationProperties applicationProperties) {
        super(applicationProperties);
    }

    @Override
    protected String sourceName() {
        return "local-filesystem";
    }

    @Override
    protected String defaultBucket() {
        return "local";
    }

    @Override
    protected String buildObjectKey(LocalDate date, String minuteAggPrefix) {
        String minuteAgg = trimSlashes(properties.getMinuteAgg());
        String stockMinuteAgg = trimSlashes(properties.getStockMinuteAgg());

        String localPrefix;
        if (trimSlashes(minuteAggPrefix).equals(stockMinuteAgg)) {
            localPrefix = properties.getLocalStock();
        } else if (trimSlashes(minuteAggPrefix).equals(minuteAgg)) {
            localPrefix = properties.getLocalOption();
        } else {
            localPrefix = properties.getLocalOption();
        }

        if (!StringUtils.hasText(localPrefix)) {
            throw new IllegalStateException("s3-flatfiles.local-option/local-stock is required for local read mode");
        }

        String localPattern = properties.getLocalFileLocationPattern();
        if (!StringUtils.hasText(localPattern)) {
            throw new IllegalStateException("s3-flatfiles.local-file-location-pattern is required for local read mode");
        }

        String filePart = localPattern
            .replace("YYYY", date.format(YYYY))
            .replace("MM", date.format(MM))
            .replace("DD", date.format(DD));

        return trimSlashes(localPrefix) + "-" + trimSlashes(filePart);
    }

    @Override
    protected InputStream openDailyInputStream(LocalDate date, String minuteAggPrefix, String key) throws Exception {
        Path localPath = resolveLocalPathFromKey(key);
        return Files.newInputStream(localPath);
    }

    @Override
    protected InputStream openLocationInputStream(FlatfileLocation location) throws Exception {
        Path localPath = resolveLocalPath(location);
        return Files.newInputStream(localPath);
    }

    @Override
    protected Map<String, Object> buildLocationInfo(FlatfileLocation location) {
        Path localPath = resolveLocalPath(location);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("bucket", "local");
        info.put("endpoint", localRootOrThrow());
        info.put("localPath", localPath.toAbsolutePath().toString());
        return info;
    }

    @Override
    protected Map<String, Object> buildUpdateTimestampResult(String key) {
        try {
            Path localPath = resolveLocalPathFromKey(key);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bucket", "local");
            result.put("endpoint", localRootOrThrow());
            result.put("objectKey", key);
            result.put("localPath", localPath.toAbsolutePath().toString());
            result.put("lastModified", Files.exists(localPath) ? Files.getLastModifiedTime(localPath).toString() : null);
            result.put("contentLength", Files.exists(localPath) ? Files.size(localPath) : null);
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read local file metadata for key '" + key + "': " + ex.getMessage(), ex);
        }
    }

    private Path resolveLocalPath(FlatfileLocation location) {
        String original = location.original() == null ? "" : location.original().trim();
        if (StringUtils.hasText(original) && !original.startsWith("s3://")) {
            Path direct = Path.of(original);
            if (direct.isAbsolute()) {
                return direct;
            }
        }
        return resolveLocalPathFromKey(location.key());
    }

    private Path resolveLocalPathFromKey(String key) {
        String root = localRootOrThrow();
        return Path.of(root, trimSlashes(key));
    }
}
