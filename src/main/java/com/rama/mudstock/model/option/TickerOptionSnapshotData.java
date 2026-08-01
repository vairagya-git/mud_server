package com.rama.mudstock.model.option;

import java.math.BigDecimal;

public record TickerOptionSnapshotData(
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
