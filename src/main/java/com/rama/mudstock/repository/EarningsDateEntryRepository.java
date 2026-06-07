package com.rama.mudstock.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.DatePeriod;

@Repository
public class EarningsDateEntryRepository {
    private final JdbcTemplate jdbc;

    public EarningsDateEntryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void createEntriesForEarningsDate(Long earningsDateId, Long stockId) {
        String sql = "INSERT INTO earnings_date_entry (stock_id, earnings_date_id, datePeriod, `from`, `Open`, `close`, `high`, `low`, `volume`, `percentage`, `value`, `status`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        for (DatePeriod dp : DatePeriod.values()) {
            jdbc.update(sql,
                stockId,
                earningsDateId,
                dp.getDbValue(),
                null,
                0,0,0,0,0,0,0,
                "new"
            );
        }
    }

    public int updateEntryForEarningsDate(Long earningsDateId, Long stockId, String datePeriod, String from, java.math.BigDecimal open, java.math.BigDecimal high, java.math.BigDecimal low, java.math.BigDecimal close, java.math.BigDecimal volume) {
        // Only update if current status is 'new' to avoid re-populating already-processed rows.
        String sql = "UPDATE earnings_date_entry SET `from` = ?, `Open` = ?, `high` = ?, `low` = ?, `close` = ?, `volume` = ?, `status` = 'done' WHERE earnings_date_id = ? AND stock_id = ? AND datePeriod = ? AND `status` = 'new'";
        return jdbc.update(sql, from, open, high, low, close, volume, earningsDateId, stockId, datePeriod);
    }

    public String getEntryStatus(Long earningsDateId, Long stockId, String datePeriod) {
        String sql = "SELECT `status` FROM earnings_date_entry WHERE earnings_date_id = ? AND stock_id = ? AND datePeriod = ?";
        try {
            return jdbc.queryForObject(sql, new Object[]{earningsDateId, stockId, datePeriod}, String.class);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public int setEntryStatusToNull(Long earningsDateId, Long stockId, String datePeriod) {
        String sql = "UPDATE earnings_date_entry SET `status` = NULL WHERE earnings_date_id = ? AND stock_id = ? AND datePeriod = ?";
        return jdbc.update(sql, earningsDateId, stockId, datePeriod);
    }

    public int setEntryStatusToDone(Long earningsDateId, Long stockId, String datePeriod) {
        String sql = "UPDATE earnings_date_entry SET `status` = 'done' WHERE earnings_date_id = ? AND stock_id = ? AND datePeriod = ?";
        return jdbc.update(sql, earningsDateId, stockId, datePeriod);
    }

    public int deleteEntryForEarningsDate(Long earningsDateId, Long stockId, String datePeriod) {
        String sql = "DELETE FROM earnings_date_entry WHERE earnings_date_id = ? AND stock_id = ? AND datePeriod = ?";
        return jdbc.update(sql, earningsDateId, stockId, datePeriod);
    }

    public boolean allEntriesDoneForEarningsDate(Long earningsDateId) {
        String sql = "SELECT COUNT(*) FROM earnings_date_entry WHERE earnings_date_id = ? AND (status IS NULL OR status <> 'done')";
        Integer count = jdbc.queryForObject(sql, Integer.class, earningsDateId);
        return count != null && count == 0;
    }
}
