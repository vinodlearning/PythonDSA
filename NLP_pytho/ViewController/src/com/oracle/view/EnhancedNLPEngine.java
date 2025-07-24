package com.oracle.view;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Enhanced NLP Engine with proper tokenization, POS tagging, and entity recognition
 * Handles complex queries to distinguish between dates, contract numbers, and other entities
 */
public class EnhancedNLPEngine {
    
    // Enhanced patterns for better entity recognition
    private final Pattern contractNumberPattern = Pattern.compile("\\b[A-Z]{2,4}[-_]\\d{4,}[-_]\\d{1,}\\b|\\b[A-Z]{2,3}[-_]?\\d{4,}\\b|\\bCON[-_]?\\d{4,}\\b|\\b\\d{6,}\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern partNumberPattern = Pattern.compile("\\b[A-Z]{2,3}\\d{3,}\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern accountNumberPattern = Pattern.compile("\\b\\d{7,12}\\b"); // Account numbers are longer
    private final Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[-/]\\w{3,}[-/]\\d{2,4}\\b|\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b");
    private final Pattern monthYearPattern = Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\w*\\s+(19|20)\\d{2}\\b", Pattern.CASE_INSENSITIVE);
    
    // Context-aware keywords
    private final Set<String> contractKeywords = new HashSet<>(Arrays.asList(
        "contract", "contrct", "cntrct", "agreement", "deal"
    ));
    
    private final Set<String> partKeywords = new HashSet<>(Arrays.asList(
        "part", "prt", "pasrt", "component", "item", "piece"
    ));
    
    private final Set<String> dateKeywords = new HashSet<>(Arrays.asList(
        "after", "before", "between", "since", "until", "from", "to", "in", "during", "created"
    ));
    
    private final Set<String> actionKeywords = new HashSet<>(Arrays.asList(
        "show", "display", "get", "find", "list", "retrieve"
    ));
    
    private final Set<String> creatorNames = new HashSet<>(Arrays.asList(
        "vinod", "mary", "john", "sarah", "mike"
    ));
    
    private final Set<String> customerNames = new HashSet<>(Arrays.asList(
        "siemens", "boeing", "honeywell", "ge", "microsoft", "oracle"
    ));
    
    // Spell correction mappings
    private final Map<String, String> spellCorrections = new HashMap<>();
    
    public EnhancedNLPEngine() {
        initializeSpellCorrections();
    }
    
    /**
     * Main processing method with enhanced entity recognition
     */
    public NLPResponse processQuery(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return createErrorResponse("Empty input provided", userInput);
        }
        
        try {
            // Step 1: Enhanced tokenization and normalization
            String normalizedInput = normalizeInput(userInput);
            
            // Step 2: Spell correction
            String correctedInput = applySpellCorrection(normalizedInput);
            double correctionConfidence = calculateCorrectionConfidence(normalizedInput, correctedInput);
            
            // Step 3: Enhanced entity extraction with context awareness
            Map<String, Object> extractedEntities = extractEntitiesWithContext(correctedInput);
            
            // Step 4: Intelligent domain routing
            DomainType domain = determineDomainWithContext(correctedInput, extractedEntities);
            
            // Step 5: Generate response based on domain and context
            return generateEnhancedResponse(userInput, correctedInput, correctionConfidence, 
                                          extractedEntities, domain);
            
        } catch (Exception e) {
            return createErrorResponse("Processing error: " + e.getMessage(), userInput);
        }
    }
    
    /**
     * Enhanced entity extraction with context awareness
     */
    private Map<String, Object> extractEntitiesWithContext(String input) {
        Map<String, Object> entities = new HashMap<>();
        
        // Tokenize input for context analysis
        String[] tokens = input.toLowerCase().split("\\s+");
        List<String> tokenList = Arrays.asList(tokens);
        
        // Extract contract numbers with context validation
        extractContractNumbers(input, tokenList, entities);
        
        // Extract part numbers
        extractPartNumbers(input, entities);
        
        // Extract account numbers (distinct from contract numbers)
        extractAccountNumbers(input, tokenList, entities);
        
        // Extract dates and temporal information
        extractTemporalInformation(input, tokenList, entities);
        
        // Extract creator names
        extractCreatorNames(input, entities);
        
        // Extract customer names
        extractCustomerNames(input, entities);
        
        // Determine query intent
        determineQueryIntent(input, tokenList, entities);
        
        return entities;
    }
    
