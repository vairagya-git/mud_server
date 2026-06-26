package com.rama.mudstock.model.analyst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BenzingaAnalystResponse {

    @JsonProperty("benzinga_id")
    private String benzingaId;

    @JsonProperty("benzinga_firm_id")
    private String benzingaFirmId;

    @JsonProperty("firm_name")
    private String firmName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("last_updated")
    private String lastUpdated;

    @JsonProperty("smart_score")
    private Double smartScore;

    @JsonProperty("total_ratings")
    private Double totalRatings;

    @JsonProperty("total_ratings_percentile")
    private Double totalRatingsPercentile;

    @JsonProperty("overall_success_rate")
    private Double overallSuccessRate;

    @JsonProperty("overall_avg_return")
    private Double overallAvgReturn;

    @JsonProperty("overall_avg_return_percentile")
    private Double overallAvgReturnPercentile;

    public BenzingaAnalystResponse() {}

    public String getBenzingaId() { return benzingaId; }
    public void setBenzingaId(String benzingaId) { this.benzingaId = benzingaId; }

    public String getBenzingaFirmId() { return benzingaFirmId; }
    public void setBenzingaFirmId(String benzingaFirmId) { this.benzingaFirmId = benzingaFirmId; }

    public String getFirmName() { return firmName; }
    public void setFirmName(String firmName) { this.firmName = firmName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public Double getSmartScore() { return smartScore; }
    public void setSmartScore(Double smartScore) { this.smartScore = smartScore; }

    public Double getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Double totalRatings) { this.totalRatings = totalRatings; }

    public Double getTotalRatingsPercentile() { return totalRatingsPercentile; }
    public void setTotalRatingsPercentile(Double totalRatingsPercentile) { this.totalRatingsPercentile = totalRatingsPercentile; }

    public Double getOverallSuccessRate() { return overallSuccessRate; }
    public void setOverallSuccessRate(Double overallSuccessRate) { this.overallSuccessRate = overallSuccessRate; }

    public Double getOverallAvgReturn() { return overallAvgReturn; }
    public void setOverallAvgReturn(Double overallAvgReturn) { this.overallAvgReturn = overallAvgReturn; }

    public Double getOverallAvgReturnPercentile() { return overallAvgReturnPercentile; }
    public void setOverallAvgReturnPercentile(Double overallAvgReturnPercentile) { this.overallAvgReturnPercentile = overallAvgReturnPercentile; }
}
