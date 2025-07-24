package com.oracle.view.source;
import java.util.*;
import java.util.regex.Pattern;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Classifies queries into types and determines appropriate actions
 */
public class QueryClassifier {
    
    private final Map<Pattern, QueryClassification> patternClassifications;
    private final Map<String, String> keywordMappings;
    
    public QueryClassifier() {
        this.patternClassifications = initializePatternClassifications();
        this.keywordMappings = initializeKeywordMappings();
    }
    
    public QueryClassification classify(String input) {
        String normalizedInput = input.toLowerCase().trim();
        
        // First, try pattern-based classification
        for (Map.Entry<Pattern, QueryClassification> entry : patternClassifications.entrySet()) {
            if (entry.getKey().matcher(normalizedInput).find()) {
                return entry.getValue();
            }
        }
        
        // Fallback to keyword-based classification
        return classifyByKeywords(normalizedInput);
    }
    private QueryClassification classifyByKeywords(String input) {
        String queryType = "CONTRACTS"; // Default to CONTRACTS instead of GENERAL_QUERY
        String actionType = "contracts_by_customer"; // Default action
        
        // Determine query type
        if (containsAny(input, Arrays.asList("part", "parts", "component"))) {
            queryType = "PARTS";
            actionType = "parts_by_contract";
        } else if (containsAny(input, Arrays.asList("help", "assist", "how", "what can", "steps"))) {
            queryType = "HELP";
            actionType = "help_general";
        } else {
            queryType = "CONTRACTS";
        }
        
        // Determine action type based on query type
        if (queryType.equals("CONTRACTS")) {
            actionType = determineContractActionType(input);
        } else if (queryType.equals("PARTS")) {
            actionType = determinePartsActionType(input);
        } else if (queryType.equals("HELP")) {
            actionType = determineHelpActionType(input);
        }
        
        return new QueryClassification(queryType, actionType);
    }
    private String determineContractActionType(String input) {
        // Tokenize for better analysis
        List<String> tokens = Arrays.asList(input.toLowerCase().split("\\s+"));
        
        if (input.contains("metadata") || input.contains("all details")) {
            return "contracts_by_number";
        } else if (Pattern.compile("contract\\s+\\d{6}").matcher(input).find()) {
            return "contracts_by_number";
        } else if (input.contains("account name") || input.contains("customer name")) {
            return "contracts_by_customer";
        } else if (Pattern.compile("account\\s+\\d{7,8}").matcher(input).find()) {
            return "contracts_by_account";
        } else if (containsAny(input, Arrays.asList("created by")) && !input.contains("customer") && !input.contains("account")) {
            return "contracts_by_user";
        } else if (input.contains("created in") || Pattern.compile("in\\s+\\d{4}").matcher(input).find()) {
            return "contracts_by_dates";
        } else if (containsAny(input, Arrays.asList("active", "expired", "pending", "status"))) {
            return "contracts_by_status";
        } else if (containsAny(input, Arrays.asList("project type", "project"))) {
            return "contracts_by_projectType";
        }
        return "contracts_by_customer";
    }
    private String determinePartsActionType(String input) {
        if (containsAny(input, Arrays.asList("failed", "failure", "error"))) {
            return "parts_by_failed";
        } else if (Pattern.compile("\\d{6}").matcher(input).find()) {
            return "parts_by_contract";
        } else if (Pattern.compile("[A-Z]{2}\\d{3}").matcher(input).find()) {
            return "parts_by_part";
        } else if (containsAny(input, Arrays.asList("created by", "by user"))) {
            return "parts_by_user";
        } else if (containsAny(input, Arrays.asList("status", "active", "expired"))) {
            return "parts_by_status";
        } else if (containsAny(input, Arrays.asList("date", "created in"))) {
            return "parts_by_date";
        } else if (containsAny(input, Arrays.asList("project type", "project"))) {
            return "parts_by_projectType";
        } else if (containsAny(input, Arrays.asList("customer", "account"))) {
            return "parts_by_customer";
        }
        return "parts_by_contract";
    }

    private String determineHelpActionType(String input) {
        if (containsAny(input, Arrays.asList("create contract", "contract creation"))) {
            return "help_create_contract";
        } else if (containsAny(input, Arrays.asList("load parts", "parts loading"))) {
            return "help_load_parts";
        } else if (containsAny(input, Arrays.asList("create opp", "opportunity"))) {
            return "help_create_opp";
        } else if (containsAny(input, Arrays.asList("steps create contract", "contract steps"))) {
            return "steps_create_contract";
        } else if (containsAny(input, Arrays.asList("steps load parts", "parts steps"))) {
            return "steps_load_parts";
        }
        return "help_general";
    }
    private boolean containsAny(String input, List<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }
    
