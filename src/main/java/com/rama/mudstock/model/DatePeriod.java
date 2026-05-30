package com.rama.mudstock.model;

public enum DatePeriod {
    OneWeekBefore("OneWeekBefore"),
    _3DaysBefore("3DaysBefore"),
    _2DaysBefore("2DaysBefore"),
    _1DayBefore("1DayBefore"),
    EarningDay("EarningDay"),
    _1DayAfter("1DayAfter"),
    _2DaysAfter("2DaysAfter"),
    OneWeekAfter("OneWeekAfter"),
    TwoWeekAfter("TwoWeekAfter");

    private final String dbValue;

    DatePeriod(String dbValue) { this.dbValue = dbValue; }

    @Override
    public String toString() { return dbValue; }

    public String getDbValue() { return dbValue; }
}
