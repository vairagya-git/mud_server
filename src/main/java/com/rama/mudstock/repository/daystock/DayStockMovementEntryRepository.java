package com.rama.mudstock.repository.daystock;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DayStockMovementEntryRepository {
    private final JdbcTemplate jdbc;

    public DayStockMovementEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Insert or update a day_stock_movement_entry row for the given stock movement mapping.
     */
    public int upsertDayStockMovementEntry(Long stockId,
                                           Long mappingId,
                                           Timestamp dayStockMovementDate,
                                           Long earningsDateId,
                                           double preDayClose, double curDayOpen, double curDayClose,
                                           double curDayHigh, double curDayLow, double curDayVolWeight,
                                           long curDayVolume, Double changePercent, Double dayOpeningChangePercent) {
        return upsertDayStockMovementEntry(
            stockId,
            mappingId,
            dayStockMovementDate,
            earningsDateId,
            preDayClose,
            curDayOpen,
            curDayClose,
            curDayHigh,
            curDayLow,
            curDayVolWeight,
            curDayVolume,
            changePercent,
            dayOpeningChangePercent,
            false);
    }

    public int upsertDayStockMovementEntry(Long stockId,
                                           Long mappingId,
                                           Timestamp dayStockMovementDate,
                                           Long earningsDateId,
                                           double preDayClose, double curDayOpen, double curDayClose,
                                           double curDayHigh, double curDayLow, double curDayVolWeight,
                                           long curDayVolume, Double changePercent, Double dayOpeningChangePercent,
                                           boolean earnings) {
        String sql = "INSERT INTO day_stock_movement_entry (stock_id, day_stock_movement_date, earnings_date_id, day_stock_movement_map_id, pre_day_close, cur_day_open, cur_day_close, cur_day_high, cur_day_low, cur_day_vol_weight, cur_day_volume, change_percent, day_opening_change_percent, earnings) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE stock_id = VALUES(stock_id), day_stock_movement_date = VALUES(day_stock_movement_date), earnings_date_id = VALUES(earnings_date_id), day_stock_movement_map_id = VALUES(day_stock_movement_map_id), pre_day_close = VALUES(pre_day_close), cur_day_open = VALUES(cur_day_open), cur_day_close = VALUES(cur_day_close), cur_day_high = VALUES(cur_day_high), cur_day_low = VALUES(cur_day_low), cur_day_vol_weight = VALUES(cur_day_vol_weight), cur_day_volume = VALUES(cur_day_volume), change_percent = VALUES(change_percent), day_opening_change_percent = VALUES(day_opening_change_percent), earnings = VALUES(earnings)";
        return jdbc.update(sql, stockId, dayStockMovementDate, earningsDateId, mappingId, preDayClose, curDayOpen, curDayClose, curDayHigh, curDayLow, curDayVolWeight, curDayVolume, changePercent, dayOpeningChangePercent, earnings);
    }

    public int insertEarningsEntry(Long stockId,
                                   Timestamp dayStockMovementDate,
                                   Long earningsDateId,
                                   double preDayClose, double curDayOpen, double curDayClose,
                                   double curDayHigh, double curDayLow, double curDayVolWeight,
                                   long curDayVolume, Double changePercent, Double dayOpeningChangePercent,
                                   boolean earnings) {
        return upsertDayStockMovementEntry(
            stockId,
            null,
            dayStockMovementDate,
            earningsDateId,
            preDayClose,
            curDayOpen,
            curDayClose,
            curDayHigh,
            curDayLow,
            curDayVolWeight,
            curDayVolume,
            changePercent,
            dayOpeningChangePercent,
            earnings);
    }

    public java.util.List<java.util.Map<String, Object>> listAllEntriesWithMeta() {
        String sql = "SELECT e.*, s.ticker as ticker, e.day_stock_movement_date as master_event_date "
            + "FROM day_stock_movement_entry e "
            + "JOIN stock s ON e.stock_id = s.id "
            + "ORDER BY s.ticker, e.day_stock_movement_date DESC";
        return jdbc.queryForList(sql);
    }

    public java.util.List<String> listDistinctEntryTickers() {
        String sql = "SELECT DISTINCT s.ticker FROM day_stock_movement_entry e "
            + "JOIN stock s ON e.stock_id = s.id ORDER BY s.ticker";
        return jdbc.queryForList(sql, String.class);
    }
}
