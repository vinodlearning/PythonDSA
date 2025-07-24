package com.oracle.view.source;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Specialized processor for Contract queries
 * Handles all queries related to contracts, customers, status, etc.
 */
public class ContractProcessor {
    
    // Action type constants
    private static final String CONTRACTS_BY_CONTRACT_NUMBER = "contracts_by_contractnumber";
    private static final String CONTRACTS_BY_FILTER = "contracts_by_filter";
    
    // Patterns for number detection - Enhanced for concatenated text
    private static final Pattern CONTRACT_NUMBER_PATTERN_WORD_BOUNDARY = Pattern.compile("\\b\\d{6}\\b");
    private static final Pattern CONTRACT_NUMBER_PATTERN_CONCATENATED = Pattern.compile("\\d{6}");
    private static final Pattern CUSTOMER_NUMBER_PATTERN = Pattern.compile("\\b\\d{4,8}\\b");
    
    /**
     * Process contract query
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
        if (nlpEntities.containsKey("CUSTOMER_NAME")) {
            headerInfo.header.customerName = nlpEntities.get("CUSTOMER_NAME");
        }
        if (nlpEntities.containsKey("CUSTOMER_NUMBER")) {
            headerInfo.header.customerNumber = nlpEntities.get("CUSTOMER_NUMBER");
        }
        if (nlpEntities.containsKey("CREATED_BY")) {
            headerInfo.header.createdBy = nlpEntities.get("CREATED_BY");
        }
        // ... add more as needed

        // Set input tracking
        result.inputTracking = new NLPQueryClassifier.InputTrackingResult(originalInput, correctedInput, 0.85);
        result.header = headerInfo.header;

        // Determine action type
        String actionType = determineActionType(originalInput, correctedInput, headerInfo);

        // For contract creation queries, set query type to HELP
        String queryType = "CONTRACTS";
        if (actionType.contains("HELP_CONTRACT_CREATE")) {
            queryType = "HELP";
        }

        result.metadata = new NLPQueryClassifier.QueryMetadata(queryType, actionType, 0.0);

        // Extract entities (use existing logic, but now headerInfo is NLP-powered)
        result.entities = extractEntities(originalInput, correctedInput, headerInfo);

        // Determine display entities
        result.displayEntities = determineDisplayEntities(originalInput, correctedInput);

        // Validate input (skip validation for contract creation queries)
        if (!actionType.contains("HELP_CONTRACT_CREATE")) {
            result.errors = validateInput(headerInfo, result.entities);
        } else {
            result.errors = new ArrayList<>(); // No errors for contract creation
        }

        return result;
    }
    
    /**
     * Analyze headers (contract/customer numbers)
     */
    private HeaderInfo analyzeHeaders(String originalInput, String correctedInput) {
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.header = new NLPQueryClassifier.Header();
        
        String lowerInput = correctedInput.toLowerCase();
        
        // Extract contract numbers (6 digits) - Enhanced for concatenated text like "show100476"
        System.out.println("=== ContractProcessor Contract Number Extraction Debug ===");
        System.out.println("Input: " + lowerInput);
        
        List<String> contractNumbers = extractContractNumbers(lowerInput);
        if (!contractNumbers.isEmpty()) {
            String contractNumber = contractNumbers.get(0); // Take the first one
            headerInfo.header.contractNumber = contractNumber;
            System.out.println("Contract number found: " + contractNumber);
            
            // If we found a contract number, don't extract customer number for the same value
            // This prevents duplicate extraction of the same number
            return headerInfo;
        }
        
        // Extract customer numbers (4-8 digits) - only if no contract number found
        java.util.regex.Matcher customerMatcher = CUSTOMER_NUMBER_PATTERN.matcher(lowerInput);
        if (customerMatcher.find()) {
            headerInfo.header.customerNumber = customerMatcher.group();
        }
        
        // Extract customer names
        String customerName = extractCustomerName(lowerInput);
        if (customerName != null) {
            headerInfo.header.customerName = customerName;
        }
        
        // Extract creator names
        String creatorName = extractCreatorName(lowerInput);
        if (creatorName != null) {
            headerInfo.header.createdBy = creatorName;
        }
        
        return headerInfo;
    }
    
