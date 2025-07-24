package com.oracle.view;
import com.oracle.view.QueryMetadata;
import com.oracle.view.ResponseHeader;

import java.util.List;

/**
 * Standardized NLP response format matching the user's JSON specification
 */
public class NLPResponse {
    private ResponseHeader header;
    private QueryMetadata queryMetadata;
    private List<EntityInfo> entities;
    private List<String> displayEntities;
    private Object moduleSpecificData;
    private List<String> errors;
    private double confidence;
    
    // Additional fields for internal processing
    private String originalInput;
    private String correctedInput;
    private double correctionConfidence;
    private String selectedModule;
    private double routingConfidence;
    private long processingTimeMs;
    private String queryType;
    private String actionType;
    
    // Private constructor for builder pattern
    private NLPResponse() {}
    
    // Getters
    public ResponseHeader getHeader() { return header; }
    public QueryMetadata getQueryMetadata() { return queryMetadata; }
    public List<EntityInfo> getEntities() { return entities; }
    public List<String> getDisplayEntities() { return displayEntities; }
    public Object getModuleSpecificData() { return moduleSpecificData; }
    public List<String> getErrors() { return errors; }
    public double getConfidence() { return confidence; }
    public String getOriginalInput() { return originalInput; }
    public String getCorrectedInput() { return correctedInput; }
    public double getCorrectionConfidence() { return correctionConfidence; }
    public String getSelectedModule() { return selectedModule; }
    public double getRoutingConfidence() { return routingConfidence; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    public String getQueryType() { return queryType; }
    public String getActionType() { return actionType; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private NLPResponse response = new NLPResponse();
        
        public Builder header(ResponseHeader header) {
            response.header = header;
            return this;
        }
        
        public Builder queryMetadata(QueryMetadata queryMetadata) {
            response.queryMetadata = queryMetadata;
            return this;
        }
        
        public Builder entities(List<EntityInfo> entities) {
            response.entities = entities;
            return this;
        }
        
        public Builder displayEntities(List<String> displayEntities) {
            response.displayEntities = displayEntities;
            return this;
        }
        
        public Builder moduleSpecificData(Object moduleSpecificData) {
            response.moduleSpecificData = moduleSpecificData;
            return this;
        }
        
        public Builder errors(List<String> errors) {
            response.errors = errors;
            return this;
        }
        
        public Builder confidence(double confidence) {
            response.confidence = confidence;
            return this;
        }
        
        public Builder originalInput(String originalInput) {
            response.originalInput = originalInput;
            return this;
        }
        
        public Builder correctedInput(String correctedInput) {
            response.correctedInput = correctedInput;
            return this;
        }
        
        public Builder correctionConfidence(double correctionConfidence) {
            response.correctionConfidence = correctionConfidence;
            return this;
        }
        
        public Builder selectedModule(String selectedModule) {
            response.selectedModule = selectedModule;
            return this;
        }
        
        public Builder routingConfidence(double routingConfidence) {
            response.routingConfidence = routingConfidence;
            return this;
        }
        
        public Builder processingTimeMs(long processingTimeMs) {
            response.processingTimeMs = processingTimeMs;
            return this;
        }
        
        public Builder queryType(String queryType) {
            response.queryType = queryType;
            return this;
        }
        
        public Builder actionType(String actionType) {
            response.actionType = actionType;
            return this;
        }
        
        public NLPResponse build() {
            return response;
        }
    }
    
    @Override
    public String toString() {
        return String.format("NLPResponse{module=%s, queryType=%s, confidence=%.2f}", 
                           selectedModule, queryType, confidence);
    }
}