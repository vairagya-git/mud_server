package com.rama.mudstock.master;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.rama.mudstock.model.MasterMarketHoliday;

/**
 * In-memory cache for master table data loaded at application startup.
 *
 * <p>Each master table that is configured in {@code master.tables} is populated
 * by {@link MasterDataLoader} once the application context is ready. Consumers
 * inject this bean and call the relevant typed accessor instead of hitting the
 * database on every request.</p>
 */
@Component
public class MasterDataCache {

    /** Canonical table-name key for master_market_holidays. */
    public static final String MARKET_HOLIDAYS = "master_market_holidays";

    /**
     * Generic store: table-name → opaque list of rows.
     * Used by {@link MasterDataLoader} to record which tables were loaded.
     */
    private final ConcurrentHashMap<String, List<?>> tableCache = new ConcurrentHashMap<>();

    /** Fast O(1) lookup set built from the loaded {@link MasterMarketHoliday} rows. */
    private final Set<LocalDate> holidayDates = ConcurrentHashMap.newKeySet();

    // -----------------------------------------------------------------------
    // Population (called by MasterDataLoader on startup)
    // -----------------------------------------------------------------------

    /**
     * Stores the full list of {@link MasterMarketHoliday} rows and rebuilds the
     * fast-lookup holiday-date set.
     */
    public void putMarketHolidays(List<MasterMarketHoliday> holidays) {
        tableCache.put(MARKET_HOLIDAYS, Collections.unmodifiableList(holidays));
        holidayDates.clear();
        for (MasterMarketHoliday h : holidays) {
            if (h.getHolidayDate() != null) {
                holidayDates.add(h.getHolidayDate());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Returns an unmodifiable view of all cached {@link MasterMarketHoliday} rows. */
    @SuppressWarnings("unchecked")
    public List<MasterMarketHoliday> getMarketHolidays() {
        List<?> cached = tableCache.get(MARKET_HOLIDAYS);
        if (cached == null) return Collections.emptyList();
        return (List<MasterMarketHoliday>) cached;
    }

    /**
     * Returns {@code true} if the given date is present in the cached holiday set.
     * This is an O(1) lookup that never touches the database.
     */
    public boolean isHoliday(LocalDate date) {
        return date != null && holidayDates.contains(date);
    }

    /**
     * Returns {@code true} if the given table name has been loaded into the cache.
     */
    public boolean isLoaded(String tableName) {
        return tableCache.containsKey(tableName);
    }

    /**
     * Returns a snapshot of all table names that have been loaded.
     */
    public Set<String> loadedTables() {
        return Collections.unmodifiableSet(tableCache.keySet());
    }
}
