package com.oracle.view.source;

import java.util.*;
import java.time.Instant;

/**
 * Contract Creation Workflow Manager
 * 
 * Handles multi-step contract creation workflows with:
 * - Centralized attribute configuration using TableColumnConfig
 * - Chain validation and session management
 * - Progressive data collection
 * - Business rule validation
 */
public class ContractCreationWorkflowManager {
    
    private static volatile ContractCreationWorkflowManager instance;
    private final TableColumnConfig tableConfig;
    private final ContractCreationConfig contractConfig;
    private final Map<String, ContractCreationSession> activeSessions = new HashMap<>();
    
    // Workflow configuration
    private static final String WORKFLOW_TYPE_CONTRACT_CREATION = "CONTRACT_CREATION";
    private static final long SESSION_TIMEOUT_MS = 600000; // 10 minutes
    
    private ContractCreationWorkflowManager() {
        this.tableConfig = TableColumnConfig.getInstance();
        this.contractConfig = ContractCreationConfig.getInstance();
        startCleanupThread();
    }
    
    public static ContractCreationWorkflowManager getInstance() {
        if (instance == null) {
            synchronized (ContractCreationWorkflowManager.class) {
                if (instance == null) {
                    instance = new ContractCreationWorkflowManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Contract Creation Session - tracks the state of contract creation workflow
     */
    public static class ContractCreationSession {
        private String sessionId;
        private String workflowId;
        private String currentStep;
        private String status; // "in_progress", "complete", "cancelled", "timeout"
        private Map<String, String> collectedData;
        private List<String> validationErrors;
        private Instant createdAt;
        private Instant lastActivity;
        private String originalQuery;
        private List<String> requiredAttributes;
        private List<String> completedSteps;
        private Map<String, Object> context;
        
        public ContractCreationSession(String sessionId, String originalQuery, ContractCreationConfig config) {
            this.sessionId = sessionId;
            this.workflowId = "workflow_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
            this.originalQuery = originalQuery;
            this.status = "in_progress";
            this.collectedData = new HashMap<>();
            this.validationErrors = new ArrayList<>();
            this.createdAt = Instant.now();
            this.lastActivity = Instant.now();
            this.completedSteps = new ArrayList<>();
            this.context = new HashMap<>();
            
            // Initialize required attributes from configuration
            this.requiredAttributes = config.getRequiredAttributes();
            this.currentStep = config.getRequiredAttributes().isEmpty() ? null : config.getRequiredAttributes().get(0);
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getWorkflowId() { return workflowId; }
        public String getCurrentStep() { return currentStep; }
        public String getStatus() { return status; }
        public Map<String, String> getCollectedData() { return collectedData; }
        public List<String> getValidationErrors() { return validationErrors; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastActivity() { return lastActivity; }
        public String getOriginalQuery() { return originalQuery; }
        public List<String> getRequiredAttributes() { return requiredAttributes; }
        public List<String> getCompletedSteps() { return completedSteps; }
        public Map<String, Object> getContext() { return context; }
        
        public void setCurrentStep(String step) {
            this.currentStep = step;
            this.lastActivity = Instant.now();
        }
        
        public void setStatus(String status) {
            this.status = status;
            this.lastActivity = Instant.now();
        }
        
        public void addCollectedData(String key, String value) {
            this.collectedData.put(key, value);
            this.lastActivity = Instant.now();
        }
        
        public void addValidationError(String error) {
            this.validationErrors.add(error);
        }
        
        public void addCompletedStep(String step) {
            if (!this.completedSteps.contains(step)) {
                this.completedSteps.add(step);
            }
        }
        
        public void addContext(String key, Object value) {
            this.context.put(key, value);
        }
        
        public boolean isTimedOut() {
            return Instant.now().isAfter(lastActivity.plusMillis(SESSION_TIMEOUT_MS));
        }
        
        public boolean isComplete() {
            return "complete".equals(status);
        }
        
        public boolean isCancelled() {
            return "cancelled".equals(status);
        }
        
        public boolean hasAllRequiredData() {
            return completedSteps.size() >= requiredAttributes.size();
        }
    }
    
    /**
     * Workflow Result - represents the result of processing workflow input
     */
    public static class WorkflowResult {
        private final boolean isComplete;
        private final boolean requiresInput;
        private final String message;
        private final String nextStep;
        private final ContractCreationSession session;
        private final String error;
        private final boolean isChainBreak;
        
        private WorkflowResult(boolean isComplete, boolean requiresInput, String message, 
                             String nextStep, ContractCreationSession session, String error, boolean isChainBreak) {
            this.isComplete = isComplete;
            this.requiresInput = requiresInput;
            this.message = message;
            this.nextStep = nextStep;
            this.session = session;
            this.error = error;
            this.isChainBreak = isChainBreak;
        }
        
        public static WorkflowResult complete(ContractCreationSession session, String message) {
            return new WorkflowResult(true, false, message, null, session, null, false);
        }
        
        public static WorkflowResult requiresInput(String message, String nextStep, ContractCreationSession session) {
            return new WorkflowResult(false, true, message, nextStep, session, null, false);
        }
        
        public static WorkflowResult error(String error) {
            return new WorkflowResult(false, false, null, null, null, error, false);
        }
        
        public static WorkflowResult chainBreak(String message) {
            return new WorkflowResult(false, false, message, null, null, null, true);
        }
        
        // Getters
        public boolean isComplete() { return isComplete; }
        public boolean requiresInput() { return requiresInput; }
        public String getMessage() { return message; }
        public String getNextStep() { return nextStep; }
        public ContractCreationSession getSession() { return session; }
        public String getError() { return error; }
        public boolean isChainBreak() { return isChainBreak; }
    }
    
    /**
     * Process contract creation workflow input
     */
    public WorkflowResult processContractCreationInput(String userInput, String sessionId) {
        try {
            // Clean up expired sessions
            cleanupExpiredSessions();
            // Check if this is a new contract creation request
            if (isNewContractCreationRequest(userInput)) {
                return startNewContractCreation(userInput, sessionId);
            }
            // Check if this is a follow-up to existing contract creation session
            ContractCreationSession existingSession = getActiveSession(sessionId);
            if (existingSession != null && existingSession.getStatus().equals("in_progress")) {
                return processFollowUpInput(userInput, existingSession);
            }
            // Check if this input might be related to a previous contract creation session
            ContractCreationSession previousSession = findRelatedSession(userInput, sessionId);
            if (previousSession != null) {
                return handleChainResumption(userInput, previousSession);
            }
            // This is not related to contract creation - return chain break
            return WorkflowResult.chainBreak("Contract creation workflow not active. Please start a new contract creation request.");
        } catch (Exception e) {
            return WorkflowResult.error("Error processing contract creation input: " + e.getMessage());
        }
    }
    
    /**
     * Check if input is a new contract creation request
     */
    private boolean isNewContractCreationRequest(String userInput) {
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("create") && lowerInput.contains("contract") ||
               lowerInput.contains("new contract") ||
               lowerInput.contains("start contract") ||
               lowerInput.matches(".*\\b(create|new|start)\\b.*\\bcontract\\b.*");
    }
    
    /**
     * Start new contract creation workflow
     */
    private WorkflowResult startNewContractCreation(String userInput, String sessionId) {
        // Cancel any existing session for this user
        cancelExistingSession(sessionId);
        
        // Create new contract creation session
        ContractCreationSession session = new ContractCreationSession(sessionId, userInput, contractConfig);
        activeSessions.put(sessionId, session);
        
        // Get the first required step
        String firstStep = session.getCurrentStep();
        String prompt = getStepPrompt(firstStep);
        
        return WorkflowResult.requiresInput(prompt, firstStep, session);
    }
    
    /**
     * Process follow-up input for existing session
     */
    private WorkflowResult processFollowUpInput(String userInput, ContractCreationSession session) {
        String currentStep = session.getCurrentStep();
        
        // Validate input for current step
        String validationResult = validateStepInput(userInput, currentStep);
        if (validationResult != null) {
            return WorkflowResult.requiresInput(validationResult, currentStep, session);
        }
        
        // Extract and store the data
        String extractedValue = extractStepValue(userInput, currentStep);
        session.addCollectedData(currentStep, extractedValue);
        session.addCompletedStep(currentStep);
        
        // Check if all required data is collected
        if (session.hasAllRequiredData()) {
            // Complete the workflow
            session.setStatus("complete");
            return WorkflowResult.complete(session, "Contract creation data complete. Processing contract creation...");
        }
        
        // Move to next step
        String nextStep = getNextRequiredStep(session);
        session.setCurrentStep(nextStep);
        String nextPrompt = getStepPrompt(nextStep);
        
        return WorkflowResult.requiresInput(nextPrompt, nextStep, session);
    }
    
    /**
     * Handle chain resumption (user returning to previous contract creation)
     */
    private WorkflowResult handleChainResumption(String userInput, ContractCreationSession session) {
        // Check if the input is valid for the current step
        String currentStep = session.getCurrentStep();
        String validationResult = validateStepInput(userInput, currentStep);
        
        if (validationResult != null) {
            return WorkflowResult.requiresInput(validationResult, currentStep, session);
        }
        
        // Process the input
        return processFollowUpInput(userInput, session);
    }
    
    /**
     * Get required contract attributes from configuration
     */
    private List<String> getRequiredContractAttributes() {
        return contractConfig.getRequiredAttributes();
    }
    
    /**
     * Get next required step for a session
     */
    private String getNextRequiredStep(ContractCreationSession session) {
        return contractConfig.getNextRequiredAttribute(session.getCompletedSteps());
    }
    
    /**
     * Get next required step for new session
     */
    private String getNextRequiredStep() {
        List<String> requiredAttributes = contractConfig.getRequiredAttributes();
        return requiredAttributes.isEmpty() ? null : requiredAttributes.get(0);
    }
    
    /**
     * Get prompt for a specific step
     */
    private String getStepPrompt(String step) {
        return contractConfig.getAttributePrompt(step);
    }
    
    /**
     * Validate input for a specific step
     */
    private String validateStepInput(String userInput, String step) {
        return contractConfig.validateAttributeInput(step, userInput);
    }
    
    /**
     * Extract value for a specific step
     */
    private String extractStepValue(String userInput, String step) {
        return contractConfig.extractAttributeValue(step, userInput);
    }
    

    
    /**
     * Session management methods
     */
    private ContractCreationSession getActiveSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    private ContractCreationSession findRelatedSession(String userInput, String sessionId) {
        // Look for sessions that might be related to this input
        for (ContractCreationSession session : activeSessions.values()) {
            if (session.getSessionId().equals(sessionId) && 
                session.getStatus().equals("in_progress")) {
                return session;
            }
        }
        return null;
    }
    
    private void cancelExistingSession(String sessionId) {
        ContractCreationSession existing = activeSessions.get(sessionId);
        if (existing != null) {
            existing.setStatus("cancelled");
        }
    }
    
    /**
     * Cleanup expired sessions
     */
    private void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> entry.getValue().isTimedOut());
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
     * Get session information for debugging
     */
    public String getSessionInfo(String sessionId) {
        ContractCreationSession session = activeSessions.get(sessionId);
        if (session == null) {
            return "No active contract creation session found for: " + sessionId;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Contract Creation Session Info:\n");
        info.append("Session ID: ").append(session.getSessionId()).append("\n");
        info.append("Workflow ID: ").append(session.getWorkflowId()).append("\n");
        info.append("Status: ").append(session.getStatus()).append("\n");
        info.append("Current Step: ").append(session.getCurrentStep()).append("\n");
        info.append("Original Query: ").append(session.getOriginalQuery()).append("\n");
        info.append("Required Attributes: ").append(session.getRequiredAttributes()).append("\n");
        info.append("Completed Steps: ").append(session.getCompletedSteps()).append("\n");
        info.append("Collected Data: ").append(session.getCollectedData()).append("\n");
        info.append("Validation Errors: ").append(session.getValidationErrors()).append("\n");
        info.append("Created: ").append(session.getCreatedAt()).append("\n");
        info.append("Last Activity: ").append(session.getLastActivity()).append("\n");
        
        return info.toString();
    }
    
    /**
     * Clear session (for testing or user cancellation)
     */
    public void clearSession(String sessionId) {
        ContractCreationSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setStatus("cancelled");
        }
    }
    
    /**
     * Get all active sessions (for monitoring)
     */
    public Map<String, ContractCreationSession> getActiveSessions() {
        return new HashMap<>(activeSessions);
    }
} 