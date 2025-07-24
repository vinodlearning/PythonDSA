package com.oracle.view;



import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Domain-specific tokenizer that handles contract and parts terminology
 * Recognizes patterns like contract numbers, part numbers, and domain-specific terms
 */

public class DomainTokenizer {
    
    // Pattern definitions for domain-specific entities
    private final Pattern contractNumberPattern = Pattern.compile("\\b\\d{6,}\\b");
    private final Pattern partNumberPattern = Pattern.compile("\\b[A-Z]{2,3}\\d{3,}\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern accountNumberPattern = Pattern.compile("\\b\\d{6,12}\\b");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b");
    
    // Domain-specific dictionaries
    private final Map<String, TokenType> contractDictionary = new HashMap<>();
    private final Map<String, TokenType> partsDictionary = new HashMap<>();
    private final Map<String, TokenType> actionDictionary = new HashMap<>();
    
    public void initialize() {
        loadContractDictionary();
        loadPartsDictionary();
        loadActionDictionary();
    }
    
    /**
     * Main tokenization method
     * @param input Raw user input
     * @return TokenizedInput with classified tokens
     */
    public TokenizedInput tokenize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new TokenizedInput(Collections.emptyList(), input);
        }
        
        // Normalize input to handle extreme cases
        String normalized = normalizeInput(input);
        
        // Extract tokens with classification
        List<Token> tokens = extractTokens(normalized);
        
        // Post-process tokens for domain-specific classification
        List<Token> classifiedTokens = classifyTokens(tokens);
        
        return new TokenizedInput(classifiedTokens, input);
    }
    
    /**
     * Normalizes input to handle various edge cases from SampleDataToTest.md
     */
    private String normalizeInput(String input) {
        String normalized = input;
        
        // Handle camelCase splitting (e.g., "contractSiemensunderaccount")
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // Handle letter-number splitting (e.g., "contract123456")
        normalized = normalized.replaceAll("([a-zA-Z])(\\d)", "$1 $2");
        
        // Handle number-letter splitting (e.g., "123456contract")
        normalized = normalized.replaceAll("(\\d)([a-zA-Z])", "$1 $2");
        
        // Handle special characters and symbols
        normalized = normalized.replaceAll("[#@$%&*()+=\\[\\]{}|\\\\:;\"'<>,.?/]+", " ");
        
        // Handle multiple spaces
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Handle non-Latin characters (convert to ASCII equivalents where possible)
        normalized = handleNonLatinCharacters(normalized);
        
        return normalized.trim().toLowerCase();
    }
    
    /**
     * Handles non-Latin characters like Japanese/Chinese
     */
    private String handleNonLatinCharacters(String input) {
        // Map common non-Latin terms to English equivalents
        Map<String, String> nonLatinMap = new HashMap<>();
        nonLatinMap.put("contract", "contract");
        nonLatinMap.put("part", "part");
        
        String result = input;
        for (Map.Entry<String, String> entry : nonLatinMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
    
    /**
     * Extracts tokens from normalized input
     */
    private List<Token> extractTokens(String normalized) {
        List<Token> tokens = new ArrayList<>();
        
        // Split on whitespace and punctuation
        String[] rawTokens = normalized.split("\\s+");
        
        for (String rawToken : rawTokens) {
            if (rawToken.isEmpty()) continue;
            
            // Check for pattern matches first
            TokenType type = classifyByPattern(rawToken);
            tokens.add(new Token(rawToken, type));
        }
        
        return tokens;
    }
    
    /**
     * Classifies token by pattern matching
     */
    private TokenType classifyByPattern(String token) {
        // Check contract number pattern
        if (contractNumberPattern.matcher(token).matches()) {
            return TokenType.CONTRACT_NUMBER;
        }
        
        // Check part number pattern
        if (partNumberPattern.matcher(token).matches()) {
            return TokenType.PART_NUMBER;
        }
        
        // Check account number pattern
        if (accountNumberPattern.matcher(token).matches()) {
            return TokenType.ACCOUNT_NUMBER;
        }
        
        // Check date pattern
        if (datePattern.matcher(token).matches()) {
            return TokenType.DATE;
        }
        
        // Check if it's a number
        if (token.matches("\\d+")) {
            return TokenType.NUMBER;
        }
        
        // Default to word
        return TokenType.WORD;
    }
    
    /**
     * Classifies tokens using domain dictionaries
     */
    private List<Token> classifyTokens(List<Token> tokens) {
        List<Token> classified = new ArrayList<>();
        
        for (Token token : tokens) {
            TokenType finalType = token.getType();
            String value = token.getValue();
            
            // Check domain dictionaries if not already classified by pattern
            if (finalType == TokenType.WORD) {
                if (contractDictionary.containsKey(value)) {
                    finalType = contractDictionary.get(value);
                } else if (partsDictionary.containsKey(value)) {
                    finalType = partsDictionary.get(value);
                } else if (actionDictionary.containsKey(value)) {
                    finalType = actionDictionary.get(value);
                }
            }
            
            classified.add(new Token(value, finalType));
        }
        
        return classified;
    }
    
    /**
     * Load contract domain dictionary
     */
    private void loadContractDictionary() {
        // Contract-related terms
        contractDictionary.put("contract", TokenType.CONTRACT_KEYWORD);
        contractDictionary.put("contrct", TokenType.CONTRACT_KEYWORD);
        contractDictionary.put("cntrct", TokenType.CONTRACT_KEYWORD);
        contractDictionary.put("contrakt", TokenType.CONTRACT_KEYWORD);
        contractDictionary.put("kontract", TokenType.CONTRACT_KEYWORD);
        contractDictionary.put("contrato", TokenType.CONTRACT_KEYWORD);
        
        // Customer/Account terms
        contractDictionary.put("customer", TokenType.CUSTOMER_KEYWORD);
        contractDictionary.put("custmer", TokenType.CUSTOMER_KEYWORD);
        contractDictionary.put("cstomer", TokenType.CUSTOMER_KEYWORD);
        contractDictionary.put("account", TokenType.ACCOUNT_KEYWORD);
        contractDictionary.put("accnt", TokenType.ACCOUNT_KEYWORD);
        contractDictionary.put("acount", TokenType.ACCOUNT_KEYWORD);
        contractDictionary.put("acc", TokenType.ACCOUNT_KEYWORD);
        
        // Status terms
        contractDictionary.put("status", TokenType.STATUS_KEYWORD);
        contractDictionary.put("statuz", TokenType.STATUS_KEYWORD);
        contractDictionary.put("statuss", TokenType.STATUS_KEYWORD);
        contractDictionary.put("active", TokenType.STATUS_VALUE);
        contractDictionary.put("expired", TokenType.STATUS_VALUE);
        contractDictionary.put("exipred", TokenType.STATUS_VALUE);
        
        // Creator terms
        contractDictionary.put("created", TokenType.CREATED_KEYWORD);
        contractDictionary.put("creatd", TokenType.CREATED_KEYWORD);
        contractDictionary.put("by", TokenType.BY_KEYWORD);
        contractDictionary.put("vinod", TokenType.CREATOR_NAME);
        contractDictionary.put("mary", TokenType.CREATOR_NAME);
        
        // Date terms
        contractDictionary.put("effective", TokenType.DATE_KEYWORD);
        contractDictionary.put("efective", TokenType.DATE_KEYWORD);
        contractDictionary.put("expiration", TokenType.DATE_KEYWORD);
        contractDictionary.put("exipraion", TokenType.DATE_KEYWORD);
        contractDictionary.put("date", TokenType.DATE_KEYWORD);
    }
    
    /**
     * Load parts domain dictionary
     */
    private void loadPartsDictionary() {
        // Parts-related terms
        partsDictionary.put("part", TokenType.PART_KEYWORD);
        partsDictionary.put("prt", TokenType.PART_KEYWORD);
        partsDictionary.put("pasrt", TokenType.PART_KEYWORD);
        partsDictionary.put("prts", TokenType.PART_KEYWORD);
        partsDictionary.put("parts", TokenType.PART_KEYWORD);
        partsDictionary.put("partz", TokenType.PART_KEYWORD);
        
        // Failure/Error terms
        partsDictionary.put("failed", TokenType.FAILURE_KEYWORD);
        partsDictionary.put("faild", TokenType.FAILURE_KEYWORD);
        partsDictionary.put("fail", TokenType.FAILURE_KEYWORD);
        partsDictionary.put("error", TokenType.ERROR_KEYWORD);
        partsDictionary.put("validation", TokenType.VALIDATION_KEYWORD);
        partsDictionary.put("validdation", TokenType.VALIDATION_KEYWORD);
        partsDictionary.put("loading", TokenType.LOADING_KEYWORD);
        partsDictionary.put("loadded", TokenType.LOADING_KEYWORD);
        partsDictionary.put("loaded", TokenType.LOADING_KEYWORD);
        
        // Status terms
        partsDictionary.put("missing", TokenType.MISSING_KEYWORD);
        partsDictionary.put("misssing", TokenType.MISSING_KEYWORD);
        partsDictionary.put("rejected", TokenType.REJECTED_KEYWORD);
        partsDictionary.put("successful", TokenType.SUCCESS_KEYWORD);
        partsDictionary.put("successfull", TokenType.SUCCESS_KEYWORD);
        partsDictionary.put("passed", TokenType.SUCCESS_KEYWORD);
        partsDictionary.put("passd", TokenType.SUCCESS_KEYWORD);
        
        // Specification terms
        partsDictionary.put("specifications", TokenType.SPEC_KEYWORD);
        partsDictionary.put("specifcations", TokenType.SPEC_KEYWORD);
        partsDictionary.put("specs", TokenType.SPEC_KEYWORD);
        partsDictionary.put("datasheet", TokenType.SPEC_KEYWORD);
        partsDictionary.put("compatible", TokenType.COMPATIBILITY_KEYWORD);
        partsDictionary.put("compatble", TokenType.COMPATIBILITY_KEYWORD);
    }
    
    /**
     * Load action dictionary
     */
    private void loadActionDictionary() {
        // Action verbs
        actionDictionary.put("show", TokenType.SHOW_ACTION);
        actionDictionary.put("shwo", TokenType.SHOW_ACTION);
        actionDictionary.put("shw", TokenType.SHOW_ACTION);
        actionDictionary.put("display", TokenType.SHOW_ACTION);
        actionDictionary.put("get", TokenType.GET_ACTION);
        actionDictionary.put("giv", TokenType.GET_ACTION);
        actionDictionary.put("give", TokenType.GET_ACTION);
        actionDictionary.put("list", TokenType.LIST_ACTION);
        actionDictionary.put("lst", TokenType.LIST_ACTION);
        actionDictionary.put("find", TokenType.FIND_ACTION);
        
        // Question words
        actionDictionary.put("what", TokenType.QUESTION_WORD);
        actionDictionary.put("wht", TokenType.QUESTION_WORD);
        actionDictionary.put("wat", TokenType.QUESTION_WORD);
        actionDictionary.put("why", TokenType.QUESTION_WORD);
        actionDictionary.put("how", TokenType.QUESTION_WORD);
        actionDictionary.put("hw", TokenType.QUESTION_WORD);
        actionDictionary.put("where", TokenType.QUESTION_WORD);
        actionDictionary.put("whr", TokenType.QUESTION_WORD);
        actionDictionary.put("when", TokenType.QUESTION_WORD);
        
        // Help terms
        actionDictionary.put("help", TokenType.HELP_KEYWORD);
        actionDictionary.put("guide", TokenType.HELP_KEYWORD);
        actionDictionary.put("steps", TokenType.HELP_KEYWORD);
        actionDictionary.put("create", TokenType.CREATE_KEYWORD);
        actionDictionary.put("creat", TokenType.CREATE_KEYWORD);
        actionDictionary.put("make", TokenType.CREATE_KEYWORD);
    }
    
    /**
     * Get tokenization statistics
     */
    public TokenizationStats getStats(TokenizedInput input) {
        Map<TokenType, Integer> typeCounts = new HashMap<>();
        
        for (Token token : input.getTokens()) {
            typeCounts.merge(token.getType(), 1, Integer::sum);
        }
        
        return new TokenizationStats(
            input.getTokens().size(),
            typeCounts,
            input.getOriginalInput().length()
        );
    }
    
    /**
     * Statistics class for tokenization results
     */
    public static class TokenizationStats {
        private final int totalTokens;
        private final Map<TokenType, Integer> typeDistribution;
        private final int originalLength;
        
        public TokenizationStats(int totalTokens, Map<TokenType, Integer> typeDistribution, int originalLength) {
            this.totalTokens = totalTokens;
            this.typeDistribution = typeDistribution;
            this.originalLength = originalLength;
        }
        
        public int getTotalTokens() { return totalTokens; }
        public Map<TokenType, Integer> getTypeDistribution() { return typeDistribution; }
        public int getOriginalLength() { return originalLength; }
        
        public double getCompressionRatio() {
            return (double) totalTokens / originalLength;
        }
        
        public boolean hasContractIndicators() {
            return typeDistribution.containsKey(TokenType.CONTRACT_KEYWORD) ||
                   typeDistribution.containsKey(TokenType.CONTRACT_NUMBER);
        }
        
        public boolean hasPartIndicators() {
            return typeDistribution.containsKey(TokenType.PART_KEYWORD) ||
                   typeDistribution.containsKey(TokenType.PART_NUMBER);
        }
        
        public boolean hasHelpIndicators() {
            return typeDistribution.containsKey(TokenType.HELP_KEYWORD) ||
                   typeDistribution.containsKey(TokenType.QUESTION_WORD);
        }
    }
}