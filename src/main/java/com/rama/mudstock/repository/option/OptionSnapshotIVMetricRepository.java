package com.rama.mudstock.repository.option;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OptionSnapshotIVMetricRepository {

    private final JdbcTemplate jdbc;

    public OptionSnapshotIVMetricRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAllWithTickerAndContract() {
        String sql = "SELECT m.id, m.stock_id, s.ticker, m.option_contract_id, oc.contract_ticker, oc.contract_type, "
            + "oc.expiration_date, oc.strike_price, m.max_iv, m.max_iv_time, m.max_iv_stock_distance, "
            + "m.min_iv, m.min_iv_time, m.min_iv_stock_distance, m.iv_date, "
            + "m.created_at, m.updated_at "
            + "FROM option_snapshot_iv_metric m "
            + "JOIN stock s ON s.id = m.stock_id "
            + "JOIN option_contract oc ON oc.id = m.option_contract_id "
            + "ORDER BY m.iv_date DESC, s.ticker, oc.contract_ticker";
        return jdbc.queryForList(sql);
    }

    public int upsertDailyMetrics(LocalDate ivDate) {
        String sql = "INSERT INTO option_snapshot_iv_metric "
            + "(stock_id, option_contract_id, max_iv, max_iv_time, max_iv_stock_distance, min_iv, min_iv_time, min_iv_stock_distance, iv_date) "
            + "SELECT oc.stock_id, agg.option_contract_id, agg.max_iv, "
            + "       (SELECT TIME(os.option_quote_time) "
            + "        FROM option_snapshot os "
            + "        WHERE os.option_contract_id = agg.option_contract_id "
            + "          AND DATE(os.option_quote_time) = agg.iv_date "
            + "          AND os.implied_volatility = agg.max_iv "
            + "          AND os.option_quote_time IS NOT NULL "
            + "        ORDER BY os.option_quote_time ASC "
            + "        LIMIT 1) AS max_iv_time, "
            + "       (SELECT ROUND(ABS(os.underlying_price - oc.strike_price), 2) "
            + "        FROM option_snapshot os "
            + "        WHERE os.option_contract_id = agg.option_contract_id "
            + "          AND DATE(os.option_quote_time) = agg.iv_date "
            + "          AND os.implied_volatility = agg.max_iv "
            + "          AND os.option_quote_time IS NOT NULL "
            + "        ORDER BY os.option_quote_time ASC "
            + "        LIMIT 1) AS max_iv_stock_distance, "
            + "       agg.min_iv, "
            + "       (SELECT TIME(os.option_quote_time) "
            + "        FROM option_snapshot os "
            + "        WHERE os.option_contract_id = agg.option_contract_id "
            + "          AND DATE(os.option_quote_time) = agg.iv_date "
            + "          AND os.implied_volatility = agg.min_iv "
            + "          AND os.option_quote_time IS NOT NULL "
            + "        ORDER BY os.option_quote_time ASC "
            + "        LIMIT 1) AS min_iv_time, "
            + "       (SELECT ROUND(ABS(os.underlying_price - oc.strike_price), 2) "
            + "        FROM option_snapshot os "
            + "        WHERE os.option_contract_id = agg.option_contract_id "
            + "          AND DATE(os.option_quote_time) = agg.iv_date "
            + "          AND os.implied_volatility = agg.min_iv "
            + "          AND os.option_quote_time IS NOT NULL "
            + "        ORDER BY os.option_quote_time ASC "
            + "        LIMIT 1) AS min_iv_stock_distance, "
            + "       agg.iv_date "
            + "FROM option_contract oc "
            + "JOIN ( "
            + "    SELECT os.option_contract_id, DATE(os.option_quote_time) AS iv_date, "
            + "           MAX(os.implied_volatility) AS max_iv, MIN(os.implied_volatility) AS min_iv "
            + "    FROM option_snapshot os "
            + "    WHERE os.option_quote_time IS NOT NULL "
            + "      AND os.implied_volatility IS NOT NULL "
            + "      AND DATE(os.option_quote_time) = ? "
            + "    GROUP BY os.option_contract_id, DATE(os.option_quote_time) "
            + ") agg ON agg.option_contract_id = oc.id "
            + "WHERE UPPER(oc.status) = UPPER(?) "
            + "ON DUPLICATE KEY UPDATE "
            + "max_iv = VALUES(max_iv), "
            + "max_iv_time = VALUES(max_iv_time), "
            + "max_iv_stock_distance = VALUES(max_iv_stock_distance), "
            + "min_iv = VALUES(min_iv), "
            + "min_iv_time = VALUES(min_iv_time), "
            + "min_iv_stock_distance = VALUES(min_iv_stock_distance), "
            + "updated_at = CURRENT_TIMESTAMP";

        Date sqlDate = Date.valueOf(ivDate);
        return jdbc.update(sql, sqlDate, OptionContractRepository.STATUS_ACTIVE);
    }
}