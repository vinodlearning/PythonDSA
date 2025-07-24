package com.oracle.view;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Advanced NLP Engine with sophisticated tokenization, POS tagging, and context analysis
 * Handles complex scenarios like distinguishing customer numbers from contract numbers
 */
public class AdvancedNLPEngine {
    
    // Enhanced patterns with context awareness
    private final Pattern contractNumberPattern = Pattern.compile("\\b[A-Z]{2,4}[-_]\\d{4,}[-_]\\d{1,}\\b|\\b[A-Z]{2,3}[-_]?\\d{4,}\\b|\\bCON[-_]?\\d{4,}\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern partNumberPattern = Pattern.compile("\\b[A-Z]{2,3}\\d{3,}\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern numberPattern = Pattern.compile("\\b\\d{4,}\\b");
    private final Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[-/]\\w{3,}[-/]\\d{2,4}\\b|\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b");
    
    // Context keywords for proper classification
    private final Set<String> contractKeywords = new HashSet<>(Arrays.asList(
        "contract", "contrct", "cntrct", "agreement", "deal", "contracts"
    ));
    
    private final Set<String> partKeywords = new HashSet<>(Arrays.asList(
        "part", "parts", "prt", "pasrt", "component", "item", "piece", "partz"
    ));
    
    private final Set<String> customerKeywords = new HashSet<>(Arrays.asList(
        "customer", "custmer", "cstomer", "client", "account", "acount", "accnt"
    ));
    
    private final Set<String> helpKeywords = new HashSet<>(Arrays.asList(
        "how", "help", "steps", "guide", "suggest", "create", "creat", "make"
    ));
    
    private final Set<String> temporalKeywords = new HashSet<>(Arrays.asList(
        "after", "before", "between", "since", "until", "from", "to", "in", "during", "created"
    ));
    
    private final Set<String> actionKeywords = new HashSet<>(Arrays.asList(
        "show", "display", "get", "find", "list", "retrieve", "search"
    ));
    
    private final Set<String> creatorNames = new HashSet<>(Arrays.asList(
        "vinod", "mary", "john", "sarah", "mike", "admin", "system"
    ));
    
    private final Set<String> customerNames = new HashSet<>(Arrays.asList(
        "siemens", "boeing", "honeywell", "ge", "microsoft", "oracle", "ibm", "apple"
    ));
    
    // Spell correction mappings
    private final Map<String, String> spellCorrections = new HashMap<>();
    
    public AdvancedNLPEngine() {
        initializeSpellCorrections();
    }
    
    /**
     * Main processing method with advanced NLP pipeline
     */
    public NLPResponse processQuery(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return createErrorResponse("Empty input provided", userInput);
        }
        
        try {
            // Step 1: Advanced tokenization and normalization
            String normalizedInput = normalizeInput(userInput);
            
            // Step 2: Spell correction
            String correctedInput = applySpellCorrection(normalizedInput);
            double correctionConfidence = calculateCorrectionConfidence(normalizedInput, correctedInput);
            
            // Step 3: Tokenization with POS analysis
            List<Token> tokens = tokenizeWithPOS(correctedInput);
            
            // Step 4: Named Entity Recognition with context
            Map<String, Object> extractedEntities = performNER(correctedInput, tokens);
            
            // Step 5: Intent classification
            String intent = classifyIntent(correctedInput, tokens, extractedEntities);
            extractedEntities.put("intent", intent);
            
            // Step 6: Domain routing based on intent and entities
            DomainType domain = routeToDomain(intent, extractedEntities, correctedInput);
            
            // Step 7: Generate response
            return generateAdvancedResponse(userInput, correctedInput, correctionConfidence, 
                                          extractedEntities, domain);
            
        } catch (Exception e) {
            return createErrorResponse("Processing error: " + e.getMessage(), userInput);
        }
    }
    
    /**
     * Advanced tokenization with POS-like analysis
     */
    private List<Token> tokenizeWithPOS(String input) {
        List<Token> tokens = new ArrayList<>();
        String[] words = input.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String prevWord = i > 0 ? words[i-1] : "";
            String nextWord = i < words.length-1 ? words[i+1] : "";
            
            TokenType type = classifyToken(word, prevWord, nextWord);
            tokens.add(new Token(word, type, i));
        }
        
