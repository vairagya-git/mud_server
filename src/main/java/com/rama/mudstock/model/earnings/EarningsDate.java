package com.rama.mudstock.model.earnings;

import java.time.LocalDate;

public class EarningsDate {
    private Long id;
    private Long stockId;
    private String quarter;
    private ReleaseTime releaseTime;
    private Status status;
    private Integer noOfDays = 10;
    private LocalDate earningsDate;

    public enum ReleaseTime { AFTER_MARKET, BEFORE_MARKET }
    public enum Status { UPCOMING, NEW, PROCESSING, PROCESSED, PAST }

    public EarningsDate() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }

    // ticker is derived from Stock; not stored on EarningsDate

    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }

    public ReleaseTime getReleaseTime() { return releaseTime; }
    public void setReleaseTime(ReleaseTime releaseTime) { this.releaseTime = releaseTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Integer getNoOfDays() { return noOfDays; }
    public void setNoOfDays(Integer noOfDays) { this.noOfDays = noOfDays; }

    public LocalDate getEarningsDate() { return earningsDate; }
    public void setEarningsDate(LocalDate earningsDate) { this.earningsDate = earningsDate; }
}
