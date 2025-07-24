package com.oracle.view.source;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalDateTime;

public class ContractQueryParser {
    
    private final ObjectMapper objectMapper;
    private final QueryClassifier classifier;
    private final EntityExtractor entityExtractor;
    private final SpellCorrector spellCorrector;
    
    public ContractQueryParser() {
        this.objectMapper = new ObjectMapper();
        this.classifier = new QueryClassifier();
        this.entityExtractor = new EntityExtractor();
        this.spellCorrector = new SpellCorrector();
    }
    
    public ContractQueryResponse parseQuery(String userInput) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Spell correction and normalization
            String correctedInput = spellCorrector.correct(userInput.toLowerCase().trim());
            
            // Step 2: Classify query type and action
            QueryClassification classification = classifier.classify(correctedInput);
            
            // Step 3: Extract entities (now called filters)
            List<QueryEntity> filters = entityExtractor.extractEntities(correctedInput, classification);
            
            
            // Remove duplicates
            filters = removeDuplicateFilters(filters);
            
            // Step 4: Determine display entities
            List<String> displayEntities = determineDisplayEntities(classification, filters, correctedInput);
            
            // Step 5: Extract header information
            QueryHeader header = extractHeaderInfo(filters); // Changed from ContractHeader to QueryHeader
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return new ContractQueryResponse(
                userInput, // Original input
                correctedInput, // Corrected input
                header,
                new QueryMetadata(classification.queryType, classification.actionType, processingTime),
                filters,
                displayEntities,
                new ArrayList<>()
            );
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            return createErrorResponse(userInput, e.getMessage(), processingTime);
        }
    }
    
    private List<String> determineDisplayEntities(QueryClassification classification,
                                                 List<QueryEntity> filters, String input) {
        Set<String> displayFields = new LinkedHashSet<>();
        
        // Default fields based on query type
        if (classification.queryType.equals("CONTRACTS")) {
            displayFields.add("CONTRACT_NUMBER");
            displayFields.add("CUSTOMER_NAME");
        } else if (classification.queryType.equals("PARTS")) {
            displayFields.add("PART_NUMBER");
            displayFields.add("PART_DESCRIPTION");
        }
        
        // Add fields based on extracted filters
        for (QueryEntity filter : filters) {
            switch (filter.attribute) {
                case "CREATED_DATE":
                case "EFFECTIVE_DATE":
                case "EXPIRY_DATE":
                    displayFields.add(filter.attribute);
                    break;
            }
        }
        
        // Add explicitly requested fields based on input keywords
        if (input.contains("status")) {
            displayFields.add("STATUS");
            displayFields.add("EFFECTIVE_DATE");
            displayFields.add("EXPIRY_DATE");
        }
        if (input.contains("details")) {
            displayFields.addAll(Arrays.asList("EFFECTIVE_DATE", "EXPIRY_DATE", "STATUS", "PROJECT_TYPE"));
        }
        if (input.contains("effective date")) displayFields.add("EFFECTIVE_DATE");
        if (input.contains("price list")) displayFields.add("PRICE_LIST");
        if (input.contains("project type")) displayFields.add("PROJECT_TYPE");
        if (input.contains("created")) displayFields.add("CREATED_DATE");
        
        return new ArrayList<>(displayFields);
    }
    
    private QueryHeader extractHeaderInfo(List<QueryEntity> filters) { // Changed return type from ContractHeader to QueryHeader
        QueryHeader header = new QueryHeader(); // Changed from ContractHeader to QueryHeader
        
        for (QueryEntity filter : filters) {
            switch (filter.attribute) {
                case "CONTRACT_NUMBER":
                    header.setContractNumber(filter.value); // Changed from direct field access to setter
                    break;
                case "CUSTOMER_NUMBER":
                    header.setCustomerNumber(filter.value); // Changed from direct field access to setter
                    break;
                case "CUSTOMER_NAME":
                    header.setCustomerName(filter.value); // Changed from direct field access to setter
                    break;
                case "PART_NUMBER":
                    header.setPartNumber(filter.value); // Changed from direct field access to setter
                    break;
                case "CREATED_BY":
                    header.setCreatedBy(filter.value); // Changed from direct field access to setter
                    break;
            }
        }
        
        return header;
    }
    
    private ContractQueryResponse createErrorResponse(String originalInput, String error, long processingTime) {
        return new ContractQueryResponse(
            originalInput,
            originalInput, // No correction on error
            new QueryHeader(), // Changed from ContractHeader to QueryHeader
            new QueryMetadata("UNKNOWN", "error", processingTime),
            new ArrayList<>(),
            new ArrayList<>(),
            Arrays.asList(error)
        );
    }
    
    private List<QueryEntity> removeDuplicateFilters(List<QueryEntity> filters) {
        Set<String> seen = new HashSet<>();
        List<QueryEntity> uniqueFilters = new ArrayList<>();
        
        for (QueryEntity filter : filters) {
            String key = filter.attribute + ":" + filter.operation + ":" + filter.value;
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueFilters.add(filter);
            }
        }
        
        return uniqueFilters;
    }
}
