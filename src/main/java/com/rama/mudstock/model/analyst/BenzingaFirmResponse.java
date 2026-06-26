package com.rama.mudstock.model.analyst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for a single firm entry returned by the Benzinga firms REST endpoint.
 *
 * <pre>
 * {
 *   "benzinga_id": "66c57995cf364000017832c6",
 *   "name": "ABCI",
 *   "currency": "USD",
 *   "last_updated": "2024-08-21T05:22:36"
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BenzingaFirmResponse {

    @JsonProperty("benzinga_id")
    private String benzingaId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("currency")
    private String currency;

    /** ISO datetime string e.g. {@code 2024-08-21T05:22:36} — only the date part is stored. */
    @JsonProperty("last_updated")
    private String lastUpdated;

    public BenzingaFirmResponse() {}

    public String getBenzingaId() { return benzingaId; }
    public void setBenzingaId(String benzingaId) { this.benzingaId = benzingaId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}