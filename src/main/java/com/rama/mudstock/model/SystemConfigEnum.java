package com.rama.mudstock.model;

public enum SystemConfigEnum {
    ;

    public enum DailyAnalystRatingCronjob {
        WATCHLIST_CODES("watchlist-codes", "StringArray", "Daily Analyst Rating Cronjob > Watchlist Codes"),
        ENABLED("enabled", "Boolean", "Daily Analyst Rating Cronjob > Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Daily Analyst Rating Cronjob > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Daily Analyst Rating Cronjob > Last Updated"),
        USEAGE("useage", "String", "Daily Analyst Rating Cronjob > Usage");

        private static final String PURPOSE = "DailyAnalystRatingCronjob";

        private final String code;
        private final String type;
        private final String description;

        DailyAnalystRatingCronjob(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum WeeklyAnalystFirmUpdateCronjob {
        ENABLED("enabled", "Boolean", "Weekly Analyst Firm Update Cronjob > Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Weekly Analyst Firm Update Cronjob > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Weekly Analyst Firm Update Cronjob > Last Updated"),
        USEAGE("useage", "String", "Weekly Analyst Firm Update Cronjob > Usage");

        private static final String PURPOSE = "WeeklyAnalystFirmUpdateCronjob";

        private final String code;
        private final String type;
        private final String description;

        WeeklyAnalystFirmUpdateCronjob(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum WeeklyUpcomingEarningCronjob {
        WATCHLIST_CODES("watchlist-codes", "StringArray", "Weekly Upcoming Earning Cronjob > Watchlist Codes"),
        ENABLED("enabled", "Boolean", "Weekly Upcoming Earning Cronjob > Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Weekly Upcoming Earning Cronjob > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Weekly Upcoming Earning Cronjob > Last Updated"),
        USEAGE("useage", "String", "Weekly Upcoming Earning Cronjob > Usage");

        private static final String PURPOSE = "WeeklyUpcomingEarningCronjob";

        private final String code;
        private final String type;
        private final String description;

        WeeklyUpcomingEarningCronjob(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum DayStockMovementData {
        ENABLED("enabled", "Boolean", "Day Stock Movement Data > cronjob Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Day Stock Movement Data > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Day Stock Movement Data > Last Updated"),
        USEAGE("useage", "String", "Day Stock Movement Data > Usage");

        private static final String PURPOSE = "DayStockMovementData";

        private final String code;
        private final String type;
        private final String description;

        DayStockMovementData(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum DayStockMovementCleanup {
        ENABLED("enabled", "Boolean", "Day Stock Movement Cleanup > cronjob Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Day Stock Movement Cleanup > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Day Stock Movement Cleanup > Last Updated"),
        USEAGE("useage", "String", "Day Stock Movement Cleanup > Usage");

        private static final String PURPOSE = "DayStockMovementCleanup";

        private final String code;
        private final String type;
        private final String description;

        DayStockMovementCleanup(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum DayStockMovementKeyMapEntry {
        ENABLED("enabled", "Boolean", "Day Stock Movement Key Map Entry > cronjob Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Day Stock Movement Key Map Entry > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Day Stock Movement Key Map Entry > Last Updated"),
        WATCHLIST_CODES("watchlist-codes", "StringArray", "Day Stock Movement Key Map Entry > Watchlist Codes"),
        USEAGE("useage", "String", "Day Stock Movement Key Map Entry > Usage");

        private static final String PURPOSE = "DayStockMovementKeyMapEntry";

        private final String code;
        private final String type;
        private final String description;

        DayStockMovementKeyMapEntry(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum DailyMysqlDBDump {
        USEAGE("useage", "String", "Daily Mysql DB Dump > Usage"),
        ENABLED("enabled", "Boolean", "Daily Mysql DB Dump > Enabled"),
        LOCATION("location", "String", "Daily Mysql DB Dump > Output Location"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Daily Mysql DB Dump > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Daily Mysql DB Dump > Last Updated");

        private static final String PURPOSE = "DailyMysqlDBDump";

        private final String code;
        private final String type;
        private final String description;

        DailyMysqlDBDump(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

    public enum OptionsIntervalAnalyseDailyJob {
        USEAGE("useage", "String", "Options Interval Analyse Daily Job > Usage"),
        ENABLED("enabled", "Boolean", "Options Interval Analyse Daily Job > Enabled"),
        CRON_EXPRESSION("cronExpression", "CronExpression", "Options Interval Analyse Daily Job > Cron Expression"),
        LAST_UPDATED("lastUpdated", "DateTime", "Options Interval Analyse Daily Job > Last Updated");

        private static final String PURPOSE = "OptionsIntervalAnalyseDailyJob";

        private final String code;
        private final String type;
        private final String description;

        OptionsIntervalAnalyseDailyJob(String code, String type, String description) {
            this.code = code;
            this.type = type;
            this.description = description;
        }

        public String purpose() {
            return PURPOSE;
        }

        public String code() {
            return code;
        }

        public String type() {
            return type;
        }

        public String description() {
            return description;
        }
    }

}