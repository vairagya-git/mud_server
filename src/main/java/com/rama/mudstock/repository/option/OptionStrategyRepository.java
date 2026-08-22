package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OptionStrategyRepository {

    private final JdbcTemplate jdbc;

    public OptionStrategyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAllWithTicker() {
        String sql = "SELECT s.id, s.stock_id, st.ticker, s.previous_strategy_id, ps.strategy_name AS previous_strategy_name, "
            + "s.strategy_name, s.strategy_type, s.strategy_mode, s.strategy_action, s.status, s.created_at, s.updated_at "
            + "FROM option_strategy s "
            + "JOIN stock st ON st.id = s.stock_id "
            + "LEFT JOIN option_strategy ps ON ps.id = s.previous_strategy_id "
            + "ORDER BY s.updated_at DESC, s.strategy_name";
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listStrategyNameOptions() {
        String sql = "SELECT id, strategy_name FROM option_strategy ORDER BY strategy_name";
        return jdbc.queryForList(sql);
    }

    public List<StrategySummaryRow> listStrategiesByTradeIds(List<Long> tradeIds) {
        if (tradeIds == null || tradeIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(tradeIds.size(), "?"));
        String sql = "SELECT s.id, s.option_trade_id, s.strategy_definition_id, d.strategy_code, d.display_name, "
            + "s.trade_mode, s.strategy_action, s.status, s.entry_time, s.created_at "
            + "FROM option_strategy s "
            + "LEFT JOIN option_strategy_definition d ON d.id = s.strategy_definition_id "
            + "WHERE s.option_trade_id IN (" + placeholders + ") "
            + "ORDER BY s.entry_time DESC, s.id DESC";
        return jdbc.query(sql, (rs, rowNum) -> new StrategySummaryRow(
            rs.getLong("id"),
            rs.getLong("option_trade_id"),
            rs.getLong("strategy_definition_id"),
            rs.getString("strategy_code"),
            rs.getString("display_name"),
            rs.getString("trade_mode"),
            rs.getString("strategy_action"),
            rs.getString("status"),
            rs.getTimestamp("entry_time"),
            rs.getTimestamp("created_at")),
            tradeIds.toArray());
    }

    public List<StrategyLegSummaryRow> listStrategyLegsByStrategyIds(List<Long> strategyIds) {
        if (strategyIds == null || strategyIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(strategyIds.size(), "?"));
        String sql = "SELECT l.id, l.option_strategy_id, l.option_contract_id, l.leg_number, l.position_side, l.quantity, "
            + "l.entry_price, l.entry_snapshot_id, l.exit_price, l.exit_snapshot_id, oc.contract_ticker, oc.contract_type "
            + "FROM option_strategy_leg l "
            + "LEFT JOIN option_contract oc ON oc.id = l.option_contract_id "
            + "WHERE l.option_strategy_id IN (" + placeholders + ") "
            + "ORDER BY l.option_strategy_id, l.leg_number, l.id";
        return jdbc.query(sql, (rs, rowNum) -> new StrategyLegSummaryRow(
            rs.getLong("id"),
            rs.getLong("option_strategy_id"),
            rs.getLong("option_contract_id"),
            rs.getInt("leg_number"),
            rs.getString("position_side"),
            rs.getInt("quantity"),
            rs.getBigDecimal("entry_price"),
            getNullableLong(rs, "entry_snapshot_id"),
            rs.getBigDecimal("exit_price"),
            getNullableLong(rs, "exit_snapshot_id"),
            rs.getString("contract_ticker"),
            rs.getString("contract_type")),
            strategyIds.toArray());
    }

    public List<StrategySnapshotSummaryRow> listStrategySnapshotsByStrategyIds(List<Long> strategyIds) {
        if (strategyIds == null || strategyIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(strategyIds.size(), "?"));
        String sql = "SELECT ss.id, ss.option_strategy_id, ss.snapshot_time, ss.underlying_price, ss.entry_cost, "
            + "ss.current_market_value, ss.unrealized_pnl, ss.unrealized_pnl_pct, ss.total_pnl, ss.net_delta, "
            + "ss.net_gamma, ss.net_theta, ss.net_vega, ss.average_iv, ss.total_open_interest, ss.total_day_volume "
            + "FROM option_strategy_snapshot ss "
            + "WHERE ss.option_strategy_id IN (" + placeholders + ") "
            + "ORDER BY ss.option_strategy_id, ss.snapshot_time DESC, ss.id DESC";
        return jdbc.query(sql, (rs, rowNum) -> new StrategySnapshotSummaryRow(
            rs.getLong("id"),
            rs.getLong("option_strategy_id"),
            rs.getTimestamp("snapshot_time"),
            rs.getBigDecimal("underlying_price"),
            rs.getBigDecimal("entry_cost"),
            rs.getBigDecimal("current_market_value"),
            rs.getBigDecimal("unrealized_pnl"),
            rs.getBigDecimal("unrealized_pnl_pct"),
            rs.getBigDecimal("total_pnl"),
            rs.getBigDecimal("net_delta"),
            rs.getBigDecimal("net_gamma"),
            rs.getBigDecimal("net_theta"),
            rs.getBigDecimal("net_vega"),
            rs.getBigDecimal("average_iv"),
            getNullableLong(rs, "total_open_interest"),
            getNullableLong(rs, "total_day_volume")),
            strategyIds.toArray());
    }

    public List<StrategyLegSnapshotSummaryRow> listStrategyLegSnapshotsBySnapshotIds(List<Long> snapshotIds) {
        if (snapshotIds == null || snapshotIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(snapshotIds.size(), "?"));
        String sql = "SELECT sls.id, sls.option_strategy_snapshot_id, sls.option_strategy_leg_id, sls.option_snapshot_id, "
            + "sls.current_market_value, sls.unrealized_pnl, sls.unrealized_pnl_pct, l.leg_number, oc.contract_ticker "
            + "FROM option_strategy_leg_snapshot sls "
            + "JOIN option_strategy_leg l ON l.id = sls.option_strategy_leg_id "
            + "LEFT JOIN option_contract oc ON oc.id = l.option_contract_id "
            + "WHERE sls.option_strategy_snapshot_id IN (" + placeholders + ") "
            + "ORDER BY sls.option_strategy_snapshot_id, l.leg_number, sls.id";
        return jdbc.query(sql, (rs, rowNum) -> new StrategyLegSnapshotSummaryRow(
            rs.getLong("id"),
            rs.getLong("option_strategy_snapshot_id"),
            rs.getLong("option_strategy_leg_id"),
            rs.getLong("option_snapshot_id"),
            rs.getBigDecimal("current_market_value"),
            rs.getBigDecimal("unrealized_pnl"),
            rs.getBigDecimal("unrealized_pnl_pct"),
            rs.getInt("leg_number"),
            rs.getString("contract_ticker")),
            snapshotIds.toArray());
    }

    private Long getNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public List<Map<String, Object>> listActiveStrategyDefinitions() {
        String sql = "SELECT id, strategy_code, display_name, description, minimum_legs, maximum_legs, "
            + "allow_roll, allow_partial_close, allow_rebalance, display_order "
            + "FROM option_strategy_definition "
            + "WHERE active = TRUE "
            + "ORDER BY display_order, display_name";
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listActiveStrategyDefinitionLegs() {
        String sql = "SELECT l.id, l.strategy_definition_id, l.leg_code, l.display_name, l.contract_type, "
            + "l.position_side, l.quantity, l.leg_order, l.expiration_group, l.required "
            + "FROM option_strategy_definition_leg l "
            + "JOIN option_strategy_definition d ON d.id = l.strategy_definition_id "
            + "WHERE d.active = TRUE "
            + "ORDER BY l.strategy_definition_id, l.leg_order";
        return jdbc.queryForList(sql);
    }

    public int insert(Long stockId,
                      Long previousStrategyId,
                      String strategyName,
                      String strategyType,
                      String strategyMode,
                      String strategyAction,
                      String status) {
        String sql = "INSERT INTO option_strategy "
            + "(stock_id, previous_strategy_id, strategy_name, strategy_type, strategy_mode, strategy_action, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        return jdbc.update(sql,
            stockId,
            previousStrategyId,
            strategyName,
            strategyType,
            strategyMode,
            strategyAction,
            status);
    }

    public List<StrategyDefinitionLegRow> listActiveStrategyDefinitionLegsByDefinitionId(Long strategyDefinitionId) {
        if (strategyDefinitionId == null) {
            return List.of();
        }

        String sql = "SELECT l.id, l.strategy_definition_id, l.leg_code, l.display_name, l.contract_type, "
            + "l.position_side, l.quantity, l.leg_order, l.expiration_group, l.required "
            + "FROM option_strategy_definition_leg l "
            + "JOIN option_strategy_definition d ON d.id = l.strategy_definition_id "
            + "WHERE d.active = TRUE AND l.strategy_definition_id = ? "
            + "ORDER BY l.leg_order";
        return jdbc.query(sql, (rs, rowNum) -> new StrategyDefinitionLegRow(
            rs.getLong("id"),
            rs.getLong("strategy_definition_id"),
            rs.getString("leg_code"),
            rs.getString("display_name"),
            rs.getString("contract_type"),
            rs.getString("position_side"),
            rs.getInt("quantity"),
            rs.getInt("leg_order"),
            rs.getInt("expiration_group"),
            rs.getBoolean("required")),
            strategyDefinitionId);
    }

    public Long insertStrategyAndReturnId(Long strategyDefinitionId,
                                          Long stockId,
                                          Long optionTradeId,
                                          String tradeMode,
                                          String strategyAction,
                                          String status,
                                          Timestamp entryTime,
                                          BigDecimal entryUnderlyingPrice) {
        String sql = "INSERT INTO option_strategy "
            + "(strategy_definition_id, stock_id, option_trade_id, trade_mode, strategy_action, status, entry_time, entry_underlying_price) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, strategyDefinitionId);
            ps.setLong(2, stockId);
            ps.setLong(3, optionTradeId);
            ps.setString(4, tradeMode);
            ps.setString(5, strategyAction);
            ps.setString(6, status);
            ps.setTimestamp(7, entryTime);
            ps.setBigDecimal(8, entryUnderlyingPrice);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public Long insertStrategyLegAndReturnId(Long optionStrategyId,
                                             Long optionContractId,
                                             Integer legNumber,
                                             String positionSide,
                                             Integer quantity,
                                             Long entrySnapshotId,
                                             BigDecimal entryPrice) {
        String sql = "INSERT INTO option_strategy_leg "
            + "(option_strategy_id, option_contract_id, leg_number, position_side, quantity, entry_snapshot_id, entry_price) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, optionStrategyId);
            ps.setLong(2, optionContractId);
            ps.setInt(3, legNumber);
            ps.setString(4, positionSide);
            ps.setInt(5, quantity);
            if (entrySnapshotId == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, entrySnapshotId);
            }
            ps.setBigDecimal(7, entryPrice);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public Long insertStrategySnapshotAndReturnId(Long optionStrategyId,
                                                  Timestamp snapshotTime,
                                                  BigDecimal underlyingPrice,
                                                  BigDecimal entryCost,
                                                  BigDecimal currentMarketValue,
                                                  BigDecimal unrealizedPnl,
                                                  BigDecimal unrealizedPnlPct,
                                                  BigDecimal realizedPnl,
                                                  BigDecimal totalPnl,
                                                  BigDecimal netDelta,
                                                  BigDecimal netGamma,
                                                  BigDecimal netTheta,
                                                  BigDecimal netVega,
                                                  BigDecimal averageIv,
                                                  Long totalOpenInterest,
                                                  Long totalDayVolume) {
        String sql = "INSERT INTO option_strategy_snapshot "
            + "(option_strategy_id, snapshot_time, underlying_price, entry_cost, current_market_value, "
            + "unrealized_pnl, unrealized_pnl_pct, realized_pnl, total_pnl, net_delta, net_gamma, net_theta, net_vega, "
            + "average_iv, total_open_interest, total_day_volume) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, optionStrategyId);
            ps.setTimestamp(2, snapshotTime);
            ps.setBigDecimal(3, underlyingPrice);
            ps.setBigDecimal(4, entryCost);
            ps.setBigDecimal(5, currentMarketValue);
            ps.setBigDecimal(6, unrealizedPnl);
            ps.setBigDecimal(7, unrealizedPnlPct);
            ps.setBigDecimal(8, realizedPnl);
            ps.setBigDecimal(9, totalPnl);
            ps.setBigDecimal(10, netDelta);
            ps.setBigDecimal(11, netGamma);
            ps.setBigDecimal(12, netTheta);
            ps.setBigDecimal(13, netVega);
            ps.setBigDecimal(14, averageIv);
            if (totalOpenInterest == null) {
                ps.setNull(15, java.sql.Types.BIGINT);
            } else {
                ps.setLong(15, totalOpenInterest);
            }
            if (totalDayVolume == null) {
                ps.setNull(16, java.sql.Types.BIGINT);
            } else {
                ps.setLong(16, totalDayVolume);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int insertStrategyLegSnapshot(Long optionStrategySnapshotId,
                                         Long optionStrategyLegId,
                                         Long optionSnapshotId,
                                         BigDecimal currentMarketValue,
                                         BigDecimal unrealizedPnl,
                                         BigDecimal unrealizedPnlPct) {
        String sql = "INSERT INTO option_strategy_leg_snapshot "
            + "(option_strategy_snapshot_id, option_strategy_leg_id, option_snapshot_id, current_market_value, unrealized_pnl, unrealized_pnl_pct) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

        return jdbc.update(sql,
            optionStrategySnapshotId,
            optionStrategyLegId,
            optionSnapshotId,
            currentMarketValue,
            unrealizedPnl,
            unrealizedPnlPct);
    }

    public record StrategyDefinitionLegRow(Long id,
                                           Long strategyDefinitionId,
                                           String legCode,
                                           String displayName,
                                           String contractType,
                                           String positionSide,
                                           Integer quantity,
                                           Integer legOrder,
                                           Integer expirationGroup,
                                           Boolean required) {
    }

    public record StrategySummaryRow(Long id,
                                     Long optionTradeId,
                                     Long strategyDefinitionId,
                                     String strategyCode,
                                     String displayName,
                                     String tradeMode,
                                     String strategyAction,
                                     String status,
                                     Timestamp entryTime,
                                     Timestamp createdAt) {
    }

    public record StrategyLegSummaryRow(Long id,
                                        Long optionStrategyId,
                                        Long optionContractId,
                                        Integer legNumber,
                                        String positionSide,
                                        Integer quantity,
                                        BigDecimal entryPrice,
                                        Long entrySnapshotId,
                                        BigDecimal exitPrice,
                                        Long exitSnapshotId,
                                        String contractTicker,
                                        String contractType) {
    }

    public record StrategySnapshotSummaryRow(Long id,
                                             Long optionStrategyId,
                                             Timestamp snapshotTime,
                                             BigDecimal underlyingPrice,
                                             BigDecimal entryCost,
                                             BigDecimal currentMarketValue,
                                             BigDecimal unrealizedPnl,
                                             BigDecimal unrealizedPnlPct,
                                             BigDecimal totalPnl,
                                             BigDecimal netDelta,
                                             BigDecimal netGamma,
                                             BigDecimal netTheta,
                                             BigDecimal netVega,
                                             BigDecimal averageIv,
                                             Long totalOpenInterest,
                                             Long totalDayVolume) {
    }

    public record StrategyLegSnapshotSummaryRow(Long id,
                                                Long optionStrategySnapshotId,
                                                Long optionStrategyLegId,
                                                Long optionSnapshotId,
                                                BigDecimal currentMarketValue,
                                                BigDecimal unrealizedPnl,
                                                BigDecimal unrealizedPnlPct,
                                                Integer legNumber,
                                                String contractTicker) {
    }
}