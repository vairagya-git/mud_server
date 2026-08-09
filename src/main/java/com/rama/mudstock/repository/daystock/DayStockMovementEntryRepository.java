package com.rama.mudstock.repository.daystock;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.daystock.StockMovementData;

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

    public List<StockMovementData> listStockMovementData() {
        String sql = "SELECT e.*, s.ticker as ticker, e.day_stock_movement_date as master_event_date "
            + "FROM day_stock_movement_entry e "
            + "JOIN stock s ON e.stock_id = s.id "
            + "ORDER BY s.ticker, e.day_stock_movement_date DESC";
        return jdbc.query(sql, (rs, rowNum) -> {
            StockMovementData row = new StockMovementData();
            row.setId(rs.getLong("id"));
            row.setStockId(rs.getLong("stock_id"));

            long earningsDateId = rs.getLong("earnings_date_id");
            row.setEarningsDateId(rs.wasNull() ? null : earningsDateId);

            row.setPreDayClose(rs.getBigDecimal("pre_day_close"));
            row.setCurDayOpen(rs.getBigDecimal("cur_day_open"));
            row.setCurDayClose(rs.getBigDecimal("cur_day_close"));
            row.setCurDayHigh(rs.getBigDecimal("cur_day_high"));
            row.setCurDayHighSnapShotDatetime(rs.getString("cur_day_high_snap_shot_datetime"));
            row.setCurDayHighFlatFileDatetime(rs.getString("cur_day_high_flat_file_datetime"));
            row.setCurDayLow(rs.getBigDecimal("cur_day_low"));
            row.setCurDayLowSnapShotDatetime(rs.getString("cur_day_low_snap_shot_datetime"));
            row.setCurDayLowFlatFileDatetime(rs.getString("cur_day_low_flat_file_datetime"));
            row.setCurDayVolWeight(rs.getBigDecimal("cur_day_vol_weight"));

            long curDayVolume = rs.getLong("cur_day_volume");
            row.setCurDayVolume(rs.wasNull() ? null : curDayVolume);

            row.setChangePercent(rs.getBigDecimal("change_percent"));
            row.setEarnings(rs.getBoolean("earnings"));
            row.setDayOpeningChangePercent(rs.getBigDecimal("day_opening_change_percent"));
            row.setDayStockMovementDate(rs.getTimestamp("day_stock_movement_date"));
            row.setTicker(rs.getString("ticker"));
            row.setMasterEventDate(rs.getTimestamp("master_event_date"));
            return row;
        });
    }

    public List<String> listDistinctEntryTickers() {
        String sql = "SELECT DISTINCT s.ticker FROM day_stock_movement_entry e "
            + "JOIN stock s ON e.stock_id = s.id ORDER BY s.ticker";
        return jdbc.queryForList(sql, String.class);
    }

    public int enrichPriceMatchDateTimes(List<Long> stockIds, LocalDate targetDate) {
        if (stockIds == null || stockIds.isEmpty() || targetDate == null) {
            return 0;
        }

        String placeholders = String.join(",", Collections.nCopies(stockIds.size(), "?"));
        String sql = "UPDATE day_stock_movement_entry dsme "
            + "SET dsme.cur_day_high_snap_shot_datetime = ("
            + "  SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(os.option_quote_time, '%H:%i') "
            + "    ORDER BY DATE_FORMAT(os.option_quote_time, '%H:%i') SEPARATOR ',') "
            + "  FROM option_snapshot os "
            + "  WHERE os.stock_id = dsme.stock_id "
            + "    AND DATE(os.option_quote_time) = DATE(dsme.day_stock_movement_date) "
            + "    AND TRUNCATE(os.underlying_price, 0) = TRUNCATE(dsme.cur_day_high, 0)"
            + "), "
            + "dsme.cur_day_low_snap_shot_datetime = ("
            + "  SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(os.option_quote_time, '%H:%i') "
            + "    ORDER BY DATE_FORMAT(os.option_quote_time, '%H:%i') SEPARATOR ',') "
            + "  FROM option_snapshot os "
            + "  WHERE os.stock_id = dsme.stock_id "
            + "    AND DATE(os.option_quote_time) = DATE(dsme.day_stock_movement_date) "
            + "    AND TRUNCATE(os.underlying_price, 0) = TRUNCATE(dsme.cur_day_low, 0)"
            + "), "
            + "dsme.cur_day_high_flat_file_datetime = ("
            + "  SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(osf.local_time, '%H:%i') "
            + "    ORDER BY DATE_FORMAT(osf.local_time, '%H:%i') SEPARATOR ',') "
            + "  FROM option_snapshot_flatfile osf "
            + "  WHERE osf.stock_id = dsme.stock_id "
            + "    AND DATE(osf.local_time) = DATE(dsme.day_stock_movement_date) "
            + "    AND TRUNCATE(osf.stock_open, 0) = TRUNCATE(dsme.cur_day_high, 0)"
            + "), "
            + "dsme.cur_day_low_flat_file_datetime = ("
            + "  SELECT GROUP_CONCAT(DISTINCT DATE_FORMAT(osf.local_time, '%H:%i') "
            + "    ORDER BY DATE_FORMAT(osf.local_time, '%H:%i') SEPARATOR ',') "
            + "  FROM option_snapshot_flatfile osf "
            + "  WHERE osf.stock_id = dsme.stock_id "
            + "    AND DATE(osf.local_time) = DATE(dsme.day_stock_movement_date) "
            + "    AND TRUNCATE(osf.stock_open, 0) = TRUNCATE(dsme.cur_day_low, 0)"
            + ") "
            + "WHERE DATE(dsme.day_stock_movement_date) = ? "
            + "AND dsme.stock_id IN (" + placeholders + ")";

        List<Object> args = new ArrayList<>();
        args.add(targetDate);
        args.addAll(stockIds);
        return jdbc.update(sql, args.toArray());
    }
}