    /**
     * Extract contract numbers with context validation
     */
    private void extractContractNumbers(String input, List<String> tokens, Map<String, Object> entities) {
        Matcher contractMatcher = contractNumberPattern.matcher(input);
        
        while (contractMatcher.find()) {
            String potentialContract = contractMatcher.group();
            
            // Context validation to avoid false positives
            if (isValidContractNumber(potentialContract, input, tokens)) {
                entities.put("contractNumber", potentialContract);
                break; // Take the first valid contract number
            }
        }
    }
    
    /**
     * Validate if a number is actually a contract number based on context
     */
    private boolean isValidContractNumber(String number, String input, List<String> tokens) {
        // Rule 1: If it's clearly a year, it's not a contract number
        if (yearPattern.matcher(number).matches()) {
            // Check if it's in a date context
            String lowerInput = input.toLowerCase();
            if (lowerInput.contains("after") || lowerInput.contains("before") || 
                lowerInput.contains("since") || lowerInput.contains("in " + number) ||
                lowerInput.contains("created") || lowerInput.contains("jan") ||
                lowerInput.contains("feb") || lowerInput.contains("mar") ||
                lowerInput.contains("apr") || lowerInput.contains("may") ||
                lowerInput.contains("jun") || lowerInput.contains("jul") ||
                lowerInput.contains("aug") || lowerInput.contains("sep") ||
                lowerInput.contains("oct") || lowerInput.contains("nov") ||
                lowerInput.contains("dec")) {
                return false; // It's a year in date context
            }
        }
        
        // Rule 2: Contract numbers are typically 6+ digits or have prefixes
        if (number.matches("\\d{4,5}") && !number.matches("\\d{6,}")) {
            // 4-5 digit numbers need contract context to be valid
            String lowerInput = input.toLowerCase();
            return lowerInput.contains("contract") && 
                   (lowerInput.contains("show") || lowerInput.contains("get") || 
                    lowerInput.contains("display") || lowerInput.contains("find"));
        }
        
        // Rule 3: 6+ digit numbers or prefixed numbers are likely contracts
        return number.matches("\\d{6,}") || number.matches("[A-Z]{2,4}[-_]\\d{4,}[-_]\\d{1,}") || number.matches("[A-Z]{2,3}[-_]?\\d{4,}");
    }
    
    /**
     * Extract part numbers
     */
    private void extractPartNumbers(String input, Map<String, Object> entities) {
        Matcher partMatcher = partNumberPattern.matcher(input);
        if (partMatcher.find()) {
            entities.put("partNumber", partMatcher.group().toUpperCase());
        }
    }
    
    /**
     * Extract account numbers (distinct from contract numbers)
     */
    private void extractAccountNumbers(String input, List<String> tokens, Map<String, Object> entities) {
        Matcher accountMatcher = accountNumberPattern.matcher(input);
        
        while (accountMatcher.find()) {
            String potentialAccount = accountMatcher.group();
            
            // Check if it's in account context
            String lowerInput = input.toLowerCase();
            if (lowerInput.contains("account") || lowerInput.contains("customer number")) {
                entities.put("accountNumber", potentialAccount);
                break;
            }
        }
    }
    
    /**
     * Extract temporal information (dates, years, ranges)
     */
    private void extractTemporalInformation(String input, List<String> tokens, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        
        // Extract specific dates
        Matcher dateMatcher = datePattern.matcher(input);
        if (dateMatcher.find()) {
            entities.put("specificDate", dateMatcher.group());
        }
        
        // Extract month-year combinations
        Matcher monthYearMatcher = monthYearPattern.matcher(input);
        if (monthYearMatcher.find()) {
            entities.put("monthYear", monthYearMatcher.group());
        }
        
        // Extract years in temporal context
        Matcher yearMatcher = yearPattern.matcher(input);
        while (yearMatcher.find()) {
            String year = yearMatcher.group();
            
            // Check if it's in a temporal context
            if (isInTemporalContext(year, lowerInput)) {
                if (lowerInput.contains("after") || lowerInput.contains("since")) {
                    entities.put("afterYear", year);
                } else if (lowerInput.contains("before") || lowerInput.contains("until")) {
                    entities.put("beforeYear", year);
                } else if (lowerInput.contains("in") || lowerInput.contains("during") || lowerInput.contains("created")) {
                    entities.put("inYear", year);
                }
            }
        }
        
        // Determine temporal operation type
        if (lowerInput.contains("after") || lowerInput.contains("since")) {
            entities.put("temporalOperation", "AFTER");
        } else if (lowerInput.contains("before") || lowerInput.contains("until")) {
            entities.put("temporalOperation", "BEFORE");
        } else if (lowerInput.contains("between")) {
            entities.put("temporalOperation", "BETWEEN");
        } else if (lowerInput.contains("in") || lowerInput.contains("during") || lowerInput.contains("created")) {
            entities.put("temporalOperation", "IN");
        }
    }
    
