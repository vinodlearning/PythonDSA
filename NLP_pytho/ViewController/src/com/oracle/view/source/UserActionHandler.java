//package com.oracle.view.source;
//
//import com.oracle.view.deep.TestQueries;
//import com.oracle.view.deep.ContractsModel;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.ArrayList;
//
///**
// * UserActionHandler - Main interface for user interactions
// * Handles user input processing and routes to appropriate action methods
// * Enhanced with Neural Network Classification for improved query understanding
// */
//public class UserActionHandler {
//    
//    // Neural Network Integration
//    private final NeuralNetworkClassifier neuralClassifier;
//    private final QueryClassifier ruleBasedClassifier;
//    private final boolean useNeuralNetwork;
//    
//
//    
//    // Test cases for comprehensive testing
//    private static final List<String> ALL_TEST_CASES = Arrays.asList(
//        // Proper questions
//        "What is the contract number for contract ABC123?",
//        "Can you show me the parts for contract XYZ789?",
//        "Which contracts have status 'ACTIVE'?",
//        "Show me failed parts for contract DEF456",
//        "What are the details of part number P001?",
//        
//        // Direct commands
//        "Show contract ABC123",
//        "Display parts for XYZ789",
//        "List active contracts",
//        "Get failed parts DEF456",
//        "Find part P001",
//        
//        // Typos and abbreviations
//        "Show ctrct ABC123",
//        "Display prts for XYZ789", 
//        "List actv cntrcts",
//        "Get fld prts DEF456",
//        "Find prt P001",
//        "ctrct ABC123",
//        "prts XYZ789",
//        "cntrcts",
//        "fld prts",
//        "prt P001",
//        
//        // Unusual word orders
//        "ABC123 contract show",
//        "XYZ789 parts display",
//        "Active contracts list",
//        "DEF456 failed parts get",
//        "P001 part find",
//        
//        // Edge cases
//        "Contract",
//        "Parts",
//        "Failed",
//        "ABC123",
//        "P001",
//        
//        // Very short inputs
//        "c",
//        "p", 
//        "f",
//        "a",
//        "x",
//        
//        // Very long inputs
//        "Please could you kindly show me the complete detailed information about the contract with number ABC123 including all its associated parts and status information",
//        "I would like to request that you display for me all the parts that are associated with the contract identified by the number XYZ789 and show me their current status",
//        
//        // Help/instruction queries
//        "How do I create a contract?",
//        "Help me create a contract",
//        "I need help creating a contract",
//        "What are the steps to create a contract?",
//        "Can you help me create a contract?",
//        "Create contract for me",
//        "Make a contract",
//        "Generate contract",
//        "Build contract",
//        "Set up contract"
//    );
//    
//    private StandardJSONProcessor nlpProcessor;
//    private ActionTypeDataProvider dataProvider;
//    private ContractsModel contractsModel;
//    
//    public UserActionHandler() {
//        this.nlpProcessor = new StandardJSONProcessor();
//        this.dataProvider = new ActionTypeDataProvider();
//        this.contractsModel = new ContractsModel();
//        
//        // Initialize neural network components
//        this.neuralClassifier = new NeuralNetworkClassifier();
//        this.ruleBasedClassifier = new QueryClassifier();
//        this.useNeuralNetwork = true; // Can be configured via system property
//        
//        // Pre-train the neural network with common query patterns
//        preTrainNeuralNetwork();
//    }
//    
//    /**
//     * Enhanced input preprocessing using WordDatabase
//     */
//    private String preprocessInput(String userInput) {
//        if (userInput == null || userInput.trim().isEmpty()) {
//            return userInput;
//        }
//        
//        String[] words = userInput.split("\\s+");
//        StringBuilder processedInput = new StringBuilder();
//        
//        for (int i = 0; i < words.length; i++) {
//            String word = words[i];
//            String processedWord = WordDatabase.normalizeWord(word);
//            
//            if (processedWord != null) {
//                processedInput.append(processedWord);
//            } else {
//                processedInput.append(word);
//            }
//            
//            if (i < words.length - 1) {
//                processedInput.append(" ");
//            }
//        }
//        
//        return processedInput.toString();
//    }
//    
//    /**
//     * Enhanced query type detection using WordDatabase and ContractsModel
//     */
//    private String detectQueryTypeEnhanced(String userInput) {
//        String lowerInput = userInput.toLowerCase();
//        
//        // Check for creation-related queries using WordDatabase
//        if (WordDatabase.containsCreationWords(lowerInput)) {
//            return "CONTRACTS";
//        }
//        
//        // Check for imperative indicators
//        if (WordDatabase.containsImperativeIndicators(lowerInput)) {
//            // This might be a help request or command
//            if (lowerInput.contains("help") || lowerInput.contains("how") || 
//                WordDatabase.containsQuestionWords(lowerInput)) {
//                return "HELP";
//            }
//        }
//        
//        // Use ContractsModel for additional business logic validation
//        try {
//            Map<String, Object> contractsResult = contractsModel.processQuery(userInput);
//            if (contractsResult != null && contractsResult.containsKey("queryType")) {
//                return (String) contractsResult.get("queryType");
//            }
//        } catch (Exception e) {
//            // Fallback to existing logic
//        }
//        
//        // Default to existing logic
//        return null;
//    }
//    
//    /**
//     * Process query using ContractsModel for business validation
//     */
//    private Map<String, Object> processWithContractsModel(String userInput) {
//        try {
//            return contractsModel.processQuery(userInput);
//        } catch (Exception e) {
//            // Return null if ContractsModel fails, fallback to StandardJSONProcessor
//            return null;
//        }
//    }
//    
//    // ============================================================================
//    // THREE MAIN METHODS FOR UI INTEGRATION
//    // ============================================================================
//    
//    /**
//     * METHOD 1: Get complete response as String (after DataProvider actions)
//     * UI calls this method to get complete response including DataProvider results
//     */
//    public String processUserInputCompleteResponse(String userInput) {
//        try {
//            // Step 1: Preprocess input using WordDatabase
//            String preprocessedInput = preprocessInput(userInput);
//            
//            // Step 2: Validate with ContractsModel for business logic
//            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);
//            
//            // Step 3: Get QueryResult from StandardJSONProcessor
//            StandardJSONProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);
//            
//            // Step 2: Execute DataProvider actions based on action type
//            String dataProviderResult = executeDataProviderAction(queryResult);
//            
//            // Step 3: Return complete response as JSON string
//            return createCompleteResponseJSON(queryResult, dataProviderResult);
//            
//        } catch (Exception e) {
//            return createErrorResponseJSON(userInput, "Error processing user input: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * METHOD 2: Get structured JSON response with specified format
//     * UI calls this method to get standardized JSON response
//     */
//    public String processUserInputJSONResponse(String userInput) {
//        try {
//            // Preprocess input using WordDatabase
//            String preprocessedInput = preprocessInput(userInput);
//            
//            // Validate with ContractsModel for business logic
//            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);
//            
//            // Get QueryResult from StandardJSONProcessor
//            StandardJSONProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);
//            
//            // Return structured JSON response
//            return createStructuredJSONResponse(queryResult, contractsValidation);
//            
//        } catch (Exception e) {
//            return createErrorStructuredJSON(userInput, "Error processing user input: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * METHOD 3: Get Java object with complete information including SQL
//     * UI calls this method to get Java object with all details
//     */
//    public UserActionResponse processUserInputCompleteObject(String userInput) {
//        try {
//            // Step 1: Preprocess input using WordDatabase
//            String preprocessedInput = preprocessInput(userInput);
//            
//            // Step 2: Validate with ContractsModel for business logic
//            Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);
//            
//            // Step 3: Get QueryResult from StandardJSONProcessor
//            StandardJSONProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);
//            
//            // Step 2: Create UserActionResponse with all details
//            UserActionResponse response = createUserActionResponse(queryResult, contractsValidation);
//            
//            // Step 3: Execute DataProvider action and add result
//            String dataProviderResult = executeDataProviderAction(queryResult);
//            response.addParameter("dataProviderResult", dataProviderResult);
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error processing user input: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * METHOD 4: Enhanced processing with Neural Network Classification
//     * Uses neural network for improved query classification with fallback safety
//     */
//    public UserActionResponse processUserInputWithNeuralClassification(String userInput) {
//        try {
//            // Step 1: Preprocess input using WordDatabase
//            String preprocessedInput = preprocessInput(userInput);
//            
//            // Step 2: Get enhanced classification using neural network
//            String enhancedQueryType = classifyQueryWithNeuralNetwork(preprocessedInput);
//            
//            // Step 3: Get QueryResult from StandardJSONProcessor
//            StandardJSONProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);
//            
//            // Step 4: Override query type with neural network prediction if confident
//            if (useNeuralNetwork && isNeuralPredictionConfident(preprocessedInput, enhancedQueryType)) {
//                queryResult.metadata.queryType = enhancedQueryType;
//            }
//            
//            // Step 5: Create UserActionResponse with enhanced classification
//            UserActionResponse response = new UserActionResponse();
//            response.setOriginalInput(userInput);
//            response.setCorrectedInput(queryResult.inputTracking.correctedInput);
//            response.setQueryType(queryResult.metadata.queryType);
//            response.setActionType(queryResult.metadata.actionType);
//            response.setNlpProcessingTime(queryResult.metadata.processingTimeMs);
//            response.setFilters(queryResult.entities);
//            response.setDisplayEntities(queryResult.displayEntities);
//            response.setSuccess(true);
//            response.setMessage("Request processed successfully with neural network classification");
//            
//            // Step 6: Add neural network analysis for debugging
//            Map<String, Object> neuralAnalysis = getClassificationAnalysis(preprocessedInput);
//            response.addParameter("neuralAnalysis", neuralAnalysis);
//            
//            // Step 7: Generate SQL query automatically
//            response.generateSQLQuery();
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error processing user input with neural classification: " + e.getMessage());
//        }
//    }
//    
//    // ============================================================================
//    // HELPER METHODS
//    // ============================================================================
//    
//    /**
//     * Get QueryResult from StandardJSONProcessor
//     */
//    private StandardJSONProcessor.QueryResult getQueryResultFromNLP(String userInput) throws Exception {
//        // Use the original processQuery method to get proper inputTracking
//        String jsonResponse = nlpProcessor.processQuery(userInput);
//        
//        // Parse the response to extract metadata
//        return nlpProcessor.parseJSONToObject(jsonResponse);
//    }
//    
//    /**
//     * Execute DataProvider action based on action type
//     */
//    private String executeDataProviderAction(StandardJSONProcessor.QueryResult queryResult) {
//        try {
//            // Route to appropriate action handler based on action type
//            return routeToActionHandlerWithDataProvider(queryResult.metadata.actionType, 
//                                                      queryResult.entities, 
//                                                      queryResult.displayEntities, 
//                                                      queryResult.inputTracking.originalInput);
//        } catch (Exception e) {
//            return "Error executing DataProvider action: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Create UserActionResponse with complete information
//     */
//    private UserActionResponse createUserActionResponse(StandardJSONProcessor.QueryResult queryResult, Map<String, Object> contractsValidation) {
//        UserActionResponse response = new UserActionResponse();
//        response.setOriginalInput(queryResult.inputTracking.originalInput);
//        response.setCorrectedInput(queryResult.inputTracking.correctedInput);
//        response.setQueryType(queryResult.metadata.queryType);
//        response.setActionType(queryResult.metadata.actionType);
//        response.setNlpProcessingTime(queryResult.metadata.processingTimeMs);
//        response.setFilters(queryResult.entities);
//        response.setDisplayEntities(queryResult.displayEntities);
//        response.setSuccess(true);
//        response.setMessage("Request processed successfully");
//        
//        // Add ContractsModel validation results if available
//        if (contractsValidation != null) {
//            response.addParameter("contractsValidation", contractsValidation);
//            response.addParameter("businessValidationPassed", contractsValidation.get("success"));
//        }
//        
//        // Generate SQL query automatically
//        response.generateSQLQuery();
//        
//        return response;
//    }
//    
//    /**
//     * Route to appropriate action handler with DataProvider integration
//     */
//    private String routeToActionHandlerWithDataProvider(String actionType, 
//                                                       List<StandardJSONProcessor.EntityFilter> filters, 
//                                                       List<String> displayEntities, 
//                                                       String userInput) {
//        
//        switch (actionType) {
//            case "contracts_by_contractnumber":
//                return handleContractByContractNumberWithDataProvider(filters, displayEntities, userInput);
//                
//            case "parts_by_contract_number":
//                return handlePartsByContractNumberWithDataProvider(filters, displayEntities, userInput);
//                
//            case "parts_failed_by_contract_number":
//                return handleFailedPartsByContractNumberWithDataProvider(filters, displayEntities, userInput);
//                
//            case "parts_by_part_number":
//                return handlePartsByPartNumberWithDataProvider(filters, displayEntities, userInput);
//                
//            case "contracts_by_filter":
//                return handleContractsByFilterWithDataProvider(filters, displayEntities, userInput);
//                
//            case "parts_by_filter":
//                return handlePartsByFilterWithDataProvider(filters, displayEntities, userInput);
//                
//            case "update_contract":
//                return handleUpdateContractWithDataProvider(filters, displayEntities, userInput);
//                
//            case "create_contract":
//                return handleCreateContractWithDataProvider(filters, displayEntities, userInput);
//                
//            case "HELP_CONTRACT_CREATE_USER":
//                return handleHelpContractCreateUserWithDataProvider(filters, displayEntities, userInput);
//                
//            case "HELP_CONTRACT_CREATE_BOT":
//                return handleHelpContractCreateBotWithDataProvider(filters, displayEntities, userInput);
//                
//            default:
//                return "Unknown action type: " + actionType;
//        }
//    }
//    
//    // ============================================================================
//    // DATA PROVIDER ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles contract queries by contract number with DataProvider
//     */
//    private String handleContractByContractNumberWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                                 List<String> displayEntities, 
//                                                                 String userInput) {
//        try {
//            // Extract contract number filter
//            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
//            if (contractNumber == null) {
//                return "Contract number not found in query";
//            }
//            
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("contracts_by_contractnumber", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in contract query: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles parts queries by contract number with DataProvider
//     */
//    private String handlePartsByContractNumberWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                              List<String> displayEntities, 
//                                                              String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("parts_by_contract_number", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in parts query: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles failed parts queries by contract number with DataProvider
//     */
//    private String handleFailedPartsByContractNumberWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                                    List<String> displayEntities, 
//                                                                    String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("parts_failed_by_contract_number", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in failed parts query: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles parts queries by part number with DataProvider
//     */
//    private String handlePartsByPartNumberWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                          List<String> displayEntities, 
//                                                          String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("parts_by_part_number", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in parts query: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles contract queries by filter with DataProvider
//     */
//    private String handleContractsByFilterWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                          List<String> displayEntities, 
//                                                          String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("contracts_by_filter", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in contract filter query: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles parts queries by filter with DataProvider
//     */
//    private String handlePartsByFilterWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                      List<String> displayEntities, 
//                                                      String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("parts_by_filter", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in parts filter query: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles contract updates with DataProvider
//     */
//    private String handleUpdateContractWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                       List<String> displayEntities, 
//                                                       String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("update_contract", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in contract update: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles contract creation with DataProvider
//     */
//    private String handleCreateContractWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                       List<String> displayEntities, 
//                                                       String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("create_contract", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in contract creation: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles help contract create user with DataProvider
//     */
//    private String handleHelpContractCreateUserWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                               List<String> displayEntities, 
//                                                               String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("HELP_CONTRACT_CREATE_USER", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in help contract create user: " + e.getMessage();
//        }
//    }
//    
//    /**
//     * Handles help contract create bot with DataProvider
//     */
//    private String handleHelpContractCreateBotWithDataProvider(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                              List<String> displayEntities, 
//                                                              String userInput) {
//        try {
//            // Call ActionTypeDataProvider method
//            return dataProvider.executeAction("HELP_CONTRACT_CREATE_BOT", filters, displayEntities, userInput);
//            
//        } catch (Exception e) {
//            return "Error in help contract create bot: " + e.getMessage();
//        }
//    }
//    
//    // ============================================================================
//    // JSON RESPONSE CREATION METHODS
//    // ============================================================================
//    
//    /**
//     * Create complete response JSON
//     */
//    private String createCompleteResponseJSON(StandardJSONProcessor.QueryResult queryResult, String dataProviderResult) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        json.append("  \"success\": true,\n");
//        json.append("  \"message\": \"Request processed successfully\",\n");
//        json.append("  \"nlpResponse\": {\n");
//        json.append("    \"originalInput\": \"").append(escapeJson(queryResult.inputTracking.originalInput)).append("\",\n");
//        json.append("    \"correctedInput\": \"").append(escapeJson(queryResult.inputTracking.correctedInput)).append("\",\n");
//        json.append("    \"queryType\": \"").append(escapeJson(queryResult.metadata.queryType)).append("\",\n");
//        json.append("    \"actionType\": \"").append(escapeJson(queryResult.metadata.actionType)).append("\",\n");
//        json.append("    \"processingTimeMs\": ").append(queryResult.metadata.processingTimeMs).append("\n");
//        json.append("  },\n");
//        json.append("  \"dataProviderResponse\": \"").append(escapeJson(dataProviderResult)).append("\"\n");
//        json.append("}");
//        return json.toString();
//    }
//    
//    /**
//     * Create structured JSON response
//     */
//    private String createStructuredJSONResponse(StandardJSONProcessor.QueryResult queryResult, Map<String, Object> contractsValidation) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        
//        // Header
//        json.append("  \"header\": {\n");
//        json.append("    \"contractNumber\": \"").append(escapeJson(extractContractNumber(queryResult))).append("\",\n");
//        json.append("    \"partNumber\": \"").append(escapeJson(extractPartNumber(queryResult))).append("\",\n");
//        json.append("    \"customerNumber\": \"\",\n");
//        json.append("    \"customerName\": \"\",\n");
//        json.append("    \"createdBy\": \"\"\n");
//        json.append("  },\n");
//        
//        // Query Metadata
//        json.append("  \"queryMetadata\": {\n");
//        json.append("    \"queryType\": \"").append(escapeJson(queryResult.metadata.queryType)).append("\",\n");
//        json.append("    \"actionType\": \"").append(escapeJson(queryResult.metadata.actionType)).append("\",\n");
//        json.append("    \"processingTimeMs\": ").append(queryResult.metadata.processingTimeMs).append(",\n");
//        json.append("    \"spellCorrection\": {\n");
//        json.append("      \"originalInput\": \"").append(escapeJson(queryResult.inputTracking.originalInput)).append("\",\n");
//        json.append("      \"correctedInput\": \"").append(escapeJson(queryResult.inputTracking.correctedInput)).append("\"\n");
//        json.append("    }\n");
//        json.append("  },\n");
//        
//        // Entities
//        json.append("  \"entities\": [],\n");
//        
//        // Display Entities
//        json.append("  \"displayEntities\": [],\n");
//        
//        // Errors
//        json.append("  \"errors\": []");
//        
//        // Add ContractsModel validation if available
//        if (contractsValidation != null) {
//            json.append(",\n  \"businessValidation\": {\n");
//            json.append("    \"validated\": true,\n");
//            json.append("    \"queryType\": \"").append(escapeJson((String) contractsValidation.get("queryType"))).append("\",\n");
//            json.append("    \"action\": \"").append(escapeJson((String) contractsValidation.get("action"))).append("\",\n");
//            json.append("    \"confidence\": ").append(contractsValidation.get("confidence")).append("\n");
//            json.append("  }");
//        }
//        
//        json.append("\n}");
//        return json.toString();
//    }
//    
//    /**
//     * Create error response JSON
//     */
//    private String createErrorResponseJSON(String userInput, String errorMessage) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        json.append("  \"success\": false,\n");
//        json.append("  \"message\": \"").append(escapeJson(errorMessage)).append("\",\n");
//        json.append("  \"originalInput\": \"").append(escapeJson(userInput)).append("\"\n");
//        json.append("}");
//        return json.toString();
//    }
//    
//    /**
//     * Create error structured JSON
//     */
//    private String createErrorStructuredJSON(String userInput, String errorMessage) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        json.append("  \"header\": {\n");
//        json.append("    \"contractNumber\": \"\",\n");
//        json.append("    \"partNumber\": \"\",\n");
//        json.append("    \"customerNumber\": \"\",\n");
//        json.append("    \"customerName\": \"\",\n");
//        json.append("    \"createdBy\": \"\"\n");
//        json.append("  },\n");
//        json.append("  \"queryMetadata\": {\n");
//        json.append("    \"queryType\": \"ERROR\",\n");
//        json.append("    \"actionType\": \"ERROR\",\n");
//        json.append("    \"processingTimeMs\": 0,\n");
//        json.append("    \"spellCorrection\": {\n");
//        json.append("      \"originalInput\": \"").append(escapeJson(userInput)).append("\",\n");
//        json.append("      \"correctedInput\": \"").append(escapeJson(userInput)).append("\"\n");
//        json.append("    }\n");
//        json.append("  },\n");
//        json.append("  \"entities\": [],\n");
//        json.append("  \"displayEntities\": [],\n");
//        json.append("  \"errors\": [\"").append(escapeJson(errorMessage)).append("\"]\n");
//        json.append("}");
//        return json.toString();
//    }
//    
//    /**
//     * Extract contract number from query result
//     */
//    private String extractContractNumber(StandardJSONProcessor.QueryResult queryResult) {
//        for (StandardJSONProcessor.EntityFilter filter : queryResult.entities) {
//            if (filter.attribute.equals("AWARD_NUMBER")) {
//                return filter.value;
//            }
//        }
//        return "";
//    }
//    
//    /**
//     * Generate SQL query based on query result and ContractsModel validation
//     */
//    private String generateSQLQuery(StandardJSONProcessor.QueryResult queryResult) {
//        // Basic SQL generation based on action type
//        String actionType = queryResult.metadata.actionType;
//        String queryType = queryResult.metadata.queryType;
//        
//        switch (actionType) {
//            case "contracts_by_contractnumber":
//                return "SELECT * FROM CRM_CONTRACTS WHERE AWARD_NUMBER = ?";
//            case "parts_by_contract_number":
//                return "SELECT * FROM PARTS WHERE AWARD_ID = ?";
//            case "parts_failed_by_contract_number":
//                return "SELECT * FROM FAILED_PARTS WHERE AWARD_NUMBER = ?";
//            case "parts_by_part_number":
//                return "SELECT * FROM PARTS WHERE INVOICE_PART_NUMBER = ?";
//            case "contracts_by_filter":
//                return "SELECT * FROM CRM_CONTRACTS WHERE " + buildWhereClause(queryResult.entities);
//            case "parts_by_filter":
//                return "SELECT * FROM PARTS WHERE " + buildWhereClause(queryResult.entities);
//            default:
//                return "SELECT * FROM CRM_CONTRACTS";
//        }
//    }
//    
//    /**
//     * Extract part number from query result
//     */
//    private String extractPartNumber(StandardJSONProcessor.QueryResult queryResult) {
//        for (StandardJSONProcessor.EntityFilter filter : queryResult.entities) {
//            if (filter.attribute.equals("INVOICE_PART_NUMBER")) {
//                return filter.value;
//            }
//        }
//        return "";
//    }
//    
//    /**
//     * Escape JSON string
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
//     * Main method for user interface interaction (LEGACY - kept for backward compatibility)
//     * Processes user input and returns structured response with SQL queries
//     */
//    public UserActionResponse processUserInput(String userInput) {
//        try {
//            // Use the original processQuery method to get proper inputTracking
//            String jsonResponse = nlpProcessor.processQuery(userInput);
//            
//            // Parse the response to extract metadata
//            StandardJSONProcessor.QueryResult queryResult = nlpProcessor.parseJSONToObject(jsonResponse);
//            
//            // Create response object
//            UserActionResponse response = new UserActionResponse();
//            response.setOriginalInput(userInput);
//            response.setCorrectedInput(queryResult.inputTracking.correctedInput);
//            response.setQueryType(queryResult.metadata.queryType);
//            response.setActionType(queryResult.metadata.actionType);
//            response.setNlpProcessingTime(queryResult.metadata.processingTimeMs);
//            response.setFilters(queryResult.entities);
//            response.setDisplayEntities(queryResult.displayEntities);
//            response.setSuccess(true);
//            response.setMessage("Request processed successfully");
//            
//            // Generate SQL query automatically
//            response.generateSQLQuery();
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error processing user input: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Routes to appropriate action handler based on action type
//     */
//    private UserActionResponse routeToActionHandler(String actionType, List<StandardJSONProcessor.EntityFilter> filters, 
//                                                   List<String> displayEntities, String userInput) {
//        
//        switch (actionType) {
//            case "contracts_by_contractnumber":
//                return handleContractByContractNumber(filters, displayEntities, userInput);
//                
//            case "parts_by_contract_number":
//                return handlePartsByContractNumber(filters, displayEntities, userInput);
//                
//            case "parts_failed_by_contract_number":
//                return handleFailedPartsByContractNumber(filters, displayEntities, userInput);
//                
//            case "parts_by_part_number":
//                return handlePartsByPartNumber(filters, displayEntities, userInput);
//                
//            case "contracts_by_filter":
//                return handleContractsByFilter(filters, displayEntities, userInput);
//                
//            case "parts_by_filter":
//                return handlePartsByFilter(filters, displayEntities, userInput);
//                
//            case "update_contract":
//                return handleUpdateContract(filters, displayEntities, userInput);
//                
//            case "create_contract":
//                return handleCreateContract(filters, displayEntities, userInput);
//                
//            case "HELP_CONTRACT_CREATE_USER":
//                return handleHelpContractCreateUser(filters, displayEntities, userInput);
//                
//            case "HELP_CONTRACT_CREATE_BOT":
//                return handleHelpContractCreateBot(filters, displayEntities, userInput);
//                
//            default:
//                return createErrorResponse(userInput, "Unknown action type: " + actionType);
//        }
//    }
//    
//    // ============================================================================
//    // CONTRACT ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles contract queries by contract number
//     * SQL: SELECT display_entities FROM contracts WHERE AWARD_NUMBER = ?
//     */
//    private UserActionResponse handleContractByContractNumber(List<StandardJSONProcessor.EntityFilter> filters, 
//                                                             List<String> displayEntities, 
//                                                             String userInput) {
//        try {
//            // Extract contract number filter
//            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
//            if (contractNumber == null) {
//                return createErrorResponse(userInput, "Contract number not found in query");
//            }
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("contracts_by_contractnumber");
//            response.setParameters(createParameterMap("AWARD_NUMBER", contractNumber));
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Contract query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in contract query: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Handles contract queries by filter criteria
//     * SQL: SELECT display_entities FROM contracts WHERE filter_conditions
//     */
//        private UserActionResponse handleContractsByFilter(List<StandardJSONProcessor.EntityFilter> filters,
//                                                      List<String> displayEntities,
//                                                      String userInput) {
//        try {
//            // Create parameter map
//            Map<String, Object> parameters = createParameterMapFromFilters(filters);
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("contracts_by_filter");
//            response.setParameters(parameters);
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Contract filter query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in contract filter query: " + e.getMessage());
//        }
//    }
//    
//    // ============================================================================
//    // PARTS ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles parts queries by contract number
//     * SQL: SELECT display_entities FROM parts WHERE AWARD_NUMBER = ?
//     */
//        private UserActionResponse handlePartsByContractNumber(List<StandardJSONProcessor.EntityFilter> filters,
//                                                          List<String> displayEntities,
//                                                          String userInput) {
//        try {
//            // Extract contract number filter
//            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
//            if (contractNumber == null) {
//                return createErrorResponse(userInput, "Contract number not found in query");
//            }
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("parts_by_contract_number");
//            response.setParameters(createParameterMap("AWARD_NUMBER", contractNumber));
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Parts by contract query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in parts by contract query: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Handles parts queries by part number
//     * SQL: SELECT display_entities FROM parts WHERE INVOICE_PART_NUMBER = ?
//     */
//        private UserActionResponse handlePartsByPartNumber(List<StandardJSONProcessor.EntityFilter> filters,
//                                                      List<String> displayEntities,
//                                                      String userInput) {
//        try {
//            // Extract part number filter
//            String partNumber = extractFilterValue(filters, "INVOICE_PART_NUMBER");
//            if (partNumber == null) {
//                return createErrorResponse(userInput, "Part number not found in query");
//            }
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("parts_by_part_number");
//            response.setParameters(createParameterMap("INVOICE_PART_NUMBER", partNumber));
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Parts by part number query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in parts by part number query: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Handles parts queries by filter criteria
//     * SQL: SELECT display_entities FROM parts WHERE filter_conditions
//     */
//        private UserActionResponse handlePartsByFilter(List<StandardJSONProcessor.EntityFilter> filters,
//                                                  List<String> displayEntities,
//                                                  String userInput) {
//        try {
//            // Create parameter map
//            Map<String, Object> parameters = createParameterMapFromFilters(filters);
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("parts_by_filter");
//            response.setParameters(parameters);
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Parts filter query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in parts filter query: " + e.getMessage());
//        }
//    }
//    
//    // ============================================================================
//    // FAILED PARTS ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles failed parts queries by contract number
//     * SQL: SELECT display_entities FROM failed_parts WHERE LOADED_CP_NUMBER = ?
//     */
//        private UserActionResponse handleFailedPartsByContractNumber(List<StandardJSONProcessor.EntityFilter> filters,
//                                                                List<String> displayEntities,
//                                                                String userInput) {
//        try {
//            // Extract contract number filter (for failed parts, use LOADED_CP_NUMBER)
//            String contractNumber = extractFilterValue(filters, "LOADED_CP_NUMBER");
//            if (contractNumber == null) {
//                return createErrorResponse(userInput, "Contract number not found in failed parts query");
//            }
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("parts_failed_by_contract_number");
//            response.setParameters(createParameterMap("LOADED_CP_NUMBER", contractNumber));
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Failed parts query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in failed parts query: " + e.getMessage());
//        }
//    }
//    
//    // ============================================================================
//    // UPDATE ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles contract update operations
//     * SQL: UPDATE contracts SET update_fields WHERE AWARD_NUMBER = ?
//     */
//        private UserActionResponse handleUpdateContract(List<StandardJSONProcessor.EntityFilter> filters,
//                                                   List<String> displayEntities,
//                                                   String userInput) {
//        try {
//            // Extract contract number filter
//            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
//            if (contractNumber == null) {
//                return createErrorResponse(userInput, "Contract number not found in update query");
//            }
//            
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("update_contract");
//            response.setParameters(createParameterMapFromFilters(filters));
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Contract update query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in contract update query: " + e.getMessage());
//        }
//    }
//    
//    // ============================================================================
//    // CREATE ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles contract creation operations
//     * SQL: INSERT INTO contracts (fields) VALUES (values)
//     */
//        private UserActionResponse handleCreateContract(List<StandardJSONProcessor.EntityFilter> filters,
//                                                   List<String> displayEntities,
//                                                   String userInput) {
//        try {
//            // Create response
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("create_contract");
//            response.setParameters(createParameterMapFromFilters(filters));
//            response.setDisplayEntities(displayEntities);
//            response.setFilters(filters);
//            response.setSuccess(true);
//            response.setMessage("Contract creation query processed successfully");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in contract creation query: " + e.getMessage());
//        }
//    }
//    
//
//    
//    // ============================================================================
//    // UTILITY METHODS
//    // ============================================================================
//    
//    /**
//     * Extracts filter value by attribute name
//     */
//    private String extractFilterValue(List<StandardJSONProcessor.EntityFilter> filters, String attributeName) {
//        for (StandardJSONProcessor.EntityFilter filter : filters) {
//            if (filter.attribute.equals(attributeName)) {
//                return filter.value;
//            }
//        }
//        return null;
//    }
//    
//    /**
//     * Builds WHERE clause from filters
//     */
//    private String buildWhereClause(List<StandardJSONProcessor.EntityFilter> filters) {
//        if (filters == null || filters.isEmpty()) {
//            return "1=1"; // Default condition
//        }
//        
//        StringBuilder whereClause = new StringBuilder();
//        for (int i = 0; i < filters.size(); i++) {
//            StandardJSONProcessor.EntityFilter filter = filters.get(i);
//            if (i > 0) whereClause.append(" AND ");
//            whereClause.append(filter.attribute).append(" ").append(filter.operation).append(" ?");
//        }
//        return whereClause.toString();
//    }
//    
//    /**
//     * Creates parameter map from filters
//     */
//    private Map<String, Object> createParameterMapFromFilters(List<StandardJSONProcessor.EntityFilter> filters) {
//        Map<String, Object> parameters = new HashMap<>();
//        if (filters != null) {
//                    for (StandardJSONProcessor.EntityFilter filter : filters) {
//            parameters.put(filter.attribute, filter.value);
//        }
//        }
//        return parameters;
//    }
//    
//    /**
//     * Creates simple parameter map
//     */
//    private Map<String, Object> createParameterMap(String key, Object value) {
//        Map<String, Object> parameters = new HashMap<>();
//        parameters.put(key, value);
//        return parameters;
//    }
//    
//    /**
//     * Creates error response
//     */
//    private UserActionResponse createErrorResponse(String userInput, String errorMessage) {
//        UserActionResponse response = new UserActionResponse();
//        response.setSuccess(false);
//        response.setMessage(errorMessage);
//        response.setOriginalInput(userInput);
//        return response;
//    }
//    
//    // ============================================================================
//    // HELP ACTION HANDLERS
//    // ============================================================================
//    
//    /**
//     * Handles help requests for contract creation (user wants instructions)
//     * No SQL needed - returns help text
//     */
//    private UserActionResponse handleHelpContractCreateUser(List<StandardJSONProcessor.EntityFilter> filters,
//                                                           List<String> displayEntities,
//                                                           String userInput) {
//        try {
//            // Create response for help request
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("HELP_CONTRACT_CREATE_USER");
//            response.setParameters(new HashMap<>()); // No parameters needed
//            response.setDisplayEntities(displayEntities); // Should be empty for help
//            response.setFilters(filters); // Should be empty for help
//            response.setSuccess(true);
//            response.setMessage("Please follow below steps to create a contract");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in help request: " + e.getMessage());
//        }
//    }
//    
//    /**
//     * Handles help requests for contract creation (user wants system to create)
//     * No SQL needed - returns help text
//     */
//    private UserActionResponse handleHelpContractCreateBot(List<StandardJSONProcessor.EntityFilter> filters,
//                                                          List<String> displayEntities,
//                                                          String userInput) {
//        try {
//            // Create response for help request
//            UserActionResponse response = new UserActionResponse();
//            response.setActionType("HELP_CONTRACT_CREATE_BOT");
//            response.setParameters(new HashMap<>()); // No parameters needed
//            response.setDisplayEntities(displayEntities); // Should be empty for help
//            response.setFilters(filters); // Should be empty for help
//            response.setSuccess(true);
//            response.setMessage("I can help you create a contract. Please provide the necessary details.");
//            
//            return response;
//            
//        } catch (Exception e) {
//            return createErrorResponse(userInput, "Error in help request: " + e.getMessage());
//        }
//    }
//    
//    // ============================================================================
//    // MAIN METHOD FOR TESTING
//    // ============================================================================
//    
//    public static void main(String v[]){
//        UserActionHandler ob = new UserActionHandler();
//       List<String> qs= TestQueries.SPANISH_QUERIES;
//        
//        // Create file writer
//        try (java.io.FileWriter writer = new java.io.FileWriter(System.currentTimeMillis()+"_UserActionHandler_Test_Results.md")) {
//            
//            // Write header
//            writer.write("INPUT | CORRECTED_INPUT | QUERY_TYPE | ACTION_TYPE | DISPLAY_ENTITIES | FILTER_ENTITIES | SQL_QUERY\n");
//            writer.write("===============================================================================================================================================\n");
//            
//            for (String input : qs) {
//                try {
//                    UserActionResponse res = ob.processUserInput(input);
//                    
//                    // Format display entities
//                    String displayEntities = formatList(res.getDisplayEntities());
//                    
//                    // Format filter entities
//                    String filterEntities = formatFilters(res.getFilters());
//                    
//                    // Format SQL query (remove newlines and extra spaces)
//                    String sqlQuery = res.getSqlQuery() != null ? 
//                        res.getSqlQuery().replace("\n", " ").replaceAll("\\s+", " ").trim() : "";
//                    
//                    // Write to file
//                    writer.write(String.format("%s | %s | %s | %s | %s | %s | %s\n",
//                        escapePipe(res.getOriginalInput()),
//                        escapePipe(res.getCorrectedInput()),
//                        escapePipe(res.getQueryType()),
//                        escapePipe(res.getActionType()),
//                        escapePipe(displayEntities),
//                        escapePipe(filterEntities),
//                        escapePipe(sqlQuery)
//                    ));
//                    
//                } catch (Exception e) {
//                    // Write error case
//                    writer.write(String.format("%s | ERROR | ERROR | ERROR | ERROR | ERROR | ERROR\n",
//                        escapePipe(input)
//                    ));
//                }
//            }
//            
//        } catch (Exception e) {
//            // Silent error handling - no console output
//        }
//    }
//    
//    /**
//     * Format list as string
//     */
//    private static String formatList(java.util.List<String> list) {
//        if (list == null || list.isEmpty()) {
//            return "[]";
//        }
//        return "[" + String.join(", ", list) + "]";
//    }
//    
//    /**
//     * Format filters as string
//     */
//    private static String formatFilters(java.util.List<StandardJSONProcessor.EntityFilter> filters) {
//        if (filters == null || filters.isEmpty()) {
//            return "[]";
//        }
//        StringBuilder sb = new StringBuilder("[");
//        for (int i = 0; i < filters.size(); i++) {
//            StandardJSONProcessor.EntityFilter filter = filters.get(i);
//            sb.append(filter.attribute).append("=").append(filter.value);
//            if (i < filters.size() - 1) {
//                sb.append(", ");
//            }
//        }
//        sb.append("]");
//        return sb.toString();
//    }
//    
//    /**
//     * Escape pipe characters in string
//     */
//    private static String escapePipe(String input) {
//        if (input == null) {
//            return "";
//        }
//        return input.replace("|", "\\|");
//    }
//    
//    // ============================================================================
//    // NEURAL NETWORK INTEGRATION METHODS
//    // ============================================================================
//    
//    /**
//     * Pre-train the neural network with common query patterns
//     */
//    private void preTrainNeuralNetwork() {
//        // Contract queries
//        neuralClassifier.train("show contract 123456", "CONTRACT_DETAILS");
//        neuralClassifier.train("get contract details for 789012", "CONTRACT_DETAILS");
//        neuralClassifier.train("what is the contract number for ABC123", "CONTRACT_DETAILS");
//        neuralClassifier.train("contract 456789 information", "CONTRACT_DETAILS");
//        
//        // Customer-based queries
//        neuralClassifier.train("contracts for customer Siemens", "CONTRACT_BY_CUSTOMER");
//        neuralClassifier.train("show contracts by customer Honeywell", "CONTRACT_BY_CUSTOMER");
//        neuralClassifier.train("contracts for account 1234567", "CONTRACT_BY_CUSTOMER");
//        
//        // Date-based queries
//        neuralClassifier.train("contracts created in 2024", "CONTRACT_BY_DATE");
//        neuralClassifier.train("contracts after 2023", "CONTRACT_BY_DATE");
//        neuralClassifier.train("contracts before 2025", "CONTRACT_BY_DATE");
//        
//        // Status queries
//        neuralClassifier.train("active contracts", "CONTRACT_STATUS");
//        neuralClassifier.train("expired contracts", "CONTRACT_STATUS");
//        neuralClassifier.train("contract status", "CONTRACT_STATUS");
//        
//        // Part queries
//        neuralClassifier.train("show part AB123", "PART_DETAILS");
//        neuralClassifier.train("get part details for CD456", "PART_DETAILS");
//        neuralClassifier.train("part information", "PART_DETAILS");
//        
//        // Parts in contract
//        neuralClassifier.train("parts for contract 123456", "PARTS_IN_CONTRACT");
//        neuralClassifier.train("show parts in contract 789012", "PARTS_IN_CONTRACT");
//        neuralClassifier.train("parts by contract", "PARTS_IN_CONTRACT");
//        
//        // Failed parts
//        neuralClassifier.train("failed parts", "FAILED_PARTS");
//        neuralClassifier.train("show failed parts", "FAILED_PARTS");
//        neuralClassifier.train("parts with errors", "FAILED_PARTS");
//        
//        // Help queries
//        neuralClassifier.train("help me create a contract", "HELP");
//        neuralClassifier.train("how to create contract", "HELP");
//        neuralClassifier.train("steps to create contract", "HELP");
//        neuralClassifier.train("help with parts loading", "HELP");
//        
//        // General queries
//        neuralClassifier.train("what can you do", "GENERAL_QUERY");
//        neuralClassifier.train("show me everything", "GENERAL_QUERY");
//        neuralClassifier.train("general information", "GENERAL_QUERY");
//    }
//    
//    /**
//     * Enhanced query classification using neural network with fallback
//     */
//    private String classifyQueryWithNeuralNetwork(String userInput) {
//        try {
//            // Get neural network classification
//            String neuralPrediction = neuralClassifier.classify(userInput);
//            
//            // Get rule-based classification as fallback
//            QueryClassification ruleBasedResult = ruleBasedClassifier.classify(userInput);
//            
//            // Use neural prediction if confident, otherwise use rule-based
//            if (isNeuralPredictionConfident(userInput, neuralPrediction) && useNeuralNetwork) {
//                return neuralPrediction;
//            } else {
//                return ruleBasedResult.queryType;
//            }
//            
//        } catch (Exception e) {
//            // Fallback to rule-based classification
//            try {
//                QueryClassification ruleBasedResult = ruleBasedClassifier.classify(userInput);
//                return ruleBasedResult.queryType;
//            } catch (Exception ex) {
//                return "CONTRACTS"; // Default fallback
//            }
//        }
//    }
//    
//    /**
//     * Check if neural network prediction is confident
//     */
//    private boolean isNeuralPredictionConfident(String userInput, String prediction) {
//        String lowerInput = userInput.toLowerCase();
//        
//        // High confidence indicators
//        if (prediction.equals("CONTRACT_DETAILS") && 
//            (lowerInput.contains("contract") && lowerInput.matches(".*\\d{6}.*"))) {
//            return true;
//        }
//        
//        if (prediction.equals("PART_DETAILS") && 
//            (lowerInput.contains("part") && lowerInput.matches(".*[A-Z]{2}\\d{3}.*"))) {
//            return true;
//        }
//        
//        if (prediction.equals("HELP") && 
//            (lowerInput.contains("help") || lowerInput.contains("how") || lowerInput.contains("steps"))) {
//            return true;
//        }
//        
//        if (prediction.equals("FAILED_PARTS") && 
//            lowerInput.contains("failed")) {
//            return true;
//        }
//        
//        // Default to rule-based if not confident
//        return false;
//    }
//    
//    /**
//     * Train the neural network with new examples
//     */
//    public void trainWithNewExamples(Map<String, String> examples) {
//        for (Map.Entry<String, String> example : examples.entrySet()) {
//            neuralClassifier.train(example.getKey(), example.getValue());
//        }
//    }
//    
//    /**
//     * Get classification analysis for debugging
//     */
//    public Map<String, Object> getClassificationAnalysis(String userInput) {
//        Map<String, Object> analysis = new HashMap<>();
//        
//        try {
//            String neuralPrediction = neuralClassifier.classify(userInput);
//            QueryClassification ruleBasedResult = ruleBasedClassifier.classify(userInput);
//            
//            analysis.put("neuralPrediction", neuralPrediction);
//            analysis.put("ruleBasedQueryType", ruleBasedResult.queryType);
//            analysis.put("ruleBasedActionType", ruleBasedResult.actionType);
//            analysis.put("neuralConfident", isNeuralPredictionConfident(userInput, neuralPrediction));
//            analysis.put("finalQueryType", classifyQueryWithNeuralNetwork(userInput));
//            
//        } catch (Exception e) {
//            analysis.put("error", e.getMessage());
//        }
//        
//        return analysis;
//    }
//} 