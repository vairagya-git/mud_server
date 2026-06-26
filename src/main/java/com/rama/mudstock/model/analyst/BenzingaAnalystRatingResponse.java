package com.rama.mudstock.model.analyst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BenzingaAnalystRatingResponse {

    @JsonProperty("benzinga_id")
    private String benzingaId;

    @JsonProperty("benzinga_analyst_id")
    private String benzingaAnalystId;

    @JsonProperty("benzinga_firm_id")
    private String benzingaFirmId;

    @JsonProperty("firm")
    private String firm;

    @JsonProperty("analyst")
    private String analyst;

    @JsonProperty("ticker")
    private String ticker;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("rating_action")
    private String ratingAction;

    @JsonProperty("price_target_action")
    private String priceTargetAction;

    @JsonProperty("rating")
    private String rating;

    @JsonProperty("previous_rating")
    private String previousRating;

    @JsonProperty("price_target")
    private Double priceTarget;

    @JsonProperty("previous_price_target")
    private Double previousPriceTarget;

    @JsonProperty("adjusted_price_target")
    private Double adjustedPriceTarget;

    @JsonProperty("previous_adjusted_price_target")
    private Double previousAdjustedPriceTarget;

    @JsonProperty("price_percent_change")
    private Double pricePercentChange;

    @JsonProperty("importance")
    private Integer importance;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("last_updated")
    private String lastUpdated;

    @JsonProperty("date")
    private String date;

    @JsonProperty("time")
    private String time;

    @JsonProperty("benzinga_calendar_url")
    private String benzingaCalendarUrl;

    @JsonProperty("benzinga_news_url")
    private String benzingaNewsUrl;

    public BenzingaAnalystRatingResponse() {}

    public String getBenzingaId() { return benzingaId; }
    public void setBenzingaId(String benzingaId) { this.benzingaId = benzingaId; }

    public String getBenzingaAnalystId() { return benzingaAnalystId; }
    public void setBenzingaAnalystId(String benzingaAnalystId) { this.benzingaAnalystId = benzingaAnalystId; }

    public String getBenzingaFirmId() { return benzingaFirmId; }
    public void setBenzingaFirmId(String benzingaFirmId) { this.benzingaFirmId = benzingaFirmId; }

    public String getFirm() { return firm; }
    public void setFirm(String firm) { this.firm = firm; }

    public String getAnalyst() { return analyst; }
    public void setAnalyst(String analyst) { this.analyst = analyst; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRatingAction() { return ratingAction; }
    public void setRatingAction(String ratingAction) { this.ratingAction = ratingAction; }

    public String getPriceTargetAction() { return priceTargetAction; }
    public void setPriceTargetAction(String priceTargetAction) { this.priceTargetAction = priceTargetAction; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getPreviousRating() { return previousRating; }
    public void setPreviousRating(String previousRating) { this.previousRating = previousRating; }

    public Double getPriceTarget() { return priceTarget; }
    public void setPriceTarget(Double priceTarget) { this.priceTarget = priceTarget; }

    public Double getPreviousPriceTarget() { return previousPriceTarget; }
    public void setPreviousPriceTarget(Double previousPriceTarget) { this.previousPriceTarget = previousPriceTarget; }

    public Double getAdjustedPriceTarget() { return adjustedPriceTarget; }
    public void setAdjustedPriceTarget(Double adjustedPriceTarget) { this.adjustedPriceTarget = adjustedPriceTarget; }

    public Double getPreviousAdjustedPriceTarget() { return previousAdjustedPriceTarget; }
    public void setPreviousAdjustedPriceTarget(Double previousAdjustedPriceTarget) { this.previousAdjustedPriceTarget = previousAdjustedPriceTarget; }

    public Double getPricePercentChange() { return pricePercentChange; }
    public void setPricePercentChange(Double pricePercentChange) { this.pricePercentChange = pricePercentChange; }

    public Integer getImportance() { return importance; }
    public void setImportance(Integer importance) { this.importance = importance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getBenzingaCalendarUrl() { return benzingaCalendarUrl; }
    public void setBenzingaCalendarUrl(String benzingaCalendarUrl) { this.benzingaCalendarUrl = benzingaCalendarUrl; }

    public String getBenzingaNewsUrl() { return benzingaNewsUrl; }
    public void setBenzingaNewsUrl(String benzingaNewsUrl) { this.benzingaNewsUrl = benzingaNewsUrl; }
}
