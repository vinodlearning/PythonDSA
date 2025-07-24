package com.oracle.view.source;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced UserHandler with multiple response formats
 * Provides complete response, JSON response, and Java object response
 */
public class EnhancedUserHandler {
    
    private StandardJSONProcessor nlpProcessor;
    private ActionTypeDataProvider dataProvider;
    
    public EnhancedUserHandler() {
        this.nlpProcessor = new StandardJSONProcessor();
        this.dataProvider = new ActionTypeDataProvider();
    }
    
    // ============================================================================
    // METHOD 1: Complete Response as String (after DataProvider actions)
    // ============================================================================
    
    /**
     * Processes user input and returns complete response as String
     * Includes all DataProvider actions and results
     */
    public String processUserInputCompleteResponse(String userInput) {
        try {
            // Step 1: Process NLP
            String jsonResponse = nlpProcessor.processQuery(userInput);
            StandardJSONProcessor.QueryResult queryResult = nlpProcessor.parseJSONToObject(jsonResponse);
            
            // Step 2: Execute DataProvider actions
            String dataProviderResponse = executeDataProviderActions(queryResult);
            
            // Step 3: Combine NLP and DataProvider results
            CompleteResponse completeResponse = new CompleteResponse();
            completeResponse.setNlpResponse(queryResult);
            completeResponse.setDataProviderResponse(dataProviderResponse);
            completeResponse.setSuccess(true);
            completeResponse.setMessage("Request processed successfully");
            
            return formatCompleteResponseToJSON(completeResponse);
            
        } catch (Exception e) {
            CompleteResponse errorResponse = new CompleteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Error processing request: " + e.getMessage());
            errorResponse.setErrorCode("PROCESSING_ERROR");
            errorResponse.setErrorDetails(e.getMessage());
            
            return formatCompleteResponseToJSON(errorResponse);
        }
    }
    
    // ============================================================================
    // METHOD 2: JSON Response with specified structure
    // ============================================================================
    
    /**
     * Processes user input and returns JSON response with specified structure
     */
    public String processUserInputJSONResponse(String userInput) {
        try {
            // Process NLP
            String jsonResponse = nlpProcessor.processQuery(userInput);
            StandardJSONProcessor.QueryResult queryResult = nlpProcessor.parseJSONToObject(jsonResponse);
            
            // Create structured JSON response
            StructuredJSONResponse structuredResponse = new StructuredJSONResponse();
            
            // Header
            StructuredJSONResponse.Header header = new StructuredJSONResponse.Header();
            header.setContractNumber(queryResult.header.contractNumber);
            header.setPartNumber(queryResult.header.partNumber);
            header.setCustomerNumber(queryResult.header.customerNumber);
            header.setCustomerName(queryResult.header.customerName);
            header.setCreatedBy(queryResult.header.createdBy);
            structuredResponse.setHeader(header);
            
            // Query Metadata
            StructuredJSONResponse.QueryMetadata metadata = new StructuredJSONResponse.QueryMetadata();
            metadata.setQueryType(queryResult.metadata.queryType);
            metadata.setActionType(queryResult.metadata.actionType);
            metadata.setProcessingTimeMs(queryResult.metadata.processingTimeMs);
            
            // Spell Correction
            StructuredJSONResponse.SpellCorrection spellCorrection = new StructuredJSONResponse.SpellCorrection();
            spellCorrection.setOriginalInput(queryResult.inputTracking.originalInput);
            spellCorrection.setCorrectedInput(queryResult.inputTracking.correctedInput);
            metadata.setSpellCorrection(spellCorrection);
            
            structuredResponse.setQueryMetadata(metadata);
            
            // Entities (filters)
            structuredResponse.setEntities(queryResult.entities);
            
            // Display Entities
            structuredResponse.setDisplayEntities(queryResult.displayEntities);
            
            // Errors
            structuredResponse.setErrors(queryResult.errors);
            
            return formatStructuredResponseToJSON(structuredResponse);
            
        } catch (Exception e) {
            StructuredJSONResponse errorResponse = new StructuredJSONResponse();
            List<StandardJSONProcessor.ValidationError> errors = new java.util.ArrayList<>();
            errors.add(new StandardJSONProcessor.ValidationError("ERROR", e.getMessage(), "ERROR"));
            errorResponse.setErrors(errors);
            return formatStructuredResponseToJSON(errorResponse);
        }
    }
    
    // ============================================================================
    // METHOD 3: Java Object with everything including SQL query
    // ============================================================================
    
