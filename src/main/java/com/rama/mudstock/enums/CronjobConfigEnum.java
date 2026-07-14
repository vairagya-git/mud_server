package com.rama.mudstock.enums;

public enum CronjobConfigEnum {
    USEAGE("useage", "String", "Usage"),
    ENABLED("enabled", "Boolean", "Enabled"),
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
}
