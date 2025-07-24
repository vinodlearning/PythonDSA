package com.oracle.view;

/**
 * Metadata about query processing
 */
public class QueryMetadata {
    private String queryType;
    private String actionType;
    private double processingTimeMs;
    private String selectedModule;
    private double routingConfidence;
    
    // Private constructor for builder pattern
    private QueryMetadata() {}
    
    // Getters
    public String getQueryType() { return queryType; }
    public String getActionType() { return actionType; }
    public double getProcessingTimeMs() { return processingTimeMs; }
    public String getSelectedModule() { return selectedModule; }
    public double getRoutingConfidence() { return routingConfidence; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private QueryMetadata metadata = new QueryMetadata();
        
        public Builder queryType(String queryType) {
            metadata.queryType = queryType;
            return this;
        }
        
        public Builder actionType(String actionType) {
            metadata.actionType = actionType;
            return this;
        }
        
        public Builder processingTimeMs(double processingTimeMs) {
            metadata.processingTimeMs = processingTimeMs;
            return this;
        }
        
        public Builder selectedModule(String selectedModule) {
            metadata.selectedModule = selectedModule;
            return this;
        }
        
        public Builder routingConfidence(double routingConfidence) {
            metadata.routingConfidence = routingConfidence;
            return this;
        }
        
        public QueryMetadata build() {
            return metadata;
        }
    }
}