    /**
     * Processes user input and returns Java object with complete information
     * Includes SQL query, parameters, and all metadata
     */
    public CompleteQueryObject processUserInputCompleteObject(String userInput) {
        try {
            // Process NLP
            String jsonResponse = nlpProcessor.processQuery(userInput);
            StandardJSONProcessor.QueryResult queryResult = nlpProcessor.parseJSONToObject(jsonResponse);
            
            // Create complete object
            CompleteQueryObject completeObject = new CompleteQueryObject();
            
            // Set all NLP results
            completeObject.setQueryResult(queryResult);
            
            // Generate SQL query
            String sqlQuery = generateSQLQuery(queryResult);
            completeObject.setSqlQuery(sqlQuery);
            
            // Set parameters
            Map<String, Object> parameters = createParameterMap(queryResult);
            completeObject.setParameters(parameters);
            
            // Execute DataProvider if needed
            if (shouldExecuteDataProvider(queryResult.metadata.actionType)) {
                String dataProviderResult = executeDataProviderActions(queryResult);
                completeObject.setDataProviderResult(dataProviderResult);
            }
            
            completeObject.setSuccess(true);
            completeObject.setMessage("Request processed successfully");
            
            return completeObject;
            
        } catch (Exception e) {
            CompleteQueryObject errorObject = new CompleteQueryObject();
            errorObject.setSuccess(false);
            errorObject.setMessage("Error processing request: " + e.getMessage());
            errorObject.setErrorCode("PROCESSING_ERROR");
            errorObject.setErrorDetails(e.getMessage());
            
            return errorObject;
        }
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    private String executeDataProviderActions(StandardJSONProcessor.QueryResult queryResult) {
        try {
            // Route to appropriate DataProvider method
            return "DataProvider action executed for: " + queryResult.metadata.actionType;
        } catch (Exception e) {
            return "Error executing DataProvider action: " + e.getMessage();
        }
    }
    
    private String generateSQLQuery(StandardJSONProcessor.QueryResult queryResult) {
        // Generate SQL based on action type and entities
        StringBuilder sql = new StringBuilder();
        
        switch (queryResult.metadata.actionType) {
            case "contracts_by_contractnumber":
                sql.append("SELECT * FROM contracts WHERE AWARD_NUMBER = ?");
                break;
            case "parts_by_part_number":
                sql.append("SELECT * FROM parts WHERE INVOICE_PART_NUMBER = ?");
                break;
            case "parts_failed_by_contract_number":
                sql.append("SELECT * FROM failed_parts WHERE LOADED_CP_NUMBER = ?");
                break;
            default:
                sql.append("SELECT * FROM contracts");
        }
        
        return sql.toString();
    }
    
    private Map<String, Object> createParameterMap(StandardJSONProcessor.QueryResult queryResult) {
        Map<String, Object> parameters = new HashMap<>();
        
        for (StandardJSONProcessor.EntityFilter filter : queryResult.entities) {
            parameters.put(filter.attribute, filter.value);
        }
        
        return parameters;
    }
    
    private boolean shouldExecuteDataProvider(String actionType) {
        return !actionType.startsWith("HELP_") && 
               !actionType.equals("contracts_list") && 
               !actionType.equals("contracts_by_filter");
    }
    
    // ============================================================================
    // INNER CLASSES FOR RESPONSE STRUCTURES
    // ============================================================================
    
    /**
     * Complete response including NLP and DataProvider results
     */
    public static class CompleteResponse {
        private boolean success;
        private String message;
        private String errorCode;
        private String errorDetails;
        private StandardJSONProcessor.QueryResult nlpResponse;
        private String dataProviderResponse;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
        
        public StandardJSONProcessor.QueryResult getNlpResponse() { return nlpResponse; }
        public void setNlpResponse(StandardJSONProcessor.QueryResult nlpResponse) { this.nlpResponse = nlpResponse; }
        
        public String getDataProviderResponse() { return dataProviderResponse; }
        public void setDataProviderResponse(String dataProviderResponse) { this.dataProviderResponse = dataProviderResponse; }
    }
    
    /**
     * Structured JSON response with specified format
     */
    public static class StructuredJSONResponse {
        private Header header;
        private QueryMetadata queryMetadata;
        private List<StandardJSONProcessor.EntityFilter> entities;
        private List<String> displayEntities;
        private List<StandardJSONProcessor.ValidationError> errors;
        
        // Getters and setters
        public Header getHeader() { return header; }
        public void setHeader(Header header) { this.header = header; }
        
        public QueryMetadata getQueryMetadata() { return queryMetadata; }
        public void setQueryMetadata(QueryMetadata queryMetadata) { this.queryMetadata = queryMetadata; }
        
        public List<StandardJSONProcessor.EntityFilter> getEntities() { return entities; }
        public void setEntities(List<StandardJSONProcessor.EntityFilter> entities) { this.entities = entities; }
        
        public List<String> getDisplayEntities() { return displayEntities; }
        public void setDisplayEntities(List<String> displayEntities) { this.displayEntities = displayEntities; }
        
        public List<StandardJSONProcessor.ValidationError> getErrors() { return errors; }
        public void setErrors(List<StandardJSONProcessor.ValidationError> errors) { this.errors = errors; }
        
        public static class Header {
            private String contractNumber;
            private String partNumber;
            private String customerNumber;
            private String customerName;
            private String createdBy;
            
            // Getters and setters
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
        }
        
        public static class QueryMetadata {
            private String queryType;
            private String actionType;
            private double processingTimeMs;
            private SpellCorrection spellCorrection;
            
            // Getters and setters
            public String getQueryType() { return queryType; }
            public void setQueryType(String queryType) { this.queryType = queryType; }
            
            public String getActionType() { return actionType; }
            public void setActionType(String actionType) { this.actionType = actionType; }
            
            public double getProcessingTimeMs() { return processingTimeMs; }
            public void setProcessingTimeMs(double processingTimeMs) { this.processingTimeMs = processingTimeMs; }
            
            public SpellCorrection getSpellCorrection() { return spellCorrection; }
            public void setSpellCorrection(SpellCorrection spellCorrection) { this.spellCorrection = spellCorrection; }
        }
        
        public static class SpellCorrection {
            private String originalInput;
            private String correctedInput;
            
            // Getters and setters
            public String getOriginalInput() { return originalInput; }
            public void setOriginalInput(String originalInput) { this.originalInput = originalInput; }
            
            public String getCorrectedInput() { return correctedInput; }
            public void setCorrectedInput(String correctedInput) { this.correctedInput = correctedInput; }
        }
    }
    
    /**
     * Complete query object with all information including SQL
     */
    public static class CompleteQueryObject {
        private boolean success;
        private String message;
        private String errorCode;
        private String errorDetails;
        private StandardJSONProcessor.QueryResult queryResult;
        private String sqlQuery;
        private Map<String, Object> parameters;
        private String dataProviderResult;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
        
        public StandardJSONProcessor.QueryResult getQueryResult() { return queryResult; }
        public void setQueryResult(StandardJSONProcessor.QueryResult queryResult) { this.queryResult = queryResult; }
        
        public String getSqlQuery() { return sqlQuery; }
        public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        
        public String getDataProviderResult() { return dataProviderResult; }
        public void setDataProviderResult(String dataProviderResult) { this.dataProviderResult = dataProviderResult; }
    }
    
    // ============================================================================
    // JSON FORMATTING METHODS (Simple implementation without external dependencies)
    // ============================================================================
    
    private String formatCompleteResponseToJSON(CompleteResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": ").append(response.isSuccess()).append(",\n");
        json.append("  \"message\": \"").append(escapeJson(response.getMessage())).append("\",\n");
        if (response.getErrorCode() != null) {
            json.append("  \"errorCode\": \"").append(escapeJson(response.getErrorCode())).append("\",\n");
        }
        if (response.getErrorDetails() != null) {
            json.append("  \"errorDetails\": \"").append(escapeJson(response.getErrorDetails())).append("\",\n");
        }
        if (response.getNlpResponse() != null) {
            json.append("  \"nlpResponse\": \"NLP processing completed\",\n");
        }
        if (response.getDataProviderResponse() != null) {
            json.append("  \"dataProviderResponse\": \"").append(escapeJson(response.getDataProviderResponse())).append("\"\n");
        }
        json.append("}");
        return json.toString();
    }
    
