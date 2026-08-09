package com.rama.mudstock.enums;

import com.rama.mudstock.config.ApplicationConfig;

public enum CronjobConfigEnum {
    USEAGE("useage", "String", "Usage"),
    ENABLED("enabled", "Boolean", "Enabled"),
    FORCE_EXECUTE("forceExecute", "Boolean", "Force Execute"),
    FORCE_EXECUTE_DAILY_DATE("forceExecuteDailyDate", "DateTime", "Set the date for forceExecute"),
    DAILY_DATE("dailyDate", "DateTime", "Set the execution date for normal runs"),
    EXECUTION("execution", "String", "Execution"),
    MINUTE_HOURLY_FREQUENCY("minuteHourlyFrequency", "Integer", "Minute/Hourly Frequency"),
    LAST_UPDATED("lastUpdated", "DateTime", "Last Updated"),
    DAILY_CUTT_OFF_TIME("dailyCutOffTime", "Time", ApplicationConfig.TIME_FORMAT_HH_MM, "Cutoff Time"),
    START_TIME("startTime", "Time", ApplicationConfig.TIME_FORMAT_HH_MM, "Start Time"),
    END_TIME("endTime", "Time", ApplicationConfig.TIME_FORMAT_HH_MM, "End Time"),
    WATCHLIST_CODES("watchlist-codes", "StringArray", "Watchlist Codes"),
    PULL_STOCK_HISTORY("pullStockHistory", "StringArray", "Pull the stock history for the ticker for the period pullStockHistoryDays"),
    PULL_STOCK_HISTORY_DAYS("pullStockHistoryDays", "Integer", "No of Days to pull the stock history"),
    LOCATION("location", "String", "Output Location");

    private final String code;
    private final String type;
    private final String format;
    private final String description;

    CronjobConfigEnum(String code, String type, String description) {
        this(code, type, null, description);
    }

    CronjobConfigEnum(String code, String type, String format, String description) {
        this.code = code;
        this.type = type;
        this.format = format;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String type() {
        return type;
    }

    public String format() {
        return format;
    }

    public String description() {
        return description;
    }

    public enum Execution {
        HOURLY("hourly"),
        MINUTES("minutes"),
        DAILY("daily");

        private final String value;

        Execution(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Execution fromValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            for (Execution execution : values()) {
                if (execution.value().equals(raw)) {
                    return execution;
                }
            }
            return null;
        }
    }

    public enum ExecutionMode {
        /**
         * Require current time to be on/after dailyCutOffTime for HOURLY/MINUTES.
         */
        DAILY_CUT_OFF,
        /**
         * Require current time to be inside [startTime, endTime] for HOURLY/MINUTES.
         */
        START_END_TIME,
        /**
         * No extra time-window/cutoff constraint for HOURLY/MINUTES.
         */
        NONE
    }

    public enum Purpose {
        WEEKLY_ANALYST_FIRM_UPDATE_CRONJOB("WeeklyAnalystFirmUpdateCronjob"),
        DAILY_ANALYST_RATING_CRONJOB("DailyAnalystRatingCronjob"),
        WEEKLY_UPCOMING_EARNING_CRONJOB("WeeklyUpcomingEarningCronjob"),
        EARNINGS_DETAIL_CRONJOB("EarningsDetailCronjob"),
        DAY_STOCK_MOVEMENT_DATA("DayStockMovementData"),
        DAY_STOCK_MOVEMENT_DATA_ENRICHEMENT("DayStockMovementDataEnrichement"),
        DAILY_MY_SQL_DB_DUMP("DailyMysqlDBDump"),
        OPTIONS_INTERVAL_ANALYSE_DAILY_JOB("OptionsIntervalAnalyseDailyJob"),
        OPTION_SNAPSHOT_IV_METRICS("OptionSnapshotIVMetrics"),
        OPTION_API_SNAPSHOT_FETCHER_JOB("OptionAPISnapshotFetcherJob"),
        OPTION_FLAT_FILE_SNAPSHOT_FETCHER_JOB("OptionFlatFileSnapshotFetcherJob");

        private final String value;

        Purpose(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}

//Changed For Git