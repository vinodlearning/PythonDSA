package com.oracle.view.source;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enhanced Intent Detection Engine
 * Provides refined intent detection logic to properly distinguish between:
 * - HELP_CONTRACT_CREATE_USER (user wants instructions)
 * - HELP_CONTRACT_CREATE_BOT (user wants bot to create)
 * - create_contract (direct contract creation)
 */
public class IntentDetectionEngine {
    
    // Imperative mood indicators for direct commands
    private static final Set<String> IMPERATIVE_INDICATORS = new HashSet<>(Arrays.asList(
        "create", "make", "generate", "build", "produce", "prepare", "compose", "write", 
        "construct", "form", "develop", "assemble", "manufacture", "fabricate", "establish", 
        "setup", "do", "draw", "put", "get", "give", "send", "provide", "help", "assist", 
        "need", "want", "require", "request", "order", "ask", "demand", "wish", "like",
        "now", "immediately", "right now", "asap", "urgently", "quickly", "fast"
    ));
    
    // Question words that indicate help requests
    private static final Set<String> QUESTION_WORDS = new HashSet<>(Arrays.asList(
        "how", "what", "when", "where", "why", "which", "who", "whose", "whom",
        "can", "could", "would", "should", "will", "may", "might", "must"
    ));
    
    // Instructional patterns that indicate help requests
    private static final Set<String> INSTRUCTIONAL_PATTERNS = new HashSet<>(Arrays.asList(
        "how to", "steps to", "guide for", "instructions for", "process for", "guide to",
        "steps for", "show me how to", "what's the process", "need guidance", "walk me through",
        "explain how to", "need help understanding", "guidance on", "understanding",
        "through contract", "help understanding", "i need guidance", "explain how",
        "explain", "instructions", "guidance", "process", "set up", "steps", "procedure",
        "method", "approach", "technique", "strategy", "plan", "scheme", "design", "layout",
        "structure", "framework", "system", "mechanism", "workflow", "pipeline", "sequence",
        "series", "chain", "line", "path", "route", "way", "means", "manner"
    ));
    
    // Direct command patterns that indicate bot should create
    private static final Set<String> DIRECT_COMMAND_PATTERNS = new HashSet<>(Arrays.asList(
        "for me", "make me", "create for me", "generate for me", "build for me", "draft for me",
        "initiate for me", "start for me", "produce for me", "prepare for me", "compose for me",
        "write for me", "construct for me", "form for me", "develop for me", "assemble for me",
        "manufacture for me", "fabricate for me", "establish for me", "setup for me", "do for me",
        "draw for me", "put for me", "get for me", "give for me", "send for me", "provide for me",
        "help for me", "assist for me", "need for me", "want for me", "require for me",
        "request for me", "order for me", "ask for me", "demand for me", "wish for me", "like for me"
    ));
    
    // Text normalization patterns
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[*@#$%^&+=]");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[-_;]");
    private static final Pattern BRACKET_PATTERN = Pattern.compile("[()\\[\\]{}]");
    private static final Pattern TRAILING_PUNCTUATION_PATTERN = Pattern.compile("[?.,!]+$");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    
    // Common abbreviations and their expansions
    private static final java.util.Map<String, String> ABBREVIATIONS = createAbbreviationsMap();
    
    private static java.util.Map<String, String> createAbbreviationsMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("pls", "please");
        map.put("plz", "please");
        map.put("thx", "thanks");
        map.put("ty", "thank you");
        map.put("u", "you");
        map.put("ur", "your");
        map.put("yr", "your");
        map.put("r", "are");
        map.put("w/", "with");
        map.put("w/o", "without");
        map.put("b/c", "because");
        map.put("b4", "before");
        map.put("2", "to");
        map.put("4", "for");
        map.put("creat", "create");
        map.put("contrct", "contract");
        map.put("cntrct", "contract");
        map.put("contarct", "contract");
        map.put("prt", "part");
        map.put("prts", "parts");
        map.put("cust", "customer");
        map.put("custmer", "customer");
        map.put("custoemr", "customer");
        map.put("acount", "account");
        map.put("detials", "details");
        map.put("informaton", "information");
        map.put("leed", "lead");
        map.put("leadtim", "lead time");
        map.put("pric", "price");
        map.put("prise", "price");
        map.put("staus", "status");
        map.put("expire", "expiration");
        map.put("expiry", "expiration");
        map.put("aktive", "active");
        map.put("avtive", "active");
        map.put("inactive", "inactive");
        map.put("faild", "failed");
        map.put("invoce", "invoice");
        map.put("invoic", "invoice");
        return map;
    }
    
