package com.rama.mudstock.model.option;

import java.math.BigDecimal;

public record TickerStockSnapshotData(
    String ticker,
    Integer volume,
    BigDecimal open,
    BigDecimal close,
    BigDecimal high,
    BigDecimal low,
    Long windowStart,
    Integer transactions
) {
}
