package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.rama.mudstock.model.option.OptionSnapshot;

@Repository
public class OptionSnapshotRepository {

    private final JdbcTemplate jdbc;

    public OptionSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long optionContractId,
                      Long stockId,
                      Long snapshotVersion,
                      Timestamp snapshotTime,
                      Timestamp optionQuoteTime,
                      Timestamp optionTradeTime,
                      Timestamp underlyingTime,
                      Long unixTime,
                      BigDecimal underlyingPrice,
                      BigDecimal breakEvenPrice,
                      BigDecimal changeToBreakEven,
                      BigDecimal bid,
                      BigDecimal ask,
                      BigDecimal midpoint,
                      BigDecimal lastTradePrice,
                      Integer bidSize,
                      Integer askSize,
                      Integer lastTradeSize,
                      BigDecimal impliedVolatility,
                      BigDecimal delta,
                      BigDecimal gamma,
                      BigDecimal theta,
                      BigDecimal vega,
                      Integer openInterest,
                      Integer dayVolume,
                      String quoteTimeframe,
                      String underlyingTimeframe) {
        String sql = "INSERT INTO option_snapshot "
            + "(option_contract_id, stock_id, snapshot_version, snapshot_time, option_quote_time, option_trade_time, underlying_time, unix_time, "
            + "underlying_price, break_even_price, change_to_break_even, bid, ask, midpoint, last_trade_price, "
            + "bid_size, ask_size, last_trade_size, implied_volatility, delta, gamma, theta, vega, "
            + "open_interest, day_volume, quote_timeframe, underlying_timeframe) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        return jdbc.update(sql,
            optionContractId,
            stockId,
            snapshotVersion,
            snapshotTime,
            optionQuoteTime,
            optionTradeTime,
            underlyingTime,
            unixTime,
            underlyingPrice,
            breakEvenPrice,
            changeToBreakEven,
            bid,
            ask,
            midpoint,
            lastTradePrice,
            bidSize,
            askSize,
            lastTradeSize,
            impliedVolatility,
            delta,
            gamma,
            theta,
            vega,
            openInterest,
            dayVolume,
            quoteTimeframe,
            underlyingTimeframe);
    }

    public List<Map<String, Object>> listAllWithContractAndTicker() {
        String sql = "SELECT os.id, os.stock_id, s.ticker, oc.contract_ticker, os.option_contract_id, os.snapshot_time, "
            + "oc.contract_type, oc.expiration_date, oc.strike_price, "
            + "os.option_quote_time, os.option_trade_time, os.underlying_time, os.underlying_price, os.break_even_price, "
            + "os.change_to_break_even, os.bid, os.ask, os.midpoint, os.last_trade_price, os.bid_size, os.ask_size, "
            + "os.last_trade_size, os.implied_volatility, os.delta, os.gamma, os.theta, os.vega, os.open_interest, "
            + "os.day_volume, os.quote_timeframe, os.underlying_timeframe "
            + "FROM option_snapshot os "
            + "JOIN option_contract oc ON oc.id = os.option_contract_id "
            + "JOIN stock s ON s.id = os.stock_id "
            + "ORDER BY os.option_quote_time DESC, s.ticker, oc.contract_ticker";
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> listByContractId(Long optionContractId) {
        String sql = "SELECT os.option_quote_time AS option_quote_time, "
            + "os.underlying_price, os.bid, os.ask, os.midpoint, os.implied_volatility, "
            + "os.delta, os.gamma, os.theta, os.vega, os.open_interest, os.day_volume "
            + "FROM option_snapshot os "
            + "WHERE os.option_contract_id = ? "
            + "ORDER BY os.option_quote_time DESC";
        return jdbc.queryForList(sql, optionContractId);
    }

    public List<OptionSnapshot> listOptionSnapshotsByContractId(Long optionContractId) {
        String sql = "SELECT os.snapshot_time, os.option_quote_time, os.underlying_price, os.bid, os.ask, os.midpoint, os.implied_volatility, "
            + "os.delta, os.gamma, os.theta, os.vega, os.open_interest, os.day_volume "
            + "FROM option_snapshot os "
            + "WHERE os.option_contract_id = ? "
            + "ORDER BY COALESCE(os.option_quote_time, os.snapshot_time) DESC";

        return jdbc.query(sql, (rs, rowNum) -> {
            OptionSnapshot row = new OptionSnapshot();
            row.setSnapshotTime(rs.getTimestamp("snapshot_time"));
            row.setOptionQuoteTime(rs.getTimestamp("option_quote_time"));
            row.setUnderlyingPrice(rs.getBigDecimal("underlying_price"));
            row.setBid(rs.getBigDecimal("bid"));
            row.setAsk(rs.getBigDecimal("ask"));
            row.setMidpoint(rs.getBigDecimal("midpoint"));
            row.setImpliedVolatility(rs.getBigDecimal("implied_volatility"));
            row.setDelta(rs.getBigDecimal("delta"));
            row.setGamma(rs.getBigDecimal("gamma"));
            row.setTheta(rs.getBigDecimal("theta"));
            row.setVega(rs.getBigDecimal("vega"));
            int openInterest = rs.getInt("open_interest");
            row.setOpenInterest(rs.wasNull() ? null : openInterest);
            int dayVolume = rs.getInt("day_volume");
            row.setDayVolume(rs.wasNull() ? null : dayVolume);
            return row;
        }, optionContractId);
    }

    public Long findIdByContractAndUnixTime(Long optionContractId, Long unixTime) {
        if (optionContractId == null || unixTime == null) {
            return null;
        }

        String sql = "SELECT id FROM option_snapshot "
            + "WHERE option_contract_id = ? "
            + "AND unix_time = ? "
            + "LIMIT 1";
        List<Long> rows = jdbc.queryForList(sql, Long.class, optionContractId, unixTime);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public OptionSnapshot findNearestSnapshotByContractAndOptionTime(Long optionContractId, Timestamp targetTime) {
        if (optionContractId == null || targetTime == null) {
            return null;
        }

        String sql = "SELECT id, option_contract_id, stock_id, unix_time, snapshot_time, option_quote_time, "
            + "underlying_price, bid, ask, midpoint, last_trade_price, implied_volatility, "
            + "delta, gamma, theta, vega, open_interest, day_volume "
            + "FROM option_snapshot "
            + "WHERE option_contract_id = ? "
            + "ORDER BY ABS(TIMESTAMPDIFF(SECOND, COALESCE(option_quote_time, snapshot_time), ?)), "
            + "COALESCE(option_quote_time, snapshot_time) DESC "
            + "LIMIT 1";

        List<OptionSnapshot> rows = jdbc.query(sql, (rs, rowNum) -> {
            OptionSnapshot snapshot = new OptionSnapshot();
            snapshot.setId(rs.getLong("id"));
            snapshot.setOptionContractId(rs.getLong("option_contract_id"));
            snapshot.setStockId(rs.getLong("stock_id"));
            snapshot.setUnixTime(getNullableLong(rs, "unix_time"));
            snapshot.setSnapshotTime(rs.getTimestamp("snapshot_time"));
            snapshot.setOptionQuoteTime(rs.getTimestamp("option_quote_time"));
            snapshot.setUnderlyingPrice(rs.getBigDecimal("underlying_price"));
            snapshot.setBid(rs.getBigDecimal("bid"));
            snapshot.setAsk(rs.getBigDecimal("ask"));
            snapshot.setMidpoint(rs.getBigDecimal("midpoint"));
            snapshot.setLastTradePrice(rs.getBigDecimal("last_trade_price"));
            snapshot.setImpliedVolatility(rs.getBigDecimal("implied_volatility"));
            snapshot.setDelta(rs.getBigDecimal("delta"));
            snapshot.setGamma(rs.getBigDecimal("gamma"));
            snapshot.setTheta(rs.getBigDecimal("theta"));
            snapshot.setVega(rs.getBigDecimal("vega"));
            int openInterest = rs.getInt("open_interest");
            snapshot.setOpenInterest(rs.wasNull() ? null : openInterest);
            int dayVolume = rs.getInt("day_volume");
            snapshot.setDayVolume(rs.wasNull() ? null : dayVolume);
            return snapshot;
        }, optionContractId, targetTime);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public OptionSnapshot findNearestSnapshotByContractAndUnixTime(Long optionContractId,
                                                                   Long targetUnixTime,
                                                                   Long minimumExclusiveUnixTime) {
        if (optionContractId == null || targetUnixTime == null) {
            return null;
        }

        String sql = "SELECT id, option_contract_id, stock_id, unix_time, snapshot_time, option_quote_time, "
            + "underlying_price, bid, ask, midpoint, last_trade_price, implied_volatility, "
            + "delta, gamma, theta, vega, open_interest, day_volume "
            + "FROM option_snapshot "
            + "WHERE option_contract_id = ? "
            + "AND unix_time IS NOT NULL "
            + "AND unix_time > ? "
            + "ORDER BY ABS(unix_time - ?), unix_time ASC "
            + "LIMIT 1";

        long minimumUnix = minimumExclusiveUnixTime == null ? Long.MIN_VALUE : minimumExclusiveUnixTime;
        List<OptionSnapshot> rows = jdbc.query(sql, (rs, rowNum) -> {
            OptionSnapshot snapshot = new OptionSnapshot();
            snapshot.setId(rs.getLong("id"));
            snapshot.setOptionContractId(rs.getLong("option_contract_id"));
            snapshot.setStockId(rs.getLong("stock_id"));
            snapshot.setUnixTime(getNullableLong(rs, "unix_time"));
            snapshot.setSnapshotTime(rs.getTimestamp("snapshot_time"));
            snapshot.setOptionQuoteTime(rs.getTimestamp("option_quote_time"));
            snapshot.setUnderlyingPrice(rs.getBigDecimal("underlying_price"));
            snapshot.setBid(rs.getBigDecimal("bid"));
            snapshot.setAsk(rs.getBigDecimal("ask"));
            snapshot.setMidpoint(rs.getBigDecimal("midpoint"));
            snapshot.setLastTradePrice(rs.getBigDecimal("last_trade_price"));
            snapshot.setImpliedVolatility(rs.getBigDecimal("implied_volatility"));
            snapshot.setDelta(rs.getBigDecimal("delta"));
            snapshot.setGamma(rs.getBigDecimal("gamma"));
            snapshot.setTheta(rs.getBigDecimal("theta"));
            snapshot.setVega(rs.getBigDecimal("vega"));
            int openInterest = rs.getInt("open_interest");
            snapshot.setOpenInterest(rs.wasNull() ? null : openInterest);
            int dayVolume = rs.getInt("day_volume");
            snapshot.setDayVolume(rs.wasNull() ? null : dayVolume);
            return snapshot;
        }, optionContractId, minimumUnix, targetUnixTime);

        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long getNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}

//Changed For Git