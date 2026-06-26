package com.rama.mudstock.model.analyst;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "firm_analyst")
public class FirmAnalyst {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "benzinga_analyst_id", nullable = false, length = 64)
    private String benzingaAnalystId;

    @Column(name = "benzinga_firm_id", nullable = false, length = 64)
    private String benzingaFirmId;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;

    @Column(name = "overall_avg_return", precision = 20, scale = 2)
    private BigDecimal overallAvgReturn;

    @Column(name = "overall_avg_return_percentile", precision = 20, scale = 2)
    private BigDecimal overallAvgReturnPercentile;

    @Column(name = "overall_success_rate", precision = 20, scale = 2)
    private BigDecimal overallSuccessRate;

    @Column(name = "smart_score", precision = 20, scale = 2)
    private BigDecimal smartScore;

    @Column(name = "total_ratings", precision = 20, scale = 2)
    private BigDecimal totalRatings;

    @Column(name = "total_ratings_percentile", precision = 20, scale = 2)
    private BigDecimal totalRatingsPercentile;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public FirmAnalyst() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFirmId() { return firmId; }
    public void setFirmId(Long firmId) { this.firmId = firmId; }

    public String getBenzingaAnalystId() { return benzingaAnalystId; }
    public void setBenzingaAnalystId(String benzingaAnalystId) { this.benzingaAnalystId = benzingaAnalystId; }

    public String getBenzingaFirmId() { return benzingaFirmId; }
    public void setBenzingaFirmId(String benzingaFirmId) { this.benzingaFirmId = benzingaFirmId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }

    public BigDecimal getOverallAvgReturn() { return overallAvgReturn; }
    public void setOverallAvgReturn(BigDecimal overallAvgReturn) { this.overallAvgReturn = overallAvgReturn; }

    public BigDecimal getOverallAvgReturnPercentile() { return overallAvgReturnPercentile; }
    public void setOverallAvgReturnPercentile(BigDecimal overallAvgReturnPercentile) { this.overallAvgReturnPercentile = overallAvgReturnPercentile; }

    public BigDecimal getOverallSuccessRate() { return overallSuccessRate; }
    public void setOverallSuccessRate(BigDecimal overallSuccessRate) { this.overallSuccessRate = overallSuccessRate; }

    public BigDecimal getSmartScore() { return smartScore; }
    public void setSmartScore(BigDecimal smartScore) { this.smartScore = smartScore; }

    public BigDecimal getTotalRatings() { return totalRatings; }
    public void setTotalRatings(BigDecimal totalRatings) { this.totalRatings = totalRatings; }

    public BigDecimal getTotalRatingsPercentile() { return totalRatingsPercentile; }
    public void setTotalRatingsPercentile(BigDecimal totalRatingsPercentile) { this.totalRatingsPercentile = totalRatingsPercentile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
