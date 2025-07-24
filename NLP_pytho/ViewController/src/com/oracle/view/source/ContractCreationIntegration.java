package com.oracle.view.source;

import java.util.Map;

/**
 * Contract Creation Integration
 * 
 * Integrates the ContractCreationWorkflowManager with the existing NLP system
 * and handles the complete contract creation workflow from start to finish.
 */
public class ContractCreationIntegration {
    
    private final ContractCreationWorkflowManager workflowManager;
    private NLPUserActionHandler nlpHandler; // Remove final, make lazy
    private final TableColumnConfig tableConfig;
    
    public ContractCreationIntegration() {
        this.workflowManager = ContractCreationWorkflowManager.getInstance();
        // this.nlpHandler = new NLPUserActionHandler(); // REMOVE this line
        this.tableConfig = TableColumnConfig.getInstance();
    }
    
    /**
     * Main entry point for processing contract creation requests
     */
    public String processContractCreationInput(String userInput, String sessionId) {
        try {
            if (nlpHandler == null) {
                nlpHandler = NLPUserActionHandler.getInstance();
            }
            System.out.println("DEBUG: Processing contract creation input: " + userInput);
            System.out.println("DEBUG: Session ID: " + sessionId);
            
            // Process through workflow manager
            ContractCreationWorkflowManager.WorkflowResult result = 
                workflowManager.processContractCreationInput(userInput, sessionId);
            
            if (result.getError() != null) {
                return createErrorResponse(userInput, result.getError());
            }
            
            if (result.isChainBreak()) {
                return createChainBreakResponse(userInput, result.getMessage());
            }
            
            if (result.requiresInput()) {
                // This is a follow-up request - return the prompt
                return createFollowUpResponse(userInput, result.getMessage(), 
                    result.getNextStep(), sessionId);
            }
            
            if (result.isComplete()) {
                // Contract creation data is complete - process the actual creation
                return processContractCreation(userInput, result.getSession());
            }
            
            // Fallback
            return createErrorResponse(userInput, "Unexpected workflow state");
            
        } catch (Exception e) {
            return createErrorResponse(userInput, "Error processing contract creation: " + e.getMessage());
        }
    }
    
    /**
     * Process the actual contract creation with collected data
     */
    private String processContractCreation(String userInput, ContractCreationWorkflowManager.ContractCreationSession session) {
        try {
            if (nlpHandler == null) {
                nlpHandler = NLPUserActionHandler.getInstance();
            }
            System.out.println("DEBUG: Processing contract creation with collected data");
            System.out.println("DEBUG: Collected data: " + session.getCollectedData());
            
            // Build the complete contract creation query
            String mergedQuery = buildContractCreationQuery(session);
            System.out.println("DEBUG: Merged query: " + mergedQuery);
            
            // Process through NLP handler
            String result = nlpHandler.processUserInputJSONResponse(mergedQuery, session.getSessionId());
            
            // Clear the session after successful processing
            workflowManager.clearSession(session.getSessionId());
            
            return result;
            
        } catch (Exception e) {
            return createErrorResponse(userInput, "Error creating contract: " + e.getMessage());
        }
    }
    
    /**
     * Build the complete contract creation query from collected data
     */
    private String buildContractCreationQuery(ContractCreationWorkflowManager.ContractCreationSession session) {
        Map<String, String> collectedData = session.getCollectedData();
        
        StringBuilder query = new StringBuilder();
        query.append("create contract");
        
        // Add customer number
        String customerNumber = collectedData.get("CUSTOMER_NUMBER");
        if (customerNumber != null) {
            query.append(" for customer ").append(customerNumber);
        }
        
        // Add contract name
        String contractName = collectedData.get("CONTRACT_NAME");
        if (contractName != null) {
            query.append(" named ").append(contractName);
        }
        
        // Add title
        String title = collectedData.get("TITLE");
        if (title != null) {
            query.append(" with title ").append(title);
        }
        
        // Add description
        String description = collectedData.get("DESCRIPTION");
        if (description != null) {
            query.append(" description ").append(description);
        }
        
        // Add effective date
        String effectiveDate = collectedData.get("EFFECTIVE_DATE");
        if (effectiveDate != null) {
            query.append(" effective ").append(effectiveDate);
        }
        
        // Add expiration date
        String expirationDate = collectedData.get("EXPIRATION_DATE");
        if (expirationDate != null) {
            query.append(" expires ").append(expirationDate);
        }
        
        return query.toString();
    }
    
