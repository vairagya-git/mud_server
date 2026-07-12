package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OptionToAnalyseRepository {

    public static final String STATUS_CREATE_CONTRACT = "CREATE_CONTRACT";
    public static final String STATUS_ACTIVE = "ACTIVE";
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

    public List<Map<String, Object>> listAllWithTicker() {
        String sql = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.status, o.expiration_date, "
            + "o.strike_from, o.strike_to, o.`interval`, o.created_at, o.updated_at "
            + "FROM options_interval_analyse o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "ORDER BY o.updated_at DESC";
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listCreateContractWithTicker() {
        String sql = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.status, o.expiration_date, "
            + "o.strike_from, o.strike_to, o.`interval`, o.created_at, o.updated_at "
            + "FROM options_interval_analyse o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "WHERE UPPER(o.status) = UPPER(?) "
            + "ORDER BY s.ticker, o.expiration_date, o.strike_from";
        return jdbc.queryForList(sql, STATUS_CREATE_CONTRACT);
    }

    public List<Map<String, Object>> listCloseWithTicker() {
        String sql = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.status, o.expiration_date, "
            + "o.strike_from, o.strike_to, o.`interval`, o.created_at, o.updated_at "
            + "FROM options_interval_analyse o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "WHERE UPPER(o.status) = UPPER(?) "
            + "ORDER BY s.ticker, o.expiration_date, o.strike_from";
        return jdbc.queryForList(sql, STATUS_CLOSE);
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