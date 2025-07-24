package com.oracle.view.source;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Enhanced Machine Learning layer with StandardJSONProcessor logic
 */
public class MachineLearningEnhancer {

    public List<TrainingExample> getTrainingData() {
        return trainingData;
    }

    public Map<String, Double> getFeatureWeights() {
        return featureWeights;
    }

    private final Map<String, Double> featureWeights;
    private final List<TrainingExample> trainingData;
    private final ConfidenceCalculator confidenceCalculator;
    private final EntityExtractor entityExtractor;
    
    // Business rules from StandardJSONProcessor
    private static final Map<String, String> INTENT_MAPPING = new HashMap<String, String>() {{
        put("show", "display");
        put("get", "retrieve");
        put("find", "search");
        put("list", "display");
        put("display", "display");
        put("retrieve", "retrieve");
        put("search", "search");
        put("query", "search");
        put("lookup", "search");
        put("fetch", "retrieve");
    }};
    
    private static final Map<String, List<String>> FIELD_GROUPS = new HashMap<String, List<String>>() {{
        put("summary", Arrays.asList("CONTRACT_NUMBER", "CUSTOMER_NAME", "STATUS", "CREATE_DATE"));
        put("details", Arrays.asList("CONTRACT_NUMBER", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRY_DATE", "STATUS", "PROJECT_TYPE"));
        put("metadata", Arrays.asList("CONTRACT_NUMBER", "CUSTOMER_NAME", "CREATE_DATE", "EFFECTIVE_DATE", "STATUS", "PROJECT_TYPE", "PRICE_LIST", "CREATED_BY"));
        put("customer", Arrays.asList("CUSTOMER_NAME", "CUSTOMER_NUMBER", "CONTRACT_NUMBER"));
        put("financial", Arrays.asList("CONTRACT_NUMBER", "PRICE_LIST", "EFFECTIVE_DATE", "CUSTOMER_NAME"));
        put("status", Arrays.asList("CONTRACT_NUMBER", "STATUS", "EFFECTIVE_DATE", "EXPIRY_DATE"));
        put("parts", Arrays.asList("PART_NUMBER", "PART_STATUS", "CONTRACT_NUMBER"));
    }};
    
    public MachineLearningEnhancer() {
        this.featureWeights = initializeFeatureWeights();
        this.trainingData = new ArrayList<>();
        this.confidenceCalculator = new ConfidenceCalculator();
        this.entityExtractor = new EntityExtractor();
    }
    
    /**
     * Enhanced processing with StandardJSONProcessor logic
     */
    public ContractQueryResponse enhance(String originalInput, ContractQueryResponse baseResponse) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Apply spell correction
            String correctedInput = entityExtractor.correctSpelling(originalInput);
            
            // Step 2: Enhanced tokenization
            List<String> tokens = entityExtractor.enhancedTokenize(correctedInput);
            
            // Step 3: Business rules extraction
            ContractQueryResponse businessRulesResponse = entityExtractor.extractWithBusinessRules(correctedInput);
            
            // Step 4: Apply ML enhancements
            ContractQueryResponse enhanced = applyMLEnhancements(correctedInput, tokens, businessRulesResponse);
            
            // Step 5: Calculate confidence
            double confidence = confidenceCalculator.calculateConfidence(correctedInput, enhanced);
            
            // Step 6: Final validation and cleanup
            enhanced = validateAndCleanup(enhanced);
            
            long processingTime = System.currentTimeMillis() - startTime;
            enhanced.getQueryMetadata().setProcessingTimeMs(processingTime);
            
            return enhanced;
            
        } catch (Exception e) {
            System.err.println("Error in ML enhancement: " + e.getMessage());
            e.printStackTrace();
            return baseResponse; // Fallback to base response
        }
    }
    
