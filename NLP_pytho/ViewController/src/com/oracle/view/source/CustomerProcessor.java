package com.oracle.view.source;

import java.util.*;

/**
 * Specialized processor for Customer queries
 * Handles all queries related to customers, customer info, and customer contracts.
 */
public class CustomerProcessor {
    // Action type constants
    private static final String CUSTOMERS_BY_NUMBER = "customers_by_number";
    private static final String CUSTOMERS_BY_NAME = "customers_by_name";
    private static final String CUSTOMERS_BY_FILTER = "customers_by_filter";

    /**
     * Process customer query
     */
    public NLPQueryClassifier.QueryResult process(String originalInput, String correctedInput, String normalizedInput) {
        NLPQueryClassifier.QueryResult result = new NLPQueryClassifier.QueryResult();

        // Use OpenNLP-powered entity extraction
        EntityExtractor extractor = new EntityExtractor();
        Map<String, String> nlpEntities = extractor.extractAllEntities(originalInput);

        // Use extracted entities to populate header and filters
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.header = new NLPQueryClassifier.Header();
        if (nlpEntities.containsKey("CUSTOMER_NUMBER")) {
            headerInfo.header.customerNumber = nlpEntities.get("CUSTOMER_NUMBER");
        }
        if (nlpEntities.containsKey("CUSTOMER_NAME")) {
            headerInfo.header.customerName = nlpEntities.get("CUSTOMER_NAME");
        }

        // Set input tracking
        result.inputTracking = new NLPQueryClassifier.InputTrackingResult(originalInput, correctedInput, 0.85);
        result.header = headerInfo.header;

        // Determine action type
        String actionType = determineActionType(originalInput, correctedInput, headerInfo);
        result.metadata = new NLPQueryClassifier.QueryMetadata("CUSTOMERS", actionType, 0.0);

        // Extract entities (use existing logic, but now headerInfo is NLP-powered)
        result.entities = extractEntities(originalInput, correctedInput, headerInfo);

        // Determine display entities
        result.displayEntities = determineDisplayEntities(originalInput, correctedInput);

        // Validate input
        result.errors = validateInput(headerInfo, result.entities);

        return result;
    }

    /**
     * Determine action type based on identifiers
     */
    private String determineActionType(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        boolean hasCustomerNumber = headerInfo.header.customerNumber != null;
        boolean hasCustomerName = headerInfo.header.customerName != null;
        if (hasCustomerNumber) {
            return CUSTOMERS_BY_NUMBER;
        } else if (hasCustomerName) {
            return CUSTOMERS_BY_NAME;
        } else {
            return CUSTOMERS_BY_FILTER;
        }
    }

    /**
     * Extract filter entities
     */
    private List<NLPQueryClassifier.EntityFilter> extractEntities(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        List<NLPQueryClassifier.EntityFilter> entities = new ArrayList<>();
        if (headerInfo.header.customerNumber != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NUMBER", "=", headerInfo.header.customerNumber, "extracted"));
        }
        if (headerInfo.header.customerName != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NAME", "=", headerInfo.header.customerName, "extracted"));
        }
        return entities;
    }

    /**
     * Determine display entities based on query content
     */
    private List<String> determineDisplayEntities(String originalInput, String correctedInput) {
        List<String> displayEntities = new ArrayList<>();
        String lowerInput = correctedInput.toLowerCase();
        if (lowerInput.contains("account") || lowerInput.contains("customer number")) {
            displayEntities.add("CUSTOMER_NUMBER");
        }
        if (lowerInput.contains("customer name") || lowerInput.contains("name")) {
            displayEntities.add("CUSTOMER_NAME");
        }
        if (!displayEntities.isEmpty()) {
            return displayEntities;
        }
        displayEntities.add("CUSTOMER_NUMBER");
        displayEntities.add("CUSTOMER_NAME");
        return displayEntities;
    }

    /**
     * Validate input
     */
    private List<NLPQueryClassifier.ValidationError> validateInput(HeaderInfo headerInfo, List<NLPQueryClassifier.EntityFilter> entities) {
        List<NLPQueryClassifier.ValidationError> errors = new ArrayList<>();
        if (headerInfo.header.customerNumber == null && headerInfo.header.customerName == null) {
            errors.add(new NLPQueryClassifier.ValidationError("MISSING_IDENTIFIER", "Please provide a customer number or customer name", "WARNING"));
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