package com.rama.mudstock.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.rama.mudstock.master.MasterDataCache;

/**
 * Service for market-calendar queries.
 *
 * <p>All holiday data is served from the in-memory {@link MasterDataCache}
 * that is populated at startup — no database calls are made per request.</p>
 */
@Service
public class MarketCalendarService {

    private final MasterDataCache cache;

    public MarketCalendarService(MasterDataCache cache) {
        this.cache = cache;
    }

    /**
     * Returns {@code true} when {@code date} is a US market holiday
     * (present in the {@code master_market_holidays} table).
     */
    public boolean isMarketHoliday(LocalDate date) {
        if (date == null) return false;
        return cache.isHoliday(date);
    }

    /**
     * Returns {@code true} when {@code date} falls on a Saturday or Sunday.
     */
    public boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Returns {@code true} when the market is closed on {@code date} —
     * i.e. the day is either a weekend or a recorded market holiday.
     */
    public boolean isMarketClosed(LocalDate date) {
        return isWeekend(date) || isMarketHoliday(date);
    }
}