    private ContractQueryResponse applyMLEnhancements(String input, List<String> tokens, ContractQueryResponse baseResponse) {
        // Enhanced entity extraction
        List<QueryEntity> enhancedEntities = enhanceEntitiesWithML(input, tokens, baseResponse.getFilters());
        
        // Enhanced display field selection
        List<String> enhancedDisplayFields = enhanceDisplayFieldsWithML(input, tokens, baseResponse.getDisplayEntities());
        
        // Enhanced query metadata
        QueryMetadata enhancedMetadata = enhanceQueryMetadataWithML(input, tokens, baseResponse.getQueryMetadata());
        
        // Enhanced header information
        QueryHeader enhancedHeader = enhanceHeaderWithML(input, enhancedEntities, baseResponse.getHeader());
        
        return new ContractQueryResponse(
            baseResponse.getOriginalInput(),
            input, // corrected input
            enhancedHeader,
            enhancedMetadata,
            enhancedEntities,
            enhancedDisplayFields,
            baseResponse.getErrors()
        );
    }
    
    private List<QueryEntity> enhanceEntitiesWithML(String input, List<String> tokens, List<QueryEntity> baseEntities) {
        List<QueryEntity> enhanced = new ArrayList<>(baseEntities);
        String lowerInput = input.toLowerCase();
        
        // ML Enhancement 1: Contextual date extraction
        if (lowerInput.contains("last month") || lowerInput.contains("previous month")) {
            enhanced.add(new QueryEntity("CREATE_DATE", ">=", "LAST_MONTH", "ml_enhancement"));
        }
        
        if (lowerInput.contains("this year") || lowerInput.contains("current year")) {
            enhanced.add(new QueryEntity("CREATE_DATE", ">=", "THIS_YEAR", "ml_enhancement"));
        }
        
        if (lowerInput.contains("last year") || lowerInput.contains("previous year")) {
            enhanced.add(new QueryEntity("CREATE_DATE", "=", "LAST_YEAR", "ml_enhancement"));
        }
        
        // ML Enhancement 2: Status inference
        if (lowerInput.contains("expired") || lowerInput.contains("inactive")) {
            enhanced.add(new QueryEntity("STATUS", "=", "EXPIRED", "ml_enhancement"));
        }
        
        if (lowerInput.contains("active") || lowerInput.contains("current")) {
            enhanced.add(new QueryEntity("STATUS", "=", "ACTIVE", "ml_enhancement"));
        }
        
        if (lowerInput.contains("failed") || lowerInput.contains("error")) {
            enhanced.add(new QueryEntity("STATUS", "=", "FAILED", "ml_enhancement"));
        }
        
        // ML Enhancement 3: Range detection
        if (lowerInput.matches(".*between.*\\d{4}.*and.*\\d{4}.*")) {
            String dateRange = extractDateRange(lowerInput);
            if (!dateRange.equals("unknown_range")) {
                enhanced.add(new QueryEntity("CREATE_DATE", "BETWEEN", dateRange, "ml_enhancement"));
            }
        }
        
        // ML Enhancement 4: Implicit filters
        if (lowerInput.contains("recent") || lowerInput.contains("latest")) {
            enhanced.add(new QueryEntity("CREATE_DATE", ">=", "LAST_30_DAYS", "ml_enhancement"));
        }
        
        if (lowerInput.contains("old") || lowerInput.contains("older")) {
            enhanced.add(new QueryEntity("CREATE_DATE", "<=", "LAST_YEAR", "ml_enhancement"));
        }
        
        return removeDuplicateEntities(enhanced);
    }
    
    private List<String> enhanceDisplayFieldsWithML(String input, List<String> tokens, List<String> baseFields) {
        Set<String> enhanced = new LinkedHashSet<>(baseFields);
        String lowerInput = input.toLowerCase();
        
        // ML Enhancement: Intent-based field selection
        for (Map.Entry<String, List<String>> entry : FIELD_GROUPS.entrySet()) {
            if (lowerInput.contains(entry.getKey())) {
                enhanced.addAll(entry.getValue());
                break; // Use first match to avoid over-inclusion
            }
        }
        
        // ML Enhancement: Context-aware field addition
        if (lowerInput.contains("price") || lowerInput.contains("cost") || lowerInput.contains("financial")) {
            enhanced.addAll(FIELD_GROUPS.get("financial"));
        }
        
        if (lowerInput.contains("when") || lowerInput.contains("date") || lowerInput.contains("time")) {
            enhanced.addAll(Arrays.asList("CREATE_DATE", "EFFECTIVE_DATE", "EXPIRY_DATE"));
        }
        
        if (lowerInput.contains("who") || lowerInput.contains("created by") || lowerInput.contains("author")) {
            enhanced.add("CREATED_BY");
        }
        
        // Ensure minimum required fields
        enhanced.add("CONTRACT_NUMBER");
        
        return new ArrayList<>(enhanced);
    }
    
