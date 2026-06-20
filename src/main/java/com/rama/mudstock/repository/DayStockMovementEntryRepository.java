package com.rama.mudstock.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DayStockMovementEntryRepository {
    private final JdbcTemplate jdbc;

    public DayStockMovementEntryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Insert or update a day_stock_movement_entry row for the given stock movement mapping.
     */
    public int upsertDayStockMovementEntry(Long mappingId,
                   double preDayClose, double curDayOpen, double curDayClose,
                   double curDayHigh, double curDayLow, double curDayVolWeight,
                   long curDayVolume, Double changePercent, Double dayOpeningChangePercent) {
        String sql = "INSERT INTO day_stock_movement_entry (day_stock_movement_map_id, pre_day_close, cur_day_open, cur_day_close, cur_day_high, cur_day_low, cur_day_vol_weight, cur_day_volume, change_percent, day_opening_change_percent) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE pre_day_close = VALUES(pre_day_close), cur_day_open = VALUES(cur_day_open), cur_day_close = VALUES(cur_day_close), cur_day_high = VALUES(cur_day_high), cur_day_low = VALUES(cur_day_low), cur_day_vol_weight = VALUES(cur_day_vol_weight), cur_day_volume = VALUES(cur_day_volume), change_percent = VALUES(change_percent), day_opening_change_percent = VALUES(day_opening_change_percent)";
        return jdbc.update(sql, mappingId, preDayClose, curDayOpen, curDayClose, curDayHigh, curDayLow, curDayVolWeight, curDayVolume, changePercent, dayOpeningChangePercent);
    }

    public java.util.List<java.util.Map<String,Object>> listAllEntriesWithMeta() {
        String sql = "SELECT e.*, s.ticker as ticker, d.code as day_code, d.date as master_event_date "
            + "FROM day_stock_movement_entry e "
            + "JOIN day_stock_movement_map m ON e.day_stock_movement_map_id = m.id "
            + "JOIN stock s ON m.stock_id = s.id "
            + "JOIN day_stock_movement_key d ON m.day_stock_movement_key_id = d.id "
            + "ORDER BY s.ticker, d.date DESC";
        return jdbc.queryForList(sql);
    }
}
