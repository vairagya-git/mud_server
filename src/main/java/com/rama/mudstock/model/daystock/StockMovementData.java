package com.rama.mudstock.model.daystock;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class StockMovementData {

    private Long id;
    private Long stockId;
    private Long earningsDateId;
    private BigDecimal preDayClose;
    private BigDecimal curDayOpen;
    private BigDecimal curDayClose;
    private BigDecimal curDayHigh;
    private String curDayHighSnapShotDatetime;
    private String curDayHighFlatFileDatetime;
    private BigDecimal curDayLow;
    private String curDayLowSnapShotDatetime;
    private String curDayLowFlatFileDatetime;
    private BigDecimal curDayVolWeight;
    private Long curDayVolume;
    private BigDecimal changePercent;
    private Boolean earnings;
    private BigDecimal dayOpeningChangePercent;
    private Timestamp dayStockMovementDate;
    private String ticker;
    private Timestamp masterEventDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public Long getEarningsDateId() {
        return earningsDateId;
    }

    public void setEarningsDateId(Long earningsDateId) {
        this.earningsDateId = earningsDateId;
    }

    public BigDecimal getPreDayClose() {
        return preDayClose;
    }

    public void setPreDayClose(BigDecimal preDayClose) {
        this.preDayClose = preDayClose;
    }

    public BigDecimal getCurDayOpen() {
        return curDayOpen;
    }

    public void setCurDayOpen(BigDecimal curDayOpen) {
        this.curDayOpen = curDayOpen;
    }

    public BigDecimal getCurDayClose() {
        return curDayClose;
    }

    public void setCurDayClose(BigDecimal curDayClose) {
        this.curDayClose = curDayClose;
    }

    public BigDecimal getCurDayHigh() {
        return curDayHigh;
    }

    public void setCurDayHigh(BigDecimal curDayHigh) {
        this.curDayHigh = curDayHigh;
    }

    public String getCurDayHighSnapShotDatetime() {
        return curDayHighSnapShotDatetime;
    }

    public void setCurDayHighSnapShotDatetime(String curDayHighSnapShotDatetime) {
        this.curDayHighSnapShotDatetime = curDayHighSnapShotDatetime;
    }

    public String getCurDayHighFlatFileDatetime() {
        return curDayHighFlatFileDatetime;
    }

    public void setCurDayHighFlatFileDatetime(String curDayHighFlatFileDatetime) {
        this.curDayHighFlatFileDatetime = curDayHighFlatFileDatetime;
    }

    public BigDecimal getCurDayLow() {
        return curDayLow;
    }

    public void setCurDayLow(BigDecimal curDayLow) {
        this.curDayLow = curDayLow;
    }

    public String getCurDayLowSnapShotDatetime() {
        return curDayLowSnapShotDatetime;
    }

    public void setCurDayLowSnapShotDatetime(String curDayLowSnapShotDatetime) {
        this.curDayLowSnapShotDatetime = curDayLowSnapShotDatetime;
    }

    public String getCurDayLowFlatFileDatetime() {
        return curDayLowFlatFileDatetime;
    }

    public void setCurDayLowFlatFileDatetime(String curDayLowFlatFileDatetime) {
        this.curDayLowFlatFileDatetime = curDayLowFlatFileDatetime;
    }

    public BigDecimal getCurDayVolWeight() {
        return curDayVolWeight;
    }

    public void setCurDayVolWeight(BigDecimal curDayVolWeight) {
        this.curDayVolWeight = curDayVolWeight;
    }

    public Long getCurDayVolume() {
        return curDayVolume;
    }

    public void setCurDayVolume(Long curDayVolume) {
        this.curDayVolume = curDayVolume;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public Boolean getEarnings() {
        return earnings;
    }

    public void setEarnings(Boolean earnings) {
        this.earnings = earnings;
    }

    public BigDecimal getDayOpeningChangePercent() {
        return dayOpeningChangePercent;
    }

    public void setDayOpeningChangePercent(BigDecimal dayOpeningChangePercent) {
        this.dayOpeningChangePercent = dayOpeningChangePercent;
    }

    public Timestamp getDayStockMovementDate() {
        return dayStockMovementDate;
    }

    public void setDayStockMovementDate(Timestamp dayStockMovementDate) {
        this.dayStockMovementDate = dayStockMovementDate;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Timestamp getMasterEventDate() {
        return masterEventDate;
    }

    public void setMasterEventDate(Timestamp masterEventDate) {
        this.masterEventDate = masterEventDate;
    }
}
