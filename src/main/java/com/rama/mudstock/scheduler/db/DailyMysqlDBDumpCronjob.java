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
public class DailyMysqlDBDumpCronjob extends AbstractCronjob {

    private final MysqlDumpService mysqlDumpService;
    private final Logger log = LoggerFactory.getLogger(DailyMysqlDBDumpCronjob.class);

    public DailyMysqlDBDumpCronjob(MysqlDumpService mysqlDumpService,
                                   SystemConfigService systemConfigService) {
        super(systemConfigService);
        this.mysqlDumpService = mysqlDumpService;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = com.rama.mudstock.config.ApplicationConfig.LISBON_ZONE)
    public void dumpMysqlDatabase() {
        var locationCfg = CronjobConfigEnum.LOCATION;
        String purpose = CronjobConfigEnum.Purpose.DAILY_MY_SQL_DB_DUMP.value();

        if (!shouldExecuteBySchedule(purpose)) {
            return;
        }

        String outputLocation = resolveStringValue(purpose, locationCfg.code());
        if (outputLocation.isBlank()) {
            log.warn("{}: missing dump location in system_config (code={})", purpose, locationCfg.code());
            return;
        }

        try {
            Path outputFile = mysqlDumpService.dumpToLocation(outputLocation);
            log.info("{}: mysql dump completed at {}", purpose, outputFile);
            updateLastUpdatedNowUtc(purpose);
        } catch (Exception ex) {
            log.error("{}: mysql dump failed", purpose, ex);
        }
    }
}