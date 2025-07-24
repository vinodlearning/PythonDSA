package com.oracle.view;

import java.util.List;

/**
 * JSON Response structure for NLP processing
 * Matches the exact JSON format required by the user
 */
public class NLPJsonResponse {
    private Header header;
    private QueryMetadata queryMetadata;
    private List<EntityFilter> entities;
    private List<String> displayEntities;
    private List<ErrorInfo> errors;
    
    // Constructors
    public NLPJsonResponse() {}
    
    // Getters and Setters
    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    
    public QueryMetadata getQueryMetadata() { return queryMetadata; }
    public void setQueryMetadata(QueryMetadata queryMetadata) { this.queryMetadata = queryMetadata; }
    
    public List<EntityFilter> getEntities() { return entities; }
    public void setEntities(List<EntityFilter> entities) { this.entities = entities; }
    
    public List<String> getDisplayEntities() { return displayEntities; }
    public void setDisplayEntities(List<String> displayEntities) { this.displayEntities = displayEntities; }
    
    public List<ErrorInfo> getErrors() { return errors; }
    public void setErrors(List<ErrorInfo> errors) { this.errors = errors; }
    
    /**
     * Header section containing extracted entities and input tracking
     */
    public static class Header {
        private String contractNumber;
        private String partNumber;
        private String customerNumber;
        private String customerName;
        private String createdBy;
        private InputTracking inputTracking;
        
        // Constructors
        public Header() {}
        
        // Getters and Setters
        public String getContractNumber() { return contractNumber; }
        public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
        
        public String getPartNumber() { return partNumber; }
        public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
        
        public String getCustomerNumber() { return customerNumber; }
        public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        
        public InputTracking getInputTracking() { return inputTracking; }
        public void setInputTracking(InputTracking inputTracking) { this.inputTracking = inputTracking; }
    }
    
    /**
     * Input tracking section for original and corrected input
     */
    public static class InputTracking {
        private String originalInput;
        private String correctedInput;
        private double correctionConfidence;
        
        // Constructors
        public InputTracking() {}
        
        // Getters and Setters
        public String getOriginalInput() { return originalInput; }
        public void setOriginalInput(String originalInput) { this.originalInput = originalInput; }
        
        public String getCorrectedInput() { return correctedInput; }
        public void setCorrectedInput(String correctedInput) { this.correctedInput = correctedInput; }
        
        public double getCorrectionConfidence() { return correctionConfidence; }
        public void setCorrectionConfidence(double correctionConfidence) { this.correctionConfidence = correctionConfidence; }
    }
    
    /**
     * Query metadata section
     */
    public static class QueryMetadata {
        private String queryType;
        private String actionType;
        private long processingTimeMs;
        
        // Constructors
        public QueryMetadata() {}
        
        // Getters and Setters
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
}