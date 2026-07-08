package com.rama.mudstock.scheduler.db;

import java.nio.file.Path;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.service.CronJobConfigSupport;
import com.rama.mudstock.service.MysqlDumpService;

@Component
@Profile("cronjob")
public class DailyMysqlDBDumpScheduler {

    private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

    private final MysqlDumpService mysqlDumpService;
    private final CronJobConfigSupport cronJobConfigSupport;
    private final Logger log = LoggerFactory.getLogger(DailyMysqlDBDumpScheduler.class);

    public DailyMysqlDBDumpScheduler(MysqlDumpService mysqlDumpService,
                                     CronJobConfigSupport cronJobConfigSupport) {
        this.mysqlDumpService = mysqlDumpService;
        this.cronJobConfigSupport = cronJobConfigSupport;
    }

    @Scheduled(cron = "${all-cronjob-schedule}", zone = "Europe/Lisbon")
    public void dumpMysqlDatabase() {
        var enabledCfg = SystemConfigEnum.DailyMysqlDBDump.ENABLED;
        var cronCfg = SystemConfigEnum.DailyMysqlDBDump.CRON_EXPRESSION;
        var lastUpdatedCfg = SystemConfigEnum.DailyMysqlDBDump.LAST_UPDATED;
        var locationCfg = SystemConfigEnum.DailyMysqlDBDump.LOCATION;
        String purpose = enabledCfg.purpose();

        if (!cronJobConfigSupport.isEnabled(purpose, enabledCfg.code())) {
            log.info("DailyMysqlDBDumpScheduler: disabled by system_config (purpose={}, code={})", purpose, enabledCfg.code());
            return;
        }

        if (!cronJobConfigSupport.shouldExecuteSinceLastUpdated(
            purpose,
            cronCfg.code(),
            lastUpdatedCfg.code(),
            LISBON)) {
            return;
        }

        String outputLocation = cronJobConfigSupport.resolveStringValue(purpose, locationCfg.code());
        if (outputLocation.isBlank()) {
            log.warn("DailyMysqlDBDumpScheduler: missing dump location in system_config (purpose={}, code={})", purpose, locationCfg.code());
            return;
        }

        try {
            Path outputFile = mysqlDumpService.dumpToLocation(outputLocation);
            log.info("DailyMysqlDBDumpScheduler: mysql dump completed at {}", outputFile);
            cronJobConfigSupport.updateLastUpdatedNowUtc(purpose, lastUpdatedCfg.code());
        } catch (Exception ex) {
            log.error("DailyMysqlDBDumpScheduler: mysql dump failed", ex);
        }
    }
}