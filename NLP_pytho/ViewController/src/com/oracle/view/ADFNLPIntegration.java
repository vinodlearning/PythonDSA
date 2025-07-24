package com.oracle.view;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Oracle ADF Integration for Advanced NLP Engine
 * JSF Managed Bean for handling natural language queries in ADF applications
 * 
 * Usage in JSF page:
 * <af:inputText value="#{aDFNLPIntegration.userQuery}" />
 * <af:commandButton text="Process Query" action="#{aDFNLPIntegration.processNaturalLanguageQuery}" />
 */
@ManagedBean(name = "aDFNLPIntegration")
@SessionScoped
public class ADFNLPIntegration implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // NLP Engine instance
    private final AdvancedNLPEngine nlpEngine;
    
    // UI Binding properties
    private String userQuery;
    private String lastProcessedQuery;
    private NLPResponse lastResponse;
    private boolean showResults;
    private String processingStatus;
    private List<QueryHistory> queryHistory;
    
    // Response display properties
    private String displayQueryType;
    private String displayActionType;
    private String displayContractNumber;
    private String displayCustomerNumber;
    private String displayCustomerName;
    private String displayPartNumber;
    private String displayCreatedBy;
    private String displayModule;
    private String displayCorrectedInput;
    private double displayConfidence;
    private List<String> displayErrors;
    
    /**
     * Constructor
     */
    public ADFNLPIntegration() {
        this.nlpEngine = new AdvancedNLPEngine();
        this.queryHistory = new ArrayList<>();
        this.displayErrors = new ArrayList<>();
        this.showResults = false;
        this.processingStatus = "Ready";
        
        // Initialize display properties
        clearDisplayProperties();
    }
    
    /**
     * Main method to process natural language queries
     * Called from JSF commandButton action
     */
    public String processNaturalLanguageQuery() {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            processingStatus = "Error: Please enter a query";
            return null;
        }
        
        try {
            processingStatus = "Processing...";
            
            // Process the query using Advanced NLP Engine
            NLPResponse response = nlpEngine.processQuery(userQuery.trim());
            
            // Store the response
            lastProcessedQuery = userQuery.trim();
            lastResponse = response;
            
            // Update display properties
            updateDisplayProperties(response);
            
            // Add to history
            addToHistory(userQuery.trim(), response);
            
            // Show results
            showResults = true;
            processingStatus = "Query processed successfully";
            
            // Route to appropriate page based on domain
            return routeToPage(response);
            
        } catch (Exception e) {
            processingStatus = "Error: " + e.getMessage();
            showResults = false;
            return null;
        }
    }
    
    /**
     * Route to appropriate ADF page based on NLP response
     */
    private String routeToPage(NLPResponse response) {
        String queryType = response.getQueryType();
        String actionType = response.getActionType();
        
        // Store response in session for the target page
        FacesContext.getCurrentInstance().getExternalContext()
            .getSessionMap().put("nlpResponse", response);
        
        // Route based on domain and action type
        switch (queryType) {
            case "CONTRACTS":
                if (actionType.equals("contracts_by_customerNumber")) {
                    return "contracts-by-customer";
                } else if (actionType.equals("contracts_by_contractNumber")) {
                    return "contracts-by-number";
                } else if (actionType.equals("contracts_by_user")) {
                    return "contracts-by-user";
                } else if (actionType.equals("contracts_by_dates")) {
                    return "contracts-by-dates";
                } else {
                    return "contracts-general";
                }
                
            case "PARTS":
                if (actionType.equals("parts_by_contract")) {
                    return "parts-by-contract";
                } else if (actionType.equals("parts_by_partNumber")) {
                    return "parts-by-number";
                } else {
                    return "parts-general";
                }
                
            case "HELP":
                if (actionType.equals("help_create_contract")) {
                    return "help-create-contract";
                } else if (actionType.equals("steps_create_contract")) {
                    return "steps-create-contract";
                } else if (actionType.equals("help_create_part")) {
                    return "help-create-part";
                } else {
                    return "help-general";
                }
                
            default:
                return "search-results";
        }
    }
    
    /**
     * Update display properties from NLP response
     */
    private void updateDisplayProperties(NLPResponse response) {
        displayQueryType = response.getQueryType();
        displayActionType = response.getActionType();
        displayModule = response.getSelectedModule();
        displayConfidence = response.getConfidence();
        displayCorrectedInput = response.getCorrectedInput();
        
        // Header properties
        if (response.getHeader() != null) {
            displayContractNumber = response.getHeader().getContractNumber();
            displayCustomerNumber = response.getHeader().getCustomerNumber();
            displayCustomerName = response.getHeader().getCustomerName();
            displayPartNumber = response.getHeader().getPartNumber();
            displayCreatedBy = response.getHeader().getCreatedBy();
        }
        
        // Errors
        displayErrors = response.getErrors() != null ? response.getErrors() : new ArrayList<>();
    }
    
    /**
     * Clear all display properties
     */
    private void clearDisplayProperties() {
        displayQueryType = null;
        displayActionType = null;
        displayContractNumber = null;
        displayCustomerNumber = null;
        displayCustomerName = null;
        displayPartNumber = null;
        displayCreatedBy = null;
        displayModule = null;
        displayCorrectedInput = null;
        displayConfidence = 0.0;
        displayErrors.clear();
    }
    
    /**
     * Add query to history
     */
    private void addToHistory(String query, NLPResponse response) {
        QueryHistory history = new QueryHistory();
        history.setQuery(query);
        history.setQueryType(response.getQueryType());
        history.setActionType(response.getActionType());
        history.setTimestamp(new java.util.Date());
        history.setConfidence(response.getConfidence());
        
        queryHistory.add(0, history); // Add to beginning
        
        // Keep only last 20 queries
        if (queryHistory.size() > 20) {
            queryHistory = queryHistory.subList(0, 20);
        }
    }
    
    /**
     * Clear query history
     */
    public void clearHistory() {
        queryHistory.clear();
    }
    
    /**
     * Reset the form
     */
    public void resetForm() {
        userQuery = null;
        lastProcessedQuery = null;
        lastResponse = null;
        showResults = false;
        processingStatus = "Ready";
        clearDisplayProperties();
    }
    
    /**
     * Get sample queries for help
     */
    public List<String> getSampleQueries() {
        List<String> samples = new ArrayList<>();
        samples.add("contracts for customer number 897654");
        samples.add("show parts for contract 123456");
        samples.add("help me create contract");
        samples.add("contracts created by vinod");
        samples.add("parts validation failed for contract ABC-123");
        samples.add("what is the expiration date for contract 789012");
        samples.add("list all parts for customer 445566");
        samples.add("steps to create contract");
        return samples;
    }
    
    /**
     * Use a sample query
     */
    public void useSampleQuery(String sampleQuery) {
        this.userQuery = sampleQuery;
    }
    
    /**
     * Get formatted JSON response for debugging
     */
    public String getFormattedJsonResponse() {
        if (lastResponse == null) {
            return "No response available";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"queryType\": \"").append(lastResponse.getQueryType()).append("\",\n");
        json.append("  \"actionType\": \"").append(lastResponse.getActionType()).append("\",\n");
        json.append("  \"selectedModule\": \"").append(lastResponse.getSelectedModule()).append("\",\n");
        json.append("  \"confidence\": ").append(String.format("%.2f", lastResponse.getConfidence())).append(",\n");
        json.append("  \"header\": {\n");
        if (lastResponse.getHeader() != null) {
            json.append("    \"contractNumber\": \"").append(lastResponse.getHeader().getContractNumber()).append("\",\n");
            json.append("    \"customerNumber\": \"").append(lastResponse.getHeader().getCustomerNumber()).append("\",\n");
            json.append("    \"customerName\": \"").append(lastResponse.getHeader().getCustomerName()).append("\",\n");
            json.append("    \"partNumber\": \"").append(lastResponse.getHeader().getPartNumber()).append("\",\n");
            json.append("    \"createdBy\": \"").append(lastResponse.getHeader().getCreatedBy()).append("\"\n");
        }
        json.append("  },\n");
        json.append("  \"errors\": ").append(lastResponse.getErrors()).append("\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Check if NLP engine is healthy
     */
    public boolean isNLPEngineHealthy() {
        return nlpEngine.isHealthy();
    }
    
    /**
     * Get available domains
     */
    public String getAvailableDomains() {
        return nlpEngine.getAvailableDomains().toString();
    }
    
    // Getters and Setters
    public String getUserQuery() {
        return userQuery;
    }
    
    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }
    
    public String getLastProcessedQuery() {
        return lastProcessedQuery;
    }
    
    public NLPResponse getLastResponse() {
        return lastResponse;
    }
    
    public boolean isShowResults() {
        return showResults;
    }
    
    public String getProcessingStatus() {
        return processingStatus;
    }
    
    public List<QueryHistory> getQueryHistory() {
        return queryHistory;
    }
    
    public String getDisplayQueryType() {
        return displayQueryType;
    }
    
    public String getDisplayActionType() {
        return displayActionType;
    }
    
    public String getDisplayContractNumber() {
        return displayContractNumber;
    }
    
    public String getDisplayCustomerNumber() {
        return displayCustomerNumber;
    }
    
    public String getDisplayCustomerName() {
        return displayCustomerName;
    }
    
    public String getDisplayPartNumber() {
        return displayPartNumber;
    }
    
    public String getDisplayCreatedBy() {
        return displayCreatedBy;
    }
    
    public String getDisplayModule() {
        return displayModule;
    }
    
    public String getDisplayCorrectedInput() {
        return displayCorrectedInput;
    }
    
    public double getDisplayConfidence() {
        return displayConfidence;
    }
    
    public List<String> getDisplayErrors() {
        return displayErrors;
    }
    
    /**
     * Inner class for query history
     */
    public static class QueryHistory implements Serializable {
        private String query;
        private String queryType;
        private String actionType;
        private java.util.Date timestamp;
        private double confidence;
        
        // Getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        
        public java.util.Date getTimestamp() { return timestamp; }
        public void setTimestamp(java.util.Date timestamp) { this.timestamp = timestamp; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getFormattedTimestamp() {
            if (timestamp == null) return "";
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
        }
    }
}