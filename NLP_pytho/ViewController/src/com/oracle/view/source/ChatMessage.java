package com.oracle.view.source;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ChatMessage {
    private String sender;
    private String message;
    private Date timestamp;
    private boolean isBot;
    private String messageType;
    
    // Enhanced conversation tracking
    private String conversationId;
    private String sessionId;
    private int messageIndex;
    private String previousMessageId;
    private String nextMessageId;
    
    // Conversation context for intelligent processing
    private Map<String, Object> context;
    private boolean isCompleteQuery;
    private boolean requiresFollowUp;
    private String expectedResponseType;
    
    // Step navigation attributes
    private boolean isStepByStepMessage;
    private List<String> steps;
    private int currentStepIndex;
    private int totalSteps;
    
    // Enhanced message metadata
    private String queryType;
    private String actionType;
    private double confidence;
    private List<String> extractedEntities;
    private String processingStatus;
    
    public ChatMessage() {
        this.messageType = "text";
        this.isStepByStepMessage = false;
        this.currentStepIndex = 0;
        this.context = new HashMap<>();
        this.isCompleteQuery = false;
        this.requiresFollowUp = false;
        this.confidence = 1.0;
        this.extractedEntities = new java.util.ArrayList<>();
        this.processingStatus = "pending";
    }
        
    public ChatMessage(String sender, String message, Date timestamp, boolean isBot) {
        this();
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.isBot = isBot;
    }

    // Constructor for step-by-step messages
    public ChatMessage(String sender, String message, Date timestamp, boolean isBot,
                       String messageType, List<String> steps) {
        this(sender, message, timestamp, isBot);
        this.messageType = messageType;
        this.steps = steps;
        this.isStepByStepMessage = true;
        this.totalSteps = steps != null ? steps.size() : 0;
    }
    
    // Enhanced constructor with conversation context
    public ChatMessage(String sender, String message, Date timestamp, boolean isBot,
                      String sessionId, String conversationId, int messageIndex) {
        this(sender, message, timestamp, isBot);
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.messageIndex = messageIndex;
    }
        
    // ============================================================================
    // BASIC GETTERS AND SETTERS
    // ============================================================================
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public boolean isBot() { return isBot; }
    public void setBot(boolean isBot) { this.isBot = isBot; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    // ============================================================================
    // CONVERSATION TRACKING GETTERS AND SETTERS
    // ============================================================================
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public int getMessageIndex() { return messageIndex; }
    public void setMessageIndex(int messageIndex) { this.messageIndex = messageIndex; }
    
    public String getPreviousMessageId() { return previousMessageId; }
    public void setPreviousMessageId(String previousMessageId) { this.previousMessageId = previousMessageId; }
    
    public String getNextMessageId() { return nextMessageId; }
    public void setNextMessageId(String nextMessageId) { this.nextMessageId = nextMessageId; }
    
    // ============================================================================
    // CONTEXT AND INTELLIGENCE GETTERS AND SETTERS
    // ============================================================================
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    public boolean isCompleteQuery() { return isCompleteQuery; }
    public void setCompleteQuery(boolean isCompleteQuery) { this.isCompleteQuery = isCompleteQuery; }
    
    public boolean isRequiresFollowUp() { return requiresFollowUp; }
    public void setRequiresFollowUp(boolean requiresFollowUp) { this.requiresFollowUp = requiresFollowUp; }
    
    public String getExpectedResponseType() { return expectedResponseType; }
    public void setExpectedResponseType(String expectedResponseType) { this.expectedResponseType = expectedResponseType; }
    
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public List<String> getExtractedEntities() { return extractedEntities; }
    public void setExtractedEntities(List<String> extractedEntities) { this.extractedEntities = extractedEntities; }
    
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { 
        System.out.println("DEBUG: Setting processing status: " + processingStatus);
        this.processingStatus = processingStatus; 
    }
    
    // ============================================================================
    // STEP NAVIGATION GETTERS AND SETTERS
    // ============================================================================

    public boolean isStepByStepMessage() { return isStepByStepMessage; }
    public void setStepByStepMessage(boolean stepByStepMessage) { this.isStepByStepMessage = stepByStepMessage; }
    
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { 
        this.steps = steps; 
        this.totalSteps = steps != null ? steps.size() : 0;
    }
    
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }
    
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    // ============================================================================
    // CONTEXT MANAGEMENT METHODS
    // ============================================================================
    
    public void addContext(String key, Object value) {
        if (this.context == null) {
            this.context = new HashMap<>();
        }
        this.context.put(key, value);
    }
    
    public Object getContextValue(String key) {
        return this.context != null ? this.context.get(key) : null;
    }
    
    public void addExtractedEntity(String entity) {
        if (this.extractedEntities == null) {
            this.extractedEntities = new java.util.ArrayList<>();
        }
        this.extractedEntities.add(entity);
    }
    
    // ============================================================================
    // NAVIGATION HELPER METHODS
    // ============================================================================
    
    public String getCurrentStep() {
        if (steps != null && currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return "";
    }
    
    public boolean canGoNext() {
        return currentStepIndex < totalSteps - 1;
    }
    
    public boolean canGoPrevious() {
        return currentStepIndex > 0;
    }
    
    public void nextStep() {
        if (canGoNext()) {
            currentStepIndex++;
        }
    }
    
    public void previousStep() {
        if (canGoPrevious()) {
            currentStepIndex--;
        }
    }
    
    public String getStepProgress() {
        return (currentStepIndex + 1) + " of " + totalSteps;
    }
    
    // ============================================================================
    // FORMATTING METHODS
    // ============================================================================
        
    public String getFormattedTime() {
        if (timestamp != null) {
            return new java.text.SimpleDateFormat("HH:mm").format(timestamp);
        }
        return "";
    }
    
    public String getFormattedTimestamp() {
        if (timestamp != null) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
        }
        return "";
    }

    // Get all steps as single formatted message
    public String getAllStepsFormatted() {
        if (steps == null || steps.isEmpty()) {
            return message;
        }
        
        StringBuilder allSteps = new StringBuilder();
        allSteps.append("<div style='font-family: Arial, sans-serif; line-height: 1.8; background: #f8f9fa; padding: 20px; border-radius: 10px;'>");
        allSteps.append("<h2 style='color: #2c5aa0; margin-bottom: 20px;'>? How to Create a New Contract</h2>");
        
        for (int i = 0; i < steps.size(); i++) {
            allSteps.append("<div style='margin: 15px 0; padding: 15px; background: white; border-left: 4px solid ");
            allSteps.append(getStepColor(i));
            allSteps.append("; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
            allSteps.append("<div style='font-weight: bold; color: ").append(getStepColor(i)).append("; margin-bottom: 8px;'>");
            allSteps.append("Step ").append(i + 1).append("</div>");
            allSteps.append(steps.get(i));
            if (i < steps.size() - 1) {
                allSteps.append("<div style='text-align: center; margin-top: 10px; color: #666;'>");
                allSteps.append("?? NEXT");
                allSteps.append("</div>");
            }
            allSteps.append("</div>");
        }
        
        allSteps.append("<div style='margin-top: 20px; padding: 15px; background: #e7f3ff; border-radius: 5px;'>");
        allSteps.append("<h3 style='color: #0066cc; margin-top: 0;'>? Quick Tips</h3>");
        allSteps.append("<ul style='margin: 10px 0;'>");
        allSteps.append("<li>Each step must be completed in order</li>");
        allSteps.append("<li>Save your work frequently</li>");
        allSteps.append("<li>Contact support if you get stuck</li>");
        allSteps.append("</ul>");
        allSteps.append("</div>");
        allSteps.append("</div>");
        
        return allSteps.toString();
    }
    
    private String getStepColor(int stepIndex) {
        String[] colors = {"#2c5aa0", "#28a745", "#ffc107", "#dc3545", "#6f42c1"};
        return colors[stepIndex % colors.length];
    }
    
    // ============================================================================
    // CONVERSATION INTELLIGENCE METHODS
    // ============================================================================
    
    /**
     * Check if this message contains a complete query with all required information
     * ENFORCED: All part queries MUST have contract number for performance
     */
    public boolean hasCompleteInformation() {
        System.out.println("=== METHOD: hasCompleteInformation() ===");
        System.out.println("Parameters: message=" + message);
        
        if (message == null || message.trim().isEmpty()) {
            System.out.println("RESULT: false (empty message)");
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // DEBUG: Log the completeness check
        System.out.println("DEBUG: hasCompleteInformation check for: " + message);
        System.out.println("DEBUG: Lower message: " + lowerMessage);
        
        // FIX: Handle typos in "part" detection
        boolean hasPartKeyword = lowerMessage.contains("part") || 
                                lowerMessage.contains("pasrt") ||  // typo
                                lowerMessage.contains("prt") ||    // typo
                                lowerMessage.contains("prat");     // typo
        
        boolean hasContractKeyword = lowerMessage.contains("contract");
        
        System.out.println("DEBUG: Contains 'part' (with typos): " + hasPartKeyword);
        System.out.println("DEBUG: Contains 'contract': " + hasContractKeyword);
        
        // Check for parts queries with both part number and contract number
        if (hasPartKeyword && hasContractKeyword) {
            System.out.println("DEBUG: Checking part + contract patterns");
            
            // FIX: More flexible pattern matching for merged queries
            // Pattern: "part EN6114V4-13 contract 100476" or "part air-a320-001 contract 100476"
            boolean pattern1 = lowerMessage.matches(".*(?:part|pasrt|prt|prat)\\s+[A-Za-z0-9-]+.*contract\\s+\\d+.*");
            System.out.println("DEBUG: Pattern 1 (part + contract): " + pattern1);
            
            // Pattern: "contract 100476 part EN6114V4-13"
            boolean pattern2 = lowerMessage.matches(".*contract\\s+\\d+.*(?:part|pasrt|prt|prat)\\s+[A-Za-z0-9-]+.*");
            System.out.println("DEBUG: Pattern 2 (contract + part): " + pattern2);
            
            if (pattern1 || pattern2) {
                System.out.println("DEBUG: Pattern match found - query is complete");
                return true;
            }
            
            // FIX: Also check if we have both entities in context (for merged queries)
            String partNumber = (String) getContextValue("partNumber");
            String contractNumber = (String) getContextValue("contractNumber");
            System.out.println("DEBUG: Part number in context: " + partNumber);
            System.out.println("DEBUG: Contract number in context: " + contractNumber);
            
            if (partNumber != null && !partNumber.isEmpty() && 
                contractNumber != null && !contractNumber.isEmpty()) {
                System.out.println("DEBUG: Both entities in context - query is complete");
                return true;
            }
        }
        
        // ENFORCED: All part-related queries (including lead time) MUST have contract number
        // FIX: Also check for lead time queries and part numbers in context
        if ((hasPartKeyword || lowerMessage.contains("lead time")) && !hasContractKeyword) {
            // Check if we have a part number in context (indicating this is a part query)
            String partNumber = (String) getContextValue("partNumber");
            if (partNumber != null && !partNumber.isEmpty()) {
                System.out.println("DEBUG: Part query without contract - INCOMPLETE (found part number: " + partNumber + ")");
                return false;
            }
        }
        
        // Check for contract queries (not part-related)
        if (hasContractKeyword && !hasPartKeyword) {
            System.out.println("DEBUG: Contract query without parts - COMPLETE");
            return true; // Contract queries can be processed without parts
        }
        
        // Check for customer queries
        if (lowerMessage.contains("customer") || lowerMessage.contains("account")) {
            System.out.println("DEBUG: Customer query - COMPLETE");
            return true; // Customer queries can be processed
        }
        
        System.out.println("DEBUG: No patterns matched - INCOMPLETE");
        System.out.println("RESULT: false (no patterns matched)");
        return false;
    }
    
    /**
     * Extract key entities from the message for intelligent processing
     */
    public void extractEntities() {
        System.out.println("=== METHOD: extractEntities() ===");
        System.out.println("Parameters: message=" + message);
        
        if (message == null || message.trim().isEmpty()) {
            System.out.println("RESULT: No entities extracted (empty message)");
            return;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // DEBUG: Log the extraction process
        System.out.println("DEBUG: extractEntities - message: " + message);
        
        // Extract contract numbers (6+ digits) FIRST to avoid conflicts
        java.util.regex.Pattern contractPattern = java.util.regex.Pattern.compile("\\b(\\d{6,})\\b");
        java.util.regex.Matcher contractMatcher = contractPattern.matcher(message);
        while (contractMatcher.find()) {
            String contractNumber = contractMatcher.group(1);
            addExtractedEntity("CONTRACT_NUMBER:" + contractNumber);
            addContext("contractNumber", contractNumber);
            System.out.println("DEBUG: Extracted contract number: " + contractNumber);
        }
        
        // Extract part numbers (alphanumeric with hyphens, must contain at least one letter)
        // FIX: More specific pattern to avoid matching "contract" as a part number
        java.util.regex.Pattern partPattern = java.util.regex.Pattern.compile("\\b([A-Za-z][A-Za-z0-9-]*|[A-Za-z0-9-]*[A-Za-z][A-Za-z0-9-]*)\\b");
        java.util.regex.Matcher partMatcher = partPattern.matcher(message);
        while (partMatcher.find()) {
            String partNumber = partMatcher.group(1);
            // Skip if this is already extracted as a contract number or is a common word
            if (!getExtractedEntities().contains("CONTRACT_NUMBER:" + partNumber) && 
                !isCommonWord(partNumber) && 
                !partNumber.equalsIgnoreCase("contract") &&
                !partNumber.equalsIgnoreCase("part") &&
                !partNumber.equalsIgnoreCase("parts")) {
                addExtractedEntity("PART_NUMBER:" + partNumber);
                addContext("partNumber", partNumber);
                System.out.println("DEBUG: Extracted part number: " + partNumber);
            }
        }
        
        // Extract account numbers (4-8 digits, but not contract numbers)
        java.util.regex.Pattern accountPattern = java.util.regex.Pattern.compile("\\b(\\d{4,8})\\b");
        java.util.regex.Matcher accountMatcher = accountPattern.matcher(message);
        while (accountMatcher.find()) {
            String accountNumber = accountMatcher.group(1);
            // Avoid double-counting contract numbers as account numbers
            if (!getExtractedEntities().contains("CONTRACT_NUMBER:" + accountNumber)) {
                addExtractedEntity("ACCOUNT_NUMBER:" + accountNumber);
                addContext("accountNumber", accountNumber);
                System.out.println("DEBUG: Extracted account number: " + accountNumber);
            }
        }
        
        // Determine query type
        if (lowerMessage.contains("lead time")) {
            setQueryType("PARTS_LEAD_TIME");
            setActionType("parts_lead_time_query");
        } else if (lowerMessage.contains("part") || 
                   lowerMessage.contains("pasrt") ||  // typo
                   lowerMessage.contains("prt") ||    // typo
                   lowerMessage.contains("prat")) {   // typo
            setQueryType("PARTS");
            setActionType("parts_query");
        } else if (lowerMessage.contains("contract")) {
            setQueryType("CONTRACTS");
            setActionType("contract_query");
        }
        
        // Set complete query flag
        setCompleteQuery(hasCompleteInformation());
        
        // DEBUG: Log final context values
        System.out.println("DEBUG: Final part number in context: " + getContextValue("partNumber"));
        System.out.println("DEBUG: Final contract number in context: " + getContextValue("contractNumber"));
        System.out.println("RESULT: Entities extracted successfully");
    }
    
    private boolean isCommonWord(String word) {
        System.out.println("=== METHOD: isCommonWord() ===");
        System.out.println("Parameters: word=" + word);
        
        String[] commonWords = {"the", "and", "for", "with", "in", "on", "at", "to", "of", "a", "an", 
                               "contract", "part", "parts", "lead", "time", "what", "is", "for"};
        for (String common : commonWords) {
            if (word.equalsIgnoreCase(common)) {
                System.out.println("RESULT: true (common word: " + common + ")");
                return true;
            }
        }
        System.out.println("RESULT: false (not a common word)");
        return false;
    }
    
    /**
     * Create a response message with proper conversation tracking
     */
    public ChatMessage createResponse(String responseMessage, boolean isBotResponse) {
        ChatMessage response = new ChatMessage();
        response.setSender(isBotResponse ? "Bot" : "You");
        response.setMessage(responseMessage);
        response.setTimestamp(new Date());
        response.setBot(isBotResponse);
        response.setSessionId(this.sessionId);
        response.setConversationId(this.conversationId);
        response.setMessageIndex(this.messageIndex + 1);
        response.setPreviousMessageId(this.conversationId + "_" + this.messageIndex);
        response.setNextMessageId(null);
        
        return response;
    }
    
    @Override
    public String toString() {
        return String.format("ChatMessage{sender='%s', message='%s', timestamp=%s, isBot=%s, sessionId='%s', messageIndex=%d, isCompleteQuery=%s}",
                           sender, message, getFormattedTimestamp(), isBot, sessionId, messageIndex, isCompleteQuery);
    }
}
