package com.rama.mudstock.model.option;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class OptionSnapshot {

    private Timestamp optionQuoteTime;
    private BigDecimal underlyingPrice;
    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal midpoint;
    private BigDecimal impliedVolatility;
    private BigDecimal delta;
    private BigDecimal gamma;
    private BigDecimal theta;
    private BigDecimal vega;
    private Integer openInterest;
    private Integer dayVolume;

    public Timestamp getOptionQuoteTime() {
        return optionQuoteTime;
    }

    public void setOptionQuoteTime(Timestamp optionQuoteTime) {
        this.optionQuoteTime = optionQuoteTime;
    }

    public BigDecimal getUnderlyingPrice() {
        return underlyingPrice;
    }

    public void setUnderlyingPrice(BigDecimal underlyingPrice) {
        this.underlyingPrice = underlyingPrice;
    }

    public BigDecimal getBid() {
        return bid;
    }

    public void setBid(BigDecimal bid) {
        this.bid = bid;
    }

    public BigDecimal getAsk() {
        return ask;
    }

    public void setAsk(BigDecimal ask) {
        this.ask = ask;
    }

    public BigDecimal getMidpoint() {
        return midpoint;
    }

    public void setMidpoint(BigDecimal midpoint) {
        this.midpoint = midpoint;
    }

    public BigDecimal getImpliedVolatility() {
        return impliedVolatility;
    }

    public void setImpliedVolatility(BigDecimal impliedVolatility) {
        this.impliedVolatility = impliedVolatility;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }

    public BigDecimal getGamma() {
        return gamma;
    }

    public void setGamma(BigDecimal gamma) {
        this.gamma = gamma;
    }

    public BigDecimal getTheta() {
        return theta;
    }

    public void setTheta(BigDecimal theta) {
        this.theta = theta;
    }

    public BigDecimal getVega() {
        return vega;
    }

    public void setVega(BigDecimal vega) {
        this.vega = vega;
    }

    public Integer getOpenInterest() {
        return openInterest;
    }

    public void setOpenInterest(Integer openInterest) {
        this.openInterest = openInterest;
    }

    public Integer getDayVolume() {
        return dayVolume;
    }

    public void setDayVolume(Integer dayVolume) {
        this.dayVolume = dayVolume;
    }
}
