package com.oracle.view;

import java.util.*;

/**
 * Simple NLP Integration for Oracle ADF
 * Processes String inputs and returns JSON responses
 * Works with Test.java test cases
 */
public class SimpleNLPIntegration {
    
    private EnhancedNLPEngine nlpEngine;
    
    public SimpleNLPIntegration() {
        try {
            nlpEngine = new EnhancedNLPEngine();
            // MockNLPEngine doesn't have initialize() method, it's ready to use
            System.out.println("NLP Engine initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize NLP Engine: " + e.getMessage());
            throw new RuntimeException("NLP initialization failed", e);
        }
    }
    
    /**
     * Process array of string inputs
     * 
     * @param inputs Array of user queries
     * @return Array of JSON responses
     */
    public String[] processInputs(String[] inputs) {
        if (inputs == null || inputs.length == 0) {
            return new String[]{createErrorJson("No inputs provided")};
        }
        
        String[] results = new String[inputs.length];
        
        for (int i = 0; i < inputs.length; i++) {
            results[i] = processInput(inputs[i]);
        }
        
        return results;
    }
    
    /**
     * Process single string input and return JSON
     * 
     * @param input User query
     * @return JSON string response
     */
    public String processInput(String input) {
        try {
            long startTime = System.currentTimeMillis();
            NLPResponse response = nlpEngine.processQuery(input);
            long processingTime = System.currentTimeMillis() - startTime;
            
            return convertToJson(response, processingTime);
        } catch (Exception e) {
            return createErrorJson("Error processing '" + input + "': " + e.getMessage());
        }
    }
    
    /**
     * Process single string input and return Java object
     * 
     * @param input User query
     * @return NLPJsonResponse Java object
     */
    public NLPJsonResponse processInputAsObject(String input) {
        try {
            long startTime = System.currentTimeMillis();
            NLPResponse response = nlpEngine.processQuery(input);
            long processingTime = System.currentTimeMillis() - startTime;
            
            return convertToObject(response, processingTime);
        } catch (Exception e) {
            return createErrorObject("Error processing '" + input + "': " + e.getMessage());
        }
    }
    
