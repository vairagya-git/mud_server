package com.rama.mudstock.model;

import java.time.LocalDate;

public class EarningsDateView {
    private Long id;
    private Long stockId;
    private String stockSymbol;
    private String quarter;
    private EarningsDate.ReleaseTime releaseTime;
    private EarningsDate.State state;
    private LocalDate earningsDate;

    public EarningsDateView() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }

    public EarningsDate.ReleaseTime getReleaseTime() { return releaseTime; }
    public void setReleaseTime(EarningsDate.ReleaseTime releaseTime) { this.releaseTime = releaseTime; }

    public EarningsDate.State getState() { return state; }
    public void setState(EarningsDate.State state) { this.state = state; }

    public LocalDate getEarningsDate() { return earningsDate; }
    public void setEarningsDate(LocalDate earningsDate) { this.earningsDate = earningsDate; }
}