    private QueryMetadata enhanceQueryMetadataWithML(String input, List<String> tokens, QueryMetadata baseMetadata) {
        String enhancedQueryType = baseMetadata.getQueryType();
        String enhancedActionType = baseMetadata.getActionType();
        String lowerInput = input.toLowerCase();
        
        // ML Enhancement: Query type refinement
        if (lowerInput.contains("part") && !enhancedQueryType.equals("PARTS")) {
            enhancedQueryType = "PARTS";
            enhancedActionType = "parts_query";
        }
        
        if (lowerInput.contains("failed") && lowerInput.contains("part")) {
            enhancedQueryType = "FAILED_PARTS";
            enhancedActionType = "failed_parts_report";
        }
        
        // ML Enhancement: Action type refinement
        if (lowerInput.contains("metadata") || lowerInput.contains("all information")) {
            enhancedActionType = enhancedActionType.contains("metadata") ? 
                enhancedActionType : enhancedActionType + "_metadata";
        }
         if (lowerInput.contains("summary") && !enhancedActionType.contains("summary")) {
            enhancedActionType = enhancedActionType + "_summary";
        }
        
        if (lowerInput.contains("detail") && !enhancedActionType.contains("detailed")) {
            enhancedActionType = enhancedActionType + "_detailed";
        }
        
        // ML Enhancement: Intent mapping
        for (Map.Entry<String, String> entry : INTENT_MAPPING.entrySet()) {
            if (lowerInput.contains(entry.getKey())) {
                enhancedActionType = entry.getValue() + "_" + enhancedActionType.replaceFirst("^\\w+_", "");
                break;
            }
        }
        
        return new QueryMetadata(enhancedQueryType, enhancedActionType, baseMetadata.getProcessingTimeMs());
    }
    
    private QueryHeader enhanceHeaderWithML(String input, List<QueryEntity> entities, QueryHeader baseHeader) {
        QueryHeader enhanced = new QueryHeader();
        
        // Copy base header values
        enhanced.setContractNumber(baseHeader.getContractNumber());
        enhanced.setCustomerNumber(baseHeader.getCustomerNumber());
        enhanced.setCustomerName(baseHeader.getCustomerName());
        enhanced.setPartNumber(baseHeader.getPartNumber());
        enhanced.setCreatedBy(baseHeader.getCreatedBy());
        
        // ML Enhancement: Fill missing header fields from entities
        for (QueryEntity entity : entities) {
            switch (entity.getAttribute()) {
                case "AWARD_NUMBER":
                case "CONTRACT_NUMBER":
                    if (enhanced.getContractNumber() == null) {
                        enhanced.setContractNumber(entity.getValue());
                    }
                    break;
                case "CUSTOMER_NUMBER":
                    if (enhanced.getCustomerNumber() == null) {
                        enhanced.setCustomerNumber(entity.getValue());
                    }
                    break;
                case "CUSTOMER_NAME":
                    if (enhanced.getCustomerName() == null) {
                        enhanced.setCustomerName(entity.getValue());
                    }
                    break;
                case "PART_NUMBER":
                    if (enhanced.getPartNumber() == null) {
                        enhanced.setPartNumber(entity.getValue());
                    }
                    break;
                case "CREATED_BY":
                    if (enhanced.getCreatedBy() == null) {
                        enhanced.setCreatedBy(entity.getValue());
                    }
                    break;
            }
        }
        
        return enhanced;
    }
    