    /**
     * Enhanced contract number extraction for concatenated text
     */
    private List<String> extractContractNumbers(String input) {
        List<String> contractNumbers = new ArrayList<>();
        
        System.out.println("=== ContractProcessor Contract Number Extraction Debug ===");
        System.out.println("Input: " + input);
        
        // Pattern 1: 6-digit numbers with word boundaries (original)
        Matcher matcher1 = CONTRACT_NUMBER_PATTERN_WORD_BOUNDARY.matcher(input);
        while (matcher1.find()) {
            String found = matcher1.group();
            contractNumbers.add(found);
            System.out.println("Pattern1 found: " + found);
        }
        
        // Pattern 2: 6-digit numbers that might be concatenated with words
        // This catches cases like "show100476", "contract100476", etc.
        Matcher matcher2 = CONTRACT_NUMBER_PATTERN_CONCATENATED.matcher(input);
        while (matcher2.find()) {
            String found = matcher2.group();
            // Only add if not already found by pattern1
            if (!contractNumbers.contains(found)) {
                contractNumbers.add(found);
                System.out.println("Pattern2 found: " + found);
            }
        }
        
        System.out.println("Final contract numbers: " + contractNumbers);
        System.out.println("=== End ContractProcessor Contract Number Extraction Debug ===");
        
        return contractNumbers;
    }
    
    /**
     * Determine action type based on identifiers
     */
    private String determineActionType(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        System.out.println("=== ContractProcessor Action Type Debug ===");
        System.out.println("Original Input: " + originalInput);
        System.out.println("Corrected Input: " + correctedInput);
        System.out.println("Has Contract Number: " + (headerInfo.header.contractNumber != null));
        System.out.println("Has Created By: " + (headerInfo.header.createdBy != null));
        
        boolean hasContractNumber = headerInfo.header.contractNumber != null;
        boolean hasCreatedBy = headerInfo.header.createdBy != null;
        
        // Step 1: Check for "created by" queries FIRST (highest priority)
        if (hasCreatedBy || isCreatedByQuery(correctedInput)) {
            System.out.println("Detected as: contracts_by_user");
            return "contracts_by_user";
        }
        
        // Step 2: Check for contract creation queries
        if (isContractCreationQuery(correctedInput)) {
            String creationType = classifyContractCreationIntent(correctedInput);
            System.out.println("Detected as: " + creationType);
            return creationType;
        }
        
        // Step 3: Check for "created" queries (with date filters but no user)
        if (isCreatedQuery(correctedInput)) {
            System.out.println("Detected as: contracts_by_filter (created with date)");
            return CONTRACTS_BY_FILTER;
        }
        
        // Step 4: Check for specific contract queries
        if (hasContractNumber) {
            System.out.println("Detected as: contracts_by_contractnumber");
            return CONTRACTS_BY_CONTRACT_NUMBER;
        } else {
            System.out.println("Detected as: contracts_by_filter");
            return CONTRACTS_BY_FILTER;
        }
    }
    
    /**
     * Check if this is a "created by" query
     */
    private boolean isCreatedByQuery(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("created by") || 
               (lowerInput.contains("created") && lowerInput.contains("by"));
    }
    
    /**
     * Check if this is a "created" query (with date filters but no user)
     */
    private boolean isCreatedQuery(String input) {
        String lowerInput = input.toLowerCase();
        // Check if it contains "created" but NOT "created by"
        return lowerInput.contains("created") && !lowerInput.contains("created by");
    }
    
