package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OptionContractRepository {

    private final JdbcTemplate jdbc;

    public OptionContractRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int upsert(Long stockId,
                      String contractType,
                      String exerciseStyle,
                      LocalDate expirationDate,
                      BigDecimal strikePrice,
                      int sharesPerContract,
                      String contractTicker) {
        String sql = "INSERT INTO option_contract "
            + "(stock_id, contract_type, exercise_style, expiration_date, strike_price, shares_per_contract, contract_ticker) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "exercise_style = VALUES(exercise_style), "
            + "strike_price = VALUES(strike_price), "
            + "shares_per_contract = VALUES(shares_per_contract), "
            + "updated_at = CURRENT_TIMESTAMP";
        return jdbc.update(sql, stockId, contractType, exerciseStyle, expirationDate, strikePrice, sharesPerContract, contractTicker);
    }

    public boolean existsByUniqueKey(Long stockId,
                                     String contractType,
                                     LocalDate expirationDate,
                                     String contractTicker) {
        String sql = "SELECT COUNT(*) FROM option_contract "
            + "WHERE stock_id = ? "
            + "AND contract_type = ? "
            + "AND expiration_date = ? "
            + "AND contract_ticker <=> ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, stockId, contractType, expirationDate, contractTicker);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> listAllWithTicker() {
        String sql = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.exercise_style, o.expiration_date, "
            + "o.strike_price, o.shares_per_contract, o.contract_ticker, o.created_at, o.updated_at "
            + "FROM option_contract o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "ORDER BY o.updated_at DESC, s.ticker, o.expiration_date, o.strike_price";
        return jdbc.queryForList(sql);
    }
}