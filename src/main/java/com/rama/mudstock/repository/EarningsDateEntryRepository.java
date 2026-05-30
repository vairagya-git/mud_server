package com.rama.mudstock.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.DatePeriod;

@Repository
public class EarningsDateEntryRepository {
    private final JdbcTemplate jdbc;

    public EarningsDateEntryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void createEntriesForEarningsDate(Long earningsDateId, Long stockId) {
        String sql = "INSERT INTO earnings_date_entry (stock_id, earnings_date_id, datePeriod, `Open`, `close`, `high`, `low`, `volume`, `percentage`, `value`) VALUES (?,?,?,?,?,?,?,?,?,?)";
        for (DatePeriod dp : DatePeriod.values()) {
            jdbc.update(sql,
                stockId,
                earningsDateId,
                dp.getDbValue(),
                0,0,0,0,0,0,0
            );
        }
    }
}
