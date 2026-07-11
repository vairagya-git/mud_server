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
@Table(name = "firm_analyst_stock_rating")
public class FirmAnalystStockRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firm_analyst_id", nullable = false)
    private Long firmAnalystId;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "rating_action", nullable = false, columnDefinition = "ENUM('maintains','downgrades','upgrades','initiates_coverage_on','reiterates','resumes_coverage','suspends_coverage')")
    private String ratingAction;

    @Column(name = "price_target_action", nullable = false, columnDefinition = "ENUM('maintains','lowers','raises','announces','removes','resumes','none')")
    private String priceTargetAction;

    @Column(name = "rating", nullable = false, columnDefinition = "ENUM('buy','outperform','overweight','positive','equal-weight','sector perform','sector outperform','market outperform','neutral','hold','sell','perform')")
    private String rating;

    @Column(name = "previous_rating", nullable = false, columnDefinition = "ENUM('buy','outperform','overweight','positive','equal-weight','sector perform','sector outperform','market outperform','neutral','hold','sell','perform')")
    private String previousRating;

    @Column(name = "price_target", nullable = false, precision = 20, scale = 0)
    private BigDecimal priceTarget;

    @Column(name = "previous_price_target", nullable = false, precision = 20, scale = 0)
    private BigDecimal previousPriceTarget;

    @Column(name = "price_percent_change", nullable = false, precision = 20, scale = 2)
    private BigDecimal pricePercentChange;

    @Column(name = "adjusted_price_target", nullable = false, precision = 20, scale = 0)
    private BigDecimal adjustedPriceTarget;

    @Column(name = "previous_adjusted_price_target", nullable = false, precision = 20, scale = 0)
    private BigDecimal previousAdjustedPriceTarget;

    @Column(name = "importance", nullable = false)
    private Integer importance;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "benzinga_calendar_url", nullable = false, length = 512)
    private String benzingaCalendarUrl;

    @Column(name = "benzinga_news_url", nullable = false, length = 512)
    private String benzingaNewsUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public FirmAnalystStockRating() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFirmAnalystId() { return firmAnalystId; }
    public void setFirmAnalystId(Long firmAnalystId) { this.firmAnalystId = firmAnalystId; }

    public Long getFirmId() { return firmId; }
    public void setFirmId(Long firmId) { this.firmId = firmId; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }

    public String getRatingAction() { return ratingAction; }
    public void setRatingAction(String ratingAction) { this.ratingAction = ratingAction; }

    public String getPriceTargetAction() { return priceTargetAction; }
    public void setPriceTargetAction(String priceTargetAction) { this.priceTargetAction = priceTargetAction; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getPreviousRating() { return previousRating; }
    public void setPreviousRating(String previousRating) { this.previousRating = previousRating; }

    public BigDecimal getPriceTarget() { return priceTarget; }
    public void setPriceTarget(BigDecimal priceTarget) { this.priceTarget = priceTarget; }

    public BigDecimal getPreviousPriceTarget() { return previousPriceTarget; }
    public void setPreviousPriceTarget(BigDecimal previousPriceTarget) { this.previousPriceTarget = previousPriceTarget; }

    public BigDecimal getPricePercentChange() { return pricePercentChange; }
    public void setPricePercentChange(BigDecimal pricePercentChange) { this.pricePercentChange = pricePercentChange; }

    public BigDecimal getAdjustedPriceTarget() { return adjustedPriceTarget; }
    public void setAdjustedPriceTarget(BigDecimal adjustedPriceTarget) { this.adjustedPriceTarget = adjustedPriceTarget; }

    public BigDecimal getPreviousAdjustedPriceTarget() { return previousAdjustedPriceTarget; }
    public void setPreviousAdjustedPriceTarget(BigDecimal previousAdjustedPriceTarget) { this.previousAdjustedPriceTarget = previousAdjustedPriceTarget; }

    public Integer getImportance() { return importance; }
    public void setImportance(Integer importance) { this.importance = importance; }

    public LocalDate getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getBenzingaCalendarUrl() { return benzingaCalendarUrl; }
    public void setBenzingaCalendarUrl(String benzingaCalendarUrl) { this.benzingaCalendarUrl = benzingaCalendarUrl; }

    public String getBenzingaNewsUrl() { return benzingaNewsUrl; }
    public void setBenzingaNewsUrl(String benzingaNewsUrl) { this.benzingaNewsUrl = benzingaNewsUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
