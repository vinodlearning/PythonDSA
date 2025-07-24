package com.oracle.view.source;

import com.oracle.view.deep.ContractsModel;
import com.oracle.view.deep.TestQueries;
import com.oracle.view.source.ActionTypeDataProvider;
import com.oracle.view.source.TableColumnConfig;
import com.oracle.view.source.UserActionResponse;
import com.oracle.view.source.StandardJSONProcessor;
import com.oracle.view.source.WordDatabase;
import com.oracle.view.source.BCCTChatBotUtility;
import com.oracle.view.source.ConversationalFlowManager;
import com.oracle.view.source.ChatMessage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ModularNLPUserActionHandler - Enhanced modular NLP handler using new architecture
 * 
 * This handler uses the new modular NLP system:
 * - NLPQueryClassifier for query classification
 * - Specialized processors (FailedPartsProcessor, PartsProcessor, ContractProcessor, HelpProcessor)
 * - SpellCorrector for input normalization
 * - StandardJSONProcessor for data processing and JSON generation
 * - TableColumnConfig for column management
 * - WordDatabase for vocabulary and lemmatization
 */
public class ModularNLPUserActionHandler {

    private static final int DEFAULT_SCREEN_WIDTH = 400; // Default screen width in pixels
    private static final int TABULAR_THRESHOLD = 3; // If more than 3 attributes, use tabular format
   
    // Centralized table column configuration
    private static final TableColumnConfig TABLE_CONFIG = TableColumnConfig.getInstance();

    // Modular NLP components
    private NLPQueryClassifier queryClassifier;
    private SpellCorrector spellCorrector;
    private FailedPartsProcessor failedPartsProcessor;
    private PartsProcessor partsProcessor;
    private ContractProcessor contractProcessor;
    private HelpProcessor helpProcessor;
    
    // Legacy components for data processing
    private StandardJSONProcessor standardProcessor;
    private ActionTypeDataProvider dataProvider;
    private ContractsModel contractsModel;
    private ConversationalFlowManager flowManager;
    private ContractCreationIntegration contractCreationIntegration;
    
    public ModularNLPUserActionHandler() {
        // Initialize modular NLP components
        this.queryClassifier = new NLPQueryClassifier();
        this.spellCorrector = new SpellCorrector();
        this.failedPartsProcessor = new FailedPartsProcessor();
        this.partsProcessor = new PartsProcessor();
        this.contractProcessor = new ContractProcessor();
        this.helpProcessor = new HelpProcessor();
        
        // Initialize legacy components for data processing
        this.standardProcessor = new StandardJSONProcessor();
        this.dataProvider = new ActionTypeDataProvider();
        this.contractsModel = new ContractsModel();
        this.flowManager = ConversationalFlowManager.getInstance();
        this.contractCreationIntegration = new ContractCreationIntegration();
    }
    
