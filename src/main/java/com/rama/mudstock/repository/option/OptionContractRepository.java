package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.enums.SystemRepositoryEnum.OptionIntervalAnalyseStatusEnum;
import com.rama.mudstock.enums.SystemRepositoryEnum.OptionSourceEnum;
import com.rama.mudstock.model.option.OptionContract;
import com.rama.mudstock.model.stockwatchlist.Stock;

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

    public List<Map<String, Object>> getOptionContractsWithTickerByStatus(List<String> statuses,
                                                                           boolean snapshotFetchOnly,
                                                                           List<String> sources) {
        String selectClause = snapshotFetchOnly
            ? "SELECT o.id, o.stock_id, s.ticker, o.contract_ticker, o.strike_price, o.expiration_date, o.options_interval_analyse_id "
            : "SELECT o.id, o.stock_id, s.ticker, o.contract_type, "
                + "o.source, "
                + "o.status, o.exercise_style, o.expiration_date, "
                + "o.strike_price, o.shares_per_contract, o.contract_ticker, o.created_at, o.updated_at ";

        StringBuilder sql = new StringBuilder(selectClause)
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ");

        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        boolean hasSources = sources != null && !sources.isEmpty();

        if (hasStatuses || hasSources || snapshotFetchOnly) {
            sql.append("WHERE 1=1 ");

            if (hasStatuses) {
                String statusPlaceholders = String.join(",", java.util.Collections.nCopies(statuses.size(), "UPPER(?)"));
                sql.append("AND UPPER(o.status) IN (").append(statusPlaceholders).append(") ");
            }

            if (hasSources) {
                String sourcePlaceholders = String.join(",", java.util.Collections.nCopies(sources.size(), "UPPER(?)"));
                sql.append("AND UPPER(o.source) IN (").append(sourcePlaceholders).append(") ");
            }

            if (snapshotFetchOnly) {
                sql.append("AND s.ticker IS NOT NULL AND s.ticker <> '' ");
            }
        }

        if (snapshotFetchOnly) {
            sql.append("ORDER BY o.updated_at DESC");
        } else if (hasStatuses) {
            sql.append("ORDER BY s.ticker, o.expiration_date, o.strike_price, o.contract_type");
        } else {
            sql.append("ORDER BY o.updated_at DESC, s.ticker, o.expiration_date, o.strike_price");
        }

        List<Object> params = new java.util.ArrayList<>();
        if (hasStatuses) {
            params.addAll(statuses);
        }
        if (hasSources) {
            params.addAll(sources);
        }

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<OptionContract> getOptionContractsByStatus(List<String> statuses,
                                                           boolean snapshotFetchOnly,
                                                           List<String> sources) {
        String selectClause = "SELECT o.id, o.stock_id, s.ticker, o.contract_type, "
            + "o.source, o.status, o.exercise_style, o.expiration_date, "
            + "o.strike_price, o.shares_per_contract, o.contract_ticker, o.created_at, o.updated_at ";

        StringBuilder sql = new StringBuilder(selectClause)
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ");

        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        boolean hasSources = sources != null && !sources.isEmpty();

        if (hasStatuses || hasSources || snapshotFetchOnly) {
            sql.append("WHERE 1=1 ");

            if (hasStatuses) {
                String statusPlaceholders = String.join(",", java.util.Collections.nCopies(statuses.size(), "UPPER(?)"));
                sql.append("AND UPPER(o.status) IN (").append(statusPlaceholders).append(") ");
            }

            if (hasSources) {
                String sourcePlaceholders = String.join(",", java.util.Collections.nCopies(sources.size(), "UPPER(?)"));
                sql.append("AND UPPER(o.source) IN (").append(sourcePlaceholders).append(") ");
            }

            if (snapshotFetchOnly) {
                sql.append("AND s.ticker IS NOT NULL AND s.ticker <> '' ");
            }
        }

        if (snapshotFetchOnly) {
            sql.append("ORDER BY o.updated_at DESC");
        } else if (hasStatuses) {
            sql.append("ORDER BY s.ticker, o.expiration_date, o.strike_price, o.contract_type");
        } else {
            sql.append("ORDER BY o.updated_at DESC, s.ticker, o.expiration_date, o.strike_price");
        }

        List<Object> params = new java.util.ArrayList<>();
        if (hasStatuses) {
            params.addAll(statuses);
        }
        if (hasSources) {
            params.addAll(sources);
        }

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            OptionContract contract = new OptionContract();
            contract.setId(rs.getLong("id"));
            contract.setStockId(rs.getLong("stock_id"));
            contract.setTicker(rs.getString("ticker"));
            contract.setContractType(rs.getString("contract_type"));
            contract.setSource(rs.getString("source"));
            contract.setStatus(rs.getString("status"));
            contract.setExerciseStyle(rs.getString("exercise_style"));
            java.sql.Date expiration = rs.getDate("expiration_date");
            contract.setExpirationDate(expiration == null ? null : expiration.toLocalDate());
            contract.setStrikePrice(rs.getBigDecimal("strike_price"));
            int shares = rs.getInt("shares_per_contract");
            contract.setSharesPerContract(rs.wasNull() ? null : shares);
            contract.setContractTicker(rs.getString("contract_ticker"));
            contract.setCreatedAt(rs.getTimestamp("created_at"));
            contract.setUpdatedAt(rs.getTimestamp("updated_at"));
            return contract;
        }, params.toArray());
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

    public List<Stock> listDistinctStocksByStatus() {
        StringBuilder sql = new StringBuilder()
            .append("SELECT s.id, s.ticker ")
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ")
            .append("WHERE s.ticker IS NOT NULL AND s.ticker <> '' ");

        sql.append("GROUP BY s.id, s.ticker ")
            .append("ORDER BY s.ticker");

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            Stock stock = new Stock();
            stock.setId(rs.getLong("id"));
            stock.setTicker(rs.getString("ticker"));
            return stock;
        });
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

    public List<OptionContract> getOptionContractExpirationForStock(String ticker) {
        StringBuilder sql = new StringBuilder()
            .append("SELECT MIN(o.id) AS id, o.stock_id, s.ticker, o.expiration_date ")
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ")
            .append("WHERE o.expiration_date IS NOT NULL ")
            .append("AND s.ticker IS NOT NULL AND s.ticker <> '' ");

        boolean hasTicker = ticker != null && !ticker.isBlank();
        if (hasTicker) {
            sql.append("AND UPPER(s.ticker) = UPPER(?) ");
        }

        sql.append("GROUP BY o.stock_id, s.ticker, o.expiration_date ")
            .append("ORDER BY s.ticker, o.expiration_date");

        if (hasTicker) {
            return jdbc.query(sql.toString(), (rs, rowNum) -> {
                OptionContract contract = new OptionContract();
                contract.setId(rs.getLong("id"));
                contract.setStockId(rs.getLong("stock_id"));
                contract.setTicker(rs.getString("ticker"));
                contract.setExpirationDate(rs.getDate("expiration_date").toLocalDate());
                return contract;
            }, ticker);
        }

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            OptionContract contract = new OptionContract();
            contract.setId(rs.getLong("id"));
            contract.setStockId(rs.getLong("stock_id"));
            contract.setTicker(rs.getString("ticker"));
            contract.setExpirationDate(rs.getDate("expiration_date").toLocalDate());
            return contract;
        });
    }

    public List<OptionContract> getOptionContractStrikeForStockAndExpiration(Long stockId, LocalDate expirationDate) {
        if (stockId == null || expirationDate == null) {
            return List.of();
        }

        String sql = "SELECT MIN(o.id) AS id, o.stock_id, s.ticker, o.expiration_date, o.strike_price "
            + "FROM option_contract o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "WHERE o.stock_id = ? "
            + "AND o.expiration_date = ? "
            + "AND o.strike_price IS NOT NULL "
            + "GROUP BY o.stock_id, s.ticker, o.expiration_date, o.strike_price "
            + "ORDER BY o.strike_price";

        return jdbc.query(sql, (rs, rowNum) -> {
            OptionContract contract = new OptionContract();
            contract.setId(rs.getLong("id"));
            contract.setStockId(rs.getLong("stock_id"));
            contract.setTicker(rs.getString("ticker"));
            Date expiration = rs.getDate("expiration_date");
            contract.setExpirationDate(expiration == null ? null : expiration.toLocalDate());
            contract.setStrikePrice(rs.getBigDecimal("strike_price"));
            return contract;
        }, stockId, expirationDate);
    }

    public List<OptionContract> getOptionContractsForStockExpirationStrike(Long stockId,
                                                                           LocalDate expirationDate,
                                                                           BigDecimal strikePrice) {
        if (stockId == null || expirationDate == null || strikePrice == null) {
            return List.of();
        }

        String sql = "SELECT o.id, o.stock_id, s.ticker, o.contract_ticker, o.contract_type, o.expiration_date, o.strike_price "
            + "FROM option_contract o "
            + "JOIN stock s ON s.id = o.stock_id "
            + "WHERE o.stock_id = ? "
            + "AND o.expiration_date = ? "
            + "AND o.strike_price = ? "
            + "ORDER BY o.contract_type, o.contract_ticker";

        return jdbc.query(sql, (rs, rowNum) -> {
            OptionContract contract = new OptionContract();
            contract.setId(rs.getLong("id"));
            contract.setStockId(rs.getLong("stock_id"));
            contract.setTicker(rs.getString("ticker"));
            contract.setContractTicker(rs.getString("contract_ticker"));
            contract.setContractType(rs.getString("contract_type"));
            Date expiration = rs.getDate("expiration_date");
            contract.setExpirationDate(expiration == null ? null : expiration.toLocalDate());
            contract.setStrikePrice(rs.getBigDecimal("strike_price"));
            return contract;
        }, stockId, expirationDate, strikePrice);
    }

    public List<OptionContract> getOptionContractsForSelection(Long stockId,
                                                               LocalDate expirationDate,
                                                               BigDecimal strikePrice,
                                                               String[] contractTypes,
                                                               String[] sources) {
        if (stockId == null || expirationDate == null) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder()
            .append("SELECT o.id, o.stock_id, s.ticker, o.contract_ticker, o.contract_type, o.source, o.status, ")
            .append("o.exercise_style, o.expiration_date, o.strike_price, o.shares_per_contract, o.updated_at ")
            .append("FROM option_contract o ")
            .append("JOIN stock s ON s.id = o.stock_id ")
            .append("WHERE o.stock_id = ? ")
            .append("AND o.expiration_date = ? ");

        List<Object> params = new java.util.ArrayList<>();
        params.add(stockId);
        params.add(expirationDate);

        if (strikePrice != null) {
            sql.append("AND o.strike_price = ? ");
            params.add(strikePrice);
        }

        if (contractTypes != null && contractTypes.length > 0) {
            String placeholders = String.join(",", java.util.Collections.nCopies(contractTypes.length, "UPPER(?)"));
            sql.append("AND UPPER(o.contract_type) IN (").append(placeholders).append(") ");
            for (String contractType : contractTypes) {
                params.add(contractType);
            }
        }

        if (sources != null && sources.length > 0) {
            String placeholders = String.join(",", java.util.Collections.nCopies(sources.length, "UPPER(?)"));
            sql.append("AND UPPER(o.source) IN (").append(placeholders).append(") ");
            for (String source : sources) {
                params.add(source);
            }
        }

        sql.append("ORDER BY o.strike_price, o.contract_type, o.contract_ticker");

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            OptionContract contract = new OptionContract();
            contract.setId(rs.getLong("id"));
            contract.setStockId(rs.getLong("stock_id"));
            contract.setTicker(rs.getString("ticker"));
            contract.setContractTicker(rs.getString("contract_ticker"));
            contract.setContractType(rs.getString("contract_type"));
            contract.setSource(rs.getString("source"));
            contract.setStatus(rs.getString("status"));
            contract.setExerciseStyle(rs.getString("exercise_style"));
            Date expiration = rs.getDate("expiration_date");
            contract.setExpirationDate(expiration == null ? null : expiration.toLocalDate());
            contract.setStrikePrice(rs.getBigDecimal("strike_price"));
            int shares = rs.getInt("shares_per_contract");
            contract.setSharesPerContract(rs.wasNull() ? null : shares);
            contract.setUpdatedAt(rs.getTimestamp("updated_at"));
            return contract;
        }, params.toArray());
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

    public List<Map<String, Object>> findContractTickersByIntervalAnalyseId(Long optionsIntervalAnalyseId) {
        if (optionsIntervalAnalyseId == null) {
            return List.of();
        }

        String sql = "SELECT id, contract_ticker "
            + "FROM option_contract "
            + "WHERE options_interval_analyse_id = ? "
            + "AND contract_ticker IS NOT NULL AND contract_ticker <> '' "
            + "ORDER BY contract_ticker";
        return jdbc.queryForList(sql, optionsIntervalAnalyseId);
    }
}