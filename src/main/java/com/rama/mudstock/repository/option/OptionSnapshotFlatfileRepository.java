package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.option.OptionFlatFile;

@Repository
public class OptionSnapshotFlatfileRepository {

    private final JdbcTemplate jdbc;

    public OptionSnapshotFlatfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long optionContractId,
                      Long stockId,
                      String contractTicker,
                      Integer optVolume,
                      BigDecimal optOpen,
                      BigDecimal optClose,
                      BigDecimal optHigh,
                      BigDecimal optLow,
                      Long unixTime,
                      Timestamp unixUtcTime,
                      Timestamp localTime,
                      String stockTicker,
                      Integer stockVolume,
                      BigDecimal stockOpen,
                      BigDecimal stockClose,
                      BigDecimal stockHigh,
                      BigDecimal stockLow,
                      Long snapshotVersion,
                      Long nearOptionSnapshotId) {
        String sql = "INSERT INTO option_snapshot_flatfile "
            + "(option_contract_id, stock_id, contract_ticker, opt_volume, opt_open, opt_close, opt_high, opt_low, "
            + "unix_time, unix_utc_time, local_time, stock_ticker, stock_volume, stock_open, stock_close, stock_high, stock_low, snapshot_version, near_option_snapshot_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbc.update(sql,
            optionContractId,
            stockId,
            contractTicker,
            optVolume,
            optOpen,
            optClose,
            optHigh,
            optLow,
            unixTime,
            unixUtcTime,
            localTime,
            stockTicker,
            stockVolume,
            stockOpen,
            stockClose,
            stockHigh,
            stockLow,
            snapshotVersion,
            nearOptionSnapshotId);
    }

    public List<OptionFlatFile> listByContractId(Long optionContractId) {
        String sql = "SELECT local_time, opt_volume, opt_open, opt_close, opt_high, opt_low, "
            + "stock_volume, stock_open, stock_close, stock_high, stock_low "
            + "FROM option_snapshot_flatfile "
            + "WHERE option_contract_id = ? "
            + "ORDER BY local_time DESC";

        return jdbc.query(sql, (rs, rowNum) -> {
            OptionFlatFile row = new OptionFlatFile();
            row.setLocalTime(rs.getTimestamp("local_time"));
            int optVolume = rs.getInt("opt_volume");
            row.setOptVolume(rs.wasNull() ? null : optVolume);
            row.setOptOpen(rs.getBigDecimal("opt_open"));
            row.setOptClose(rs.getBigDecimal("opt_close"));
            row.setOptHigh(rs.getBigDecimal("opt_high"));
            row.setOptLow(rs.getBigDecimal("opt_low"));
            int stockVolume = rs.getInt("stock_volume");
            row.setStockVolume(rs.wasNull() ? null : stockVolume);
            row.setStockOpen(rs.getBigDecimal("stock_open"));
            row.setStockClose(rs.getBigDecimal("stock_close"));
            row.setStockHigh(rs.getBigDecimal("stock_high"));
            row.setStockLow(rs.getBigDecimal("stock_low"));
            return row;
        }, optionContractId);
    }

    public FlatFileLookupRow findNearestFlatFileByContractAndTime(Long optionContractId, Timestamp targetTime) {
        if (optionContractId == null || targetTime == null) {
            return null;
        }

        String sql = "SELECT id, local_time, near_option_snapshot_id, opt_close "
            + "FROM option_snapshot_flatfile "
            + "WHERE option_contract_id = ? "
            + "ORDER BY ABS(TIMESTAMPDIFF(SECOND, local_time, ?)), local_time DESC "
            + "LIMIT 1";

        List<FlatFileLookupRow> rows = jdbc.query(sql, (rs, rowNum) -> new FlatFileLookupRow(
            rs.getLong("id"),
            rs.getTimestamp("local_time"),
            getNullableLong(rs, "near_option_snapshot_id"),
            rs.getBigDecimal("opt_close")),
            optionContractId,
            targetTime);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long getNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public record FlatFileLookupRow(Long id,
                                    Timestamp localTime,
                                    Long nearOptionSnapshotId,
                                    BigDecimal optClose) {
    }
}
