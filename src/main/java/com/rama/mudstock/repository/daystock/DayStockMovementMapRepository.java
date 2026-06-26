package com.rama.mudstock.repository.daystock;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DayStockMovementMapRepository {
    private final JdbcTemplate jdbc;
    private static final Logger log = LoggerFactory.getLogger(DayStockMovementMapRepository.class);

    public DayStockMovementMapRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> listAllMappings() {
        String sql = "SELECT m.id as map_id, m.stock_id as stock_id, s.ticker as ticker, m.day_stock_movement_key_id as day_stock_movement_key_id, d.code as code, d.date as date, m.status as status "
            + "FROM day_stock_movement_map m JOIN stock s ON m.stock_id = s.id JOIN day_stock_movement_key d ON m.day_stock_movement_key_id = d.id ORDER BY s.ticker, d.code";
        return jdbc.queryForList(sql);
    }

    public int createMapping(Long stockId, Long movementKeyId) {
        String sql = "INSERT INTO day_stock_movement_map (stock_id, day_stock_movement_key_id, status) VALUES (?,?, 'NEW')";
        return jdbc.update(sql, stockId, movementKeyId);
    }

    public int deleteMapping(Long stockId, Long movementKeyId) {
        String sql = "DELETE FROM day_stock_movement_map WHERE stock_id = ? AND day_stock_movement_key_id = ?";
        return jdbc.update(sql, stockId, movementKeyId);
    }

    /** Deletes day_stock_movement_entry rows whose mapping belongs to the given key. Call before deleting mappings (FK order). */
    public int deleteEntriesByMasterId(Long movementKeyId) {
        String sql = "DELETE FROM day_stock_movement_entry WHERE day_stock_movement_map_id IN "
            + "(SELECT id FROM day_stock_movement_map WHERE day_stock_movement_key_id = ?)";
        return jdbc.update(sql, movementKeyId);
    }

    /** Deletes all day_stock_movement_map rows for the given key. */
    public int deleteMappingsByMasterId(Long movementKeyId) {
        return jdbc.update("DELETE FROM day_stock_movement_map WHERE day_stock_movement_key_id = ?", movementKeyId);
    }

    /** Deletes the day_stock_movement_key row itself. */
    public int deleteMasterById(Long movementKeyId) {
        return jdbc.update("DELETE FROM day_stock_movement_key WHERE id = ?", movementKeyId);
    }

    public List<Map<String,Object>> listMappingsByStatus(String status) {
        String sql = "SELECT m.id as map_id, m.stock_id as stock_id, s.ticker as ticker, m.day_stock_movement_key_id as day_stock_movement_key_id, d.code as code, d.date as date, m.status as status "
            + "FROM day_stock_movement_map m JOIN stock s ON m.stock_id = s.id JOIN day_stock_movement_key d ON m.day_stock_movement_key_id = d.id WHERE UPPER(m.status) = UPPER(?) ORDER BY s.ticker, d.code";
        return jdbc.queryForList(sql, status);
    }

    public int updateStatus(Long mappingId, String status) {
        String sql = "UPDATE day_stock_movement_map SET status = ? WHERE id = ?";
        try {
            String normalized = status == null ? null : status.toUpperCase();
            log.debug("Executing SQL: {} with params [{}, {}]", sql, normalized, mappingId);
            int updated = jdbc.update(sql, normalized, mappingId);
            log.debug("Update result: {} rows affected for id={}", updated, mappingId);
            return updated;
        } catch (Exception ex) {
            log.error("Failed to execute updateStatus for id={}", mappingId, ex);
            throw ex;
        }
    }
}
