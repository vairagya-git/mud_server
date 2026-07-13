package com.rama.mudstock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "option-analysis")
public class ApplicationProperties {

    private long snapshotRefreshMs = 120000L;

    public long getSnapshotRefreshMs() {
        return snapshotRefreshMs;
    }

    public void setSnapshotRefreshMs(long snapshotRefreshMs) {
        this.snapshotRefreshMs = snapshotRefreshMs;
    }
}