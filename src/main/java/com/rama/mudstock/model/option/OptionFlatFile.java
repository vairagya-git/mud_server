package com.rama.mudstock.model.option;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class OptionFlatFile {

    private Timestamp localTime;
    private Integer optVolume;
    private BigDecimal optOpen;
    private BigDecimal optClose;
    private BigDecimal optHigh;
    private BigDecimal optLow;
    private Integer stockVolume;
    private BigDecimal stockOpen;
    private BigDecimal stockClose;
    private BigDecimal stockHigh;
    private BigDecimal stockLow;

    public Timestamp getLocalTime() {
        return localTime;
    }

    public void setLocalTime(Timestamp localTime) {
        this.localTime = localTime;
    }

    public Integer getOptVolume() {
        return optVolume;
    }

    public void setOptVolume(Integer optVolume) {
        this.optVolume = optVolume;
    }

    public BigDecimal getOptOpen() {
        return optOpen;
    }

    public void setOptOpen(BigDecimal optOpen) {
        this.optOpen = optOpen;
    }

    public BigDecimal getOptClose() {
        return optClose;
    }

    public void setOptClose(BigDecimal optClose) {
        this.optClose = optClose;
    }

    public BigDecimal getOptHigh() {
        return optHigh;
    }

    public void setOptHigh(BigDecimal optHigh) {
        this.optHigh = optHigh;
    }

    public BigDecimal getOptLow() {
        return optLow;
    }

    public void setOptLow(BigDecimal optLow) {
        this.optLow = optLow;
    }

    public Integer getStockVolume() {
        return stockVolume;
    }

    public void setStockVolume(Integer stockVolume) {
        this.stockVolume = stockVolume;
    }

    public BigDecimal getStockOpen() {
        return stockOpen;
    }

    public void setStockOpen(BigDecimal stockOpen) {
        this.stockOpen = stockOpen;
    }

    public BigDecimal getStockClose() {
        return stockClose;
    }

    public void setStockClose(BigDecimal stockClose) {
        this.stockClose = stockClose;
    }

    public BigDecimal getStockHigh() {
        return stockHigh;
    }

    public void setStockHigh(BigDecimal stockHigh) {
        this.stockHigh = stockHigh;
    }

    public BigDecimal getStockLow() {
        return stockLow;
    }

    public void setStockLow(BigDecimal stockLow) {
        this.stockLow = stockLow;
    }
}
