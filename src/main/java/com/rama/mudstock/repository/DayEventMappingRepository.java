package com.rama.mudstock.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DayEventMappingRepository {
    private final JdbcTemplate jdbc;

    public DayEventMappingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> listAllMappings() {
        String sql = "SELECT m.id as day_event_map_id, m.stock_id as stock_id, s.ticker as ticker, m.day_event_master_id as day_event_master_id, d.code as code, d.event_date as event_date, m.status as status "
            + "FROM day_event_map m JOIN stock s ON m.stock_id = s.id JOIN day_event_master d ON m.day_event_master_id = d.id ORDER BY s.ticker, d.code";
        return jdbc.queryForList(sql);
    }

    public int createMapping(Long stockId, Long dayEventMasterId) {
        String sql = "INSERT INTO day_event_map (stock_id, day_event_master_id) VALUES (?,?)";
        return jdbc.update(sql, stockId, dayEventMasterId);
    }

    public int deleteMapping(Long stockId, Long dayEventMasterId) {
        String sql = "DELETE FROM day_event_map WHERE stock_id = ? AND day_event_master_id = ?";
        return jdbc.update(sql, stockId, dayEventMasterId);
    }

    public List<Map<String,Object>> listMappingsByStatus(String status) {
        String sql = "SELECT m.id as day_event_map_id, m.stock_id as stock_id, s.ticker as ticker, m.day_event_master_id as day_event_master_id, d.code as code, d.event_date as event_date, m.status as status "
            + "FROM day_event_map m JOIN stock s ON m.stock_id = s.id JOIN day_event_master d ON m.day_event_master_id = d.id WHERE m.status = ? ORDER BY s.ticker, d.code";
        return jdbc.queryForList(sql, status);
    }
}
