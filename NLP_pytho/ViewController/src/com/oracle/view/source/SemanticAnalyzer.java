package com.oracle.view.source;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Semantic analyzer for understanding query intent and extracting entities
 */
public class SemanticAnalyzer {
    
    private final Map<Pattern, String> entityPatterns;
    private final Map<String, String> intentKeywords;
    
    public SemanticAnalyzer() {
        this.entityPatterns = initializeEntityPatterns();
        this.intentKeywords = initializeIntentKeywords();
    }
    
    public SemanticContext analyze(String input) {
        List<SemanticEntity> entities = extractEntities(input);
        String primaryIntent = determinePrimaryIntent(input);
        List<String> secondaryIntents = determineSecondaryIntents(input);
        double confidence = calculateSemanticConfidence(input, entities, primaryIntent);
        
        return new SemanticContext(entities, primaryIntent, secondaryIntents, confidence);
    }
    
    private List<SemanticEntity> extractEntities(String input) {
        List<SemanticEntity> entities = new ArrayList<>();
        
        for (Map.Entry<Pattern, String> entry : entityPatterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(input);
            while (matcher.find()) {
                String value = matcher.group(1);
                String operation = determineOperation(input, value);
                entities.add(new SemanticEntity(entry.getValue(), operation, value, matcher.start(), matcher.end()));
            }
        }
        
        return entities;
    }
    
    private String determinePrimaryIntent(String input) {
        String lowerInput = input.toLowerCase();
        
        // Check for specific intent patterns
        if (lowerInput.contains("show") || lowerInput.contains("get") || lowerInput.contains("display")) {
            if (lowerInput.contains("contract") && Pattern.compile("\\d{6}").matcher(input).find()) {
                return "SHOW_SPECIFIC_CONTRACT";
            } else if (lowerInput.contains("part") && Pattern.compile("[A-Z]{2}\\d{3}").matcher(input).find()) {
                return "SHOW_SPECIFIC_PART";
            } else if (lowerInput.contains("metadata")) {
                return "SHOW_METADATA";
            }
            return "SHOW_INFORMATION";
        }
        
        if (lowerInput.contains("create") || lowerInput.contains("add") || lowerInput.contains("new")) {
            return "CREATE_ENTITY";
        }
        
        if (lowerInput.contains("update") || lowerInput.contains("modify") || lowerInput.contains("change")) {
            return "UPDATE_ENTITY";
        }
        
        if (lowerInput.contains("delete") || lowerInput.contains("remove")) {
            return "DELETE_ENTITY";
        }
        
        if (lowerInput.contains("help") || lowerInput.contains("how") || lowerInput.contains("what can")) {
            return "REQUEST_HELP";
        }
        
        if (lowerInput.contains("search") || lowerInput.contains("find") || lowerInput.contains("list")) {
            return "SEARCH_ENTITIES";
        }
        
        if (lowerInput.contains("status") || lowerInput.contains("active") || lowerInput.contains("expired")) {
            return "CHECK_STATUS";
        }
        
        return "GENERAL_INQUIRY";
    }
    
    private List<String> determineSecondaryIntents(String input) {
        List<String> secondaryIntents = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("details") || lowerInput.contains("information")) {
            secondaryIntents.add("DETAILED_VIEW");
        }
        
        if (lowerInput.contains("summary") || lowerInput.contains("overview")) {
            secondaryIntents.add("SUMMARY_VIEW");
        }
        
        if (lowerInput.contains("all") || lowerInput.contains("everything")) {
            secondaryIntents.add("COMPREHENSIVE_VIEW");
        }
        
        if (lowerInput.contains("recent") || lowerInput.contains("latest") || lowerInput.contains("new")) {
            secondaryIntents.add("RECENT_FOCUS");
        }
        
        if (lowerInput.contains("failed") || lowerInput.contains("error") || lowerInput.contains("problem")) {
            secondaryIntents.add("ERROR_FOCUS");
        }
        
        if (lowerInput.contains("count") || lowerInput.contains("number of") || lowerInput.contains("how many")) {
            secondaryIntents.add("COUNT_REQUEST");
        }
        
