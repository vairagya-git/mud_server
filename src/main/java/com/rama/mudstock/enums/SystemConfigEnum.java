package com.rama.mudstock.enums;

public enum SystemConfigEnum {
    SYSTEM_WATCHLIST_CODES("SystemWatchlistCodes", "StringArray", "CommonSystemSettings", "fetch for teh configured watchlist");

    private final String code;
    private final String type;
    private final String purpose;
    private final String description;

    SystemConfigEnum(String code, String type, String purpose, String description) {
        this.code = code;
        this.type = type;
        this.purpose = purpose;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String type() {
        return type;
    }

    public String purpose() {
        return purpose;
    }

    public String description() {
        return description;
    }
}
