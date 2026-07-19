package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OptionContractRepository {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";

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

    public List<Map<String, Object>> getOptionContractsWithTickerByStatus(String status, boolean snapshotFetchOnly) {
        String selectClause = snapshotFetchOnly
            ? "SELECT o.id, o.stock_id, s.ticker, o.contract_ticker, o.strike_price, o.expiration_date "
            : "SELECT o.id, o.stock_id, s.ticker, o.contract_type, o.status, o.exercise_style, o.expiration_date, "
                + "o.strike_price, o.shares_per_contract, o.contract_ticker, o.created_at, o.updated_at ";

        StringBuilder sql = new StringBuilder(selectClause)
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ");

        boolean hasStatus = status != null && !status.isBlank();
        if (hasStatus) {
            sql.append("WHERE UPPER(o.status) = UPPER(?) ");
            if (snapshotFetchOnly) {
                sql.append("AND s.ticker IS NOT NULL AND s.ticker <> '' ");
            }
        } else if (snapshotFetchOnly) {
            sql.append("WHERE s.ticker IS NOT NULL AND s.ticker <> '' ");
        }

        if (snapshotFetchOnly) {
            sql.append("ORDER BY o.updated_at DESC");
        } else if (hasStatus) {
            sql.append("ORDER BY s.ticker, o.expiration_date, o.strike_price, o.contract_type");
        } else {
            sql.append("ORDER BY o.updated_at DESC, s.ticker, o.expiration_date, o.strike_price");
        }

        if (hasStatus) {
            return jdbc.queryForList(sql.toString(), status);
        }
        return jdbc.queryForList(sql.toString());
    }

    public int markContractsCompletedForInterval(Long stockId,
                                                 String contractType,
                                                 LocalDate expirationDate,
                                                 BigDecimal strikeFrom,
                                                 BigDecimal strikeTo) {
        String normalizedContractType = contractType == null ? "" : contractType.trim().toUpperCase();

        if ("BOTH".equals(normalizedContractType)) {
            String sql = "UPDATE option_contract "
                + "SET status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE stock_id = ? "
                + "AND expiration_date = ? "
                + "AND strike_price >= ? "
                + "AND strike_price <= ? "
                + "AND status <> ?";
            return jdbc.update(sql,
                STATUS_COMPLETED,
                stockId,
                expirationDate,
                strikeFrom,
                strikeTo,
                STATUS_COMPLETED);
        }

        String sql = "UPDATE option_contract "
            + "SET status = ?, updated_at = CURRENT_TIMESTAMP "
            + "WHERE stock_id = ? "
            + "AND contract_type = ? "
            + "AND expiration_date = ? "
            + "AND strike_price >= ? "
            + "AND strike_price <= ? "
            + "AND status <> ?";
        return jdbc.update(sql,
            STATUS_COMPLETED,
            stockId,
            normalizedContractType,
            expirationDate,
            strikeFrom,
            strikeTo,
            STATUS_COMPLETED);
    }
}