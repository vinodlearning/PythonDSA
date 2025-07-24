package com.oracle.view.source;


import com.oracle.view.deep.ContractsModel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import oracle.binding.OperationBinding;

/**
 * NLPUserActionHandler - Main interface for user interactions
 * Handles user input processing and routes to appropriate action methods
 */
public class NLPUserActionHandler {
    // Singleton instance
    private static volatile NLPUserActionHandler instance;
    private static final Object lock = new Object();

    private static final int DEFAULT_SCREEN_WIDTH = 400; // Default screen width in pixels
    private static final int TABULAR_THRESHOLD = 3; // If more than 3 attributes, use tabular format

    // Centralized table column configuration
    private static final TableColumnConfig TABLE_CONFIG = TableColumnConfig.getInstance();


    private NLPEntityProcessor nlpProcessor;
    private ActionTypeDataProvider dataProvider;
    private ContractsModel contractsModel;
    private ConversationalFlowManager flowManager;
    private ContractCreationIntegration contractCreationIntegration;

    /**
     * Private constructor for singleton pattern
     */
    private NLPUserActionHandler() {
        this.nlpProcessor = new NLPEntityProcessor();
        this.dataProvider = new ActionTypeDataProvider();
        this.contractsModel = new ContractsModel();
        this.flowManager = ConversationalFlowManager.getInstance();
        this.contractCreationIntegration = new ContractCreationIntegration();
    }

