package com.oracle.view.source;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Advanced query processor with sophisticated NLP capabilities
 */
public class AdvancedQueryProcessor {
    
    private final NeuralNetworkClassifier neuralClassifier;
    private final SemanticAnalyzer semanticAnalyzer;
    private final ContextManager contextManager;
    
    public AdvancedQueryProcessor() {
        this.neuralClassifier = new NeuralNetworkClassifier();
        this.semanticAnalyzer = new SemanticAnalyzer();
        this.contextManager = new ContextManager();
        
        // Pre-train the neural network with some examples
        preTrainNeuralNetwork();
    }
    
    public ContractQueryResponse processAdvancedQuery(String input) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Semantic analysis
            SemanticContext context = semanticAnalyzer.analyze(input);
            
            // Step 2: Neural network classification
            String neuralPrediction = neuralClassifier.classify(input);
            
            // Step 3: Context-aware processing
            QueryContext queryContext = contextManager.buildContext(input, context);
            
            // Step 4: Generate enhanced response
            ContractQueryResponse response = generateEnhancedResponse(input, context, queryContext, neuralPrediction);
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.getQueryMetadata().setProcessingTimeMs(processingTime);
            
            return response;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            return createErrorResponse(e.getMessage(), processingTime);
        }
    }
    
    private ContractQueryResponse generateEnhancedResponse(String input, SemanticContext semanticContext, 
                                                         QueryContext queryContext, String neuralPrediction) {
        
        QueryHeader header = new QueryHeader(); // Changed from ContractHeader to QueryHeader
        List<QueryEntity> entities = new ArrayList<>();
        List<String> displayEntities = new ArrayList<>();
        
        // Extract entities based on semantic analysis
        for (SemanticEntity entity : semanticContext.entities) {
            entities.add(new QueryEntity(entity.type, entity.operation, entity.value, "semantic_analysis"));
            
            // Update header
            switch (entity.type) {
                case "CONTRACT_NUMBER":
                    header.setContractNumber(entity.value);
                    break;
                case "CUSTOMER_NUMBER":
                    header.setCustomerNumber(entity.value);
                    break;
                case "CUSTOMER_NAME":
                    header.setCustomerName(entity.value);
                    break;
                case "PART_NUMBER":
                    header.setPartNumber(entity.value);
                    break;
                case "CREATED_BY":
                    header.setCreatedBy(entity.value);
                    break;
            }
        }
        
        // Determine query type and action based on neural prediction and context
        String queryType = mapNeuralPredictionToQueryType(neuralPrediction);
        String actionType = determineActionType(neuralPrediction, queryContext, entities);
        
        // Determine display entities based on intent
        displayEntities = determineDisplayEntitiesAdvanced(queryContext, entities);
        
        QueryMetadata metadata = new QueryMetadata(queryType, actionType, 0);
        
        return new ContractQueryResponse(
            input,                    // originalInput
            input,                    // correctedInput (same as input since no correction applied here)
            header, 
            metadata, 
            entities, 
            displayEntities, 
            new ArrayList<>()
        );
    }
    
    private String mapNeuralPredictionToQueryType(String neuralPrediction) {
        switch (neuralPrediction) {
            case "CONTRACT_DETAILS":
            case "CONTRACT_BY_DATE":
            case "CONTRACT_BY_CUSTOMER":
            case "CONTRACT_STATUS":
                return "CONTRACTS";
            case "PART_DETAILS":
            case "PART_STATUS":
            case "PARTS_IN_CONTRACT":
                return "PARTS";
            case "FAILED_PARTS":
                return "FAILED_PARTS";
            case "HELP":
                return "HELP";
            default:
                return "GENERAL_QUERY";
        }
    }
    
    private String determineActionType(String neuralPrediction, QueryContext queryContext, List<QueryEntity> entities) {
        String baseAction = neuralPrediction.toLowerCase();
        
        // FIX: Override neural prediction for contract-specific queries
        if (hasContractNumber(entities)) {
            baseAction = "contract_by_contractNumber";
        }
        // Add other specific overrides as needed
        else if (hasCustomerNumber(entities)) {
            baseAction = "contracts_by_customer";
        }
        else if (hasCustomerName(entities)) {
            baseAction = "contracts_by_customerName";
        }
        
        // Apply modifiers
        if (queryContext.hasMetadataRequest) {
            return baseAction + "_metadata";
        } else if (queryContext.hasDetailRequest) {
            return baseAction + "_detailed";
        } else if (queryContext.hasSummaryRequest) {
            return baseAction + "_summary";
        }
        
        return baseAction;
    }
    
    // Helper methods to check for specific entity types
    private boolean hasContractNumber(List<QueryEntity> entities) {
        return entities != null && entities.stream()
            .anyMatch(e -> "AWARD_NUMBER".equals(e.getAttribute()) || "CONTRACT_NUMBER".equals(e.getAttribute()));
    }

    private boolean hasCustomerNumber(List<QueryEntity> entities) {
        return entities != null && entities.stream()
            .anyMatch(e -> "CUSTOMER_NUMBER".equals(e.getAttribute()));
    }

    private boolean hasCustomerName(List<QueryEntity> entities) {
        return entities != null && entities.stream()
            .anyMatch(e -> "CUSTOMER_NAME".equals(e.getAttribute()));
    }
    
    private List<String> determineDisplayEntitiesAdvanced(QueryContext queryContext, List<QueryEntity> entities) {
        Set<String> displayFields = new LinkedHashSet<>();
        
        // Always include primary identifiers
        displayFields.add("CONTRACT_NUMBER");
        
        // Add fields based on query context
        if (queryContext.hasCustomerFocus) {
            displayFields.addAll(Arrays.asList("CUSTOMER_NAME", "CUSTOMER_NUMBER"));
        }
        
        if (queryContext.hasDateFocus) {
            displayFields.addAll(Arrays.asList("CREATED_DATE", "EFFECTIVE_DATE", "EXPIRY_DATE"));
        }
        
        if (queryContext.hasPartFocus) {
            displayFields.addAll(Arrays.asList("PART_NUMBER", "PART_STATUS"));
        }
        
        if (queryContext.hasStatusFocus) {
            displayFields.addAll(Arrays.asList("STATUS", "EFFECTIVE_DATE"));
        }
        
        if (queryContext.hasMetadataRequest) {
            displayFields.addAll(Arrays.asList(
                "CONTRACT_NUMBER", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "PART_NUMBER",
                "CREATED_DATE", "EFFECTIVE_DATE", "EXPIRY_DATE", "STATUS", "PROJECT_TYPE",
                "PRICE_LIST", "CREATED_BY"
            ));
        }
        
        // Add explicitly requested fields
        displayFields.addAll(queryContext.explicitFields);
        
        return new ArrayList<>(displayFields);
    }
    
    private void preTrainNeuralNetwork() {
        // Training examples for neural network
        String[][] trainingData = {
            {"show contract 123456", "CONTRACT_DETAILS"},
            {"contracts created in 2024", "CONTRACT_BY_DATE"},
            {"contracts for customer 897654", "CONTRACT_BY_CUSTOMER"},
            {"get contract status", "CONTRACT_STATUS"},
            {"show part AE125", "PART_DETAILS"},
            {"is part active", "PART_STATUS"},
            {"parts in contract 123456", "PARTS_IN_CONTRACT"},
            {"failed parts report", "FAILED_PARTS"},
            {"help me create contract", "HELP"},
            {"what can you do", "HELP"}
        };
        
        for (String[] example : trainingData) {
            neuralClassifier.train(example[0], example[1]);
        }
    }
    
    private ContractQueryResponse createErrorResponse(String errorMessage, long processingTime) {
        QueryHeader header = new QueryHeader(); // Changed from ContractHeader to QueryHeader
        QueryMetadata metadata = new QueryMetadata("ERROR", "error_handling", processingTime);
        List<QueryEntity> entities = new ArrayList<>();
        List<String> displayEntities = new ArrayList<>();
        List<String> errors = Arrays.asList(errorMessage);
        
        return new ContractQueryResponse(
            "",                       // originalInput (empty since not provided)
            "",                       // correctedInput (empty since not provided)
            header, 
            metadata, 
            entities, 
            displayEntities, 
            errors
        );
    }
}