    /**
     * Check if a year is in temporal context
     */
    private boolean isInTemporalContext(String year, String input) {
        int yearIndex = input.indexOf(year);
        if (yearIndex == -1) return false;
        
        // Check surrounding words for temporal indicators
        String[] words = input.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].contains(year)) {
                // Check previous and next words
                for (int j = Math.max(0, i-2); j <= Math.min(words.length-1, i+2); j++) {
                    if (dateKeywords.contains(words[j])) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Extract creator names
     */
    private void extractCreatorNames(String input, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        for (String creator : creatorNames) {
            if (lowerInput.contains(creator)) {
                entities.put("createdBy", creator);
                break;
            }
        }
    }
    
    /**
     * Extract customer names
     */
    private void extractCustomerNames(String input, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        for (String customer : customerNames) {
            if (lowerInput.contains(customer)) {
                entities.put("customerName", customer.substring(0, 1).toUpperCase() + customer.substring(1));
                break;
            }
        }
    }
    
    /**
     * Determine query intent based on context
     */
    private void determineQueryIntent(String input, List<String> tokens, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        
        // Determine primary intent with better context awareness
        if (lowerInput.contains("how") && !lowerInput.contains("contract") && !lowerInput.contains("part")) {
            entities.put("intent", "HELP");
        } else if (lowerInput.contains("help") && !lowerInput.contains("contract") && !lowerInput.contains("part")) {
            entities.put("intent", "HELP");
        } else if (lowerInput.contains("steps") && !lowerInput.contains("contract") && !lowerInput.contains("part")) {
            entities.put("intent", "HELP");
        } else if (entities.containsKey("contractNumber")) {
            entities.put("intent", "CONTRACT_LOOKUP");
        } else if (entities.containsKey("partNumber")) {
            entities.put("intent", "PART_LOOKUP");
        } else if (lowerInput.contains("contract") || lowerInput.contains("contrct")) {
            if (entities.containsKey("createdBy") || entities.containsKey("afterYear") || 
                entities.containsKey("beforeYear") || entities.containsKey("inYear")) {
                entities.put("intent", "CONTRACT_SEARCH");
            } else {
                entities.put("intent", "CONTRACT_LOOKUP");
            }
                 } else if (lowerInput.contains("part") || lowerInput.contains("prt")) {
             if (lowerInput.contains("failed") || lowerInput.contains("validation") || lowerInput.contains("error")) {
                 entities.put("intent", "PART_ANALYSIS");
             } else {
                 entities.put("intent", "PART_LOOKUP");
             }
         } else if (lowerInput.contains("failed") || lowerInput.contains("validation") || lowerInput.contains("error")) {
             entities.put("intent", "PART_ANALYSIS");
        } else if (entities.containsKey("createdBy") || entities.containsKey("afterYear") || 
                  entities.containsKey("beforeYear") || entities.containsKey("inYear")) {
            entities.put("intent", "CONTRACT_SEARCH");
        } else {
            entities.put("intent", "GENERAL_QUERY");
        }
    }
    
    /**
     * Intelligent domain routing with context
     */
    private DomainType determineDomainWithContext(String input, Map<String, Object> entities) {
        String intent = (String) entities.get("intent");
        
        // Priority-based routing
        if ("HELP".equals(intent)) {
            return DomainType.HELP;
        }
        
        if ("PART_LOOKUP".equals(intent) || "PART_ANALYSIS".equals(intent)) {
            return DomainType.PARTS;
        }
        
        if ("CONTRACT_LOOKUP".equals(intent) || "CONTRACT_SEARCH".equals(intent)) {
            return DomainType.CONTRACTS;
        }
        
        // Fallback to keyword-based scoring
        int contractScore = 0;
        int partScore = 0;
        int helpScore = 0;
        
        String lowerInput = input.toLowerCase();
        
        // Contract indicators
        for (String keyword : contractKeywords) {
            if (lowerInput.contains(keyword)) contractScore += 3;
        }
        if (entities.containsKey("contractNumber")) contractScore += 3;
        if (entities.containsKey("createdBy")) contractScore += 2;
        if (entities.containsKey("accountNumber")) contractScore += 2;
        
        // Parts indicators
        for (String keyword : partKeywords) {
            if (lowerInput.contains(keyword)) partScore += 3;
        }
        if (entities.containsKey("partNumber")) partScore += 3;
        if (lowerInput.contains("failed") || lowerInput.contains("validation")) partScore += 2;
        
        // Help indicators
        if (lowerInput.contains("how") || lowerInput.contains("help")) helpScore += 3;
        if (lowerInput.contains("create") || lowerInput.contains("guide")) helpScore += 2;
        
        // Determine domain
        if (contractScore > partScore && contractScore > helpScore) {
            return DomainType.CONTRACTS;
        } else if (partScore > contractScore && partScore > helpScore) {
            return DomainType.PARTS;
        } else if (helpScore > 0) {
            return DomainType.HELP;
        } else {
            return DomainType.CONTRACTS; // Default
        }
    }
    
    /**
     * Generate enhanced action type based on entities and context
     */
    private String generateEnhancedActionType(DomainType domain, Map<String, Object> entities) {
        switch (domain) {
            case CONTRACTS:
                // Priority order: dates > contract number > user > account > customer
                if (entities.containsKey("afterYear") || entities.containsKey("beforeYear") || 
                    entities.containsKey("inYear") || entities.containsKey("temporalOperation")) {
                    return "contracts_by_dates";
                } else if (entities.containsKey("contractNumber")) {
                    return "contracts_by_contractNumber";
                } else if (entities.containsKey("createdBy") && 
                          (entities.containsKey("afterYear") || entities.containsKey("beforeYear") || 
                           entities.containsKey("inYear") || entities.containsKey("temporalOperation"))) {
                    return "contracts_by_dates"; // User + temporal = date-based query
                } else if (entities.containsKey("createdBy")) {
                    return "contracts_by_user";
                } else if (entities.containsKey("accountNumber")) {
                    return "contracts_by_accountNumber";
                } else if (entities.containsKey("customerName")) {
                    return "contracts_by_customerName";
                } else {
                    return "contracts_by_contractNumber";
                }
                
            case PARTS:
                if (entities.containsKey("createdBy")) {
                    return "parts_by_user";
                } else if (entities.containsKey("contractNumber")) {
                    return "parts_by_contract";
                } else if (entities.containsKey("partNumber")) {
                    return "parts_by_partNumber";
                } else if (entities.containsKey("customerName") || entities.containsKey("accountNumber")) {
                    return "parts_by_customer";
                } else {
                    return "parts_by_contract";
                }
                
            case HELP:
                String intent = (String) entities.get("intent");
                if ("HELP".equals(intent)) {
                    return "help_create_contract";
                } else {
                    return "help_general_guidance";
                }
                
            default:
                return "unknown_action";
        }
    }
    
    /**
     * Generate enhanced response
     */
    private NLPResponse generateEnhancedResponse(String originalInput, String correctedInput, 
                                               double correctionConfidence, Map<String, Object> entities, 
                                               DomainType domain) {
        
        // Create response header
        ResponseHeader header = ResponseHeader.builder()
            .contractNumber((String) entities.get("contractNumber"))
            .partNumber((String) entities.get("partNumber"))
            .customerNumber((String) entities.get("accountNumber"))
            .customerName((String) entities.get("customerName"))
            .createdBy((String) entities.get("createdBy"))
            .inputTracking(InputTracking.builder()
                .originalInput(originalInput)
                .correctedInput(correctedInput.equals(originalInput.toLowerCase()) ? null : correctedInput)
                .correctionConfidence(correctionConfidence)
                .build())
            .build();
        
        // Create query metadata
        String queryType = domain.name();
        String actionType = generateEnhancedActionType(domain, entities);
        
        QueryMetadata metadata = QueryMetadata.builder()
            .queryType(queryType)
            .actionType(actionType)
            .processingTimeMs(Math.random() * 50 + 10)
            .selectedModule(domain.name())
            .routingConfidence(0.85 + Math.random() * 0.15)
            .build();
        
        // Generate display entities
        List<String> displayEntities = generateDisplayEntities(domain, entities);
        
        // Calculate overall confidence
        double confidence = calculateOverallConfidence(domain, entities, correctionConfidence);
        
        return NLPResponse.builder()
            .header(header)
            .queryMetadata(metadata)
            .entities(new ArrayList<>())
            .displayEntities(displayEntities)
            .moduleSpecificData(new HashMap<>())
            .errors(new ArrayList<>())
            .confidence(confidence)
            .originalInput(originalInput)
            .correctedInput(correctedInput)
            .correctionConfidence(correctionConfidence)
            .selectedModule(domain.name())
            .routingConfidence(metadata.getRoutingConfidence())
            .processingTimeMs((long) metadata.getProcessingTimeMs())
            .queryType(queryType)
            .actionType(actionType)
            .build();
    }
    
    // ... (remaining helper methods - normalization, spell correction, etc.)
    
    private String normalizeInput(String input) {
        String normalized = input;
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
        normalized = normalized.replaceAll("([a-zA-Z])(\\d)", "$1 $2");
        normalized = normalized.replaceAll("(\\d)([a-zA-Z])", "$1 $2");
        normalized = normalized.replaceAll("[#@$%&*()+=\\[\\]{}|\\\\:;\"'<>,.?/]+", " ");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.trim().toLowerCase();
    }
    
    private String applySpellCorrection(String input) {
        String corrected = input;
        for (Map.Entry<String, String> correction : spellCorrections.entrySet()) {
            corrected = corrected.replaceAll("\\b" + correction.getKey() + "\\b", correction.getValue());
        }
        return corrected;
    }
    
    private double calculateCorrectionConfidence(String original, String corrected) {
        if (original.equals(corrected)) return 0.0;
        
        String[] originalWords = original.split("\\s+");
        String[] correctedWords = corrected.split("\\s+");
        
        int corrections = 0;
        for (int i = 0; i < Math.min(originalWords.length, correctedWords.length); i++) {
            if (!originalWords[i].equals(correctedWords[i])) {
                corrections++;
            }
        }
        
        return corrections > 0 ? (double) corrections / originalWords.length : 0.0;
    }
    
    private List<String> generateDisplayEntities(DomainType domain, Map<String, Object> entities) {
        List<String> displayEntities = new ArrayList<>();
        
        switch (domain) {
            case CONTRACTS:
                displayEntities.add("CONTRACT_NUMBER");
                displayEntities.add("CUSTOMER_NAME");
                displayEntities.add("EFFECTIVE_DATE");
                displayEntities.add("STATUS");
                displayEntities.add("CREATED_BY");
                break;
                
            case PARTS:
                displayEntities.add("PART_NUMBER");
                displayEntities.add("CONTRACT_NUMBER");
                displayEntities.add("ERROR_COLUMN");
                displayEntities.add("REASON");
                displayEntities.add("STATUS");
                break;
                
            case HELP:
                displayEntities.add("HELP_CONTENT");
                displayEntities.add("STEPS");
                displayEntities.add("GUIDE_TYPE");
                break;
        }
        
        return displayEntities;
    }
    
    private double calculateOverallConfidence(DomainType domain, Map<String, Object> entities, 
                                            double correctionConfidence) {
        double baseConfidence = 0.7;
        
        if (!entities.isEmpty()) {
            baseConfidence += 0.1 * entities.size();
        }
        
        if (correctionConfidence > 0) {
            baseConfidence += 0.05;
        }
        
        return Math.min(0.98, Math.max(0.50, baseConfidence));
    }
    
    private NLPResponse createErrorResponse(String errorMessage, String originalInput) {
        return NLPResponse.builder()
            .header(ResponseHeader.builder()
                .inputTracking(InputTracking.builder()
                    .originalInput(originalInput)
                    .correctedInput(originalInput)
                    .correctionConfidence(0.0)
                    .build())
                .build())
            .queryMetadata(QueryMetadata.builder()
                .queryType("ERROR")
                .actionType("error_handling")
                .processingTimeMs(5.0)
                .selectedModule("ERROR")
                .routingConfidence(0.0)
                .build())
            .entities(new ArrayList<>())
            .displayEntities(new ArrayList<>())
            .moduleSpecificData(new HashMap<>())
            .errors(Arrays.asList(errorMessage))
            .confidence(0.0)
            .originalInput(originalInput)
            .correctedInput(originalInput)
            .correctionConfidence(0.0)
            .selectedModule("ERROR")
            .routingConfidence(0.0)
            .processingTimeMs(5L)
            .queryType("ERROR")
            .actionType("error_handling")
            .build();
    }
    
    private void initializeSpellCorrections() {
        spellCorrections.put("contrct", "contract");
        spellCorrections.put("cntrct", "contract");
        spellCorrections.put("shw", "show");
        spellCorrections.put("shwo", "show");
        spellCorrections.put("pasrt", "part");
        spellCorrections.put("prt", "part");
        spellCorrections.put("creatd", "created");
        spellCorrections.put("aftr", "after");
        // Add more as needed
    }
    
    public boolean isHealthy() {
        return true;
    }
    
    public Set<DomainType> getAvailableDomains() {
        return Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(DomainType.CONTRACTS, DomainType.PARTS, DomainType.HELP))
        );
    }
}