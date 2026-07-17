package com.rama.mudstock.enums;

public enum CronjobConfigEnum {
    USEAGE("useage", "String", "Usage"),
    ENABLED("enabled", "Boolean", "Enabled"),
    FORCE_EXECUTE("forceExecute", "Boolean", "Force Execute"),
    EXECUTION("execution", "String", "Execution"),
    MINUTE_HOURLY_FREQUENCY("minuteHourlyFrequency", "Integer", "Minute/Hourly Frequency"),
    LAST_UPDATED("lastUpdated", "DateTime", "Last Updated"),
    CUTOFF_TIME("cutOffTime", "Time", "HH:mm", "Cutoff Time"),
    START_TIME("startTime", "Time", "HH:mm", "Start Time"),
    END_TIME("endTime", "Time", "HH:mm", "End Time"),
    WATCHLIST_CODES("watchlist-codes", "StringArray", "Watchlist Codes"),
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

    public enum Purpose {
        WEEKLY_ANALYST_FIRM_UPDATE_CRONJOB("WeeklyAnalystFirmUpdateCronjob"),
        DAILY_ANALYST_RATING_CRONJOB("DailyAnalystRatingCronjob"),
        WEEKLY_UPCOMING_EARNING_CRONJOB("WeeklyUpcomingEarningCronjob"),
        DAY_STOCK_MOVEMENT_DATA("DayStockMovementData"),
        DAY_STOCK_MOVEMENT_CLEANUP("DayStockMovementCleanup"),
        DAY_STOCK_MOVEMENT_KEY_MAP_ENTRY("DayStockMovementKeyMapEntry"),
        DAILY_MY_SQL_DB_DUMP("DailyMysqlDBDump"),
        OPTIONS_INTERVAL_ANALYSE_DAILY_JOB("OptionsIntervalAnalyseDailyJob"),
        OPTION_SNAPSHOT_IV_METRICS("OptionSnapshotIVMetrics"),
        OPTION_SNAPSHOT_FETCHER_JOB("OptionSnapshotFetcherJob");

        private final String value;

        Purpose(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