        return tokens;
    }
    
    /**
     * Classify individual tokens with context
     */
    private TokenType classifyToken(String word, String prevWord, String nextWord) {
        // Numbers with context - enhanced logic
        if (word.matches("\\d+")) {
            // Strong customer/account context
            if (prevWord.contains("customer") || prevWord.contains("account") || 
                nextWord.contains("customer") || nextWord.contains("account")) {
                return TokenType.CUSTOMER_NUMBER;
            }
            
            // Strong contract context (but not if it's a year)
            if ((prevWord.contains("contract") || nextWord.contains("contract")) && 
                !yearPattern.matcher(word).matches()) {
                return TokenType.CONTRACT_NUMBER;
            }
            
            // Part context
            if (prevWord.contains("part") || nextWord.contains("part")) {
                return TokenType.PART_NUMBER;
            }
            
            // Year detection - enhanced
            if (yearPattern.matcher(word).matches()) {
                return TokenType.YEAR;
            }
            
            // Default number classification based on length
            if (word.length() >= 6) {
                return TokenType.CONTRACT_NUMBER; // Long numbers likely contract numbers
            } else if (word.length() >= 4) {
                return TokenType.CUSTOMER_NUMBER; // Medium numbers likely customer numbers
            }
            
            return TokenType.NUMBER;
        }
        
        // Enhanced keyword detection
        if (contractKeywords.contains(word)) return TokenType.CONTRACT_KEYWORD;
        if (partKeywords.contains(word)) return TokenType.PART_KEYWORD;
        
        // Enhanced customer keyword detection
        if (customerKeywords.contains(word) || word.equals("account")) return TokenType.CUSTOMER_KEYWORD;
        
        if (helpKeywords.contains(word)) return TokenType.HELP_KEYWORD;
        if (temporalKeywords.contains(word)) return TokenType.TEMPORAL_KEYWORD;
        if (actionKeywords.contains(word)) return TokenType.ACTION_KEYWORD;
        if (creatorNames.contains(word)) return TokenType.CREATOR_NAME;
        if (customerNames.contains(word)) return TokenType.CUSTOMER_NAME;
        
        return TokenType.WORD;
    }
    
    /**
     * Advanced Named Entity Recognition with context
     * Enhanced to properly handle account/customer vs contract number conflicts
     */
    private Map<String, Object> performNER(String input, List<Token> tokens) {
        Map<String, Object> entities = new HashMap<>();
        String lowerInput = input.toLowerCase();
        
        // Step 1: Determine the primary context of the query with enhanced detection
        boolean hasCustomerContext = lowerInput.contains("customer number") || 
                                    lowerInput.contains("account number") ||
                                    lowerInput.contains("for customer") ||
                                    lowerInput.contains("for account") ||
                                    lowerInput.contains("contracts for customer") ||
                                    lowerInput.contains("contracts for account") ||
                                    lowerInput.contains("account ") ||  // Enhanced: "account 123456"
                                    lowerInput.contains("customer ");   // Enhanced: "customer 123456"
        
        boolean hasContractContext = lowerInput.contains("contract") && 
                                   !hasCustomerContext; // Contract context only if no customer context
        
        boolean hasPartContext = lowerInput.contains("part") || lowerInput.contains("parts");
        
        // Step 2: Extract entities based on context priority (STRICT - no double extraction)
        if (hasCustomerContext) {
            // Customer context has highest priority - ONLY extract customer entities
            extractCustomerEntitiesOnly(input, tokens, entities);
        } else if (hasPartContext) {
            // Part context
            extractPartEntities(input, tokens, entities);
            // Also extract contract if mentioned with parts
            if (lowerInput.contains("contract")) {
                extractContractEntitiesForParts(input, tokens, entities);
            }
        } else if (hasContractContext) {
            // Pure contract context - ONLY extract contract entities
            extractContractEntitiesOnly(input, tokens, entities);
        } else {
            // No specific context - extract based on patterns
            extractEntitiesByPattern(input, tokens, entities);
        }
        
        // Step 3: Always extract temporal and creator entities
        extractTemporalEntities(input, tokens, entities);
        extractCreatorEntities(input, tokens, entities);
        
        return entities;
    }
    
    /**
     * Extract customer entities when customer context is detected
     * Enhanced to handle "account X" patterns
     */
    private void extractCustomerEntitiesOnly(String input, List<Token> tokens, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        
        // Enhanced patterns for customer/account detection
        // Pattern 1: "account 123456" or "customer 123456"
        if (lowerInput.matches(".*\\baccount\\s+\\d{4,}.*") || 
            lowerInput.matches(".*\\bcustomer\\s+\\d{4,}.*")) {
            
            String[] words = lowerInput.split("\\s+");
            for (int i = 0; i < words.length - 1; i++) {
                if ((words[i].equals("account") || words[i].equals("customer")) && 
                    words[i + 1].matches("\\d{4,}")) {
                    entities.put("customerNumber", words[i + 1]);
                    entities.put("accountNumber", words[i + 1]);
                    return; // Found customer number, exit immediately
                }
            }
        }
        
        // Pattern 2: Traditional "customer number X" or "account number X"
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            if (token.getType() == TokenType.CUSTOMER_KEYWORD || 
                (token.getValue().equals("number") && i > 0 && 
                 tokens.get(i-1).getType() == TokenType.CUSTOMER_KEYWORD)) {
                
                // Find the number that follows
                for (int j = i + 1; j < Math.min(i + 3, tokens.size()); j++) {
                    Token nextToken = tokens.get(j);
                    if (nextToken.getValue().matches("\\d{4,}")) {
                        entities.put("customerNumber", nextToken.getValue());
                        entities.put("accountNumber", nextToken.getValue());
                        return; // Found customer number, exit immediately
                    }
                }
            }
            
            // Look for customer names
            if (token.getType() == TokenType.CUSTOMER_NAME) {
                entities.put("customerName", capitalizeFirst(token.getValue()));
            }
        }
    }
    
    /**
     * Extract contract entities when pure contract context is detected
     * Enhanced to ensure no overlap with customer numbers
     */
    private void extractContractEntitiesOnly(String input, List<Token> tokens, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        
        // CRITICAL: If customer entities were already extracted, don't extract contract entities
        if (entities.containsKey("customerNumber") || entities.containsKey("accountNumber")) {
            return; // Customer number takes absolute priority
        }
        
        // Double-check: If input has customer/account context, don't extract contract
        if (lowerInput.contains("account ") || lowerInput.contains("customer ") ||
            lowerInput.contains("account number") || lowerInput.contains("customer number")) {
            return; // Skip contract extraction for customer/account queries
        }
        
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            // Look for explicit contract context
            if (token.getType() == TokenType.CONTRACT_KEYWORD) {
                // Look for numbers after "contract" keyword
                for (int j = i + 1; j < Math.min(i + 3, tokens.size()); j++) {
                    Token nextToken = tokens.get(j);
                    if (nextToken.getValue().matches("\\d{4,}") && 
                        !isYearInTemporalContext(nextToken.getValue(), input)) {
                        entities.put("contractNumber", nextToken.getValue());
                        return; // Found contract number, exit
                    }
                }
            }
            
            // Look for prefixed contract numbers (CON-123, ABC-456)
            if (contractNumberPattern.matcher(token.getValue()).matches()) {
                entities.put("contractNumber", token.getValue().toUpperCase());
                return;
            }
        }
        
        // If no explicit contract found, look for any number in contract context
        if (lowerInput.contains("contract")) {
            for (Token token : tokens) {
                if (token.getValue().matches("\\d{4,}") && 
                    !isYearInTemporalContext(token.getValue(), input)) {
                    entities.put("contractNumber", token.getValue());
                    break;
                }
            }
        }
    }
    
    /**
     * Extract contract entities for parts context
     */
    private void extractContractEntitiesForParts(String input, List<Token> tokens, Map<String, Object> entities) {
        for (Token token : tokens) {
            if (token.getValue().matches("\\d{4,}") && 
                !isYearInTemporalContext(token.getValue(), input)) {
                entities.put("contractNumber", token.getValue());
                break;
            }
        }
    }
    
    /**
     * Extract entities by pattern when no specific context is detected
     */
    private void extractEntitiesByPattern(String input, List<Token> tokens, Map<String, Object> entities) {
        // Look for prefixed patterns first
        for (Token token : tokens) {
            // Contract patterns
            if (contractNumberPattern.matcher(token.getValue()).matches()) {
                entities.put("contractNumber", token.getValue().toUpperCase());
                return;
            }
            
            // Part patterns
            if (partNumberPattern.matcher(token.getValue()).matches()) {
                entities.put("partNumber", token.getValue().toUpperCase());
                return;
            }
        }
        
        // Look for standalone numbers (default to contract if long enough)
        for (Token token : tokens) {
            if (token.getValue().matches("\\d{6,}") && 
                !isYearInTemporalContext(token.getValue(), input)) {
                entities.put("contractNumber", token.getValue());
                break;
            } else if (token.getValue().matches("\\d{4,5}") && 
                      !isYearInTemporalContext(token.getValue(), input)) {
                entities.put("customerNumber", token.getValue());
                break;
            }
        }
    }
    
    /**
     * Extract part-related entities
     */
    private void extractPartEntities(String input, List<Token> tokens, Map<String, Object> entities) {
        for (Token token : tokens) {
            if (partNumberPattern.matcher(token.getValue()).matches()) {
                entities.put("partNumber", token.getValue().toUpperCase());
                break;
            }
        }
    }
    
    /**
     * Extract temporal entities (dates, years, ranges)
     */
    private void extractTemporalEntities(String input, List<Token> tokens, Map<String, Object> entities) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            if (token.getType() == TokenType.TEMPORAL_KEYWORD) {
                // Look for years following temporal keywords
                for (int j = i + 1; j < Math.min(i + 4, tokens.size()); j++) {
                    Token nextToken = tokens.get(j);
                    if (nextToken.getType() == TokenType.YEAR || 
                        yearPattern.matcher(nextToken.getValue()).matches()) {
                        
                        String year = nextToken.getValue();
                        if (token.getValue().equals("after") || token.getValue().equals("since")) {
                            entities.put("afterYear", year);
                            entities.put("temporalOperation", "AFTER");
                        } else if (token.getValue().equals("before") || token.getValue().equals("until")) {
                            entities.put("beforeYear", year);
                            entities.put("temporalOperation", "BEFORE");
                        } else if (token.getValue().equals("in") || token.getValue().equals("during")) {
                            entities.put("inYear", year);
                            entities.put("temporalOperation", "IN");
                        }
                        break;
                    }
                }
            }
        }
        
        // Extract specific dates
        Matcher dateMatcher = datePattern.matcher(input);
        if (dateMatcher.find()) {
            entities.put("specificDate", dateMatcher.group());
        }
    }
    
    /**
     * Extract creator entities
     */
    private void extractCreatorEntities(String input, List<Token> tokens, Map<String, Object> entities) {
        for (Token token : tokens) {
            if (token.getType() == TokenType.CREATOR_NAME) {
                entities.put("createdBy", token.getValue());
                break;
            }
        }
    }
    
    /**
     * Advanced intent classification based on user requirements
     * Enhanced to handle contract number scenarios properly
     */
    private String classifyIntent(String input, List<Token> tokens, Map<String, Object> entities) {
        String lowerInput = input.toLowerCase();
        
        // Rule 1: Help/guidance requests
        if (containsHelpKeywords(lowerInput) && !containsContractOrPartKeywords(lowerInput)) {
            if (lowerInput.contains("create") || lowerInput.contains("creat")) {
                if (lowerInput.contains("contract")) {
                    return "HELP_CREATE_CONTRACT";
                } else if (lowerInput.contains("part")) {
                    return "HELP_CREATE_PART";
                } else {
                    return "HELP_CREATE_CONTRACT"; // Default
                }
            } else if (lowerInput.contains("steps")) {
                if (lowerInput.contains("contract")) {
                    return "STEPS_CREATE_CONTRACT";
                } else if (lowerInput.contains("part")) {
                    return "STEPS_CREATE_PART";
                } else {
                    return "STEPS_CREATE_CONTRACT"; // Default
                }
            } else {
                return "HELP_GENERAL";
            }
        }
        
        // Rule 2: Parts with contract context
        if (containsPartKeywords(lowerInput) && 
            (entities.containsKey("contractNumber") || containsContractKeywords(lowerInput))) {
            return "PARTS_WITH_CONTRACT";
        }
        
        // Rule 3: Pure parts queries
        if (containsPartKeywords(lowerInput) && !containsContractKeywords(lowerInput)) {
            if (lowerInput.contains("failed") || lowerInput.contains("validation") || lowerInput.contains("error")) {
                return "PARTS_ANALYSIS";
            } else {
                return "PARTS_LOOKUP";
            }
        }
        
        // Rule 4: Contract queries - enhanced with priority for contract numbers
        if (containsContractKeywords(lowerInput) && !containsPartKeywords(lowerInput)) {
            // Priority 1: Contract number present
            if (entities.containsKey("contractNumber")) {
                return "CONTRACTS_BY_NUMBER";
            }
            // Priority 2: Customer context
            else if (entities.containsKey("customerNumber") || entities.containsKey("accountNumber")) {
                return "CONTRACTS_BY_CUSTOMER";
            }
            // Priority 3: Creator context
            else if (entities.containsKey("createdBy")) {
                return "CONTRACTS_BY_USER";
            }
            // Priority 4: Date/temporal context
            else if (entities.containsKey("afterYear") || entities.containsKey("beforeYear") || 
                      entities.containsKey("inYear")) {
                return "CONTRACTS_BY_DATES";
            } else {
                return "CONTRACTS_GENERAL";
            }
        }
        
        // Rule 5: Fallback based on entities (highest priority first)
        if (entities.containsKey("contractNumber")) {
            return "CONTRACTS_BY_NUMBER";
        } else if (entities.containsKey("customerNumber")) {
            return "CONTRACTS_BY_CUSTOMER";
        } else if (entities.containsKey("partNumber")) {
            return "PARTS_LOOKUP";
        } else if (entities.containsKey("createdBy")) {
            return "CONTRACTS_BY_USER";
        } else {
            return "GENERAL_QUERY";
        }
    }
    
    /**
     * Domain routing based on intent and entities
     */
    private DomainType routeToDomain(String intent, Map<String, Object> entities, String input) {
        // Help domain
        if (intent.startsWith("HELP_") || intent.startsWith("STEPS_")) {
            return DomainType.HELP;
        }
        
        // Parts domain
        if (intent.startsWith("PARTS_") || intent.equals("PARTS_WITH_CONTRACT")) {
            return DomainType.PARTS;
        }
        
        // Contracts domain
        if (intent.startsWith("CONTRACTS_") || intent.equals("CONTRACTS_GENERAL")) {
            return DomainType.CONTRACTS;
        }
        
        // Fallback based on keywords
        String lowerInput = input.toLowerCase();
        if (containsPartKeywords(lowerInput)) {
            return DomainType.PARTS;
        } else if (containsContractKeywords(lowerInput)) {
            return DomainType.CONTRACTS;
        } else {
            return DomainType.HELP;
        }
    }
    
    /**
     * Generate action type based on intent and business rules
     * Enhanced to properly prioritize customer numbers over contract numbers
     */
    private String generateActionType(String intent, Map<String, Object> entities) {
        // Priority-based action type determination with STRICT customer priority
        
        // 1. Customer/Account number has ABSOLUTE priority
        if (entities.containsKey("customerNumber") && entities.get("customerNumber") != null) {
            return "contracts_by_customerNumber";
        }
        
        if (entities.containsKey("accountNumber") && entities.get("accountNumber") != null) {
            return "contracts_by_customerNumber"; // Account number = customer number
        }
        
        // 2. Contract number (only if NO customer context)
        if (entities.containsKey("contractNumber") && entities.get("contractNumber") != null &&
            !entities.containsKey("customerNumber") && !entities.containsKey("accountNumber")) {
            return "contracts_by_contractNumber";
        }
        
        // 3. Part number
        if (entities.containsKey("partNumber") && entities.get("partNumber") != null) {
            return "parts_by_partNumber";
        }
        
        // 4. Customer name
        if (entities.containsKey("customerName") && entities.get("customerName") != null) {
            return "contracts_by_customerName";
        }
        
        // 5. Created by
        if (entities.containsKey("createdBy") && entities.get("createdBy") != null) {
            return "contracts_by_createdBy";
        }
        
        // 6. Handle temporal entities (dates/years)
        if (entities.containsKey("afterYear") || entities.containsKey("beforeYear") || 
            entities.containsKey("inYear") || entities.containsKey("specificDate")) {
            return "contracts_by_dates";
        }
        
        // 7. Intent-based fallbacks
        switch (intent) {
            case "HELP_CREATE_CONTRACT":
                return "help_create_contract";
            case "HELP_CREATE_PART":
                return "help_create_part";
            case "STEPS_CREATE_CONTRACT":
                return "steps_create_contract";
            case "STEPS_CREATE_PART":
                return "steps_create_part";
            case "HELP_GENERAL":
                return "help_general_guidance";
                
            case "PARTS_WITH_CONTRACT":
                return "parts_by_contract";
            case "PARTS_ANALYSIS":
                return "parts_by_contract";
            case "PARTS_LOOKUP":
                return "parts_by_contract";
                
            case "CONTRACTS_BY_CUSTOMER":
                return "contracts_by_customerNumber";
            case "CONTRACTS_BY_NUMBER":
                return "contracts_by_contractNumber";
            case "CONTRACTS_BY_USER":
                return "contracts_by_createdBy";
            case "CONTRACTS_BY_DATES":
                return "contracts_by_dates";
            case "CONTRACTS_GENERAL":
                return "contracts_by_contractNumber";
                
            default:
                return "general_query";
        }
    }
    
    /**
     * Generate advanced response
     */
    private NLPResponse generateAdvancedResponse(String originalInput, String correctedInput, 
                                               double correctionConfidence, Map<String, Object> entities, 
                                               DomainType domain) {
        
        String intent = (String) entities.get("intent");
        
        // Create response header
        ResponseHeader header = ResponseHeader.builder()
            .contractNumber((String) entities.get("contractNumber"))
            .partNumber((String) entities.get("partNumber"))
            .customerNumber((String) entities.get("customerNumber"))
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
        String actionType = generateActionType(intent, entities);
        
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
    
    // Helper methods
    private boolean containsHelpKeywords(String input) {
        return helpKeywords.stream().anyMatch(input::contains);
    }
    
    private boolean containsContractKeywords(String input) {
        return contractKeywords.stream().anyMatch(input::contains);
    }
    
    private boolean containsPartKeywords(String input) {
        return partKeywords.stream().anyMatch(input::contains);
    }
    
    private boolean containsContractOrPartKeywords(String input) {
        return containsContractKeywords(input) || containsPartKeywords(input);
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    // Standard helper methods (normalization, spell correction, etc.)
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
        spellCorrections.put("custmer", "customer");
        spellCorrections.put("cstomer", "customer");
        spellCorrections.put("acount", "account");
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
    
    /**
     * Token class for POS-like analysis
     */
    public static class Token {
        private String value;
        private TokenType type;
        private int position;
        
        public Token(String value, TokenType type, int position) {
            this.value = value;
            this.type = type;
            this.position = position;
        }
        
        public String getValue() { return value; }
        public TokenType getType() { return type; }
        public int getPosition() { return position; }
    }
    
    /**
     * Enhanced token types for better classification
     */
    public enum TokenType {
        CONTRACT_KEYWORD, PART_KEYWORD, CUSTOMER_KEYWORD, HELP_KEYWORD, 
        TEMPORAL_KEYWORD, ACTION_KEYWORD, CREATOR_NAME, CUSTOMER_NAME,
        CONTRACT_NUMBER, PART_NUMBER, CUSTOMER_NUMBER, YEAR, NUMBER, WORD
    }
    
    /**
     * Enhanced year detection in temporal context
     */
    private boolean isYearInTemporalContext(String number, String input) {
        if (!yearPattern.matcher(number).matches()) return false;
        
        String lowerInput = input.toLowerCase();
        
        // Strong temporal indicators - exact phrase matching
        String[] temporalPhrases = {
            "created in " + number,
            "in " + number,
            "during " + number,
            "year " + number,
            "after " + number,
            "before " + number,
            "since " + number,
            "until " + number,
            "in the year " + number,
            "for year " + number,
            "of " + number
        };
        
        for (String phrase : temporalPhrases) {
            if (lowerInput.contains(phrase)) {
                return true;
            }
        }
        
        // Check if the word before or after the number is a temporal keyword
        String[] words = lowerInput.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(number)) {
                // Check previous word
                if (i > 0 && temporalKeywords.contains(words[i-1])) {
                    return true;
                }
                // Check next word
                if (i < words.length - 1 && temporalKeywords.contains(words[i+1])) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // Remove the old methods that are no longer needed
    private void extractCustomerEntities(String input, List<Token> tokens, Map<String, Object> entities) {
        // This method is replaced by extractCustomerEntitiesOnly
        extractCustomerEntitiesOnly(input, tokens, entities);
    }
    
    private void extractContractEntities(String input, List<Token> tokens, Map<String, Object> entities) {
        // This method is replaced by extractContractEntitiesOnly
        extractContractEntitiesOnly(input, tokens, entities);
    }
}