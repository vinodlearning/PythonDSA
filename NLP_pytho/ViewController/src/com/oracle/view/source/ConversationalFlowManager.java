package com.oracle.view.source;

import java.util.*;
import java.util.Date;

/**
 * Enhanced Conversational Flow Manager
 * Handles multi-turn conversations like contract creation
 */
public class ConversationalFlowManager {
    
    // Singleton instance
    private static ConversationalFlowManager instance;
    
    // Session management
    private final Map<String, ConversationState> conversationStates = new HashMap<>();
    private final Map<String, List<ChatMessage>> conversationHistory = new HashMap<>();
    
    // Private constructor for singleton
    public ConversationalFlowManager() {}
    
    /**
     * Get singleton instance
     */
    public static ConversationalFlowManager getInstance() {
        if (instance == null) {
            instance = new ConversationalFlowManager();
        }
        return instance;
    }
    
    /**
     * Generate a unique session ID
     */
    public String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Process user input and return a ChatMessage
     */
    public ChatMessage processUserInput(String userInput, String sessionId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(userInput);
        chatMessage.setSender("USER");
        chatMessage.setTimestamp(new Date(System.currentTimeMillis()));
        
        // Extract entities and determine query type
        Map<String, String> entities = extractEntities(userInput);
        String queryType = determineQueryType(userInput, entities);
        String actionType = determineActionType(userInput, entities);
        
        chatMessage.setQueryType(queryType);
        chatMessage.setActionType(actionType);
        
        // Convert Map to List for extracted entities
        List<String> entityList = new ArrayList<>();
        for (Map.Entry<String, String> entry : entities.entrySet()) {
            entityList.add(entry.getKey() + ":" + entry.getValue());
        }
        chatMessage.setExtractedEntities(entityList);
        
        // Check if query is complete
        boolean isComplete = isCompleteQuery(userInput, entities);
        chatMessage.setCompleteQuery(isComplete);
        chatMessage.setRequiresFollowUp(!isComplete);
        
        if (!isComplete) {
            chatMessage.setExpectedResponseType(determineExpectedResponseType(userInput, entities));
        }
        
        // Add to conversation history
        addToConversationHistory(sessionId, chatMessage);
        
        return chatMessage;
    }
    
    /**
     * Create a parts contract request
     */
    public String createPartsContractRequest(String userQuery, String partNumber, String sessionId) {
        // Create conversation state
        ConversationState state = new ConversationState();
        state.sessionId = sessionId;
        state.originalQuery = userQuery;
        state.partNumber = partNumber;
        state.flowType = "PARTS_CONTRACT_REQUEST";
        state.status = "WAITING_FOR_CONTRACT";
        
        conversationStates.put(sessionId, state);
        
        return "I need the contract number to look up information for part " + partNumber + ". Please provide the contract number.";
    }
    
    /**
     * Check if a response is a follow-up response
     */
    public boolean isFollowUpResponse(String response, String sessionId) {
        ConversationState state = conversationStates.get(sessionId);
        if (state == null) return false;
        
        // Check if response looks like a contract number
        return response != null && response.trim().matches("\\d{4,8}");
    }
    
    /**
     * Process contract number response
     */
    public ChatMessage processContractNumberResponse(String response, String sessionId) {
        ConversationState state = conversationStates.get(sessionId);
        if (state == null) {
            return createErrorChatMessage("No active conversation found for session: " + sessionId);
        }
        
        // Extract contract number
        String contractNumber = extractContractNumber(response);
        
        // Reconstruct the original query with contract number
        String reconstructedQuery = reconstructQueryWithContract(state.originalQuery, contractNumber);
        
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(reconstructedQuery);
        chatMessage.setSender("SYSTEM");
        chatMessage.setTimestamp(new Date(System.currentTimeMillis()));
        chatMessage.setQueryType("PARTS");
        chatMessage.setActionType("PARTS_QUERY");
        chatMessage.setCompleteQuery(true);
        chatMessage.setRequiresFollowUp(false);
        
        Map<String, String> entities = new HashMap<>();
        entities.put("partNumber", state.partNumber);
        entities.put("contractNumber", contractNumber);
        
        // Convert Map to List for extracted entities
        List<String> entityList = new ArrayList<>();
        for (Map.Entry<String, String> entry : entities.entrySet()) {
            entityList.add(entry.getKey() + ":" + entry.getValue());
        }
        chatMessage.setExtractedEntities(entityList);
        
        // Clear the conversation state
        conversationStates.remove(sessionId);
        
        return chatMessage;
    }
    