    /**
     * Check if this is a contract creation query
     */
    private boolean isContractCreationQuery(String input) {
        String lowerInput = input.toLowerCase();
        
        // Check for explicit creation keywords (PRESENT TENSE ONLY)
        String[] creationKeywords = {
            "create contract", "make contract", "generate contract", "build contract",
            "set up contract", "new contract", "start contract", "initiate contract",
            "draft contract", "establish contract", "form contract", "develop contract"
        };
        
        for (String keyword : creationKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        // Check for single word "create" with contract context (PRESENT TENSE ONLY)
        // EXCLUDE past tense words like "created", "made", "generated", etc.
        if (lowerInput.contains("create") && lowerInput.contains("contract") && 
            !lowerInput.contains("created") && !lowerInput.contains("making") && 
            !lowerInput.contains("generated") && !lowerInput.contains("built")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Classify contract creation intent
     */
    private String classifyContractCreationIntent(String input) {
        String lowerInput = input.toLowerCase();
        
        // Check for user asking system to create contract (HELP_CONTRACT_CREATE_BOT)
        String[] botCreationPatterns = {
            "can you create", "why can't you create", "please create", "psl create",
            "for account", "create for me", "create a contract for me"
        };
        
        for (String pattern : botCreationPatterns) {
            if (lowerInput.contains(pattern)) {
                return NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT;
            }
        }
        
        // Check for user asking for steps/help to create contract (HELP_CONTRACT_CREATE_USER)
        String[] userHelpPatterns = {
            "steps to create", "how to create", "show me create", "show me how to create",
            "help me to create", "list the steps to create", "guide me to create"
        };
        
        for (String pattern : userHelpPatterns) {
            if (lowerInput.contains(pattern)) {
                return NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_USER;
            }
        }
        
        // Default: assume user wants system to create contract
        return NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT;
    }
    
    /**
     * Extract filter entities
     */
    private List<NLPQueryClassifier.EntityFilter> extractEntities(String originalInput, String correctedInput, HeaderInfo headerInfo) {
        List<NLPQueryClassifier.EntityFilter> entities = new ArrayList<>();
        // Always use AWARD_NUMBER for contract number
        if (headerInfo.header.contractNumber != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("AWARD_NUMBER", "=", headerInfo.header.contractNumber, "extracted"));
        }
        if (headerInfo.header.customerNumber != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NUMBER", "=", headerInfo.header.customerNumber, "extracted"));
        }
        if (headerInfo.header.customerName != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NAME", "=", headerInfo.header.customerName, "extracted"));
        }
        if (headerInfo.header.createdBy != null) {
            entities.add(new NLPQueryClassifier.EntityFilter("CREATED_BY", "=", headerInfo.header.createdBy, "extracted"));
        }
        // Enhanced date extraction using EnhancedDateExtractor
        EnhancedDateExtractor.DateExtractionResult dateResult = EnhancedDateExtractor.extractDateInfo(originalInput);
        if (dateResult.hasDateInfo()) {
            if (dateResult.getInYear() != null) {
                entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "IN_YEAR", dateResult.getInYear().toString(), "extracted"));
            } else if (dateResult.getAfterYear() != null) {
                if ("AFTER_TO_CURRENT".equals(dateResult.getTemporalOperation())) {
                    int currentYear = java.time.LocalDate.now().getYear();
                    String dateRange = dateResult.getAfterYear() + "," + currentYear;
                    entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "YEAR_RANGE", dateRange, "extracted"));
                } else {
                    entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "AFTER_YEAR", dateResult.getAfterYear().toString(), "extracted"));
                }
            } else if (dateResult.getBeforeYear() != null) {
                entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "BEFORE_YEAR", dateResult.getBeforeYear().toString(), "extracted"));
            } else if (dateResult.getStartYear() != null && dateResult.getEndYear() != null) {
                String dateRange = dateResult.getStartYear() + "," + dateResult.getEndYear();
                entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "YEAR_RANGE", dateRange, "extracted"));
            } else if (dateResult.getStartMonth() != null && dateResult.getEndMonth() != null) {
                int currentYear = dateResult.getStartYear() != null ? dateResult.getStartYear() : java.time.LocalDate.now().getYear();
                int endYear = dateResult.getEndYear() != null ? dateResult.getEndYear() : currentYear;
                java.time.LocalDate startDate = java.time.LocalDate.of(currentYear, dateResult.getStartMonth(), 1);
                java.time.LocalDate endDate = java.time.LocalDate.of(endYear, dateResult.getEndMonth(), java.time.LocalDate.of(endYear, dateResult.getEndMonth(), 1).lengthOfMonth());
                String dateRange = startDate.toString() + "," + endDate.toString();
                entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "MONTH_RANGE", dateRange, "extracted"));
            } else if (dateResult.getSpecificDate() != null) {
                entities.add(new NLPQueryClassifier.EntityFilter("CREATE_DATE", "=", dateResult.getSpecificDate().toString(), "extracted"));
            }
        }
        return entities;
    }
    
    /**
     * Determine display entities based on query content
     */
    private List<String> determineDisplayEntities(String originalInput, String correctedInput) {
        List<String> displayEntities = new ArrayList<>();
        String lowerInput = correctedInput.toLowerCase();
        TableColumnConfig tcc = TableColumnConfig.getInstance();
        String[] userKeywords = lowerInput.split("[^a-zA-Z0-9]+");
        Set<String> foundColumns = new LinkedHashSet<>();
        boolean customerMentioned = false;
        for (String kw : userKeywords) {
            String col = tcc.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_CONTRACTS, kw);
            if (col != null) {
                foundColumns.add(col);
                if (col.equals("CUSTOMER_NAME") || col.equals("CUSTOMER_NUMBER")) {
                    customerMentioned = true;
                }
            }
        }
        // If user asked for customer, always include both name and number
        if (customerMentioned) {
            foundColumns.add("CUSTOMER_NAME");
            foundColumns.add("CUSTOMER_NUMBER");
        }
        // If any columns found, return them
        if (!foundColumns.isEmpty()) {
            return new ArrayList<>(foundColumns);
        }
        // Otherwise, return default columns
        return Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "CREATE_DATE", "EXPIRATION_DATE", "STATUS");
    }
    
    /**
     * Extract customer name from input
     */
    private String extractCustomerName(String lowerInput) {
        // Simple pattern matching for customer names
        if (lowerInput.contains("customer") && lowerInput.contains("name")) {
            // Extract name after "customer name"
            int index = lowerInput.indexOf("customer name");
            if (index != -1 && index + 12 < lowerInput.length()) {
                String afterCustomerName = lowerInput.substring(index + 12).trim();
                String[] words = afterCustomerName.split("\\s+");
                if (words.length > 0 && words[0].length() > 2) {
                    return words[0].substring(0, 1).toUpperCase() + words[0].substring(1);
                }
            }
        }
        return null;
    }
    
    /**
     * Extract creator name from input
     * Handles various patterns: "created by vinod", "contracts created by vinod", etc.
     * Enhanced to handle complex queries with date filters
     */
    private String extractCreatorName(String lowerInput) {
        System.out.println("=== Creator Name Extraction Debug ===");
        System.out.println("Input: " + lowerInput);
        
        // Pattern 1: "created by [name]" - direct extraction
        if (lowerInput.contains("created by")) {
            int index = lowerInput.indexOf("created by");
            if (index != -1 && index + 10 < lowerInput.length()) {
                String afterCreatedBy = lowerInput.substring(index + 10).trim();
                System.out.println("After 'created by': " + afterCreatedBy);
                
                // Extract name before any date-related keywords
                String name = extractNameBeforeDateKeywords(afterCreatedBy);
                if (name != null) {
                    System.out.println("Extracted name (Pattern 1): " + name);
                    return name;
                }
            }
        }
        
        // Pattern 2: "by [name]" when "created" appears earlier
        if (lowerInput.contains("created")) {
            int byIndex = lowerInput.indexOf("by");
            if (byIndex != -1 && byIndex + 2 < lowerInput.length()) {
                String afterBy = lowerInput.substring(byIndex + 2).trim();
                System.out.println("After 'by': " + afterBy);
                
                // Extract name before any date-related keywords
                String name = extractNameBeforeDateKeywords(afterBy);
                if (name != null) {
                    System.out.println("Extracted name (Pattern 2): " + name);
                    return name;
                }
            }
        }
        
        // Pattern 3: Handle "show the contracts created by vinod" pattern
        if (lowerInput.contains("show") && lowerInput.contains("created by")) {
            int createdByIndex = lowerInput.indexOf("created by");
            if (createdByIndex != -1 && createdByIndex + 10 < lowerInput.length()) {
                String afterCreatedBy = lowerInput.substring(createdByIndex + 10).trim();
                System.out.println("After 'created by' (show pattern): " + afterCreatedBy);
                
                // Extract name before any date-related keywords
                String name = extractNameBeforeDateKeywords(afterCreatedBy);
                if (name != null) {
                    System.out.println("Extracted name (Pattern 3): " + name);
                    return name;
                }
            }
        }
        
        // Pattern 4: Handle "contracts created by vinod" pattern
        if (lowerInput.contains("contracts") && lowerInput.contains("created by")) {
            int createdByIndex = lowerInput.indexOf("created by");
            if (createdByIndex != -1 && createdByIndex + 10 < lowerInput.length()) {
                String afterCreatedBy = lowerInput.substring(createdByIndex + 10).trim();
                System.out.println("After 'created by' (contracts pattern): " + afterCreatedBy);
                
                // Extract name before any date-related keywords
                String name = extractNameBeforeDateKeywords(afterCreatedBy);
                if (name != null) {
                    System.out.println("Extracted name (Pattern 4): " + name);
                    return name;
                }
            }
        }
        
        System.out.println("No creator name extracted");
        System.out.println("=== End Creator Name Extraction Debug ===");
        return null;
    }
    
    /**
     * Extract name before date-related keywords
     * Handles cases like "vinod and in 2025", "vinod and after 2024", etc.
     */
    private String extractNameBeforeDateKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // Split by common date-related keywords
        String[] dateKeywords = {
            " and ", " in ", " after ", " before ", " between ", " to ", " till ", " until ",
            " created ", " during ", " since ", " from ", " of ", " on "
        };
        
        String name = text;
        for (String keyword : dateKeywords) {
            if (text.contains(keyword)) {
                int index = text.indexOf(keyword);
                name = text.substring(0, index).trim();
                break;
            }
        }
        
        // Clean up the name (remove extra spaces, capitalize first letter)
        if (name.length() > 2) {
            String[] words = name.split("\\s+");
            if (words.length > 0) {
                String firstName = words[0];
                return firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
            }
        }
        
        return null;
    }
    
    /**
     * Validate input
     */
    private List<NLPQueryClassifier.ValidationError> validateInput(HeaderInfo headerInfo, List<NLPQueryClassifier.EntityFilter> entities) {
        List<NLPQueryClassifier.ValidationError> errors = new ArrayList<>();
        
        // Check if we have at least one identifier
        if (headerInfo.header.contractNumber == null && 
            headerInfo.header.customerNumber == null && 
            headerInfo.header.customerName == null &&
            headerInfo.header.createdBy == null) {
            errors.add(new NLPQueryClassifier.ValidationError("MISSING_IDENTIFIER", 
                "Please provide a contract number, customer number, customer name, or creator name", "WARNING"));
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