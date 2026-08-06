package com.rama.mudstock.model.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OptionContract {

    private Long id;
    private Long stockId;
    private String ticker;
    private String contractType;
    private String source;
    private String status;
    private String exerciseStyle;
    private LocalDate expirationDate;
    private BigDecimal strikePrice;
    private Integer sharesPerContract;
    private String contractTicker;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<OptionSnapshot> optionSnapshots = new ArrayList<>();
    private List<OptionFlatFile> optionFlatFiles = new ArrayList<>();

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

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExerciseStyle() {
        return exerciseStyle;
    }

    public void setExerciseStyle(String exerciseStyle) {
        this.exerciseStyle = exerciseStyle;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(BigDecimal strikePrice) {
        this.strikePrice = strikePrice;
    }

    public Integer getSharesPerContract() {
        return sharesPerContract;
    }

    public void setSharesPerContract(Integer sharesPerContract) {
        this.sharesPerContract = sharesPerContract;
    }

    public String getContractTicker() {
        return contractTicker;
    }

    public void setContractTicker(String contractTicker) {
        this.contractTicker = contractTicker;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OptionSnapshot> getOptionSnapshots() {
        return optionSnapshots;
    }

    public void setOptionSnapshots(List<OptionSnapshot> optionSnapshots) {
        this.optionSnapshots = optionSnapshots;
    }

    public List<OptionFlatFile> getOptionFlatFiles() {
        return optionFlatFiles;
    }

    public void setOptionFlatFiles(List<OptionFlatFile> optionFlatFiles) {
        this.optionFlatFiles = optionFlatFiles;
    }
}
