package com.rama.mudstock.model.analyst;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BenzingaAnalystListResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("results")
    private List<BenzingaAnalystResponse> results;

    public BenzingaAnalystListResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public List<BenzingaAnalystResponse> getResults() { return results; }
    public void setResults(List<BenzingaAnalystResponse> results) { this.results = results; }
}
