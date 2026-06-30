package com.rama.mudstock.repository.daystock;

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

    public java.util.List<java.util.Map<String,Object>> listEntriesWithMeta(java.util.List<String> tickers, java.util.List<String> dayCodes) {
        StringBuilder sql = new StringBuilder(
            "SELECT e.*, s.ticker as ticker, d.code as day_code, d.date as master_event_date "
            + "FROM day_stock_movement_entry e "
            + "JOIN day_stock_movement_map m ON e.day_stock_movement_map_id = m.id "
            + "JOIN stock s ON m.stock_id = s.id "
            + "JOIN day_stock_movement_key d ON m.day_stock_movement_key_id = d.id ");

        java.util.List<Object> params = new java.util.ArrayList<>();
        java.util.List<String> conditions = new java.util.ArrayList<>();

        java.util.List<String> effectiveTickers = nonBlank(tickers);
        if (!effectiveTickers.isEmpty()) {
            conditions.add("UPPER(s.ticker) IN (" + placeholders(effectiveTickers.size()) + ")");
            effectiveTickers.stream().map(String::trim).map(String::toUpperCase).forEach(params::add);
        }

        java.util.List<String> effectiveDayCodes = nonBlank(dayCodes);
        if (!effectiveDayCodes.isEmpty()) {
            conditions.add("d.code IN (" + placeholders(effectiveDayCodes.size()) + ")");
            effectiveDayCodes.forEach(params::add);
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }
        sql.append("ORDER BY s.ticker, d.date DESC");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    private static String placeholders(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> "?").collect(java.util.stream.Collectors.joining(","));
    }

    private static java.util.List<String> nonBlank(java.util.List<String> values) {
        if (values == null) {
            return java.util.Collections.emptyList();
        }
        return values.stream().filter(v -> v != null && !v.isBlank()).collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<String> listDistinctEntryTickers() {
        String sql = "SELECT DISTINCT s.ticker FROM day_stock_movement_entry e "
            + "JOIN day_stock_movement_map m ON e.day_stock_movement_map_id = m.id "
            + "JOIN stock s ON m.stock_id = s.id ORDER BY s.ticker";
        return jdbc.queryForList(sql, String.class);
    }

    public java.util.List<String> listDistinctEntryCodes() {
        String sql = "SELECT DISTINCT d.code FROM day_stock_movement_entry e "
            + "JOIN day_stock_movement_map m ON e.day_stock_movement_map_id = m.id "
            + "JOIN day_stock_movement_key d ON m.day_stock_movement_key_id = d.id ORDER BY d.code";
        return jdbc.queryForList(sql, String.class);
    }
}
