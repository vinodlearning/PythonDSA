package com.oracle.view.source;

import java.util.*;

/**
 * Specialized processor for Opportunities queries
 * Handles all queries related to opportunities, opportunity info, and opportunity contracts.
 */
public class OpportunitiesProcessor {
    // Action type constants
    private static final String OPPORTUNITIES_BY_NUMBER = "opportunities_by_number";
    private static final String OPPORTUNITIES_BY_NAME = "opportunities_by_name";
    private static final String OPPORTUNITIES_BY_FILTER = "opportunities_by_filter";

    /**
     * Process opportunity query
     */
    public NLPQueryClassifier.QueryResult process(String originalInput, String correctedInput, String normalizedInput) {
        NLPQueryClassifier.QueryResult result = new NLPQueryClassifier.QueryResult();

        // Use OpenNLP-powered entity extraction
        EntityExtractor extractor = new EntityExtractor();
        Map<String, String> nlpEntities = extractor.extractAllEntities(originalInput);

        // Use extracted entities to populate header and filters
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.header = new NLPQueryClassifier.Header();
        if (nlpEntities.containsKey("OPPORTUNITY_NUMBER")) {
            headerInfo.header.opportunityNumber = nlpEntities.get("OPPORTUNITY_NUMBER");
        }
        if (nlpEntities.containsKey("OPPORTUNITY_NAME")) {
            headerInfo.header.opportunityName = nlpEntities.get("OPPORTUNITY_NAME");
        }

        // Set input tracking
        result.inputTracking = new NLPQueryClassifier.InputTrackingResult(originalInput, correctedInput, 0.85);
        result.header = headerInfo.header;

        // Determine action type
        String actionType = determineActionType(originalInput, correctedInput, headerInfo);
        result.metadata = new NLPQueryClassifier.QueryMetadata("OPPORTUNITIES", actionType, 0.0);

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
        boolean hasOpportunityNumber = headerInfo.header.opportunityNumber != null;
        boolean hasOpportunityName = headerInfo.header.opportunityName != null;
        if (hasOpportunityNumber) {
            return OPPORTUNITIES_BY_NUMBER;
        } else if (hasOpportunityName) {
            return OPPORTUNITIES_BY_NAME;
        } else {
            return OPPORTUNITIES_BY_FILTER;
        }
    }

    /**
     * Extract filter entities
     */
    private List<NLPQueryClassifier.EntityFilter> extractEntities(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        List<NLPQueryClassifier.EntityFilter> entities = new ArrayList<>();
        if (headerInfo.header.opportunityNumber != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("OPPORTUNITY_NUMBER", "=", headerInfo.header.opportunityNumber, "extracted"));
        }
        if (headerInfo.header.opportunityName != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("OPPORTUNITY_NAME", "=", headerInfo.header.opportunityName, "extracted"));
        }
        return entities;
    }

    /**
     * Determine display entities based on query content
     */
    private List<String> determineDisplayEntities(String originalInput, String correctedInput) {
        List<String> displayEntities = new ArrayList<>();
        String lowerInput = correctedInput.toLowerCase();
        if (lowerInput.contains("opportunity number")) {
            displayEntities.add("OPPORTUNITY_NUMBER");
        }
        if (lowerInput.contains("opportunity name") || lowerInput.contains("name")) {
            displayEntities.add("OPPORTUNITY_NAME");
        }
        if (!displayEntities.isEmpty()) {
            return displayEntities;
        }
        displayEntities.add("OPPORTUNITY_NUMBER");
        displayEntities.add("OPPORTUNITY_NAME");
        return displayEntities;
    }

    /**
     * Validate input
     */
    private List<NLPQueryClassifier.ValidationError> validateInput(HeaderInfo headerInfo, List<NLPQueryClassifier.EntityFilter> entities) {
        List<NLPQueryClassifier.ValidationError> errors = new ArrayList<>();
        if (headerInfo.header.opportunityNumber == null && headerInfo.header.opportunityName == null) {
            errors.add(new NLPQueryClassifier.ValidationError("MISSING_IDENTIFIER", "Please provide an opportunity number or opportunity name", "WARNING"));
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