package com.oracle.view.source;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryMetadata {
    
    @JsonProperty("queryType")
    public String queryType;
    
    @JsonProperty("actionType")
    public String actionType;
    
    @JsonProperty("processingTimeMs")
    public long processingTimeMs;
    
    public QueryMetadata() {}
    
    public QueryMetadata(String queryType, String actionType, long processingTimeMs) {
        this.queryType = queryType;
        this.actionType = actionType;
        this.processingTimeMs = processingTimeMs;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionType() {
        return actionType;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
}