    /**
     * Create follow-up response JSON for contract creation workflow
     */
    private String createFollowUpResponse(String userInput, String message, String nextStep, String sessionId) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Header section
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": null,\n");
        json.append("    \"partNumber\": null,\n");
        json.append("    \"customerNumber\": null,\n");
        json.append("    \"customerName\": null,\n");
        json.append("    \"createdBy\": null,\n");
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": \"").append(escapeJson(userInput)).append("\",\n");
        json.append("      \"correctedInput\": null,\n");
        json.append("      \"correctionConfidence\": 1.0\n");
        json.append("    }\n");
        json.append("  },\n");
        
        // Query metadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": \"HELP\",\n");
        json.append("    \"actionType\": \"HELP_CONTRACT_CREATE_BOT\",\n");
        json.append("    \"processingTimeMs\": 25,\n");
        json.append("    \"selectedModule\": \"CONTRACT_CREATION\",\n");
        json.append("    \"routingConfidence\": 0.95\n");
        json.append("  },\n");
        
        // Entities section
        json.append("  \"entities\": [],\n");
        
        // Display entities section
        json.append("  \"displayEntities\": [],\n");
        
        // Module specific data
        json.append("  \"moduleSpecificData\": {\n");
        json.append("    \"requiresFollowUp\": true,\n");
        json.append("    \"workflowType\": \"CONTRACT_CREATION\",\n");
        json.append("    \"nextStep\": \"").append(nextStep).append("\",\n");
        json.append("    \"sessionId\": \"").append(sessionId).append("\"\n");
        json.append("  },\n");
        
        // Errors section
        json.append("  \"errors\": [],\n");
        
        // Confidence
        json.append("  \"confidence\": 0.95,\n");
        
        // Processing result
        json.append("  \"processingResult\": \"").append(escapeJson(message)).append("\"\n");
        
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Create chain break response JSON
     */
    private String createChainBreakResponse(String userInput, String message) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Header section
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": null,\n");
        json.append("    \"partNumber\": null,\n");
        json.append("    \"customerNumber\": null,\n");
        json.append("    \"customerName\": null,\n");
        json.append("    \"createdBy\": null,\n");
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": \"").append(escapeJson(userInput)).append("\",\n");
        json.append("      \"correctedInput\": null,\n");
        json.append("      \"correctionConfidence\": 1.0\n");
        json.append("    }\n");
        json.append("  },\n");
        
        // Query metadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": \"INFO\",\n");
        json.append("    \"actionType\": \"system_message\",\n");
        json.append("    \"processingTimeMs\": 10,\n");
        json.append("    \"selectedModule\": \"SYSTEM\",\n");
        json.append("    \"routingConfidence\": 1.0\n");
        json.append("  },\n");
        
        // Entities section
        json.append("  \"entities\": [],\n");
        
        // Display entities section
        json.append("  \"displayEntities\": [],\n");
        
        // Module specific data
        json.append("  \"moduleSpecificData\": {\n");
        json.append("    \"requiresFollowUp\": false,\n");
        json.append("    \"workflowType\": null,\n");
        json.append("    \"sessionId\": null\n");
        json.append("  },\n");
        
        // Errors section
        json.append("  \"errors\": [],\n");
        
        // Confidence
        json.append("  \"confidence\": 1.0,\n");
        
        // Processing result
        json.append("  \"processingResult\": \"").append(escapeJson(message)).append("\"\n");
        
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Create error response JSON
     */
    private String createErrorResponse(String userInput, String errorMessage) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Header section
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": null,\n");
        json.append("    \"partNumber\": null,\n");
        json.append("    \"customerNumber\": null,\n");
        json.append("    \"customerName\": null,\n");
        json.append("    \"createdBy\": null,\n");
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": \"").append(escapeJson(userInput)).append("\",\n");
        json.append("      \"correctedInput\": null,\n");
        json.append("      \"correctionConfidence\": 1.0\n");
        json.append("    }\n");
        json.append("  },\n");
        
        // Query metadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": \"ERROR\",\n");
        json.append("    \"actionType\": \"ERROR\",\n");
        json.append("    \"processingTimeMs\": 0,\n");
        json.append("    \"selectedModule\": \"ERROR\",\n");
        json.append("    \"routingConfidence\": 0.0\n");
        json.append("  },\n");
        
        // Entities section
        json.append("  \"entities\": [],\n");
        
        // Display entities section
        json.append("  \"displayEntities\": [],\n");
        
        // Module specific data
        json.append("  \"moduleSpecificData\": {\n");
        json.append("    \"requiresFollowUp\": false,\n");
        json.append("    \"workflowType\": null,\n");
        json.append("    \"sessionId\": null\n");
        json.append("  },\n");
        
        // Errors section
        json.append("  \"errors\": [\"").append(escapeJson(errorMessage)).append("\"],\n");
        
        // Confidence
        json.append("  \"confidence\": 0.0,\n");
        
        // Processing result
        json.append("  \"processingResult\": \"Error occurred during contract creation\"\n");
        
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Check if input is related to contract creation
     */
    public boolean isContractCreationRelated(String userInput) {
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("create") && lowerInput.contains("contract") ||
               lowerInput.contains("new contract") ||
               lowerInput.contains("start contract") ||
               lowerInput.matches(".*\\b(create|new|start)\\b.*\\bcontract\\b.*");
    }
    
    /**
     * Get session information for debugging
     */
    public String getSessionInfo(String sessionId) {
        return workflowManager.getSessionInfo(sessionId);
    }
    
    /**
     * Clear session (for testing or user cancellation)
     */
    public void clearSession(String sessionId) {
        workflowManager.clearSession(sessionId);
    }
    
    /**
     * Get all active contract creation sessions (for monitoring)
     */
    public Map<String, ContractCreationWorkflowManager.ContractCreationSession> getActiveSessions() {
        return workflowManager.getActiveSessions();
    }
    
    /**
     * Utility method to escape JSON strings
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
} 