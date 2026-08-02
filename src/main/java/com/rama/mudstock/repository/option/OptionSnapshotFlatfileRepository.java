package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}