        return secondaryIntents;
    }
    
    private String determineOperation(String input, String value) {
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("not") || lowerInput.contains("exclude")) {
            return "!=";
        } else if (lowerInput.contains("after") || lowerInput.contains("since")) {
            return ">=";
        } else if (lowerInput.contains("before") || lowerInput.contains("until")) {
            return "<=";
        } else if (lowerInput.contains("between")) {
            return "BETWEEN";
        } else if (lowerInput.contains("like") || lowerInput.contains("similar")) {
            return "LIKE";
        } else if (lowerInput.contains("in") || lowerInput.contains("contains")) {
            return "IN";
        }
        
        return "=";
    }
    
    private double calculateSemanticConfidence(String input, List<SemanticEntity> entities, String primaryIntent) {
        double confidence = 0.5; // Base confidence
        
        // Increase confidence based on entities found
        confidence += entities.size() * 0.1;
        
        // Increase confidence for clear intent
        if (!primaryIntent.equals("GENERAL_INQUIRY")) {
            confidence += 0.2;
        }
        
        // Increase confidence for well-formed queries
        if (input.split("\\s+").length >= 3) {
            confidence += 0.1;
        }
        
        // Decrease confidence for very short or very long queries
        int wordCount = input.split("\\s+").length;
        if (wordCount < 2 || wordCount > 20) {
            confidence -= 0.2;
        }
        
        return Math.min(1.0, Math.max(0.0, confidence));
    }
    
    private Map<Pattern, String> initializeEntityPatterns() {
        Map<Pattern, String> patterns = new HashMap<>();
        
        // Contract number patterns
        patterns.put(Pattern.compile("contract\\s+(\\d{6})"), "CONTRACT_NUMBER");
        patterns.put(Pattern.compile("\\b(\\d{6})\\b(?!.*\\d{4})"), "CONTRACT_NUMBER");
        
        // Customer/Account patterns - treat account and customer as same
        patterns.put(Pattern.compile("customer\\s+(?:number\\s+)?(\\d+)"), "CUSTOMER_NUMBER");
        patterns.put(Pattern.compile("account\\s+(?:number\\s+)?(\\d+)"), "CUSTOMER_NUMBER");
        patterns.put(Pattern.compile("(?:customer|account)\\s+(?:name\\s+)?['\"]?([A-Za-z][A-Za-z0-9\\s]*)['\"]?"), "CUSTOMER_NAME");
        patterns.put(Pattern.compile("under\\s+(?:account|customer)\\s+(?:name\\s+)?['\"]?([A-Za-z][A-Za-z0-9\\s]*)['\"]?"), "CUSTOMER_NAME");
        patterns.put(Pattern.compile("for\\s+(?:account|customer)\\s+['\"]?([A-Za-z][A-Za-z0-9\\s]*)['\"]?"), "CUSTOMER_NAME");
        
        // Created by patterns
        patterns.put(Pattern.compile("(?:created\\s+by|by\\s+user|by)\\s+([A-Za-z]+)"), "CREATED_BY");
        
        // Part number patterns
        patterns.put(Pattern.compile("part\\s+([A-Z]{2}\\d{3})"), "PART_NUMBER");
        patterns.put(Pattern.compile("\\b([A-Z]{2}\\d{3})\\b"), "PART_NUMBER");
        
        // Date patterns
        patterns.put(Pattern.compile("(?:created\\s+)?in\\s+(\\d{4})"), "CREATED_DATE");
        patterns.put(Pattern.compile("(?:after|since)\\s+(\\d{4})"), "CREATED_DATE");
        patterns.put(Pattern.compile("(?:before|until)\\s+(\\d{4})"), "CREATED_DATE");
        
        // Status patterns
        patterns.put(Pattern.compile("status\\s+(active|expired|pending|cancelled)", Pattern.CASE_INSENSITIVE), "STATUS");
        
        // Project type patterns
        patterns.put(Pattern.compile("project\\s+type\\s+([A-Za-z_]+)"), "PROJECT_TYPE");
        
        return patterns;
    }
    
    private Map<String, String> initializeIntentKeywords() {
        Map<String, String> keywords = new HashMap<>();
        
        keywords.put("show", "DISPLAY");
        keywords.put("get", "RETRIEVE");
        keywords.put("display", "DISPLAY");
        keywords.put("find", "SEARCH");
        keywords.put("search", "SEARCH");
        keywords.put("list", "LIST");
        keywords.put("create", "CREATE");
        keywords.put("add", "CREATE");
        keywords.put("new", "CREATE");
        keywords.put("update", "UPDATE");
        keywords.put("modify", "UPDATE");
        keywords.put("change", "UPDATE");
        keywords.put("delete", "DELETE");
        keywords.put("remove", "DELETE");
        keywords.put("help", "HELP");
        keywords.put("status", "STATUS");
        keywords.put("details", "DETAILS");
        keywords.put("metadata", "METADATA");
        keywords.put("summary", "SUMMARY");
        keywords.put("overview", "SUMMARY");
        
        return keywords;
    }
}