    /**
     * Convert NLPResponse to JSON string
     */
    private String convertToJson(NLPResponse response, long processingTime) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Header section
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": ").append(formatJsonValue(getContractNumber(response))).append(",\n");
        json.append("    \"partNumber\": ").append(formatJsonValue(getPartNumber(response))).append(",\n");
        json.append("    \"customerNumber\": ").append(formatJsonValue(getCustomerNumber(response))).append(",\n");
        json.append("    \"customerName\": ").append(formatJsonValue(getCustomerName(response))).append(",\n");
        json.append("    \"createdBy\": ").append(formatJsonValue(getCreatedBy(response))).append(",\n");
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": ").append(formatJsonValue(response.getOriginalInput())).append(",\n");
        json.append("      \"correctedInput\": ").append(formatJsonValue(getCorrectedInput(response))).append(",\n");
        json.append("      \"correctionConfidence\": ").append(response.getCorrectionConfidence()).append("\n");
        json.append("    }\n");
        json.append("  },\n");
        
        // Query metadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": ").append(formatJsonValue(response.getQueryType())).append(",\n");
        json.append("    \"actionType\": ").append(formatJsonValue(response.getActionType())).append(",\n");
        json.append("    \"processingTimeMs\": ").append(processingTime).append("\n");
        json.append("  },\n");
        
        // Entities section
        json.append("  \"entities\": [\n");
        List<EntityFilter> entities = buildEntityFilters(response);
        for (int i = 0; i < entities.size(); i++) {
            EntityFilter entity = entities.get(i);
            json.append("    {\n");
            json.append("      \"attribute\": ").append(formatJsonValue(entity.getAttribute())).append(",\n");
            json.append("      \"operation\": ").append(formatJsonValue(entity.getOperation())).append(",\n");
            json.append("      \"value\": ").append(formatJsonValue(entity.getValue())).append(",\n");
            json.append("      \"source\": ").append(formatJsonValue(entity.getSource())).append("\n");
            json.append("    }");
            if (i < entities.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Display entities section
        json.append("  \"displayEntities\": [\n");
        List<String> displayEntities = buildDisplayEntities(response);
        for (int i = 0; i < displayEntities.size(); i++) {
            json.append("    ").append(formatJsonValue(displayEntities.get(i)));
            if (i < displayEntities.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Errors section
        json.append("  \"errors\": [\n");
        List<ErrorInfo> errors = buildErrors(response);
        for (int i = 0; i < errors.size(); i++) {
            ErrorInfo error = errors.get(i);
            json.append("    {\n");
            json.append("      \"code\": ").append(formatJsonValue(error.getCode())).append(",\n");
            json.append("      \"message\": ").append(formatJsonValue(error.getMessage())).append(",\n");
            json.append("      \"severity\": ").append(formatJsonValue(error.getSeverity())).append("\n");
            json.append("    }");
            if (i < errors.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Convert NLPResponse to Java object
     */
    private NLPJsonResponse convertToObject(NLPResponse response, long processingTime) {
        NLPJsonResponse jsonResponse = new NLPJsonResponse();
        
        // Header
        NLPJsonResponse.Header header = new NLPJsonResponse.Header();
        header.setContractNumber(getContractNumber(response));
        header.setPartNumber(getPartNumber(response));
        header.setCustomerNumber(getCustomerNumber(response));
        header.setCustomerName(getCustomerName(response));
        header.setCreatedBy(getCreatedBy(response));
        
        NLPJsonResponse.InputTracking inputTracking = new NLPJsonResponse.InputTracking();
        inputTracking.setOriginalInput(response.getOriginalInput());
        inputTracking.setCorrectedInput(getCorrectedInput(response));
        inputTracking.setCorrectionConfidence(response.getCorrectionConfidence());
        header.setInputTracking(inputTracking);
        
        jsonResponse.setHeader(header);
        
        // Query metadata
        NLPJsonResponse.QueryMetadata queryMetadata = new NLPJsonResponse.QueryMetadata();
        queryMetadata.setQueryType(response.getQueryType());
        queryMetadata.setActionType(response.getActionType());
        queryMetadata.setProcessingTimeMs(processingTime);
        jsonResponse.setQueryMetadata(queryMetadata);
        
        // Entities and display entities
        jsonResponse.setEntities(buildEntityFilters(response));
        jsonResponse.setDisplayEntities(buildDisplayEntities(response));
        jsonResponse.setErrors(buildErrors(response));
        
        return jsonResponse;
    }
    
    /**
     * Build entity filters based on response with enhanced temporal handling
     */
    private List<EntityFilter> buildEntityFilters(NLPResponse response) {
        List<EntityFilter> entities = new ArrayList<>();
        
        // Add contract number filter if present (only if it's not a year in date context)
        String contractNumber = getContractNumber(response);
        if (contractNumber != null && !isYearInDateContext(contractNumber, response.getOriginalInput())) {
            entities.add(new EntityFilter("CONTRACT_NUMBER", "=", contractNumber, "user_input"));
        }
        
        // Add part number filter if present
        String partNumber = getPartNumber(response);
        if (partNumber != null) {
            entities.add(new EntityFilter("PART_NUMBER", "=", partNumber, "user_input"));
        }
        
        // Add customer number filter if present
        String customerNumber = getCustomerNumber(response);
        if (customerNumber != null) {
            entities.add(new EntityFilter("CUSTOMER_NUMBER", "=", customerNumber, "user_input"));
        }
        
        // Add customer name filter if present
        String customerName = getCustomerName(response);
        if (customerName != null) {
            entities.add(new EntityFilter("CUSTOMER_NAME", "=", customerName, "user_input"));
        }
        
        // Add created by filter if present
        String createdBy = getCreatedBy(response);
        if (createdBy != null) {
            entities.add(new EntityFilter("CREATED_BY", "=", createdBy, "user_input"));
        }
        
        // Enhanced date filters based on temporal context
        addTemporalFilters(response, entities);
        
        // Add status filters
        addStatusFilters(response, entities);
        
        return entities;
    }
    
    /**
     * Check if a number is a year in date context
     */
    private boolean isYearInDateContext(String number, String input) {
        if (!number.matches("\\b(19|20)\\d{2}\\b")) {
            return false; // Not a year format
        }
        
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("after") || lowerInput.contains("before") || 
               lowerInput.contains("since") || lowerInput.contains("in " + number) ||
               lowerInput.contains("created") || lowerInput.contains("jan") ||
               lowerInput.contains("feb") || lowerInput.contains("mar") ||
               lowerInput.contains("apr") || lowerInput.contains("may") ||
               lowerInput.contains("jun") || lowerInput.contains("jul") ||
               lowerInput.contains("aug") || lowerInput.contains("sep") ||
               lowerInput.contains("oct") || lowerInput.contains("nov") ||
               lowerInput.contains("dec");
    }
    
    /**
     * Add temporal filters based on date context
     */
    private void addTemporalFilters(NLPResponse response, List<EntityFilter> entities) {
        String inputToAnalyze = getCorrectedInput(response);
        if (inputToAnalyze == null) {
            inputToAnalyze = response.getOriginalInput();
        }
        
        if (inputToAnalyze != null) {
            String input = inputToAnalyze.toLowerCase();
            
            // Extract year from input for temporal operations
            java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b");
            java.util.regex.Matcher yearMatcher = yearPattern.matcher(input);
            
            if (yearMatcher.find()) {
                String year = yearMatcher.group();
                
                if (input.contains("after") || input.contains("since")) {
                    entities.add(new EntityFilter("EFFECTIVE_DATE", ">=", year + "-01-01", "inferred"));
                } else if (input.contains("before") || input.contains("until")) {
                    entities.add(new EntityFilter("EFFECTIVE_DATE", "<", year + "-01-01", "inferred"));
                } else if (input.contains("in") || input.contains("created")) {
                    entities.add(new EntityFilter("EFFECTIVE_DATE", ">=", year + "-01-01", "inferred"));
                    entities.add(new EntityFilter("EFFECTIVE_DATE", "<=", year + "-12-31", "inferred"));
                }
            } else if (input.contains("after") || input.contains("before") || input.contains("between")) {
                // Generic date filter when no specific year is mentioned
                entities.add(new EntityFilter("EFFECTIVE_DATE", ">=", "2020-01-01", "inferred"));
            }
        }
    }
    
    /**
     * Add status filters
     */
    private void addStatusFilters(NLPResponse response, List<EntityFilter> entities) {
        String inputToAnalyze = getCorrectedInput(response);
        if (inputToAnalyze == null) {
            inputToAnalyze = response.getOriginalInput();
        }
        
        if (inputToAnalyze != null) {
            String input = inputToAnalyze.toLowerCase();
            
            if (input.contains("expired")) {
                entities.add(new EntityFilter("STATUS", "=", "EXPIRED", "inferred"));
            }
            if (input.contains("active")) {
                entities.add(new EntityFilter("STATUS", "=", "ACTIVE", "inferred"));
            }
        }
    }
    
    /**
     * Build display entities based on response
     */
    private List<String> buildDisplayEntities(NLPResponse response) {
        List<String> displayEntities = new ArrayList<>();
        
        String queryType = response.getQueryType();
        
        if ("CONTRACTS".equals(queryType)) {
            displayEntities.add("CONTRACT_NUMBER");
            displayEntities.add("CUSTOMER_NAME");
            displayEntities.add("EFFECTIVE_DATE");
            displayEntities.add("STATUS");
            displayEntities.add("CREATED_BY");
            
            // Add specific fields based on query content
            String inputToAnalyze = getCorrectedInput(response);
            if (inputToAnalyze == null) {
                inputToAnalyze = response.getOriginalInput();
            }
            if (inputToAnalyze != null) {
                String input = inputToAnalyze.toLowerCase();
                if (input.contains("price") || input.contains("pricing")) {
                    displayEntities.add("PRICE_LIST");
                }
                if (input.contains("project")) {
                    displayEntities.add("PROJECT_TYPE");
                }
                if (input.contains("metadata")) {
                    displayEntities.add("ALL_FIELDS");
                }
            }
        } else if ("PARTS".equals(queryType)) {
            displayEntities.add("PART_NUMBER");
            displayEntities.add("CONTRACT_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
            displayEntities.add("STATUS");
            
            // Add specific fields based on query content
            String inputToAnalyze = getCorrectedInput(response);
            if (inputToAnalyze == null) {
                inputToAnalyze = response.getOriginalInput();
            }
            if (inputToAnalyze != null) {
                String input = inputToAnalyze.toLowerCase();
                if (input.contains("specification") || input.contains("specs")) {
                    displayEntities.add("SPECIFICATIONS");
                }
                if (input.contains("price") || input.contains("cost")) {
                    displayEntities.add("PRICE");
                }
                if (input.contains("manufacturer")) {
                    displayEntities.add("MANUFACTURER");
                }
            }
        } else if ("HELP".equals(queryType)) {
            displayEntities.add("HELP_CONTENT");
            displayEntities.add("STEPS");
            displayEntities.add("GUIDE_TYPE");
        }
        
        return displayEntities;
    }
    
    /**
     * Build errors list
     */
    private List<ErrorInfo> buildErrors(NLPResponse response) {
        List<ErrorInfo> errors = new ArrayList<>();
        
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            for (String errorMsg : response.getErrors()) {
                errors.add(new ErrorInfo("PROCESSING_ERROR", errorMsg, "BLOCKER"));
            }
        }
        
        // Add validation warnings
        if (response.getConfidence() < 0.7) {
            errors.add(new ErrorInfo("LOW_CONFIDENCE", 
                "Query confidence is low (" + Math.round(response.getConfidence() * 100) + "%)", 
                "WARNING"));
        }
        
        return errors;
    }
    
    // Helper methods to extract values from response
    private String getContractNumber(NLPResponse response) {
        return response.getHeader() != null ? response.getHeader().getContractNumber() : null;
    }
    
    private String getPartNumber(NLPResponse response) {
        return response.getHeader() != null ? response.getHeader().getPartNumber() : null;
    }
    
    private String getCustomerNumber(NLPResponse response) {
        return response.getHeader() != null ? response.getHeader().getCustomerNumber() : null;
    }
    
    private String getCustomerName(NLPResponse response) {
        return response.getHeader() != null ? response.getHeader().getCustomerName() : null;
    }
    
    private String getCreatedBy(NLPResponse response) {
        return response.getHeader() != null ? response.getHeader().getCreatedBy() : null;
    }
    
    private String getCorrectedInput(NLPResponse response) {
        String corrected = response.getCorrectedInput();
        String original = response.getOriginalInput();
        
        // Only return corrected input if it's different from original
        if (corrected != null && original != null && !corrected.equals(original.toLowerCase())) {
            return corrected;
        }
        return null;
    }
    
    /**
     * Format value for JSON output
     */
    private String formatJsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }
    
    /**
     * Create error JSON response
     */
    private String createErrorJson(String errorMessage) {
        return "{\n" +
               "  \"header\": {\n" +
               "    \"contractNumber\": null,\n" +
               "    \"partNumber\": null,\n" +
               "    \"customerNumber\": null,\n" +
               "    \"customerName\": null,\n" +
               "    \"createdBy\": null,\n" +
               "    \"inputTracking\": {\n" +
               "      \"originalInput\": \"\",\n" +
               "      \"correctedInput\": null,\n" +
               "      \"correctionConfidence\": 0\n" +
               "    }\n" +
               "  },\n" +
               "  \"queryMetadata\": {\n" +
               "    \"queryType\": \"ERROR\",\n" +
               "    \"actionType\": \"error_handling\",\n" +
               "    \"processingTimeMs\": 0\n" +
               "  },\n" +
               "  \"entities\": [],\n" +
               "  \"displayEntities\": [],\n" +
               "  \"errors\": [\n" +
               "    {\n" +
               "      \"code\": \"SYSTEM_ERROR\",\n" +
               "      \"message\": \"" + errorMessage + "\",\n" +
               "      \"severity\": \"BLOCKER\"\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }
    
    /**
     * Create error object response
     */
    private NLPJsonResponse createErrorObject(String errorMessage) {
        NLPJsonResponse response = new NLPJsonResponse();
        
        // Header
        NLPJsonResponse.Header header = new NLPJsonResponse.Header();
        NLPJsonResponse.InputTracking inputTracking = new NLPJsonResponse.InputTracking();
        inputTracking.setOriginalInput("");
        inputTracking.setCorrectionConfidence(0.0);
        header.setInputTracking(inputTracking);
        response.setHeader(header);
        
        // Query metadata
        NLPJsonResponse.QueryMetadata queryMetadata = new NLPJsonResponse.QueryMetadata();
        queryMetadata.setQueryType("ERROR");
        queryMetadata.setActionType("error_handling");
        queryMetadata.setProcessingTimeMs(0L);
        response.setQueryMetadata(queryMetadata);
        
        // Empty lists
        response.setEntities(new ArrayList<>());
        response.setDisplayEntities(new ArrayList<>());
        
        // Error
        List<ErrorInfo> errors = new ArrayList<>();
        errors.add(new ErrorInfo("SYSTEM_ERROR", errorMessage, "BLOCKER"));
        response.setErrors(errors);
        
        return response;
    }
    
    /**
     * Check if NLP engine is healthy
     */
    public boolean isHealthy() {
        return nlpEngine != null && nlpEngine.isHealthy();
    }
    
    /**
     * Get available domains
     */
    public String[] getAvailableDomains() {
        if (nlpEngine != null) {
            return nlpEngine.getAvailableDomains().stream()
                    .map(DomainType::name)
                    .toArray(String[]::new);
        }
        return new String[0];
    }
    

}