package com.oracle.view.source;

import com.oracle.view.source.ConversationSession.DataExtractionResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversation Session for managing multi-turn conversations
 * Stores conversation state, user inputs, and expected entities
 */
public class ConversationSession {
    
    private final String sessionId;
    private final String userId;
    private final long creationTime;
    private long lastActivityTime;
    
    // Conversation state
    private ConversationState state;
    private String currentFlowType; // "CONTRACT_CREATION", "HELP", etc.
    private String contractCreationStatus; // "PENDING", "RECEIVED", "COMPLETED", "CANCELLED"
    private  Map<String, Map<String,Object>> auditData=new HashMap<>();


    public void setAuditData(Map<String, Map<String, Object>> auditData) {
        this.auditData = auditData;
    }

    public Map<String, Map<String, Object>> getAuditData() {
        return auditData;
    }
    // Data collection for multi-turn conversations
    private final Map<String, Object> collectedData = new ConcurrentHashMap<>();
    
    // User search results for "created by" queries
    private Map<String, List<Map<String, String>>> userSearchResults = new HashMap<>();
    private List<String> userDisplayOrder = new ArrayList<>(); // Store the display order
    private long userSearchTimestamp = 0;
    private static final long USER_SEARCH_EXPIRY = 300000; // 5 minutes
    
    // Contract search results for multiple contracts per user
    private Map<String, List<Map<String, Object>>> contractSearchResults = new HashMap<>();
    private List<String> contractDisplayOrder = new ArrayList<>(); // Store the display order
    private long contractSearchTimestamp = 0;
    private static final long CONTRACT_SEARCH_EXPIRY = 300000; // 5 minutes
    
    // Validation results
    private final Map<String, ValidationResult> validationResults = new ConcurrentHashMap<>();
    
    // Conversation history
    private final List<ConversationTurn> conversationHistory = new ArrayList<>();

    // --- Checklist/Child Activity Support ---
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    public Map<String, Object> getContext() { return context; }
    // --- End Checklist/Child Activity Support ---
    
    // --- Modular Contract Creation & Checklist Tracking ---
    private boolean isContractCreationBotInitiated = false;
    private String checklistStatus = "pending"; // "pending", "completed", "skipped"
    private long lastUserInputTime = System.currentTimeMillis();
    public boolean isContractCreationBotInitiated() { return isContractCreationBotInitiated; }
    public void setContractCreationBotInitiated(boolean val) { isContractCreationBotInitiated = val; }
    public String getContractCreationStatus() { return contractCreationStatus; }
    public void setContractCreationStatus(String status) { contractCreationStatus = status; }
    public String getChecklistStatus() { return checklistStatus; }
    public void setChecklistStatus(String status) { checklistStatus = status; }
    public long getLastUserInputTime() { return lastUserInputTime; }
    public void updateLastUserInputTime() { lastUserInputTime = System.currentTimeMillis(); }
    // --- End Modular Contract Creation & Checklist Tracking ---
    
    /**
     * True if the system is waiting for the user to confirm whether to start the checklist process after contract creation.
     */
    private boolean awaitingChecklistConfirmation = false;
    /**
     * True if the checklist process has been initiated and the system is waiting for the user to provide checklist details.
     */
    private boolean checklistInputPending = false;
    /**
     * True if the checklist data has been submitted and the checklist creation process is in progress.
     */
    private boolean checklistProcessing = false;

    public boolean isAwaitingChecklistConfirmation() { return awaitingChecklistConfirmation; }
    public void setAwaitingChecklistConfirmation(boolean value) { this.awaitingChecklistConfirmation = value; }

    public boolean isChecklistInputPending() { return checklistInputPending; }
    public void setChecklistInputPending(boolean value) { this.checklistInputPending = value; }

    public boolean isChecklistProcessing() { return checklistProcessing; }
    public void setChecklistProcessing(boolean value) { this.checklistProcessing = value; }
    
    /**
     * True if the system is waiting for the user to confirm the checklist summary before creation.
     */
    private boolean awaitingChecklistFinalConfirmation = false;
    public boolean isAwaitingChecklistFinalConfirmation() { return awaitingChecklistFinalConfirmation; }
    public void setAwaitingChecklistFinalConfirmation(boolean value) { this.awaitingChecklistFinalConfirmation = value; }
    
