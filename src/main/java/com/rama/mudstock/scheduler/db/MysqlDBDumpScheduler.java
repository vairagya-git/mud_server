package com.rama.mudstock.scheduler.db;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.enums.CronjobConfigEnum;
import com.rama.mudstock.scheduler.AbstractCronjob;
import com.rama.mudstock.service.MysqlDumpService;
import com.rama.mudstock.service.SystemConfigService;

@Component
@Profile("cronjob")
public class MysqlDBDumpScheduler extends AbstractCronjob {

    private final MysqlDumpService mysqlDumpService;
    private final Logger log = LoggerFactory.getLogger(MysqlDBDumpScheduler.class);

    public MysqlDBDumpScheduler(MysqlDumpService mysqlDumpService,
                                SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.mysqlDumpService = mysqlDumpService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = AbstractCronjob.LISBON_ZONE)
    public void dumpMysqlDatabase() {
        var locationCfg = CronjobConfigEnum.LOCATION;
        String purpose = CronjobConfigEnum.Purpose.DAILY_MY_SQL_DB_DUMP.value();

        if (!isEnabled(purpose)) {
            log.info("MysqlDBDumpScheduler: disabled by system_config (purpose={}, code={})", purpose, enabledCode());
            return;
        }

        if (!shouldExecuteSinceLastUpdated("MysqlDBDumpScheduler", null, purpose, LISBON)) {
            return;
        }

        String outputLocation = resolveStringValue(purpose, locationCfg.code());
        if (outputLocation.isBlank()) {
            log.warn("MysqlDBDumpScheduler: missing dump location in system_config (purpose={}, code={})", purpose, locationCfg.code());
            return;
        }

        try {
            Path outputFile = mysqlDumpService.dumpToLocation(outputLocation);
            log.info("MysqlDBDumpScheduler: mysql dump completed at {}", outputFile);
            updateLastUpdatedNowUtc(purpose, lastUpdatedCode());
        } catch (Exception ex) {
            log.error("MysqlDBDumpScheduler: mysql dump failed", ex);
        }
    }
}