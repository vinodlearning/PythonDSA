package com.oracle.view.source;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * ContractCreationFlowManager
 * 
 * Manages the complete contract creation workflow with conversational capabilities:
 * - Multi-turn conversation management
 * - Step-by-step data collection
 * - Real-time validation
 * - State persistence
 * - Error handling and recovery
 * - Integration with business logic
 */
public class ContractCreationFlowManager {
    
    // Singleton instance
    private static volatile ContractCreationFlowManager instance;
    
    // Active contract creation sessions
    private final Map<String, ContractCreationSession> activeSessions = new ConcurrentHashMap<>();
    
    // Configuration constants
    private static final long SESSION_TIMEOUT_MS = 300000; // 5 minutes
    private static final int MAX_SESSION_ATTEMPTS = 3;
    
    // Flow steps
    public static final String STEP_INITIAL = "INITIAL";
    public static final String STEP_ACCOUNT_VALIDATION = "ACCOUNT_VALIDATION";
    public static final String STEP_CONTRACT_NAME = "CONTRACT_NAME";
    public static final String STEP_CONTRACT_TITLE = "CONTRACT_TITLE";
    public static final String STEP_CONTRACT_DESCRIPTION = "CONTRACT_DESCRIPTION";
    public static final String STEP_OPTIONAL_FIELDS = "OPTIONAL_FIELDS";
    public static final String STEP_DATES = "DATES";
    public static final String STEP_CONFIRMATION = "CONFIRMATION";
    public static final String STEP_COMPLETE = "COMPLETE";
    public static final String STEP_CANCELLED = "CANCELLED";
    public static final String STEP_ERROR = "ERROR";
    
    // Field validation patterns
    private static final String ACCOUNT_NUMBER_PATTERN = "\\b\\d{7,}\\b";
    private static final String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";
    private static final String YES_NO_PATTERN = "(?i)(yes|no|y|n|skip|default)";
    
    private ContractCreationFlowManager() {
        // Start cleanup thread
        startCleanupThread();
    }
    