    // --- Contract and Checklist Flow Flags ---
    private boolean isCreateContractInitiated = false;
    private boolean isCheckListInitiated = false;
    private final Map<String, String> contractFieldMap = new ConcurrentHashMap<>();
    private final Map<String, String> checklistFieldMap = new ConcurrentHashMap<>();

    public boolean isCreateContractInitiated() { return isCreateContractInitiated; }
    public void setCreateContractInitiated(boolean val) { isCreateContractInitiated = val; }
    public boolean isCheckListInitiated() { return isCheckListInitiated; }
    public void setCheckListInitiated(boolean val) { isCheckListInitiated = val; }
    public Map<String, String> getContractFieldMap() { return contractFieldMap; }
    public Map<String, String> getChecklistFieldMap() { return checklistFieldMap; }
    // --- End Contract and Checklist Flow Flags ---
    
    public ConversationSession(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.creationTime = System.currentTimeMillis();
        this.lastActivityTime = System.currentTimeMillis();
        this.state = ConversationState.IDLE;
    }
    
    /**
     * Check if session is waiting for user input
     */
    public boolean isWaitingForUserInput() {
        System.out.println("isWaitingForUserInput=state==="+state);
        return state == ConversationState.WAITING_FOR_INPUT || 
               state == ConversationState.COLLECTING_DATA ||
               state == ConversationState.READY_TO_PROCESS;
    }
    
    /**
     * Check if session is completed
     */
    public boolean isCompleted() {
        return state == ConversationState.COMPLETED || 
               state == ConversationState.CANCELLED;
    }
    
    /**
     * Set session to wait for user input
     */
    public void setWaitingForUserInput(boolean waiting) {
        if (waiting) {
            this.state = ConversationState.WAITING_FOR_INPUT;
        } else {
            this.state = ConversationState.IDLE;
        }
        updateActivityTime();
    }
    