    /**
     * Get singleton instance (thread-safe)
     */
    public static NLPUserActionHandler getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new NLPUserActionHandler();
                }
            }
        }
        return instance;
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
     * Enhanced query type detection using WordDatabase and ContractsModel
     */
    private String detectQueryTypeEnhanced(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // Check for creation-related queries using WordDatabase
        if (WordDatabase.containsCreationWords(lowerInput)) {
            return "CONTRACTS";
        }

        // Check for imperative indicators
        if (WordDatabase.containsImperativeIndicators(lowerInput)) {
            // This might be a help request or command
            if (lowerInput.contains("help") || lowerInput.contains("how") ||
                WordDatabase.containsQuestionWords(lowerInput)) {
                return "HELP";
            }
        }

        // Use ContractsModel for additional business logic validation
        try {
            Map<String, Object> contractsResult = contractsModel.processQuery(userInput);
            if (contractsResult != null && contractsResult.containsKey("queryType")) {
                return (String) contractsResult.get("queryType");
            }
        } catch (Exception e) {
            // Fallback to existing logic
        }

        // Default to existing logic
        return null;
    }

    /**
     * Process query using ContractsModel for business validation
     */
    private Map<String, Object> processWithContractsModel(String userInput) {
        try {
            return contractsModel.processQuery(userInput);
        } catch (Exception e) {
            // Return null if ContractsModel fails, fallback to NLPEntityProcessor
            return null;
        }
    }

    // ============================================================================
    // THREE MAIN METHODS FOR UI INTEGRATION
    // ============================================================================


    /**
     * METHOD 2: Get structured JSON response with specified format
     * UI calls this method to get standardized JSON response
     */
    public String processUserInputJSONResponse(String userInput) {
        return processUserInputJSONResponse(userInput, null);
    }

    /**
     * METHOD 2: Get structured JSON response with session management
     * UI calls this method to get standardized JSON response with conversational flow
     */
    public String processUserInputJSONResponse(String userInput, String sessionId) {
        try {
            // Clean up old conversation states
            flowManager.cleanupOldStates();

            // NEW: Check if this is a contract creation request
            if (contractCreationIntegration.isContractCreationRelated(userInput)) {
                return contractCreationIntegration.processContractCreationInput(userInput, sessionId);
            }

            // FIX: If this is a follow-up response, handle it as such
            if (sessionId != null && flowManager.isFollowUpResponse(userInput, sessionId)) {
                return handleFollowUpResponse(userInput, sessionId);
            }

            // FIX: Check if this query needs conversational flow (only for incomplete part queries)
            // For simple queries like "show 100476", use the original processing
            if (sessionId != null && needsConversationalFlow(userInput)) {
                // Process with enhanced conversational flow
                ChatMessage chatMessage = flowManager.processUserInput(userInput, sessionId);

                // If this is a complete query, process it directly
                if (chatMessage.isCompleteQuery()) {
                    return processCompleteQueryDirectly(chatMessage);
                }

                // If this requires follow-up, return the follow-up message
                if (chatMessage.isRequiresFollowUp()) {
                    return createStructuredJSONResponseForFollowUp(chatMessage);
                }
            }

            // FIX: For all other queries, use the original processing (preserve existing functionality)
            // Preprocess input using WordDatabase
            String preprocessedInput = preprocessInput(userInput);

            // Validate with ContractsModel for business logic
            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);

            // Get QueryResult from NLPEntityProcessor
            NLPEntityProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);

            // CRITICAL BUSINESS RULE: Validate contract number for parts queries
            String contractValidationError = validatePartsQueryContractNumber(queryResult);
            if (contractValidationError != null) {
                return createErrorStructuredJSON(userInput, contractValidationError);
            }

            // Return structured JSON response
            return createStructuredJSONResponse(queryResult, contractsValidation);

        } catch (Exception e) {
            return createErrorStructuredJSON(userInput, "Error processing user input: " + e.getMessage());
        }
    }


    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Check if a query needs conversational flow (only for incomplete part queries)
     */
    private boolean needsConversationalFlow(String userInput) {
        if (userInput == null)
            return false;

        String lowerInput = userInput.toLowerCase();

        // Only use conversational flow for part queries that might be incomplete
        // Examples: "lead time for part ABC", "parts for XYZ", etc.
        boolean isPartQuery = lowerInput.contains("part") || lowerInput.contains("lead time");
        boolean hasContractNumber = lowerInput.matches(".*\\b\\d{6,}\\b.*"); // 6+ digit contract number

        // Use conversational flow only for part queries WITHOUT contract number
        return isPartQuery && !hasContractNumber;
    }

    /**
     * Get QueryResult from NLPEntityProcessor
     */
    private NLPEntityProcessor.QueryResult getQueryResultFromNLP(String userInput) throws Exception {
        // Use the original processQuery method to get proper inputTracking
        String jsonResponse = nlpProcessor.processQuery(userInput);

        // Parse the response to extract metadata
        return nlpProcessor.parseJSONToObject(jsonResponse);
    }

    /**
     * Handle follow-up responses in conversational flow
     */
    private String handleFollowUpResponse(String userInput, String sessionId) {
        try {
            // Process the contract number response and get a ChatMessage
            ChatMessage reconstructedMessage = flowManager.processContractNumberResponse(userInput, sessionId);
            if (reconstructedMessage == null) {
                return createErrorStructuredJSON(userInput, "Failed to process contract number response");
            }
            // If the reconstructedMessage is a bot error message, return it directly
            if (reconstructedMessage.isBot()) {
                return createErrorStructuredJSON(userInput, reconstructedMessage.getMessage());
            }
            // Process the reconstructed query as a complete query
            return processCompleteQueryDirectly(reconstructedMessage);
        } catch (Exception e) {
            return createErrorStructuredJSON(userInput, "Error processing follow-up response: " + e.getMessage());
        }
    }

    /**
     * Execute DataProvider action based on action type
     */
    private String executeDataProviderAction(NLPEntityProcessor.QueryResult queryResult) {
        return executeDataProviderAction(queryResult, null);
    }

    private String executeDataProviderAction(NLPEntityProcessor.QueryResult queryResult,
                                             Map<String, Object> contractsValidation) {
        try {
            // FIX: Use corrected action type from ContractsModel if available
            String finalActionType = queryResult.metadata.actionType;

            // If ContractsModel validation is available and has a different action type, use it
            if (contractsValidation != null && contractsValidation.containsKey("action")) {
                String contractsAction = (String) contractsValidation.get("action");
                if (contractsAction != null && !contractsAction.isEmpty()) {
                    finalActionType = contractsAction;
                }
            }

            // Route to appropriate action handler based on corrected action type
            return routeToActionHandlerWithDataProvider(finalActionType, queryResult.entities,
                                                        queryResult.displayEntities,
                                                        queryResult.inputTracking.originalInput);
        } catch (Exception e) {
            return "Error executing DataProvider action: " + e.getMessage();
        }
    }

    /**
     * Create UserActionResponse with complete information
     */
    private UserActionResponse createUserActionResponse(NLPEntityProcessor.QueryResult queryResult,
                                                        Map<String, Object> contractsValidation) {
        UserActionResponse response = new UserActionResponse();
        response.setOriginalInput(queryResult.inputTracking.originalInput);
        response.setCorrectedInput(queryResult.inputTracking.correctedInput);

        // FIX: Use corrected query type and action type from ContractsModel if available
        String finalQueryType = queryResult.metadata.queryType;
        String finalActionType = queryResult.metadata.actionType;

        // If ContractsModel validation is available and has a different query type, use it
        if (contractsValidation != null && contractsValidation.containsKey("queryType")) {
            String contractsQueryType = (String) contractsValidation.get("queryType");
            String contractsAction = (String) contractsValidation.get("action");

            // CRITICAL FIX: Always prioritize ContractsModel result when it's more specific
            // ContractsModel has business logic validation that should override NLP classification
            if (contractsQueryType != null && !contractsQueryType.isEmpty()) {
                // If ContractsModel says CONTRACTS but NLP says HELP, use CONTRACTS
                if ("CONTRACTS".equals(contractsQueryType) && "HELP".equals(finalQueryType)) {
                    finalQueryType = contractsQueryType;
                    if (contractsAction != null && !contractsAction.isEmpty()) {
                        finalActionType = contractsAction;
                    }
                }
                // If ContractsModel says PARTS but NLP says HELP, use PARTS
                else if ("PARTS".equals(contractsQueryType) && "HELP".equals(finalQueryType)) {
                    finalQueryType = contractsQueryType;
                    if (contractsAction != null && !contractsAction.isEmpty()) {
                        finalActionType = contractsAction;
                    }
                }
                // If ContractsModel says FAILED_PARTS but NLP says HELP, use FAILED_PARTS
                else if ("FAILED_PARTS".equals(contractsQueryType) && "HELP".equals(finalQueryType)) {
                    finalQueryType = contractsQueryType;
                    if (contractsAction != null && !contractsAction.isEmpty()) {
                        finalActionType = contractsAction;
                    }
                }
                // For all other cases, use ContractsModel result if it's not HELP
                else if (!"HELP".equals(contractsQueryType)) {
                    finalQueryType = contractsQueryType;
                    if (contractsAction != null && !contractsAction.isEmpty()) {
                        finalActionType = contractsAction;
                    }
                }
            }
        }

        response.setQueryType(finalQueryType);
        response.setActionType(finalActionType);
        response.setNlpProcessingTime(queryResult.metadata.processingTimeMs);
        response.setFilters(convertToStandardFilters(queryResult.entities));
        response.setDisplayEntities(queryResult.displayEntities);
        response.setSuccess(true);
        response.setMessage("Request processed successfully");

        // Add ContractsModel validation results if available
        if (contractsValidation != null) {
            response.addParameter("contractsValidation", contractsValidation);
            response.addParameter("businessValidationPassed", contractsValidation.get("success"));
        }

        // Generate SQL query automatically
        response.generateSQLQuery();

        return response;
    }

    /**
     * Route to appropriate action handler with DataProvider integration
     */
    private String routeToActionHandlerWithDataProvider(String actionType,
                                                        List<NLPEntityProcessor.EntityFilter> filters,
                                                        List<String> displayEntities, String userInput) {

        // Check for count queries first
        if (isCountQuery(userInput)) {
            return handleCountQuery(actionType, filters, userInput);
        }

        // Check for contract information queries (details, information, show, get)
        if (isContractInfoQuery(userInput)) {
            return handleContractInfoQuery(filters, displayEntities, userInput);
        }

        // Check for parts queries
        if (isPartsQuery(userInput)) {
            return handlePartsQuery(filters, displayEntities, userInput);
        }

        // Check for customer queries
        if (isCustomerQuery(userInput)) {
            return handleCustomerQuery(filters, displayEntities, userInput);
        }

        // Check for contract creation queries
        if (isContractCreationQueryPrivate(userInput)) {
            return handleContractCreationQuery(filters, displayEntities, userInput);
        }

        switch (actionType) {
        case "contracts_by_contractnumber":
            return handleContractByContractNumberWithDataProvider(filters, displayEntities, userInput);

        case "parts_by_contract_number":
            return handlePartsByContractNumberWithDataProvider(filters, displayEntities, userInput);

        case "parts_failed_by_contract_number":
            return handleFailedPartsByContractNumberWithDataProvider(filters, displayEntities, userInput);

        case "parts_by_part_number":
            return handlePartsByPartNumberWithDataProvider(filters, displayEntities, userInput);

        case "parts_lead_time_query":
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage(userInput);
            chatMessage.setQueryType("PARTS_LEAD_TIME");
            chatMessage.setActionType(actionType);
            return handleLeadTimeQuery(chatMessage);

        case "contracts_by_filter":
            return handleContractsByFilterWithDataProvider(filters, displayEntities, userInput);

        case "parts_by_filter":
            return handlePartsByFilterWithDataProvider(filters, displayEntities, userInput);

        case "update_contract":
            return handleUpdateContractWithDataProvider(filters, displayEntities, userInput);

        case "create_contract":
            return handleCreateContractWithDataProvider(filters, displayEntities, userInput);

        case "HELP_CONTRACT_CREATE_USER":
            return handleHelpContractCreateUserWithDataProvider(filters, displayEntities, userInput);

        case "HELP_CONTRACT_CREATE_BOT":
            return handleHelpContractCreateBotWithDataProvider(filters, displayEntities, userInput);

        default:
            return "Unknown action type: " + actionType;
        }
    }

    // ============================================================================
    // DATA PROVIDER ACTION HANDLERS
    // ============================================================================
    private String handleContractByContractNumberWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                                  List<String> displayEntities, String userInput) {
        try {
            // Check if this is a count query
            if (isCountQuery(userInput)) {
                return handleCountQuery("contracts_by_contractnumber", filters, userInput);
            }

            // Use specific or default columns based on user query
            List<String> finalDisplayEntities = getSpecificDisplayEntities(userInput, displayEntities, "contracts");

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "contracts_by_contractnumber");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", finalDisplayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b>Error in contract query:</b> " + e.getMessage() + "</p>";
        }
    }
    // ============================================================================
    // JSON RESPONSE CREATION METHODS
    // ============================================================================

    /**
     * Create complete response JSON
     */
    public String createCompleteResponseJSON(NLPEntityProcessor.QueryResult queryResult, String dataProviderResult) {
        return createCompleteResponseJSON(queryResult, dataProviderResult, null);
    }

    private String createCompleteResponseJSON(NLPEntityProcessor.QueryResult queryResult, String dataProviderResult,
                                              Map<String, Object> contractsValidation) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": true,\n");
        json.append("  \"message\": \"Request processed successfully\",\n");
        json.append("  \"nlpResponse\": {\n");
        json.append("    \"originalInput\": \"")
            .append(escapeJson(queryResult.inputTracking.originalInput))
            .append("\",\n");
        json.append("    \"correctedInput\": \"")
            .append(escapeJson(queryResult.inputTracking.correctedInput))
            .append("\",\n");

        // FIX: Use corrected query type and action type from ContractsModel if available
        String finalQueryType = queryResult.metadata.queryType;
        String finalActionType = queryResult.metadata.actionType;

        // If ContractsModel validation is available and has a different query type, use it
        if (contractsValidation != null && contractsValidation.containsKey("queryType")) {
            String contractsQueryType = (String) contractsValidation.get("queryType");
            String contractsAction = (String) contractsValidation.get("action");

            // Use ContractsModel result if it's more specific (not HELP when we have CONTRACTS)
            if (!"HELP".equals(contractsQueryType) || "HELP".equals(finalQueryType)) {
                finalQueryType = contractsQueryType;
                if (contractsAction != null && !contractsAction.isEmpty()) {
                    finalActionType = contractsAction;
                }
            }
        }

        json.append("    \"queryType\": \"")
            .append(escapeJson(finalQueryType))
            .append("\",\n");
        json.append("    \"actionType\": \"")
            .append(escapeJson(finalActionType))
            .append("\",\n");
        json.append("    \"processingTimeMs\": ")
            .append(queryResult.metadata.processingTimeMs)
            .append("\n");
        json.append("  },\n");
        json.append("  \"dataProviderResponse\": \"")
            .append(escapeJson(dataProviderResult))
            .append("\"\n");
        json.append("}");
        return json.toString();
    }

    /**
     * Create structured JSON response
     */
    private String createStructuredJSONResponse(NLPEntityProcessor.QueryResult queryResult,
                                                Map<String, Object> contractsValidation) {
        try {
            // FIX: Get actual data from database first with corrected action type
            String dataProviderResult = executeDataProviderAction(queryResult, contractsValidation);

            // Create the JSON structure with actual data
            StringBuilder json = new StringBuilder();
            json.append("{\n");

            // Header
            json.append("  \"header\": {\n");
            json.append("    \"contractNumber\": \"")
                .append(escapeJson(extractContractNumber(queryResult)))
                .append("\",\n");
            json.append("    \"partNumber\": \"")
                .append(escapeJson(extractPartNumber(queryResult)))
                .append("\",\n");
            json.append("    \"customerNumber\": \"\",\n");
            json.append("    \"customerName\": \"\",\n");
            json.append("    \"createdBy\": \"\"\n");
            json.append("  },\n");

            // Query Metadata - FIX: Use ContractsModel result when available
            String finalQueryType = queryResult.metadata.queryType;
            String finalActionType = queryResult.metadata.actionType;

            // If ContractsModel validation is available and has a different query type, use it
            if (contractsValidation != null && contractsValidation.containsKey("queryType")) {
                String contractsQueryType = (String) contractsValidation.get("queryType");
                String contractsAction = (String) contractsValidation.get("action");

                // Use ContractsModel result if it's more specific (not HELP when we have CONTRACTS)
                if (!"HELP".equals(contractsQueryType) || "HELP".equals(finalQueryType)) {
                    finalQueryType = contractsQueryType;
                    if (contractsAction != null && !contractsAction.isEmpty()) {
                        finalActionType = contractsAction;
                    }
                }
            }

            json.append("  \"queryMetadata\": {\n");
            json.append("    \"queryType\": \"")
                .append(escapeJson(finalQueryType))
                .append("\",\n");
            json.append("    \"actionType\": \"")
                .append(escapeJson(finalActionType))
                .append("\",\n");
            json.append("    \"processingTimeMs\": ")
                .append(queryResult.metadata.processingTimeMs)
                .append(",\n");
            json.append("    \"spellCorrection\": {\n");
            json.append("      \"originalInput\": \"")
                .append(escapeJson(queryResult.inputTracking.originalInput))
                .append("\",\n");
            json.append("      \"correctedInput\": \"")
                .append(escapeJson(queryResult.inputTracking.correctedInput))
                .append("\"\n");
            json.append("    }\n");
            json.append("  },\n");

            // Entities
            json.append("  \"entities\": [],\n");

            // Display Entities
            json.append("  \"displayEntities\": [],\n");

            // Errors
            json.append("  \"errors\": []");

            // Add ContractsModel validation if available
            if (contractsValidation != null) {
                json.append(",\n  \"businessValidation\": {\n");
                json.append("    \"validated\": true,\n");
                json.append("    \"queryType\": \"")
                    .append(escapeJson((String) contractsValidation.get("queryType")))
                    .append("\",\n");
                json.append("    \"action\": \"")
                    .append(escapeJson((String) contractsValidation.get("action")))
                    .append("\",\n");
                json.append("    \"confidence\": ")
                    .append(contractsValidation.get("confidence"))
                    .append("\n");
                json.append("  }");
            }

            // FIX: Add the actual data from database
            json.append(",\n  \"dataProviderResponse\": \"")
                .append(escapeJson(dataProviderResult))
                .append("\"");

            json.append("\n}");
            return json.toString();

        } catch (Exception e) {
            // If there's an error getting data, return error response
            return createErrorStructuredJSON(queryResult.inputTracking.originalInput,
                                             "Error retrieving data: " + e.getMessage());
        }
    }

    /**
     * Create error response JSON
     */
    private String createErrorResponseJSON(String userInput, String errorMessage) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": false,\n");
        json.append("  \"message\": \"")
            .append(escapeJson(errorMessage))
            .append("\",\n");
        json.append("  \"originalInput\": \"")
            .append(escapeJson(userInput))
            .append("\"\n");
        json.append("}");
        return json.toString();
    }

    /**
     * Create error structured JSON
     */
    private String createErrorStructuredJSON(String userInput, String errorMessage) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": \"\",\n");
        json.append("    \"partNumber\": \"\",\n");
        json.append("    \"customerNumber\": \"\",\n");
        json.append("    \"customerName\": \"\",\n");
        json.append("    \"createdBy\": \"\"\n");
        json.append("  },\n");
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": \"ERROR\",\n");
        json.append("    \"actionType\": \"ERROR\",\n");
        json.append("    \"processingTimeMs\": 0,\n");
        json.append("    \"spellCorrection\": {\n");
        json.append("      \"originalInput\": \"")
            .append(escapeJson(userInput))
            .append("\",\n");
        json.append("      \"correctedInput\": \"")
            .append(escapeJson(userInput))
            .append("\"\n");
        json.append("    }\n");
        json.append("  },\n");
        json.append("  \"entities\": [],\n");
        json.append("  \"displayEntities\": [],\n");
        json.append("  \"errors\": [\"")
            .append(escapeJson(errorMessage))
            .append("\"]\n");
        json.append("}");
        return json.toString();
    }

    /**
     * Extract contract number from query result using centralized column mapping
     */
    private String extractContractNumber(NLPEntityProcessor.QueryResult queryResult) {
        for (NLPEntityProcessor.EntityFilter filter :
             queryResult.entities) {
            // Use centralized column mapping for contract number
            String contractNumberColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "contract_number");
            if (filter.attribute.equals(contractNumberColumn)) {
                return filter.value;
            }
        }
        return "";
    }

    /**
     * Generate SQL query based on query result and ContractsModel validation
     * Uses centralized TableColumnConfig for column validation
     */
    private String generateSQLQuery(NLPEntityProcessor.QueryResult queryResult) {
        // Basic SQL generation based on action type
        String actionType = queryResult.metadata.actionType;
        String queryType = queryResult.metadata.queryType;

        switch (actionType) {
        case "contracts_by_contractnumber":
            return "SELECT * FROM " + TableColumnConfig.TABLE_CONTRACTS + " WHERE " +
                   TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "contract_number") + " = ?";
        case "parts_by_contract_number":
            return "SELECT * FROM " + TableColumnConfig.TABLE_PARTS + " WHERE " +
                   TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, "award_id") + " = ?";
        case "parts_failed_by_contract_number":
            return "SELECT * FROM " + TableColumnConfig.TABLE_FAILED_PARTS + " WHERE " +
                   TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_FAILED_PARTS, "award_number") + " = ?";
        case "parts_by_part_number":
            return "SELECT * FROM " + TableColumnConfig.TABLE_PARTS + " WHERE " +
                   TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, "part_number") + " = ?";
        case "contracts_by_filter":
            return "SELECT * FROM " + TableColumnConfig.TABLE_CONTRACTS + " WHERE " +
                   buildWhereClauseWithValidation(queryResult.entities, TableColumnConfig.TABLE_CONTRACTS);
        case "parts_by_filter":
            return "SELECT * FROM " + TableColumnConfig.TABLE_PARTS + " WHERE " +
                   buildWhereClauseWithValidation(queryResult.entities, TableColumnConfig.TABLE_PARTS);
        default:
            return "SELECT * FROM " + TableColumnConfig.TABLE_CONTRACTS;
        }
    }

    /**
     * Extract part number from query result using centralized column mapping
     */
    private String extractPartNumber(NLPEntityProcessor.QueryResult queryResult) {
        for (NLPEntityProcessor.EntityFilter filter :
             queryResult.entities) {
            // Use centralized column mapping for part number
            String partNumberColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, "part_number");
            if (filter.attribute.equals(partNumberColumn)) {
                return filter.value;
            }
        }
        return "";
    }

    /**
     * Check if the query is asking for a count
     */
    private boolean isCountQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("count") || lowerInput.contains("how many") || lowerInput.contains("number of") ||
               lowerInput.contains("total") || lowerInput.contains("score");
    }

    /**
     * Handle count queries by returning row count instead of data
     */
    private String handleCountQuery(String actionType, List<NLPEntityProcessor.EntityFilter> filters,
                                    String userInput) {
        try {
            // Prepare input parameters for count query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", actionType + "_count");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", Arrays.asList("COUNT"));
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute count query
            String result = pullData(inputParams);

            // Extract count from result and format response
            if (result.contains("No records found")) {
                return "<p><i>Count: 0</i></p>";
            }

            // Try to extract count from the result
            if (result.contains("rows")) {
                return result; // Already formatted
            }

            return "<p><b>Count: " + extractCountFromResult(result) + "</b></p>";

        } catch (Exception e) {
            return "<p><b>Error getting count:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Extract count number from result string
     */
    private String extractCountFromResult(String result) {
        // Simple extraction - look for numbers in the result
        String[] words = result.split("\\s+");
        for (String word : words) {
            if (word.matches("\\d+")) {
                return word;
            }
        }
        return "0";
    }

    /**
     * Get default display entities based on table type
     */
    private List<String> getDefaultDisplayEntities(List<String> displayEntities, String tableType) {
        // Only use defaults if displayEntities is null or empty
        if (displayEntities != null && !displayEntities.isEmpty()) {
            return displayEntities;
        }

        // Return default columns based on table type
        switch (tableType.toLowerCase()) {
        case "contracts":
            return new ArrayList<>(TABLE_CONFIG.DEFAULT_CONTRACTS_COLMS);
        case "parts":
            return new ArrayList<>(TABLE_CONFIG.DEFAULT_PARTS_COLMS);
        case "failed_parts":
            return new ArrayList<>(TABLE_CONFIG.DEFAULT_PARTS_FAILED_COLMS);
        default:
            return new ArrayList<>(TABLE_CONFIG.DEFAULT_CONTRACTS_COLMS);
        }
    }

    /**
     * Get specific display entities based on user query
     */
    private List<String> getSpecificDisplayEntities(String userInput, List<String> displayEntities, String tableType) {
        // Only use defaults if displayEntities is null or empty
        if (displayEntities != null && !displayEntities.isEmpty()) {
            return displayEntities;
        }

        // Check for specific field requests in the query
        String lowerInput = userInput.toLowerCase();
        List<String> specificFields = new ArrayList<>();

        // Check for customer name requests
        if (lowerInput.contains("customer name") || lowerInput.contains("customer")) {
            specificFields.add("CUSTOMER_NAME");
        }

        // Check for contract name requests
        if (lowerInput.contains("contract name") || lowerInput.contains("award name")) {
            specificFields.add("CONTRACT_NAME");
        }

        // Check for expiration date requests
        if (lowerInput.contains("expiration") || lowerInput.contains("expire")) {
            specificFields.add("EXPIRATION_DATE");
        }

        // Check for effective date requests
        if (lowerInput.contains("effective date")) {
            specificFields.add("EFFECTIVE_DATE");
        }

        // Check for status requests
        if (lowerInput.contains("status") || lowerInput.contains("active") || lowerInput.contains("expired")) {
            specificFields.add("STATUS");
        }

        // If specific fields were found, return them; otherwise use defaults
        if (!specificFields.isEmpty()) {
            return specificFields;
        }

        // Return default columns based on table type
        return getDefaultDisplayEntities(null, tableType);
    }

    /**
     * Check if query is asking for contract information/details
     */
    private boolean isContractInfoQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for contract info keywords
        boolean hasInfoKeywords =
            lowerInput.contains("details") || lowerInput.contains("information") || lowerInput.contains("info") ||
            lowerInput.contains("show") || lowerInput.contains("get") || lowerInput.contains("about") ||
            lowerInput.contains("what is") || lowerInput.contains("pull");

        // Check for 6-digit contract number pattern
        boolean hasContractNumber = userInput.matches(".*\\b\\d{6}\\b.*");

        // Check for contract-related words
        boolean hasContractWords = lowerInput.contains("contract") || lowerInput.contains("contarct");

        // If it has info keywords AND (contract number OR contract words), it's a contract info query
        return hasInfoKeywords && (hasContractNumber || hasContractWords);
    }

    /**
     * Handle contract information queries with expiration date logic
     */
    private String handleContractInfoQuery(List<NLPEntityProcessor.EntityFilter> filters, List<String> displayEntities,
                                           String userInput) {
        try {
            // Extract contract number from user input or filters
            String contractNumber = extractContractNumberFromInput(userInput, filters);
            if (contractNumber == null || contractNumber.isEmpty()) {
                return "<p><b>Contract number not found in query</b></p>";
            }

            // Check if this is an expiration date query
            if (isExpirationDateQuery(userInput)) {
                return handleExpirationDateQuery(contractNumber, userInput);
            }

            // Check if this is an active/expired status query
            if (isActiveStatusQuery(userInput)) {
                return handleActiveStatusQuery(contractNumber, userInput);
            }

            // For general contract info queries, create proper filters and use contracts_by_contractnumber
            List<NLPEntityProcessor.EntityFilter> contractFilters = createContractFilters(contractNumber);
            List<String> finalDisplayEntities = getDefaultDisplayEntities(displayEntities, "contracts");

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "contracts_by_contractnumber");
            inputParams.put("filters", contractFilters);
            inputParams.put("displayEntities", finalDisplayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b>Error in contract info query:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Extract contract number from user input or filters
     */
    private String extractContractNumberFromInput(String userInput, List<NLPEntityProcessor.EntityFilter> filters) {
        // First try to extract from user input using regex (including with symbols)
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b#?(\\d{6,})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        if (matcher.find()) {
            return matcher.group(1); // Return the number without the # symbol
        }

        // Try alternative patterns for different formats
        pattern = java.util
                      .regex
                      .Pattern
                      .compile("\\b(\\d{6,})\\b");
        matcher = pattern.matcher(userInput);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // If not found in input, try filters
        if (filters != null) {
            for (NLPEntityProcessor.EntityFilter filter :
                 filters) {
                // Check for contract number patterns
                if (filter.attribute.equals("AWARD_NUMBER") || filter.attribute.equals("CONTRACT_NUMBER") ||
                    filter.attribute.matches("\\d{6,}")) {
                    return filter.value;
                }
            }
        }
        return null;
    }

    /**
     * Create contract filters for contract number
     */
    private List<NLPEntityProcessor.EntityFilter> createContractFilters(String contractNumber) {
        List<NLPEntityProcessor.EntityFilter> filters = new ArrayList<>();
        filters.add(new NLPEntityProcessor.EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
        return filters;
    }

    /**
     * Check if query is asking about expiration date
     */
    private boolean isExpirationDateQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for expiration date keywords (including misspellings and variations)
        boolean hasExpirationKeywords =
            lowerInput.contains("expire") || lowerInput.contains("expiration") || lowerInput.contains("expiry") ||
            lowerInput.contains("expires") || lowerInput.contains("expired") || lowerInput.contains("end date") ||
            lowerInput.contains("end date") || lowerInput.contains("termination") ||
            lowerInput.contains("valid until") || lowerInput.contains("valid till");

        // Check for question patterns and variations
        boolean hasQuestionPattern =
            lowerInput.contains("when") || lowerInput.contains("what") || lowerInput.contains("how long") ||
            lowerInput.contains("until when") || lowerInput.contains("till when") || lowerInput.contains("?") ||
            lowerInput.contains("check") || lowerInput.contains("confirm");

        // Check for 6+ digit contract numbers (including with symbols)
        boolean hasContractNumber =
            lowerInput.matches(".*\\b\\d{6,}\\b.*") || lowerInput.matches(".*#\\d{6,}.*") ||
            lowerInput.matches(".*\\d{6,}.*");

        // Check for contract-related words
        boolean hasContractWords = lowerInput.contains("contract") || lowerInput.contains("contarct") || // typo
            lowerInput.contains("id") || lowerInput.contains("number");

        // Must have expiration keywords AND (question pattern OR contract number OR contract words)
        return hasExpirationKeywords && (hasQuestionPattern || hasContractNumber || hasContractWords);
    }

    /**
     * Check if query is asking about active/expired status
     */
    private boolean isActiveStatusQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for active/expired status keywords (including misspellings and variations)
        boolean hasStatusKeywords =
            lowerInput.contains("active") || lowerInput.contains("aktive") || // misspelling
            lowerInput.contains("avtive") || // misspelling
            lowerInput.contains("expired") || lowerInput.contains("expire") || lowerInput.contains("inactive") ||
                          lowerInput.contains("inactive") || lowerInput.contains("valid") ||
                          lowerInput.contains("invalid") || lowerInput.contains("current") ||
                          lowerInput.contains("live") || lowerInput.contains("running") ||
                          lowerInput.contains("still") || lowerInput.contains("not") || lowerInput.contains("status") ||
                          lowerInput.contains("check") || lowerInput.contains("confirm");

        // Check for question patterns and variations
        boolean hasQuestionPattern =
            lowerInput.contains("is") || lowerInput.contains("are") || lowerInput.contains("does") ||
            lowerInput.contains("can") || lowerInput.contains("what") || lowerInput.contains("how") ||
            lowerInput.contains("when") || lowerInput.contains("who") || lowerInput.contains("?") ||
            lowerInput.contains("check") || lowerInput.contains("confirm") || lowerInput.contains("status");

        // Check for 6+ digit contract numbers (including with symbols)
        boolean hasContractNumber =
            lowerInput.matches(".*\\b\\d{6,}\\b.*") || lowerInput.matches(".*#\\d{6,}.*") ||
            lowerInput.matches(".*\\d{6,}.*");

        // Check for contract-related words
        boolean hasContractWords = lowerInput.contains("contract") || lowerInput.contains("contarct") || // typo
            lowerInput.contains("id") || lowerInput.contains("number");

        // Must have status keywords AND (question pattern OR contract number OR contract words)
        return hasStatusKeywords && (hasQuestionPattern || hasContractNumber || hasContractWords);
    }

    /**
     * Check if query is asking for parts information
     */
    private boolean isPartsQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for parts keywords (including misspellings)
        boolean hasPartsKeywords =
            lowerInput.contains("part") || lowerInput.contains("parts") || lowerInput.contains("invoice part") ||
            lowerInput.contains("invoce part") || // misspelling
            lowerInput.contains("invoic part"); // misspelling

        // Check for part-specific attributes
        boolean hasPartAttributes =
            lowerInput.contains("lead time") || lowerInput.contains("leed time") || // misspelling
            lowerInput.contains("price") || lowerInput.contains("pric") || // misspelling
            lowerInput.contains("prise") || // misspelling
            lowerInput.contains("cost") || lowerInput.contains("moq") || lowerInput.contains("minimum order") ||
                          lowerInput.contains("min order") || lowerInput.contains("uom") ||
                          lowerInput.contains("unit of measure") || lowerInput.contains("unit measure") ||
                          lowerInput.contains("status") || lowerInput.contains("staus") || // misspelling
                          lowerInput.contains("item classification") || lowerInput.contains("item class") ||
                          lowerInput.contains("classification") || lowerInput.contains("class");

        // Check for part number pattern (alphanumeric with hyphens, typically 3+ chars)
        boolean hasPartNumber = lowerInput.matches(".*\\b[A-Za-z0-9]{3,}(?:-[A-Za-z0-9]+)*\\b.*");

        // Check for contract number (6+ digits)
        boolean hasContractNumber = lowerInput.matches(".*\\b\\d{6,}\\b.*");

        // Check for question/action keywords
        boolean hasActionKeywords =
            lowerInput.contains("what") || lowerInput.contains("show") || lowerInput.contains("get") ||
            lowerInput.contains("list") || lowerInput.contains("?") || lowerInput.contains("details") ||
            lowerInput.contains("info") || lowerInput.contains("information");

        // Must have parts keywords AND (part number OR contract number OR part attributes OR action keywords)
        return hasPartsKeywords && (hasPartNumber || hasContractNumber || hasPartAttributes || hasActionKeywords);
    }

    /**
     * Check if parts query has contract number
     */
    private boolean hasContractNumberInPartsQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for 6+ digit contract numbers
        boolean hasContractNumber = lowerInput.matches(".*\\b\\d{6,}\\b.*");

        // Check for contract-related words
        boolean hasContractWords = lowerInput.contains("contract") || lowerInput.contains("contarct") || // misspelling
            lowerInput.contains("for contract") || lowerInput.contains("in contract");

        return hasContractNumber || hasContractWords;
    }

    /**
     * Handle parts queries with contract number validation
     */
    private String handlePartsQuery(List<NLPEntityProcessor.EntityFilter> filters, List<String> displayEntities,
                                    String userInput) {
        return handlePartsQuery(filters, displayEntities, userInput, null);
    }

    /**
     * Handle parts queries with contract number validation and session management
     */
    private String handlePartsQuery(List<NLPEntityProcessor.EntityFilter> filters, List<String> displayEntities,
                                    String userInput, String sessionId) {
        try {
            // Check if parts query has contract number
            if (!hasContractNumberInPartsQuery(userInput)) {
                // Extract part number for the request
                String partNumber = extractPartNumberFromInput(userInput, filters);
                if (partNumber == null || partNumber.isEmpty()) {
                    partNumber = "the part"; // fallback
                }

                // If we have a session ID, create a conversational flow
                if (sessionId != null) {
                    return flowManager.createPartsContractRequest(userInput, partNumber, sessionId);
                } else {
                    // Fallback to static message if no session
                    return "<h4>Contract Number Required</h4>" +
                           "<p>Can you specify the contract number because parts number (invoice part number) is loaded w.r.t Contract.</p>" +
                           "<p><b>Examples:</b><br>" +
                           "&bull; &quot;What is the lead time for part EN6114V4-13 in contract 100476?&quot;<br>" +
                           "&bull; &quot;Show me part details for EN6114V4-13 for contract 100476&quot;<br>" +
                           "&bull; &quot;What's the price for part EN6114V4-13 in 100476?&quot;<br>" +
                           "&bull; &quot;Show me all parts for contract 100476&quot;<br>" +
                           "&bull; &quot;List invoice parts for 100476&quot;</p>";
                }
            }

            // Extract contract number and part number
            String contractNumber = extractContractNumberFromInput(userInput, filters);
            String partNumber = extractPartNumberFromInput(userInput, filters);

            if (contractNumber == null || contractNumber.isEmpty()) {
                return "<p><b>Contract number not found in query. Please specify a contract number.</b></p>";
            }

            // Determine the type of parts query and route accordingly
            if (partNumber != null && !partNumber.isEmpty()) {
                // Specific part query - use parts_by_part_number
                return handleSpecificPartQuery(contractNumber, partNumber, userInput, displayEntities);
            } else {
                // General parts query - use parts_by_contract_number
                return handleGeneralPartsQuery(contractNumber, userInput, displayEntities);
            }

        } catch (Exception e) {
            return "<p><b>Error in parts query:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Handle specific part queries (with part number)
     */
    private String handleSpecificPartQuery(String contractNumber, String partNumber, String userInput,
                                           List<String> displayEntities) {
        try {
            // Create filters for both contract and part number
            List<NLPEntityProcessor.EntityFilter> filters = new ArrayList<>();
            filters.add(new NLPEntityProcessor.EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
            filters.add(new NLPEntityProcessor.EntityFilter("PART_NUMBER", "=", partNumber, "extracted"));

            // Determine specific columns based on user query
            List<String> specificColumns = determineSpecificPartColumns(userInput);

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "parts_by_part_number");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", specificColumns.isEmpty() ? displayEntities : specificColumns);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Handle general parts queries (without specific part number)
     */
    private String handleGeneralPartsQuery(String contractNumber, String userInput, List<String> displayEntities) {
        try {
            // Create filters for contract number
            List<NLPEntityProcessor.EntityFilter> filters = new ArrayList<>();
            filters.add(new NLPEntityProcessor.EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));

            // Determine if it's invoice parts or general parts
            String actionType = determinePartsActionType(userInput);

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", actionType);
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", displayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Extract part number from user input
     */
    private String extractPartNumberFromInput(String userInput, List<NLPEntityProcessor.EntityFilter> filters) {
        if (userInput == null)
            return null;

        // Pattern for part numbers (alphanumeric with hyphens, typically 3+ chars)
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b([A-Za-z0-9]{3,}(?:-[A-Za-z0-9]+)*)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            String partNumber = matcher.group(1);
            // Filter out common words that might match the pattern
            if (!isCommonWord(partNumber)) {
                return partNumber;
            }
        }

        // If not found in input, try filters
        if (filters != null) {
            for (NLPEntityProcessor.EntityFilter filter : filters) {
                if (filter.attribute.equals("PART_NUMBER") || filter.attribute.equals("INVOICE_PART_NUMBER")) {
                    return filter.value;
                }
            }
        }
        return null;
    }

    /**
     * Determine specific part columns based on user query
     */
    private List<String> determineSpecificPartColumns(String userInput) {
        if (userInput == null)
            return new ArrayList<>();

        String lowerInput = userInput.toLowerCase();
        List<String> columns = new ArrayList<>();

        // Lead time queries
        if (lowerInput.contains("lead time") || lowerInput.contains("leed time")) {
            columns.add("LEAD_TIME");
        }

        // Price queries
        if (lowerInput.contains("price") || lowerInput.contains("pric") || lowerInput.contains("prise") ||
            lowerInput.contains("cost")) {
            columns.add("PRICE");
        }

        // MOQ queries
        if (lowerInput.contains("moq") || lowerInput.contains("minimum order") || lowerInput.contains("min order")) {
            columns.add("MOQ");
        }

        // UOM queries
        if (lowerInput.contains("uom") || lowerInput.contains("unit of measure") ||
            lowerInput.contains("unit measure") || lowerInput.contains("unit")) {
            columns.add("UOM");
        }

        // Status queries
        if (lowerInput.contains("status") || lowerInput.contains("staus")) {
            columns.add("STATUS");
        }

        // Item classification queries
        if (lowerInput.contains("item classification") || lowerInput.contains("item class") ||
            lowerInput.contains("classification") || lowerInput.contains("class")) {
            columns.add("ITEM_CLASSIFICATION");
        }

        return columns;
    }

    /**
     * Determine parts action type based on user query
     */
    private String determinePartsActionType(String userInput) {
        if (userInput == null)
            return "parts_by_contract_number";

        String lowerInput = userInput.toLowerCase();

        // Check for invoice parts
        if (lowerInput.contains("invoice part") || lowerInput.contains("invoce part") ||
            lowerInput.contains("invoic part")) {
            return "parts_by_contract_number"; // or specific invoice parts action if available
        }

        // Default to general parts
        return "parts_by_contract_number";
    }

    /**
     * Check if a word is a common word (not a part number)
     */
    private boolean isCommonWord(String word) {
        if (word == null || word.length() < 3)
            return true;

        String lowerWord = word.toLowerCase();
        Set<String> commonWords =
            new HashSet<>(Arrays.asList("part", "parts", "for", "the", "and", "with", "show", "get", "what", "how",
                                        "when", "where", "contract", "number", "details", "info", "information",
                                        "price", "cost", "status", "lead", "time"));

        return commonWords.contains(lowerWord);
    }

    /**
     * Handle expiration date queries using pullContractDatesByAwardNumber
     */
    private String handleExpirationDateQuery(String contractNumber, String userInput) {
        try {
            // Call pullContractDatesByAwardNumber to get expiration date
            // Create proper filters for the contract number
            List<NLPEntityProcessor.EntityFilter> contractFilters = createContractFilters(contractNumber);

            // Prepare input parameters for pullContractDatesByAwardNumber
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "pullContractDatesByAwardNumber");
            inputParams.put("filters", contractFilters);
            inputParams.put("displayEntities", Arrays.asList("EXPIRATION_DATE"));
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query to get expiration date
            String result = pullData(inputParams);

            // Check if we got data
            if (result.contains("No records found")) {
                return "<p><i></i></p>";
            }

            // Extract expiration date from result (this would need parsing logic)
            // For now, we'll use a simple approach - in real implementation, parse the result
            String expirationDate = "2024-12-31"; // This should be extracted from the result

            // Format the response
            StringBuilder response = new StringBuilder();
            response.append("<h3>Contract Expiration Date</h3><hr>");
            response.append("<h4>Contract ")
                    .append(contractNumber)
                    .append("</h4>");
            response.append("<p><b>Expiration Date:</b> ")
                    .append(expirationDate)
                    .append("</p>");
            response.append(addProfessionalNote());

            return response.toString();

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Handle active/expired status queries using pullContractDatesByAwardNumber
     */
    private String handleActiveStatusQuery(String contractNumber, String userInput) {
        try {
            // Call pullContractDatesByAwardNumber to get expiration date
            // Create proper filters for the contract number
            List<NLPEntityProcessor.EntityFilter> contractFilters = createContractFilters(contractNumber);

            // Prepare input parameters for pullContractDatesByAwardNumber
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "pullContractDatesByAwardNumber");
            inputParams.put("filters", contractFilters);
            inputParams.put("displayEntities", Arrays.asList("EXPIRATION_DATE"));
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query to get expiration date
            String result = pullData(inputParams);

            // Check if we got data
            if (result.contains("No records found")) {
                return "<p><i></i></p>";
            }

            // Extract expiration date from result (this would need parsing logic)
            // For now, we'll use a simple approach - in real implementation, parse the result
            String expirationDate = "2024-12-31"; // This should be extracted from the result

            // Compare with current date
            java.time.LocalDate currentDate = java.time
                                                  .LocalDate
                                                  .now();
            java.time.LocalDate expDate = java.time
                                              .LocalDate
                                              .parse(expirationDate);

            boolean isActive = expDate.isAfter(currentDate);
            String status = isActive ? "ACTIVE" : "EXPIRED";
            String statusColor = isActive ? "#28a745" : "#dc3545";

            // Format the response
            StringBuilder response = new StringBuilder();
            response.append("<h3>Contract Status</h3><hr>");
            response.append("<h4>Contract ")
                    .append(contractNumber)
                    .append("</h4>");
            response.append("<p><b>Status:</b> ")
                    .append(status)
                    .append("</p>");
            response.append("<p><b>Expiration Date:</b> ")
                    .append(expirationDate)
                    .append("</p>");
            response.append(addProfessionalNote());

            return response.toString();

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Escape JSON string
     */
    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    /**
     * Main method for user interface interaction (LEGACY - kept for backward compatibility)
     * Processes user input and returns structured response with SQL queries
     */
    public UserActionResponse processUserInput(String userInput) {
        try {
            // Use the original processQuery method to get proper inputTracking
            String jsonResponse = nlpProcessor.processQuery(userInput);

            // Parse the response to extract metadata
            NLPEntityProcessor.QueryResult queryResult = nlpProcessor.parseJSONToObject(jsonResponse);

            // Create response object
            UserActionResponse response = new UserActionResponse();
            response.setOriginalInput(userInput);
            response.setCorrectedInput(queryResult.inputTracking.correctedInput);
            response.setQueryType(queryResult.metadata.queryType);
            response.setActionType(queryResult.metadata.actionType);
            response.setNlpProcessingTime(queryResult.metadata.processingTimeMs);
            response.setFilters(convertToStandardFilters(queryResult.entities));
            response.setDisplayEntities(queryResult.displayEntities);
            response.setSuccess(true);
            response.setMessage("Request processed successfully");

            // Generate SQL query automatically
            response.generateSQLQuery();

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error processing user input: " + e.getMessage());
        }
    }

    /**
     * Routes to appropriate action handler based on action type
     */
    private UserActionResponse routeToActionHandler(String actionType, List<NLPEntityProcessor.EntityFilter> filters,
                                                    List<String> displayEntities, String userInput) {

        switch (actionType) {
        case "contracts_by_contractnumber":
            return handleContractByContractNumber(filters, displayEntities, userInput);

        case "parts_by_contract_number":
            return handlePartsByContractNumber(filters, displayEntities, userInput);

        case "parts_failed_by_contract_number":
            return handleFailedPartsByContractNumber(filters, displayEntities, userInput);

        case "parts_by_part_number":
            return handlePartsByPartNumber(filters, displayEntities, userInput);

        case "contracts_by_filter":
            return handleContractsByFilter(filters, displayEntities, userInput);

        case "parts_by_filter":
            return handlePartsByFilter(filters, displayEntities, userInput);

        case "update_contract":
            return handleUpdateContract(filters, displayEntities, userInput);

        case "create_contract":
            return handleCreateContract(filters, displayEntities, userInput);

        case "HELP_CONTRACT_CREATE_USER":
            return handleHelpContractCreateUser(filters, displayEntities, userInput);

        case "HELP_CONTRACT_CREATE_BOT":
            return handleHelpContractCreateBot(filters, displayEntities, userInput);

        default:
            return createErrorResponse(userInput, "Unknown action type: " + actionType);
        }
    }

    // ============================================================================
    // CONTRACT ACTION HANDLERS
    // ============================================================================

    /**
     * Handles contract queries by contract number
     * SQL: SELECT display_entities FROM contracts WHERE AWARD_NUMBER = ?
     */
    private UserActionResponse handleContractByContractNumber(List<NLPEntityProcessor.EntityFilter> filters,
                                                              List<String> displayEntities, String userInput) {
        try {
            // Extract contract number filter using centralized column mapping
            String contractNumberColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "contract_number");
            String contractNumber = extractFilterValue(filters, contractNumberColumn);
            if (contractNumber == null) {
                return createErrorResponse(userInput, "Contract number not found in query");
            }

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("contracts_by_contractnumber");
            response.setParameters(createParameterMap(contractNumberColumn, contractNumber));
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Contract query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in contract query: " + e.getMessage());
        }
    }

    /**
     * Handles contract queries by filter criteria
     * SQL: SELECT display_entities FROM contracts WHERE filter_conditions
     */
    private UserActionResponse handleContractsByFilter(List<NLPEntityProcessor.EntityFilter> filters,
                                                       List<String> displayEntities, String userInput) {
        try {
            // Create parameter map
            Map<String, Object> parameters = createParameterMapFromFilters(filters);

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("contracts_by_filter");
            response.setParameters(parameters);
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Contract filter query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in contract filter query: " + e.getMessage());
        }
    }

    // ============================================================================
    // PARTS ACTION HANDLERS
    // ============================================================================

    /**
     * Handles parts queries by contract number
     * SQL: SELECT display_entities FROM parts WHERE AWARD_NUMBER = ?
     */
    private UserActionResponse handlePartsByContractNumber(List<NLPEntityProcessor.EntityFilter> filters,
                                                           List<String> displayEntities, String userInput) {
        try {
            // Extract contract number filter using centralized column mapping
            String contractNumberColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "contract_number");
            String contractNumber = extractFilterValue(filters, contractNumberColumn);
            if (contractNumber == null) {
                return createErrorResponse(userInput, "Contract number not found in query");
            }

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("parts_by_contract_number");
            response.setParameters(createParameterMap(contractNumberColumn, contractNumber));
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Parts by contract query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in parts by contract query: " + e.getMessage());
        }
    }

    /**
     * Handles parts queries by part number
     * SQL: SELECT display_entities FROM parts WHERE INVOICE_PART_NUMBER = ?
     */
    private UserActionResponse handlePartsByPartNumber(List<NLPEntityProcessor.EntityFilter> filters,
                                                       List<String> displayEntities, String userInput) {
        try {
            // Extract part number filter using centralized column mapping
            String partNumberColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, "part_number");
            String partNumber = extractFilterValue(filters, partNumberColumn);
            if (partNumber == null) {
                return createErrorResponse(userInput, "Part number not found in query");
            }

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("parts_by_part_number");
            response.setParameters(createParameterMap(partNumberColumn, partNumber));
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Parts by part number query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in parts by part number query: " + e.getMessage());
        }
    }

    /**
     * Handles parts queries by filter criteria
     * SQL: SELECT display_entities FROM parts WHERE filter_conditions
     */
    private UserActionResponse handlePartsByFilter(List<NLPEntityProcessor.EntityFilter> filters,
                                                   List<String> displayEntities, String userInput) {
        try {
            // Create parameter map
            Map<String, Object> parameters = createParameterMapFromFilters(filters);

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("parts_by_filter");
            response.setParameters(parameters);
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Parts filter query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in parts filter query: " + e.getMessage());
        }
    }

    // ============================================================================
    // FAILED PARTS ACTION HANDLERS
    // ============================================================================

    /**
     * Handles failed parts queries by contract number
     * SQL: SELECT display_entities FROM failed_parts WHERE LOADED_CP_NUMBER = ?
     */
    private UserActionResponse handleFailedPartsByContractNumber(List<NLPEntityProcessor.EntityFilter> filters,
                                                                 List<String> displayEntities, String userInput) {
        try {
            // Extract contract number filter (for failed parts, use LOADED_CP_NUMBER)
            String contractNumber = extractFilterValue(filters, "LOADED_CP_NUMBER");
            if (contractNumber == null) {
                return createErrorResponse(userInput, "Contract number not found in failed parts query");
            }

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("parts_failed_by_contract_number");
            response.setParameters(createParameterMap("LOADED_CP_NUMBER", contractNumber));
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Failed parts query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in failed parts query: " + e.getMessage());
        }
    }

    // ============================================================================
    // UPDATE ACTION HANDLERS
    // ============================================================================

    /**
     * Handles contract update operations
     * SQL: UPDATE contracts SET update_fields WHERE AWARD_NUMBER = ?
     */
    private UserActionResponse handleUpdateContract(List<NLPEntityProcessor.EntityFilter> filters,
                                                    List<String> displayEntities, String userInput) {
        try {
            // Extract contract number filter using centralized column mapping
            String contractNumberColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "contract_number");
            String contractNumber = extractFilterValue(filters, contractNumberColumn);
            if (contractNumber == null) {
                return createErrorResponse(userInput, "Contract number not found in update query");
            }

            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("update_contract");
            response.setParameters(createParameterMapFromFilters(filters));
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Contract update query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in contract update query: " + e.getMessage());
        }
    }

    // ============================================================================
    // CREATE ACTION HANDLERS
    // ============================================================================

    /**
     * Handles contract creation operations
     * SQL: INSERT INTO contracts (fields) VALUES (values)
     */
    private UserActionResponse handleCreateContract(List<NLPEntityProcessor.EntityFilter> filters,
                                                    List<String> displayEntities, String userInput) {
        try {
            // Create response
            UserActionResponse response = new UserActionResponse();
            response.setActionType("create_contract");
            response.setParameters(createParameterMapFromFilters(filters));
            response.setDisplayEntities(displayEntities);
            response.setFilters(convertToStandardFilters(filters));
            response.setSuccess(true);
            response.setMessage("Contract creation query processed successfully");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in contract creation query: " + e.getMessage());
        }
    }


    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Extracts filter value by attribute name
     */
    private String extractFilterValue(List<NLPEntityProcessor.EntityFilter> filters, String attributeName) {
        for (NLPEntityProcessor.EntityFilter filter : filters) {
            if (filter.attribute.equals(attributeName)) {
                return filter.value;
            }
        }
        return null;
    }

    /**
     * Builds WHERE clause from filters with column validation
     */
    private String buildWhereClause(List<NLPEntityProcessor.EntityFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "1=1"; // Default condition
        }

        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            NLPEntityProcessor.EntityFilter filter = filters.get(i);
            if (i > 0)
                whereClause.append(" AND ");
            whereClause.append(filter.attribute)
                       .append(" ")
                       .append(filter.operation)
                       .append(" ?");
        }
        return whereClause.toString();
    }

    /**
     * Builds WHERE clause from filters with centralized column validation
     */
    private String buildWhereClauseWithValidation(List<NLPEntityProcessor.EntityFilter> filters, String tableType) {
        if (filters == null || filters.isEmpty()) {
            return "1=1"; // Default condition
        }

        StringBuilder whereClause = new StringBuilder();
        int validFilterCount = 0;

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            // Validate column exists in the specified table
            if (TABLE_CONFIG.isValidColumn(tableType, filter.attribute)) {
                if (validFilterCount > 0)
                    whereClause.append(" AND ");
                whereClause.append(filter.attribute)
                           .append(" ")
                           .append(filter.operation)
                           .append(" ?");
                validFilterCount++;
            }
        }

        // If no valid filters, return default condition
        return validFilterCount > 0 ? whereClause.toString() : "1=1";
    }

    /**
     * Creates parameter map from filters
     */
    private Map<String, Object> createParameterMapFromFilters(List<NLPEntityProcessor.EntityFilter> filters) {
        Map<String, Object> parameters = new HashMap<>();
        if (filters != null) {
            for (NLPEntityProcessor.EntityFilter filter : filters) {
                parameters.put(filter.attribute, filter.value);
            }
        }
        return parameters;
    }

    /**
     * Creates simple parameter map
     */
    private Map<String, Object> createParameterMap(String key, Object value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(key, value);
        return parameters;
    }

    /**
     * Creates error response
     */
    private UserActionResponse createErrorResponse(String userInput, String errorMessage) {
        UserActionResponse response = new UserActionResponse();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setOriginalInput(userInput);
        return response;
    }

    // ============================================================================
    // HELP ACTION HANDLERS
    // ============================================================================

    /**
     * Handles help requests for contract creation (user wants instructions)
     * No SQL needed - returns help text
     */
    private UserActionResponse handleHelpContractCreateUser(List<NLPEntityProcessor.EntityFilter> filters,
                                                            List<String> displayEntities, String userInput) {
        try {
            // Create response for help request
            UserActionResponse response = new UserActionResponse();
            response.setActionType("HELP_CONTRACT_CREATE_USER");
            response.setParameters(new HashMap<>()); // No parameters needed
            response.setDisplayEntities(displayEntities); // Should be empty for help
            response.setFilters(convertToStandardFilters(filters)); // Should be empty for help
            response.setSuccess(true);
            response.setMessage("Please follow below steps to create a contract");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in help request: " + e.getMessage());
        }
    }

    /**
     * Handles help requests for contract creation (user wants system to create)
     * No SQL needed - returns help text
     */
    private UserActionResponse handleHelpContractCreateBot(List<NLPEntityProcessor.EntityFilter> filters,
                                                           List<String> displayEntities, String userInput) {
        try {
            // Create response for help request
            UserActionResponse response = new UserActionResponse();
            response.setActionType("HELP_CONTRACT_CREATE_BOT");
            response.setParameters(new HashMap<>()); // No parameters needed
            response.setDisplayEntities(displayEntities); // Should be empty for help
            response.setFilters(convertToStandardFilters(filters)); // Should be empty for help
            response.setSuccess(true);
            response.setMessage("I can help you create a contract. Please provide the necessary details.");

            return response;

        } catch (Exception e) {
            return createErrorResponse(userInput, "Error in help request: " + e.getMessage());
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================


    /**
     * Format list as string
     */
    private static String formatList(java.util.List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", list) + "]";
    }

    /**
     * Format filters as string
     */
    private static String formatFilters(java.util.List<NLPEntityProcessor.EntityFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filters.size(); i++) {
            NLPEntityProcessor.EntityFilter filter = filters.get(i);
            sb.append(filter.attribute)
              .append("=")
              .append(filter.value);
            if (i < filters.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escape pipe characters in string
     */
    private static String escapePipe(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("|", "\\|");
    }

    public String pullData(Map<String, Object> inputParams) {
        try {
            // Extract parameters
            String actionType = (String) inputParams.get("actionType");
            List<NLPEntityProcessor.EntityFilter> filters =
                (List<NLPEntityProcessor.EntityFilter>) inputParams.get("filters");
            List<String> displayEntities = (List<String>) inputParams.get("displayEntities");
            int screenWidth =
                inputParams.containsKey("screenWidth") ? (Integer) inputParams.get("screenWidth") :
                DEFAULT_SCREEN_WIDTH;
            String customSqlQuery = (String) inputParams.get("sqlQuery"); // Check for custom SQL

            // DEBUG LOGGING
            System.out.println("=== PULLDATA DEBUG ===");
            System.out.println("Action Type: " + actionType);
            System.out.println("Display Entities List: " + displayEntities);
            System.out.println("Custom SQL Query: " + customSqlQuery);

            // Handle HELP queries directly without any database calls
            if (actionType != null && actionType.startsWith("HELP_")) {
                return BCCTChatBotUtility.getHelpContent(actionType);
            }
            if (isCreatedByQuery(filters)) {
                return handleCreatedByQuery(actionType, filters, displayEntities, screenWidth);
            }

            // If custom SQL is provided, use it directly
            if (customSqlQuery != null && !customSqlQuery.trim().isEmpty()) {
                System.out.println("Using custom SQL query: " + customSqlQuery);
                return executeCustomSqlQuery(customSqlQuery, filters, displayEntities, screenWidth, actionType);
            }

            // Convert List<String> to comma-separated string for Model layer
            String displayColumns = null;
            if (displayEntities != null && !displayEntities.isEmpty()) {
                displayColumns = String.join(",", displayEntities);
                System.out.println("Display Columns String: " + displayColumns);
            }

            // Convert filters to comma-separated strings
            String filterAttributesStr = null;
            String filterValuesStr = null;
            String filterOperationsStr = null;

            if (filters != null && !filters.isEmpty()) {
                StringBuilder attrs = new StringBuilder();
                StringBuilder vals = new StringBuilder();
                StringBuilder ops = new StringBuilder();

                for (int i = 0; i < filters.size(); i++) {
                    if (i > 0) {
                        attrs.append(",");
                        vals.append(",");
                        ops.append(",");
                    }
                    NLPEntityProcessor.EntityFilter filter = filters.get(i);
                    attrs.append(filter.attribute);
                    vals.append(filter.value);
                    ops.append(filter.operation);
                }

                filterAttributesStr = attrs.toString();
                filterValuesStr = vals.toString();
                filterOperationsStr = ops.toString();
            }

            // Call Model layer to get raw data
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeNLPQuery");
            operationBind.getParamsMap().put("actionType", actionType);
            operationBind.getParamsMap().put("filterAttributes", filterAttributesStr);
            operationBind.getParamsMap().put("filterValues", filterValuesStr);
            operationBind.getParamsMap().put("filterOperations", filterOperationsStr);
            operationBind.getParamsMap().put("displayColumns", displayColumns);

            Map<String, Object> queryResult = (Map<String, Object>) operationBind.execute();

            if (!(Boolean) queryResult.get("success")) {
                return "<p><b></b></p>";
            }

            // Get raw data from Model layer
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) queryResult.get("data");
            List<String> columnNames = (List<String>) queryResult.get("columnNames");
            int rowCount = (Integer) queryResult.get("rowCount");

            System.out.println("Retrieved " + rowCount + " rows with columns: " + columnNames);

            // Format data in View layer (UI logic)
            return formatQueryResultInView(dataRows, columnNames, displayEntities, screenWidth, actionType);

        } catch (Exception ex) {
            ex.printStackTrace();
            return "<p><b></b></p>";
        }
    }

    /**
     * Execute custom SQL query directly
     */
    private String executeCustomSqlQuery(String sqlQuery, List<NLPEntityProcessor.EntityFilter> filters,
                                         List<String> displayEntities, int screenWidth, String actionType) {
        try {
            System.out.println("DEBUG: Executing custom SQL: " + sqlQuery);

            // Extract parameter values from filters
            List<String> paramValues = new ArrayList<>();
            for (NLPEntityProcessor.EntityFilter filter : filters) {
                paramValues.add(filter.value);
            }

            // Convert to arrays for the existing method
            String[] paramValuesArray = paramValues.toArray(new String[0]);
            String[] paramTypesArray = new String[paramValues.size()];
            for (int i = 0; i < paramTypesArray.length; i++) {
                paramTypesArray[i] = "String"; // Default to String type
            }

            // Convert display entities to comma-separated string
            String displayColumns = String.join(",", displayEntities);

            // Use the existing getFormattedQueryResults method
            return getFormattedQueryResults(sqlQuery, paramValuesArray, paramTypesArray, displayColumns, screenWidth,
                                            actionType);

        } catch (Exception e) {
            System.out.println("DEBUG: Error executing custom SQL: " + e.getMessage());
            e.printStackTrace();
            return "<p><b></b></p>";
        }
    }


    private String formatQueryResultInView(List<Map<String, Object>> dataRows, List<String> columnNames,
                                           List<String> displayEntities, int screenWidth, String actionType) {
        StringBuilder response = new StringBuilder();
        try {
            // Use only the requested displayEntities if not empty
            List<String> columnsToShow =
                (displayEntities != null && !displayEntities.isEmpty()) ? displayEntities : columnNames;
            boolean useTabular = shouldUseTabularFormat(columnsToShow, screenWidth);
            response.append(getFormattedHeader(actionType));
            if (dataRows.isEmpty()) {
                response.append("<p><i></i></p>");
            } else if (useTabular) {
                response.append(formatTabularResultFromData(dataRows, columnsToShow));
            } else {
                response.append(formatLineByLineLayout(dataRows, columnsToShow));
            }
            response.append(addProfessionalNote());
        } catch (Exception e) {
            response.append("<p><b></b></p>");
        }
        return response.toString();
    }

    private String formatLineByLineLayout(List<Map<String, Object>> dataRows, List<String> displayEntities) {
        if (dataRows == null || dataRows.isEmpty() || displayEntities == null || displayEntities.isEmpty()) {
            return "<p><i>No data available</i></p>";
        }
        StringBuilder response = new StringBuilder();
        for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
            Map<String, Object> row = dataRows.get(rowIndex);
            if (row == null)
                continue;
            if (rowIndex > 0) {
                response.append("<br><hr><br>");
            }
            if (dataRows.size() > 1) {
                response.append("<h5><b>Record ")
                        .append(rowIndex + 1)
                        .append("</b></h5>");
            }
            for (String entity : displayEntities) {
                String displayName = getDisplayName(entity);
                Object valueObj = row.get(entity);
                String value = valueObj != null ? valueObj.toString() : "N/A";
                response.append("<p>")
                        .append("<b>")
                        .append(displayName)
                        .append(":</b> ")
                        .append(escapeHtml(value))
                        .append("</p>");
            }
        }
        return response.toString();
    }

    private String formatTabularLayout(List<Map<String, Object>> dataRows, List<String> displayEntities) {
        if (dataRows == null || dataRows.isEmpty() || displayEntities == null || displayEntities.isEmpty()) {
            return "<p><i>No data available</i></p>";
        }
        StringBuilder response = new StringBuilder();
        response.append("<pre><small>");
        response.append("<b>");
        for (String entity : displayEntities) {
            String displayName = getDisplayName(entity);
            response.append(String.format("%-20s ", truncateString(displayName, 18)));
        }
        response.append("</b><br>");
        int totalWidth = displayEntities.size() * 21;
        response.append(repeatString("=", totalWidth)).append("<br>");
        for (Map<String, Object> row : dataRows) {
            if (row != null) {
                for (String entity : displayEntities) {
                    Object valueObj = row.get(entity);
                    String value = valueObj != null ? valueObj.toString() : "N/A";
                    value = truncateString(value, 18);
                    response.append(String.format("%-20s ", value));
                }
                response.append("<br>");
            }
        }
        return response.toString();
    }

    /**
     * Format result in label-value format from data rows
     */

    private String formatLabelValueResultFromData(List<Map<String, Object>> dataRows, List<String> displayEntities) {
        System.out.println("formatLabelValueResultFromData====================>");
        StringBuilder response = new StringBuilder();

        int recordCount = 0;
        for (Map<String, Object> row : dataRows) {
            recordCount++;
            response.append("<h4>Record ")
                    .append(recordCount)
                    .append("</h4>");
            response.append("<p>");

            if (displayEntities != null && !displayEntities.isEmpty()) {
                for (String entity : displayEntities) {
                    String value = (String) row.get(entity);
                    value = value != null ? value : "N/A";

                    // Format dates if the field contains date-related keywords
                    if (entity.contains("DATE") || entity.contains("EFFECTIVE") || entity.contains("EXPIRATION")) {
                        value = formatDate(value);
                    }

                    response.append("<b>")
                            .append(getDisplayName(entity))
                            .append(":</b> ");
                    response.append("<span>")
                            .append(value)
                            .append("</span>");
                    response.append("<br>");
                }
            }
            response.append("</p>");
            response.append("<hr>");
        }

        return response.toString();
    }

    /**
     * Set parameters from filters to prepared statement
     */
    private void setParametersFromFilters(PreparedStatement stmt,
                                          List<NLPEntityProcessor.EntityFilter> filters) throws SQLException {
        if (filters != null) {
            for (int i = 0; i < filters.size(); i++) {
                NLPEntityProcessor.EntityFilter filter = filters.get(i);
                stmt.setString(i + 1, filter.value);
            }
        }
    }

    /**
     * Format query result based on display entities and screen width
     */
    private String formatQueryResult(ResultSet resultset, List<String> displayEntities, int screenWidth,
                                     String actionType) throws SQLException {
        StringBuilder response = new StringBuilder();

        // Determine format type based on number of display entities and screen width
        boolean useTabular = shouldUseTabularFormat(displayEntities, screenWidth);

        // Add header
        response.append(getFormattedHeader(actionType));

        if (useTabular) {
            response.append(formatTabularResult(resultset, displayEntities));
        } else {
            response.append(formatLabelValueResult(resultset, displayEntities));
        }

        response.append(addProfessionalNote());
        return response.toString();
    }

    /**
     * Determine if tabular format should be used
     */
    private boolean shouldUseTabularFormat(List<String> displayEntities, int screenWidth) {
        if (displayEntities == null || displayEntities.isEmpty()) {
            return false; // Default to label-value for unknown entities
        }

        // ALWAYS use label-value format for contract info (line by line display)
        // This ensures contract information is always displayed as individual lines
        // rather than in a paragraph or tabular format
        return false;
    }

    /**
     * Format result in tabular format
     */
    private String formatTabularResult(ResultSet resultset, List<String> displayEntities) throws SQLException {
        System.out.println("formatTabularResult====================>");
        StringBuilder response = new StringBuilder();

        response.append("<pre>");
        response.append("<small>");
        response.append("<b>");

        // Header row
        if (displayEntities != null && !displayEntities.isEmpty()) {
            for (String entity : displayEntities) {
                response.append(String.format("%-20s ", getDisplayName(entity)));
            }
        }
        response.append("</b>");
        response.append("<br>");

        // Separator


        response.append(repeatString("=", (displayEntities.size() * 21)));
        response.append("<br>");

        // Data rows
        while (resultset.next()) {
            for (String entity : displayEntities) {
                String value = resultset.getString(entity);
                value = value != null ? value : "N/A";
                value = truncateString(value, 18);
                response.append(String.format("%-20s ", value));
            }
            response.append("<br>");
        }
        response.append(repeatString("=", (displayEntities.size() * 21)));
        response.append("</small>");
        response.append("</pre>");

        return response.toString();
    }

    /**
     * Format result in label-value format
     */
    private String formatLabelValueResult(ResultSet resultset, List<String> displayEntities) throws SQLException {
        System.out.println("formatLabelValueResult===============>");
        StringBuilder response = new StringBuilder();

        int recordCount = 0;
        while (resultset.next()) {
            recordCount++;
            response.append("<h4>Record ")
                    .append(recordCount)
                    .append("</h4>");
            response.append("<p>");

            if (displayEntities != null && !displayEntities.isEmpty()) {
                for (String entity : displayEntities) {
                    String value = resultset.getString(entity);
                    value = value != null ? value : "N/A";

                    response.append("<b>")
                            .append(getDisplayName(entity))
                            .append(":</b> ");
                    response.append("<span>")
                            .append(value)
                            .append("</span>");
                    response.append("<br>");
                }
            }
            response.append("</p>");
            response.append("<hr>");
        }

        if (recordCount == 0) {
            response.append("<p><i>No records found.</i></p>");
        }

        return response.toString();
    }


    /**
     * Get formatted header based on action type
     */
    private String getFormattedHeader(String actionType) {
        StringBuilder header = new StringBuilder();

        switch (actionType) {
        case "contracts_by_contractnumber":
        case "contracts_by_filter":
            header.append("<h3>Contract Information</h3>");
            break;
        case "parts_by_contract_number":
        case "parts_by_part_number":
        case "parts_by_filter":
            header.append("<h3>Parts Information</h3>");
            break;
        case "parts_failed_by_contract_number":
            header.append("<h3>Failed Parts Information</h3>");
            break;
        default:
            header.append("<h3>Query Results</h3>");
        }

        header.append("<hr>");
        return header.toString();
    }

    /**
     * Get display name for database column using centralized configuration
     */
    private String getDisplayName(String columnName) {

        // Check if column exists in any table and use business term mapping if available
        String mappedColumn =
            TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, columnName.toLowerCase());
        if (mappedColumn != null) {
            columnName = mappedColumn;
        } else {
            mappedColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, columnName.toLowerCase());
            if (mappedColumn != null) {
                columnName = mappedColumn;
            } else {
                mappedColumn =
                    TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_FAILED_PARTS,
                                                          columnName.toLowerCase());
                if (mappedColumn != null) {
                    columnName = mappedColumn;
                }
            }
        }

        // Fallback to hardcoded mappings if not found in centralized config
        switch (columnName.toLowerCase()) {
        case "award_number":
            return "Award#";
        case "contract_name":
            return "Award Name";
        case "customer_number":
            return "Customer#";
        case "customer_name":
            return "Customer Name";
        case "expiration_date":
            return "Expiration";
        case "price_expiration_date":
            return "Price Exp";
        case "award_rep":
            return "Owner";
        case "effective_date":
            return "Effective Date";
        case "lead_time":
            return "Lead Time";
        case "part_number":
            return "Part#";
        case "invoice_part_number":
            return "Invoice Part#";
        case "part_description":
            return "Description";
            case "create_date":
                return "Created on";
            case "status":
                return "Status of Contract";
        default:
            return columnName.replace("_", " ");
        }
    }


    /**
     * Format date string
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.equals("N/A"))
            return "N/A";

        try {
            // Handle Oracle date format: "2024-01-01 00:00:00.0"
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+")) {
                // Extract just the date part and format as MM-DD-YYYY
                String datePart = dateStr.substring(0, 10); // "2024-01-01"
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    return parts[1] + "-" + parts[2] + "-" + parts[0]; // "01-01-2024"
                }
            }

            // Handle other date formats if needed
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = dateStr.split("-");
                if (parts.length == 3) {
                    return parts[1] + "-" + parts[2] + "-" + parts[0];
                }
            }

            return dateStr; // Return original if no pattern matches
        } catch (Exception e) {
            return dateStr; // Return original on any error
        }
    }

    /**
     * Add professional note at the end
     */
    private String addProfessionalNote() {
        return "<br><br><br>" + "<small>" + "<p><b>Note:</b><br>" + "Data retrieved from BCCT database.<br>" +
               "For detailed information, please contact your system administrator." + "</p>" + "</small>";
    }

    /**
     * Convert NLPEntityProcessor.EntityFilter to StandardJSONProcessor.EntityFilter
     */
    private List<StandardJSONProcessor.EntityFilter> convertToStandardFilters(List<NLPEntityProcessor.EntityFilter> filters) {
        List<StandardJSONProcessor.EntityFilter> convertedFilters = new ArrayList<>();
        if (filters != null) {
            for (NLPEntityProcessor.EntityFilter filter : filters) {
                convertedFilters.add(new StandardJSONProcessor.EntityFilter(filter.attribute, filter.operation,
                                                                            filter.value, filter.source));
            }
        }
        return convertedFilters;
    }

    /**
     * Enhanced parts handler with database integration
     */
    private String handlePartsByContractNumberWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                               List<String> displayEntities, String userInput) {
        try {
            // Use default columns if none specified
            List<String> finalDisplayEntities = getDefaultDisplayEntities(displayEntities, "parts");

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "parts_by_contract_number");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", finalDisplayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Enhanced failed parts handler with database integration
     */
    private String handleFailedPartsByContractNumberWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                                     List<String> displayEntities, String userInput) {
        try {
            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "parts_failed_by_contract_number");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", displayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Enhanced parts by part number handler with database integration
     */
    private String handlePartsByPartNumberWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                           List<String> displayEntities, String userInput) {
        try {
            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "parts_by_part_number");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", displayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    private String handleContractsByFilterWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                           List<String> displayEntities, String userInput) {
        try {
            // Use default columns if none specified
            List<String> finalDisplayEntities = getDefaultDisplayEntities(displayEntities, "contracts");

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "contracts_by_filter");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", finalDisplayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Enhanced parts by filter handler with database integration
     */
    private String handlePartsByFilterWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                       List<String> displayEntities, String userInput) {
        try {
            // Use default columns if none specified
            List<String> finalDisplayEntities = getDefaultDisplayEntities(displayEntities, "parts");

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "parts_by_filter");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", finalDisplayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Enhanced update contract handler with database integration
     */
    private String handleUpdateContractWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                        List<String> displayEntities, String userInput) {
        try {
            // For update operations, we might want to show the updated record
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "update_contract");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", displayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute update operation (this would need additional logic for actual updates)
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Enhanced create contract handler with database integration
     */
    private String handleCreateContractWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                        List<String> displayEntities, String userInput) {
        try {
            // For create operations, return creation form or confirmation
            return "<h4>Contract Creation</h4>" +
                   "<p>To create a new contract, please provide the following information:</p>" + "<ul>" +
                   "<li>Contract Number</li>" + "<li>Contract Name</li>" + "<li>Customer Information</li>" +
                   "<li>Effective Date</li>" + "<li>Expiration Date</li>" + "</ul>" +
                   "<p><i>Please contact your administrator for contract creation assistance.</i></p>";

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Enhanced help contract create user handler
     */
    private String handleHelpContractCreateUserWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                                List<String> displayEntities, String userInput) {
        try {
            return "<h4>Contract Creation Steps</h4>" + "<ol>" + "<li><b>Gather Required Information:</b>" +
                   "<ul><li>Contract/Award Number</li><li>Contract Name</li><li>Customer Details</li><li>Dates (Effective, Expiration, Price Expiration)</li></ul></li>" +
                   "<li><b>Access Contract Module:</b> Navigate to the contracts section in your system</li>" +
                   "<li><b>Fill Contract Details:</b> Enter all required information accurately</li>" +
                   "<li><b>Review and Submit:</b> Double-check all information before saving</li>" +
                   "<li><b>Confirmation:</b> Wait for system confirmation of contract creation</li>" + "</ol>" +
                   "<p><b>Note:</b> Ensure you have proper authorization before creating contracts.</p>";

        } catch (Exception e) {
            return "<p><b>Error in help request:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Enhanced help contract create bot handler
     */
    private String handleHelpContractCreateBotWithDataProvider(List<NLPEntityProcessor.EntityFilter> filters,
                                                               List<String> displayEntities, String userInput) {
        try {
            return "<h4>Automated Contract Creation</h4>" +
                   "<p>I can help you create a contract! Please provide the following details:</p>" +
                   "<p><b>Required Information:</b></p>" + "<ul>" + "<li>Contract/Award Number</li>" +
                   "<li>Contract Name</li>" + "<li>Customer Number and Name</li>" + "<li>Effective Date</li>" +
                   "<li>Expiration Date</li>" + "<li>Price Expiration Date</li>" + "<li>Award Representative</li>" +
                   "</ul>" +
                   "<p><b>Example:</b> &quot;Create contract ABC123 named 'IT Services Contract' for customer C001 'Tech Corp' effective 2024-01-01 expiring 2024-12-31&quot;</p>" +
                   "<p><i>Please provide the contract details and I'll help you create it.</i></p>";

        } catch (Exception e) {
            return "<p><b>Error in help request:</b> " + e.getMessage() + "</p>";
        }
    }

    // ============================================================================
    // ENHANCED MAIN PROCESSING METHODS WITH DATABASE INTEGRATION
    // ============================================================================

    /**
     * Enhanced METHOD 1: Get complete response as String with database integration
     */
    public String processUserInputCompleteResponse(String userInput) {
        return processUserInputCompleteResponse(userInput, DEFAULT_SCREEN_WIDTH);
    }

    /**
     * Enhanced METHOD 1 with screen width parameter
     */
    public String processUserInputCompleteResponse(String userInput, int screenWidth) {
        System.out.println("processUserInputCompleteResponse=====================>");
        try {
            // Step 1: Preprocess input using WordDatabase
            String preprocessedInput = preprocessInput(userInput);

            // Step 2: Validate with ContractsModel for business logic
            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);

            // Step 3: Get QueryResult from NLPEntityProcessor
            NLPEntityProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);

            // CRITICAL BUSINESS RULE: Validate contract number for parts queries
            String contractValidationError = validatePartsQueryContractNumber(queryResult);
            if (contractValidationError != null) {
                return createErrorResponseJSON(userInput, contractValidationError);
            }

            // Step 4: Check if this is a HELP query - handle directly without DB calls
            if (queryResult.metadata.actionType != null && queryResult.metadata
                                                                      .actionType
                                                                      .startsWith("HELP_")) {
                String helpContent = BCCTChatBotUtility.getHelpContent(queryResult.metadata.actionType);
                return BCCTChatBotUtility.createCompleteResponseJSONWithHelpContent(queryResult, helpContent);
            }

            // Step 5: For non-HELP queries, execute DataProvider actions with database integration
            String dataProviderResult = executeDataProviderActionWithDB(queryResult, screenWidth);

            // Step 6: Return complete response as JSON string
            return createCompleteResponseJSON(queryResult, dataProviderResult);

        } catch (Exception e) {
            return createErrorResponseJSON(userInput, "Error processing user input: " + e.getMessage());
        }
    }

    /**
     * Execute DataProvider action with database integration
     */
    public String executeDataProviderActionWithDB(NLPEntityProcessor.QueryResult queryResult, int screenWidth) {
        return executeDataProviderActionWithDB(queryResult, screenWidth, null);
    }
    
    public String executeDataProviderActionWithDB(NLPEntityProcessor.QueryResult queryResult, int screenWidth, String sessionId) {
        System.out.println("starting of executeDataProviderActionWithDB==>" + queryResult.displayEntities);
        try {
            // Check if this is a "created by" or "created in" query first
            if (isCreatedByQuery(queryResult.entities) || isCreatedInQuery(queryResult.entities)) {
                // Use the enhanced method that accepts the complete QueryResult object
                return handleCreatedByQueryWithQueryResult(queryResult, screenWidth);
            }

            // Check if this is a date-related query
            if (isDateRelatedQuery(queryResult)) {
                System.out.println("It is date realetd queries=============>");
                return handleDateRelatedQuery(queryResult, screenWidth);
            }


            // Route to appropriate action handler with database integration
            return routeToActionHandlerWithDataProviderDB(queryResult.metadata.actionType, queryResult.entities,
                                                          queryResult.displayEntities,
                                                          queryResult.inputTracking.originalInput, screenWidth);
        } catch (Exception e) {
            return "<p><b>Error executing DataProvider action:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Route to appropriate action handler with database integration
     */
    public String routeToActionHandlerWithDataProviderDB(String actionType,
                                                          List<NLPEntityProcessor.EntityFilter> filters,
                                                          List<String> displayEntities, String userInput,
                                                          int screenWidth) {

        System.out.println("Starting of routeToActionHandlerWithDataProviderDB====>");

        // Update screen width for all handlers
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("actionType", actionType);
        inputParams.put("filters", filters);
        inputParams.put("displayEntities", displayEntities);
        inputParams.put("screenWidth", screenWidth);
        System.out.println("Action Type===>" + actionType);
        switch (actionType) {
        case "contracts_by_contractnumber":
        case "contracts_by_award_numbers":
            return pullData(inputParams);

        case "contracts_by_user":
        case "contracts_by_user_date":
            // Enhanced "created by" queries are handled by handleCreatedByQuery method
            // This case should not be reached as it's handled earlier in executeDataProviderActionWithDB
            return pullData(inputParams);

        case "parts_by_contract_number":
            return pullData(inputParams);

        case "parts_failed_by_contract_number":
            return pullData(inputParams);

        case "parts_by_part_number":
            if (!hasContractNumberInFilters(filters)) {
                return generateContractNumberRequiredMessage("PARTS");
            }
            return pullData(inputParams);

        case "contracts_by_filter":
            return pullData(inputParams);

        case "parts_by_filter":
            // Validate that contract number is present for parts filtering
            if (!hasContractNumberInFilters(filters)) {
                return generateContractNumberRequiredMessage("PARTS");
            }
            return pullData(inputParams);

        case "update_contract":
            return handleUpdateContractWithDataProvider(filters, displayEntities, userInput);

        case "create_contract":
            return handleCreateContractWithDataProvider(filters, displayEntities, userInput);

        case "HELP_CONTRACT_CREATE_USER":
            return handleHelpContractCreateUserWithDataProvider(filters, displayEntities, userInput);

        case "HELP_CONTRACT_CREATE_BOT":
            return handleHelpContractCreateBotWithDataProvider(filters, displayEntities, userInput);

        default:
            return "<p><b>Unknown action type:</b> " + actionType + "</p>";
        }
    }

    /**
     * Enhanced METHOD 3: Get Java object with database integration
     */
    public UserActionResponse processUserInputCompleteObject(String userInput) {
        return processUserInputCompleteObject(userInput, DEFAULT_SCREEN_WIDTH);
    }

    public UserActionResponse processUserInputCompleteObject(String userInput, int screenWidth) {
        try {
            // Step 1: Conversational flow check (use existing logic)
            // If session/context is available, use flowManager.isFollowUpResponse or similar
            // For now, proceed with normal flow

            // Step 2: Preprocess input (typo correction, normalization)
            String preprocessedInput = preprocessInput(userInput);

            // Step 3: Use NLPEntityProcessor for all entity extraction and classification
            NLPEntityProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);

            // Step 4: Build standardized response
            UserActionResponse response = new UserActionResponse();
            response.setOriginalInput(queryResult.inputTracking.originalInput);
            response.setCorrectedInput(queryResult.inputTracking.correctedInput);
            response.setQueryType(queryResult.metadata.queryType);
            response.setActionType(queryResult.metadata.actionType);
            response.setDisplayEntities(queryResult.displayEntities);
            response.setFilters(convertToStandardFilters(queryResult.entities));
            response.setSuccess(true);
            response.setMessage("Request processed successfully");
            response.setNlpProcessingTime(queryResult.metadata.processingTimeMs);

            // Step 5: Add parameters if available from business validation (contractsValidation)
            // (Assume contractsValidation is available if needed)
            // Not using queryResult.metadata.parameters as it does not exist

            // Step 6: For HELP queries, add help content
            if (queryResult.metadata.actionType != null && queryResult.metadata
                                                                      .actionType
                                                                      .startsWith("HELP_")) {
                String helpContent = BCCTChatBotUtility.getHelpContent(queryResult.metadata.actionType);
                response.addParameter("isHelpQuery", true);
                response.addParameter("helpContent", helpContent);
                response.addParameter("screenWidth", screenWidth);
                return response;
            }

            // Step 7: For non-HELP queries, execute DataProvider action and add result
            String dataProviderResult = executeDataProviderActionWithDB(queryResult, screenWidth);
            response.addParameter("isHelpQuery", false);
            response.addParameter("dataProviderResult", dataProviderResult);
            response.addParameter("screenWidth", screenWidth);

            // Step 8: Generate and set SQL query for non-HELP queries
            String sqlQuery = generateSQLQuery(queryResult);
            response.addParameter("generatedSQL", sqlQuery);

            return response;
        } catch (Exception e) {
            return createErrorResponse(userInput, "Error processing user input: " + e.getMessage());
        }
    }

    // ============================================================================
    // UTILITY METHODS FOR SCREEN WIDTH MANAGEMENT
    // ============================================================================

    /**
     * Set screen width for responsive formatting
     */
    public void setScreenWidth(int screenWidth) {
        // This could be stored as an instance variable if needed
        // For now, it's passed as parameter to methods
    }

    /**
     * Get optimal column width based on screen width
     */
    private int getOptimalColumnWidth(int screenWidth, int numberOfColumns) {
        // Reserve some space for padding and borders
        int availableWidth = screenWidth - 50;
        return Math.max(10, availableWidth / numberOfColumns);
    }

    /**
     * Determine if responsive design should switch to mobile layout
     */
    private boolean isMobileLayout(int screenWidth) {
        return screenWidth < 600;
    }


    public String getFormattedQueryResults(String sqlQuery, String[] paramValues, String[] paramTypes,
                                           String displayColumns, int screenWidth, String actionType) {
        try {
            // Execute query
            Map<String, Object> queryResult = null;
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", paramValues);
            operationBind.getParamsMap().put("paramTypes", paramTypes);
            queryResult = (Map<String, Object>) operationBind.execute();

            //vinod.executeDynamicQuery(sqlQuery, filterValues, paramTypes);

            if (!(Boolean) queryResult.get("success")) {
                return "<p><b></b></p>";
            }

            // Format results
            List<Map<String, Object>> rows = (List<Map<String, Object>>) queryResult.get("rows");
            String[] displayColumnArray =
                displayColumns != null && !displayColumns.trim().isEmpty() ? displayColumns.split(",") : null;

            return formatQueryResultsForUI(rows, displayColumnArray, screenWidth, actionType);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }


    private String arrayToString(String[] array) {
        if (array == null || array.length == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    private String formatTabularResultInModel(List<Map<String, Object>> rows, String[] displayColumns) {
        System.out.println("formatTabularResultInModel===============>");
        StringBuilder response = new StringBuilder();

        response.append("<pre>");
        response.append("<small>");
        response.append("<b>");

        // Header row
        if (displayColumns != null && displayColumns.length > 0) {
            for (String column : displayColumns) {
                response.append(String.format("%-20s ", getDisplayName(column.trim())));
            }
        }
        response.append("</b>");
        response.append("<br>");

        // Separator
        int separatorLength = displayColumns != null ? displayColumns.length * 21 : 21;
        response.append(repeatString("=", separatorLength));
        response.append("<br>");

        // Data rows
        for (Map<String, Object> row : rows) {
            if (displayColumns != null) {
                for (String column : displayColumns) {
                    Object value = row.get(column.trim());
                    String valueStr = value != null ? value.toString() : "N/A";
                    valueStr = truncateString(valueStr, 18);
                    response.append(String.format("%-20s ", valueStr));
                }
            }
            response.append("<br>");
        }

        // Bottom separator
        response.append(repeatString("=", separatorLength));
        response.append("</small>");
        response.append("</pre>");

        return response.toString();
    }

    /**
     * Format label-value result in model (missing method)
     */
    private String formatLabelValueResultInModel(List<Map<String, Object>> rows, String[] displayColumns) {
        System.out.println("formatLabelValueResultInModel====================>");
        StringBuilder response = new StringBuilder();

        int recordCount = 0;
        for (Map<String, Object> row : rows) {
            recordCount++;
            response.append("<h4>Record ")
                    .append(recordCount)
                    .append("</h4>");
            response.append("<p>");

            if (displayColumns != null && displayColumns.length > 0) {
                for (String column : displayColumns) {
                    Object value = row.get(column.trim());
                    String valueStr = value != null ? value.toString() : "N/A";

                    response.append("<b>")
                            .append(getDisplayName(column.trim()))
                            .append(":</b> ");
                    response.append("<span>")
                            .append(valueStr)
                            .append("</span>");
                    response.append("<br>");
                }
            }
            response.append("</p>");
            response.append("<hr>");
        }

        if (recordCount == 0) {
            response.append("<p><i>No records found.</i></p>");
        }

        return response.toString();
    }


    /**
     * Add professional note in model (missing method)
     */
    private String addProfessionalNoteInModel() {
        return "<br><br><br>" + "<small>" + "<p><b>Note:</b><br>" + "Data retrieved from BCCT database.<br>" +
               "For detailed information, please contact your system administrator." + "</p>" + "</small>";
    }

    /**
     * Get formatted header for action (missing method)
     */
    private String getFormattedHeaderForAction(String actionType) {
        StringBuilder header = new StringBuilder();

        switch (actionType) {
        case "contracts_by_contractnumber":
        case "contracts_by_filter":
            header.append("<h3>Contract Information</h3>");
            break;
        case "parts_by_contract_number":
        case "parts_by_part_number":
        case "parts_by_filter":
            header.append("<h3>Parts Information</h3>");
            break;
        case "parts_failed_by_contract_number":
            header.append("<h3>Failed Parts Information</h3>");
            break;
        default:
            header.append("<h3>Query Results</h3>");
        }

        header.append("<hr>");
        return header.toString();
    }

    /**
     * Should use tabular format in model (missing method)
     */
    private boolean shouldUseTabularFormatInModel(String[] displayColumns, int screenWidth) {
        if (displayColumns == null || displayColumns.length == 0) {
            return false; // Default to label-value for unknown entities
        }

        // ALWAYS use label-value format for contract info (line by line display)
        // This ensures contract information is always displayed as individual lines
        // rather than in a paragraph or tabular format
        return false;
    }

    // Fix 4: Update the formatQueryResultsForUI method to remove duplicates
    private String formatQueryResultsForUI(List<Map<String, Object>> rows, String[] displayColumns, int screenWidth,
                                           String actionType) {
        StringBuilder response = new StringBuilder();

        // Add header
        response.append(getFormattedHeaderForAction(actionType));

        // Determine format type
        boolean useTabular = shouldUseTabularFormatInModel(displayColumns, screenWidth);

        if (useTabular) {
            response.append(formatTabularResultInModel(rows, displayColumns));
        } else {
            response.append(formatLabelValueResultInModel(rows, displayColumns));
        }

        response.append(addProfessionalNoteInModel());
        return response.toString();
    }

    private boolean isHelpQuery(String actionType) {
        return actionType != null && actionType.startsWith("HELP_");
    }

    private boolean isCreatedByQuery(List<NLPEntityProcessor.EntityFilter> filters) {
        if (filters == null)
            return false;

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            String createdByColumn =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACT_CONTACTS, "created_by");
            if (createdByColumn != null && filter.attribute.equals(createdByColumn)) {
                return true;
            }
            // Also check for direct AWARD_REP or CREATED_BY references
            if ("AWARD_REP".equals(filter.attribute) || "CREATED_BY".equals(filter.attribute)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the query is a "created in" query (by date/year)
     */
    private boolean isCreatedInQuery(List<NLPEntityProcessor.EntityFilter> filters) {
        if (filters == null)
            return false;

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            // Check for CREATE_DATE or creation date related filters
            if ("CREATE_DATE".equals(filter.attribute) || filter.attribute
                                                                .toLowerCase()
                                                                .contains("create") || filter.attribute
                                                                                             .toLowerCase()
                                                                                             .contains("date")) {
                // Additional check: if the value contains a year (4 digits), it's likely a "created in" query
                if (filter.value != null && filter.value
                                                  .toString()
                                                  .matches(".*\\b(19|20)\\d{2}\\b.*")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Add instance variables for user search caching
    private Map<String, List<Map<String, String>>> userSearchCache = new HashMap<>();
    private List<String> userDisplayOrder = new ArrayList<>(); // Store the display order
    private long cacheExpiryTime = 300000; // 5 minutes cache
    private long lastCacheTime = 0;
    
    // Add instance variables for contract search caching
    private Map<String, List<Map<String, Object>>> contractSearchCache = new HashMap<>();
    private List<String> contractDisplayOrder = new ArrayList<>();
    private long contractCacheExpiryTime = 300000; // 5 minutes cache
    private long lastContractCacheTime = 0;

    /**
     * Enhanced "created by" query handler with user search and date filters
     * This method is called from executeDataProviderActionWithDB with NLPEntityProcessor.QueryResult
     */
    private String handleCreatedByQuery(String actionType, List<NLPEntityProcessor.EntityFilter> filters,
                                        List<String> displayEntities, int screenWidth) {
        try {
            // Extract username and date filters
            String username = extractUsernameFromFilters(filters);
            Map<String, Object> dateFilters = extractDateFilters(filters);

            if (username == null || username.trim().isEmpty()) {
                return "<p><b>Error:</b> Username not found in query. Please specify who created the contracts.</p>";
            }

            // Check if this is a user selection (1, 2, 3 or full name)
            if (isUserSelection(username)) {
                return handleUserSelection(username, filters);
            }

            // Search for users with the given name
            Map<String, Object> searchResult = searchUsersInContractContacts(username, dateFilters);

            if (!(Boolean) searchResult.get("success")) {
                return "<p><b>Error:</b> " + searchResult.get("message") + "</p>";
            }

            List<String> users = (List<String>) searchResult.get("users");
            int userCount = (Integer) searchResult.get("userCount");

            if (userCount == 0) {
                return "<p><i>No users found with name '" + username + "'</i></p>";
            }

            if (userCount == 1) {
                // Single user found - get their contracts directly
                String selectedUser = users.get(0);
                return getContractsForSelectedUser(selectedUser, displayEntities, screenWidth, dateFilters);
            } else {
                // Multiple users found - show selection list
                return createUserSelectionResponse(users, username);
            }

        } catch (Exception e) {
            return "<p><b>Error processing created by query:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Enhanced "created by" and "created in" query handler that accepts NLPEntityProcessor.QueryResult
     * This method provides better integration with the NLP processing pipeline
     */
    private String handleCreatedByQueryWithQueryResult(NLPEntityProcessor.QueryResult queryResult, int screenWidth) {
        try {
            // Check if this is a "created in" query (by date/year) or "created by" query (by user)
            boolean isCreatedInQuery = isCreatedInQuery(queryResult.entities);
            boolean isCreatedByQuery = isCreatedByQuery(queryResult.entities);

            // Extract username and date filters from the complete query result
            String username = extractUsernameFromFilters(queryResult.entities);
            Map<String, Object> dateFilters = extractDateFilters(queryResult.entities);

            // Also check the original input for additional context
            String originalInput = queryResult.inputTracking
                                              .originalInput
                                              .toLowerCase();

            // Handle "created in" queries (by date/year)
            if (isCreatedInQuery && !isCreatedByQuery) {
                return handleCreatedInQuery(queryResult, screenWidth, dateFilters);
            }

            // Handle "created by" queries (by user)
            if (isCreatedByQuery) {
                if (username == null || username.trim().isEmpty()) {
                    return "<p><b>Error:</b> Username not found in query. Please specify who created the contracts.</p>";
                }

                // Check if this is a user selection (1, 2, 3 or full name)
                if (isUserSelection(username)) {
                    return handleUserSelection(username, queryResult.entities);
                }

                // Search for users with the given name
                Map<String, Object> searchResult = searchUsersInContractContacts(username, dateFilters);

                if (!(Boolean) searchResult.get("success")) {
                    return "<p><b>Error:</b> " + searchResult.get("message") + "</p>";
                }

                List<String> users = (List<String>) searchResult.get("users");
                int userCount = (Integer) searchResult.get("userCount");

                if (userCount == 0) {
                    return "<p><i>No users found with name '" + username + "'</i></p>";
                }

                if (userCount == 1) {
                    // Single user found - get their contracts directly
                    String selectedUser = users.get(0);
                    return getContractsForSelectedUser(selectedUser, queryResult.displayEntities, screenWidth,
                                                       dateFilters);
                } else {
                    // Multiple users found - show selection list
                    return createUserSelectionResponse(users, username);
                }
            }

            // Fallback for mixed queries or unclear intent
            return "<p><b>Error:</b> Unable to determine if this is a 'created by' or 'created in' query. Please be more specific.</p>";

        } catch (Exception e) {
            return "<p><b>Error processing created by/in query:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Handle "created in" queries (by date/year)
     * Note: Entity extraction is handled by NLPQueryClassifier, this method only handles business logic
     */
    private String handleCreatedInQuery(NLPEntityProcessor.QueryResult queryResult, int screenWidth,
                                        Map<String, Object> dateFilters) {
        System.out.println("handleCreatedInQuery================>");
        try {
            // Extract year from the query (entity extraction already done by NLPQueryClassifier)
            String year = extractYearFromQuery(queryResult);

            if (year == null || year.trim().isEmpty()) {
                return "<p><b>Error:</b> Year not found in query. Please specify the year (e.g., '2025', '2024').</p>";
            }

            // Build SQL query for contracts created in the specified year
            String sqlQuery = buildCreatedInQueryWithYear(year, dateFilters);

            // Execute the query
            Map<String, Object> result = null;
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[] { year });
            operationBind.getParamsMap().put("paramTypes", new String[] { "String" });
            result = (Map<String, Object>) operationBind.execute();
            System.out.println("Result from executeDynamicQuery'''"+result);

            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }

            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("rows");

            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No contracts found created in " + year + "</i></p>";
            }

            // Format the results
            return formatQueryResultInView(dataRows, getColumnNames(dataRows), queryResult.displayEntities, screenWidth,
                                           "contracts_by_filter");

        } catch (Exception e) {
            return "<p><b>Error processing created in query:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Extract year from the query
     */
    private String extractYearFromQuery(NLPEntityProcessor.QueryResult queryResult) {
        // Check filters for year values
        for (NLPEntityProcessor.EntityFilter filter : queryResult.entities) {
            if (filter.value != null) {
                String value = filter.value.toString();
                // Look for 4-digit year pattern
                if (value.matches(".*\\b(19|20)\\d{2}\\b.*")) {
                    // Extract the year
                    java.util.regex.Pattern pattern = java.util
                                                          .regex
                                                          .Pattern
                                                          .compile("\\b(19|20)\\d{2}\\b");
                    java.util.regex.Matcher matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        }

        // Check original input for year
        String originalInput = queryResult.inputTracking.originalInput;
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b(19|20)\\d{2}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(originalInput);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Build SQL query for contracts created in a specific year
     */
    private String buildCreatedInQueryWithYear(String year, Map<String, Object> dateFilters) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT CONTRACT_NAME, CUSTOMER_NAME, EFFECTIVE_DATE, EXPIRATION_DATE, STATUS, CREATED_BY, CREATE_DATE ");
        sql.append("FROM ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(" ");
        sql.append("WHERE EXTRACT(YEAR FROM CREATE_DATE) = ? ");

        // Add additional date filters if present
        if (dateFilters != null && !dateFilters.isEmpty()) {
            if (dateFilters.containsKey("startDate")) {
                sql.append("AND CREATE_DATE >= ? ");
            }
            if (dateFilters.containsKey("endDate")) {
                sql.append("AND CREATE_DATE <= ? ");
            }
        }

        sql.append("ORDER BY CREATE_DATE DESC");

        return sql.toString();
    }

    /**
     * Get column names from data rows
     */
    private List<String> getColumnNames(List<Map<String, Object>> dataRows) {
        if (dataRows == null || dataRows.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(dataRows.get(0).keySet());
    }

    /**
     * Check if input is a user selection (number or full name)
     */
    private boolean isUserSelection(String input) {
        if (input == null)
            return false;

        // Check if it's a number (1, 2, 3, etc.)
        if (input.matches("^\\d+$")) {
            return true;
        }

        // Check if it's a full name (contains space or is in cache)
        return input.contains(" ") || userSearchCache.containsKey(input);
    }

    /**
     * Handle user selection from the list (1, 2, 3 or full name)
     * Enhanced to handle both user selections and contract selections
     */
    private String handleUserSelection(String userInput, List<NLPEntityProcessor.EntityFilter> filters) {
        // First check if this is a contract selection (contract cache has data)
        if (!contractSearchCache.isEmpty() && System.currentTimeMillis() - lastContractCacheTime <= contractCacheExpiryTime) {
            return handleContractSelection(userInput);
        }

        // Check if user cache is expired
        if (System.currentTimeMillis() - lastCacheTime > cacheExpiryTime) {
            userSearchCache.clear();
            return "<p><b>User selection expired. Please search again.</b></p>";
        }

        String selectedUser = null;

        // If input is a number, get the corresponding user from cache
        if (userInput.matches("^\\d+$")) {
            int userIndex = Integer.parseInt(userInput) - 1;
            int currentIndex = 0;

            for (String cachedUser : userSearchCache.keySet()) {
                if (currentIndex == userIndex) {
                    selectedUser = cachedUser;
                    break;
                }
                currentIndex++;
            }
        } else {
            // Input is a full name, check if it exists in cache
            selectedUser = userInput;
        }

        if (selectedUser == null || !userSearchCache.containsKey(selectedUser)) {
            return "<p><b>Invalid selection. Please try again.</b></p>";
        }

        // Get contracts for selected user
        List<Map<String, String>> userContracts = userSearchCache.get(selectedUser);

        // Clear cache after use
        userSearchCache.clear();
        lastCacheTime = 0;

        // Format and return the contracts
        return formatUserContracts(selectedUser, userContracts);
    }

    /**
     * Handle contract selection from the list (1, 2, 3...)
     */
    private String handleContractSelection(String userInput) {
        // Check if contract cache is expired
        if (System.currentTimeMillis() - lastContractCacheTime > contractCacheExpiryTime) {
            contractSearchCache.clear();
            return "<p><b>Contract selection expired. Please search again.</b></p>";
        }

        // If input is a number, get the corresponding contract from cache
        if (userInput.matches("^\\d+$")) {
            if (contractSearchCache.containsKey(userInput)) {
                List<Map<String, Object>> selectedContract = contractSearchCache.get(userInput);
                
                // Clear cache after use
                contractSearchCache.clear();
                lastContractCacheTime = 0;
                
                // Format and return the selected contract
                return formatSelectedContract(selectedContract.get(0));
            } else {
                return "<p><b>Invalid contract selection. Please try again.</b></p>";
            }
        } else {
            return "<p><b>Please enter a number (1, 2, 3...) to select a contract.</b></p>";
        }
    }

    /**
     * Format selected contract for display
     */
    private String formatSelectedContract(Map<String, Object> contract) {
        StringBuilder response = new StringBuilder();
        response.append("<p><b>Selected Contract Details:</b></p>");
        
        // Create a list with single contract for formatting
        List<Map<String, Object>> contracts = Arrays.asList(contract);
        
        // Use existing formatting method
        return formatQueryResultInView(contracts, getColumnNames(contracts), 
                                      Arrays.asList("AWARD_NUMBER", "CONTRACT_NAME", "CUSTOMER_NAME", 
                                                   "EFFECTIVE_DATE", "EXPIRATION_DATE", "STATUS"), 
                                      DEFAULT_SCREEN_WIDTH, "contracts_by_user");
    }

    /**
     * Search for users in CONTRACT_CONTACTS table with date filters
     */
    private Map<String, Object> searchUsersInContractContacts(String username, Map<String, Object> dateFilters) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Build SQL query with LIKE for partial name matching
            String sqlQuery = buildCreatedByQueryWithDateFilters(username, dateFilters);
            System.out.println("searchUsersInContractContacts==============>" + sqlQuery);
            // Call pullAwardRepsContractsByUser method
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("pullContractsByFilters");
            operationBind.getParamsMap().put("userName", username);
            operationBind.getParamsMap().put("query", sqlQuery);

            // Add date filters if present
            if (dateFilters != null && !dateFilters.isEmpty()) {
                operationBind.getParamsMap().put("dateFilters", dateFilters);
            }

            Map<String, List<Map<String, String>>> queryResult =
                (Map<String, List<Map<String, String>>>) operationBind.execute();

            if (queryResult != null && !queryResult.isEmpty()) {
                // Extract user count (first map key)
                Integer userCount = queryResult.size();

                if (userCount > 1) {
                    // Multiple users found - extract user names and cache the data
                    // Use the same order that will be displayed in the UI
                    List<String> userNames = new ArrayList<>(queryResult.keySet());

                    // Cache the complete data for user selection with display order
                    cacheUserSearchResults(queryResult, userNames);

                    result.put("success", true);
                    result.put("users", userNames);
                    result.put("userCount", userCount);
                    result.put("message", "Multiple users found");
                } else if (userCount == 1) {
                    // Single user found
                    List<String> userNames = new ArrayList<>(queryResult.keySet());
                    result.put("success", true);
                    result.put("users", userNames);
                    result.put("userCount", userCount);
                    result.put("message", "Single user found");
                } else {
                    result.put("success", false);
                    result.put("message", "No users found");
                }
            } else {
                result.put("success", false);
                result.put("message", "No data returned from query");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error searching users: " + e.getMessage());
        }

        return result;
    }

    /**
     * Build SQL query for created by search with date filters
     */
    private String buildCreatedByQueryWithDateFilters(String username, Map<String, Object> dateFilters) {
        StringBuilder sql = new StringBuilder();

        // Base SELECT clause with all required columns
        sql.append("SELECT contracts.AWARD_NUMBER, contracts_Contacts.AWARD_REP, ");
        sql.append("contracts.CONTRACT_NAME, contracts.CUSTOMER_NAME, ");
        sql.append("contracts.EFFECTIVE_DATE, contracts.EXPIRATION_DATE, ");
        sql.append("contracts.CREATE_DATE");

        // FROM clause with JOIN
        sql.append("   FROM ")
           .append(TableColumnConfig.TABLE_CONTRACT_CONTACTS)
           .append(" contracts_Contacts, ");
        sql.append(TableColumnConfig.TABLE_CONTRACTS).append(" contracts ");

        // WHERE clause with join condition and user filter
        sql.append("WHERE contracts_Contacts.AWARD_NUMBER = contracts.AWARD_NUMBER");
        sql.append(" AND contracts_Contacts.AWARD_REP LIKE '%")
           .append(username)
           .append("%'");

        // Add date filters if present
        if (dateFilters != null && !dateFilters.isEmpty()) {
            if (dateFilters.containsKey("year")) {
                sql.append(" AND EXTRACT(YEAR FROM ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE) = ").append(dateFilters.get("year"));
            }
            if (dateFilters.containsKey("afterYear")) {
                sql.append(" AND ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE >= TO_DATE('")
                   .append(dateFilters.get("afterYear"))
                   .append("-01-01', 'YYYY-MM-DD')");
            }
            if (dateFilters.containsKey("beforeYear")) {
                sql.append(" AND ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE < TO_DATE('")
                   .append(dateFilters.get("beforeYear"))
                   .append("-01-01', 'YYYY-MM-DD')");
            }
            if (dateFilters.containsKey("startDate") && dateFilters.containsKey("endDate")) {
                sql.append(" AND ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE BETWEEN TO_DATE('")
                   .append(dateFilters.get("startDate"))
                   .append("', 'YYYY-MM-DD') AND TO_DATE('")
                   .append(dateFilters.get("endDate"))
                   .append("', 'YYYY-MM-DD')");
            }
            if (dateFilters.containsKey("inYear")) {
                sql.append(" AND EXTRACT(YEAR FROM ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE) = ").append(dateFilters.get("inYear"));
            }
            if (dateFilters.containsKey("yearRange")) {
                String[] years = dateFilters.get("yearRange")
                                            .toString()
                                            .split(",");
                if (years.length == 2) {
                    sql.append(" AND EXTRACT(YEAR FROM ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE) BETWEEN ")
                       .append(years[0].trim())
                       .append(" AND ")
                       .append(years[1].trim());
                }
            }
            if (dateFilters.containsKey("monthRange")) {
                String[] dates = dateFilters.get("monthRange")
                                            .toString()
                                            .split(",");
                if (dates.length == 2) {
                    sql.append(" AND ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE BETWEEN DATE '")
                       .append(dates[0].trim())
                       .append("' AND DATE '")
                       .append(dates[1].trim())
                       .append("'");
                }
            }
        }

        // ORDER BY clause
       // sql.append(" ORDER BY ").append(TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)).append(".CREATE_DATE DESC");

        return sql.toString();
    }

    /**
     * Get contracts for selected user
     */
    private String getContractsForSelectedUser(String selectedUser, List<String> displayEntities, int screenWidth,
                                               Map<String, Object> dateFilters) {
        
        System.out.println("getContractsForSelectedUser===================>");
        try {
            // Build SQL query with date filters - now returns full contract data
            String sqlQuery = buildCreatedByQueryWithDateFilters(selectedUser, dateFilters);

            // Execute query
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[0]);
            operationBind.getParamsMap().put("paramTypes", new String[0]);

            Map<String, Object> result = (Map<String, Object>) operationBind.execute();

            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }

            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("data");

            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No contracts found for user '" + selectedUser + "'</i></p>";
            }

            // CRITICAL ENHANCEMENT: Check if multiple contracts found (count > 1)
            if (dataRows.size() > 1) {
                // Multiple contracts found - ask for user confirmation
                return createContractSelectionResponse(dataRows, selectedUser, displayEntities, screenWidth);
            }

            // Single contract found - return directly
            // Ensure we have the required display entities
            List<String> finalDisplayEntities = new ArrayList<>();
            if (displayEntities != null && !displayEntities.isEmpty()) {
                finalDisplayEntities.addAll(displayEntities);
            } else {
                // Default display entities for created by queries
                finalDisplayEntities.addAll(Arrays.asList("AWARD_NUMBER", "AWARD_REP", "CONTRACT_NAME", "CUSTOMER_NAME",
                                                          "EFFECTIVE_DATE", "EXPIRATION_DATE", "CREATE_DATE",
                                                          "STATUS"));
            }

            // Format the results
            return formatQueryResultInView(dataRows, getColumnNames(dataRows), finalDisplayEntities, screenWidth,
                                           "contracts_by_user");

        } catch (Exception e) {
            return "<p><b>Error processing user contracts:</b> " + e.getMessage() + "</p>";
        }
    }


    /**
     * Cache user search results for selection with display order
     */
    private void cacheUserSearchResults(Map<String, List<Map<String, String>>> queryResult, List<String> displayOrder) {
        userSearchCache.clear();
        userSearchCache.putAll(queryResult);
        userDisplayOrder.clear();
        if (displayOrder != null) {
            userDisplayOrder.addAll(displayOrder);
        }
        lastCacheTime = System.currentTimeMillis();
    }

    /**
     * Cache user search results for selection (legacy method)
     */
    private void cacheUserSearchResults(Map<String, List<Map<String, String>>> queryResult) {
        cacheUserSearchResults(queryResult, new ArrayList<>(queryResult.keySet()));
    }
    
    /**
     * Cache contract search results for selection
     */
    private void cacheContractSearchResults(Map<String, List<Map<String, Object>>> contractResults, List<String> displayOrder) {
        contractSearchCache.clear();
        contractSearchCache.putAll(contractResults);
        contractDisplayOrder.clear();
        if (displayOrder != null) {
            contractDisplayOrder.addAll(displayOrder);
        }
        lastContractCacheTime = System.currentTimeMillis();
    }
    
    /**
     * Get user search cache - Added for compatibility with July 19th 2 AM code
     */
    public Map<String, List<Map<String, String>>> getUserSearchCache() {
        // Check if cache has expired
        if (System.currentTimeMillis() - lastCacheTime > cacheExpiryTime) {
            userSearchCache.clear();
        }
        return userSearchCache;
    }
    
    /**
     * Get contract search cache
     */
    public Map<String, List<Map<String, Object>>> getContractSearchCache() {
        // Check if cache has expired
        if (System.currentTimeMillis() - lastContractCacheTime > contractCacheExpiryTime) {
            contractSearchCache.clear();
        }
        return contractSearchCache;
    }
    
    /**
     * Get user display order - Added for compatibility with July 19th 2 AM code
     */
    public List<String> getUserDisplayOrder() {
        // Return the display order that was used to create the user selection response
        return new ArrayList<>(userDisplayOrder);
    }
    
    /**
     * Get contract display order
     */
    public List<String> getContractDisplayOrder() {
        return new ArrayList<>(contractDisplayOrder);
    }

    /**
     * Create user selection response
     */
    private String createUserSelectionResponse(List<String> users, String searchTerm) {
        StringBuilder response = new StringBuilder();
        response.append("<div useractionrequired=\"true\">");
        response.append("<p><b>We found ")
                .append(users.size())
                .append(" users with name '")
                .append(searchTerm)
                .append("':</b></p>");
        response.append("<p>Please select a user:</p>");
        response.append("<ol>");

        for (int i = 0; i < users.size(); i++) {
            response.append("<li>")
                    .append(users.get(i))
                    .append("</li>");
        }

        response.append("</ol>");
        response.append("<p><i>Enter the number (1, 2, 3...) or the full name to select a user.</i></p>");
        response.append("</div>");
        return response.toString();
    }

    /**
     * Create contract selection response when multiple contracts found for a user
     */
    private String createContractSelectionResponse(List<Map<String, Object>> contracts, String selectedUser, 
                                                   List<String> displayEntities, int screenWidth) {
        StringBuilder response = new StringBuilder();
        response.append("<p><b>We found ")
                .append(contracts.size())
                .append(" contracts created by '")
                .append(selectedUser)
                .append("':</b></p>");
        response.append("<p>Please select a contract:</p>");
        response.append("<ol>");

        // Cache contract data for selection
        Map<String, List<Map<String, Object>>> contractCache = new HashMap<>();
        List<String> contractDisplayOrder = new ArrayList<>();

        for (int i = 0; i < contracts.size(); i++) {
            Map<String, Object> contract = contracts.get(i);
            
            // Create contract identifier (Award Number + Contract Name)
            String awardNumber = (String) contract.get("AWARD_NUMBER");
            String contractName = (String) contract.get("CONTRACT_NAME");
            String customerName = (String) contract.get("CUSTOMER_NAME");
            
            String contractIdentifier = awardNumber != null ? awardNumber : 
                                      (contractName != null ? contractName : "Contract " + (i + 1));
            
            // Create display text
            StringBuilder displayText = new StringBuilder();
            displayText.append(contractIdentifier);
            
            if (customerName != null && !customerName.trim().isEmpty()) {
                displayText.append(" - ").append(customerName);
            }
            
            // Add status if available
            String status = (String) contract.get("STATUS");
            if (status != null && !status.trim().isEmpty()) {
                displayText.append(" (").append(status).append(")");
            }

            response.append("<li>")
                    .append(displayText.toString())
                    .append("</li>");

            // Cache contract data with index as key
            String cacheKey = String.valueOf(i + 1);
            contractCache.put(cacheKey, Arrays.asList(contract));
            contractDisplayOrder.add(cacheKey);
        }

        response.append("</ol>");
        response.append("<p><i>Enter the number (1, 2, 3...) to select a contract.</i></p>");

        // Store contract cache for user selection
        cacheContractSearchResults(contractCache, contractDisplayOrder);

        return response.toString();
    }

    /**
     * Format user contracts for display
     */
    private String formatUserContracts(String userName, List<Map<String, String>> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return "<p><i>No contracts found for user '" + userName + "'</i></p>";
        }

        StringBuilder response = new StringBuilder();
        response.append("<p><b>Contracts created by '")
                .append(userName)
                .append("':</b></p>");
        Set<String> kesyMapSet = contracts.get(0).keySet();
        List<String> colmsNames = kesyMapSet.stream().collect(Collectors.toList());

        // Use existing formatting method
        return formatQueryResultInView((List) contracts, colmsNames, new ArrayList<>(), DEFAULT_SCREEN_WIDTH,
                                       "contracts_by_user");
    }

    /**
     * Convert contracts map to data rows
     */
    private List<Map<String, Object>> convertContractsToDataRows(Map<String, String> contracts) {
        List<Map<String, Object>> dataRows = new ArrayList<>();

        // Create a single row with all contract data
        Map<String, Object> row = new HashMap<>();
        row.putAll(contracts);
        dataRows.add(row);

        return dataRows;
    }

    /**
     * Extract username from filters
     */
    private String extractUsernameFromFilters(List<NLPEntityProcessor.EntityFilter> filters) {
        if (filters == null)
            return null;

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            if (isCreatedByFilter(filter)) {
                return filter.value;
            }
        }
        return null;
    }

    /**
     * Extract date filters from query
     */
    private Map<String, Object> extractDateFilters(List<NLPEntityProcessor.EntityFilter> filters) {
        Map<String, Object> dateFilters = new HashMap<>();

        if (filters == null)
            return dateFilters;

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            if (filter.attribute.equals("CREATE_DATE")) {
                switch (filter.operation.toUpperCase()) {
                case "IN_YEAR":
                    dateFilters.put("inYear", filter.value);
                    break;
                case "AFTER_YEAR":
                    dateFilters.put("afterYear", filter.value);
                    break;
                case "BEFORE_YEAR":
                    dateFilters.put("beforeYear", filter.value);
                    break;
                case "YEAR_RANGE":
                    dateFilters.put("yearRange", filter.value);
                    break;
                case "MONTH_RANGE":
                    dateFilters.put("monthRange", filter.value);
                    break;
                case "=":
                    // Specific date
                    dateFilters.put("specificDate", filter.value);
                    break;
                }
            }
        }

        return dateFilters;
    }

    /**
     * Query CONTRACTS table with specific award numbers
     */
    private String queryContractsWithAwardNumbers(String actionType, List<String> awardNumbers,
                                                  List<String> displayEntities, int screenWidth) {
        try {
            // Convert List<String> to comma-separated string for Model layer
            String displayColumns = null;
            if (displayEntities != null && !displayEntities.isEmpty()) {
                displayColumns = String.join(",", displayEntities);
            }

            // Build IN clause for award numbers
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < awardNumbers.size(); i++) {
                if (i > 0)
                    inClause.append(",");
                inClause.append("?");
            }

            // Call Model layer with modified parameters
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeNLPQuery");
            operationBind.getParamsMap().put("actionType", "contracts_by_award_numbers"); // New action type
            operationBind.getParamsMap().put("filterAttributes", "AWARD_NUMBER");
            operationBind.getParamsMap().put("filterValues", String.join(",", awardNumbers));
            operationBind.getParamsMap().put("filterOperations", "IN");
            operationBind.getParamsMap().put("displayColumns", displayColumns);

            Map<String, Object> queryResult = (Map<String, Object>) operationBind.execute();

            if (!(Boolean) queryResult.get("success")) {
                return "<p><b></b></p>";
            }

            // Get raw data from Model layer
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) queryResult.get("data");
            List<String> columnNames = (List<String>) queryResult.get("columnNames");

            // Format data in View layer
            return formatQueryResultInView(dataRows, columnNames, displayEntities, screenWidth, actionType);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    private boolean isCreatedByFilter(NLPEntityProcessor.EntityFilter filter) {
        if (filter == null)
            return false;

        String createdByColumn =
            TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACT_CONTACTS, "created_by");

        return (createdByColumn != null && filter.attribute.equals(createdByColumn)) ||
               "AWARD_REP".equals(filter.attribute) || "CREATED_BY".equals(filter.attribute);
    }

    private String[] createStringTypeArray(int size) {
        String[] types = new String[size];
        for (int i = 0; i < size; i++) {
            types[i] = "STRING";
        }
        return types;
    }

    /**
     * Check if query is date-related
     */
    private boolean isDateRelatedQuery(NLPEntityProcessor.QueryResult queryResult) {
        if (queryResult == null || queryResult.displayEntities == null)
            return false;

        String lowerInput = queryResult.inputTracking
                                       .originalInput
                                       .toLowerCase();
        return lowerInput.contains("expiration date") || lowerInput.contains("effective date") ||
               lowerInput.contains("price expiration") || lowerInput.contains("price exp") ||
               lowerInput.contains("date of signature") || lowerInput.contains("flow down date") ||
               (lowerInput.contains("price") && (lowerInput.contains("date") || lowerInput.contains("exp")));
    }

    /**
     * Handle date-related queries using Model layer method
     */
    private String handleDateRelatedQuery(NLPEntityProcessor.QueryResult queryResult, int screenWidth) {
        System.out.println("handleDateRelatedQuery====================>");
        try {
            // Extract contract number from filters
            String contractNumber = null;
            for (NLPEntityProcessor.EntityFilter filter : queryResult.entities) {
                if (filter.attribute.equals("AWARD_NUMBER") || filter.attribute.equals("CONTRACT_NUMBER")) {
                    contractNumber = filter.value;
                    break;
                }
            }

            if (contractNumber == null) {
                return "<p><b>Contract number not found in date query</b></p>";
            }

            // Call Model layer method for contract dates
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("pullContractDatesByAwardNumber");
            operationBind.getParamsMap().put("awardNumber", contractNumber);

            Map<String, Object> result = (Map<String, Object>) operationBind.execute();

            if (!(Boolean) result.get("success")) {
                return "<p><b>Error getting contract dates: " + result.get("error") + "</b></p>";
            }

            // Get dates from result
            Map<String, String> dates = (Map<String, String>) result.get("dates");

            // Format the response
            StringBuilder response = new StringBuilder();
            response.append("<h3>Contract Dates</h3><hr>");
            response.append("<h4>Contract ")
                    .append(contractNumber)
                    .append("</h4>");
            response.append("<p>");

            if (dates != null) {
                for (Map.Entry<String, String> entry : dates.entrySet()) {
                    response.append("<b>")
                            .append(getDisplayName(entry.getKey()))
                            .append(":</b> ");
                    response.append("<span>")
                            .append(entry.getValue())
                            .append("</span>");
                    response.append("<br>");
                }
            } else {
                response.append("<i>No date information found.</i>");
            }

            response.append("</p>");
            response.append(addProfessionalNote());

            return response.toString();

        } catch (Exception e) {
            return "<p><b>Error in date query: " + e.getMessage() + "</b></p>";
        }
    }


    /**
     * Check if query is asking for customer information
     */
    private boolean isCustomerQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for customer keywords (including misspellings)
        boolean hasCustomerKeywords =
            lowerInput.contains("customer") || lowerInput.contains("custmer") || // misspelling
            lowerInput.contains("custoemr") || // misspelling
            lowerInput.contains("client") || lowerInput.contains("account") ||
                          lowerInput.contains("acount"); // misspelling

        // Check for customer-specific attributes
        boolean hasCustomerAttributes =
            lowerInput.contains("sales rep") || lowerInput.contains("sales team") ||
            lowerInput.contains("sales manager") || lowerInput.contains("sales director") ||
            lowerInput.contains("sales vp") || lowerInput.contains("account type") ||
            lowerInput.contains("payment terms") || lowerInput.contains("currency") ||
            lowerInput.contains("order min") || lowerInput.contains("line min") || lowerInput.contains("warehouse") ||
            lowerInput.contains("active") || lowerInput.contains("inactive") || lowerInput.contains("created by") ||
            lowerInput.contains("created date") || lowerInput.contains("accountrep") ||
            lowerInput.contains("awardrep") || lowerInput.contains("is dfar") || lowerInput.contains("is asl") ||
            lowerInput.contains("is gt25") || lowerInput.contains("is ecomm") || lowerInput.contains("hppflag") ||
            lowerInput.contains("asl code");

        // Check for customer number patterns (4-8 digits)
        boolean hasCustomerNumber = lowerInput.matches(".*\\b\\d{4,8}\\b.*");

        // Check for question/action keywords
        boolean hasActionKeywords =
            lowerInput.contains("what") || lowerInput.contains("show") || lowerInput.contains("get") ||
            lowerInput.contains("list") || lowerInput.contains("?") || lowerInput.contains("details") ||
            lowerInput.contains("info") || lowerInput.contains("information");

        // Must have customer keywords AND (customer number OR customer attributes OR action keywords)
        return hasCustomerKeywords && (hasCustomerNumber || hasCustomerAttributes || hasActionKeywords);
    }

    /**
     * Check if filters contain a contract number for PARTS table specifically
     * For PARTS table: LOADED_CP_NUMBER is the contract number column
     * For CONTRACTS table: AWARD_NUMBER is the contract number column
     * For FAILED_PARTS table: CONTRACT_NO is the contract number column
     */
    private boolean hasContractNumberInFilters(List<NLPEntityProcessor.EntityFilter> filters) {
        return hasContractNumberInFilters(filters, "PARTS");
    }

    /**
     * Generate contract number required error message
     * ADF Component Compatible - Uses only supported HTML tags
     * @param tableType The table type for context-specific messaging
     * @return Formatted HTML error message
     */
    private String generateContractNumberRequiredMessage(String tableType) {
        String contractColumnName = "";
        String examples = "";

        // Set table-specific information
        switch (tableType.toUpperCase()) {
        case "PARTS":
            contractColumnName = "LOADED_CP_NUMBER";
            examples =
                "&bull; &quot;Show parts for contract 100476&quot;<br>" +
                "&bull; &quot;List parts with status active for contract 100476&quot;<br>" +
                "&bull; &quot;Get parts by part number EN6114V4-13 for contract 100476&quot;";
            break;
        case "FAILED_PARTS":
            contractColumnName = "CONTRACT_NO";
            examples =
                "&bull; &quot;Show failed parts for contract 100476&quot;<br>" +
                "&bull; &quot;List failed parts with error status for contract 100476&quot;<br>" +
                "&bull; &quot;Get failed parts by part number EN6114V4-13 for contract 100476&quot;";
            break;
        case "CONTRACTS":
            contractColumnName = "AWARD_NUMBER";
            examples =
                "&bull; &quot;Show contract 100476&quot;<br>" +
                "&bull; &quot;List contracts with status active&quot;<br>" +
                "&bull; &quot;Get contract details for 100476&quot;";
            break;
        default:
            contractColumnName = "LOADED_CP_NUMBER";
            examples =
                "&bull; &quot;Show parts for contract 100476&quot;<br>" +
                "&bull; &quot;List parts with status active for contract 100476&quot;<br>" +
                "&bull; &quot;Get parts by part number EN6114V4-13 for contract 100476&quot;";
        }

        return "<h4>Contract Number Required</h4>" + "<p>Contract number is mandatory to filter " +
               tableType.toLowerCase() + ". " + tableType + " are associated with contracts.</p>" +
               "<p><b>Examples:</b><br>" + examples + "</p>";
    }

    /**
     * Check if filters contain a contract number based on table type
     * Business Rules:
     * - Contract numbers are exactly 6 digits
     * - Column mapping: PARTS->LOADED_CP_NUMBER, CONTRACTS->AWARD_NUMBER, FAILED_PARTS->CONTRACT_NO
     * @param filters List of entity filters to check
     * @param tableType The table type ("PARTS", "CONTRACTS", "FAILED_PARTS")
     * @return true if contract number is found, false otherwise
     */
    private boolean hasContractNumberInFilters(List<NLPEntityProcessor.EntityFilter> filters, String tableType) {
        if (filters == null || filters.isEmpty()) {
            return false;
        }

        String contractNumberColumn = null;

        // BUSINESS RULE: Determine the correct contract number column based on table type
        switch (tableType.toUpperCase()) {
        case "PARTS":
            contractNumberColumn = "LOADED_CP_NUMBER";
            break;
        case "CONTRACTS":
            contractNumberColumn = "AWARD_NUMBER";
            break;
        case "FAILED_PARTS":
            contractNumberColumn = "CONTRACT_NO";
            break;
        default:
            // Default to PARTS table column
            contractNumberColumn = "LOADED_CP_NUMBER";
        }

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            // Check if the filter attribute matches the contract number column for the specific table
            if (filter.attribute.equals(contractNumberColumn)) {
                return true;
            }

            // BUSINESS RULE: Check if the value looks like a contract number (exactly 6 digits)
            // This helps catch cases where the column name might be generic but value is clearly a contract number
            if (filter.value != null && filter.value.matches("\\d{6}")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Enhanced contract creation query detection with intent classification
     */
    private boolean isContractCreationQueryPrivate(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for creation keywords
        boolean hasCreationKeywords =
            lowerInput.contains("create") || lowerInput.contains("new") || lowerInput.contains("add") ||
            lowerInput.contains("make") || lowerInput.contains("build") || lowerInput.contains("generate") ||
            lowerInput.contains("setup") || lowerInput.contains("establish");

        // Check for contract keywords
        boolean hasContractKeywords = lowerInput.contains("contract") || lowerInput.contains("contarct") || // misspelling
            lowerInput.contains("agreement") || lowerInput.contains("deal");

        // Check for account number pattern (7+ digits per business rules)
        boolean hasAccountNumber = lowerInput.matches(".*\\b\\d{7,}\\b.*");

        // Check for account keywords
        boolean hasAccountKeywords = lowerInput.contains("account") || lowerInput.contains("acount") || // misspelling
            lowerInput.contains("customer");

        // Must have creation keywords AND (contract keywords OR account keywords OR account number)
        return hasCreationKeywords && (hasContractKeywords || hasAccountKeywords || hasAccountNumber);
    }

    /**
     * Public wrapper for isContractCreationQuery
     */
    public boolean isContractCreationQuery(String userInput) {
        return isContractCreationQueryPrivate(userInput);
    }

    /**
     * Classify contract creation intent (manual steps vs automated creation)
     * @param userInput User input to analyze
     * @return "HELP_CONTRACT_CREATE_USER" for manual steps, "HELP_CONTRACT_CREATE_BOT" for automated creation
     */
    private String classifyContractCreationIntentPrivate(String userInput) {
        if (userInput == null)
            return "HELP_CONTRACT_CREATE_USER";
        String lowerInput = userInput.toLowerCase();

        // Keywords indicating request for manual steps/instructions
        boolean requestsManualSteps =
            lowerInput.contains("steps") || lowerInput.contains("how to") || lowerInput.contains("help") ||
            lowerInput.contains("guide") || lowerInput.contains("instructions") || lowerInput.contains("process") ||
            lowerInput.contains("procedure") || lowerInput.contains("manual") || lowerInput.contains("show me") ||
            lowerInput.contains("explain") || lowerInput.contains("understand") || lowerInput.contains("learn");

        // Keywords indicating request for bot to create contract
        boolean requestsBotCreation =
            lowerInput.contains("create") || lowerInput.contains("make") || lowerInput.contains("build") ||
            lowerInput.contains("generate") || lowerInput.contains("why can't you") ||
            lowerInput.contains("can you create") || lowerInput.contains("please create") ||
            lowerInput.contains("want you to create");

        // Check for account number presence (7+ digits)
        boolean hasAccountNumber = lowerInput.matches(".*\\b\\d{7,}\\b.*");

        // If user provides account number, they likely want bot to create
        if (hasAccountNumber && requestsBotCreation) {
            return "HELP_CONTRACT_CREATE_BOT";
        }

        // If user asks for steps/help, they want manual instructions
        if (requestsManualSteps) {
            return "HELP_CONTRACT_CREATE_USER";
        }

        // Default to bot creation if they use creation keywords
        if (requestsBotCreation) {
            return "HELP_CONTRACT_CREATE_BOT";
        }

        // Default to manual steps
        return "HELP_CONTRACT_CREATE_USER";
    }

    /**
     * Public wrapper for classifyContractCreationIntent
     */
    public String classifyContractCreationIntent(String userInput) {
        return classifyContractCreationIntentPrivate(userInput);
    }

    /**
     * Check if contract creation query has account number
     */
    private boolean hasAccountNumberInCreationQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for 7+ digit account numbers (per business rules)
        boolean hasAccountNumber = lowerInput.matches(".*\\b\\d{7,}\\b.*");

        // Check for account-related words
        boolean hasAccountWords = lowerInput.contains("account") || lowerInput.contains("acount") || // misspelling
            lowerInput.contains("customer") || lowerInput.contains("custno") || lowerInput.contains("customer no");

        return hasAccountNumber || hasAccountWords;
    }

    /**
     * Extract account number from user input
     */
    private String extractAccountNumberFromInputPrivate(String userInput) {
        if (userInput == null)
            return null;

        // Pattern for account numbers (7+ digits per business rules)
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b(\\d{7,})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Public wrapper for extractAccountNumberFromInput
     */
    public String extractAccountNumberFromInput(String userInput) {
        return extractAccountNumberFromInputPrivate(userInput);
    }

    /**
     * Handle customer queries
     */
    private String handleCustomerQuery(List<NLPEntityProcessor.EntityFilter> filters, List<String> displayEntities,
                                       String userInput) {
        try {
            // Extract customer number from user input
            String customerNumber = extractCustomerNumberFromInput(userInput, filters);

            // Determine specific columns based on user query
            List<String> specificColumns = determineSpecificCustomerColumns(userInput);

            // Create filters for customer number if available
            List<NLPEntityProcessor.EntityFilter> customerFilters = new ArrayList<>();
            if (customerNumber != null && !customerNumber.isEmpty()) {
                customerFilters.add(new NLPEntityProcessor.EntityFilter("CUSTOMER_NO", "=", customerNumber,
                                                                        "extracted"));
            }

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "customers_by_filter");
            inputParams.put("filters", customerFilters);
            inputParams.put("displayEntities", specificColumns.isEmpty() ? displayEntities : specificColumns);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Execute database query and get formatted result
            return pullData(inputParams);

        } catch (Exception e) {
            return "<p><b></b></p>";
        }
    }

    /**
     * Extract customer number from user input or filters
     */
    private String extractCustomerNumberFromInput(String userInput, List<NLPEntityProcessor.EntityFilter> filters) {
        // First try to extract from user input using regex
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b(\\d{4,8})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // If not found in input, try filters
        if (filters != null) {
            for (NLPEntityProcessor.EntityFilter filter : filters) {
                if (filter.attribute.equals("CUSTOMER_NO") || filter.attribute.equals("CUST_ID") ||
                    filter.attribute.matches("\\d{4,8}")) {
                    return filter.value;
                }
            }
        }
        return null;
    }

    /**
     * Determine specific customer columns based on user query
     */
    private List<String> determineSpecificCustomerColumns(String userInput) {
        if (userInput == null)
            return new ArrayList<>();

        String lowerInput = userInput.toLowerCase();
        List<String> columns = new ArrayList<>();

        // Use TableColumnConfig to map user input to actual column names
        String[] words = lowerInput.split("\\s+");

        for (String word : words) {
            // Check business term mappings first
            String columnName = TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CUSTOMERS, word);
            if (columnName != null && !columns.contains(columnName)) {
                columns.add(columnName);
                continue;
            }

            // Check field synonyms
            columnName = TABLE_CONFIG.getColumnForSynonym(TableColumnConfig.TABLE_CUSTOMERS, word);
            if (columnName != null && !columns.contains(columnName)) {
                columns.add(columnName);
                continue;
            }

            // Check multi-word phrases
            for (int i = 0; i < words.length - 1; i++) {
                String phrase = words[i] + " " + words[i + 1];
                columnName = TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CUSTOMERS, phrase);
                if (columnName != null && !columns.contains(columnName)) {
                    columns.add(columnName);
                }
            }
        }

        // Handle specific patterns that might not be caught by single word mapping
        if (lowerInput.contains("sales rep") || lowerInput.contains("sales team") ||
            lowerInput.contains("sales manager") || lowerInput.contains("sales director") ||
            lowerInput.contains("sales vp") || lowerInput.contains("sales owner")) {
            addColumnIfNotExists(columns, "SALES_REP_ID");
            addColumnIfNotExists(columns, "SALES_TEAM");
            addColumnIfNotExists(columns, "SALES_MANAGER");
            addColumnIfNotExists(columns, "SALES_DIRECTOR");
            addColumnIfNotExists(columns, "SALES_VP");
            addColumnIfNotExists(columns, "SALES_OWNER");
        }

        // Handle order minimum variations
        if (lowerInput.contains("order min") || lowerInput.contains("minimum order") ||
            lowerInput.contains("min order") || lowerInput.contains("minorder")) {
            addColumnIfNotExists(columns, "ORDER_MIN");
        }

        // Handle line minimum variations
        if (lowerInput.contains("line min") || lowerInput.contains("minimum line") || lowerInput.contains("min line") ||
            lowerInput.contains("minline")) {
            addColumnIfNotExists(columns, "LINE_MIN");
        }

        // Handle status variations
        if (lowerInput.contains("active") || lowerInput.contains("inactive") || lowerInput.contains("status")) {
            addColumnIfNotExists(columns, "IS_ACTIVE");
        }

        // Handle creation variations
        if (lowerInput.contains("created by") || lowerInput.contains("created date") ||
            lowerInput.contains("creation date")) {
            addColumnIfNotExists(columns, "CREATED_BY");
            addColumnIfNotExists(columns, "CREATED_DATE");
        }

        // Handle account rep variations
        if (lowerInput.contains("accountrep") || lowerInput.contains("account rep") ||
            lowerInput.contains("account_rep")) {
            addColumnIfNotExists(columns, "ACCOUNTREP");
        }

        // Handle award rep variations
        if (lowerInput.contains("awardrep") || lowerInput.contains("award rep") || lowerInput.contains("award_rep")) {
            addColumnIfNotExists(columns, "AWARDREP");
        }

        // Handle DFAR variations
        if (lowerInput.contains("is dfar") || lowerInput.contains("dfar") || lowerInput.contains("defense")) {
            addColumnIfNotExists(columns, "IS_DFAR");
        }

        // Handle ASL variations
        if (lowerInput.contains("is asl") || lowerInput.contains("asl") || lowerInput.contains("approved supplier")) {
            addColumnIfNotExists(columns, "IS_ASL_APPLICABLE");
            addColumnIfNotExists(columns, "ASL_CODE");
        }

        // Handle GT25 variations
        if (lowerInput.contains("is gt25") || lowerInput.contains("gt25") || lowerInput.contains("greater than 25")) {
            addColumnIfNotExists(columns, "IS_GT25");
        }

        // Handle ECOMM variations
        if (lowerInput.contains("is ecomm") || lowerInput.contains("ecomm") || lowerInput.contains("e commerce")) {
            addColumnIfNotExists(columns, "IS_ECOMM");
        }

        // Handle HPPFLAG variations
        if (lowerInput.contains("hppflag") || lowerInput.contains("hpp") || lowerInput.contains("hpp_flag")) {
            addColumnIfNotExists(columns, "HPPFLAG");
        }

        return columns;
    }

    /**
     * Helper method to add column if not already in the list
     */
    private void addColumnIfNotExists(List<String> columns, String columnName) {
        if (!columns.contains(columnName)) {
            columns.add(columnName);
        }
    }

    /**
     * Handle contract creation flow with enhanced intent classification
     */
    private String handleContractCreationQuery(List<NLPEntityProcessor.EntityFilter> filters,
                                               List<String> displayEntities, String userInput) {
        try {
            // Classify the intent
            String intent = classifyContractCreationIntentPrivate(userInput);

            if ("HELP_CONTRACT_CREATE_USER".equals(intent)) {
                return handleHelpContractCreateUserWithDataProvider(filters, displayEntities, userInput);
            } else if ("HELP_CONTRACT_CREATE_BOT".equals(intent)) {
                return handleAutomatedContractCreation(filters, displayEntities, userInput);
            } else {
                // Default to manual steps
                return handleHelpContractCreateUserWithDataProvider(filters, displayEntities, userInput);
            }

        } catch (Exception e) {
            return "<p><b>Error in contract creation:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Enhanced automated contract creation with conversational flow
     */
    private String handleAutomatedContractCreation(List<NLPEntityProcessor.EntityFilter> filters,
                                                   List<String> displayEntities, String userInput) {
        try {
            // Extract account number from input
            String accountNumber = extractAccountNumberFromInputPrivate(userInput);

            if (accountNumber == null) {
                // No account number provided - show prompt for all details
                return getContractCreationPrompt("HELP_CONTRACT_CREATE_BOT", null);
            }

            // Validate account number
            if (!isCustomerNumberValid(accountNumber)) {
                return "<h4>�?� Invalid Account Number</h4>" + 
                       "<p>The account number <b>" + accountNumber + "</b> is not a valid customer.</p>" +
                       "<p>Please provide a valid customer account number (6+ digits).</p>";
            }

            // Check if user provided all details in single input
            if (isCompleteContractCreationInput(userInput)) {
                return processCompleteContractCreationInput(userInput, accountNumber);
            }

            // Initialize contract creation flow with account number
            return initiateAutomatedContractCreation(accountNumber);

        } catch (Exception e) {
            return "<p><b>Error in automated contract creation:</b> " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Check if input contains complete contract creation data
     */
    private boolean isCompleteContractCreationInput(String userInput) {
        // Check for comma-separated format first
        if (userInput.contains(",")) {
            String[] parts = userInput.split(",");
            if (parts.length >= 4) {
                // Check if first part is account number (6+ digits)
                String firstPart = parts[0].trim();
                boolean hasAccountNumber = firstPart.matches("\\d{6,}");
                
                // Check if we have contract name, title, description
                boolean hasContractName = parts.length >= 2 && !parts[1].trim().matches("\\d+");
                boolean hasTitle = parts.length >= 3 && !parts[2].trim().matches("\\d+");
                boolean hasDescription = parts.length >= 4 && !parts[3].trim().matches("\\d+");
                
                return hasAccountNumber && hasContractName && hasTitle && hasDescription;
            }
        }
        
        // Check space-separated format (backward compatibility)
        String[] words = userInput.split("\\s+");
        
        // Check if we have enough words for all required fields
        if (words.length >= 4) {
            // Check if first word is account number (6+ digits)
            boolean hasAccountNumber = words[0].matches("\\d{6,}");
            
            // Check if we have contract name, title, description
            boolean hasContractName = words.length >= 2 && !words[1].matches("\\d+");
            boolean hasTitle = words.length >= 3 && !words[2].matches("\\d+");
            boolean hasDescription = words.length >= 4 && !words[3].matches("\\d+");
            
            return hasAccountNumber && hasContractName && hasTitle && hasDescription;
        }
        
        return false;
    }
    
    /**
     * Process complete contract creation input
     */
    private String processCompleteContractCreationInput(String userInput, String accountNumber) {
        try {
            // Check if input uses comma separation
            if (userInput.contains(",")) {
                return processCommaSeparatedContractInput(userInput, accountNumber);
            } else {
                return processSpaceSeparatedContractInput(userInput, accountNumber);
            }
        } catch (Exception e) {
            return "<p><b>Error processing contract creation data:</b> " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Process comma-separated contract input
     */
    private String processCommaSeparatedContractInput(String userInput, String accountNumber) {
        String[] parts = userInput.split(",");
        
        // Extract all details
        String contractName = parts.length >= 2 ? parts[1].trim() : "";
        String title = parts.length >= 3 ? parts[2].trim() : "";
        String description = parts.length >= 4 ? parts[3].trim() : "";
        String comments = parts.length >= 5 ? parts[4].trim() : "";
        
        // Handle comments
        if (comments.equalsIgnoreCase("nocomments") || comments.equalsIgnoreCase("no")) {
            comments = "";
        }
        
        // Check for pricelist
        String isPricelist = "NO";
        if (parts.length >= 6) {
            String pricelist = parts[5].trim();
            if (pricelist.equalsIgnoreCase("yes")) {
                isPricelist = "YES";
            }
        }
        
        // Create contract parameters
        Map<String, Object> contractParams = new HashMap<>();
        contractParams.put("accountNumber", accountNumber);
        contractParams.put("contractName", contractName);
        contractParams.put("title", title);
        contractParams.put("description", description);
        contractParams.put("comments", comments);
        contractParams.put("isPricelist", isPricelist);
        
        // Call handleAutomatedContractCreation with complete data
        return handleAutomatedContractCreationWithData(contractParams);
    }
    
    /**
     * Process space-separated contract input (backward compatibility)
     */
    private String processSpaceSeparatedContractInput(String userInput, String accountNumber) {
        String[] words = userInput.split("\\s+");
        
        // Extract all details
        String contractName = words.length >= 2 ? words[1] : "";
        String title = words.length >= 3 ? words[2] : "";
        String description = words.length >= 4 ? words[3] : "";
        String comments = words.length >= 5 ? words[4] : "";
        
        // Handle comments
        if (comments.equalsIgnoreCase("nocomments") || comments.equalsIgnoreCase("no")) {
            comments = "";
        }
        
        // Check for pricelist
        String isPricelist = "NO";
        String lowerInput = userInput.toLowerCase();
        if (lowerInput.contains("yes")) {
            isPricelist = "YES";
        }
        
        // Create contract parameters
        Map<String, Object> contractParams = new HashMap<>();
        contractParams.put("accountNumber", accountNumber);
        contractParams.put("contractName", contractName);
        contractParams.put("title", title);
        contractParams.put("description", description);
        contractParams.put("comments", comments);
        contractParams.put("isPricelist", isPricelist);
        
        // Call handleAutomatedContractCreation with complete data
        return handleAutomatedContractCreationWithData(contractParams);
    }
    
    /**
     * Handle automated contract creation with complete data
     */
    private String handleAutomatedContractCreationWithData(Map<String, Object> contractParams) {
        try {
            // Call the existing createContractByBOT method
            Map<String, String> stringParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : contractParams.entrySet()) {
                stringParams.put(entry.getKey(), entry.getValue().toString());
            }
            
            String result = createContractByBOT(stringParams);
            
            // Parse the result to extract contract number
            String contractNumber = extractContractNumberFromSaveResult(result);
            
            if (contractNumber != null && !contractNumber.isEmpty()) {
                return handleSuccessfulContractCreation(contractNumber);
            } else {
                return "<h4>⚠�? Contract Creation Issue</h4>" +
                       "<p>The contract creation process encountered an issue. Please try again.</p>" +
                       "<p><b>Details:</b> " + result + "</p>";
            }
            
        } catch (Exception e) {
            return "<p><b>Error in contract creation:</b> " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Get contract creation prompt
     */
    private String getContractCreationPrompt(String actionType, String accountNumber) {
        // Create HelpProcessor instance to get prompts
        HelpProcessor helpProcessor = new HelpProcessor();
        return helpProcessor.getContractCreationPrompt(actionType, accountNumber);
    }

    /**
     * Initiate automated contract creation flow
     */
    private String initiateAutomatedContractCreation(String accountNumber) {
        try {
            // Create flow state for contract creation
            Map<String, Object> flowState = new HashMap<>();
            flowState.put("flowType", "CONTRACT_CREATION");
            flowState.put("accountNumber", accountNumber);
            flowState.put("step", "COLLECT_CONTRACT_NAME");
            flowState.put("startTime", System.currentTimeMillis());

            // Store flow state (this should integrate with ConversationalFlowManager)
            // flowManager.startFlow(flowState);

            return "<h4>Contract Creation - Account Validated</h4>" + "<p>Account number <b>" + accountNumber +
                   "</b> is valid. Let's create your contract!</p>" +
                   "<p><b>Step 1:</b> Please provide the contract name.</p>" +
                   "<p><i>Example: &quot;Contract name: ABC Project Services&quot;</i></p>";

        } catch (Exception e) {
            return "<p><b>Error initiating contract creation:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Handle contract creation when account number is provided
     */
    private String handleContractCreationWithAccount(String userInput) {
        try {
            // Extract account number
            String accountNumber = extractAccountNumberFromInputPrivate(userInput);

            if (accountNumber == null || accountNumber.isEmpty()) {
                return "<p><b></b></p>";
            }

            // Validate customer account number
            boolean isValidCustomer = validateCustomer(accountNumber);

            if (!isValidCustomer) {
                return "<h4>Invalid Account Number</h4>" + "<p>Please provide a valid account number.</p>" +
                       "<p><b>Account Number:</b> " + accountNumber + "<br>" +
                       "<b>Status:</b> Not found in customer database</p>";
            }

            // If valid customer, proceed with contract creation
            return initiateContractCreation(accountNumber);

        } catch (Exception e) {
            return "<p><b>Error validating customer:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Initiate contract creation process
     */
    public String initiateContractCreation(String accountNumber) {
        return "<h4>Contract Creation Initiated</h4>" + "<p>Account number <b>" + accountNumber +
               "</b> is valid. Let's create your contract!</p>" +
               "<p><b>Step 1:</b> Please provide the contract name.</p>" +
               "<p><i>Example: &quot;Contract name: ABC Project Services&quot;</i></p>";
    }

    /**
     * Handle contract creation when no account number is provided
     * ADF Component Compatible - Uses only supported HTML tags
     */
    private String handleContractCreationWithoutAccount(String userInput) {
        return "<h4>Contract Creation</h4>" + "<p>Please provide the following details to create a contract:</p>" +
               "<p><b>Required Information:</b><br>" + "&bull; <b>Account Number:</b> (4-8 digit customer number)<br>" +
               "&bull; <b>Contract Name:</b><br>" + "&bull; <b>Title:</b><br>" +
               "&bull; <b>Price List:</b> (yes/no) - default: no<br>" + "&bull; <b>Description:</b><br>" +
               "&bull; <b>Comments:</b><br>" + "&bull; <b>Effective Date:</b><br>" +
               "&bull; <b>Expiration Date:</b><br>" + "&bull; <b>Price Expiration Date:</b><br>" +
               "&bull; <b>System Loaded Data:</b> (yes/no) - default: yes</p>" +
               "<p><i>Example: &quot;Create contract for account 12345 with name 'ABC Project Contract'&quot;</i></p>";
    }

    /**
     * Validate customer account number
     */
    public boolean validateCustomer(String accountNumber) {
        
        try {
            // Call BCCTChatBotAppModuleImpl to validate customer
            // This should be implemented in the Model layer
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "validate_customer");
            inputParams.put("customerNumber", accountNumber);

            // For now, return true (implement actual validation)
            return false;

        } catch (Exception e) {
            return false;
        }
        //return false;
    }

    /**
     * Process contract creation flow step
     */
    public String processContractCreationStep(String userInput, Map<String, Object> flowState) {
        try {
            String step = (String) flowState.get("step");
            String accountNumber = (String) flowState.get("accountNumber");

            // Check for timeout (300 seconds = 5 minutes)
            long startTime = (Long) flowState.get("startTime");
            if (System.currentTimeMillis() - startTime > 300000) {
                return "<h4>Session Timeout</h4>" +
                       "<p>Your contract creation session has timed out. Would you like to start over?</p>";
            }

            // Check for interruption (user changed topic)
            if (isInterruption(userInput)) {
                return "<h4>Contract Creation Cancelled</h4>" +
                       "<p>I've cancelled the contract creation. How else can I help you?</p>";
            }

            switch (step) {
            case "COLLECT_CONTRACT_NAME":
                return processContractNameStep(userInput, flowState);
            case "COLLECT_TITLE":
                return processTitleStep(userInput, flowState);
            case "COLLECT_DESCRIPTION":
                return processDescriptionStep(userInput, flowState);
            case "COLLECT_OPTIONAL_FIELDS":
                return processOptionalFieldsStep(userInput, flowState);
            case "CONFIRM_CREATION":
                return processConfirmationStep(userInput, flowState);
            default:
                return "<p><b>Error:</b> Unknown step in contract creation flow.</p>";
            }

        } catch (Exception e) {
            return "<p><b>Error in contract creation step:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Check if user input indicates interruption
     */
    private boolean isInterruption(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Keywords indicating user wants to change topic
        return lowerInput.contains("cancel") || lowerInput.contains("stop") || lowerInput.contains("never mind") ||
               lowerInput.contains("forget it") || lowerInput.contains("show contract") ||
               lowerInput.contains("show parts") || lowerInput.contains("customer") || lowerInput.contains("help") ||
               lowerInput.contains("what can you do");
    }

    /**
     * Process contract name collection step
     */
    private String processContractNameStep(String userInput, Map<String, Object> flowState) {
        // Extract contract name from user input
        String contractName = extractContractNameFromInput(userInput);

        if (contractName == null || contractName.trim().isEmpty()) {
            return "<h4>Contract Name Required</h4>" + "<p>Please provide a contract name.</p>" +
                   "<p><i>Example: &quot;Contract name: ABC Project Services&quot;</i></p>";
        }

        // Store contract name and move to next step
        flowState.put("contractName", contractName);
        flowState.put("step", "COLLECT_TITLE");

        return "<h4>Contract Name: " + contractName + "</h4>" +
               "<p><b>Step 2:</b> Please provide the contract title.</p>" +
               "<p><i>Example: &quot;Title: Software Development Services&quot;</i></p>";
    }

    /**
     * Process title collection step
     */
    private String processTitleStep(String userInput, Map<String, Object> flowState) {
        String title = extractTitleFromInput(userInput);

        if (title == null || title.trim().isEmpty()) {
            return "<h4>Contract Title Required</h4>" + "<p>Please provide a contract title.</p>" +
                   "<p><i>Example: &quot;Title: Software Development Services&quot;</i></p>";
        }

        flowState.put("title", title);
        flowState.put("step", "COLLECT_DESCRIPTION");

        return "<h4>Contract Title: " + title + "</h4>" +
               "<p><b>Step 3:</b> Please provide a description for the contract.</p>" +
               "<p><i>Example: &quot;Description: This contract covers software development services for the ABC project&quot;</i></p>";
    }

    /**
     * Process description collection step
     */
    private String processDescriptionStep(String userInput, Map<String, Object> flowState) {
        String description = extractDescriptionFromInput(userInput);

        if (description == null || description.trim().isEmpty()) {
            return "<h4>Contract Description Required</h4>" + "<p>Please provide a description for the contract.</p>" +
                   "<p><i>Example: &quot;Description: This contract covers software development services&quot;</i></p>";
        }

        flowState.put("description", description);
        flowState.put("step", "COLLECT_OPTIONAL_FIELDS");

        return "<h4>Contract Description: " + description + "</h4>" +
               "<p><b>Step 4:</b> Optional fields (you can skip any by saying &quot;skip&quot;):</p>" +
               "<p><b>Price List:</b> Do you want a price list? (Yes/No, default: No)</p>" +
               "<p><b>isEM:</b> Is this an EM contract? (Yes/No, default: No)</p>" +
               "<p><b>Effective Date:</b> When should the contract start? (YYYY-MM-DD)</p>" +
               "<p><b>Expiration Date:</b> When should the contract expire? (YYYY-MM-DD)</p>";
    }

    /**
     * Process optional fields collection step
     */
    private String processOptionalFieldsStep(String userInput, Map<String, Object> flowState) {
        // Extract optional fields
        String priceList = extractPriceListFromInput(userInput);
        String isEM = extractIsEMFromInput(userInput);
        String effectiveDate = extractEffectiveDateFromInput(userInput);
        String expirationDate = extractExpirationDateFromInput(userInput);

        // Store optional fields (use defaults if not provided)
        flowState.put("priceList", priceList != null ? priceList : "NO");
        flowState.put("isEM", isEM != null ? isEM : "NO");
        flowState.put("effectiveDate", effectiveDate);
        flowState.put("expirationDate", expirationDate);
        flowState.put("step", "CONFIRM_CREATION");

        // Build confirmation message
        StringBuilder confirmation = new StringBuilder();
        confirmation.append("<h4>Contract Creation Summary</h4>");
        confirmation.append("<p><b>Account Number:</b> ")
                    .append(flowState.get("accountNumber"))
                    .append("</p>");
        confirmation.append("<p><b>Contract Name:</b> ")
                    .append(flowState.get("contractName"))
                    .append("</p>");
        confirmation.append("<p><b>Title:</b> ")
                    .append(flowState.get("title"))
                    .append("</p>");
        confirmation.append("<p><b>Description:</b> ")
                    .append(flowState.get("description"))
                    .append("</p>");
        confirmation.append("<p><b>Price List:</b> ")
                    .append(flowState.get("priceList"))
                    .append("</p>");
        confirmation.append("<p><b>isEM:</b> ")
                    .append(flowState.get("isEM"))
                    .append("</p>");
        if (effectiveDate != null)
            confirmation.append("<p><b>Effective Date:</b> ")
                        .append(effectiveDate)
                        .append("</p>");
        if (expirationDate != null)
            confirmation.append("<p><b>Expiration Date:</b> ")
                        .append(expirationDate)
                        .append("</p>");

        confirmation.append("<p><b>Step 5:</b> Please confirm to create the contract.</p>");
        confirmation.append("<p><i>Say &quot;yes&quot; to create the contract, or &quot;no&quot; to cancel.</i></p>");

        return confirmation.toString();
    }

    /**
     * Process confirmation step
     */
    private String processConfirmationStep(String userInput, Map<String, Object> flowState) {
        String lowerInput = userInput.toLowerCase();

        if (lowerInput.contains("yes") || lowerInput.contains("confirm") || lowerInput.contains("create")) {
            // Create the contract
            Map<String, String> contractParams = new HashMap<>();
            contractParams.put("accountNumber", (String) flowState.get("accountNumber"));
            contractParams.put("contractName", (String) flowState.get("contractName"));
            contractParams.put("title", (String) flowState.get("title"));
            contractParams.put("description", (String) flowState.get("description"));
            contractParams.put("priceList", (String) flowState.get("priceList"));
            contractParams.put("isEM", (String) flowState.get("isEM"));
            contractParams.put("effectiveDate", (String) flowState.get("effectiveDate"));
            contractParams.put("expirationDate", (String) flowState.get("expirationDate"));

            String contractNumber = createContractByBOT(contractParams);

            if ("NA".equals(contractNumber)) {
                return "<h4>Contract Creation Failed</h4>" +
                       "<p>Sorry, we are unable to create a contract at this time.</p>" +
                       "<p>Please try again later or contact your administrator.</p>";
            } else {
                // Success - get contract details
                return handleSuccessfulContractCreation(contractNumber);
            }
        } else {
            return "<h4>Contract Creation Cancelled</h4>" +
                   "<p>Contract creation has been cancelled. How else can I help you?</p>";
        }
    }

    // Helper methods for extracting contract data from user input
    private String extractContractNameFromInput(String userInput) {
        if (userInput == null)
            return null;

        // Look for patterns like "Contract name: ABC" or "name: ABC"
        java.util.regex.Pattern pattern =
            java.util
                                              .regex
                                              .Pattern
                                              .compile("(?:contract\\s+)?name\\s*:\\s*([^\\n]+)",
                                                       java.util
                                                                                                      .regex
                                                                                                      .Pattern
                                                                                                      .CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // If no pattern found, return the input as contract name
        return userInput.trim();
    }

    private String extractTitleFromInput(String userInput) {
        if (userInput == null)
            return null;

        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("(?:title\\s*:\\s*)([^\\n]+)", java.util
                                                                                          .regex
                                                                                          .Pattern
                                                                                          .CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return userInput.trim();
    }

    private String extractDescriptionFromInput(String userInput) {
        if (userInput == null)
            return null;

        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("(?:description\\s*:\\s*)([^\\n]+)", java.util
                                                                                                .regex
                                                                                                .Pattern
                                                                                                .CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return userInput.trim();
    }

    private String extractPriceListFromInput(String userInput) {
        if (userInput == null)
            return null;

        String lowerInput = userInput.toLowerCase();
        if (lowerInput.contains("price list") || lowerInput.contains("pricelist")) {
            if (lowerInput.contains("yes") || lowerInput.contains("true")) {
                return "YES";
            } else if (lowerInput.contains("no") || lowerInput.contains("false")) {
                return "NO";
            }
        }

        return null; // Use default
    }

    private String extractIsEMFromInput(String userInput) {
        if (userInput == null)
            return null;

        String lowerInput = userInput.toLowerCase();
        if (lowerInput.contains("em") || lowerInput.contains("emergency")) {
            if (lowerInput.contains("yes") || lowerInput.contains("true")) {
                return "YES";
            } else if (lowerInput.contains("no") || lowerInput.contains("false")) {
                return "NO";
            }
        }

        return null; // Use default
    }

    private String extractEffectiveDateFromInput(String userInput) {
        if (userInput == null)
            return null;

        // Look for date patterns
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractExpirationDateFromInput(String userInput) {
        if (userInput == null)
            return null;

        // Look for date patterns
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Get contract creation form fields from configuration
     */
    private Map<String, Object> getContractCreationFormFields() {
        Map<String, Object> fields = new HashMap<>();

        // These fields can be loaded from configuration or database
        fields.put("accountNumber", "Required - 4-8 digit customer number");
        fields.put("contractName", "Required - Contract name/title");
        fields.put("title", "Required - Contract title");
        fields.put("priceList", "Optional - yes/no (default: no)");
        fields.put("description", "Optional - Contract description");
        fields.put("comments", "Optional - Additional comments");
        fields.put("effectiveDate", "Required - Contract effective date");
        fields.put("expirationDate", "Required - Contract expiration date");
        fields.put("priceExpirationDate", "Optional - Price expiration date");
        fields.put("systemLoadedData", "Optional - yes/no (default: yes)");

        return fields;
    }

    /**
     * Save contract with provided parameters
     */
    private String saveContract(Map<String, Object> contractParams) {
        try {
            // Call BCCTChatBotAppModuleImpl to save contract
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "save_contract");
            inputParams.putAll(contractParams);

            // Execute save operation
            String result = pullData(inputParams);

            // Extract contract number from result
            String contractNumber = extractContractNumberFromSaveResult(result);

            if (contractNumber != null && !contractNumber.isEmpty()) {
                return handleSuccessfulContractCreation(contractNumber);
            } else {
                return "<p><b>Error:</b> Contract number not generated</p>";
            }

        } catch (Exception e) {
            return "<p><b>Error saving contract:</b> " + e.getMessage() + "</p>";
        }
    }

    /**
     * Extract contract number from save result
     */
    private String extractContractNumberFromSaveResult(String result) {
        try {
            // Parse result to extract contract number
            // This should be implemented based on the actual response format
            java.util.regex.Pattern pattern = java.util
                                                  .regex
                                                  .Pattern
                                                  .compile("\\b(\\d{6})\\b");
            java.util.regex.Matcher matcher = pattern.matcher(result);

            if (matcher.find()) {
                return matcher.group(1);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Handle successful contract creation
     */
    private String handleSuccessfulContractCreation(String contractNumber) {
        try {
            // Create filters for the new contract
            List<NLPEntityProcessor.EntityFilter> filters = new ArrayList<>();
            filters.add(new NLPEntityProcessor.EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));

            // Prepare input parameters to get contract details
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "contracts_by_contractnumber");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities",
                            Arrays.asList("AWARD_NUMBER", "CONTRACT_NAME", "TITLE", "EFFECTIVE_DATE",
                                          "EXPIRATION_DATE"));
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);

            // Get contract details
            String contractDetails = pullData(inputParams);

            return "<h4>Contract Created Successfully</h4>" + "<p>Contract has been created with number: <b>" +
                   contractNumber + "</b></p>" + "<p>" + contractDetails + "</p>";

        } catch (Exception e) {
            return "<h4>Contract Created Successfully</h4>" + "<p>Contract has been created with number: <b>" +
                   contractNumber + "</b></p>";
        }
    }

    /**
     * Process complete query directly without follow-up questions
     * ENFORCED: All part queries MUST have contract number for performance
     */
    private String processCompleteQueryDirectly(ChatMessage chatMessage) {
        try {
            String userInput = chatMessage.getMessage();
            String queryType = chatMessage.getQueryType();
            String actionType = chatMessage.getActionType();

            // FIX: For merged lead time queries, ensure we have the correct context
            if (queryType != null && "PARTS_LEAD_TIME".equals(queryType)) {
                String partNumber = (String) chatMessage.getContextValue("partNumber");
                String contractNumber = (String) chatMessage.getContextValue("contractNumber");

                // If we have both part and contract numbers, process as lead time query
                if (partNumber != null && !partNumber.isEmpty() && contractNumber != null &&
                    !contractNumber.isEmpty()) {
                    System.out.println("DEBUG: Processing merged lead time query with part: " + partNumber +
                                       ", contract: " + contractNumber);
                    return handleLeadTimeQuery(chatMessage);
                }
            }

            // ENFORCED: Check if this is a part query without contract number
            if (queryType != null && queryType.startsWith("PARTS") &&
                chatMessage.getContextValue("contractNumber") == null) {
                return createErrorStructuredJSON(chatMessage.getMessage(),
                                                 "Contract number is required for all part queries. Please provide the contract number for faster results.");
            }

            // Handle other complete queries
            String preprocessedInput = preprocessInput(userInput);
            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);
            NLPEntityProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);

            return createStructuredJSONResponse(queryResult, contractsValidation);

        } catch (Exception e) {
            return createErrorStructuredJSON(chatMessage.getMessage(),
                                             "Error processing complete query: " + e.getMessage());
        }
    }

    /**
     * Handle lead time queries with complete information
     * Example: "What is the lead time for part EN6114V4-13 contract 100476"
     */
    private String handleLeadTimeQuery(ChatMessage chatMessage) {
        try {
            String partNumber = (String) chatMessage.getContextValue("partNumber");
            String contractNumber = (String) chatMessage.getContextValue("contractNumber");

            if (partNumber == null || contractNumber == null) {
                return createErrorStructuredJSON(chatMessage.getMessage(),
                                                 "Missing part number or contract number for lead time query");
            }

            System.out.println("DEBUG: Processing lead time query for part: " + partNumber + ", contract: " +
                               contractNumber);

            // FIX: Use correct parts table and column names
            // Table: HR.CCT_PARTS_TMG
            // Columns: INVOICE_PART_NUMBER (for part), LOADED_CP_NUMBER (for contract), LEAD_TIME
            String sqlQuery =
                "SELECT LEAD_TIME FROM " + TableColumnConfig.TABLE_PARTS +
                " WHERE INVOICE_PART_NUMBER = ? AND LOADED_CP_NUMBER = ?";

            System.out.println("DEBUG: Generated SQL: " + sqlQuery);

            // Create filters for the query
            List<NLPEntityProcessor.EntityFilter> filters = new ArrayList<>();
            filters.add(new NLPEntityProcessor.EntityFilter("INVOICE_PART_NUMBER", "=", partNumber, "extracted"));
            filters.add(new NLPEntityProcessor.EntityFilter("LOADED_CP_NUMBER", "=", contractNumber, "extracted"));

            // Create display entities for lead time
            List<String> displayEntities = new ArrayList<>();
            displayEntities.add("LEAD_TIME");

            // Prepare input parameters for database query
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("actionType", "parts_lead_time_query");
            inputParams.put("filters", filters);
            inputParams.put("displayEntities", displayEntities);
            inputParams.put("screenWidth", DEFAULT_SCREEN_WIDTH);
            inputParams.put("sqlQuery", sqlQuery);

            // Execute database query and get formatted result
            String result = pullData(inputParams);

            System.out.println("DEBUG: Database result: " + result);

            // Create structured JSON response
            return createStructuredJSONResponseForLeadTime(chatMessage, result, sqlQuery);

        } catch (Exception e) {
            System.out.println("DEBUG: Error in handleLeadTimeQuery: " + e.getMessage());
            e.printStackTrace();
            return createErrorStructuredJSON(chatMessage.getMessage(),
                                             "Error processing lead time query: " + e.getMessage());
        }
    }

    /**
     * Handle lead time queries without contract number
     * Example: "What is the lead time for part EN6114V4-13"
     * Uses existing NLP system for filtering and processing
     */
    private String handleLeadTimeQueryWithoutContract(ChatMessage chatMessage) {
        try {
            String userInput = chatMessage.getMessage();

            // Use existing NLP system to process the query
            String preprocessedInput = preprocessInput(userInput);
            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);
            NLPEntityProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);

            // The existing system will handle the filtering and show all matching parts
            // with their lead times across different contracts
            return createStructuredJSONResponse(queryResult, contractsValidation);

        } catch (Exception e) {
            return createErrorStructuredJSON(chatMessage.getMessage(),
                                             "Error processing lead time query: " + e.getMessage());
        }
    }

    /**
     * Create structured JSON response for lead time queries
     */
    private String createStructuredJSONResponseForLeadTime(ChatMessage chatMessage, String result, String sqlQuery) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Header section
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("contractNumber")))
            .append("\",\n");
        json.append("    \"partNumber\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("partNumber")))
            .append("\",\n");
        json.append("    \"customerNumber\": null,\n");
        json.append("    \"customerName\": null,\n");
        json.append("    \"createdBy\": null,\n");
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": \"")
            .append(escapeJson(chatMessage.getMessage()))
            .append("\",\n");
        json.append("      \"correctedInput\": null,\n");
        json.append("      \"correctionConfidence\": 1.0\n");
        json.append("    }\n");
        json.append("  },\n");

        // Query metadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": \"")
            .append(chatMessage.getQueryType())
            .append("\",\n");
        json.append("    \"actionType\": \"")
            .append(chatMessage.getActionType())
            .append("\",\n");
        json.append("    \"processingTimeMs\": 50,\n");
        json.append("    \"selectedModule\": \"PARTS\",\n");
        json.append("    \"routingConfidence\": 0.95\n");
        json.append("  },\n");

        // Entities section
        json.append("  \"entities\": [\n");
        json.append("    {\n");
        json.append("      \"attribute\": \"INVOICE_PART\",\n");
        json.append("      \"operation\": \"=\",\n");
        json.append("      \"value\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("partNumber")))
            .append("\",\n");
        json.append("      \"source\": \"extracted\"\n");
        json.append("    },\n");
        json.append("    {\n");
        json.append("      \"attribute\": \"LOADED_CP_NUMBER\",\n");
        json.append("      \"operation\": \"=\",\n");
        json.append("      \"value\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("contractNumber")))
            .append("\",\n");
        json.append("      \"source\": \"extracted\"\n");
        json.append("    }\n");
        json.append("  ],\n");

        // Display entities section
        json.append("  \"displayEntities\": [\"LEAD_TIME\"],\n");

        // Module specific data
        json.append("  \"moduleSpecificData\": {\n");
        json.append("    \"sqlQuery\": \"")
            .append(escapeJson(sqlQuery))
            .append("\",\n");
        json.append("    \"queryType\": \"lead_time_query\",\n");
        json.append("    \"partNumber\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("partNumber")))
            .append("\",\n");
        json.append("    \"contractNumber\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("contractNumber")))
            .append("\"\n");
        json.append("  },\n");

        // Errors section
        json.append("  \"errors\": [],\n");

        // Confidence
        json.append("  \"confidence\": 0.95,\n");

        // Processing result
        json.append("  \"processingResult\": \"")
            .append(escapeJson(result))
            .append("\"\n");

        json.append("}");

        return json.toString();
    }

    /**
     * Create structured JSON response for follow-up requests
     */
    private String createStructuredJSONResponseForFollowUp(ChatMessage chatMessage) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Header section
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": null,\n");
        json.append("    \"partNumber\": \"")
            .append(escapeJson((String) chatMessage.getContextValue("partNumber")))
            .append("\",\n");
        json.append("    \"customerNumber\": null,\n");
        json.append("    \"customerName\": null,\n");
        json.append("    \"createdBy\": null,\n");
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": \"")
            .append(escapeJson(chatMessage.getMessage()))
            .append("\",\n");
        json.append("      \"correctedInput\": null,\n");
        json.append("      \"correctionConfidence\": 1.0\n");
        json.append("    }\n");
        json.append("  },\n");

        // Query metadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": \"FOLLOW_UP_REQUEST\",\n");
        json.append("    \"actionType\": \"request_missing_info\",\n");
        json.append("    \"processingTimeMs\": 25,\n");
        json.append("    \"selectedModule\": \"CONVERSATION\",\n");
        json.append("    \"routingConfidence\": 0.90\n");
        json.append("  },\n");

        // Entities section
        json.append("  \"entities\": [],\n");

        // Display entities section
        json.append("  \"displayEntities\": [],\n");

        // Module specific data
        json.append("  \"moduleSpecificData\": {\n");
        json.append("    \"requiresFollowUp\": true,\n");
        json.append("    \"expectedResponseType\": \"")
            .append(escapeJson(chatMessage.getExpectedResponseType()))
            .append("\",\n");
        json.append("    \"sessionId\": \"")
            .append(escapeJson(chatMessage.getSessionId()))
            .append("\"\n");
        json.append("  },\n");

        // Errors section
        json.append("  \"errors\": [],\n");

        // Confidence
        json.append("  \"confidence\": 0.90,\n");

        // Processing result
        json.append("  \"processingResult\": \"")
            .append(escapeJson(chatMessage.getMessage()))
            .append("\"\n");

        json.append("}");

        return json.toString();
    }

    /**
     * Create contract by BOT with all provided parameters
     * @param inputParams Map containing all contract creation parameters
     * @return Contract number (6 digits) or "NA" for failed case
     */
    public String createContractByBOT(Map<String, String> inputParams) {
        System.out.println("NLPUSERActionHanlder=============createContractByBOT========="+inputParams);
        try {
            OperationBinding binding = BCCTChatBotUtility.findOperationBinding("createContractByBOT");
            binding.getParamsMap().put("contractData", inputParams);
            binding.getParamsMap().put("createdBy", "BOT");
            System.out.println("Calling AMIMPL class method-------------->");
            Map<String, Object> result = (Map<String, Object>) binding.execute();
            System.out.println("AFtre created by bot contarct-----"+result);
            boolean status=(Boolean)result.get("success");
            
            if(status){
                return ""+result.get("message");
            }else{
                return "NA";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "NA"; // Error occurred
        }
    }

    /**
     * Validate customer account number
     * @param accountNo Account number to validate
     * @return true if valid customer, false otherwise
     */
    public boolean isCustomerNumberValid(String accountNo) {
        try {
            if (accountNo == null || accountNo.trim().isEmpty()) {
                return false;
            }

            // Validate account number format (7+ digits per business rules)
            if (!accountNo.matches("\\d{7,}")) {
                return false;
            }

            OperationBinding binding = BCCTChatBotUtility.findOperationBinding("pullCustomerDetails");
            binding.getParamsMap().put("customerNumber", accountNo);
            Map result = (Map) binding.execute();
            System.out.println("result for pullCustomerDetails==============>"+result);
            return (Boolean)result.get("exists");


        } catch (Exception e) {
            return false;
        }
    }


    /**
     * CRITICAL BUSINESS RULE: Validate contract number for parts queries
     * All parts queries MUST have contract number due to 100M contracts × 10M parts = 1B parts
     * Without contract number, it's impossible to find specific data
     */
    private String validatePartsQueryContractNumber(NLPEntityProcessor.QueryResult queryResult) {
        String queryType = queryResult.getQueryType();
        String actionType = queryResult.getActionType();
        List<NLPEntityProcessor.EntityFilter> filters = queryResult.entities;

        // Check if this is a parts-related query
        boolean isPartsQuery =
            "PARTS".equals(queryType) || actionType.contains("parts") || actionType.contains("failed");

        if (!isPartsQuery) {
            return null; // Not a parts query, no validation needed
        }

        // Check for contract number in filters
        boolean hasContractNumber = false;
        String contractNumber = null;

        for (NLPEntityProcessor.EntityFilter filter : filters) {
            if ("CONTRACT_NO".equals(filter.attribute) || "LOADED_CP_NUMBER".equals(filter.attribute) ||
                "AWARD_NUMBER".equals(filter.attribute)) {
                hasContractNumber = true;
                contractNumber = filter.value;
                break;
            }
        }

        // CRITICAL: If no contract number found, return error message
        if (!hasContractNumber) {
            return createContractNumberRequiredMessage(queryType, actionType);
        }

        return null; // Contract number found, validation passed
    }

    /**
     * Create appropriate message asking for contract number
     */
    private String createContractNumberRequiredMessage(String queryType, String actionType) {
        StringBuilder message = new StringBuilder();
        message.append("<p><b>Contract Number Required</b><br>");
        message.append("To process your parts query, I need a contract number because:<br>");
        message.append("<ul>");
        message.append("<li>We have 100M+ contracts in our system</li>");
        message.append("<li>Each contract can have millions of parts</li>");
        message.append("<li>Without a contract number, I cannot efficiently find your specific data</li>");
        message.append("</ul>");
        message.append("<b>Please provide a contract number with your query.</b><br>");
        message.append("<i>Example: \"Show parts for contract 123456\" or \"How many parts failed in 789012\"</i></p>");
        return message.toString();
    }

    /**
     * Format result in tabular format from data rows
     */
    private String formatTabularResultFromData(List<Map<String, Object>> dataRows, List<String> displayEntities) {
        if (dataRows == null || dataRows.isEmpty() || displayEntities == null || displayEntities.isEmpty()) {
            return "<p>No data to display</p>";
        }
        StringBuilder response = new StringBuilder();
        response.append("<pre><small>");
        // Header row
        response.append("<b>");
        for (String entity : displayEntities) {
            String displayName = getDisplayName(entity);
            response.append(String.format("%-20s ", truncateString(displayName, 18)));
        }
        response.append("</b><br>");
        // Separator line
        int totalWidth = displayEntities.size() * 21;
        response.append(repeatString("=", totalWidth)).append("<br>");
        // Data rows
        for (Map<String, Object> row : dataRows) {
            if (row != null) {
                for (String entity : displayEntities) {
                    Object valueObj = row.get(entity);
                    String value = valueObj != null ? valueObj.toString() : "N/A";
                    value = truncateString(value, 18);
                    response.append(String.format("%-20s ", value));
                }
                response.append("<br>");
            }
        }
        // Bottom separator
        response.append(repeatString("=", totalWidth));
        response.append("</small></pre>");
        return response.toString();
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "N/A";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    private String truncateString(String str, int maxLength) {
        if (str == null)
            return "N/A";
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Handle Quick Action Button requests
     * Centralized method for all button actions
     * @param actionType The button action type
     * @return HTML formatted response for the button action
     */
    public String handleQuickActionButton(String actionType) {
        System.out.println("handleQuickActionButton================> actionType: " + actionType);
        
        try {
            switch (actionType.toUpperCase()) {
                case "QUICK_ACTION_RECENT_CONTRACTS":
                    return handleRecentContractsAction();
                    
                case "QUICK_ACTION_PARTS_COUNT":
                    return handlePartsCountAction();
                    
                case "QUICK_ACTION_FAILED_CONTRACTS":
                    return handleFailedContractsAction();
                    
                case "QUICK_ACTION_EXPIRING_SOON":
                    return handleExpiringSoonAction();
                    
                case "QUICK_ACTION_AWARD_REPS":
                    return handleAwardRepsAction();
                    
                case "QUICK_ACTION_HELP":
                    return handleHelpAction();
                    
                case "QUICK_ACTION_CREATE_CONTRACT":
                    return handleCreateContractAction();
                    
                default:
                    return "<p><b>Error:</b> Unknown button action: " + actionType + "</p>";
            }
        } catch (Exception e) {
            System.out.println("Error in handleQuickActionButton: " + e.getMessage());
            return "<p><b>Error:</b> Failed to process button action: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Handle Recent Contracts button - List contracts created in last 24 hours
     */
    private String handleRecentContractsAction() {
        System.out.println("handleRecentContractsAction================>");
        
        try {
            // Build SQL query for contracts created in last 24 hours
            String sqlQuery = "SELECT CONTRACT_NAME, CUSTOMER_NAME, EFFECTIVE_DATE, EXPIRATION_DATE, STATUS, CREATED_BY, CREATE_DATE " +
                            "FROM " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS) + " " +
                            "WHERE CREATE_DATE >= SYSDATE - 1 " +
                            "ORDER BY CREATE_DATE DESC";
            
            // Execute query using existing utility
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[0]);
            operationBind.getParamsMap().put("paramTypes", new String[0]);
            
            Map<String, Object> result = (Map<String, Object>) operationBind.execute();
            System.out.println("Recent Contracts Result: " + result);
            
            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }
            
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("rows");
            
            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No contracts created in the last 24 hours.</i></p>";
            }
            
            // Format the results
            return formatQueryResultInView(dataRows, getColumnNames(dataRows), 
                                          Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "CREATE_DATE", "STATUS"), 
                                          DEFAULT_SCREEN_WIDTH, "recent_contracts");
            
        } catch (Exception e) {
            return "<p><b>Error:</b> Failed to retrieve recent contracts: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Handle Parts Count button - Total Parts Loaded count
     */
    private String handlePartsCountAction() {
        System.out.println("handlePartsCountAction================>");
        
        try {
            // Build SQL query for total parts count
            String sqlQuery = "SELECT COUNT(*) as TOTAL_PARTS " +
                            "FROM " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_PARTS);
            
            // Execute query using existing utility
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[0]);
            operationBind.getParamsMap().put("paramTypes", new String[0]);
            
            Map<String, Object> result = (Map<String, Object>) operationBind.execute();
            System.out.println("Parts Count Result: " + result);
            
            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }
            
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("rows");
            
            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No parts data available.</i></p>";
            }
            
            // Extract count from first row
            Map<String, Object> firstRow = dataRows.get(0);
            Object totalParts = firstRow.get("TOTAL_PARTS");
            
            return "<div style='text-align: center; padding: 20px;'>" +
                   "<h3>Total Parts Loaded</h3>" +
                   "<div style='font-size: 48px; font-weight: bold; color: #007bff;'>" + totalParts + "</div>" +
                   "<p style='color: #666;'>Total number of parts in the system</p>" +
                   "</div>";
            
        } catch (Exception e) {
            return "<p><b>Error:</b> Failed to retrieve parts count: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Handle Failed Contracts button - Show contracts and count for each contract (failed parts)
     */
    private String handleFailedContractsAction() {
        System.out.println("handleFailedContractsAction================>");
        
        try {
            // Build SQL query for failed parts grouped by contract
            String sqlQuery = "SELECT c.CONTRACT_NAME, c.CUSTOMER_NAME, COUNT(fp.PART_NUMBER) as FAILED_PARTS_COUNT " +
                            "FROM " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS) + " c " +
                            "LEFT JOIN " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_FAILED_PARTS) + " fp " +
                            "ON c.AWARD_NUMBER = fp.CONTRACT_NO " +
                            "GROUP BY c.CONTRACT_NAME, c.CUSTOMER_NAME, c.AWARD_NUMBER " +
                            "HAVING COUNT(fp.PART_NUMBER) > 0 " +
                            "ORDER BY FAILED_PARTS_COUNT DESC";
            
            // Execute query using existing utility
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[0]);
            operationBind.getParamsMap().put("paramTypes", new String[0]);
            
            Map<String, Object> result = (Map<String, Object>) operationBind.execute();
            System.out.println("Failed Contracts Result: " + result);
            
            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }
            
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("rows");
            
            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No contracts with failed parts found.</i></p>";
            }
            
            // Format the results
            return formatQueryResultInView(dataRows, getColumnNames(dataRows), 
                                          Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "FAILED_PARTS_COUNT"), 
                                          DEFAULT_SCREEN_WIDTH, "failed_contracts");
            
        } catch (Exception e) {
            return "<p><b>Error:</b> Failed to retrieve failed contracts: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Handle Expiring Soon button - Show contracts in expiring order
     */
    private String handleExpiringSoonAction() {
        System.out.println("handleExpiringSoonAction================>");
        
        try {
            // Build SQL query for contracts expiring soon (next 30 days)
            String sqlQuery = "SELECT CONTRACT_NAME, CUSTOMER_NAME, EFFECTIVE_DATE, EXPIRATION_DATE, STATUS, " +
                            "ROUND(EXPIRATION_DATE - SYSDATE) as DAYS_TO_EXPIRE " +
                            "FROM " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS) + " " +
                            "WHERE EXPIRATION_DATE >= SYSDATE " +
                            "AND EXPIRATION_DATE <= SYSDATE + 30 " +
                            "ORDER BY EXPIRATION_DATE ASC";
            
            // Execute query using existing utility
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[0]);
            operationBind.getParamsMap().put("paramTypes", new String[0]);
            
            Map<String, Object> result = (Map<String, Object>) operationBind.execute();
            System.out.println("Expiring Soon Result: " + result);
            
            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }
            
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("rows");
            
            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No contracts expiring in the next 30 days.</i></p>";
            }
            
            // Format the results
            return formatQueryResultInView(dataRows, getColumnNames(dataRows), 
                                          Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "EXPIRATION_DATE", "DAYS_TO_EXPIRE"), 
                                          DEFAULT_SCREEN_WIDTH, "expiring_soon");
            
        } catch (Exception e) {
            return "<p><b>Error:</b> Failed to retrieve expiring contracts: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Handle Award Reps button - List the award reps
     */
    private String handleAwardRepsAction() {
        System.out.println("handleAwardRepsAction================>");
        
        try {
            // Build SQL query for unique award reps
            String sqlQuery = "SELECT DISTINCT AWARD_REP, COUNT(*) as CONTRACT_COUNT " +
                            "FROM " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS) + " " +
                            "WHERE AWARD_REP IS NOT NULL " +
                            "GROUP BY AWARD_REP " +
                            "ORDER BY CONTRACT_COUNT DESC";
            
            // Execute query using existing utility
            OperationBinding operationBind = BCCTChatBotUtility.findOperationBinding("executeDynamicQuery");
            operationBind.getParamsMap().put("sqlQuery", sqlQuery);
            operationBind.getParamsMap().put("paramValues", new String[0]);
            operationBind.getParamsMap().put("paramTypes", new String[0]);
            
            Map<String, Object> result = (Map<String, Object>) operationBind.execute();
            System.out.println("Award Reps Result: " + result);
            
            if (!(Boolean) result.get("success")) {
                return "<p><b>Error:</b> " + result.get("message") + "</p>";
            }
            
            List<Map<String, Object>> dataRows = (List<Map<String, Object>>) result.get("rows");
            
            if (dataRows == null || dataRows.isEmpty()) {
                return "<p><i>No award representatives found.</i></p>";
            }
            
            // Format the results
            return formatQueryResultInView(dataRows, getColumnNames(dataRows), 
                                          Arrays.asList("AWARD_REP", "CONTRACT_COUNT"), 
                                          DEFAULT_SCREEN_WIDTH, "award_reps");
            
        } catch (Exception e) {
            return "<p><b>Error:</b> Failed to retrieve award reps: " + e.getMessage() + "</p>";
        }
    }
    
    /**
     * Handle Help button - Show the default message
     */
    private String handleHelpAction() {
        System.out.println("handleHelpAction================>");
        
        return "<div style='padding: 20px; background-color: #f8f9fa; border-radius: 8px;'>" +
               "<h3>BCCT Contract Management Assistant</h3>" +
               "<p>Welcome to the BCCT Contract Management System. Here's what you can do:</p>" +
               "<ul>" +
               "<li><b>Recent Contracts:</b> View contracts created in the last 24 hours</li>" +
               "<li><b>Parts Count:</b> See total number of parts loaded in the system</li>" +
               "<li><b>Failed Contracts:</b> View contracts with failed parts and their counts</li>" +
               "<li><b>Expiring Soon:</b> Check contracts expiring in the next 30 days</li>" +
               "<li><b>Award Reps:</b> List all award representatives and their contract counts</li>" +
               "<li><b>Create Contract:</b> Get step-by-step instructions for contract creation</li>" +
               "</ul>" +
               "<p><b>Natural Language Queries:</b> You can also ask questions like:</p>" +
               "<ul>" +
               "<li>'Show me contracts created by John Smith'</li>" +
               "<li>'What contracts expire in 2024?'</li>" +
               "<li>'List parts for contract ABC123'</li>" +
               "<li>'Create a new contract'</li>" +
               "</ul>" +
               "<p><i>Need more help? Just ask!</i></p>" +
               "</div>";
    }
    
    /**
     * Handle Create Contract button - Show the steps to create contracts
     */
    private String handleCreateContractAction() {
        System.out.println("handleCreateContractAction================>");
        
        return "<div style='padding: 20px; background-color: #e8f5e8; border-radius: 8px;'>" +
               "<h3>Contract Creation Guide</h3>" +
               "<p>Follow these steps to create a contract manually:</p>" +
               "<ol>" +
               "<li><b>Login to BCCT Application:</b> You will land on the Opportunities screen</li>" +
               "<li><b>Navigate to Award Management:</b> Click on the 'Award Management' link</li>" +
               "<li><b>Access Award Management Dashboard:</b> You will be taken to the Award Management Dashboard</li>" +
               "<li><b>Select Implementation:</b> Click on any area of the Award Implementation Pie Chart or click on the Total Implementation number</li>" +
               "<li><b>Navigate to Awards Result:</b> You will be taken to the Awards Result screen</li>" +
               "<li><b>Create Award:</b> Click on the 'Create Award' button</li>" +
               "<li><b>Fill Contract Details:</b> You will be taken to the Contract Creation/Award screen where you can fill the following data:" +
               "<ul>" +
               "<li>Account Number (6+ digits)</li>" +
               "<li>Contract Name</li>" +
               "<li>Title</li>" +
               "<li>Description</li>" +
               "<li>Comments (optional)</li>" +
               "<li>Price List Contract (Yes/No)</li>" +
               "</ul></li>" +
               "<li><b>Save the Data:</b> Review all details and save the contract</li>" +
               "</ol>" +
               "<p><b>Required Fields:</b></p>" +
               "<ul>" +
               "<li>Account Number (mandatory)</li>" +
               "<li>Contract Name</li>" +
               "<li>Title</li>" +
               "<li>Description</li>" +
               "</ul>" +
               "<p><b>Automated Creation:</b> You can also use the comma-separated format:</p>" +
               "<p style='background-color: #f0f0f0; padding: 10px; border-radius: 4px; font-family: monospace;'>" +
               "123456789, ContractName, Title, Description, Comments, No" +
               "</p>" +
               "<p><i>Need help with automated creation? Just say 'create contract' and follow the prompts!</i></p>" +
               "</div>";
    }

    public  Map<String, String> validateCheckListDates(Map<String, String> dateMap) {
        Map<String, String> validationResults = new HashMap<>();
        if (dateMap == null || dateMap.isEmpty()) {
            validationResults.put("ERROR", "No dates to validate");
            return validationResults;
        }

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('/')
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter();
        LocalDate today = LocalDate.now();
        Map<String, LocalDate> parsedDates = new HashMap<>();
        // Parse all dates first
        for (Map.Entry<String, String> entry : dateMap.entrySet()) {
            try {
                parsedDates.put(entry.getKey(), LocalDate.parse(entry.getValue(), formatter));
            } catch (Exception e) {
                validationResults.put(entry.getKey(), "Invalid date format: " + entry.getValue());
            }
        }
        // Now validate each field
        for (String key : dateMap.keySet()) {
            LocalDate date = parsedDates.get(key);
            if (date == null) continue; // Already marked as invalid
            switch (key) {
                case "EXPIRATION_DATE":
                    if (date.isBefore(today)) {
                        validationResults.put(key, "Expiration date must be in the future: " + dateMap.get(key));
                    } else {
                        boolean afterAll = true;
                        for (Map.Entry<String, LocalDate> e : parsedDates.entrySet()) {
                            if (!e.getKey().equals("EXPIRATION_DATE") && date.isBefore(e.getValue())) {
                                afterAll = false;
                                break;
                            }
                        }
                        if (!afterAll) {
                            validationResults.put(key, "Expiration date must be after all other dates: " + dateMap.get(key));
                        } else {
                            validationResults.put(key, "success");
                        }
                    }
                    break;
                case "PRICE_EXPIRATION_DATE":
                    LocalDate expDate = parsedDates.get("EXPIRATION_DATE");
                    if (expDate != null && date.isAfter(expDate)) {
                        validationResults.put(key, "Price expiration date cannot be after expiration date: " + dateMap.get(key));
                    } else {
                        validationResults.put(key, "success");
                    }
                    break;
                case "SYSTEM_LOADED_DATE":
                case "DATE_OF_SIGNATURE":
                    if (!date.equals(today)) {
                        validationResults.put(key, key.replace("_", " ") + " must be today: " + dateMap.get(key));
                    } else {
                        validationResults.put(key, "success");
                    }
                    break;
                case "QUATAR":
                    if (date.isBefore(today)) {
                        validationResults.put(key, "Qatar date is expired: " + dateMap.get(key));
                    } else {
                        validationResults.put(key, "success");
                    }
                    break;
                default:
                    if (date.isBefore(today)) {
                        validationResults.put(key, "Date must not be in the past: " + dateMap.get(key));
                    } else {
                        validationResults.put(key, "success");
                    }
                    break;
            }
        }
        return validationResults;
    }


    /**
     * Create checklist entry in the system after validation passes
     * @param checklistData Map of checklist fields and values
     * @return 'SUCCESS' if created, or error message if failed
     */
    public Map checkListCreation(Map<String, String> checklistData) {
        System.out.println("checkListCreation==============>"+checklistData);
        // TODO: Implement actual checklist creation logic (DB save, etc.)
        // For now, return 'SUCCESS' as a placeholder
        return null;
    }
    
    
    public Map saveUserSessionObject(Object input){
        
        return null;
    }
    public static void main(String v[]){
        NLPUserActionHandler ob=new NLPUserActionHandler();
        Map<String, String> dateMap =new HashMap<>();
        dateMap.put("EXPIRATION_DATE", "8/8/25");
        dateMap.put("PRICE_EXPIRATION_DATE", "7/27/25");
        dateMap.put("SYSTEM_LOADED_DATE", "7/23/25");
        dateMap.put("QUATAR", "7/23/25");
        dateMap.put("DATE_OF_SIGNATURE", "7/23/25");
        
        Map checkListCreation = ob.validateCheckListDates(dateMap);
        
        System.out.println(checkListCreation);

    }
}
