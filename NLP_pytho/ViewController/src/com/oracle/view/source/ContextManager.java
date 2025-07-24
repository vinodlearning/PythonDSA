package com.oracle.view.source;

import java.util.*;

/**
 * Manages query context and builds comprehensive understanding
 */
public class ContextManager {
    
    private final Map<String, List<String>> fieldMappings;
    
    public ContextManager() {
        this.fieldMappings = initializeFieldMappings();
    }
    
    public QueryContext buildContext(String input, SemanticContext semanticContext) {
        QueryContext context = new QueryContext();
        
        // Analyze focus areas
        context.hasCustomerFocus = hasCustomerFocus(input, semanticContext);
        context.hasDateFocus = hasDateFocus(input, semanticContext);
        context.hasPartFocus = hasPartFocus(input, semanticContext);
        context.hasStatusFocus = hasStatusFocus(input, semanticContext);
        
        // Analyze request types
        context.hasMetadataRequest = hasMetadataRequest(input, semanticContext);
        context.hasDetailRequest = hasDetailRequest(input, semanticContext);
        context.hasSummaryRequest = hasSummaryRequest(input, semanticContext);
        
        // Extract explicit fields
        context.explicitFields = extractExplicitFields(input);
        
        // Determine complexity
        context.queryComplexity = determineComplexity(input, semanticContext);
        
        return context;
    }
    
    private boolean hasCustomerFocus(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("customer") || lowerInput.contains("account") ||
               semanticContext.entities.stream().anyMatch(e -> 
                   e.type.equals("CUSTOMER_NUMBER") || e.type.equals("CUSTOMER_NAME"));
    }
    
    private boolean hasDateFocus(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("date") || lowerInput.contains("created") || 
               lowerInput.contains("effective") || lowerInput.contains("expiry") ||
               semanticContext.entities.stream().anyMatch(e -> 
                   e.type.contains("DATE") || e.type.equals("YEAR"));
    }
    
    private boolean hasPartFocus(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("part") || 
               semanticContext.entities.stream().anyMatch(e -> e.type.equals("PART_NUMBER"));
    }
    
    private boolean hasStatusFocus(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("status") || lowerInput.contains("active") || 
               lowerInput.contains("expired") || lowerInput.contains("pending") ||
               semanticContext.entities.stream().anyMatch(e -> e.type.equals("STATUS"));
    }
    
    private boolean hasMetadataRequest(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("metadata") || lowerInput.contains("all") || 
               lowerInput.contains("everything") || lowerInput.contains("complete") ||
               semanticContext.secondaryIntents.contains("COMPREHENSIVE_VIEW");
    }
    
    private boolean hasDetailRequest(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("details") || lowerInput.contains("detailed") || 
               lowerInput.contains("information") || lowerInput.contains("info") ||
               semanticContext.secondaryIntents.contains("DETAILED_VIEW");
    }
    
    private boolean hasSummaryRequest(String input, SemanticContext semanticContext) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("summary") || lowerInput.contains("overview") || 
               lowerInput.contains("brief") ||
               semanticContext.secondaryIntents.contains("SUMMARY_VIEW");
    }
    
    private List<String> extractExplicitFields(String input) {
        List<String> explicitFields = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        
        // Check for explicitly mentioned fields
        for (Map.Entry<String, List<String>> entry : fieldMappings.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerInput.contains(keyword.toLowerCase())) {
                    explicitFields.add(entry.getKey());
                    break;
                }
            }
        }
        
        return explicitFields;
    }
    
    private String determineComplexity(String input, SemanticContext semanticContext) {
        int complexityScore = 0;
        
        // Add points for entities
        complexityScore += semanticContext.entities.size();
        
        // Add points for secondary intents
        complexityScore += semanticContext.secondaryIntents.size();
        
        // Add points for word count
        int wordCount = input.split("\\s+").length;
        if (wordCount > 10) complexityScore += 2;
        else if (wordCount > 5) complexityScore += 1;
        
        // Add points for multiple conditions
        if (input.toLowerCase().contains(" and ")) complexityScore += 2;
        if (input.toLowerCase().contains(" or ")) complexityScore += 2;
        
        if (complexityScore >= 6) return "HIGH";
        else if (complexityScore >= 3) return "MEDIUM";
        else return "LOW";
    }
    
    private Map<String, List<String>> initializeFieldMappings() {
        Map<String, List<String>> mappings = new HashMap<>();
        
        mappings.put("CONTRACT_NUMBER", Arrays.asList("contract number", "contract", "contract id"));
        mappings.put("CUSTOMER_NAME", Arrays.asList("customer name", "customer", "client name"));
        mappings.put("CUSTOMER_NUMBER", Arrays.asList("customer number", "account number", "customer id"));
        mappings.put("PART_NUMBER", Arrays.asList("part number", "part", "part id"));
        mappings.put("CREATED_DATE", Arrays.asList("created date", "creation date", "date created"));
        mappings.put("EFFECTIVE_DATE", Arrays.asList("effective date", "start date", "begin date"));
        mappings.put("EXPIRY_DATE", Arrays.asList("expiry date", "end date", "expiration date"));
        mappings.put("STATUS", Arrays.asList("status", "state", "condition"));
        mappings.put("PROJECT_TYPE", Arrays.asList("project type", "type", "project"));
        mappings.put("PRICE_LIST", Arrays.asList("price list", "pricing", "price"));
        mappings.put("CREATED_BY", Arrays.asList("created by", "author", "creator"));
        
        return mappings;
    }
}