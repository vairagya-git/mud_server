package com.rama.mudstock.repository.daystock;

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
