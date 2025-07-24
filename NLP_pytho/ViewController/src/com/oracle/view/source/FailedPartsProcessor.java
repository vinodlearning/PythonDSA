package com.oracle.view.source;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Specialized processor for Failed Parts queries
 * Handles all queries related to failed parts, errors, validation issues, etc.
 */
public class FailedPartsProcessor {
    
    // Action type constants
    private static final String FAILED_PARTS_BY_CONTRACT_NUMBER = "parts_failed_by_contract_number";
    private static final String FAILED_PARTS_BY_PART_NUMBER = "failed_parts_by_part_number";
    private static final String VALIDATION_ERRORS_BY_CONTRACT_NUMBER = "validation_errors_by_contract_number";
    private static final String VALIDATION_ERRORS_BY_PART_NUMBER = "validation_errors_by_part_number";
    private static final String BUSINESS_RULE_VIOLATIONS_BY_CONTRACT_NUMBER = "business_rule_violations_by_contract_number";
    private static final String BUSINESS_RULE_VIOLATIONS_BY_PART_NUMBER = "business_rule_violations_by_part_number";
    private static final String PROCESSING_ERRORS_BY_CONTRACT_NUMBER = "processing_errors_by_contract_number";
    private static final String PROCESSING_ERRORS_BY_PART_NUMBER = "processing_errors_by_part_number";
    private static final String LOADING_ERRORS_BY_CONTRACT_NUMBER = "loading_errors_by_contract_number";
    private static final String LOADING_ERRORS_BY_PART_NUMBER = "loading_errors_by_part_number";
    
    // Patterns for number detection
    private static final Pattern CONTRACT_NUMBER_PATTERN = Pattern.compile("\\b\\d{6,}\\b");
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("\\b[A-Za-z]{2}\\d{4,6}\\b");
    
    /**
     * Process failed parts query
     */
    public NLPQueryClassifier.QueryResult process(String originalInput, String correctedInput, String normalizedInput) {
        NLPQueryClassifier.QueryResult result = new NLPQueryClassifier.QueryResult();

        // --- NEW: Use OpenNLP-powered entity extraction ---
        EntityExtractor extractor = new EntityExtractor();
        Map<String, String> nlpEntities = extractor.extractAllEntities(originalInput);

        // Use extracted entities to populate header and filters
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.header = new NLPQueryClassifier.Header();
        if (nlpEntities.containsKey("CONTRACT_NUMBER")) {
            headerInfo.header.contractNumber = nlpEntities.get("CONTRACT_NUMBER");
        }
        if (nlpEntities.containsKey("PART_NUMBER")) {
            headerInfo.header.partNumber = nlpEntities.get("PART_NUMBER");
        }
        // ... add more as needed

        // Set input tracking
        result.inputTracking = new NLPQueryClassifier.InputTrackingResult(originalInput, correctedInput, 0.85);
        result.header = headerInfo.header;

        // Determine action type
        String actionType = determineActionType(originalInput, correctedInput, headerInfo);
        result.metadata = new NLPQueryClassifier.QueryMetadata("PARTS", actionType, 0.0);

        // Extract entities (use existing logic, but now headerInfo is NLP-powered)
        result.entities = extractEntities(originalInput, correctedInput, headerInfo);

        // Determine display entities
        result.displayEntities = determineDisplayEntities(originalInput, correctedInput);

        // Validate input
        result.errors = validateInput(headerInfo, result.entities);

        return result;
    }
    
    /**
     * Analyze headers (contract/part numbers)
     */
    private HeaderInfo analyzeHeaders(String originalInput, String correctedInput) {
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.header = new NLPQueryClassifier.Header();
        
        String lowerInput = correctedInput.toLowerCase();
        
        // Extract contract numbers
        java.util.regex.Matcher contractMatcher = CONTRACT_NUMBER_PATTERN.matcher(lowerInput);
        if (contractMatcher.find()) {
            headerInfo.header.contractNumber = contractMatcher.group();
        }
        
        // Extract part numbers
        java.util.regex.Matcher partMatcher = PART_NUMBER_PATTERN.matcher(lowerInput);
        if (partMatcher.find()) {
            headerInfo.header.partNumber = partMatcher.group().toUpperCase();
        }
        
        return headerInfo;
    }
    
    /**
     * Determine action type based on error type and identifiers
     */
    private String determineActionType(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        String lowerInput = correctedInput.toLowerCase();
        boolean hasContractNumber = headerInfo.header.contractNumber != null;
        boolean hasPartNumber = headerInfo.header.partNumber != null;
        
        // Check for specific error types
        if (isValidationError(lowerInput)) {
            return hasPartNumber ? VALIDATION_ERRORS_BY_PART_NUMBER : VALIDATION_ERRORS_BY_CONTRACT_NUMBER;
        }
        
        if (isBusinessRuleViolation(lowerInput)) {
            return hasPartNumber ? BUSINESS_RULE_VIOLATIONS_BY_PART_NUMBER : BUSINESS_RULE_VIOLATIONS_BY_CONTRACT_NUMBER;
        }
        
        if (isProcessingError(lowerInput)) {
            return hasPartNumber ? PROCESSING_ERRORS_BY_PART_NUMBER : PROCESSING_ERRORS_BY_CONTRACT_NUMBER;
        }
        
        if (isLoadingError(lowerInput)) {
            return hasPartNumber ? LOADING_ERRORS_BY_PART_NUMBER : LOADING_ERRORS_BY_CONTRACT_NUMBER;
        }
        
        // Default failed parts action
        return hasPartNumber ? FAILED_PARTS_BY_PART_NUMBER : FAILED_PARTS_BY_CONTRACT_NUMBER;
    }
    