    private ContractQueryResponse validateAndCleanup(ContractQueryResponse response) {
        List<String> errors = new ArrayList<>(response.getErrors());
        
        // Validation 1: Check for conflicting entities
        List<QueryEntity> contractEntities = response.getFilters().stream()
            .filter(e -> "AWARD_NUMBER".equals(e.getAttribute()) || "CONTRACT_NUMBER".equals(e.getAttribute()))
            .collect(Collectors.toList());
        
        if (contractEntities.size() > 1) {
            errors.add("Multiple contract numbers detected - please specify one contract");
        }
        
        // Validation 2: Check for empty results
        if (response.getFilters().isEmpty() && response.getDisplayEntities().isEmpty()) {
            errors.add("No valid query parameters found - please provide more specific information");
        }
        
        // Validation 3: Check query consistency
        String queryType = response.getQueryMetadata().getQueryType();
        boolean hasRelevantFilters = false;
        
        if ("CONTRACTS".equals(queryType)) {
            hasRelevantFilters = response.getFilters().stream()
                .anyMatch(e -> Arrays.asList("AWARD_NUMBER", "CONTRACT_NUMBER", "CUSTOMER_NUMBER", "CUSTOMER_NAME").contains(e.getAttribute()));
        } else if ("PARTS".equals(queryType)) {
            hasRelevantFilters = response.getFilters().stream()
                .anyMatch(e -> "PART_NUMBER".equals(e.getAttribute()));
        }
        
        if (!hasRelevantFilters && !"GENERAL".equals(queryType)) {
            errors.add("Query type '" + queryType + "' detected but no relevant filters found");
        }
        
        // Cleanup: Remove duplicate display entities
        List<String> cleanedDisplayEntities = response.getDisplayEntities().stream()
            .distinct()
            .collect(Collectors.toList());
        
        return new ContractQueryResponse(
            response.getOriginalInput(),
            response.getCorrectedInput(),
            response.getHeader(),
            response.getQueryMetadata(),
            response.getFilters(),
            cleanedDisplayEntities,
            errors
        );
    }
    
    private String extractDateRange(String input) {
        Pattern dateRangePattern = Pattern.compile("between\\s+(\\d{4})\\s+and\\s+(\\d{4})");
        Matcher matcher = dateRangePattern.matcher(input.toLowerCase());
        
        if (matcher.find()) {
            String startYear = matcher.group(1);
            String endYear = matcher.group(2);
            return startYear + " TO " + endYear;
        }
        
        // Try month-year pattern
        Pattern monthYearPattern = Pattern.compile("between\\s+(\\w+)\\s+(\\d{4})\\s+and\\s+(\\w+)\\s+(\\d{4})");
        Matcher monthMatcher = monthYearPattern.matcher(input.toLowerCase());
        
        if (monthMatcher.find()) {
            String startMonth = monthMatcher.group(1);
            String startYear = monthMatcher.group(2);
            String endMonth = monthMatcher.group(3);
            String endYear = monthMatcher.group(4);
            return startMonth + "_" + startYear + " TO " + endMonth + "_" + endYear;
        }
        
        return "unknown_range";
    }
    
    private List<QueryEntity> removeDuplicateEntities(List<QueryEntity> entities) {
        Map<String, QueryEntity> uniqueEntities = new LinkedHashMap<>();
        
        for (QueryEntity entity : entities) {
            String key = entity.getAttribute() + "_" + entity.getOperation() + "_" + entity.getValue();
            if (!uniqueEntities.containsKey(key)) {
                uniqueEntities.put(key, entity);
            } else {
                // Keep the one with higher priority source
                QueryEntity existing = uniqueEntities.get(key);
                if (getSourcePriority(entity.getSource()) > getSourcePriority(existing.getSource())) {
                    uniqueEntities.put(key, entity);
                }
            }
        }
        
        return new ArrayList<>(uniqueEntities.values());
    }
    
    private int getSourcePriority(String source) {
        switch (source) {
            case "business_rule": return 4;
            case "pattern": return 3;
            case "contextual": return 2;
            case "ml_enhancement": return 2;
            case "pos_analysis": return 1;
            case "ner_analysis": return 1;
            default: return 0;
        }
    }
    
    private Map<String, Double> initializeFeatureWeights() {
        Map<String, Double> weights = new HashMap<>();
        
        // Enhanced feature weights based on StandardJSONProcessor patterns
        weights.put("contract_number_present", 0.95);
        weights.put("customer_info_present", 0.85);
        weights.put("part_number_present", 0.90);
        weights.put("date_range_present", 0.75);
        weights.put("status_keyword_present", 0.70);
        weights.put("action_verb_present", 0.60);
        weights.put("spelling_errors_corrected", 0.40);
        weights.put("business_rule_match", 0.90);
        weights.put("intent_clarity", 0.80);
        weights.put("entity_consistency", 0.85);
        
        return weights;
    }
    