    /**
     * Enhanced input preprocessing using WordDatabase
     */
    private String preprocessInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return userInput;
        }
        
        String[] words = userInput.split("\\s+");
        StringBuilder processedInput = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String processedWord = WordDatabase.normalizeWord(word);
            
            if (processedWord != null) {
                processedInput.append(processedWord);
            } else {
                processedInput.append(word);
            }
            
            if (i < words.length - 1) {
                processedInput.append(" ");
            }
        }
        
        return processedInput.toString();
    }
    
    /**
     * Main processing method using modular NLP architecture
     * This is the primary method that should be called from the UI
     */
    public String processUserInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return createErrorResponse("Empty input provided");
        }
        
        try {
            // Step 1: Preprocess input using WordDatabase
            String preprocessedInput = preprocessInput(userInput);
            
            // Step 2: Spell correction using SpellCorrector
            String correctedInput = spellCorrector.correct(preprocessedInput);
            double correctionConfidence = spellCorrector.getCorrectionConfidence(preprocessedInput, correctedInput);
            
            // Step 3: Query classification using NLPQueryClassifier
            NLPQueryClassifier.QueryResult classificationResult = queryClassifier.processQuery(userInput);
            
            // Step 4: Route to appropriate processor based on query type
            NLPQueryClassifier.QueryResult processedResult = routeToProcessor(
                userInput, correctedInput, preprocessedInput, classificationResult
            );
            
            // Step 5: Process data using StandardJSONProcessor (legacy logic)
            String finalResponse = processDataWithLegacyLogic(processedResult, userInput);
            
            return finalResponse;
            
        } catch (Exception e) {
            return createErrorResponse("Processing error: " + e.getMessage());
        }
    }
    
    /**
     * Process user input and return JSON response - for test compatibility
     * This method is expected by the test file
     */
    public String processUserInputJSONResponse(String userInput) {
        return processUserInput(userInput);
    }
    
    /**
     * Process user input with session ID for conversational flow - for test compatibility
     * This method is expected by the test file
     */
    public String processUserInputJSONResponse(String userInput, String sessionId) {
        return processConversationalInput(userInput, sessionId);
    }
    
    /**
     * Route query to appropriate specialized processor
     */
    private NLPQueryClassifier.QueryResult routeToProcessor(
            String originalInput, 
            String correctedInput, 
            String normalizedInput,
            NLPQueryClassifier.QueryResult classificationResult) {
        
        String queryType = classificationResult.metadata.queryType;
        
        switch (queryType.toUpperCase()) {
            case "FAILED_PARTS":
                return failedPartsProcessor.process(originalInput, correctedInput, normalizedInput);
                
            case "PARTS":
                return partsProcessor.process(originalInput, correctedInput, normalizedInput);
                
            case "CONTRACT":
                return contractProcessor.process(originalInput, correctedInput, normalizedInput);
                
            case "HELP":
                return helpProcessor.process(originalInput, correctedInput, normalizedInput);
                
            default:
                // Fallback to contract processor for unknown types
                return contractProcessor.process(originalInput, correctedInput, normalizedInput);
        }
    }
    
    /**
     * Process data using legacy StandardJSONProcessor logic
     * This maintains compatibility with existing data processing and JSON generation
     */
    private String processDataWithLegacyLogic(NLPQueryClassifier.QueryResult processedResult, String userInput) {
        try {
            // Convert NLPQueryClassifier.QueryResult to StandardJSONProcessor.QueryResult
            StandardJSONProcessor.QueryResult standardResult = convertToStandardResult(processedResult);
            
            // Use StandardJSONProcessor's processUserRequest method for data processing
            return standardProcessor.processUserRequest(userInput);
            
        } catch (Exception e) {
            // Fallback to direct processing if conversion fails
            return standardProcessor.processQuery(userInput);
        }
    }
    
    /**
     * Convert NLPQueryClassifier.QueryResult to StandardJSONProcessor.QueryResult
     */
    private StandardJSONProcessor.QueryResult convertToStandardResult(NLPQueryClassifier.QueryResult nlpResult) {
        StandardJSONProcessor.QueryResult standardResult = new StandardJSONProcessor.QueryResult();
        
        // Convert input tracking
        if (nlpResult.inputTracking != null) {
            standardResult.inputTracking = new StandardJSONProcessor.InputTrackingResult(
                nlpResult.inputTracking.originalInput,
                nlpResult.inputTracking.correctedInput,
                nlpResult.inputTracking.correctionConfidence
            );
        }
        
        // Convert header
        if (nlpResult.header != null) {
            standardResult.header = new StandardJSONProcessor.Header();
            standardResult.header.contractNumber = nlpResult.header.contractNumber;
            standardResult.header.partNumber = nlpResult.header.partNumber;
            standardResult.header.customerNumber = nlpResult.header.customerNumber;
            standardResult.header.customerName = nlpResult.header.customerName;
            standardResult.header.createdBy = nlpResult.header.createdBy;
        }
        
        // Convert metadata
        if (nlpResult.metadata != null) {
            standardResult.metadata = new StandardJSONProcessor.QueryMetadata(
                nlpResult.metadata.queryType,
                nlpResult.metadata.actionType,
                nlpResult.metadata.processingTimeMs
            );
        }
        
        // Convert entities
        if (nlpResult.entities != null) {
            standardResult.entities = new ArrayList<>();
            for (NLPQueryClassifier.EntityFilter nlpEntity : nlpResult.entities) {
                StandardJSONProcessor.EntityFilter standardEntity = new StandardJSONProcessor.EntityFilter(
                    nlpEntity.attribute,
                    nlpEntity.operation,
                    nlpEntity.value,
                    nlpEntity.source
                );
                standardResult.entities.add(standardEntity);
            }
        }
        
        // Convert display entities
        if (nlpResult.displayEntities != null) {
            standardResult.displayEntities = new ArrayList<>(nlpResult.displayEntities);
        }
        
        // Convert errors
        if (nlpResult.errors != null) {
            standardResult.errors = new ArrayList<>();
            for (NLPQueryClassifier.ValidationError nlpError : nlpResult.errors) {
                StandardJSONProcessor.ValidationError standardError = new StandardJSONProcessor.ValidationError(
                    nlpError.code,
                    nlpError.message,
                    nlpError.severity
                );
                standardResult.errors.add(standardError);
            }
        }
        
        return standardResult;
    }
    
    /**
     * Create error response in JSON format
     */
    private String createErrorResponse(String errorMessage) {
        return "{\n" +
               "  \"dataProviderResponse\": {\n" +
               "    \"status\": \"error\",\n" +
               "    \"message\": \"" + errorMessage + "\",\n" +
               "    \"data\": [],\n" +
               "    \"processingTime\": 0\n" +
               "  }\n" +
               "}";
    }
    
    /**
     * Enhanced query type detection using WordDatabase and ContractsModel
     */
    private String determineQueryType(String userInput) {
        String lowerInput = userInput.toLowerCase();
        
        // Check for failed parts queries
        if (lowerInput.contains("failed parts") || lowerInput.contains("failed part") ||
            lowerInput.contains("error parts") || lowerInput.contains("error part")) {
            return "FAILED_PARTS";
        }
        
        // Check for parts queries
        if (lowerInput.contains("parts") || lowerInput.contains("part ")) {
            return "PARTS";
        }
        
        // Check for contract queries
        if (lowerInput.contains("contract") || lowerInput.contains("contracts")) {
            return "CONTRACT";
        }
        
        // Check for help queries
        if (lowerInput.contains("help") || lowerInput.contains("how") || 
            lowerInput.contains("guide") || lowerInput.contains("instructions")) {
            return "HELP";
        }
        
        // Default to contract queries
        return "CONTRACT";
    }
    
    /**
     * Validate column names using TableColumnConfig
     */
    private boolean isValidColumn(String columnName, String tableType) {
        return TABLE_CONFIG.isValidColumn(tableType, columnName);
    }
    
    /**
     * Get displayable columns for a table type
     */
    private Set<String> getDisplayableColumns(String tableType) {
        return TABLE_CONFIG.getDisplayableColumns(tableType);
    }
    
    /**
     * Get filterable columns for a table type
     */
    private Set<String> getFilterableColumns(String tableType) {
        return TABLE_CONFIG.getFilterableColumns(tableType);
    }
    
    /**
     * Get business term mapping for a table type
     */
    private String getColumnForBusinessTerm(String tableType, String businessTerm) {
        return TABLE_CONFIG.getColumnForBusinessTerm(tableType, businessTerm);
    }
    
    /**
     * Get field synonym mapping for a table type
     */
    private String getColumnForSynonym(String tableType, String synonym) {
        return TABLE_CONFIG.getColumnForSynonym(tableType, synonym);
    }
    
    /**
     * Enhanced entity extraction with TableColumnConfig validation
     */
    private List<StandardJSONProcessor.EntityFilter> extractEntitiesWithValidation(String userInput, String tableType) {
        List<StandardJSONProcessor.EntityFilter> entities = new ArrayList<>();
        
        // Extract entities using StandardJSONProcessor logic
        String jsonResult = standardProcessor.processQuery(userInput);
        StandardJSONProcessor.QueryResult queryResult = standardProcessor.parseJSONToObject(jsonResult);
        
        if (queryResult.entities != null) {
            for (StandardJSONProcessor.EntityFilter entity : queryResult.entities) {
                // Validate column using TableColumnConfig
                if (isValidColumn(entity.attribute, tableType)) {
                    entities.add(entity);
                }
            }
        }
        
        return entities;
    }
    
    /**
     * Enhanced display entity determination with TableColumnConfig
     */
    private List<String> determineDisplayEntitiesWithValidation(String userInput, String tableType) {
        List<String> displayEntities = new ArrayList<>();
        
        // Get displayable columns for the table type
        Set<String> displayableColumns = getDisplayableColumns(tableType);
        
        // Extract display entities using StandardJSONProcessor logic
        String jsonResult = standardProcessor.processQuery(userInput);
        StandardJSONProcessor.QueryResult queryResult = standardProcessor.parseJSONToObject(jsonResult);
        
        if (queryResult.displayEntities != null) {
            for (String entity : queryResult.displayEntities) {
                // Validate column using TableColumnConfig
                if (displayableColumns.contains(entity)) {
                    displayEntities.add(entity);
                }
            }
        }
        
        return displayEntities;
    }
    
    /**
     * Process conversational flow using ConversationalFlowManager
     */
    public String processConversationalInput(String userInput, String sessionId) {
        try {
            // Check if this is a conversational continuation
            if (flowManager.isFollowUpResponse(userInput, sessionId)) {
                ChatMessage chatMessage = flowManager.processUserInput(userInput, sessionId);
                return chatMessage.getMessage(); // Convert ChatMessage to String
            } else {
                // Process as new query
                return processUserInput(userInput);
            }
        } catch (Exception e) {
            return createErrorResponse("Conversational processing error: " + e.getMessage());
        }
    }
    
    /**
     * Get conversation session information
     */
    public ConversationalFlowManager.ConversationState getConversationState(String sessionId) {
        try {
            return flowManager.getConversationState(sessionId);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * End conversation session
     */
    public void endConversation(String sessionId) {
        try {
            // ConversationalFlowManager doesn't have endConversation method
            // We'll implement a simple cleanup or leave it empty for now
            // The session will naturally expire or be cleaned up by the manager
        } catch (Exception e) {
            // Log error but don't throw
        }
    }
    
    /**
     * Health check for all components
     */
    public boolean isHealthy() {
        try {
            // Check if all required components are initialized
            return queryClassifier != null && 
                   spellCorrector != null && 
                   failedPartsProcessor != null && 
                   partsProcessor != null && 
                   contractProcessor != null && 
                   helpProcessor != null && 
                   standardProcessor != null && 
                   dataProvider != null && 
                   contractsModel != null && 
                   flowManager != null && 
                   contractCreationIntegration != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get component status for debugging
     */
    public Map<String, String> getComponentStatus() {
        Map<String, String> status = new HashMap<>();
        
        status.put("queryClassifier", queryClassifier != null ? "OK" : "NULL");
        status.put("spellCorrector", spellCorrector != null ? "OK" : "NULL");
        status.put("failedPartsProcessor", failedPartsProcessor != null ? "OK" : "NULL");
        status.put("partsProcessor", partsProcessor != null ? "OK" : "NULL");
        status.put("contractProcessor", contractProcessor != null ? "OK" : "NULL");
        status.put("helpProcessor", helpProcessor != null ? "OK" : "NULL");
        status.put("standardProcessor", standardProcessor != null ? "OK" : "NULL");
        status.put("dataProvider", dataProvider != null ? "OK" : "NULL");
        status.put("contractsModel", contractsModel != null ? "OK" : "NULL");
        status.put("flowManager", flowManager != null ? "OK" : "NULL");
        status.put("contractCreationIntegration", contractCreationIntegration != null ? "OK" : "NULL");
        status.put("tableColumnConfig", TABLE_CONFIG != null ? "OK" : "NULL");
        
        return status;
    }
} 