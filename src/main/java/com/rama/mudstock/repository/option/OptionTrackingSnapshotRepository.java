package com.rama.mudstock.repository.option;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.option.OptionTrackingFlatfileRow;

@Repository
public class OptionTrackingSnapshotRepository {

    private final JdbcTemplate jdbc;

    public OptionTrackingSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OptionTrackingFlatfileRow> findFlatfileRowsForContracts(List<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", Collections.nCopies(contractIds.size(), "?"));
        String sql = "SELECT f.unix_time, f.local_time, f.stock_open, f.stock_close, "
            + "f.option_contract_id, f.contract_ticker, f.opt_volume, f.opt_open, f.opt_close, f.opt_high, f.opt_low, "
            + "f.near_option_snapshot_id, "
            + "s.bid, s.ask, s.midpoint, s.delta, s.gamma, s.theta, s.vega "
            + "FROM option_snapshot_flatfile f "
            + "LEFT JOIN option_snapshot s ON s.id = f.near_option_snapshot_id "
            + "WHERE f.option_contract_id IN (" + placeholders + ") "
            + "ORDER BY f.unix_time";

        List<Object> params = new ArrayList<>(contractIds);
        return jdbc.query(sql, this::mapRow, params.toArray());
    }

    private OptionTrackingFlatfileRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OptionTrackingFlatfileRow(
            rs.getObject("unix_time", Long.class),
            rs.getTimestamp("local_time"),
            rs.getBigDecimal("stock_open"),
            rs.getBigDecimal("stock_close"),
            rs.getObject("option_contract_id", Long.class),
            rs.getString("contract_ticker"),
            rs.getObject("opt_volume", Integer.class),
            rs.getBigDecimal("opt_open"),
            rs.getBigDecimal("opt_close"),
            rs.getBigDecimal("opt_high"),
            rs.getBigDecimal("opt_low"),
            rs.getObject("near_option_snapshot_id", Long.class),
            rs.getBigDecimal("bid"),
            rs.getBigDecimal("ask"),
            rs.getBigDecimal("midpoint"),
            rs.getBigDecimal("delta"),
            rs.getBigDecimal("gamma"),
            rs.getBigDecimal("theta"),
            rs.getBigDecimal("vega"));
    }
}