    public static ContractCreationFlowManager getInstance() {
        if (instance == null) {
            synchronized (ContractCreationFlowManager.class) {
                if (instance == null) {
                    instance = new ContractCreationFlowManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Contract Creation Session - represents an active contract creation workflow
     */
    public static class ContractCreationSession {
        private String sessionId;
        private String conversationId;
        private String currentStep;
        private Map<String, Object> collectedData;
        private List<String> validationErrors;
        private long createdAt;
        private long lastActivity;
        private int attemptCount;
        private String status; // "active", "paused", "completed", "cancelled", "error"
        private Map<String, Object> context;
        // --- Checklist Extension ---
        private boolean isChecklistPending = false;
        private boolean isWaitingForUserInput = false;
        private Map<String, String> checklistData = new HashMap<>();
        private List<String> checklistFields = Arrays.asList(
            "EFFECTIVE_DATE", "SYSTEM_DATE", "EXPIRATION_DATE", "PARTS_EXPIRATION_DATE", "QUARTER", "FLOW_DATE"
        );
        private int checklistFieldIndex = 0;
        // --- End Checklist Extension ---
        
        public ContractCreationSession(String sessionId) {
            this.sessionId = sessionId;
            this.conversationId = generateConversationId();
            this.currentStep = STEP_INITIAL;
            this.collectedData = new HashMap<>();
            this.validationErrors = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
            this.attemptCount = 0;
            this.status = "active";
            this.context = new HashMap<>();
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getConversationId() { return conversationId; }
        public String getCurrentStep() { return currentStep; }
        public Map<String, Object> getCollectedData() { return collectedData; }
        public List<String> getValidationErrors() { return validationErrors; }
        public long getCreatedAt() { return createdAt; }
        public long getLastActivity() { return lastActivity; }
        public int getAttemptCount() { return attemptCount; }
        public String getStatus() { return status; }
        public Map<String, Object> getContext() { return context; }
        
        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void addCollectedData(String key, Object value) {
            this.collectedData.put(key, value);
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void addValidationError(String error) {
            this.validationErrors.add(error);
        }
        
        public void incrementAttemptCount() {
            this.attemptCount++;
        }
        
        public void setStatus(String status) {
            this.status = status;
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void addContext(String key, Object value) {
            this.context.put(key, value);
        }
        
        public boolean isTimedOut() {
            return System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS;
        }
        
        public boolean isActive() {
            return "active".equals(status);
        }
        
        public boolean isCompleted() {
            return "completed".equals(status);
        }
        
        public boolean isCancelled() {
            return "cancelled".equals(status);
        }
        
        public boolean hasMaxAttempts() {
            return attemptCount >= MAX_SESSION_ATTEMPTS;
        }

        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean isChecklistPending() { return isChecklistPending; }
        public void setChecklistPending(boolean pending) { this.isChecklistPending = pending; }
        public boolean isWaitingForUserInput() { return isWaitingForUserInput; }
        public void setWaitingForUserInput(boolean waiting) { this.isWaitingForUserInput = waiting; }
        public Map<String, String> getChecklistData() { return checklistData; }
        public void setChecklistData(Map<String, String> data) { this.checklistData = data; }
        public List<String> getChecklistFields() { return checklistFields; }
        public int getChecklistFieldIndex() { return checklistFieldIndex; }
        public void setChecklistFieldIndex(int idx) { this.checklistFieldIndex = idx; }
        public void incrementChecklistFieldIndex() { this.checklistFieldIndex++; }
        public void resetChecklist() {
            this.checklistData.clear();
            this.checklistFieldIndex = 0;
        }
    }
    
    /**
     * Workflow Result - represents the result of a workflow step
     */
    public static class WorkflowResult {
        private final boolean success;
        private final String message;
        private final String nextStep;
        private final boolean requiresInput;
        private final String expectedInputType;
        private final Map<String, Object> data;
        private final List<String> errors;
        
        private WorkflowResult(boolean success, String message, String nextStep, 
                             boolean requiresInput, String expectedInputType, 
                             Map<String, Object> data, List<String> errors) {
            this.success = success;
            this.message = message;
            this.nextStep = nextStep;
            this.requiresInput = requiresInput;
            this.expectedInputType = expectedInputType;
            this.data = data != null ? data : new HashMap<>();
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public static WorkflowResult success(String message, String nextStep) {
            return new WorkflowResult(true, message, nextStep, false, null, null, null);
        }
        
        public static WorkflowResult success(String message, String nextStep, Map<String, Object> data) {
            return new WorkflowResult(true, message, nextStep, false, null, data, null);
        }
        
        public static WorkflowResult inputRequired(String message, String expectedInputType) {
            return new WorkflowResult(true, message, null, true, expectedInputType, null, null);
        }
        
        public static WorkflowResult error(String message, List<String> errors) {
            return new WorkflowResult(false, message, STEP_ERROR, false, null, null, errors);
        }
        
        public static WorkflowResult cancelled(String message) {
            return new WorkflowResult(false, message, STEP_CANCELLED, false, null, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getNextStep() { return nextStep; }
        public boolean isRequiresInput() { return requiresInput; }
        public String getExpectedInputType() { return expectedInputType; }
        public Map<String, Object> getData() { return data; }
        public List<String> getErrors() { return errors; }
    }
    
    /**
     * Start a new contract creation session
     */
    public ContractCreationSession startSession(String sessionId) {
        ContractCreationSession session = new ContractCreationSession(sessionId);
        activeSessions.put(sessionId, session);
        return session;
    }
    
    /**
     * Process user input in contract creation flow
     */
    public WorkflowResult processInput(String userInput, String sessionId) {
        try {
            // Get or create session
            ContractCreationSession session = getOrCreateSession(sessionId);
            
            // Check for session timeout
            if (session.isTimedOut()) {
                return WorkflowResult.error("Session timed out. Please start over.", 
                    Arrays.asList("Session expired due to inactivity"));
            }
            
            // Check for cancellation
            if (isCancellationRequest(userInput)) {
                return cancelSession(sessionId);
            }
            
            // Update session activity
            session.updateActivity();
            session.incrementAttemptCount();
            
            // Check for max attempts
            if (session.hasMaxAttempts()) {
                return WorkflowResult.error("Maximum attempts reached. Please start over.", 
                    Arrays.asList("Too many failed attempts"));
            }
            
            // Process based on current step
            String currentStep = session.getCurrentStep();
            
            switch (currentStep) {
                case STEP_INITIAL:
                    return processInitialStep(userInput, session);
                    
                case STEP_ACCOUNT_VALIDATION:
                    return processAccountValidationStep(userInput, session);
                    
                case STEP_CONTRACT_NAME:
                    return processContractNameStep(userInput, session);
                    
                case STEP_CONTRACT_TITLE:
                    return processContractTitleStep(userInput, session);
                    
                case STEP_CONTRACT_DESCRIPTION:
                    return processContractDescriptionStep(userInput, session);
                    
                case STEP_OPTIONAL_FIELDS:
                    return processOptionalFieldsStep(userInput, session);
                    
                case STEP_DATES:
                    return processDatesStep(userInput, session);
                    
                case STEP_CONFIRMATION:
                    return processConfirmationStep(userInput, session);
                    
                default:
                    return WorkflowResult.error("Invalid workflow state", 
                        Arrays.asList("Unknown step: " + currentStep));
            }
            
        } catch (Exception e) {
            return WorkflowResult.error("Error processing input: " + e.getMessage(), 
                Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * Process initial step
     */
    private WorkflowResult processInitialStep(String userInput, ContractCreationSession session) {
        // Extract account number if present
        String accountNumber = extractAccountNumber(userInput);
        
        if (accountNumber != null) {
            // Validate account number
            if (validateAccountNumber(accountNumber)) {
                session.addCollectedData("accountNumber", accountNumber);
                session.setCurrentStep(STEP_CONTRACT_NAME);
                
                return WorkflowResult.inputRequired(
                    formatStepMessage("Account Number", accountNumber, "Validated") +
                    "<p><b>Step 1:</b> What would you like to name this contract?</p>" +
                    "<p><i>Please provide a descriptive contract name.</i></p>",
                    "CONTRACT_NAME"
                );
            } else {
                return WorkflowResult.error("Invalid account number: " + accountNumber,
                    Arrays.asList("Account number must be 7+ digits and exist in system"));
            }
        } else {
            // Ask for account number
            return WorkflowResult.inputRequired(
                "<h4>Contract Creation - Account Required</h4>" +
                "<p>I can help you create a contract automatically. Please provide a valid customer account number.</p>" +
                "<p><b>Example:</b> 10840607 (HONEYWELL INTERNATIONAL INC.)</p>" +
                "<p>What is the customer account number for this contract?</p>",
                "ACCOUNT_NUMBER"
            );
        }
    }
    
    /**
     * Process account validation step
     */
    private WorkflowResult processAccountValidationStep(String userInput, ContractCreationSession session) {
        String accountNumber = extractAccountNumber(userInput);
        
        if (accountNumber == null) {
            return WorkflowResult.error("Account number not provided",
                Arrays.asList("Please provide a valid 7+ digit account number"));
        }
        
        if (validateAccountNumber(accountNumber)) {
            session.addCollectedData("accountNumber", accountNumber);
            session.setCurrentStep(STEP_CONTRACT_NAME);
            
            return WorkflowResult.inputRequired(
                formatStepMessage("Account Number", accountNumber, "Validated") +
                "<p><b>Step 1:</b> What would you like to name this contract?</p>" +
                "<p><i>Please provide a descriptive contract name.</i></p>",
                "CONTRACT_NAME"
            );
        } else {
            return WorkflowResult.error("Invalid account number: " + accountNumber,
                Arrays.asList("Account number must be 7+ digits and exist in system"));
        }
    }
    
    /**
     * Process contract name step
     */
    private WorkflowResult processContractNameStep(String userInput, ContractCreationSession session) {
        String contractName = extractContractName(userInput);
        
        if (contractName == null || contractName.trim().isEmpty()) {
            return WorkflowResult.error("Contract name not provided",
                Arrays.asList("Please provide a descriptive contract name"));
        }
        
        session.addCollectedData("contractName", contractName);
        session.setCurrentStep(STEP_CONTRACT_TITLE);
        
        return WorkflowResult.inputRequired(
            formatStepMessage("Contract Name", contractName, "OK") +
            "<p><b>Step 2:</b> What title would you like for this contract?</p>" +
            "<p><i>Please provide a brief title.</i></p>",
            "CONTRACT_TITLE"
        );
    }
    
    /**
     * Process contract title step
     */
    private WorkflowResult processContractTitleStep(String userInput, ContractCreationSession session) {
        String contractTitle = extractContractTitle(userInput);
        
        if (contractTitle == null || contractTitle.trim().isEmpty()) {
            return WorkflowResult.error("Contract title not provided",
                Arrays.asList("Please provide a brief contract title"));
        }
        
        session.addCollectedData("contractTitle", contractTitle);
        session.setCurrentStep(STEP_CONTRACT_DESCRIPTION);
        
        return WorkflowResult.inputRequired(
            formatStepMessage("Contract Title", contractTitle, "OK") +
            "<p><b>Step 3:</b> Please provide a description for this contract.</p>" +
            "<p><i>Add detailed information about what this contract covers.</i></p>",
            "CONTRACT_DESCRIPTION"
        );
    }
    
    /**
     * Process contract description step
     */
    private WorkflowResult processContractDescriptionStep(String userInput, ContractCreationSession session) {
        String contractDescription = extractContractDescription(userInput);
        
        if (contractDescription == null || contractDescription.trim().isEmpty()) {
            return WorkflowResult.error("Contract description not provided",
                Arrays.asList("Please provide a detailed contract description"));
        }
        
        session.addCollectedData("contractDescription", contractDescription);
        session.setCurrentStep(STEP_OPTIONAL_FIELDS);
        
        return WorkflowResult.inputRequired(
            formatStepMessage("Contract Description", contractDescription, "OK") +
            "<p><b>Step 4:</b> Optional settings (you can skip these with 'skip' or 'no'):</p>" +
            "<p><b>Price List Required?</b> (YES/NO, default: NO)</p>" +
            "<p><b>EM Required?</b> (YES/NO, default: NO)</p>" +
            "<p><i>Please specify for each, or say 'skip' to use defaults.</i></p>",
            "OPTIONAL_FIELDS"
        );
    }
    
    /**
     * Process optional fields step
     */
    private WorkflowResult processOptionalFieldsStep(String userInput, ContractCreationSession session) {
        String lowerInput = userInput.toLowerCase();
        
        // Extract price list setting
        String priceList = extractYesNoSetting(lowerInput, "price list", "price", "pricelist");
        if (priceList == null) {
            priceList = "NO"; // Default
        }
        
        // Extract EM setting
        String isEM = extractYesNoSetting(lowerInput, "em", "em required", "em status");
        if (isEM == null) {
            isEM = "NO"; // Default
        }
        
        session.addCollectedData("priceList", priceList);
        session.addCollectedData("isEM", isEM);
        session.setCurrentStep(STEP_DATES);
        
        return WorkflowResult.inputRequired(
            formatStepMessage("Optional Fields", "Price List: " + priceList + ", EM: " + isEM, "OK") +
            "<p><b>Step 5:</b> Contract dates (you can skip with 'skip' or 'default'):</p>" +
            "<p><b>Effective Date:</b> (YYYY-MM-DD, default: today)</p>" +
            "<p><b>Expiration Date:</b> (YYYY-MM-DD, default: 2 years from today)</p>" +
            "<p><b>Price Expiration Date:</b> (YYYY-MM-DD, optional)</p>" +
            "<p><i>Please specify dates in YYYY-MM-DD format, or say 'skip' for defaults.</i></p>",
            "DATES"
        );
    }
    
    /**
     * Process dates step
     */
    private WorkflowResult processDatesStep(String userInput, ContractCreationSession session) {
        String lowerInput = userInput.toLowerCase();
        
        // Extract effective date
        String effectiveDate = extractDate(lowerInput, "effective", "start", "begin");
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        // Extract expiration date
        String expirationDate = extractDate(lowerInput, "expiration", "expire", "end", "expiry");
        if (expirationDate == null) {
            expirationDate = LocalDate.now().plusYears(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        // Extract price expiration date (optional)
        String priceExpirationDate = extractDate(lowerInput, "price expiration", "price expire", "price end");
        
        session.addCollectedData("effectiveDate", effectiveDate);
        session.addCollectedData("expirationDate", expirationDate);
        if (priceExpirationDate != null) {
            session.addCollectedData("priceExpirationDate", priceExpirationDate);
        }
        
        session.setCurrentStep(STEP_CONFIRMATION);
        
        return WorkflowResult.inputRequired(
            formatStepMessage("Contract Dates", "Effective: " + effectiveDate + ", Expiration: " + expirationDate, "OK") +
            "<p><b>Step 6:</b> Please review the contract details:</p>" +
            formatContractSummary(session) +
            "<p><b>Ready to create?</b> Type 'yes' to create the contract, or 'no' to cancel.</p>",
            "CONFIRMATION"
        );
    }
    
    /**
     * Process confirmation step
     */
    private WorkflowResult processConfirmationStep(String userInput, ContractCreationSession session) {
        String normalized = SpellCorrector.normalizeYesNo(userInput);
        if ("YES".equals(normalized)) {
            // Create the contract
            return createContract(session);
        } else if ("NO".equals(normalized)) {
            return cancelSession(session.getSessionId());
        } else {
            return WorkflowResult.error("Invalid confirmation response",
                Arrays.asList("Please respond with Yes or No."));
        }
    }
    
    /**
     * Create the contract
     */
    private WorkflowResult createContract(ContractCreationSession session) {
        try {
            // Prepare contract data
            Map<String, Object> contractData = new HashMap<>(session.getCollectedData());
            
            // Add session metadata
            contractData.put("sessionId", session.getSessionId());
            contractData.put("createdAt", System.currentTimeMillis());
            
            // Call contract creation service
            String result = createContractInSystem(contractData);
            
            if (result != null && result.contains("success")) {
                // Extract contract number from result
                String contractNumber = extractContractNumberFromResult(result);
                
                session.setStatus("completed");
                session.setCurrentStep(STEP_COMPLETE);
                
                HashMap<String, Object> contractMap = new HashMap<String, Object>();
                contractMap.put("contractNumber", contractNumber);
                // --- Checklist Extension ---
                session.setChecklistPending(true);
                session.setWaitingForUserInput(true);
                session.resetChecklist();
                return WorkflowResult.success(
                    "<h4>Contract Created Successfully!</h4>" +
                    "<p><b>Contract Number:</b> " + contractNumber + "</p>" +
                    "<p><b>Account:</b> " + contractData.get("accountNumber") + "</p>" +
                    "<p><b>Name:</b> " + contractData.get("contractName") + "</p>" +
                    "<p>The contract has been created and is now active in the system.</p>" +
                    "<p>Would you like to create a checklist for this contract? (Yes/No)</p>",
                    "CHECKLIST_PROMPT",
                    contractMap
                );
                // --- End Checklist Extension ---
            } else {
                return WorkflowResult.error("Failed to create contract",
                    Arrays.asList("System error: " + (result != null ? result : "Unknown error")));
            }
            
        } catch (Exception e) {
            return WorkflowResult.error("Error creating contract: " + e.getMessage(),
                Arrays.asList(e.getMessage()));
        }
    }
    
    /**
     * Cancel session
     */
    private WorkflowResult cancelSession(String sessionId) {
        ContractCreationSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setStatus("cancelled");
            session.setCurrentStep(STEP_CANCELLED);
        }
        
        return WorkflowResult.cancelled(
            "<p><b>Contract Creation Cancelled</b></p>" +
            "<p>The contract creation process has been cancelled.</p>" +
            "<p>You can start a new contract creation anytime by saying 'create contract' with an account number.</p>"
        );
    }
    
    /**
     * Get or create session
     */
    private ContractCreationSession getOrCreateSession(String sessionId) {
        ContractCreationSession session = activeSessions.get(sessionId);
        if (session == null) {
            session = startSession(sessionId);
        }
        return session;
    }
    
    /**
     * Check if input is a cancellation request
     */
    private boolean isCancellationRequest(String userInput) {
        if (userInput == null) return false;
        String lowerInput = userInput.toLowerCase();
        
        return lowerInput.contains("cancel") || 
               lowerInput.contains("stop") || 
               lowerInput.contains("quit") || 
               lowerInput.contains("exit") ||
               lowerInput.contains("abort") ||
               lowerInput.contains("never mind");
    }
    
    /**
     * Extract account number from input
     */
    private String extractAccountNumber(String userInput) {
        if (userInput == null) return null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(ACCOUNT_NUMBER_PATTERN);
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }
    
    /**
     * Extract contract name from input
     */
    private String extractContractName(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        
        // Remove common prefixes and clean up
        String cleaned = userInput.trim();
        cleaned = cleaned.replaceAll("(?i)^(contract name|name|title):\\s*", "");
        cleaned = cleaned.replaceAll("(?i)\\s*(contract|agreement|deal)$", "");
        
        return cleaned.trim();
    }
    
    /**
     * Extract contract title from input
     */
    private String extractContractTitle(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        
        // Remove common prefixes and clean up
        String cleaned = userInput.trim();
        cleaned = cleaned.replaceAll("(?i)^(contract title|title|name):\\s*", "");
        cleaned = cleaned.replaceAll("(?i)\\s*(contract|agreement|deal)$", "");
        
        return cleaned.trim();
    }
    
    /**
     * Extract contract description from input
     */
    private String extractContractDescription(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        
        // Remove common prefixes and clean up
        String cleaned = userInput.trim();
        cleaned = cleaned.replaceAll("(?i)^(contract description|description|desc|details):\\s*", "");
        
        return cleaned.trim();
    }
    
    /**
     * Extract yes/no setting from input
     */
    private String extractYesNoSetting(String lowerInput, String... keywords) {
        for (String keyword : keywords) {
            if (lowerInput.contains(keyword)) {
                // Look for yes/no after the keyword
                int keywordIndex = lowerInput.indexOf(keyword);
                String afterKeyword = lowerInput.substring(keywordIndex + keyword.length());
                
                if (afterKeyword.matches(".*\\b(yes|y)\\b.*")) {
                    return "YES";
                } else if (afterKeyword.matches(".*\\b(no|n|skip|default)\\b.*")) {
                    return "NO";
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract date from input
     */
    private String extractDate(String lowerInput, String... keywords) {
        for (String keyword : keywords) {
            if (lowerInput.contains(keyword)) {
                // Look for date pattern after the keyword
                int keywordIndex = lowerInput.indexOf(keyword);
                String afterKeyword = lowerInput.substring(keywordIndex + keyword.length());
                
                java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(DATE_PATTERN);
                java.util.regex.Matcher matcher = datePattern.matcher(afterKeyword);
                
                if (matcher.find()) {
                    String dateStr = matcher.group();
                    // Validate date format
                    try {
                        LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                        return dateStr;
                    } catch (DateTimeParseException e) {
                        // Invalid date format
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validate account number
     */
    private boolean validateAccountNumber(String accountNumber) {
        if (accountNumber == null || !accountNumber.matches(ACCOUNT_NUMBER_PATTERN)) {
            return false;
        }
        
        // Use NLPUserActionHandler for business validation
        try {
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
            return handler.isCustomerNumberValid(accountNumber);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Create contract in system
     */
    private String createContractInSystem(Map<String, Object> contractData) {
        try {
            // Use NLPUserActionHandler to create contract
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
            return handler.createContractByBOT(convertToStringMap(contractData));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Extract contract number from result
     */
    private String extractContractNumberFromResult(String result) {
        if (result == null) return "UNKNOWN";
        
        // Look for contract number pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{6}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Format step message
     */
    private String formatStepMessage(String field, String value, String status) {
        return "<p><b>" + field + ":</b> " + value + " " + status + "</p>";
    }
    
    /**
     * Format contract summary
     */
    private String formatContractSummary(ContractCreationSession session) {
        Map<String, Object> data = session.getCollectedData();
        
        StringBuilder summary = new StringBuilder();
        summary.append("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0;'>");
        summary.append("<h5>Contract Summary:</h5>");
        summary.append("<p><b>Account Number:</b> ").append(data.get("accountNumber")).append("</p>");
        summary.append("<p><b>Contract Name:</b> ").append(data.get("contractName")).append("</p>");
        summary.append("<p><b>Contract Title:</b> ").append(data.get("contractTitle")).append("</p>");
        summary.append("<p><b>Description:</b> ").append(data.get("contractDescription")).append("</p>");
        summary.append("<p><b>Price List:</b> ").append(data.getOrDefault("priceList", "NO")).append("</p>");
        summary.append("<p><b>EM Required:</b> ").append(data.getOrDefault("isEM", "NO")).append("</p>");
        summary.append("<p><b>Effective Date:</b> ").append(data.getOrDefault("effectiveDate", "Today")).append("</p>");
        summary.append("<p><b>Expiration Date:</b> ").append(data.getOrDefault("expirationDate", "2 years from today")).append("</p>");
        if (data.containsKey("priceExpirationDate")) {
            summary.append("<p><b>Price Expiration:</b> ").append(data.get("priceExpirationDate")).append("</p>");
        }
        summary.append("</div>");
        return summary.toString();
    }
    
    /**
     * Convert Map<String, Object> to Map<String, String>
     */
    private Map<String, String> convertToStringMap(Map<String, Object> data) {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return stringMap;
    }
    
    /**
     * Get session by ID
     */
    public ContractCreationSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * End session
     */
    public void endSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    /**
     * Clean up expired sessions
     */
    private void cleanupExpiredSessions() {
        List<String> expiredSessions = new ArrayList<>();
        
        for (Map.Entry<String, ContractCreationSession> entry : activeSessions.entrySet()) {
            if (entry.getValue().isTimedOut()) {
                expiredSessions.add(entry.getKey());
            }
        }
        
        for (String sessionId : expiredSessions) {
            activeSessions.remove(sessionId);
        }
    }
    
    /**
     * Start cleanup thread
     */
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
     * Generate conversation ID
     */
    private static String generateConversationId() {
        return "contract_creation_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // --- Checklist Extension ---
    // Add a new method to handle checklist flow after contract creation
    public WorkflowResult processChecklistStep(String userInput, ContractCreationSession session) {
        String normalized = SpellCorrector.normalizeYesNo(userInput);
        if (session.isChecklistPending()) {
            // If waiting for yes/no to start checklist
            if (session.getChecklistFieldIndex() == 0 && session.getChecklistData().isEmpty()) {
                if ("YES".equals(normalized)) {
                    // Start checklist collection
                    session.setWaitingForUserInput(true);
                    return promptNextChecklistField(session);
                } else if ("NO".equals(normalized) || "N".equals(normalized) || "Nope".equals(normalized)) {
                    // User declined checklist, mark as complete
                    session.setChecklistPending(false);
                    session.setWaitingForUserInput(false);
                    session.setStatus("completed");
                    return WorkflowResult.success(
                        "<p>Checklist creation skipped. Contract process is now fully complete.</p>",
                        STEP_COMPLETE
                    );
                } else {
                    // Invalid response, re-prompt
                    return WorkflowResult.success(
                        "<p>Please respond with Yes or No: Would you like to create a checklist for this contract?</p>",
                        "CHECKLIST_PROMPT"
                    );
                }
            } else {
                // Collect checklist fields sequentially
                List<String> fields = session.getChecklistFields();
                int idx = session.getChecklistFieldIndex();
                if (idx < fields.size()) {
                    String field = fields.get(idx);
                    String value = userInput.trim();
                    // Validate input
                    String validation = validateChecklistField(field, value);
                    if (validation != null) {
                        // Invalid, re-prompt
                        return WorkflowResult.success(
                            "<p>" + validation + "</p>" + promptTextForChecklistField(field),
                            "CHECKLIST_FIELD_" + field
                        );
                    }
                    // Store value
                    session.getChecklistData().put(field, value);
                    session.incrementChecklistFieldIndex();
                    // If more fields, prompt next
                    if (session.getChecklistFieldIndex() < fields.size()) {
                        return promptNextChecklistField(session);
                    } else {
                        // All checklist fields collected
                        session.setChecklistPending(false);
                        session.setWaitingForUserInput(false);
                        session.setStatus("completed");
                        return WorkflowResult.success(
                            "<p>Checklist completed and saved. Contract process is now fully complete.</p>",
                            STEP_COMPLETE
                        );
                    }
                }
            }
        }
        // Not in checklist mode, fallback
        return WorkflowResult.success("<p>Checklist not active.</p>", STEP_COMPLETE);
    }
    private WorkflowResult promptNextChecklistField(ContractCreationSession session) {
        List<String> fields = session.getChecklistFields();
        int idx = session.getChecklistFieldIndex();
        if (idx < fields.size()) {
            String field = fields.get(idx);
            String prompt = promptTextForChecklistField(field);
            return WorkflowResult.success(prompt, "CHECKLIST_FIELD_" + field);
        }
        return WorkflowResult.success("<p>Checklist complete.</p>", STEP_COMPLETE);
    }
    private String promptTextForChecklistField(String field) {
        switch (field) {
            case "EFFECTIVE_DATE": return "Please provide the Effective Date (MM/dd/yy):";
            case "SYSTEM_DATE": return "Please provide the System Date (MM/dd/yy):";
            case "EXPIRATION_DATE": return "Please provide the Expiration Date (MM/dd/yy):";
            case "PARTS_EXPIRATION_DATE": return "Please provide the Parts Expiration Date (MM/dd/yy):";
            case "QUARTER": return "Please provide the Quarter (1/2/3/4):";
            case "FLOW_DATE": return "Please provide the Flow Date (MM/dd/yy):";
            default: return "Please provide the value for " + field + ":";
        }
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
    // --- End Checklist Extension ---
} 