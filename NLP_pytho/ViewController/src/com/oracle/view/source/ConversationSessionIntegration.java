//package com.oracle.view.source;
//
//import java.util.Map;
//
///**
// * Conversation Session Integration
// * 
// * Demonstrates how to integrate the ConversationSessionManager with the existing NLP system.
// * This class provides a clean interface for handling multi-turn conversations.
// */
//public class ConversationSessionIntegration {
//    
//    private final ConversationSessionManager sessionManager;
//    private final NLPUserActionHandler nlpHandler;
//    
//    public ConversationSessionIntegration() {
//        this.sessionManager = ConversationSessionManager.getInstance();
//        this.nlpHandler = NLPUserActionHandler.getInstance();
//    }
//    
//    /**
//     * Main entry point for processing user input
//     * This method handles both new requests and follow-up responses
//     */
//    public String processUserInput(String userInput, String sessionId) {
//        try {
//            System.out.println("DEBUG: Processing user input: " + userInput);
//            System.out.println("DEBUG: Session ID: " + sessionId);
//            
//            // Process through session manager first
//            ConversationSessionManager.ConversationResult result = 
//                sessionManager.processUserInput(userInput, sessionId);
//            
//            if (result.getError() != null) {
//                return createErrorResponse(userInput, result.getError());
//            }
//            
//            if (result.requiresFollowUp()) {
//                // This is a follow-up request - return the prompt
//                return createFollowUpResponse(userInput, result.getMessage(), 
//                    result.getExpectedResponseType(), sessionId);
//            }
//            
//            if (result.isComplete()) {
//                // This is a complete conversation or query
//                if (result.getConversation() != null) {
//                    // Process the complete conversation
//                    return processCompleteConversation(result.getConversation());
//                } else {
//                    // This was a complete query from the start
//                    return nlpHandler.processUserInputJSONResponse(userInput, sessionId);
//                }
//            }
//            
//            // Fallback
//            return nlpHandler.processUserInputJSONResponse(userInput, sessionId);
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error processing user input: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Process a complete conversation with all required information
//     */
//    private String processCompleteConversation(ConversationSessionManager.ConversationState conversation) {
//        try {
//            String conversationType = conversation.getConversationType();
//            String originalQuery = conversation.getOriginalQuery();
//            Map<String, String> collectedData = conversation.getCollectedData();
//            
//            System.out.println("DEBUG: Processing complete conversation");
//            System.out.println("DEBUG: Type: " + conversationType);
//            System.out.println("DEBUG: Original query: " + originalQuery);
//            System.out.println("DEBUG: Collected data: " + collectedData);
//            
//            switch (conversationType) {
//                case ConversationSessionManager.CONVERSATION_TYPE_PARTS_CONTRACT:
//                    return processPartsContractConversation(originalQuery, collectedData);
//                    
//                case ConversationSessionManager.CONVERSATION_TYPE_CUSTOMER_ACCOUNT:
//                    return processCustomerAccountConversation(originalQuery, collectedData);
//                    
//                case ConversationSessionManager.CONVERSATION_TYPE_CONTRACT_CREATION:
//                    return processContractCreationConversation(originalQuery, collectedData);
//                    
//                case ConversationSessionManager.CONVERSATION_TYPE_PART_SEARCH:
//                    return processPartSearchConversation(originalQuery, collectedData);
//                    
//                case ConversationSessionManager.CONVERSATION_TYPE_LEAD_TIME:
//                    return processLeadTimeConversation(originalQuery, collectedData);
//                    
//                default:
//                    return createErrorResponse(originalQuery, "Unknown conversation type: " + conversationType);
//            }
//            
//        } catch (Exception e) {
//            return createErrorResponse(conversation.getOriginalQuery(), 
//                "Error processing conversation: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Process parts contract conversation
//     * Example: "What is the lead time for part ABC?" + contract number "123456"
//     */
//    private String processPartsContractConversation(String originalQuery, Map<String, String> collectedData) {
//        String contractNumber = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_CONTRACT_NUMBER);
//        
//        // Merge the original query with the contract number
//        String mergedQuery = originalQuery + " contract " + contractNumber;
//        
//        System.out.println("DEBUG: Merged query: " + mergedQuery);
//        
//        // Process through NLP handler
//        return nlpHandler.processUserInputJSONResponse(mergedQuery, "session_" + System.currentTimeMillis());
//    }
//    
//    /**
//     * Process customer account conversation
//     * Example: "Show customer details" + account number "12345"
//     */
//    private String processCustomerAccountConversation(String originalQuery, Map<String, String> collectedData) {
//        String customerAccount = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_CUSTOMER_ACCOUNT);
//        
//        // Merge the original query with the customer account
//        String mergedQuery = originalQuery + " for account " + customerAccount;
//        
//        System.out.println("DEBUG: Merged query: " + mergedQuery);
//        
//        // Process through NLP handler
//        return nlpHandler.processUserInputJSONResponse(mergedQuery, "session_" + System.currentTimeMillis());
//    }
//    
//    /**
//     * Process contract creation conversation
//     * Example: "Create contract" + name "ABC Contract" + duration "12 months"
//     */
//    private String processContractCreationConversation(String originalQuery, Map<String, String> collectedData) {
//        String contractName = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_CONTRACT_NAME);
//        String contractDuration = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_CONTRACT_DURATION);
//        
//        // Build the complete contract creation query
//        String mergedQuery = originalQuery + " named " + contractName + " for " + contractDuration;
//        
//        System.out.println("DEBUG: Merged query: " + mergedQuery);
//        
//        // Process through NLP handler
//        return nlpHandler.processUserInputJSONResponse(mergedQuery, "session_" + System.currentTimeMillis());
//    }
//    
//    /**
//     * Process part search conversation
//     * Example: "Search for parts" + part number "ABC123"
//     */
//    private String processPartSearchConversation(String originalQuery, Map<String, String> collectedData) {
//        String partNumber = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_PART_NUMBER);
//        
//        // Merge the original query with the part number
//        String mergedQuery = originalQuery + " " + partNumber;
//        
//        System.out.println("DEBUG: Merged query: " + mergedQuery);
//        
//        // Process through NLP handler
//        return nlpHandler.processUserInputJSONResponse(mergedQuery, "session_" + System.currentTimeMillis());
//    }
//    
//    /**
//     * Process lead time conversation
//     * Example: "What is the lead time?" + part "ABC123" + contract "456789"
//     */
//    private String processLeadTimeConversation(String originalQuery, Map<String, String> collectedData) {
//        String partNumber = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_PART_NUMBER);
//        String contractNumber = collectedData.get(ConversationSessionManager.EXPECTED_RESPONSE_CONTRACT_NUMBER);
//        
//        // Build the complete lead time query
//        String mergedQuery = originalQuery + " for part " + partNumber + " contract " + contractNumber;
//        
//        System.out.println("DEBUG: Merged query: " + mergedQuery);
//        
//        // Process through NLP handler
//        return nlpHandler.processUserInputJSONResponse(mergedQuery, "session_" + System.currentTimeMillis());
//    }
//    
//    /**
//     * Create follow-up response JSON
//     */
//    private String createFollowUpResponse(String userInput, String message, String expectedResponseType, String sessionId) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        
//        // Header section
//        json.append("  \"header\": {\n");
//        json.append("    \"contractNumber\": null,\n");
//        json.append("    \"partNumber\": null,\n");
//        json.append("    \"customerNumber\": null,\n");
//        json.append("    \"customerName\": null,\n");
//        json.append("    \"createdBy\": null,\n");
//        json.append("    \"inputTracking\": {\n");
//        json.append("      \"originalInput\": \"").append(escapeJson(userInput)).append("\",\n");
//        json.append("      \"correctedInput\": null,\n");
//        json.append("      \"correctionConfidence\": 1.0\n");
//        json.append("    }\n");
//        json.append("  },\n");
//        
//        // Query metadata section
//        json.append("  \"queryMetadata\": {\n");
//        json.append("    \"queryType\": \"FOLLOW_UP_REQUEST\",\n");
//        json.append("    \"actionType\": \"request_missing_info\",\n");
//        json.append("    \"processingTimeMs\": 25,\n");
//        json.append("    \"selectedModule\": \"CONVERSATION\",\n");
//        json.append("    \"routingConfidence\": 0.90\n");
//        json.append("  },\n");
//        
//        // Entities section
//        json.append("  \"entities\": [],\n");
//        
//        // Display entities section
//        json.append("  \"displayEntities\": [],\n");
//        
//        // Module specific data
//        json.append("  \"moduleSpecificData\": {\n");
//        json.append("    \"requiresFollowUp\": true,\n");
//        json.append("    \"expectedResponseType\": \"").append(expectedResponseType).append("\",\n");
//        json.append("    \"sessionId\": \"").append(sessionId).append("\"\n");
//        json.append("  },\n");
//        
//        // Errors section
//        json.append("  \"errors\": [],\n");
//        
//        // Confidence
//        json.append("  \"confidence\": 0.90,\n");
//        
//        // Processing result
//        json.append("  \"processingResult\": \"").append(escapeJson(message)).append("\"\n");
//        
//        json.append("}");
//        
//        return json.toString();
//    }
//    
//    /**
//     * Create error response JSON
//     */
//    private String createErrorResponse(String userInput, String errorMessage) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        
//        // Header section
//        json.append("  \"header\": {\n");
//        json.append("    \"contractNumber\": null,\n");
//        json.append("    \"partNumber\": null,\n");
//        json.append("    \"customerNumber\": null,\n");
//        json.append("    \"customerName\": null,\n");
//        json.append("    \"createdBy\": null,\n");
//        json.append("    \"inputTracking\": {\n");
//        json.append("      \"originalInput\": \"").append(escapeJson(userInput)).append("\",\n");
//        json.append("      \"correctedInput\": null,\n");
//        json.append("      \"correctionConfidence\": 1.0\n");
//        json.append("    }\n");
//        json.append("  },\n");
//        
//        // Query metadata section
//        json.append("  \"queryMetadata\": {\n");
//        json.append("    \"queryType\": \"ERROR\",\n");
//        json.append("    \"actionType\": \"ERROR\",\n");
//        json.append("    \"processingTimeMs\": 0,\n");
//        json.append("    \"selectedModule\": \"ERROR\",\n");
//        json.append("    \"routingConfidence\": 0.0\n");
//        json.append("  },\n");
//        
//        // Entities section
//        json.append("  \"entities\": [],\n");
//        
//        // Display entities section
//        json.append("  \"displayEntities\": [],\n");
//        
//        // Module specific data
//        json.append("  \"moduleSpecificData\": {\n");
//        json.append("    \"requiresFollowUp\": false,\n");
//        json.append("    \"sessionId\": null\n");
//        json.append("  },\n");
//        
//        // Errors section
//        json.append("  \"errors\": [\"").append(escapeJson(errorMessage)).append("\"],\n");
//        
//        // Confidence
//        json.append("  \"confidence\": 0.0,\n");
//        
//        // Processing result
//        json.append("  \"processingResult\": \"Error occurred during processing\"\n");
//        
//        json.append("}");
//        
//        return json.toString();
//    }
//    
//    /**
//     * Utility method to escape JSON strings
//     */
//    private String escapeJson(String input) {
//        if (input == null) return "";
//        return input.replace("\\", "\\\\")
//                   .replace("\"", "\\\"")
//                   .replace("\n", "\\n")
//                   .replace("\r", "\\r")
//                   .replace("\t", "\\t");
//    }
//    
//    /**
//     * Get session information for debugging
//     */
//    public String getSessionInfo(String sessionId) {
//        ConversationSessionManager.UserSession session = sessionManager.getUserSession(sessionId);
//        if (session == null) {
//            return "Session not found: " + sessionId;
//        }
//        
//        StringBuilder info = new StringBuilder();
//        info.append("Session ID: ").append(session.getSessionId()).append("\n");
//        info.append("User ID: ").append(session.getUserId()).append("\n");
//        info.append("Created: ").append(session.getCreatedAt()).append("\n");
//        info.append("Last Activity: ").append(session.getLastActivity()).append("\n");
//        info.append("Message Count: ").append(session.getMessageHistory().size()).append("\n");
//        
//        if (session.getCurrentConversation() != null) {
//            ConversationSessionManager.ConversationState conv = session.getCurrentConversation();
//            info.append("Current Conversation:\n");
//            info.append("  ID: ").append(conv.getConversationId()).append("\n");
//            info.append("  Type: ").append(conv.getConversationType()).append("\n");
//            info.append("  Status: ").append(conv.getStatus()).append("\n");
//            info.append("  Expected Response: ").append(conv.getExpectedResponseType()).append("\n");
//            info.append("  Turn Count: ").append(conv.getTurnCount()).append("\n");
//            info.append("  Collected Data: ").append(conv.getCollectedData()).append("\n");
//        } else {
//            info.append("No active conversation\n");
//        }
//        
//        return info.toString();
//    }
//    
//    /**
//     * Clear session (for testing or user logout)
//     */
//    public void clearSession(String sessionId) {
//        ConversationSessionManager.UserSession session = sessionManager.getUserSession(sessionId);
//        if (session != null && session.getCurrentConversation() != null) {
//            sessionManager.cancelConversation(session.getCurrentConversation().getConversationId());
//        }
//    }
//} 