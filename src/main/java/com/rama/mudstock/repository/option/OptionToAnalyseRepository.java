package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.option.OptionsInternalAnalyseEntity;

@Repository
public class OptionToAnalyseRepository {

    public static final String STATUS_CREATE_CONTRACT = "CREATE_CONTRACT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PARTIALLY_COMPLETED = "PARTIALLY_COMPLETED";
    public static final String STATUS_CLOSE = "CLOSE";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private final JdbcTemplate jdbc;

    public OptionToAnalyseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long stockId,
                      String contractType,
                      String status,
                      LocalDate expirationDate,
                      BigDecimal strikeFrom,
                      BigDecimal strikeTo,
                      BigDecimal interval) {
        String sql = "INSERT INTO options_interval_analyse "
            + "(stock_id, contract_type, status, expiration_date, strike_from, strike_to, `interval`) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        return jdbc.update(sql, stockId, contractType, status, expirationDate, strikeFrom, strikeTo, interval);
    }

    public List<OptionsInternalAnalyseEntity> getOptionsInternalAnalyseByStatus(String status) {
        String baseSql = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.status, o.expiration_date, "
            + "o.strike_from, o.strike_to, o.`interval`, o.created_at, o.updated_at "
            + "FROM options_interval_analyse o "
            + "JOIN stock s ON s.id = o.stock_id ";
        String orderBy = " ORDER BY s.ticker, o.expiration_date, o.strike_from";

        RowMapper<OptionsInternalAnalyseEntity> mapper = (rs, rowNum) -> new OptionsInternalAnalyseEntity(
            rs.getObject("id", Long.class),
            rs.getObject("stock_id", Long.class),
            rs.getString("ticker"),
            rs.getString("contract_type"),
            rs.getString("status"),
            rs.getObject("expiration_date", LocalDate.class),
            rs.getBigDecimal("strike_from"),
            rs.getBigDecimal("strike_to"),
            rs.getBigDecimal("interval"),
            rs.getObject("created_at", LocalDateTime.class),
            rs.getObject("updated_at", LocalDateTime.class));

        if (status == null || status.isBlank()) {
            return jdbc.query(baseSql + orderBy, mapper);
        }

        String sql = baseSql + "WHERE UPPER(o.status) = UPPER(?)" + orderBy;
        return jdbc.query(sql, mapper, status);
    }

    public Map<String, Object> findByIdWithTicker(Long id) {
        String sql = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.status, o.expiration_date, "
            + "o.strike_from, o.strike_to, o.`interval`, o.created_at, o.updated_at "
            + "FROM options_interval_analyse o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "WHERE o.id = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int updateById(Long id,
                          Long stockId,
                          String contractType,
                          String status,
                          LocalDate expirationDate,
                          BigDecimal strikeFrom,
                          BigDecimal strikeTo,
                          BigDecimal interval) {
        String sql = "UPDATE options_interval_analyse "
            + "SET stock_id = ?, contract_type = ?, status = ?, expiration_date = ?, strike_from = ?, strike_to = ?, `interval` = ? "
            + "WHERE id = ?";
        return jdbc.update(sql, stockId, contractType, status, expirationDate, strikeFrom, strikeTo, interval, id);
    }

    public int updateStatusById(Long id, String status) {
        String sql = "UPDATE options_interval_analyse SET status = ? WHERE id = ?";
        return jdbc.update(sql, status, id);
    }
}