    /**
     * Add training example for continuous learning
     */
    public void addTrainingExample(String input, ContractQueryResponse expectedOutput,
                                  ContractQueryResponse actualOutput, double userFeedback) {
        trainingData.add(new TrainingExample(input, expectedOutput, actualOutput, userFeedback));
        
        // Retrain model if we have enough examples
        if (trainingData.size() % 50 == 0) { // More frequent retraining
            retrainModel();
        }
    }
    
    private void retrainModel() {
        System.out.println("Retraining ML model with " + trainingData.size() + " examples");
        
        // Enhanced online learning
        for (TrainingExample example : trainingData.subList(Math.max(0, trainingData.size() - 50), trainingData.size())) {
            adjustWeights(example);
        }
        
        // Normalize weights to prevent drift
        normalizeWeights();
    }
    
    private void adjustWeights(TrainingExample example) {
        double learningRate = 0.005; // Reduced learning rate for stability
        double error = example.userFeedback - 0.5; // Normalize feedback
        
        // Adjust weights based on features present in the input
        if (containsContractNumber(example.input)) {
            adjustWeight("contract_number_present", error, learningRate);
        }
        
        if (containsCustomerInfo(example.input)) {
            adjustWeight("customer_info_present", error, learningRate);
        }
        
        if (containsPartNumber(example.input)) {
            adjustWeight("part_number_present", error, learningRate);
        }
        
        if (containsDateInfo(example.input)) {
            adjustWeight("date_range_present", error, learningRate);
        }
        
        if (containsStatusKeywords(example.input)) {
            adjustWeight("status_keyword_present", error, learningRate);
        }
        
        if (containsActionVerbs(example.input)) {
            adjustWeight("action_verb_present", error, learningRate);
        }
        
        // Check if spelling was corrected
        String corrected = entityExtractor.correctSpelling(example.input);
        if (!corrected.equals(example.input)) {
            adjustWeight("spelling_errors_corrected", error, learningRate);
        }
    }
    
    private void adjustWeight(String feature, double error, double learningRate) {
        double currentWeight = featureWeights.get(feature);
        double newWeight = currentWeight + learningRate * error;
        
        // Clamp weights between 0 and 1
        newWeight = Math.max(0.0, Math.min(1.0, newWeight));
        featureWeights.put(feature, newWeight);
    }
    
    private void normalizeWeights() {
        double sum = featureWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            featureWeights.replaceAll((k, v) -> v / sum * featureWeights.size());
        }
    }
    
    private boolean containsContractNumber(String input) {
        return Pattern.compile("\\b\\d{6}\\b").matcher(input).find() ||
               input.toLowerCase().contains("contract") ||
               input.toLowerCase().contains("award");
    }
    
    private boolean containsCustomerInfo(String input) {
        return input.toLowerCase().contains("customer") || 
               input.toLowerCase().contains("account") ||
               Pattern.compile("\\b\\d{7,8}\\b").matcher(input).find();
    }
    
    private boolean containsPartNumber(String input) {
        return Pattern.compile("\\b[A-Z]{2}\\d{3}\\b").matcher(input).find() ||
               input.toLowerCase().contains("part");
    }
    
    private boolean containsDateInfo(String input) {
        return Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(input).find() ||
               input.toLowerCase().contains("date") ||
               input.toLowerCase().contains("created") ||
               input.toLowerCase().contains("year") ||
               input.toLowerCase().contains("month");
    }
    
    private boolean containsStatusKeywords(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("active") ||
               lowerInput.contains("expired") ||
               lowerInput.contains("failed") ||
               lowerInput.contains("status");
    }
    
    private boolean containsActionVerbs(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("show") ||
               lowerInput.contains("get") ||
               lowerInput.contains("find") ||
               lowerInput.contains("list") ||
               lowerInput.contains("display") ||
               lowerInput.contains("search") ||
               lowerInput.contains("retrieve");
    }
    
    /**
     * Training example for machine learning
     */
    public static class TrainingExample {
        public final String input;
        public final ContractQueryResponse expectedOutput;
        public final ContractQueryResponse actualOutput;
        public final double userFeedback;
        
        public TrainingExample(String input, ContractQueryResponse expectedOutput,
                              ContractQueryResponse actualOutput, double userFeedback) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.actualOutput = actualOutput;
            this.userFeedback = userFeedback;
        }
    }
}
        