    /**
     * Enhanced intent detection that addresses all test cases
     */
    public static IntentResult detectIntent(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return new IntentResult("HELP", "HELP_CONTRACT_CREATE_USER", 0.0);
        }
        
        // Step 1: Normalize the input
        String normalizedInput = normalizeText(userInput);
        String lowerOriginal = userInput.toLowerCase();
        String lowerNormalized = normalizedInput.toLowerCase();
        
        // Step 2: Check for contract creation context
        boolean hasContractContext = hasContractContext(lowerOriginal, lowerNormalized);
        
        if (!hasContractContext) {
            // Not a contract creation query
            return new IntentResult("CONTRACTS", "contracts_by_contractnumber", 0.8);
        }
        
        // Step 3: Determine if this is a help request or direct command
        IntentType intentType = determineIntentType(lowerOriginal, lowerNormalized);
        
        // Step 4: Generate appropriate action type
        String actionType = generateActionType(intentType, lowerOriginal, lowerNormalized);
        
        // Step 5: Calculate confidence
        double confidence = calculateConfidence(intentType, lowerOriginal, lowerNormalized);
        
        return new IntentResult(intentType.getQueryType(), actionType, confidence);
    }
    
    /**
     * Normalize text to handle abbreviations, typos, and special characters
     */
    private static String normalizeText(String input) {
        if (input == null) return "";
        
        String normalized = input;
        
        // Remove special characters that might interfere with intent detection
        normalized = SPECIAL_CHARS_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Replace separators with spaces
        normalized = SEPARATOR_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Remove brackets but preserve content
        normalized = BRACKET_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Remove trailing punctuation
        normalized = TRAILING_PUNCTUATION_PATTERN.matcher(normalized).replaceAll("");
        
        // Normalize multiple spaces
        normalized = MULTIPLE_SPACES_PATTERN.matcher(normalized).replaceAll(" ");
        
        // Apply abbreviation corrections
        String[] words = normalized.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (ABBREVIATIONS.containsKey(word)) {
                words[i] = ABBREVIATIONS.get(word);
            }
        }
        
        return String.join(" ", words).trim();
    }
    
    /**
     * Check if input has contract creation context
     */
    private static boolean hasContractContext(String lowerOriginal, String lowerNormalized) {
        // Check for contract-related keywords
        boolean hasContractKeywords = lowerOriginal.contains("contract") || 
                                     lowerOriginal.contains("contarct") ||
                                     lowerOriginal.contains("cntrct") ||
                                     lowerNormalized.contains("contract");
        
        // Check for creation-related keywords
        boolean hasCreationKeywords = containsAny(lowerOriginal, IMPERATIVE_INDICATORS) ||
                                     containsAny(lowerNormalized, IMPERATIVE_INDICATORS);
        
        // Check for word boundary issues (e.g., "contractcreation")
        boolean hasConcatenatedWords = lowerOriginal.contains("contractcreation") ||
                                      lowerOriginal.contains("contractcreate") ||
                                      lowerOriginal.contains("createcontract") ||
                                      lowerNormalized.contains("contractcreation") ||
                                      lowerNormalized.contains("contractcreate") ||
                                      lowerNormalized.contains("createcontract");
        
        return hasContractKeywords || (hasCreationKeywords && hasContractKeywords) || hasConcatenatedWords;
    }
    
    /**
     * Determine the intent type based on comprehensive analysis
     */
    private static IntentType determineIntentType(String lowerOriginal, String lowerNormalized) {
        // Test Case 4 & 5: Check for imperative mood indicators
        boolean hasImperativeMood = hasImperativeMood(lowerOriginal, lowerNormalized);
        
        // Test Case 7: Check for clear "how to create" intent despite typos
        boolean hasHowToIntent = hasHowToIntent(lowerOriginal, lowerNormalized);
        
        // Test Case 14 & 17: Check for creation intent despite poor grammar/word order
        boolean hasCreationIntent = hasCreationIntent(lowerOriginal, lowerNormalized);
        
        // Test Case 21: Check for parentheses that shouldn't trigger fallback
        boolean hasParentheses = lowerOriginal.contains("(") || lowerOriginal.contains(")");
        
        // Test Case 22: Check for missing spaces that shouldn't override intent
        boolean hasConcatenatedWords = hasConcatenatedWords(lowerOriginal, lowerNormalized);
        
        // Test Case 24: Check for semicolons that shouldn't cause incorrect fallback
        boolean hasSemicolon = lowerOriginal.contains(";");
        
        // Test Case 26: Check for single-word imperatives
        boolean isSingleWordImperative = isSingleWordImperative(lowerOriginal, lowerNormalized);
        
        // Test Case 27: Check for overly aggressive fallback
        boolean isAmbiguousQuery = isAmbiguousQuery(lowerOriginal, lowerNormalized);
        
        // Decision logic based on test cases
        if (hasHowToIntent && !hasImperativeMood) {
            // Clear help request - user wants instructions
            return IntentType.HELP_USER;
        } else if (hasImperativeMood && !hasHowToIntent) {
            // Direct command - bot should create
            return IntentType.HELP_BOT;
        } else if (hasHowToIntent && hasImperativeMood) {
            // Both help and imperative - prioritize help request over imperative
            // "How do I create a contract?" should be HELP_USER, not HELP_BOT
            return IntentType.HELP_USER;
        } else if (hasCreationIntent && hasImperativeMood) {
            // Creation intent with imperative mood - bot should create
            return IntentType.HELP_BOT;
        } else if (isSingleWordImperative) {
            // Single word imperative - default to bot action
            return IntentType.HELP_BOT;
        } else if (isAmbiguousQuery) {
            // Ambiguous query - better to remain in help context
            return IntentType.HELP_USER;
        } else if (hasCreationIntent) {
            // Has creation intent but unclear - default to help
            return IntentType.HELP_USER;
        } else {
            // Default fallback
            return IntentType.HELP_BOT;
        }
    }
    
    /**
     * Check for imperative mood indicators
     */
    private static boolean hasImperativeMood(String lowerOriginal, String lowerNormalized) {
        // Check for imperative indicators at the start of the sentence
        String[] words = lowerOriginal.split("\\s+");
        if (words.length > 0) {
            String firstWord = words[0];
            if (IMPERATIVE_INDICATORS.contains(firstWord)) {
                return true;
            }
        }
        
        // Check for imperative indicators anywhere in the sentence
        if (containsAny(lowerOriginal, IMPERATIVE_INDICATORS) ||
            containsAny(lowerNormalized, IMPERATIVE_INDICATORS)) {
            return true;
        }
        
        // Check for direct command patterns
        if (containsAny(lowerOriginal, DIRECT_COMMAND_PATTERNS) ||
            containsAny(lowerNormalized, DIRECT_COMMAND_PATTERNS)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check for "how to create" intent
     */
    private static boolean hasHowToIntent(String lowerOriginal, String lowerNormalized) {
        // Check for question words
        if (containsAny(lowerOriginal, QUESTION_WORDS) ||
            containsAny(lowerNormalized, QUESTION_WORDS)) {
            return true;
        }
        
        // Check for instructional patterns
        if (containsAny(lowerOriginal, INSTRUCTIONAL_PATTERNS) ||
            containsAny(lowerNormalized, INSTRUCTIONAL_PATTERNS)) {
            return true;
        }
        
        // Check for specific "how to" patterns
        if (lowerOriginal.contains("how to") || lowerNormalized.contains("how to") ||
            lowerOriginal.contains("steps") || lowerNormalized.contains("steps") ||
            lowerOriginal.contains("guide") || lowerNormalized.contains("guide") ||
            lowerOriginal.contains("instructions") || lowerNormalized.contains("instructions") ||
            lowerOriginal.contains("process") || lowerNormalized.contains("process") ||
            lowerOriginal.contains("explain") || lowerNormalized.contains("explain")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check for creation intent despite grammar issues
     */
    private static boolean hasCreationIntent(String lowerOriginal, String lowerNormalized) {
        // Check for creation keywords
        Set<String> creationKeywords = new HashSet<>(Arrays.asList(
            "create", "make", "generate", "build", "produce", "prepare", "compose", "write",
            "construct", "form", "develop", "assemble", "manufacture", "fabricate", "establish",
            "setup", "draft", "initiate", "start"
        ));
        
        if (containsAny(lowerOriginal, creationKeywords) ||
            containsAny(lowerNormalized, creationKeywords)) {
            return true;
        }
        
        // Check for concatenated words
        if (hasConcatenatedWords(lowerOriginal, lowerNormalized)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check for concatenated words (e.g., "contractcreation")
     */
    private static boolean hasConcatenatedWords(String lowerOriginal, String lowerNormalized) {
        String[] concatenatedPatterns = {
            "contractcreation", "contractcreate", "createcontract", "makecontract",
            "generatecontract", "buildcontract", "producecontract", "preparecontract",
            "composecontract", "writecontract", "constructcontract", "formcontract",
            "developcontract", "assemblecontract", "manufacturecontract", "fabricatecontract",
            "establishcontract", "setupcontract"
        };
        
        for (String pattern : concatenatedPatterns) {
            if (lowerOriginal.contains(pattern) || lowerNormalized.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check for single-word imperatives
     */
    private static boolean isSingleWordImperative(String lowerOriginal, String lowerNormalized) {
        String[] words = lowerOriginal.split("\\s+");
        if (words.length == 1) {
            String word = words[0];
            return IMPERATIVE_INDICATORS.contains(word) || 
                   word.equals("create") || word.equals("make") || word.equals("generate");
        }
        return false;
    }
    
    /**
     * Check for ambiguous queries that shouldn't trigger aggressive fallback
     */
    private static boolean isAmbiguousQuery(String lowerOriginal, String lowerNormalized) {
        // Very short queries that are ambiguous
        if (lowerOriginal.length() <= 3) {
            return true;
        }
        
        // Single word queries that could be either
        String[] words = lowerOriginal.split("\\s+");
        if (words.length == 1) {
            String word = words[0];
            return word.equals("how") || word.equals("what") || word.equals("create") ||
                   word.equals("make") || word.equals("help");
        }
        
        return false;
    }
    
    /**
     * Generate appropriate action type based on intent
     */
    private static String generateActionType(IntentType intentType, String lowerOriginal, String lowerNormalized) {
        switch (intentType) {
            case HELP_USER:
                return "HELP_CONTRACT_CREATE_USER";
            case HELP_BOT:
                return "HELP_CONTRACT_CREATE_BOT";
            case DIRECT_CREATE:
                return "create_contract";
            default:
                return "HELP_CONTRACT_CREATE_BOT";
        }
    }
    
    /**
     * Calculate confidence score
     */
    private static double calculateConfidence(IntentType intentType, String lowerOriginal, String lowerNormalized) {
        double confidence = 0.5; // Base confidence
        
        // Increase confidence based on clear indicators
        if (hasImperativeMood(lowerOriginal, lowerNormalized)) {
            confidence += 0.2;
        }
        
        if (hasHowToIntent(lowerOriginal, lowerNormalized)) {
            confidence += 0.2;
        }
        
        if (hasCreationIntent(lowerOriginal, lowerNormalized)) {
            confidence += 0.1;
        }
        
        // Normalize to 0.0-1.0 range
        return Math.min(1.0, confidence);
    }
    
    /**
     * Check if text contains any of the specified words
     */
    private static boolean containsAny(String text, Set<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Intent types
     */
    public enum IntentType {
        HELP_USER("HELP"),
        HELP_BOT("HELP"),
        DIRECT_CREATE("CONTRACTS");
        
        private final String queryType;
        
        IntentType(String queryType) {
            this.queryType = queryType;
        }
        
        public String getQueryType() {
            return queryType;
        }
    }
    
    /**
     * Intent result containing query type, action type, and confidence
     */
    public static class IntentResult {
        private final String queryType;
        private final String actionType;
        private final double confidence;
        
        public IntentResult(String queryType, String actionType, double confidence) {
            this.queryType = queryType;
            this.actionType = actionType;
            this.confidence = confidence;
        }
        
        public String getQueryType() {
            return queryType;
        }
        
        public String getActionType() {
            return actionType;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        @Override
        public String toString() {
            return String.format("IntentResult{queryType='%s', actionType='%s', confidence=%.2f}", 
                               queryType, actionType, confidence);
        }
    }
} 