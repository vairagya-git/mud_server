package com.rama.mudstock.repository.daystock;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DayStockMovementEntryRepository {
    private final JdbcTemplate jdbc;

    public DayStockMovementEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Upserts a non-earnings day-stock movement entry by stock and market day.
     */
    public int upsertDayStockMovementEntry(Long stockId,
                                           Timestamp dayStockMovementDate,
                                           double preDayClose,
                                           double curDayOpen,
                                           double curDayClose,
                                           double curDayHigh,
                                           double curDayLow,
                                           double curDayVolWeight,
                                           long curDayVolume,
                                           Double changePercent,
                                           Double dayOpeningChangePercent) {
        String updateSql = "UPDATE day_stock_movement_entry "
            + "SET pre_day_close = ?, cur_day_open = ?, cur_day_close = ?, cur_day_high = ?, "
            + "cur_day_low = ?, cur_day_vol_weight = ?, cur_day_volume = ?, change_percent = ?, "
            + "day_opening_change_percent = ?, earnings = false "
            + "WHERE stock_id = ? AND DATE(day_stock_movement_date) = DATE(?) AND earnings_date_id IS NULL";

        int updated = jdbc.update(updateSql,
            preDayClose,
            curDayOpen,
            curDayClose,
            curDayHigh,
            curDayLow,
            curDayVolWeight,
            curDayVolume,
            changePercent,
            dayOpeningChangePercent,
            stockId,
            dayStockMovementDate);

        if (updated > 0) {
            return updated;
        }

        String insertSql = "INSERT INTO day_stock_movement_entry "
            + "(stock_id, day_stock_movement_date, earnings_date_id, pre_day_close, cur_day_open, cur_day_close, "
            + "cur_day_high, cur_day_low, cur_day_vol_weight, cur_day_volume, change_percent, day_opening_change_percent, earnings) "
            + "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, false)";

        return jdbc.update(insertSql,
            stockId,
            dayStockMovementDate,
            preDayClose,
            curDayOpen,
            curDayClose,
            curDayHigh,
            curDayLow,
            curDayVolWeight,
            curDayVolume,
            changePercent,
            dayOpeningChangePercent);
    }

    /**
     * Upserts an earnings-linked day-stock movement entry by earnings_date_id and market day.
     */
    public int upsertEarningsEntry(Long stockId,
                                   Timestamp dayStockMovementDate,
                                   Long earningsDateId,
                                   double preDayClose,
                                   double curDayOpen,
                                   double curDayClose,
                                   double curDayHigh,
                                   double curDayLow,
                                   double curDayVolWeight,
                                   long curDayVolume,
                                   Double changePercent,
                                   Double dayOpeningChangePercent) {
        String updateSql = "UPDATE day_stock_movement_entry "
            + "SET stock_id = ?, pre_day_close = ?, cur_day_open = ?, cur_day_close = ?, cur_day_high = ?, "
            + "cur_day_low = ?, cur_day_vol_weight = ?, cur_day_volume = ?, change_percent = ?, "
            + "day_opening_change_percent = ?, earnings = true "
            + "WHERE earnings_date_id = ? AND DATE(day_stock_movement_date) = DATE(?)";

        int updated = jdbc.update(updateSql,
            stockId,
            preDayClose,
            curDayOpen,
            curDayClose,
            curDayHigh,
            curDayLow,
            curDayVolWeight,
            curDayVolume,
            changePercent,
            dayOpeningChangePercent,
            earningsDateId,
            dayStockMovementDate);

        if (updated > 0) {
            return updated;
        }

        String insertSql = "INSERT INTO day_stock_movement_entry "
            + "(stock_id, day_stock_movement_date, earnings_date_id, pre_day_close, cur_day_open, cur_day_close, "
            + "cur_day_high, cur_day_low, cur_day_vol_weight, cur_day_volume, change_percent, day_opening_change_percent, earnings) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)";

        return jdbc.update(insertSql,
            stockId,
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
            dayOpeningChangePercent);
    }

    public List<Map<String, Object>> listAllEntriesWithMeta() {
        String sql = "SELECT e.*, s.ticker as ticker, e.day_stock_movement_date as master_event_date "
            + "FROM day_stock_movement_entry e "
            + "JOIN stock s ON e.stock_id = s.id "
            + "ORDER BY s.ticker, e.day_stock_movement_date DESC";
        return jdbc.queryForList(sql);
    }

    public List<String> listDistinctEntryTickers() {
        String sql = "SELECT DISTINCT s.ticker FROM day_stock_movement_entry e "
            + "JOIN stock s ON e.stock_id = s.id ORDER BY s.ticker";
        return jdbc.queryForList(sql, String.class);
    }
}