    /**
     * Store user search results with display order
     */
    public void storeUserSearchResults(Map<String, List<Map<String, String>>> results, List<String> displayOrder) {
        this.userSearchResults.clear();
        this.userSearchResults.putAll(results);
        this.userDisplayOrder.clear();
        if (displayOrder != null) {
            this.userDisplayOrder.addAll(displayOrder);
        }
        this.userSearchTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Store user search results (backward compatibility)
     */
    public void storeUserSearchResults(Map<String, List<Map<String, String>>> results) {
        this.userSearchResults.clear();
        this.userSearchResults.putAll(results);
        // Create display order from map keys (maintains insertion order if using LinkedHashMap)
        this.userDisplayOrder.clear();
        this.userDisplayOrder.addAll(results.keySet());
        this.userSearchTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Get user search results
     */
    public Map<String, List<Map<String, String>>> getUserSearchResults() {
        return new HashMap<>(userSearchResults);
    }
    
    /**
     * Check if user search results are valid (not expired)
     */
    public boolean isUserSearchValid() {
        return !userSearchResults.isEmpty() && 
               (System.currentTimeMillis() - userSearchTimestamp) < USER_SEARCH_EXPIRY;
    }
    
    /**
     * Get user by index (1, 2, 3, etc.)
     */
    public String getUserByIndex(int index) {
        if (!isUserSearchValid()) {
            return null;
        }
        
        // Use display order if available, otherwise fall back to map keys
        List<String> userNames = !userDisplayOrder.isEmpty() ? userDisplayOrder : new ArrayList<>(userSearchResults.keySet());
        if (index >= 1 && index <= userNames.size()) {
            return userNames.get(index - 1);
        }
        return null;
    }
    
    /**
     * Get user by name
     */
    public String getUserByName(String name) {
        if (!isUserSearchValid()) {
            return null;
        }
        
        for (String userName : userSearchResults.keySet()) {
            if (userName.equalsIgnoreCase(name)) {
                return userName;
            }
        }
        return null;
    }
    
    /**
     * Get contracts for a specific user
     */
    public List<Map<String, String>> getContractsForUser(String userName) {
        if (!isUserSearchValid()) {
            return null;
        }
        return userSearchResults.get(userName);
    }
    
    /**
     * Get available users in display order
     */
    public List<String> getAvailableUsers() {
        if (!isUserSearchValid()) {
            return new ArrayList<>();
        }
        // Return display order if available, otherwise fall back to map keys
        return !userDisplayOrder.isEmpty() ? new ArrayList<>(userDisplayOrder) : new ArrayList<>(userSearchResults.keySet());
    }
    
    /**
     * Clear user search results
     */
    public void clearUserSearchResults() {
        userSearchResults.clear();
        userDisplayOrder.clear();
        userSearchTimestamp = 0;
    }
    
    /**
     * Store contract search results
     */
    public void storeContractSearchResults(Map<String, List<Map<String, Object>>> results) {
        this.contractSearchResults.clear();
        this.contractSearchResults.putAll(results);
        this.contractSearchTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Store contract search results with display order
     */
    public void storeContractSearchResults(Map<String, List<Map<String, Object>>> results, List<String> displayOrder) {
        this.contractSearchResults.clear();
        this.contractSearchResults.putAll(results);
        this.contractDisplayOrder.clear();
        if (displayOrder != null) {
            this.contractDisplayOrder.addAll(displayOrder);
        }
        this.contractSearchTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Get contract search results
     */
    public Map<String, List<Map<String, Object>>> getContractSearchResults() {
        return new HashMap<>(contractSearchResults);
    }
    
    /**
     * Check if contract search results are valid (not expired)
     */
    public boolean isContractSearchValid() {
        return !contractSearchResults.isEmpty() && 
               (System.currentTimeMillis() - contractSearchTimestamp) < CONTRACT_SEARCH_EXPIRY;
    }
    
    /**
     * Get contract by index (1, 2, 3, etc.)
     */
    public List<Map<String, Object>> getContractByIndex(int index) {
        String cacheKey = String.valueOf(index);
        return contractSearchResults.get(cacheKey);
    }
    
    /**
     * Get contract display order
     */
    public List<String> getContractDisplayOrder() {
        return new ArrayList<>(contractDisplayOrder);
    }
    
    /**
     * Clear contract search results
     */
    public void clearContractSearchResults() {
        contractSearchResults.clear();
        contractDisplayOrder.clear();
        contractSearchTimestamp = 0;
    }
    
    /**
     * Start contract creation flow
     */
    public void startContractCreationFlow() {
        startContractCreationFlow(null);
    }

    /**
     * Start contract creation flow with optional account number
     */
    public void startContractCreationFlow(String accountNumber) {
        this.currentFlowType = "CONTRACT_CREATION";
        this.state = ConversationState.COLLECTING_DATA;
        this.contractCreationStatus = "PENDING"; // Initialize contract creation status
        // Use centralized config for required fields
        collectedData.clear();
        // Set account number if provided
        if (accountNumber != null && !accountNumber.isEmpty()) {
            collectedData.put("ACCOUNT_NUMBER", accountNumber);
        }
        // Set default values
        collectedData.put("IS_PRICELIST", "NO");
        collectedData.put("COMMENTS", "");
        updateActivityTime();
    }
    
    /**
     * Start help flow
     */
    public void startHelpFlow() {
        this.currentFlowType = "HELP";
        this.state = ConversationState.WAITING_FOR_INPUT;
        
        // Clear any existing data
        collectedData.clear();
        
        updateActivityTime();
    }
    
    /**
     * Add user input to conversation
     */
    public void addUserInput(String userInput) {
        conversationHistory.add(new ConversationTurn("USER", userInput, System.currentTimeMillis()));
        updateActivityTime();
        updateLastUserInputTime();
    }
    
    /**
     * Add bot response to conversation
     */
    public void addBotResponse(String botResponse) {
        conversationHistory.add(new ConversationTurn("BOT", botResponse, System.currentTimeMillis()));
        updateActivityTime();
        updateLastUserInputTime();
    }
    
    /**
     * Process user input and extract data
     */
    public DataExtractionResult processUserInput(String userInput) {
        addUserInput(userInput);
        
        DataExtractionResult result = new DataExtractionResult();
        
        // Extract data based on current flow type
        switch (currentFlowType) {
            case "CONTRACT_CREATION":
                extractContractCreationData(userInput, result);
                break;
        case "CREATE_CHECKLIST":
            extractCheckLIstCreationData(userInput, result);
            break;
            case "HELP":
                extractHelpData(userInput, result);
                break;
            default:
                result.extractedFields = new HashMap<>();
                result.remainingFields = new ArrayList<>();
        }
        
        return result;
    }
    
    /**
     * Extract data for contract creation
     */
    private void extractContractCreationData(String userInput, DataExtractionResult result) {
        
        System.out.println("Starting of extractContractCreationData ==>User Input=="+userInput);
        Map<String, String> extracted = new HashMap<>();
        List<String> remaining = new ArrayList<>();
        
        // ENHANCED: Check if this is a single input with all details
        if (true) {
            System.out.println("======isSingleInputWithAllDetails=====");
            extractAllDetailsFromSingleInput(userInput, extracted, result);
        } 
        
        // Add extracted data to session
        collectedData.putAll(extracted);
        
        // Determine remaining fields
        for (String field : ContractFieldConfig.CONTRACT_CREATION_FIELDS) {
            if (!collectedData.containsKey(field) || 
                collectedData.get(field) == null || 
                collectedData.get(field).toString().trim().isEmpty()) {
                remaining.add(field);
            }
        }
        
        result.extractedFields = extracted;
        result.remainingFields = remaining;
        
        // Check if all required fields are collected
        if (remaining.isEmpty()) {
            state = ConversationState.READY_TO_PROCESS;
        }
    }

    /**
     * Check if input contains all details in a single response
     */
    private boolean isSingleInputWithAllDetails(String userInput) {
        String lowerInput = userInput.toLowerCase();
        
        // Check if it contains multiple data points with field labels
        // Pattern: field: value, field: value, etc.
        if (userInput.contains(":") && userInput.contains(",")) {
            return true;
        }
        
        // Check if it contains multiple data points without field labels
        // Pattern: account_number contract_name title description comments
        String[] words = userInput.split("\\s+");
        
        // If we have 4+ words and no field labels, likely single input
        if (words.length >= 4) {
            boolean hasFieldLabels = lowerInput.contains("account") || 
                                   lowerInput.contains("name") || 
                                   lowerInput.contains("title") || 
                                   lowerInput.contains("description") ||
                                   lowerInput.contains("comments") ||
                                   lowerInput.contains("pricelist");
            
            // Check if first word is likely an account number (6+ digits)
            boolean startsWithAccountNumber = words[0].matches("\\d{6,}");
            
            return !hasFieldLabels && startsWithAccountNumber;
        }
        
        // Check for comma-separated format
        if (userInput.contains(",")) {
            String[] commaParts = userInput.split(",");
            if (commaParts.length >= 4) {
                // Check if first part is account number (6+ digits)
                String firstPart = commaParts[0].trim();
                boolean startsWithAccountNumber = firstPart.matches("\\d{6,}");
                
                // Check if no field labels
                boolean hasFieldLabels = lowerInput.contains("account") || 
                                       lowerInput.contains("name") || 
                                       lowerInput.contains("title") || 
                                       lowerInput.contains("description") ||
                                       lowerInput.contains("comments") ||
                                       lowerInput.contains("pricelist");
                
                return !hasFieldLabels && startsWithAccountNumber;
            }
        }
        
        return false;
    }

    /**
     * Extract all details from a single input with field labels
     */
    private void extractAllDetailsFromSingleInput(String userInput, Map<String, String> extracted, DataExtractionResult result) {
        if("CONTRACT_CREATION".equalsIgnoreCase(currentFlowType)){
        extractFromCommaSeparatedInputContracts(userInput, extracted, result);
        }else if("CREATE_CHECKLIST".equalsIgnoreCase(currentFlowType)){            
            extractFromCommaSeparatedInputCheckList(userInput, extracted, result);
        }else{
            extractFromCommaSeparatedInputContracts(userInput, extracted, result);
        }
    }

    private void extractFromCommaSeparatedInputCheckList(String userInput, Map<String, String> extracted,
                                                         DataExtractionResult result) {
        // Use RobustDataExtractor for initial extraction
        System.out.println("extractFromCommaSeparatedInput===>" + userInput);
        Map<String, String> rawExtracted = RobustDataExtractor.extractData(userInput);
        System.out.println("rawExtracted from RObutsExtractor===>" + rawExtracted);
        Set<String> allowedFields = new HashSet<>(ContractFieldConfig.CHECKLIST_FIELDS);
        System.out.println("extractFromCommaSeparatedInput=============>" + allowedFields);
        for (Map.Entry<String, String> entry : rawExtracted.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String normalizedKey = SpellCorrector.normalizeField(key);
            System.out.println("Key :"+key+", Value :"+value+", normalizedKey :"+normalizedKey);
            if(normalizedKey.equalsIgnoreCase("SIGNATURE_DATE")){
                normalizedKey="DATE_OF_SIGNATURE";
            }
            
            extracted.put(normalizedKey, value);
        }
    }
    /**
     * Extract from comma-separated input with field labels
     */
    private void extractFromCommaSeparatedInputContracts(String userInput, Map<String, String> extracted, DataExtractionResult result) {
        // Use RobustDataExtractor for initial extraction
        System.out.println("extractFromCommaSeparatedInput===>"+userInput);
        Map<String, String> rawExtracted = RobustDataExtractor.extractData(userInput);
       System.out.println("rawExtracted from RObutsExtractor===>"+rawExtracted);
        Set<String> allowedFields = new HashSet<>(ContractFieldConfig.CONTRACT_CREATION_FIELDS);
     System.out.println("extractFromCommaSeparatedInput=============>"+allowedFields);
        for (Map.Entry<String, String> entry : rawExtracted.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String normalizedKey = SpellCorrector.normalizeField(key);
            
            if ("ACCOUNT_NUMBER".equals(normalizedKey) || "CUSTOMER".equals(normalizedKey) || "ACCOUNT".equals(normalizedKey)) {
                String accountNumber = value.replaceAll("[^0-9]", "");
                if (accountNumber.length() >= 6) {
                    boolean isValid = NLPUserActionHandler.getInstance().isCustomerNumberValid(accountNumber);
                    if (isValid) {
                        extracted.put("ACCOUNT_NUMBER", accountNumber);
                    } else {
                        result.validationErrors.add("Invalid account number: " + accountNumber);
                    }
                }
            } else if (allowedFields.contains(normalizedKey)) {
                extracted.put(normalizedKey, value);
            }
        }
    }

    
    /**
     * Extract data for help flow
     */
    private void extractHelpData(String userInput, DataExtractionResult result) {
        // Help flow doesn't collect specific data
        result.extractedFields = new HashMap<>();
        result.remainingFields = new ArrayList<>();
    }
    
   
    /**
     * Get collected data
     */
    public Map<String, Object> getCollectedData() {
        return collectedData; // Return the actual map, not a copy
    }
    
    /**
     * Get remaining required fields
     */
    public List<String> getRemainingFields() {
        List<String> remaining = new ArrayList<>();
        for (String field : ContractFieldConfig.CONTRACT_CREATION_FIELDS) {
            if (!collectedData.containsKey(field) || collectedData.get(field) == null) {
                remaining.add(field);
            }
        }
        return remaining;
    }


    public List<String> getRemainingCheckLIstFields() {
        List<String> remaining = new ArrayList<>();
        for (String field : ContractFieldConfig.CHECKLIST_FIELDS) {
            if (!collectedData.containsKey(field) || collectedData.get(field) == null) {
                remaining.add(field);
            }
        }
        return remaining;
    }
    /**
     * Get missing required contract fields
     */
    public List<String> getMissingContractFields() {
        List<String> missing = new ArrayList<>();
        for (String field : ContractFieldConfig.CONTRACT_CREATION_FIELDS) {
            if (!collectedData.containsKey(field) || collectedData.get(field) == null || collectedData.get(field).toString().trim().isEmpty()) {
                missing.add(field);
            }
        }
        return missing;
    }
    
    /**
     * Clear current flow
     */
    public void clearCurrentFlow() {
        this.state = ConversationState.IDLE;
        this.currentFlowType = null;
        this.contractCreationStatus = null; // Clear contract creation status
        this.collectedData.clear();
        this.validationResults.clear();
        updateActivityTime();
    }
    
    public void clearCache(){
        this.state = ConversationState.IDLE;
        this.currentFlowType = null;
        this.contractCreationStatus = null; // Clear contract creation status
        this.collectedData.clear();
        this.validationResults.clear();
        this.isCreateContractInitiated=false;
        this.isCheckListInitiated=false;
    }
    
    /**
     * Mark session as completed
     */
    public void markCompleted() {
        this.state = ConversationState.COMPLETED;
        updateActivityTime();
    }
    
    /**
     * Mark session as cancelled
     */
    public void markCancelled() {
        this.state = ConversationState.CANCELLED;
        updateActivityTime();
    }
    
    /**
     * Update activity time
     */
    private void updateActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public long getCreationTime() { return creationTime; }
    public long getLastActivityTime() { return lastActivityTime; }
    public ConversationState getState() { return state; }
    public String getCurrentFlowType() { return currentFlowType; }
    public void setCurrentFlowType(String flowType) { this.currentFlowType = flowType; }
    public List<ConversationTurn> getConversationHistory() { return new ArrayList<>(conversationHistory); }

    private void extractCheckLIstCreationData(String userInput, DataExtractionResult result) {


        System.out.println("Starting of extractContractCreationData ==>User Input==" + userInput);
        System.out.println("Collected data===>"+collectedData);
        Map<String, String> extracted = new HashMap<>();
        List<String> remaining = new ArrayList<>();

        // ENHANCED: Check if this is a single input with all details
        if (true) {
            System.out.println("======isSingleInputWithAllDetails=====");
            extractAllDetailsFromSingleInput(userInput, extracted, result);
        }

        // Add extracted data to session
        collectedData.putAll(extracted);

        // Determine remaining fields
        for (String field : ContractFieldConfig.CHECKLIST_FIELDS) {
            if (!collectedData.containsKey(field) || collectedData.get(field) == null || collectedData.get(field)
                                                                                                      .toString()
                                                                                                      .trim()
                                                                                                      .isEmpty()) {
                remaining.add(field);
            }
        }

        result.extractedFields = extracted;
        result.remainingFields = remaining;

        // Check if all required fields are collected
        if (remaining.isEmpty()) {
            state = ConversationState.READY_TO_PROCESS;
        }
    }

    /**
     * Conversation State Enum
     */
    public enum ConversationState {
        IDLE,
        COLLECTING_DATA,
        WAITING_FOR_INPUT,
        READY_TO_PROCESS,
        PROCESSING,
        COMPLETED,
        CANCELLED
    }
    
    /**
     * Conversation Turn Class
     */
    public static class ConversationTurn {
        public final String speaker; // "USER" or "BOT"
        public final String message;
        public final long timestamp;
        
        public ConversationTurn(String speaker, String message, long timestamp) {
            this.speaker = speaker;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Data Extraction Result Class
     */
    public static class DataExtractionResult {
        public Map<String, String> extractedFields = new HashMap<>();
        public List<String> remainingFields = new ArrayList<>();
        public List<String> validationErrors = new ArrayList<>();
    }
    
    /**
     * Validation Result Class
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }
    
    public static void main(String v[]) {
        ConversationSession session=new ConversationSession("e16902bf-273f-418c-86ca-970b79b31ebc","igsbhs");
//        DataExtractionResult dataExtractionResult = session.processUserInput("contract name: Vinod Contract BY VINod,\n" + "account: 12345678,\n" +
//                                     "Title : This is the title,\n" + "description: No description,\n" +
//                                     "commenst : my comments");
        session.setCurrentFlowType("CONTRACT_CREATION");
        DataExtractionResult result = new DataExtractionResult();
        session.extractContractCreationData("hpp=no",result);
        System.out.println("Extracted Fields="+result.extractedFields);
        System.out.println("Remaining Fields :"+result.remainingFields);
        
    }
} 