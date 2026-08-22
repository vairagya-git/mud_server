package com.rama.mudstock.repository.option;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OptionTradeRepository {

    private final JdbcTemplate jdbc;

    public OptionTradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OpenTradeRow> listOpenTradesByStockId(Long stockId) {
        if (stockId == null) {
            return List.of();
        }

        String sql = "SELECT id, trade_name, trade_mode "
            + "FROM option_trade "
            + "WHERE stock_id = ? AND status = 'OPEN' "
            + "ORDER BY trade_name, id";
        return jdbc.query(sql, (rs, rowNum) -> new OpenTradeRow(
            rs.getLong("id"),
            rs.getString("trade_name"),
            rs.getString("trade_mode")),
            stockId);
    }

    public List<AliveTradeRow> listAliveTradesWithTicker() {
        String sql = "SELECT t.id, t.stock_id, s.ticker, t.trade_name, t.trade_mode, t.status, t.with_historic_data, t.opened_at, t.created_at "
            + "FROM option_trade t "
            + "JOIN stock s ON s.id = t.stock_id "
            + "WHERE t.status = 'OPEN' "
            + "ORDER BY t.opened_at DESC, t.id DESC";
        return jdbc.query(sql, (rs, rowNum) -> new AliveTradeRow(
            rs.getLong("id"),
            rs.getLong("stock_id"),
            rs.getString("ticker"),
            rs.getString("trade_name"),
            rs.getString("trade_mode"),
            rs.getString("status"),
                rs.getBoolean("with_historic_data"),
            rs.getTimestamp("opened_at"),
            rs.getTimestamp("created_at")));
    }

    public List<HistoricLiveTradeRow> listOpenHistoricLiveTrades() {
        String sql = "SELECT t.id, t.stock_id, s.ticker, t.trade_name, t.trade_mode, t.status, t.with_historic_data "
            + "FROM option_trade t "
            + "JOIN stock s ON s.id = t.stock_id "
            + "WHERE t.status = 'OPEN' "
            + "AND UPPER(t.trade_mode) = 'LIVE' "
            + "AND t.with_historic_data = 1 "
            + "ORDER BY t.opened_at DESC, t.id DESC";
        return jdbc.query(sql, (rs, rowNum) -> new HistoricLiveTradeRow(
            rs.getLong("id"),
            rs.getLong("stock_id"),
            rs.getString("ticker"),
            rs.getString("trade_name"),
            rs.getString("trade_mode"),
            rs.getString("status"),
            rs.getBoolean("with_historic_data")));
    }

    public String findOpenTradeModeByIdAndStockId(Long tradeId, Long stockId) {
        if (tradeId == null || stockId == null) {
            return null;
        }

        String sql = "SELECT trade_mode "
            + "FROM option_trade "
            + "WHERE id = ? AND stock_id = ? AND status = 'OPEN' "
            + "LIMIT 1";
        List<String> rows = jdbc.queryForList(sql, String.class, tradeId, stockId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Long insertOpenTrade(Long stockId,
                                String tradeName,
                                String tradeMode,
                                boolean withHistoricData,
                                LocalDateTime openedAt) {
        String sql = "INSERT INTO option_trade (stock_id, trade_name, trade_mode, status, with_historic_data, opened_at) "
            + "VALUES (?, ?, ?, 'OPEN', ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, stockId);
            ps.setString(2, tradeName);
            ps.setString(3, tradeMode);
            ps.setBoolean(4, withHistoricData);
            ps.setTimestamp(5, Timestamp.valueOf(openedAt));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public record OpenTradeRow(Long id,
                               String tradeName,
                               String tradeMode) {
    }

    public record AliveTradeRow(Long id,
                                Long stockId,
                                String ticker,
                                String tradeName,
                                String tradeMode,
                                String status,
                                boolean withHistoricData,
                                Timestamp openedAt,
                                Timestamp createdAt) {
    }

    public record HistoricLiveTradeRow(Long id,
                                       Long stockId,
                                       String ticker,
                                       String tradeName,
                                       String tradeMode,
                                       String status,
                                       boolean withHistoricData) {
    }
}