    /**
     * Check if query is about validation errors
     */
    private boolean isValidationError(String lowerInput) {
        String[] validationKeywords = {
            "validation error", "validation errors", "validation issues", 
            "validation problems", "validation failures"
        };
        
        for (String keyword : validationKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if query is about business rule violations
     */
    private boolean isBusinessRuleViolation(String lowerInput) {
        String[] businessRuleKeywords = {
            "business rule violation", "business rule violations", 
            "business rules failed", "business rules violated",
            "rule violation", "rule violations"
        };
        
        for (String keyword : businessRuleKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if query is about processing errors
     */
    private boolean isProcessingError(String lowerInput) {
        String[] processingKeywords = {
            "processing error", "processing errors", "processing issues",
            "processing problems", "processing failures"
        };
        
        for (String keyword : processingKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if query is about loading errors
     */
    private boolean isLoadingError(String lowerInput) {
        String[] loadingKeywords = {
            "loading error", "loading errors", "loading issues",
            "loading problems", "loading failures", "load error",
            "load errors", "load issues", "load problems", "load failures"
        };
        
        for (String keyword : loadingKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract filter entities
     */
    private List<NLPQueryClassifier.EntityFilter> extractEntities(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        List<NLPQueryClassifier.EntityFilter> entities = new ArrayList<>();
        
        // Add contract number filter
        if (headerInfo.header.contractNumber != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("CONTRACT_NO", "=", headerInfo.header.contractNumber, "extracted"));
        }
        
        // Add part number filter
        if (headerInfo.header.partNumber != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("PART_NUMBER", "=", headerInfo.header.partNumber, "extracted"));
        }
        
        return entities;
    }
    
    /**
     * Determine display entities based on action type
     */
    private List<String> determineDisplayEntities(String originalInput, String correctedInput) {
        List<String> displayEntities = new ArrayList<>();
        String lowerInput = correctedInput.toLowerCase();

        // List of possible fields and their triggers
        Map<String, List<String>> fieldTriggers = new LinkedHashMap<>();
        fieldTriggers.put("PART_NUMBER", Arrays.asList("part number", "part#", "part"));
        fieldTriggers.put("ERROR_COLUMN", Arrays.asList("error column", "error"));
        fieldTriggers.put("REASON", Arrays.asList("reason", "failure", "cause"));
        fieldTriggers.put("CONTRACT_NO", Arrays.asList("contract number", "contract no", "contract#", "contract"));
        fieldTriggers.put("STATUS", Arrays.asList("status", "staus"));
        fieldTriggers.put("EFFECTIVE_DATE", Arrays.asList("effective date", "start date", "begin date", "effective"));
        fieldTriggers.put("EXPIRATION_DATE", Arrays.asList("expiration date", "expiry date", "end date", "expire", "expiration"));

        // Collect all specific fields requested by user
        for (Map.Entry<String, List<String>> entry : fieldTriggers.entrySet()) {
            for (String trigger : entry.getValue()) {
                if (lowerInput.contains(trigger)) {
                    if (!displayEntities.contains(entry.getKey())) {
                        displayEntities.add(entry.getKey());
                    }
                }
            }
        }

        // If any specific fields were found, return ONLY those
        if (!displayEntities.isEmpty()) {
            return displayEntities;
        }

        // Otherwise, return default columns
        displayEntities.add("PART_NUMBER");
        displayEntities.add("ERROR_COLUMN");
        displayEntities.add("REASON");
        displayEntities.add("CONTRACT_NO");
        displayEntities.add("STATUS");
        return displayEntities;
    }
    
    /**
     * Validate input
     */
    private List<NLPQueryClassifier.ValidationError> validateInput(HeaderInfo headerInfo, List<NLPQueryClassifier.EntityFilter> entities) {
        List<NLPQueryClassifier.ValidationError> errors = new ArrayList<>();
        
        // Check if we have at least one identifier
        if (headerInfo.header.contractNumber == null && headerInfo.header.partNumber == null) {
            errors.add(new NLPQueryClassifier.ValidationError("MISSING_IDENTIFIER", 
                "Please provide a contract number or part number", "WARNING"));
        }
        
        return errors;
    }
    
    /**
     * Header Info class
     */
    private static class HeaderInfo {
        NLPQueryClassifier.Header header;
        List<String> issues = new ArrayList<>();
    }
} 