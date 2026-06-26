package com.rama.mudstock.model.analyst;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for the Benzinga firms list API response.
 *
 * <pre>
 * {
 *   "status": "OK",
 *   "request_id": "...",
 *   "results": [ { "benzinga_id": "...", "name": "...", ... } ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BenzingaFirmListResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("results")
    private List<BenzingaFirmResponse> results;

    public BenzingaFirmListResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public List<BenzingaFirmResponse> getResults() { return results; }
    public void setResults(List<BenzingaFirmResponse> results) { this.results = results; }
}