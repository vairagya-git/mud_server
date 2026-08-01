package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionIntervalAnalyseStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;

@Repository
public class OptionContractRepository {

    private final JdbcTemplate jdbc;

    public OptionContractRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int upsert(Long stockId,
                      String contractType,
                      String source,
                      String exerciseStyle,
                      LocalDate expirationDate,
                      BigDecimal strikePrice,
                      int sharesPerContract,
                      String contractTicker,
                      Long optionsIntervalAnalyseId) {
        OptionSourceEnum resolvedSource = OptionSourceEnum.fromValue(source);
        String normalizedSource = (resolvedSource != null ? resolvedSource : OptionSourceEnum.API).name();

        String sql = "INSERT INTO option_contract "
            + "(stock_id, contract_type, source, exercise_style, expiration_date, strike_price, shares_per_contract, contract_ticker, options_interval_analyse_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "source = VALUES(source), "
            + "exercise_style = VALUES(exercise_style), "
            + "strike_price = VALUES(strike_price), "
            + "shares_per_contract = VALUES(shares_per_contract), "
            + "options_interval_analyse_id = VALUES(options_interval_analyse_id), "
            + "updated_at = CURRENT_TIMESTAMP";
        return jdbc.update(sql, stockId, contractType, normalizedSource, exerciseStyle, expirationDate, strikePrice, sharesPerContract, contractTicker, optionsIntervalAnalyseId);
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

    public List<Map<String, Object>> getOptionContractsWithTickerByStatus(String status,
                                                                           boolean snapshotFetchOnly,
                                                                           List<String> sources) {
        String selectClause = snapshotFetchOnly
            ? "SELECT o.id, o.stock_id, s.ticker, o.contract_ticker, o.strike_price, o.expiration_date "
            : "SELECT o.id, o.stock_id, s.ticker, o.contract_type, "
                + "o.source, "
                + "o.status, o.exercise_style, o.expiration_date, "
                + "o.strike_price, o.shares_per_contract, o.contract_ticker, o.created_at, o.updated_at ";

        StringBuilder sql = new StringBuilder(selectClause)
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ");

        boolean hasStatus = status != null;
        boolean hasSources = sources != null && !sources.isEmpty();

        if (hasStatus || hasSources || snapshotFetchOnly) {
            sql.append("WHERE 1=1 ");
            if (hasStatus) {
                sql.append("AND UPPER(o.status) = UPPER(?) ");
            }
            if (hasSources) {
                String placeholders = String.join(",", java.util.Collections.nCopies(sources.size(), "UPPER(?)"));
                sql.append("AND UPPER(o.source) IN (").append(placeholders).append(") ");
            }
            if (snapshotFetchOnly) {
                sql.append("AND s.ticker IS NOT NULL AND s.ticker <> '' ");
            }
        }

        if (snapshotFetchOnly) {
            sql.append("ORDER BY o.updated_at DESC");
        } else if (hasStatus) {
            sql.append("ORDER BY s.ticker, o.expiration_date, o.strike_price, o.contract_type");
        } else {
            sql.append("ORDER BY o.updated_at DESC, s.ticker, o.expiration_date, o.strike_price");
        }

        List<Object> params = new java.util.ArrayList<>();
        if (hasStatus) {
            params.add(status);
        }
        if (hasSources) {
            params.addAll(sources);
        }

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<String> listDistinctTickersByStatus(String status) {
        StringBuilder sql = new StringBuilder()
            .append("SELECT DISTINCT s.ticker ")
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ")
            .append("WHERE s.ticker IS NOT NULL AND s.ticker <> '' ");

        boolean hasStatus = status != null && !status.isBlank();
        if (hasStatus) {
            sql.append("AND UPPER(o.status) = UPPER(?) ");
        }

        sql.append("ORDER BY s.ticker");

        if (hasStatus) {
            return jdbc.queryForList(sql.toString(), String.class, status);
        }
        return jdbc.queryForList(sql.toString(), String.class);
    }

    public List<LocalDate> listDistinctExpirationDatesByStatus(String status) {
        StringBuilder sql = new StringBuilder()
            .append("SELECT DISTINCT o.expiration_date ")
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ")
            .append("WHERE o.expiration_date IS NOT NULL ")
            .append("AND s.ticker IS NOT NULL AND s.ticker <> '' ");

        boolean hasStatus = status != null && !status.isBlank();
        if (hasStatus) {
            sql.append("AND UPPER(o.status) = UPPER(?) ");
        }

        sql.append("ORDER BY o.expiration_date");

        if (hasStatus) {
            return jdbc.queryForList(sql.toString(), LocalDate.class, status);
        }
        return jdbc.queryForList(sql.toString(), LocalDate.class);
    }

    public List<Map<String, Object>> listActiveContractsForSimulator() {
        String sql = "SELECT o.id, s.ticker, o.contract_ticker, o.contract_type, o.expiration_date, o.strike_price "
            + "FROM option_contract o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "WHERE UPPER(o.status) = UPPER(?) "
            + "AND s.ticker IS NOT NULL AND s.ticker <> '' "
            + "AND o.expiration_date IS NOT NULL "
            + "ORDER BY s.ticker, o.expiration_date, o.contract_type, o.strike_price";
        return jdbc.queryForList(sql, OptionIntervalAnalyseStatusEnum.ACTIVE.name());
    }

    public int markContractsStatusForInterval(Long optionsIntervalAnalyseId, String status) {
        if (optionsIntervalAnalyseId == null || status == null || status.isBlank()) {
            return 0;
        }

        String sql = "UPDATE option_contract "
            + "SET status = ?, updated_at = CURRENT_TIMESTAMP "
            + "WHERE options_interval_analyse_id = ? "
            + "AND status <> ?";
        return jdbc.update(sql, status, optionsIntervalAnalyseId, status);
    }

    public int markContractCompletedById(Long contractId) {
        String completedStatus = OptionIntervalAnalyseStatusEnum.COMPLETED.name();
        String sql = "UPDATE option_contract "
            + "SET status = ?, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = ? "
            + "AND status <> ?";
        return jdbc.update(sql, completedStatus, contractId, completedStatus);
    }
}