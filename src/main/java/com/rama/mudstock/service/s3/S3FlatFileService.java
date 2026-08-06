package com.rama.mudstock.service.s3;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rama.mudstock.config.ApplicationProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Service
@ConditionalOnProperty(prefix = "s3-flatfiles", name = "read-local", havingValue = "false", matchIfMissing = true)
public class S3FlatFileService extends AbstractFlatfileService {

    private static final Logger log = LoggerFactory.getLogger(S3FlatFileService.class);

    public S3FlatFileService(ApplicationProperties applicationProperties) {
        super(applicationProperties);
    }

    @Override
    protected String sourceName() {
        return "s3";
    }

    @Override
    protected String defaultBucket() {
        return trimSlashes(properties.getBucket());
    }

    @Override
    protected InputStream openDailyInputStream(LocalDate date, String minuteAggPrefix, String key) throws Exception {
        S3Client client = buildClient();
        try {
            Path localPath = ensureLocalCopy(client, date, minuteAggPrefix, key);
            if (localPath != null) {
                client.close();
                return Files.newInputStream(localPath);
            }

            ResponseInputStream<GetObjectResponse> remote = client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
            return wrapWithClient(remote, client);
        } catch (Exception ex) {
            client.close();
            throw ex;
        }
    }

    @Override
    protected InputStream openLocationInputStream(FlatfileLocation location) throws Exception {
        S3Client client = buildClient();
        try {
            ResponseInputStream<GetObjectResponse> remote = client.getObject(GetObjectRequest.builder()
                .bucket(location.bucket())
                .key(location.key())
                .build());
            return wrapWithClient(remote, client);
        } catch (Exception ex) {
            client.close();
            throw ex;
        }
    }

    @Override
    protected Map<String, Object> buildLocationInfo(FlatfileLocation location) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("bucket", location.bucket());
        info.put("endpoint", properties.getEndpoint());
        return info;
    }

    @Override
    protected Map<String, Object> buildUpdateTimestampResult(String key) {
        try (S3Client client = buildClient()) {
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

    private S3Client buildClient() {
        AwsBasicCredentials creds = AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey());
        S3Configuration s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        return S3Client.builder()
            .endpointOverride(URI.create(properties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .region(Region.US_EAST_1)
            .serviceConfiguration(s3Configuration)
            .build();
    }

    private InputStream wrapWithClient(ResponseInputStream<GetObjectResponse> stream, S3Client client) {
        return new FilterInputStream(stream) {
            @Override
            public void close() throws java.io.IOException {
                try {
                    super.close();
                } finally {
                    client.close();
                }
            }
        };
    }

    private Path ensureLocalCopy(S3Client client,
                                 LocalDate date,
                                 String minuteAggPrefix,
                                 String key) {
        String root = properties.getLocalDirectory();
        if (!StringUtils.hasText(root)) {
            root = properties.getLocalFilePath();
        }
        if (!StringUtils.hasText(root)) {
            log.warn("S3FlatFileService: local cache path not configured (s3-flatfiles.local-directory), streaming directly from S3. bucket={}, key={}",
                properties.getBucket(), key);
            return null;
        }

        try {
            Path dateDir = Path.of(root.trim(), date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
            Files.createDirectories(dateDir);

            String safePrefix = trimSlashes(minuteAggPrefix).replace('/', '_');
            Path localFile = dateDir.resolve(safePrefix + "-" + date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) + ".csv.gz");

            log.info("S3FlatFileService: resolved s3Location=s3://{}/{} localPath={}",
                properties.getBucket(), key, localFile.toAbsolutePath());

            if (Files.exists(localFile) && Files.size(localFile) > 0L) {
                log.info("S3FlatFileService: using cached local file. localPath={}", localFile.toAbsolutePath());
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

            log.info("S3FlatFileService: downloaded and cached s3 object. s3Location=s3://{}/{} localPath={}",
                properties.getBucket(), key, localFile.toAbsolutePath());

            return localFile;
        } catch (Exception ex) {
            log.error("S3FlatFileService: failed to cache s3 object locally. bucket={}, key={}",
                properties.getBucket(), key, ex);
            throw new RuntimeException("Failed to cache s3 object locally for key '" + key + "': " + ex.getMessage(), ex);
        }
    }
}
