package com.rama.mudstock.repository.option;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
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
}