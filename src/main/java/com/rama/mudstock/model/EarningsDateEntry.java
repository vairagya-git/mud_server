package com.rama.mudstock.model;

import java.math.BigDecimal;

public class EarningsDateEntry {
    private Long id;
    private Long stockId;
    private Long earningsDateId;
    private EarningsDateEnum datePeriod;
    private BigDecimal Open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private BigDecimal percentage;
    private BigDecimal value;

    public EarningsDateEntry() {}

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public Long getEarningsDateId() { return earningsDateId; }
    public void setEarningsDateId(Long earningsDateId) { this.earningsDateId = earningsDateId; }
    public EarningsDateEnum getDatePeriod() { return datePeriod; }
    public void setDatePeriod(EarningsDateEnum datePeriod) { this.datePeriod = datePeriod; }
    public BigDecimal getOpen() { return Open; }
    public void setOpen(BigDecimal open) { Open = open; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