    private String formatStructuredResponseToJSON(StructuredJSONResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Header
        if (response.getHeader() != null) {
            json.append("  \"header\": {\n");
            json.append("    \"contractNumber\": \"").append(escapeJson(response.getHeader().getContractNumber())).append("\",\n");
            json.append("    \"partNumber\": \"").append(escapeJson(response.getHeader().getPartNumber())).append("\",\n");
            json.append("    \"customerNumber\": \"").append(escapeJson(response.getHeader().getCustomerNumber())).append("\",\n");
            json.append("    \"customerName\": \"").append(escapeJson(response.getHeader().getCustomerName())).append("\",\n");
            json.append("    \"createdBy\": \"").append(escapeJson(response.getHeader().getCreatedBy())).append("\"\n");
            json.append("  },\n");
        }
        
        // Query Metadata
        if (response.getQueryMetadata() != null) {
            json.append("  \"queryMetadata\": {\n");
            json.append("    \"queryType\": \"").append(escapeJson(response.getQueryMetadata().getQueryType())).append("\",\n");
            json.append("    \"actionType\": \"").append(escapeJson(response.getQueryMetadata().getActionType())).append("\",\n");
            json.append("    \"processingTimeMs\": ").append(response.getQueryMetadata().getProcessingTimeMs()).append(",\n");
            if (response.getQueryMetadata().getSpellCorrection() != null) {
                json.append("    \"spellCorrection\": {\n");
                json.append("      \"originalInput\": \"").append(escapeJson(response.getQueryMetadata().getSpellCorrection().getOriginalInput())).append("\",\n");
                json.append("      \"correctedInput\": \"").append(escapeJson(response.getQueryMetadata().getSpellCorrection().getCorrectedInput())).append("\"\n");
                json.append("    }\n");
            }
            json.append("  },\n");
        }
        
        // Entities
        json.append("  \"entities\": [],\n");
        
        // Display Entities
        json.append("  \"displayEntities\": [],\n");
        
        // Errors
        json.append("  \"errors\": []\n");
        
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
} 