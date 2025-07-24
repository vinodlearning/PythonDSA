package com.oracle.view.source;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

/**
 * Comprehensive Conversation Session Manager
 * 
 * Manages user conversation sessions with proper state tracking:
 * - Session creation and management
 * - Conversation state tracking
 * - Follow-up detection and handling
 * - Multi-turn conversation chains
 * - Timeout and cleanup
 */
public class ConversationSessionManager {
    
    // Singleton instance
    private static volatile ConversationSessionManager instance;
    
    // Session storage with thread-safe access
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();
    
    // Configuration constants
    private static final long SESSION_TIMEOUT_MS = 300000; // 5 minutes
    private static final long CONVERSATION_TIMEOUT_MS = 600000; // 10 minutes
    private static final int MAX_CONVERSATION_HISTORY = 50;
    
    // Conversation types
    public static final String CONVERSATION_TYPE_PARTS_CONTRACT = "PARTS_CONTRACT_REQUEST";
    public static final String CONVERSATION_TYPE_CUSTOMER_ACCOUNT = "CUSTOMER_ACCOUNT_REQUEST";
    public static final String CONVERSATION_TYPE_CONTRACT_CREATION = "CONTRACT_CREATION_REQUEST";
    public static final String CONVERSATION_TYPE_PART_SEARCH = "PART_SEARCH_REQUEST";
    public static final String CONVERSATION_TYPE_LEAD_TIME = "LEAD_TIME_REQUEST";
    
    // Expected response types
    public static final String EXPECTED_RESPONSE_CONTRACT_NUMBER = "CONTRACT_NUMBER";
    public static final String EXPECTED_RESPONSE_PART_NUMBER = "PART_NUMBER";
    public static final String EXPECTED_RESPONSE_CUSTOMER_ACCOUNT = "CUSTOMER_ACCOUNT";
    public static final String EXPECTED_RESPONSE_CONTRACT_NAME = "CONTRACT_NAME";
    public static final String EXPECTED_RESPONSE_CONTRACT_DURATION = "CONTRACT_DURATION";
    public static final String EXPECTED_RESPONSE_CONFIRMATION = "CONFIRMATION";
    
    private ConversationSessionManager() {
        // Start cleanup thread
        startCleanupThread();
    }
    
