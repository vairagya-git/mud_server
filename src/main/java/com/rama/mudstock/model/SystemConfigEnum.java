package com.rama.mudstock.model;

public enum SystemConfigEnum {
    DAILY_ANALYST_RATING_WATCHLIST_CODES("DailyAnalystRatingCronjob", "watchlist-codes", "StringArray"),
    DAILY_ANALYST_RATING_ENABLED("DailyAnalystRatingCronjob", "enabled", "Boolean"),
    DAILY_ANALYST_RATING_DATE("DailyAnalystRatingCronjob", "benzinga-analyst-rating-date", "Date");

    private final String purpose;
    private final String code;
    private final String type;

    SystemConfigEnum(String purpose, String code, String type) {
        this.purpose = purpose;
        this.code = code;
        this.type = type;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}