package com.rama.mudstock.repository.option;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OptionSnapshotRepository {

    private final JdbcTemplate jdbc;

    public OptionSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long optionContractId,
                      Long stockId,
                      Timestamp snapshotTime,
                      Timestamp optionQuoteTime,
                      Timestamp optionTradeTime,
                      Timestamp underlyingTime,
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
            + "(option_contract_id, stock_id, snapshot_time, option_quote_time, option_trade_time, underlying_time, "
            + "underlying_price, break_even_price, change_to_break_even, bid, ask, midpoint, last_trade_price, "
            + "bid_size, ask_size, last_trade_size, implied_volatility, delta, gamma, theta, vega, "
            + "open_interest, day_volume, quote_timeframe, underlying_timeframe) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        return jdbc.update(sql,
            optionContractId,
            stockId,
            snapshotTime,
            optionQuoteTime,
            optionTradeTime,
            underlyingTime,
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
}