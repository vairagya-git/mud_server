package com.rama.mudstock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "s3-flatfiles")
public class S3FlatfilesProperties {

    private String bucket;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String minuteAgg;
    private String fileLocationPattern;
    private String testTicker;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getMinuteAgg() {
        return minuteAgg;
    }

    public void setMinuteAgg(String minuteAgg) {
        this.minuteAgg = minuteAgg;
    }

    public String getFileLocationPattern() {
        return fileLocationPattern;
    }

    public void setFileLocationPattern(String fileLocationPattern) {
        this.fileLocationPattern = fileLocationPattern;
    }

    public String getTestTicker() {
        return testTicker;
    }

    public void setTestTicker(String testTicker) {
        this.testTicker = testTicker;
    }
}
