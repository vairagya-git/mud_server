package com.rama.mudstock.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DayEventEntryRepository {
    private final JdbcTemplate jdbc;

    public DayEventEntryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Insert or update a day_event_entry row for the given stock and day event.
     * Uses snake_case column names commonly used in DB schema.
     */
        public int upsertDayEventEntry(Long dayEventMapId,
                       double preDayClose, double curDayOpen, double curDayClose,
                       double curDayHigh, double curDayLow, double curDayVolWeight,
                       long curDayVolume, Double changePercent, Double dayOpeningChangePercent) {
        String sql = "INSERT INTO day_event_entry (day_event_map_id, pre_day_close, cur_day_open, cur_day_close, cur_day_high, cur_day_low, cur_day_vol_weight, cur_day_volume, change_percent, day_opening_change_percent) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE pre_day_close = VALUES(pre_day_close), cur_day_open = VALUES(cur_day_open), cur_day_close = VALUES(cur_day_close), cur_day_high = VALUES(cur_day_high), cur_day_low = VALUES(cur_day_low), cur_day_vol_weight = VALUES(cur_day_vol_weight), cur_day_volume = VALUES(cur_day_volume), change_percent = VALUES(change_percent), day_opening_change_percent = VALUES(day_opening_change_percent)";
        return jdbc.update(sql, dayEventMapId, preDayClose, curDayOpen, curDayClose, curDayHigh, curDayLow, curDayVolWeight, curDayVolume, changePercent, dayOpeningChangePercent);
        }

    public java.util.List<java.util.Map<String,Object>> listAllEntriesWithMeta() {
        // day_event_entry references day_event_map via day_event_map_id; join through it to reach stock and master
        String sql = "SELECT e.*, s.ticker as ticker, d.code as day_code, d.event_date as master_event_date "
            + "FROM day_event_entry e "
            + "JOIN day_event_map m ON e.day_event_map_id = m.id "
            + "JOIN stock s ON m.stock_id = s.id "
            + "JOIN day_event_master d ON m.day_event_master_id = d.id "
            + "ORDER BY s.ticker, d.event_date DESC";
        return jdbc.queryForList(sql);
    }
}
