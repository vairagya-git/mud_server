package com.rama.mudstock.scheduler.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.SystemConfigService;
import com.rama.mudstock.util.TypeConverstionUtil;

public abstract class AbstractOptionStrategySnapshotRefinementJob extends AbstractCronjob {

    private static final Logger log = LoggerFactory.getLogger(AbstractOptionStrategySnapshotRefinementJob.class);
    private Integer optionSnapshotInterval = 5;
    private String lastFetchedSnapshotTime;
    private String lastFetchedFlatFileTime;
    private String lastFetchedManualEntryTime;

    protected AbstractOptionStrategySnapshotRefinementJob(SystemConfigService systemConfigService,
                                                          String purpose) {
        super(systemConfigService, purpose);
    }

    protected void loadOptionSnapshotInterval() {
        Integer configured = TypeConverstionUtil.toInteger(getConfigValue(CronjobConfigEnum.OPTION_SNAPSHOT_INTERVAL.code()));
        if (configured == null || configured <= 0) {
            optionSnapshotInterval = 5;
            log.warn("{}: missing/invalid {} config; using default {}",
                getPurpose(),
                CronjobConfigEnum.OPTION_SNAPSHOT_INTERVAL.code(),
                optionSnapshotInterval);
            return;
        }

        optionSnapshotInterval = configured;
    }

    protected Integer optionSnapshotInterval() {
        return optionSnapshotInterval;
    }

    protected void loadAndLogSharedConfig(Logger jobLogger) {
        loadOptionSnapshotInterval();
        loadLastFetchedTimes();
        jobLogger.info("{}: {}={} minute(s)",
            getPurpose(),
            CronjobConfigEnum.OPTION_SNAPSHOT_INTERVAL.code(),
            optionSnapshotInterval());
        jobLogger.info("{}: {}={}, {}={}, {}={}",
            getPurpose(),
            CronjobConfigEnum.LAST_FETCHED_SNAPSHOT_TIME.code(),
            lastFetchedSnapshotTime(),
            CronjobConfigEnum.LAST_FETCHED_FLAT_FILE_TIME.code(),
            lastFetchedFlatFileTime(),
            CronjobConfigEnum.LAST_FETCHED_MANUAL_ENTRY_TIME.code(),
            lastFetchedManualEntryTime());
    }

    protected void loadLastFetchedTimes() {
        lastFetchedSnapshotTime = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.LAST_FETCHED_SNAPSHOT_TIME.code()));
        lastFetchedFlatFileTime = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.LAST_FETCHED_FLAT_FILE_TIME.code()));
        lastFetchedManualEntryTime = TypeConverstionUtil.toString(getConfigValue(CronjobConfigEnum.LAST_FETCHED_MANUAL_ENTRY_TIME.code()));
    }

    protected String lastFetchedSnapshotTime() {
        return lastFetchedSnapshotTime;
    }

    protected String lastFetchedFlatFileTime() {
        return lastFetchedFlatFileTime;
    }

    protected String lastFetchedManualEntryTime() {
        return lastFetchedManualEntryTime;
    }
}