    public static ConversationSessionManager getInstance() {
        if (instance == null) {
            synchronized (ConversationSessionManager.class) {
                if (instance == null) {
                    instance = new ConversationSessionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * User Session - represents a user's active session
     */
    public static class UserSession {
        private String sessionId;
        private String userId;
        private Instant createdAt;
        private Instant lastActivity;
        private List<ChatMessage> messageHistory;
        private ConversationState currentConversation;
        private Map<String, Object> sessionData;
        
        public UserSession(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.createdAt = Instant.now();
            this.lastActivity = Instant.now();
            this.messageHistory = new ArrayList<>();
            this.sessionData = new HashMap<>();
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastActivity() { return lastActivity; }
        public List<ChatMessage> getMessageHistory() { return messageHistory; }
        public ConversationState getCurrentConversation() { return currentConversation; }
        public Map<String, Object> getSessionData() { return sessionData; }
        
        public void updateActivity() {
            this.lastActivity = Instant.now();
        }
        
        public void addMessage(ChatMessage message) {
            this.messageHistory.add(message);
            if (this.messageHistory.size() > MAX_CONVERSATION_HISTORY) {
                this.messageHistory.remove(0); // Remove oldest message
            }
        }
        
        public void setCurrentConversation(ConversationState conversation) {
            this.currentConversation = conversation;
        }
        
        public boolean isTimedOut() {
            return Instant.now().isAfter(lastActivity.plusMillis(SESSION_TIMEOUT_MS));
        }
    }
    
    /**
     * Conversation State - represents the current state of a conversation
     */
    public static class ConversationState {
        private String conversationId;
        private String sessionId;
        private String conversationType;
        private String status; // "waiting_for_input", "in_progress", "complete", "cancelled"
        private String expectedResponseType;
        private String originalQuery;
        private Map<String, String> collectedData;
        private List<String> validationErrors;
        private Instant createdAt;
        private Instant lastUpdated;
        private int turnCount;
        private String currentStep;
        private Map<String, Object> context;
        
        public ConversationState(String sessionId, String conversationType) {
            this.conversationId = generateConversationId();
            this.sessionId = sessionId;
            this.conversationType = conversationType;
            this.status = "waiting_for_input";
            this.collectedData = new HashMap<>();
            this.validationErrors = new ArrayList<>();
            this.createdAt = Instant.now();
            this.lastUpdated = Instant.now();
            this.turnCount = 0;
            this.context = new HashMap<>();
        }
        
        // Getters and setters
        public String getConversationId() { return conversationId; }
        public String getSessionId() { return sessionId; }
        public String getConversationType() { return conversationType; }
        public String getStatus() { return status; }
        public String getExpectedResponseType() { return expectedResponseType; }
        public String getOriginalQuery() { return originalQuery; }
        public Map<String, String> getCollectedData() { return collectedData; }
        public List<String> getValidationErrors() { return validationErrors; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastUpdated() { return lastUpdated; }
        public int getTurnCount() { return turnCount; }
        public String getCurrentStep() { return currentStep; }
        public Map<String, Object> getContext() { return context; }
        
        public void setStatus(String status) {
            this.status = status;
            this.lastUpdated = Instant.now();
        }
        
        public void setExpectedResponseType(String expectedResponseType) {
            this.expectedResponseType = expectedResponseType;
        }
        
        public void setOriginalQuery(String originalQuery) {
            this.originalQuery = originalQuery;
        }
        
        public void addCollectedData(String key, String value) {
            this.collectedData.put(key, value);
            this.lastUpdated = Instant.now();
        }
        
        public void addValidationError(String error) {
            this.validationErrors.add(error);
        }
        
        public void incrementTurnCount() {
            this.turnCount++;
        }
        
        public void setCurrentStep(String step) {
            this.currentStep = step;
        }
        
        public void addContext(String key, Object value) {
            this.context.put(key, value);
        }
        
        public boolean isTimedOut() {
            return Instant.now().isAfter(lastUpdated.plusMillis(CONVERSATION_TIMEOUT_MS));
        }
        
        public boolean isWaitingForInput() {
            return "waiting_for_input".equals(status);
        }
        
        public boolean isComplete() {
            return "complete".equals(status);
        }
        
        public boolean isCancelled() {
            return "cancelled".equals(status);
        }
    }
    
    /**
     * Process user input and determine if it's a new request or follow-up
     */
    public ConversationResult processUserInput(String userInput, String sessionId) {
        try {
            // Clean up expired sessions
            cleanupExpiredSessions();
            
            // Get or create user session
            UserSession userSession = getUserOrCreateSession(sessionId);
            
            // Check if this is a follow-up response to an existing conversation
            if (userSession.getCurrentConversation() != null && 
                userSession.getCurrentConversation().isWaitingForInput()) {
                
                ConversationState currentConversation = userSession.getCurrentConversation();
                
                // Check if this input matches the expected response type
                if (isExpectedResponse(userInput, currentConversation)) {
                    return processFollowUpResponse(userInput, userSession, currentConversation);
                } else {
                    // User provided unexpected input - might be a new request
                    return handleUnexpectedInput(userInput, userSession, currentConversation);
                }
            }
            
            // This is a new request - analyze and create new conversation
            return processNewRequest(userInput, userSession);
            
        } catch (Exception e) {
            return ConversationResult.error("Error processing user input: " + e.getMessage());
        }
    }
    
    /**
     * Check if user input matches the expected response type
     */
    private boolean isExpectedResponse(String userInput, ConversationState conversation) {
        String expectedType = conversation.getExpectedResponseType();
        
        if (expectedType == null) return false;
        
        switch (expectedType) {
            case EXPECTED_RESPONSE_CONTRACT_NUMBER:
                return isContractNumberResponse(userInput);
                
            case EXPECTED_RESPONSE_PART_NUMBER:
                return isPartNumberResponse(userInput);
                
            case EXPECTED_RESPONSE_CUSTOMER_ACCOUNT:
                return isCustomerAccountResponse(userInput);
                
            case EXPECTED_RESPONSE_CONTRACT_NAME:
                return isContractNameResponse(userInput);
                
            case EXPECTED_RESPONSE_CONTRACT_DURATION:
                return isContractDurationResponse(userInput);
                
            case EXPECTED_RESPONSE_CONFIRMATION:
                return isConfirmationResponse(userInput);
                
            default:
                return false;
        }
    }
    
    /**
     * Process a follow-up response
     */
    private ConversationResult processFollowUpResponse(String userInput, UserSession userSession, ConversationState conversation) {
        try {
            // Extract the expected data from user input
            String extractedValue = extractExpectedValue(userInput, conversation.getExpectedResponseType());
            
            if (extractedValue == null) {
                return ConversationResult.followUpRequired(
                    "I didn't understand that. " + getExpectedResponsePrompt(conversation.getExpectedResponseType()),
                    conversation.getConversationId(),
                    conversation.getExpectedResponseType()
                );
            }
            
            // Add the collected data
            conversation.addCollectedData(conversation.getExpectedResponseType(), extractedValue);
            conversation.incrementTurnCount();
            
            // Check if we have all required information
            if (hasAllRequiredInformation(conversation)) {
                // Complete the conversation
                conversation.setStatus("complete");
                return ConversationResult.complete(conversation, "Conversation complete with all required information.");
            } else {
                // Need more information - determine what's next
                String nextExpectedType = determineNextExpectedResponse(conversation);
                conversation.setExpectedResponseType(nextExpectedType);
                conversation.setStatus("waiting_for_input");
                
                return ConversationResult.followUpRequired(
                    getExpectedResponsePrompt(nextExpectedType),
                    conversation.getConversationId(),
                    nextExpectedType
                );
            }
            
        } catch (Exception e) {
            return ConversationResult.error("Error processing follow-up response: " + e.getMessage());
        }
    }
    
    /**
     * Process a new request
     */
    private ConversationResult processNewRequest(String userInput, UserSession userSession) {
        try {
            // Analyze the request to determine conversation type
            String conversationType = analyzeConversationType(userInput);
            
            if (conversationType == null) {
                // This is a complete query that doesn't need conversation
                return ConversationResult.complete(null, "Complete query - no conversation needed.");
            }
            
            // Create new conversation state
            ConversationState newConversation = new ConversationState(userSession.getSessionId(), conversationType);
            newConversation.setOriginalQuery(userInput);
            
            // Determine what information is missing
            String missingInfo = determineMissingInformation(userInput, conversationType);
            if (missingInfo != null) {
                newConversation.setExpectedResponseType(missingInfo);
                newConversation.setStatus("waiting_for_input");
                
                // Set the conversation as current for this session
                userSession.setCurrentConversation(newConversation);
                conversationStates.put(newConversation.getConversationId(), newConversation);
                
                return ConversationResult.followUpRequired(
                    getExpectedResponsePrompt(missingInfo),
                    newConversation.getConversationId(),
                    missingInfo
                );
            } else {
                // All information is present
                newConversation.setStatus("complete");
                return ConversationResult.complete(newConversation, "Complete query with all information.");
            }
            
        } catch (Exception e) {
            return ConversationResult.error("Error processing new request: " + e.getMessage());
        }
    }
    
    /**
     * Handle unexpected input (user provided different type of response)
     */
    private ConversationResult handleUnexpectedInput(String userInput, UserSession userSession, ConversationState conversation) {
        // Check if this looks like a new request
        if (isNewRequest(userInput)) {
            // Cancel current conversation and start new one
            conversation.setStatus("cancelled");
            return processNewRequest(userInput, userSession);
        } else {
            // Ask for the expected response again
            return ConversationResult.followUpRequired(
                "I was expecting " + getExpectedResponseDescription(conversation.getExpectedResponseType()) + 
                ". Please provide that information, or ask a new question.",
                conversation.getConversationId(),
                conversation.getExpectedResponseType()
            );
        }
    }
    
    /**
     * Analyze conversation type based on user input
     */
    private String analyzeConversationType(String userInput) {
        String lowerInput = userInput.toLowerCase();
        
        // Parts queries without contract number
        if ((lowerInput.contains("part") || lowerInput.contains("lead time")) && 
            !lowerInput.matches(".*\\b\\d{6,}\\b.*")) {
            return CONVERSATION_TYPE_PARTS_CONTRACT;
        }
        
        // Customer queries without account number
        if (lowerInput.contains("customer") && !lowerInput.matches(".*\\b\\d{4,8}\\b.*")) {
            return CONVERSATION_TYPE_CUSTOMER_ACCOUNT;
        }
        
        // Contract creation queries
        if (lowerInput.contains("create") && lowerInput.contains("contract")) {
            return CONVERSATION_TYPE_CONTRACT_CREATION;
        }
        
        // Part search queries
        if (lowerInput.contains("search") && lowerInput.contains("part")) {
            return CONVERSATION_TYPE_PART_SEARCH;
        }
        
        return null; // No conversation needed
    }
    
    /**
     * Determine missing information for a conversation type
     */
    private String determineMissingInformation(String userInput, String conversationType) {
        String lowerInput = userInput.toLowerCase();
        
        switch (conversationType) {
            case CONVERSATION_TYPE_PARTS_CONTRACT:
                if (!lowerInput.matches(".*\\b\\d{6,}\\b.*")) {
                    return EXPECTED_RESPONSE_CONTRACT_NUMBER;
                }
                break;
                
            case CONVERSATION_TYPE_CUSTOMER_ACCOUNT:
                if (!lowerInput.matches(".*\\b\\d{4,8}\\b.*")) {
                    return EXPECTED_RESPONSE_CUSTOMER_ACCOUNT;
                }
                break;
                
            case CONVERSATION_TYPE_CONTRACT_CREATION:
                if (!lowerInput.contains("name") && !lowerInput.contains("title")) {
                    return EXPECTED_RESPONSE_CONTRACT_NAME;
                }
                break;
        }
        
        return null; // All information present
    }
    
    /**
     * Check if user input looks like a new request
     */
    private boolean isNewRequest(String userInput) {
        String lowerInput = userInput.toLowerCase();
        
        // Check for question words or action words
        return lowerInput.matches(".*\\b(what|how|when|where|why|show|get|find|search|create|update|delete)\\b.*") ||
               lowerInput.matches(".*\\b(contract|part|customer|lead time|price|status)\\b.*");
    }
    
    /**
     * Response type detection methods
     */
    private boolean isContractNumberResponse(String userInput) {
        return userInput.trim().matches("\\d{6,}") || 
               userInput.toLowerCase().matches(".*contract\\s+\\d{6,}.*");
    }
    
    private boolean isPartNumberResponse(String userInput) {
        return userInput.trim().matches("[A-Za-z0-9-]+") && userInput.length() >= 3;
    }
    
    private boolean isCustomerAccountResponse(String userInput) {
        return userInput.trim().matches("\\d{4,8}") || 
               userInput.matches(".*[a-zA-Z].*") && userInput.length() >= 3;
    }
    
    private boolean isContractNameResponse(String userInput) {
        return userInput.matches(".*[a-zA-Z].*") && userInput.length() >= 3;
    }
    
    private boolean isContractDurationResponse(String userInput) {
        return userInput.matches(".*\\d+\\s*(month|year|day).*") || 
               userInput.matches("\\d+");
    }
    
    private boolean isConfirmationResponse(String userInput) {
        String normalized = SpellCorrector.normalizeYesNo(userInput);
        return "YES".equals(normalized);
    }
    
    /**
     * Extract expected value from user input
     */
    private String extractExpectedValue(String userInput, String expectedType) {
        switch (expectedType) {
            case EXPECTED_RESPONSE_CONTRACT_NUMBER:
                return extractContractNumber(userInput);
                
            case EXPECTED_RESPONSE_PART_NUMBER:
                return extractPartNumber(userInput);
                
            case EXPECTED_RESPONSE_CUSTOMER_ACCOUNT:
                return extractCustomerAccount(userInput);
                
            case EXPECTED_RESPONSE_CONTRACT_NAME:
                return extractContractName(userInput);
                
            case EXPECTED_RESPONSE_CONTRACT_DURATION:
                return extractContractDuration(userInput);
                
            case EXPECTED_RESPONSE_CONFIRMATION:
                return extractConfirmation(userInput);
                
            default:
                return null;
        }
    }
    
    /**
     * Value extraction methods
     */
    private String extractContractNumber(String userInput) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{6,})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractPartNumber(String userInput) {
        return userInput.trim().replaceAll("[^A-Za-z0-9-]", "");
    }
    
    private String extractCustomerAccount(String userInput) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{4,8})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractContractName(String userInput) {
        return userInput.trim();
    }
    
    private String extractContractDuration(String userInput) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(month|year|day)");
        java.util.regex.Matcher matcher = pattern.matcher(userInput.toLowerCase());
        return matcher.find() ? matcher.group(1) + " " + matcher.group(2) : null;
    }
    
    private String extractConfirmation(String userInput) {
        String normalized = SpellCorrector.normalizeYesNo(userInput);
        if ("YES".equals(normalized)) {
            return "YES";
        } else if ("NO".equals(normalized)) {
            return "NO";
        }
        return null;
    }
    
    /**
     * Check if conversation has all required information
     */
    private boolean hasAllRequiredInformation(ConversationState conversation) {
        switch (conversation.getConversationType()) {
            case CONVERSATION_TYPE_PARTS_CONTRACT:
                return conversation.getCollectedData().containsKey(EXPECTED_RESPONSE_CONTRACT_NUMBER);
                
            case CONVERSATION_TYPE_CUSTOMER_ACCOUNT:
                return conversation.getCollectedData().containsKey(EXPECTED_RESPONSE_CUSTOMER_ACCOUNT);
                
            case CONVERSATION_TYPE_CONTRACT_CREATION:
                return conversation.getCollectedData().containsKey(EXPECTED_RESPONSE_CONTRACT_NAME) &&
                       conversation.getCollectedData().containsKey(EXPECTED_RESPONSE_CONTRACT_DURATION);
                
            default:
                return true;
        }
    }
    
    /**
     * Determine next expected response
     */
    private String determineNextExpectedResponse(ConversationState conversation) {
        switch (conversation.getConversationType()) {
            case CONVERSATION_TYPE_CONTRACT_CREATION:
                if (!conversation.getCollectedData().containsKey(EXPECTED_RESPONSE_CONTRACT_NAME)) {
                    return EXPECTED_RESPONSE_CONTRACT_NAME;
                } else if (!conversation.getCollectedData().containsKey(EXPECTED_RESPONSE_CONTRACT_DURATION)) {
                    return EXPECTED_RESPONSE_CONTRACT_DURATION;
                }
                break;
        }
        return null;
    }
    
    /**
     * Get prompt for expected response type
     */
    private String getExpectedResponsePrompt(String expectedType) {
        switch (expectedType) {
            case EXPECTED_RESPONSE_CONTRACT_NUMBER:
                return "Please provide the contract number (e.g., 100476, 123456):";
                
            case EXPECTED_RESPONSE_PART_NUMBER:
                return "Please provide the part number:";
                
            case EXPECTED_RESPONSE_CUSTOMER_ACCOUNT:
                return "Please provide the customer account number or name:";
                
            case EXPECTED_RESPONSE_CONTRACT_NAME:
                return "Please provide a name for the contract:";
                
            case EXPECTED_RESPONSE_CONTRACT_DURATION:
                return "Please specify the contract duration (e.g., 12 months, 2 years):";
                
            case EXPECTED_RESPONSE_CONFIRMATION:
                return "Please confirm (yes/no):";
                
            default:
                return "Please provide the required information:";
        }
    }
    
    private String getExpectedResponseDescription(String expectedType) {
        switch (expectedType) {
            case EXPECTED_RESPONSE_CONTRACT_NUMBER:
                return "a contract number";
            case EXPECTED_RESPONSE_PART_NUMBER:
                return "a part number";
            case EXPECTED_RESPONSE_CUSTOMER_ACCOUNT:
                return "a customer account";
            case EXPECTED_RESPONSE_CONTRACT_NAME:
                return "a contract name";
            case EXPECTED_RESPONSE_CONTRACT_DURATION:
                return "a contract duration";
            case EXPECTED_RESPONSE_CONFIRMATION:
                return "a confirmation";
            default:
                return "the required information";
        }
    }
    
    /**
     * Session management methods
     */
    public UserSession getUserOrCreateSession(String sessionId) {
        UserSession userSession = userSessions.get(sessionId);
        if (userSession == null) {
            userSession = new UserSession(sessionId, sessionId);
            userSessions.put(sessionId, userSession);
        }
        return userSession;
    }
    
    public UserSession getUserSession(String sessionId) {
        return userSessions.get(sessionId);
    }
    
    public ConversationState getConversationState(String conversationId) {
        return conversationStates.get(conversationId);
    }
    
    public void endConversation(String conversationId) {
        ConversationState conversation = conversationStates.get(conversationId);
        if (conversation != null) {
            conversation.setStatus("complete");
        }
    }
    
    public void cancelConversation(String conversationId) {
        ConversationState conversation = conversationStates.get(conversationId);
        if (conversation != null) {
            conversation.setStatus("cancelled");
        }
    }
    
    /**
     * Cleanup methods
     */
    private void cleanupExpiredSessions() {
        userSessions.entrySet().removeIf(entry -> entry.getValue().isTimedOut());
        conversationStates.entrySet().removeIf(entry -> entry.getValue().isTimedOut());
    }
    
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Run every minute
                    cleanupExpiredSessions();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * Utility methods
     */
    private static String generateConversationId() {
        return "conv_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }
    
    /**
     * Conversation Result - represents the result of processing user input
     */
    public static class ConversationResult {
        private final boolean isComplete;
        private final boolean requiresFollowUp;
        private final String message;
        private final ConversationState conversation;
        private final String expectedResponseType;
        private final String error;
        
        private ConversationResult(boolean isComplete, boolean requiresFollowUp, String message, 
                                 ConversationState conversation, String expectedResponseType, String error) {
            this.isComplete = isComplete;
            this.requiresFollowUp = requiresFollowUp;
            this.message = message;
            this.conversation = conversation;
            this.expectedResponseType = expectedResponseType;
            this.error = error;
        }
        
        public static ConversationResult complete(ConversationState conversation, String message) {
            return new ConversationResult(true, false, message, conversation, null, null);
        }
        
        public static ConversationResult followUpRequired(String message, String conversationId, String expectedResponseType) {
            return new ConversationResult(false, true, message, null, expectedResponseType, null);
        }
        
        public static ConversationResult error(String error) {
            return new ConversationResult(false, false, null, null, null, error);
        }
        
        // Getters
        public boolean isComplete() { return isComplete; }
        public boolean requiresFollowUp() { return requiresFollowUp; }
        public String getMessage() { return message; }
        public ConversationState getConversation() { return conversation; }
        public String getExpectedResponseType() { return expectedResponseType; }
        public String getError() { return error; }
    }
} 