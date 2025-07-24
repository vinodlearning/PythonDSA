package com.oracle.view.source;

import java.util.List;

public class EnhancedContractQueryParser extends ContractQueryParser {
    
    private final MachineLearningEnhancer mlEnhancer;
    private final EntityExtractor entityExtractor;
    
    public EnhancedContractQueryParser() {
        super();
        this.mlEnhancer = new MachineLearningEnhancer();
        this.entityExtractor = new EntityExtractor();
    }
    
    @Override
    public ContractQueryResponse parseQuery(String userInput) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Use StandardJSONProcessor logic through EntityExtractor
            ContractQueryResponse businessRulesResponse = entityExtractor.extractWithBusinessRules(userInput);
            
            // Step 2: Apply ML enhancements
            ContractQueryResponse enhancedResponse = mlEnhancer.enhance(userInput, businessRulesResponse);
            
            // Step 3: Final validation and corrections
            ContractQueryResponse finalResponse = applyFinalCorrections(userInput, enhancedResponse);
            
            long processingTime = System.currentTimeMillis() - startTime;
            finalResponse.getQueryMetadata().setProcessingTimeMs(processingTime);
            
            return finalResponse;
            
        } catch (Exception e) {
            System.err.println("Error in enhanced parsing: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to base parser
            return super.parseQuery(userInput);
        }
    }
 private ContractQueryResponse applyFinalCorrections(String input, ContractQueryResponse response) {
        // Final correction 1: Ensure action type matches entities
        String correctedActionType = determineActionTypeFromEntities(input, response.getFilters());
        response.getQueryMetadata().setActionType(correctedActionType);
        
        // Final correction 2: Populate header from filters
        entityExtractor.populateHeaderFromFilters(response);
        
        // Final correction 3: Validate display entities match query intent
        List<String> validatedDisplayEntities = validateDisplayEntities(input, response.getDisplayEntities(), response.getFilters());
        
        // Final correction 4: Add missing essential fields
        validatedDisplayEntities = addEssentialFields(validatedDisplayEntities, response.getFilters());
        
        return new ContractQueryResponse(
            response.getOriginalInput(),
            response.getCorrectedInput(),
            response.getHeader(),
            response.getQueryMetadata(),
            response.getFilters(),
            validatedDisplayEntities,
            response.getErrors()
        );
    }
    
    private String determineActionTypeFromEntities(String input, List<QueryEntity> entities) {
        String lowerInput = input.toLowerCase();
        
        // Check for specific entity types
        boolean hasContractNumber = entities.stream()
            .anyMatch(e -> "AWARD_NUMBER".equals(e.getAttribute()) || "CONTRACT_NUMBER".equals(e.getAttribute()));
        boolean hasCustomerNumber = entities.stream()
            .anyMatch(e -> "CUSTOMER_NUMBER".equals(e.getAttribute()));
        boolean hasCustomerName = entities.stream()
            .anyMatch(e -> "CUSTOMER_NAME".equals(e.getAttribute()));
        boolean hasPartNumber = entities.stream()
            .anyMatch(e -> "PART_NUMBER".equals(e.getAttribute()));
        boolean hasDateFilter = entities.stream()
            .anyMatch(e -> "CREATE_DATE".equals(e.getAttribute()) || "EFFECTIVE_DATE".equals(e.getAttribute()));
        boolean hasCreatedBy = entities.stream()
            .anyMatch(e -> "CREATED_BY".equals(e.getAttribute()));
        boolean hasStatusFilter = entities.stream()
            .anyMatch(e -> "STATUS".equals(e.getAttribute()));
        
        // Determine base action based on extracted entities
        String baseAction;
        if (hasContractNumber) {
            baseAction = "contract_by_contractNumber";
        } else if (hasPartNumber) {
            if (lowerInput.contains("failed")) {
                baseAction = "failed_parts_report";
            } else {
                baseAction = "parts_by_partNumber";
            }
        } else if (hasCustomerNumber) {
            baseAction = "contracts_by_customer";
        } else if (hasCustomerName) {
            baseAction = "contracts_by_customerName";
        } else if (hasDateFilter) {
            baseAction = "contracts_by_date";
        } else if (hasCreatedBy) {
            baseAction = "contracts_by_createdBy";
        } else if (hasStatusFilter) {
            baseAction = "contracts_by_status";
        } else {
            baseAction = "show_specific_fields";
        }
        
        // Apply modifiers based on input text analysis
        if (lowerInput.contains("get all metadata") || 
            lowerInput.contains("all metadata") ||
            lowerInput.contains("metadata")) {
            return baseAction + "_metadata";
        } else if (lowerInput.contains("detail") || 
                   lowerInput.contains("detailed")) {
            return baseAction + "_detailed";
        } else if (lowerInput.contains("summary")) {
            return baseAction + "_summary";
        } else if (lowerInput.contains("failed") && lowerInput.contains("part")) {
            return "failed_parts_report";
        }
        
        return baseAction;
    }
    
    private List<String> validateDisplayEntities(String input, List<String> displayEntities, List<QueryEntity> filters) {
        String lowerInput = input.toLowerCase();
        
        // If user asks for specific fields, prioritize those
        if (lowerInput.contains("show only") || lowerInput.contains("just show")) {
            return extractSpecificFields(input, displayEntities);
        }
        
        // Validate that display entities make sense for the query
        if (lowerInput.contains("customer") && !displayEntities.contains("CUSTOMER_NAME")) {
            displayEntities.add("CUSTOMER_NAME");
        }
        
        if (lowerInput.contains("date") && !displayEntities.contains("CREATE_DATE")) {
            displayEntities.add("CREATE_DATE");
        }
        
        if (lowerInput.contains("status") && !displayEntities.contains("STATUS")) {
            displayEntities.add("STATUS");
        }
        
        if (lowerInput.contains("part") && !displayEntities.contains("PART_NUMBER")) {
            displayEntities.add("PART_NUMBER");
        }
        
        return displayEntities;
    }
    
    private List<String> extractSpecificFields(String input, List<String> currentFields) {
        // Extract specific field requests from input
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("contract number only")) {
            return java.util.Arrays.asList("CONTRACT_NUMBER");
        }
        
        if (lowerInput.contains("customer name only")) {
            return java.util.Arrays.asList("CUSTOMER_NAME");
        }
        
        if (lowerInput.contains("status only")) {
            return java.util.Arrays.asList("STATUS");
        }
        
        // Default to current fields if no specific request
        return currentFields;
    }
    
    private List<String> addEssentialFields(List<String> displayEntities, List<QueryEntity> filters) {
        java.util.Set<String> essentialFields = new java.util.LinkedHashSet<>(displayEntities);
        
        // Always include primary identifier
        essentialFields.add("CONTRACT_NUMBER");
        
        // Add related fields based on filters
        boolean hasCustomerFilter = filters.stream()
            .anyMatch(f -> "CUSTOMER_NUMBER".equals(f.getAttribute()) || "CUSTOMER_NAME".equals(f.getAttribute()));
        
        if (hasCustomerFilter) {
            essentialFields.add("CUSTOMER_NAME");
        }
        
        boolean hasPartFilter = filters.stream()
            .anyMatch(f -> "PART_NUMBER".equals(f.getAttribute()));
        
        if (hasPartFilter) {
            essentialFields.add("PART_NUMBER");
        }
        
        boolean hasDateFilter = filters.stream()
            .anyMatch(f -> "CREATE_DATE".equals(f.getAttribute()) || "EFFECTIVE_DATE".equals(f.getAttribute()));
        
        if (hasDateFilter) {
            essentialFields.add("CREATE_DATE");
        }
        
        return new java.util.ArrayList<>(essentialFields);
    }
    
    /**
     * Provide feedback to improve future parsing
     */
    public void provideFeedback(String input, ContractQueryResponse response, double userRating) {
        // Create expected response based on user feedback
        ContractQueryResponse expectedResponse = createExpectedResponse(input, response, userRating);
        
        // Add training example to ML enhancer
        mlEnhancer.addTrainingExample(input, expectedResponse, response, userRating);
        
        // Log feedback for analysis
        System.out.println("Feedback received for input: '" + input + "' - Rating: " + userRating);
    }
    
    private ContractQueryResponse createExpectedResponse(String input, ContractQueryResponse actualResponse, double userRating) {
        // If rating is high (>0.8), use actual response as expected
        if (userRating > 0.8) {
            return actualResponse;
        }
        
        // If rating is low, try to infer what the expected response should be
        // This is a simplified approach - in a real system, you'd get explicit user corrections
        ContractQueryResponse expected = new ContractQueryResponse(
            actualResponse.getOriginalInput(),
            actualResponse.getCorrectedInput(),
            actualResponse.getHeader(),
            actualResponse.getQueryMetadata(),
            actualResponse.getFilters(),
            actualResponse.getDisplayEntities(),
            actualResponse.getErrors()
        );
        
        // Apply corrections based on common issues
        if (userRating < 0.3) {
            // Very low rating - likely wrong query type or missing entities
            expected = applyLowRatingCorrections(input, expected);
        } else if (userRating < 0.6) {
            // Medium rating - likely wrong display fields or action type
            expected = applyMediumRatingCorrections(input, expected);
        }
        
        return expected;
    }
    
    private ContractQueryResponse applyLowRatingCorrections(String input, ContractQueryResponse response) {
        String lowerInput = input.toLowerCase();
        
        // Correct query type if obviously wrong
        if (lowerInput.contains("part") && !"PARTS".equals(response.getQueryMetadata().getQueryType())) {
            response.getQueryMetadata().setQueryType("PARTS");
            response.getQueryMetadata().setActionType("parts_query");
        }
        
        if (lowerInput.contains("contract") && !"CONTRACTS".equals(response.getQueryMetadata().getQueryType())) {
            response.getQueryMetadata().setQueryType("CONTRACTS");
            response.getQueryMetadata().setActionType("contract_details");
        }
        
        return response;
    }
    
    private ContractQueryResponse applyMediumRatingCorrections(String input, ContractQueryResponse response) {
        String lowerInput = input.toLowerCase();
        
        // Adjust display fields based on likely user intent
        java.util.List<String> correctedDisplayFields = new java.util.ArrayList<>(response.getDisplayEntities());
        
        if (lowerInput.contains("summary") && correctedDisplayFields.size() > 4) {
            // Reduce fields for summary
            correctedDisplayFields = java.util.Arrays.asList("CONTRACT_NUMBER", "CUSTOMER_NAME", "STATUS", "CREATE_DATE");
        }
        
        if (lowerInput.contains("detail") && correctedDisplayFields.size() < 6) {
            // Add more fields for detailed view
            correctedDisplayFields.addAll(java.util.Arrays.asList(
                "EFFECTIVE_DATE", "EXPIRY_DATE", "PROJECT_TYPE", "CREATED_BY"
            ));
        }
        
        return new ContractQueryResponse(
            response.getOriginalInput(),
            response.getCorrectedInput(),
            response.getHeader(),
            response.getQueryMetadata(),
            response.getFilters(),
            correctedDisplayFields,
            response.getErrors()
        );
    }
    
    /**
     * Get processing statistics
     */
    public ProcessingStats getProcessingStats() {
        return new ProcessingStats(
            mlEnhancer.getTrainingData().size(),
            mlEnhancer.getFeatureWeights(),
            calculateAverageProcessingTime()
        );
    }
    
    private double calculateAverageProcessingTime() {
        // This would be calculated from actual processing times
        // For now, return a placeholder
        return 150.0; // milliseconds
    }
    
    /**
     * Processing statistics class
     */
    public static class ProcessingStats {
        public final int trainingExamples;
        public final java.util.Map<String, Double> featureWeights;
        public final double averageProcessingTime;
        
        public ProcessingStats(int trainingExamples, java.util.Map<String, Double> featureWeights, double averageProcessingTime) {
            this.trainingExamples = trainingExamples;
            this.featureWeights = featureWeights;
            this.averageProcessingTime = averageProcessingTime;
        }
        
        @Override
        public String toString() {
            return String.format("ProcessingStats{trainingExamples=%d, avgProcessingTime=%.2fms, featureWeights=%s}", 
                trainingExamples, averageProcessingTime, featureWeights);
        }
    }
}