package com.rama.mudstock.model;

public enum DatePeriod {
    OneWeekBefore("OneWeekBefore", -5),
    _4DaysBefore("4DaysBefore", -4),
    _3DaysBefore("3DaysBefore", -3),
    _2DaysBefore("2DaysBefore", -2),
    _1DayBefore("1DayBefore", -1),
    EarningDay("EarningDay", 0),
    _1DayAfter("1DayAfter", 1),
    _2DaysAfter("2DaysAfter", 2),
    _3DaysAfter("3DaysAfter", 3),
    _4DaysAfter("4DaysAfter", 4),
    OneWeekAfter("OneWeekAfter", 5),
    TwoWeekAfter("TwoWeekAfter", 10);

    private final String dbValue;
    private final int daysOffset; // days offset relative to earnings_date (negative = before, positive = after)

    DatePeriod(String dbValue, int daysOffset) { this.dbValue = dbValue; this.daysOffset = daysOffset; }
    @Override
    public String toString() { return dbValue; }

    public String getDbValue() { return dbValue; }

    public int getDaysOffset() { return daysOffset; }
}
