package com.rama.mudstock.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.DatePeriod;

@Repository
public class EarningsDateEntryRepository {
    private final JdbcTemplate jdbc;

    public EarningsDateEntryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void createEntriesForEarningsDate(Long earningsDateId, Long stockId) {
        String sql = "INSERT INTO earnings_date_entry (stock_id, earnings_date_id, datePeriod, `from`, `Open`, `close`, `high`, `low`, `volume`, `percentage`, `value`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        for (DatePeriod dp : DatePeriod.values()) {
            jdbc.update(sql,
                stockId,
                earningsDateId,
                dp.getDbValue(),
                null,
                0,0,0,0,0,0,0
            );
        }
    }

    public void updateEntryForEarningsDate(Long earningsDateId, Long stockId, String datePeriod, String from, java.math.BigDecimal open, java.math.BigDecimal high, java.math.BigDecimal low, java.math.BigDecimal close, java.math.BigDecimal volume) {
        String sql = "UPDATE earnings_date_entry SET `from` = ?, `Open` = ?, `high` = ?, `low` = ?, `close` = ?, `volume` = ? WHERE earnings_date_id = ? AND stock_id = ? AND datePeriod = ?";
        jdbc.update(sql, from, open, high, low, close, volume, earningsDateId, stockId, datePeriod);
    }
}