    private Map<Pattern, QueryClassification> initializePatternClassifications() {
        Map<Pattern, QueryClassification> patterns = new HashMap<>();
        
        // Specific contract queries
        patterns.put(
            Pattern.compile("(?:show|get|display)\\s+contract\\s+\\d{6}"),
            new QueryClassification("CONTRACTS", "show_specific_contract")
        );
        
        // Contract metadata queries
        patterns.put(
            Pattern.compile("(?:get|show)\\s+(?:all\\s+)?metadata\\s+for\\s+contract\\s+\\d{6}"),
            new QueryClassification("CONTRACTS", "show_metadata")
        );
        
        // Customer-based queries
        patterns.put(
            Pattern.compile("contracts\\s+for\\s+customer\\s+(?:number\\s+)?\\d+"),
            new QueryClassification("CONTRACTS", "search_by_customer")
        );
        
        patterns.put(
            Pattern.compile("contracts\\s+for\\s+(?:customer\\s+)?['\"]?[a-zA-Z]+['\"]?"),
            new QueryClassification("CONTRACTS", "search_by_customer")
        );
        
        // Date-based queries
        patterns.put(
            Pattern.compile("contracts\\s+created\\s+in\\s+\\d{4}"),
            new QueryClassification("CONTRACTS", "search_by_date")
        );
        
        patterns.put(
            Pattern.compile("contracts\\s+(?:after|before|since)\\s+\\d{4}"),
            new QueryClassification("CONTRACTS", "search_by_date")
        );
        
        // Part queries
        patterns.put(
            Pattern.compile("(?:show|get|display)\\s+part\\s+[A-Z]{2}\\d{3}"),
            new QueryClassification("PARTS", "show_specific_part")
        );
        
        patterns.put(
            Pattern.compile("parts\\s+in\\s+contract\\s+\\d{6}"),
            new QueryClassification("PARTS", "show_parts_in_contract")
        );
        
        // Failed parts queries
        patterns.put(
            Pattern.compile("failed\\s+parts"),
            new QueryClassification("FAILED_PARTS", "show_failed_parts")
        );
        
        patterns.put(
            Pattern.compile("failed\\s+parts\\s+(?:in|for)\\s+contract\\s+\\d{6}"),
            new QueryClassification("FAILED_PARTS", "show_failed_parts_in_contract")
        );
        
        // Status queries
        patterns.put(
            Pattern.compile("(?:is\\s+)?(?:contract\\s+\\d{6}|part\\s+[A-Z]{2}\\d{3})\\s+(?:active|expired|status)"),
            new QueryClassification("STATUS", "check_status")
        );
        
        // Help queries
        patterns.put(
            Pattern.compile("(?:help|how|what\\s+can)"),
            new QueryClassification("HELP", "provide_help")
        );
        
        // Complex field requests
        patterns.put(
            Pattern.compile("get\\s+[a-zA-Z\\s,]+\\s+for\\s+(?:contract|account)\\s+(?:number\\s+)?\\d+"),
            new QueryClassification("CONTRACTS", "show_specific_fields")
        );
        
        return patterns;
    }
    
    private Map<String, String> initializeKeywordMappings() {
        Map<String, String> mappings = new HashMap<>();
        
        // Action keywords
        mappings.put("show", "DISPLAY");
        mappings.put("get", "RETRIEVE");
        mappings.put("display", "DISPLAY");
        mappings.put("find", "SEARCH");
        mappings.put("search", "SEARCH");
        mappings.put("list", "LIST");
        mappings.put("create", "CREATE");
        mappings.put("add", "CREATE");
        mappings.put("new", "CREATE");
        mappings.put("update", "UPDATE");
        mappings.put("modify", "UPDATE");
        mappings.put("change", "UPDATE");
        mappings.put("delete", "DELETE");
        mappings.put("remove", "DELETE");
        mappings.put("help", "HELP");
        
        // Entity keywords
        mappings.put("contract", "CONTRACT");
        mappings.put("contracts", "CONTRACT");
        mappings.put("part", "PART");
        mappings.put("parts", "PART");
        mappings.put("customer", "CUSTOMER");
        mappings.put("account", "CUSTOMER");
        mappings.put("failed", "FAILED");
        mappings.put("status", "STATUS");
        mappings.put("metadata", "METADATA");
        
        return mappings;
    }
}