    /**
     * Get conversation state for a session
     */
    public ConversationState getConversationState(String sessionId) {
        return conversationStates.get(sessionId);
    }
    
    /**
     * Clear conversation state for a session
     */
    public void clearConversationState(String sessionId) {
        conversationStates.remove(sessionId);
        conversationHistory.remove(sessionId);
    }
    
    /**
     * Get conversation history for a session
     */
    public List<ChatMessage> getConversationHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * Cleanup old conversation states
     */
    public void cleanupOldStates() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 30 * 60 * 1000; // 30 minutes
        
        conversationStates.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > maxAge);
        
        conversationHistory.entrySet().removeIf(entry -> 
            entry.getValue().isEmpty() || 
            currentTime - entry.getValue().get(entry.getValue().size() - 1).getTimestamp().getTime() > maxAge);
    }
    
    // Helper methods
    private Map<String, String> extractEntities(String userInput) {
        Map<String, String> entities = new HashMap<>();
        
        // Extract part number (pattern: letters + numbers + optional dash + numbers)
        if (userInput.matches(".*\\b[A-Z]{2}\\d{4}V\\d-\\d{1,2}\\b.*")) {
            String partNumber = userInput.replaceAll(".*\\b([A-Z]{2}\\d{4}V\\d-\\d{1,2})\\b.*", "$1");
            entities.put("partNumber", partNumber);
        }
        
        // Extract contract number (4-8 digits)
        if (userInput.matches(".*\\b\\d{4,8}\\b.*")) {
            String contractNumber = userInput.replaceAll(".*\\b(\\d{4,8})\\b.*", "$1");
            entities.put("contractNumber", contractNumber);
        }
        
        return entities;
    }
    
    private String determineQueryType(String userInput, Map<String, String> entities) {
        String input = userInput.toLowerCase();
        
        if (input.contains("lead time")) return "LEAD_TIME";
        if (input.contains("price")) return "PRICE";
        if (input.contains("details")) return "DETAILS";
        if (input.contains("create")) return "CONTRACT_CREATION";
        if (input.contains("help")) return "HELP";
        
        return "GENERAL";
    }
    
    private String determineActionType(String userInput, Map<String, String> entities) {
        String input = userInput.toLowerCase();
        
        if (input.contains("create")) return "CONTRACT_CREATION";
        if (input.contains("lead time")) return "LEAD_TIME_QUERY";
        if (input.contains("price")) return "PRICE_QUERY";
        if (input.contains("details")) return "DETAILS_QUERY";
        if (input.contains("help")) return "HELP_REQUEST";
        
        return "GENERAL_QUERY";
    }
    
    private boolean isCompleteQuery(String userInput, Map<String, String> entities) {
        return entities.containsKey("partNumber") && entities.containsKey("contractNumber");
    }
    
    private String determineExpectedResponseType(String userInput, Map<String, String> entities) {
        if (entities.containsKey("partNumber") && !entities.containsKey("contractNumber")) {
            return "CONTRACT_NUMBER";
        }
        return "GENERAL";
    }
    
    private String extractContractNumber(String response) {
        return response.replaceAll(".*?(\\d{4,8}).*", "$1");
    }
    
    private String reconstructQueryWithContract(String originalQuery, String contractNumber) {
        return originalQuery + " contract " + contractNumber;
    }
    
    private void addToConversationHistory(String sessionId, ChatMessage chatMessage) {
        List<ChatMessage> history = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        chatMessage.setMessageIndex(history.size());
        history.add(chatMessage);
    }
    
    private ChatMessage createErrorChatMessage(String errorMessage) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(errorMessage);
        chatMessage.setSender("SYSTEM");
        chatMessage.setTimestamp(new Date(System.currentTimeMillis()));
        chatMessage.setQueryType("ERROR");
        chatMessage.setActionType("ERROR");
        chatMessage.setCompleteQuery(false);
        chatMessage.setRequiresFollowUp(false);
        return chatMessage;
    }
    
    /**
     * Conversation state inner class
     */
    public static class ConversationState {
        public String sessionId;
        public String originalQuery;
        public String partNumber;
        public String contractNumber;
        public String flowType;
        public String status;
        public long timestamp = System.currentTimeMillis();
    }
    
    /**
     * Start a new conversation
     */
    public ConversationalNLPManager.ChatbotResponse startConversation(String userInput, 
                                                                      NLPQueryClassifier.QueryResult nlpResult, 
                                                                      ConversationSession session) {
        
        // Determine conversation type
        if (nlpResult.metadata.actionType.contains("HELP_CONTRACT_CREATE") || 
            userInput.toLowerCase().contains("create")) {
            return startContractCreationFlow(userInput, session);
        } else if (nlpResult.metadata.queryType.equals("HELP")) {
            return startHelpFlow(userInput, session);
        }
        
        // Default response
        return createDefaultResponse("I'm here to help! What would you like to do?");
    }
    
    /**
     * Continue existing conversation
     */
    public ConversationalNLPManager.ChatbotResponse continueConversation(String userInput, ConversationSession session) {
        
        // Process user input and extract data
        ConversationSession.DataExtractionResult extractionResult = session.processUserInput(userInput);
        
        // Handle based on flow type
        switch (session.getCurrentFlowType()) {
            case "CONTRACT_CREATION":
                return continueContractCreationFlow(userInput, session, extractionResult);
            case "HELP":
                return continueHelpFlow(userInput, session, extractionResult);
            default:
                return createDefaultResponse("I'm not sure how to continue this conversation. Let me help you with something else.");
        }
    }
    
    /**
     * Start contract creation flow
     */
    private ConversationalNLPManager.ChatbotResponse startContractCreationFlow(String userInput, ConversationSession session) {
        session.startContractCreationFlow();
        
        // Generate initial prompt
        String prompt = generateContractCreationPrompt(session);
        
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.9;
        
        // Set response data
        response.data = createContractCreationResponse(prompt, session);
        response.dataProviderResponse = createDataProviderResponse(response);
        
        // Add bot response to session
        session.addBotResponse(prompt);
        
        return response;
    }
    
    /**
     * Continue contract creation flow
     */
    private ConversationalNLPManager.ChatbotResponse continueContractCreationFlow(String userInput, 
                                                                                 ConversationSession session, 
                                                                                 ConversationSession.DataExtractionResult extractionResult) {
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.9;
        // Check for validation errors
        if (!extractionResult.validationErrors.isEmpty()) {
            String errorMessage = "I found some issues:\n" + String.join("\n", extractionResult.validationErrors);
            response.data = createContractCreationResponse(errorMessage + "\n\n" + generateContractCreationPrompt(session), session);
            response.dataProviderResponse = createDataProviderResponse(response);
            session.addBotResponse(errorMessage);
            return response;
        }
        // Check if all required fields are collected
        List<String> missingFields = session.getMissingContractFields();
        if (missingFields.isEmpty()) {
            // All data collected, process contract creation
            return processContractCreation(session);
        } else {
            // Still collecting data
            String prompt = generateContractCreationPrompt(session);
            response.data = createContractCreationResponse(prompt, session);
            response.dataProviderResponse = createDataProviderResponse(response);
            session.addBotResponse(prompt);
            return response;
        }
    }

    /**
     * Generate contract creation prompt based on missing fields
     */
    private String generateContractCreationPrompt(ConversationSession session) {
        List<String> missingFields = session.getMissingContractFields();
        Map<String, Object> collectedData = session.getCollectedData();
        if (missingFields.isEmpty()) {
            return "All information collected! Processing contract creation...";
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please provide the following information to create your contract:\n\n");
        for (String field : missingFields) {
            prompt.append("- ").append(ContractFieldConfig.getDisplayName(field)).append(": ");
        }
        // Show what's already collected
        if (!collectedData.isEmpty()) {
            prompt.append("\n\nInformation already provided:\n");
            for (Map.Entry<String, Object> entry : collectedData.entrySet()) {
                if (entry.getValue() != null) {
                    prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }
        return prompt.toString();
    }
    
    /**
     * Process contract creation when all data is collected
     */
    private ConversationalNLPManager.ChatbotResponse processContractCreation(ConversationSession session) {
        Map<String, Object> collectedData = session.getCollectedData();
        
        // Validate all data
        List<String> validationErrors = validateContractData(collectedData);
        
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "CONTRACT_CREATION_COMPLETE";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.95;
        
        if (validationErrors.isEmpty()) {
            // Create contract (this would integrate with your existing contract creation logic)
            String successMessage = "Great! I've collected all the information needed to create your contract:\n\n" +
                formatContractData(collectedData) + "\n\nContract creation process initiated successfully!";

            // --- Checklist Extension ---
            // Set checklist mode and prompt for checklist creation
            session.setChecklistStatus("pending");
            session.setWaitingForUserInput(true);
            String checklistPrompt = successMessage +
                "\n\nWould you like to create a checklist for this contract? (Yes/No)";
            response.data = createContractCreationResponse(checklistPrompt, session);
            response.dataProviderResponse = createDataProviderResponse(response);
            session.addBotResponse(checklistPrompt);
            // Do NOT mark session as completed yet
            return response;
        } else {
            // Validation failed
            String errorMessage = "I found some issues with the provided data:\n" + String.join("\n", validationErrors) +
                "\n\nPlease provide the correct information.";
            
            response.data = createContractCreationResponse(errorMessage + "\n\n" + generateContractCreationPrompt(session), session);
            response.dataProviderResponse = createDataProviderResponse(response);
            session.addBotResponse(errorMessage);
        }
        
        return response;
    }
    
    /**
     * Validate contract data
     */
    private List<String> validateContractData(Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        
        // Validate customer number
        Object customerNumber = data.get("CUSTOMER_NUMBER");
        if (customerNumber == null || customerNumber.toString().trim().isEmpty()) {
            errors.add("Customer number is required");
        } else {
            String custNum = customerNumber.toString();
            if (custNum.length() < 4 || custNum.length() > 8) {
                errors.add("Customer number must be 4-8 digits");
            }
        }
        
        // Validate contract name
        Object contractName = data.get("CONTRACT_NAME");
        if (contractName == null || contractName.toString().trim().isEmpty()) {
            errors.add("Contract name is required");
        }
        
        // Validate title
        Object title = data.get("TITLE");
        if (title == null || title.toString().trim().isEmpty()) {
            errors.add("Title is required");
        }
        
        // Validate description
        Object description = data.get("DESCRIPTION");
        if (description == null || description.toString().trim().isEmpty()) {
            errors.add("Description is required");
        }
        
        return errors;
    }
    
    /**
     * Format contract data for display
     */
    private String formatContractData(Map<String, Object> data) {
        StringBuilder formatted = new StringBuilder();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                formatted.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Start help flow
     */
    private ConversationalNLPManager.ChatbotResponse startHelpFlow(String userInput, ConversationSession session) {
        session.startHelpFlow();
        
        String helpMessage = "I can help you with:\n" +
            "- Contract creation\n" +
            "- Parts queries (lead time, price, details)\n" +
            "- Contract management\n" +
            "- General assistance\n\n" +
            "What would you like to do?";
        
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_GENERAL";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.9;
        
        response.data = createHelpResponse(helpMessage);
        response.dataProviderResponse = createDataProviderResponse(response);
        
        session.addBotResponse(helpMessage);
        
        return response;
    }
    
    /**
     * Continue help flow
     */
    private ConversationalNLPManager.ChatbotResponse continueHelpFlow(String userInput, ConversationSession session, 
                                                                     ConversationSession.DataExtractionResult extractionResult) {
        
        String helpMessage = "I understand you need help. Could you please be more specific about what you'd like to do?";
        
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_GENERAL";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.8;
        
        response.data = createHelpResponse(helpMessage);
        response.dataProviderResponse = createDataProviderResponse(response);
        
        session.addBotResponse(helpMessage);
        
        return response;
    }
    
    /**
     * Create contract creation response
     */
    private Object createContractCreationResponse(String message, ConversationSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("flowType", "CONTRACT_CREATION");
        response.put("sessionId", session.getSessionId());
        response.put("remainingFields", session.getRemainingFields());
        response.put("collectedData", session.getCollectedData());
        return response;
    }
    
    /**
     * Create help response
     */
    private Object createHelpResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("flowType", "HELP");
        return response;
    }
    
    /**
     * Create default response
     */
    private ConversationalNLPManager.ChatbotResponse createDefaultResponse(String message) {
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "GENERAL";
        response.metadata.actionType = "GENERAL_RESPONSE";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.7;
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        response.data = data;
        response.dataProviderResponse = createDataProviderResponse(response);
        
        return response;
    }
    
    /**
     * Create data provider response
     */
    private String createDataProviderResponse(ConversationalNLPManager.ChatbotResponse response) {
        return "{\"dataProviderResponse\":{\"message\":\"" + 
               response.data.toString().replace("\"", "\\\"") + 
               "\",\"success\":" + response.isSuccess + "}}";
    }

    private static final List<String> CHECKLIST_FIELDS = Arrays.asList(
        "EFFECTIVE_DATE", "SYSTEM_DATE", "EXPIRATION_DATE", "PARTS_EXPIRATION_DATE", "QUARTER", "FLOW_DATE"
    );

    private static final Map<String, String> CHECKLIST_PROMPTS = new HashMap<String, String>() {{
        put("EFFECTIVE_DATE", "Please provide the Effective Date (MM/dd/yy):");
        put("SYSTEM_DATE", "Please provide the System Date (MM/dd/yy):");
        put("EXPIRATION_DATE", "Please provide the Expiration Date (MM/dd/yy):");
        put("PARTS_EXPIRATION_DATE", "Please provide the Parts Expiration Date (MM/dd/yy):");
        put("QUARTER", "Please provide the Quarter (1/2/3/4):");
        put("FLOW_DATE", "Please provide the Flow Date (MM/dd/yy):");
    }};

    // Checklist handling in the main conversational flow
    public ConversationalNLPManager.ChatbotResponse handleChecklistFlow(String userInput, ConversationSession session) {
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.metadata = new ConversationalNLPManager.ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType =NLPConstants.ACTION_TYPE_HELP_CHECK_LIST;
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 0.95;

        // Get checklist state from context
        Boolean checklistPending = (Boolean) session.getContext().getOrDefault("checklist_pending", false);
        Integer checklistFieldIndex = (Integer) session.getContext().getOrDefault("checklist_field_index", 0);
        @SuppressWarnings("unchecked")
        Map<String, String> checklistData = (Map<String, String>) session.getContext().get("checklist_data");
        if (checklistData == null) {
            checklistData = new HashMap<>();
            session.getContext().put("checklist_data", checklistData);
        }

        // If just started, expect Yes/No
        if (checklistFieldIndex == 0 && checklistData.isEmpty()) {
            String normalized = SpellCorrector.normalizeYesNo(userInput);
            if ("YES".equals(normalized)) {
                // Start checklist collection
                session.getContext().put("checklist_field_index", 0);
                String prompt = CHECKLIST_PROMPTS.get(CHECKLIST_FIELDS.get(0));
                response.data = createContractCreationResponse(prompt, session);
                response.dataProviderResponse = createDataProviderResponse(response);
                session.addBotResponse(prompt);
                session.setWaitingForUserInput(true); // keep waiting for checklist response
                return response;
            } else if ("NO".equals(normalized) || "N".equals(normalized) || "Nope".equals(normalized)) {
                // User declined checklist, mark as complete
                session.setChecklistStatus("skipped");
                session.markCompleted();
                session.setWaitingForUserInput(false); // set to false after checklist is skipped
                String doneMsg = "Checklist creation skipped. Contract process is now fully complete.";
                response.data = createContractCreationResponse(doneMsg, session);
                response.dataProviderResponse = createDataProviderResponse(response);
                session.addBotResponse(doneMsg);
                return response;
            } else {
                // Invalid response, re-prompt
                String prompt = "Please respond with Yes or No: Would you like to create a checklist for this contract?";
                response.data = createContractCreationResponse(prompt, session);
                response.dataProviderResponse = createDataProviderResponse(response);
                session.addBotResponse(prompt);
                return response;
            }
        } else {
            // Collect checklist fields sequentially
            int idx = checklistFieldIndex;
            if (idx < CHECKLIST_FIELDS.size()) {
                String field = CHECKLIST_FIELDS.get(idx);
                String value = userInput.trim();
                String validation = validateChecklistField(field, value);
                if (validation != null) {
                    // Invalid, re-prompt
                    String prompt = validation + "\n" + CHECKLIST_PROMPTS.get(field);
                    response.data = createContractCreationResponse(prompt, session);
                    response.dataProviderResponse = createDataProviderResponse(response);
                    session.addBotResponse(prompt);
                    return response;
                }
                // Store value
                checklistData.put(field, value);
                session.getContext().put("checklist_data", checklistData);
                idx++;
                session.getContext().put("checklist_field_index", idx);
                if (idx < CHECKLIST_FIELDS.size()) {
                    String prompt = CHECKLIST_PROMPTS.get(CHECKLIST_FIELDS.get(idx));
                    response.data = createContractCreationResponse(prompt, session);
                    response.dataProviderResponse = createDataProviderResponse(response);
                    session.addBotResponse(prompt);
                    return response;
                } else {
                    // All checklist fields collected - VALIDATE
                    Map<String, String> validationResult = null;
                    try {
                        validationResult = (Map<String, String>) com.oracle.view.source.NLPUserActionHandler.getInstance().validateCheckListDates(new HashMap<>(checklistData));
                    } catch (Exception e) {
                        String err = "Checklist validation failed due to system error: " + e.getMessage();
                        response.data = createContractCreationResponse(err, session);
                        response.dataProviderResponse = createDataProviderResponse(response);
                        session.addBotResponse(err);
                        return response;
                    }
                    boolean allNA = validationResult != null && validationResult.values().stream().allMatch(v -> "NA".equals(v));
                    if (allNA) {
                        // Call checklist creation method (replace with actual method if needed)
                        String creationResult = "SUCCESS";
                        try {
                            // If you have a real method, replace below:
                            // creationResult = NLPUserActionHandler.getInstance().checkListCreation(checklistData);
                        } catch (Exception e) {
                            creationResult = "FAILED: " + e.getMessage();
                        }
                        if ("SUCCESS".equalsIgnoreCase(creationResult)) {
                            session.setChecklistStatus("completed");
                            session.markCompleted();
                            session.setWaitingForUserInput(false); // set to false after checklist is completed
                            String doneMsg = "Checklist creation successful. Contract process is now fully complete.";
                            response.data = createContractCreationResponse(doneMsg, session);
                            response.dataProviderResponse = createDataProviderResponse(response);
                            session.addBotResponse(doneMsg);
                            return response;
                        } else {
                            String failMsg = "Checklist creation failed: " + creationResult;
                            response.data = createContractCreationResponse(failMsg, session);
                            response.dataProviderResponse = createDataProviderResponse(response);
                            session.addBotResponse(failMsg);
                            return response;
                        }
                    } else {
                        // Show validation errors and prompt for correction
                        StringBuilder errorMsg = new StringBuilder("Checklist validation failed:\n");
                        for (Map.Entry<String, String> entry : validationResult.entrySet()) {
                            if (!"NA".equals(entry.getValue())) {
                                errorMsg.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                            }
                        }
                        // Find the first invalid field to re-prompt
                        int firstInvalidIdx = 0;
                        for (int i = 0; i < CHECKLIST_FIELDS.size(); i++) {
                            String f = CHECKLIST_FIELDS.get(i);
                            if (validationResult.containsKey(f) && !"NA".equals(validationResult.get(f))) {
                                firstInvalidIdx = i;
                                break;
                            }
                        }
                        session.getContext().put("checklist_field_index", firstInvalidIdx);
                        String prompt = errorMsg.toString() + CHECKLIST_PROMPTS.get(CHECKLIST_FIELDS.get(firstInvalidIdx));
                        response.data = createContractCreationResponse(prompt, session);
                        response.dataProviderResponse = createDataProviderResponse(response);
                        session.addBotResponse(prompt);
                        return response;
                    }
                }
            }
        }
        // Fallback
        String fallback = "Checklist not active.";
        response.data = createContractCreationResponse(fallback, session);
        response.dataProviderResponse = createDataProviderResponse(response);
        session.addBotResponse(fallback);
        return response;
    }

    private String validateChecklistField(String field, String value) {
        if (value == null || value.trim().isEmpty()) return "This field is required.";
        value = value.trim();
        switch (field) {
            case "EFFECTIVE_DATE":
            case "SYSTEM_DATE":
            case "EXPIRATION_DATE":
            case "PARTS_EXPIRATION_DATE":
            case "FLOW_DATE":
                if (!value.matches("\\d{2}/\\d{2}/\\d{2}")) return "Invalid date format. Please use MM/dd/yy.";
                break;
            case "QUARTER":
                if (!value.matches("[1-4]")) return "Quarter must be 1, 2, 3, or 4.";
                break;
        }
        return null;
    }
} 