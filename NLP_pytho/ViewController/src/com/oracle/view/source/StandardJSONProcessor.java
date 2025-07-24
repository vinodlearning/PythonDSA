package com.oracle.view.source;

// import com.oracle.view.deep.TestQueries;

import com.oracle.view.deep.TestQueries;

import com.oracle.view.source.StandardJSONProcessor.QueryResult;

import java.io.FileWriter;
import java.io.IOException;

import java.io.PrintWriter;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Standard JSON Processor
 * Follows JSON_DESIGN.md standards and architecture_design.md requirements
 * Returns only the required JSON format with inputTracking
 */
public class StandardJSONProcessor {

    // Business rule patterns
    private static final Pattern CONTRACT_NUMBER_PATTERN = Pattern.compile("\\d{6,}");
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("[A-Za-z0-9]{3,}");
    private static final Pattern CUSTOMER_NUMBER_PATTERN = Pattern.compile("\\d{4,8}");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    // ENHANCED: Multiple part number patterns for different formats
    private static final Pattern PART_PATTERN_1 = Pattern.compile("\\b[A-Za-z]{2}\\d{5}\\b"); // AE12345
    private static final Pattern PART_PATTERN_2 = Pattern.compile("\\b[A-Za-z]{2}\\d{4,6}\\b"); // BC67890, DE23456
    private static final Pattern PART_PATTERN_3 = Pattern.compile("\\b[A-Za-z]\\d{4,8}\\b"); // A12345678
    private static final Pattern PART_PATTERN_4 = Pattern.compile("\\b\\d{4,8}[A-Za-z]{1,3}\\b"); // 12345ABC
    private static final Pattern PART_PATTERN_5 = Pattern.compile("\\b[A-Za-z]{3,4}-\\d{3,6}\\b"); // ABC-123456

    // FIXED: Standardized action type constants
    private static final String PARTS_BY_PART_NUMBER = "parts_by_part_number";
    private static final String PARTS_BY_CONTRACT_NUMBER = "parts_by_contract_number";
    private static final String CONTRACTS_BY_CONTRACT_NUMBER = "contracts_by_contractnumber";
    private static final String CONTRACTS_BY_FILTER = "contracts_by_filter";
    private static final String FAILED_PARTS_BY_CONTRACT_NUMBER = "parts_failed_by_contract_number";
    private static final String FAILED_PARTS_BY_FILTER = "parts_failed_by_filter";
    private static final String HELP_CONTRACT_CREATE_BOT = "HELP_CONTRACT_CREATE_BOT";
    private static final String HELP_CONTRACT_CREATE_USER = "HELP_CONTRACT_CREATE_USER";

    // Field names validation - NOW USING CENTRALIZED CONFIG
    // FIELD_NAMES are now managed by TableColumnConfig and validated through isValidColumn()

    // Enhanced spell corrections using WordDatabase + SpellCorrector add-on
    private static final Map<String, String> SPELL_CORRECTIONS = WordDatabase.getSpellCorrections();
    private static final SpellCorrector SPELL_CORRECTOR = new SpellCorrector();

    // Business term mappings - NOW USING CENTRALIZED CONFIG
    // BUSINESS_FILTER_TERMS and ENHANCED_BUSINESS_TERMS are now managed by TableColumnConfig

    // Table column mappings for validation - NOW USING CENTRALIZED CONFIG
    private static final TableColumnConfig TABLE_CONFIG = TableColumnConfig.getInstance();

    /**
     * Validate if a column exists in the specified table
     */
    private static boolean isValidColumn(String columnName, String tableType) {
        if (columnName == null)
            return false;

        return TABLE_CONFIG.isValidColumn(tableType, columnName);
    }

    // Spell corrections now handled by WordDatabase

    /**
     * Normalize user input for prompt-action mapping
     */
    private static String normalizePrompt(String input) {
        if (input == null)
            return null;
        
        // Use enhanced NLP processor for comprehensive normalization
        return EnhancedNLPProcessor.normalizeText(input);
    }


    /**
     * Process query and return Java object for easy access
     * Converts JSON string to QueryResult object
     */
    public QueryResult processQueryToObject(String originalInput) {
        String jsonString = processQuery(originalInput);
        return parseJSONToObject(jsonString);
    }

    /**
     * Parse JSON string to QueryResult object
     */
    public QueryResult parseJSONToObject(String jsonString) {
        try {
            // Simple JSON parsing without external libraries
            QueryResult result = new QueryResult();

            // Extract main sections
            result.inputTracking = parseInputTracking(jsonString);
            result.header = parseHeader(jsonString);
            result.metadata = parseMetadata(jsonString);
            result.entities = parseEntities(jsonString);
            result.displayEntities = parseDisplayEntities(jsonString);
            result.errors = parseErrors(jsonString);

            return result;

        } catch (Exception e) {
            // Return error result
            QueryResult errorResult = new QueryResult();
            errorResult.metadata = new QueryMetadata("ERROR", "PARSE_ERROR", 0.0);
            errorResult.errors =
                Arrays.asList(new ValidationError("PARSE_ERROR", "Failed to parse JSON: " + e.getMessage(), "ERROR"));
            return errorResult;
        }
    }

    /**
     * Parse inputTracking section from JSON
     */
    private InputTrackingResult parseInputTracking(String json) {
        // Extract the inputTracking section which is nested inside header
        String inputTrackingSection = extractNestedJSONObject(json, "header", "inputTracking");

        if (inputTrackingSection == null) {
            // Fallback: try to find at root level
            String originalInput = extractJSONValue(json, "originalInput");
            String correctedInput = extractJSONValue(json, "correctedInput");
            String confidenceStr = extractJSONValue(json, "correctionConfidence");

            double confidence = 0.0;
            if (confidenceStr != null && !confidenceStr.equals("null")) {
                try {
                    confidence = Double.parseDouble(confidenceStr);
                } catch (NumberFormatException e) {
                    confidence = 0.0;
                }
            }

            return new InputTrackingResult(originalInput, correctedInput, confidence);
        }

        // Parse from the extracted inputTracking section
        String originalInput = extractJSONValue(inputTrackingSection, "originalInput");
        String correctedInput = extractJSONValue(inputTrackingSection, "correctedInput");
        String confidenceStr = extractJSONValue(inputTrackingSection, "correctionConfidence");

        double confidence = 0.0;
        if (confidenceStr != null && !confidenceStr.equals("null")) {
            try {
                confidence = Double.parseDouble(confidenceStr);
            } catch (NumberFormatException e) {
                confidence = 0.0;
            }
        }

        return new InputTrackingResult(originalInput, correctedInput, confidence);
    }

    /**
     * Parse header section from JSON
     */
    private Header parseHeader(String json) {
        // Extract the header section first
        String headerSection = extractJSONObject(json, "header");

        if (headerSection == null) {
            // Fallback: try to find at root level
            Header header = new Header();
            header.contractNumber = extractJSONValue(json, "contractNumber");
            header.partNumber = extractJSONValue(json, "partNumber");
            header.customerNumber = extractJSONValue(json, "customerNumber");
            header.customerName = extractJSONValue(json, "customerName");
            header.createdBy = extractJSONValue(json, "createdBy");

            // Handle null values
            if ("null".equals(header.contractNumber))
                header.contractNumber = null;
            if ("null".equals(header.partNumber))
                header.partNumber = null;
            if ("null".equals(header.customerNumber))
                header.customerNumber = null;
            if ("null".equals(header.customerName))
                header.customerName = null;
            if ("null".equals(header.createdBy))
                header.createdBy = null;

            return header;
        }

        // Parse from the extracted header section
        Header header = new Header();
        header.contractNumber = extractJSONValue(headerSection, "contractNumber");
        header.partNumber = extractJSONValue(headerSection, "partNumber");
        header.customerNumber = extractJSONValue(headerSection, "customerNumber");
        header.customerName = extractJSONValue(headerSection, "customerName");
        header.createdBy = extractJSONValue(headerSection, "createdBy");

        // Handle null values
        if ("null".equals(header.contractNumber))
            header.contractNumber = null;
        if ("null".equals(header.partNumber))
            header.partNumber = null;
        if ("null".equals(header.customerNumber))
            header.customerNumber = null;
        if ("null".equals(header.customerName))
            header.customerName = null;
        if ("null".equals(header.createdBy))
            header.createdBy = null;

        return header;
    }

    /**
     * Parse metadata section from JSON
     */
    private QueryMetadata parseMetadata(String json) {
        String queryType = extractJSONValue(json, "queryType");
        String actionType = extractJSONValue(json, "actionType");
        String processingTimeStr = extractJSONValue(json, "processingTimeMs");

        double processingTime = 0.0;
        if (processingTimeStr != null && !processingTimeStr.equals("null")) {
            try {
                processingTime = Double.parseDouble(processingTimeStr);
            } catch (NumberFormatException e) {
                processingTime = 0.0;
            }
        }

        return new QueryMetadata(queryType, actionType, processingTime);
    }

    /**
     * Parse entities array from JSON
     */
    private List<EntityFilter> parseEntities(String json) {
        List<EntityFilter> entities = new ArrayList<>();

        // Find entities array
        String entitiesSection = extractJSONArray(json, "entities");
        if (entitiesSection != null) {
            // Parse each entity object
            String[] entityObjects = splitJSONObjects(entitiesSection);
            for (String entityObj : entityObjects) {
                String attribute = extractJSONValue(entityObj, "attribute");
                String operation = extractJSONValue(entityObj, "operation");
                String value = extractJSONValue(entityObj, "value");
                String source = extractJSONValue(entityObj, "source");

                entities.add(new EntityFilter(attribute, operation, value, source));
            }
        }

        return entities;
    }

    /**
     * Parse displayEntities array from JSON
     */
    private List<String> parseDisplayEntities(String json) {
        List<String> displayEntities = new ArrayList<>();

        String displaySection = extractJSONArray(json, "displayEntities");
        if (displaySection != null) {
            String[] items = splitJSONArrayItems(displaySection);
            for (String item : items) {
                displayEntities.add(item.replace("\"", ""));
            }
        }

        return displayEntities;
    }

    /**
     * Parse errors array from JSON
     */
    private List<ValidationError> parseErrors(String json) {
        List<ValidationError> errors = new ArrayList<>();

        String errorsSection = extractJSONArray(json, "errors");
        if (errorsSection != null) {
            String[] errorObjects = splitJSONObjects(errorsSection);
            for (String errorObj : errorObjects) {
                String code = extractJSONValue(errorObj, "code");
                String message = extractJSONValue(errorObj, "message");
                String severity = extractJSONValue(errorObj, "severity");

                errors.add(new ValidationError(code, message, severity));
            }
        }

        return errors;
    }

    /**
     * Extract JSON value by key
     */
    private String extractJSONValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"|\"" + key + "\"\\s*:\\s*([^,}\\]]+)";
        java.util.regex.Pattern p = java.util
                                        .regex
                                        .Pattern
                                        .compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2).trim();
        }

        return null;
    }

    /**
     * Extract JSON array by key
     */
    private String extractJSONArray(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]*(?:\\[[^\\]]*\\][^\\]]*)*)\\]";
        java.util.regex.Pattern p = java.util
                                        .regex
                                        .Pattern
                                        .compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    /**
     * Split JSON objects in array
     */
    private String[] splitJSONObjects(String arrayContent) {
        if (arrayContent == null || arrayContent.trim().isEmpty()) {
            return new String[0];
        }

        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(arrayContent.substring(start, i + 1));
                    start = i + 1;
                    // Skip comma and whitespace
                    while (start < arrayContent.length() &&
                           (arrayContent.charAt(start) == ',' || Character.isWhitespace(arrayContent.charAt(start)))) {
                        start++;
                    }
                    i = start - 1; // -1 because loop will increment
                }
            }
        }

        return objects.toArray(new String[0]);
    }

    /**
     * Split JSON array items (for simple arrays)
     */
    private String[] splitJSONArrayItems(String arrayContent) {
        if (arrayContent == null || arrayContent.trim().isEmpty()) {
            return new String[0];
        }

        return arrayContent.split(",\\s*");
    }

    /**
     * Extract JSON object by key
     */
    private String extractJSONObject(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\{([^}]*(?:\\{[^}]*\\}[^}]*)*)\\}";
        java.util.regex.Pattern p = java.util
                                        .regex
                                        .Pattern
                                        .compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    /**
     * Extract nested JSON object (parent.child)
     */
    private String extractNestedJSONObject(String json, String parentKey, String childKey) {
        // First extract the parent object
        String parentObject = extractJSONObject(json, parentKey);
        if (parentObject == null) {
            return null;
        }

        // Then extract the child object from the parent
        return extractJSONObject("{" + parentObject + "}", childKey);
    }

    /**
     * Enhanced input tracking with better spell correction
     * Now also corrects common prompt misspellings (e.g., cntract, contarct, etc.)
     */

    /**
     * Enhanced input tracking with comprehensive spell correction
     * Now includes SpellCorrector add-on for better accuracy
     */
    private InputTrackingResult processInputTracking(String originalInput) {
        if (originalInput == null || originalInput.trim().isEmpty()) {
            return new InputTrackingResult(originalInput, originalInput, 0.0);
        }

        // STEP 1: Apply SpellCorrector add-on for comprehensive spell correction
        String spellCorrectedInput = SPELL_CORRECTOR.correct(originalInput);
        boolean spellCorrectorMadeChanges = !spellCorrectedInput.equals(originalInput);
        
        // STEP 2: Apply existing WordDatabase spell correction logic
        String normalizedInput = normalizePrompt(spellCorrectedInput);
        String[] words = normalizedInput.split("\\s+");
        StringBuilder correctedBuilder = new StringBuilder();
        boolean hasCorrections = spellCorrectorMadeChanges; // Start with SpellCorrector changes
        int totalWords = words.length;
        int correctedWords = spellCorrectorMadeChanges ? 1 : 0; // Count SpellCorrector as one correction

        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-zA-Z0-9]", ""); // Remove special chars for lookup
            String originalWord = words[i]; // Keep original with punctuation

            // Enhanced special handling for common patterns
            if (i > 0 && "created".equals(words[i - 1].replaceAll("[^a-zA-Z0-9]", "")) && "buy".equals(word)) {
                correctedBuilder.append("by");
                hasCorrections = true;
                correctedWords++;
            }
            // Handle number substitutions (e.g., "4" -> "for", "2" -> "to")
            else if (word.equals("4") && i > 0) {
                correctedBuilder.append("for");
                hasCorrections = true;
                correctedWords++;
            } else if (word.equals("2")) {
                correctedBuilder.append("to");
                hasCorrections = true;
                correctedWords++;
            }
            // CRITICAL FIX: Preserve "no" when it's part of "no data"
            else if (word.equals("no") && 
                     (originalInput.toLowerCase().contains("no data") || 
                      originalInput.toLowerCase().contains("no information"))) {
                // Keep "no" as is when it's part of "no data"
                correctedBuilder.append("no");
                hasCorrections = false; // Not really a correction, just preservation
            }
            // ENHANCED: Contextual spell correction for "expire" vs "expiration"
            else if (word.equals("expiration") && 
                     (originalInput.toLowerCase().contains("when") || originalInput.toLowerCase().contains("does"))) {
                // Keep "expire" when used as a verb in questions
                correctedBuilder.append("expire");
                hasCorrections = true;
                correctedWords++;
            }
            // CRITICAL FIX: Preserve "no data" - don't change to "number data"
            else if (word.equals("number") && originalInput.toLowerCase().contains("no data")) {
                // Keep "no data" as is - don't change to "number data"
                correctedBuilder.append("no");
                hasCorrections = true;
                correctedWords++;
            }
            // CRITICAL FIX: Handle "faild" correction
            else if (word.equals("faild")) {
                correctedBuilder.append("failed");
                hasCorrections = true;
                correctedWords++;
            }
            // Enhanced spell correction using WordDatabase
            else if (SPELL_CORRECTIONS.containsKey(word)) {
                // Preserve original punctuation
                String correction = SPELL_CORRECTIONS.get(word);
                String punctuation = originalWord.replaceAll("[a-zA-Z0-9]", "");
                correctedBuilder.append(correction).append(punctuation);
                hasCorrections = true;
                correctedWords++;
                
                // Also check for word boundary corrections
                String boundaryCorrection = WordDatabase.getWordBoundaryCorrection(word);
                if (boundaryCorrection != null) {
                    // Apply boundary correction as additional processing
                    String[] boundaryWords = boundaryCorrection.split("\\s+");
                    if (boundaryWords.length >
                        1) {
                        // This is a multi-word correction, apply it
                        correctedBuilder.setLength(correctedBuilder.length() - correction.length() -
                                                   punctuation.length());
                        correctedBuilder.append(boundaryCorrection).append(punctuation);
                    }
                }
            } else {
                correctedBuilder.append(originalWord); // Keep original with special chars
            }

            if (i < words.length - 1) {
                correctedBuilder.append(" ");
            }
        }

        // If no corrections, set correctedInput to originalInput (not null)
        String correctedInput = hasCorrections ? correctedBuilder.toString() : originalInput;
        
        // Enhanced confidence calculation: consider both SpellCorrector and WordDatabase corrections
        double confidence = totalWords > 0 ? Math.min(1.0, (double) correctedWords / totalWords + 0.1) : 0.0;

        // Apply additional spell corrections for zero-defect
        String corrected = originalInput;
        Map<String, String> additionalCorrections = new HashMap<>();
        additionalCorrections.put("contrat", "contract");
        additionalCorrections.put("experation", "expiration");
        additionalCorrections.put("expiry", "expiration");
        for (Map.Entry<String, String> entry : additionalCorrections.entrySet()) {
            corrected = corrected.replaceAll("\\b" + java.util
                                                         .regex
                                                         .Pattern
                                                         .quote(entry.getKey()) + "\\b", entry.getValue());
        }
        // Now apply the main spell corrector logic
        String finalCorrected = SPELL_CORRECTOR != null ? SPELL_CORRECTOR.correct(corrected) : corrected;
        // Enhanced confidence calculation: consider both SpellCorrector and WordDatabase corrections
        double confidence1 = totalWords > 0 ? Math.min(1.0, (double) correctedWords / totalWords + 0.1) : 0.0;
        return new InputTrackingResult(originalInput, finalCorrected, confidence1);
    }

    private String extractCreatorNameEnhanced(String input) {
        // Pattern 1: "created by [name]"
        Pattern creatorPattern1 = Pattern.compile("created\\s+by\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = creatorPattern1.matcher(input);
        if (matcher1.find()) {
            return matcher1.group(1);
        }

        // Pattern 2: "by [name]" (when "created" appears earlier)
        if (input.contains("created")) {
            Pattern creatorPattern2 = Pattern.compile("\\bby\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher2 = creatorPattern2.matcher(input);
            if (matcher2.find()) {
                return matcher2.group(1);
            }
        }

        // Pattern 3: Handle cases where "created" and "by" are separated by other words
        Pattern creatorPattern3 = Pattern.compile("created\\s+\\w+\\s+by\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = creatorPattern3.matcher(input);
        if (matcher3.find()) {
            return matcher3.group(1);
        }

        return null;
    }


    /**
     * Enhanced header analysis that handles all the failed cases
     * Improved to extract contract/part numbers from conversational queries.
     */
    private HeaderResult analyzeHeaders(String input) {
        Header header = new Header();
        List<String> issues = new ArrayList<>();

        String cleanInput = input.toLowerCase().trim();
        String[] tokens = tokenizeInput(cleanInput);

        boolean hasCustomerContext =
            Arrays.stream(tokens).anyMatch(WordDatabase.getCustomerContextWords()::contains) ||
            cleanInput.contains("account name") || cleanInput.contains("customer name");
        boolean hasCreatorContext =
            cleanInput.contains("created by") || cleanInput.contains("by ") ||
            cleanInput.matches(".*created\\s+.*by\\s+\\w+.*");
        boolean hasContractContext = cleanInput.contains("contract");

        // Check for filter contexts to avoid treating filter values as contract numbers
        boolean hasFilterContext =
            cleanInput.matches(".*\\b(>|<|=|between|after|before|with|where|by|type|status|moq|rebate|minimum|fee|expir|active|failed|program|vmi|cmi|kitting|pl_3|hpp|unpriced|customer|price)\\b.*");

        // 1. Try to extract contract/part numbers from patterns like 'part X in contract Y'
        java.util.regex.Matcher partInContract = java.util
                                                     .regex
                                                     .Pattern
                                                     .compile("part\\s+([A-Za-z0-9_/-]+).*contract\\s+(\\d{3,})")
                                                     .matcher(cleanInput);
        if (partInContract.find()) {
            String partNum = partInContract.group(1);
            String contractNum = partInContract.group(2);
            if (partNum != null && partNum.length() >= 3)
                header.partNumber = partNum.toUpperCase();
            if (contractNum != null && contractNum.length() >= 6)
                header.contractNumber = contractNum;
        }

        // 2. Fallback to token-based extraction
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty() || WordDatabase.isCommandWord(token) ||
                isValidColumn(token.toUpperCase(), TableColumnConfig.TABLE_CONTRACTS)) {
                continue;
            }
            if (YEAR_PATTERN.matcher(token).matches()) {
                continue;
            }
            if (token.startsWith("contract")) {
                String numberPart = token.substring("contract".length());
                if (!numberPart.isEmpty() && CONTRACT_NUMBER_PATTERN.matcher(numberPart).matches()) {
                    header.contractNumber = numberPart;
                } else if (!numberPart.isEmpty() && !numberPart.matches("\\d+")) {
                    if (numberPart.matches("\\d+")) {
                        issues.add("Contract number '" + numberPart + "' must be 6+ digits");
                    }
                }
            } else if (token.startsWith("part")) {
                String numberPart = token.substring("part".length());
                if (!numberPart.isEmpty() && PART_NUMBER_PATTERN.matcher(numberPart).matches() &&
                    !isValidColumn(numberPart.toUpperCase(), TableColumnConfig.TABLE_CONTRACTS)) {
                    header.partNumber = numberPart.toUpperCase();
                } else if (!numberPart.isEmpty()) {
                    issues.add("Part number '" + numberPart + "' must be 3+ alphanumeric characters");
                }
            } else if (token.startsWith("customer")) {
                String numberPart = token.substring("customer".length());
                if (!numberPart.isEmpty() && CUSTOMER_NUMBER_PATTERN.matcher(numberPart).matches()) {
                    header.customerNumber = numberPart;
                } else if (!numberPart.isEmpty()) {
                    issues.add("Customer number '" + numberPart + "' must be 4-8 digits");
                }
            } else if (token.matches("\\d+")) {
                // Don't treat filter values as contract numbers
                if (hasFilterContext && token.length() < 6) {
                    // This is likely a filter value, not a contract number
                    continue;
                }

                if (hasCustomerContext && CUSTOMER_NUMBER_PATTERN.matcher(token).matches()) {
                    header.customerNumber = token;
                } else if (hasContractContext && token.length() >= 6 && header.contractNumber == null) {
                    header.contractNumber = token;
                } else if (hasContractContext && token.length() >= 3 && header.contractNumber == null) {
                    header.contractNumber = token;
                } else if (token.length() >= 6 && header.contractNumber == null) {
                    header.contractNumber = token;
                } else if (token.length() >= 4 && token.length() <= 8 && header.customerNumber == null) {
                    if (!hasContractContext || hasCustomerContext) {
                        header.customerNumber = token;
                    }
                }
            } else if ((token.matches("[A-Za-z0-9_-]+") && token.length() >= 3) || token.matches("[A-Z]{2,3}\\d+")) {
                if ((containsLettersAndNumbers(token) || token.equals(token.toUpperCase()) ||
                     token.matches("[A-Z]{2,3}\\d+") || token.contains("_") || token.contains("-")) &&
                    !isValidColumn(token.toUpperCase(), TableColumnConfig.TABLE_CONTRACTS)) {
                    if (!hasContractContext || !token.matches("\\d+[a-zA-Z]+")) {
                        header.partNumber = token.toUpperCase();
                    }
                }
            }
        }

        // 3. If contract number is still null, extract first 6+ digit number from input
        if (header.contractNumber == null) {
            java.util.regex.Matcher contractNumMatcher = java.util
                                                             .regex
                                                             .Pattern
                                                             .compile("\\b(\\d{6,})\\b")
                                                             .matcher(cleanInput);
            if (contractNumMatcher.find()) {
                String potentialContractNum = contractNumMatcher.group(1);
                // Don't use filter values as contract numbers
                if (!hasFilterContext || potentialContractNum.length() >= 6) {
                    header.contractNumber = potentialContractNum;
                }
            }
        }

        if (hasCreatorContext) {
            String creatorName = extractCreatorNameEnhanced(cleanInput);
            if (creatorName != null) {
                header.createdBy = creatorName;
            }
        }
        String customerName = extractCustomerName(cleanInput);
        if (customerName != null) {
            header.customerName = customerName;
        }
        return new HeaderResult(header, issues);
    }

    /**
     * Enhanced tokenization to handle concatenated words and special formats
     */
    private String[] tokenizeInput(String input) {
        List<String> tokens = new ArrayList<>();

        // First split by standard delimiters
        String[] primaryTokens = input.split("[;\\s,&@#\\$\\|\\+\\-\\*\\/\\(\\)\\[\\]\\{\\}\\?\\!\\:\\.=]+");

        for (String token : primaryTokens) {
            if (token.trim().isEmpty())
                continue;

            // Handle concatenated patterns like "contract123sumry", "customer897654contracts"
            List<String> subTokens = splitConcatenatedWords(token.trim());

            // Apply splitting recursively to sub-tokens if needed
            List<String> finalSubTokens = new ArrayList<>();
            for (String subToken : subTokens) {
                if (subToken.matches("\\d+[a-zA-Z]+")) {
                    // Further split number+suffix patterns
                    List<String> furtherSplit = splitConcatenatedWords(subToken);
                    finalSubTokens.addAll(furtherSplit);
                } else {
                    finalSubTokens.add(subToken);
                }
            }

            tokens.addAll(finalSubTokens);
        }

        return tokens.toArray(new String[0]);
    }

    /**
     * Split concatenated words like "contract123sumry" into ["contract", "123", "sumry"]
     */
    private List<String> splitConcatenatedWords(String word) {
        List<String> result = new ArrayList<>();

        // Handle specific complex patterns first

        // Pattern 1: "contractSiemensunderaccount" -> ["contract", "siemens", "under", "account"]
        if (word.matches("contract[a-zA-Z]+")) {
            result.add("contract");
            String remainder = word.substring("contract".length());
            result.addAll(splitByKnownWords(remainder));
            return result;
        }

        // Pattern for "contract" + number + suffix (like "456789status")
        if (word.matches("\\d+[a-zA-Z]+")) {
            Pattern numberSuffixPattern = Pattern.compile("(\\d+)([a-zA-Z]+)");
            java.util.regex.Matcher matcher = numberSuffixPattern.matcher(word);
            if (matcher.matches()) {
                result.add(matcher.group(1)); // number part
                result.add(matcher.group(2)); // suffix part
                return result;
            }
        }

        // Pattern 2: "customernumber123456contract" -> ["customer", "number", "123456", "contract"]
        if (word.matches("customer[a-zA-Z]*\\d+[a-zA-Z]*")) {
            Pattern pattern = Pattern.compile("(customer)([a-zA-Z]*)(\\d+)([a-zA-Z]*)");
            java.util.regex.Matcher matcher = pattern.matcher(word);
            if (matcher.matches()) {
                result.add(matcher.group(1)); // "customer"
                if (!matcher.group(2).isEmpty())
                    result.add(matcher.group(2)); // "number"
                result.add(matcher.group(3)); // "123456"
                if (!matcher.group(4).isEmpty())
                    result.add(matcher.group(4)); // "contract"
                return result;
            }
        }

        // Pattern 3: "contractAE125parts" -> ["contract", "AE125", "parts"] (case insensitive)
        if (word.matches("contract[a-zA-Z]+\\d+[a-zA-Z]*")) {
            result.add("contract");
            String remainder = word.substring("contract".length());
            Pattern partPattern = Pattern.compile("([a-zA-Z]+\\d+)([a-zA-Z]*)");
            java.util.regex.Matcher partMatcher = partPattern.matcher(remainder);
            if (partMatcher.matches()) {
                result.add(partMatcher.group(1).toUpperCase()); // "AE125"
                if (!partMatcher.group(2).isEmpty())
                    result.add(partMatcher.group(2)); // "parts"
                return result;
            }
        }

        // Pattern 4: Handle case where contract prefix might be split incorrectly
        if (word.toLowerCase().startsWith("contract") && word.length() > 8) {
            result.add("contract");
            String remainder = word.substring(8); // "contract".length()
            result.addAll(splitConcatenatedWords(remainder));
            return result;
        }

        // General pattern: letters followed by numbers followed by letters
        Pattern pattern = Pattern.compile("([a-zA-Z]+)(\\d+)([a-zA-Z]*)");
        java.util.regex.Matcher matcher = pattern.matcher(word);

        if (matcher.matches()) {
            String prefix = matcher.group(1); // e.g., "contract"
            String number = matcher.group(2); // e.g., "123"
            String suffix = matcher.group(3); // e.g., "sumry"

            result.add(prefix);
            result.add(number);
            if (!suffix.isEmpty()) {
                result.add(suffix);
            }
        } else {
            // Try simple letter-number splits like "AE125parts"
            Pattern pattern3 = Pattern.compile("([A-Z]+\\d+)([a-zA-Z]+)");
            java.util.regex.Matcher matcher3 = pattern3.matcher(word);

            if (matcher3.matches()) {
                String partNumber = matcher3.group(1); // e.g., "AE125"
                String suffix = matcher3.group(2); // e.g., "parts"

                result.add(partNumber);
                result.add(suffix);
            } else {
                // No pattern matched, return as-is
                result.add(word);
            }
        }

        return result;
    }

    /**
     * Split remainder by known words
     */
    private List<String> splitByKnownWords(String text) {
        List<String> result = new ArrayList<>();
        String[] knownWords = { "siemens", "under", "account", "number", "contract", "parts", "status", "customer" };

        String remaining = text.toLowerCase();
        int lastIndex = 0;

        for (String word : knownWords) {
            int index = remaining.indexOf(word);
            if (index != -1) {
                // Add any text before this word
                if (index > lastIndex) {
                    String before = remaining.substring(lastIndex, index);
                    if (!before.isEmpty())
                        result.add(before);
                }
                // Add the word
                result.add(word);
                lastIndex = index + word.length();
            }
        }

        // Add any remaining text
        if (lastIndex < remaining.length()) {
            String remainder = remaining.substring(lastIndex);
            if (!remainder.isEmpty())
                result.add(remainder);
        }

        // If no known words found, return original
        if (result.isEmpty()) {
            result.add(text);
        }

        return result;
    }

    /**
     * Extract creator name from "created by [name]" patterns
     */
    private String extractCreatorName(String input) {
        Pattern creatorPattern = Pattern.compile("(?:created\\s+by|by)\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = creatorPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract customer name from quotes or "account name" patterns
     */
    private String extractCustomerName(String input) {
        // Look for quoted names
        Pattern quotedPattern = Pattern.compile("'([^']+)'|\"([^\"]+)\"");
        java.util.regex.Matcher quotedMatcher = quotedPattern.matcher(input);
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1) != null ? quotedMatcher.group(1) : quotedMatcher.group(2);
        }

        // Look for "account name [name]" or "customer name [name]"
        Pattern namePattern =
            Pattern.compile("(?:account\\s+name|customer\\s+name)\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher nameMatcher = namePattern.matcher(input);
        if (nameMatcher.find()) {
            return nameMatcher.group(1);
        }

        return null;
    }

    private boolean containsLettersAndNumbers(String token) {
        boolean hasLetter = false, hasNumber = false;
        for (char c : token.toCharArray()) {
            if (Character.isLetter(c))
                hasLetter = true;
            if (Character.isDigit(c))
                hasNumber = true;
            if (hasLetter && hasNumber)
                return true;
        }
        return false;
    }

    /**
     * Enhanced entity extraction with improved accuracy based on NLPQueryProcessor patterns
     */
    private List<EntityFilter> extractEntities(String input) {
        List<EntityFilter> entities = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        // Step 1: Extract numeric and comparison filters with ENHANCED business term mapping
        Pattern filterPattern =
            Pattern.compile("([A-Z_]+|rebates?|line minimums?|line min|pricing?|minimum order|min order qty|unit of measure|lead time|leadtime)\\s*(=|>|<|>=|<=)\\s*([0-9A-Za-z_.%-]+)",
                            Pattern.CASE_INSENSITIVE);
        Matcher filterMatcher = filterPattern.matcher(input);
        Set<String> filterNumbers = new HashSet<>();
        while (filterMatcher.find()) {
            String field = filterMatcher.group(1).toUpperCase();
            String op = filterMatcher.group(2);
            String value = filterMatcher.group(3).replace("%", "");

            // ENHANCED: Use centralized business term mappings
            String mappedField =
                TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, field.toLowerCase());
            if (mappedField == null) {
                mappedField = TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, field.toLowerCase());
            }
            if (mappedField == null) {
                mappedField =
                    TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_FAILED_PARTS, field.toLowerCase());
            }
            if (mappedField != null) {
                field = mappedField;
            }

            if (isValidColumn(field, TableColumnConfig.TABLE_CONTRACTS) &&
                !WordDatabase.isStopWord(value.toLowerCase())) {
                entities.add(new EntityFilter(field, op, value, "user_input"));
                if (value.matches("\\d+"))
                    filterNumbers.add(value);
            }
        }

        // Step 2: Extract boolean/contextual flag filters
        extractBooleanFilters(lowerInput, entities);

        // Step 3: Extract date-based filters
        extractDateFilters(lowerInput, entities);

        // Step 4: Extract status filters
        extractStatusFilters(lowerInput, entities);

        // Step 5: Extract part numbers FIRST (higher priority for parts queries)
        extractPartNumbers(lowerInput, entities);

        // Step 6: Extract contract numbers (enhanced pattern matching)
        extractContractNumbers(lowerInput, entities, filterNumbers);

        // Step 7: Extract customer numbers
        extractCustomerNumbers(lowerInput, entities);

        // Step 8: Post-processing cleanup and deduplication
        List<EntityFilter> cleanedEntities = cleanupAndDeduplicateFilters(entities);

        // FIXED: Post-process failed parts queries to ensure proper filter assignment
        postProcessFailedPartsFilters(lowerInput, cleanedEntities);

        // FIXED: Apply business rule validation to ensure consistent column usage
        applyBusinessRuleValidation(cleanedEntities);

        return cleanedEntities;
    }

    /**
     * Extract boolean/contextual flag filters
     */
    private void extractBooleanFilters(String lowerInput, List<EntityFilter> entities) {
        if (lowerInput.contains("vmi enabled")) {
            entities.add(new EntityFilter("VMI", "=", "true", "user_input"));
        }
        if (lowerInput.contains("program contracts")) {
            entities.add(new EntityFilter("IS_PROGRAM", "=", "true", "user_input"));
        }

        // Enhanced failed parts detection with more comprehensive patterns
        if (lowerInput.contains("failed parts") || lowerInput.contains("failed part") ||
            lowerInput.contains("failing parts") || lowerInput.contains("faild parts") ||
            lowerInput.contains("parts failed") || lowerInput.contains("part failed") ||
            lowerInput.contains("failing part") || lowerInput.contains("faild part")) {
            entities.add(new EntityFilter("HAS_FAILED_PARTS", "=", "true", "user_input"));
        }

        // Enhanced error-related queries detection
        if (lowerInput.contains("error") || lowerInput.contains("errors") || lowerInput.contains("failure") ||
            lowerInput.contains("failures") || lowerInput.contains("why did") || lowerInput.contains("what caused") ||
            lowerInput.contains("error column") || lowerInput.contains("error columns") ||
            lowerInput.contains("which columns") || lowerInput.contains("column errors") ||
            lowerInput.contains("column error") || lowerInput.contains("validation error") ||
            lowerInput.contains("validation errors") || lowerInput.contains("loading error") ||
            lowerInput.contains("loading errors") || lowerInput.contains("load error") ||
            lowerInput.contains("load errors")) {
            entities.add(new EntityFilter("HAS_FAILED_PARTS", "=", "true", "user_input"));
        }

        // Enhanced status detection for failed parts
        if (lowerInput.contains("failed") && (lowerInput.contains("status") || lowerInput.contains("staus"))) {
            entities.add(new EntityFilter("STATUS", "=", "FAILED", "user_input"));
        }

        if (lowerInput.contains("is active")) {
            entities.add(new EntityFilter("STATUS", "=", "ACTIVE", "user_input"));
        }

        // Type filter extraction
        Matcher typeMatcher = Pattern.compile("type\\s+([A-Za-z0-9_]+)").matcher(lowerInput);
        if (typeMatcher.find()) {
            String typeValue = typeMatcher.group(1).toUpperCase();
            if (!WordDatabase.isStopWord(typeValue.toLowerCase())) {
                entities.add(new EntityFilter("CONTRACT_TYPE", "=", typeValue, "user_input"));
            }
        }
    }

    /**
     * Extract date-based filters
     */
    private void extractDateFilters(String lowerInput,
                                    List<EntityFilter> entities) {
        // Next month expiration
        if ((lowerInput.contains("expiring") || lowerInput.contains("expire") || lowerInput.contains("expiration")) &&
            lowerInput.contains("next month")) {
            java.time.LocalDate today = java.time
                                            .LocalDate
                                            .now();
            java.time.LocalDate firstOfNextMonth = today.plusMonths(1).withDayOfMonth(1);
            java.time.LocalDate lastOfNextMonth = firstOfNextMonth.withDayOfMonth(firstOfNextMonth.lengthOfMonth());
            String val = "'" + firstOfNextMonth.toString() + "' AND '" + lastOfNextMonth.toString() + "'";
            entities.add(new EntityFilter("EXPIRATION_DATE", "BETWEEN", val, "user_input"));
        }

        // Quarter-based filters
        Matcher qMatcher = Pattern.compile("q([1-4])\\s*(20\\d{2})").matcher(lowerInput);
        if (qMatcher.find()) {
            int quarter = Integer.parseInt(qMatcher.group(1));
            int year = Integer.parseInt(qMatcher.group(2));
            java.time.LocalDate start, end;
            switch (quarter) {
            case 1:
                start = java.time
                            .LocalDate
                            .of(year, 1, 1);
                end = java.time
                          .LocalDate
                          .of(year, 3, 31);
                break;
            case 2:
                start = java.time
                            .LocalDate
                            .of(year, 4, 1);
                end = java.time
                          .LocalDate
                          .of(year, 6, 30);
                break;
            case 3:
                start = java.time
                            .LocalDate
                            .of(year, 7, 1);
                end = java.time
                          .LocalDate
                          .of(year, 9, 30);
                break;
            case 4:
                start = java.time
                            .LocalDate
                            .of(year, 10, 1);
                end = java.time
                          .LocalDate
                          .of(year, 12, 31);
                break;
            default:
                start = null;
                end = null;
            }
            if (start != null && end != null) {
                String val = "'" + start.toString() + "' AND '" + end.toString() + "'";
                entities.add(new EntityFilter("EXPIRATION_DATE", "BETWEEN", val, "user_input"));
            }
        }

        // 90-day notice
        if (lowerInput.contains("90-day notice") || lowerInput.contains("within 90 days")) {
            java.time.LocalDate today = java.time
                                            .LocalDate
                                            .now();
            java.time.LocalDate ninetyDays = today.plusDays(90);
            String val = "'" + today.toString() + "' AND '" + ninetyDays.toString() + "'";
            entities.add(new EntityFilter("EXPIRATION_DATE", "BETWEEN", val, "user_input"));
        }

        // Created date filters
        if (lowerInput.contains("created in") || lowerInput.contains("created after") ||
            lowerInput.contains("created before") || lowerInput.contains("created between")) {
            String dateRange = extractDateRange(lowerInput);
            if (dateRange != null && dateRange.contains("AND")) {
                String[] parts = dateRange.split("AND");
                if (parts.length == 2) {
                    String val = "'" + parts[0].trim() + "' AND '" + parts[1].trim() + "'";
                    entities.add(new EntityFilter("CREATE_DATE", "BETWEEN", val, "user_input"));
                }
            } else if (dateRange != null) {
                entities.add(new EntityFilter("CREATE_DATE", "=", dateRange, "user_input"));
            }
        }

        // After date pattern
        Matcher afterMatcher = Pattern.compile("after\\s+(\\d{4}-\\d{2}-\\d{2})").matcher(lowerInput);
        if (afterMatcher.find()) {
            String date = afterMatcher.group(1);
            entities.add(new EntityFilter("CREATE_DATE", ">", date, "user_input"));
        }

        // Price filters
        Matcher priceMatcher =
            Pattern.compile("price\\s*(>|<|=|>=|<=)\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(lowerInput);
        if (priceMatcher.find()) {
            String op = priceMatcher.group(1);
            String value = priceMatcher.group(2);
            entities.add(new EntityFilter("PRICE", op, value, "user_input"));
        }
    }

    /**
     * Extract status filters
     */
    private void extractStatusFilters(String lowerInput, List<EntityFilter> entities) {
        if (lowerInput.contains("status") || lowerInput.contains("expired") || lowerInput.contains("active") ||
            lowerInput.contains("inactive")) {
            String status = extractStatusEnhanced(lowerInput);
            if (status != null) {
                entities.add(new EntityFilter("STATUS", "=", status, "user_input"));
            }
        }
        if (lowerInput.contains("failed") || lowerInput.contains("failure") || lowerInput.contains("issues") ||
            lowerInput.contains("defect")) {
            entities.add(new EntityFilter("STATUS", "=", "FAILED", "user_input"));
        }
    }

    /**
     * Enhanced contract number extraction
     */
    private void extractContractNumbers(String lowerInput, List<EntityFilter> entities, Set<String> filterNumbers) {
        // Enhanced pattern matching for contract numbers
        Pattern[] contractPatterns = {
            Pattern.compile("(?:for|of|contract|in)\\s*(\\d{6,})(?![0-9])"), Pattern.compile("\\b(\\d{6,})\\b"),
            Pattern.compile("contract\\s*(\\d{6,})"), Pattern.compile("(\\d{6,})\\s*(?:contract|details|info|status)")
        };

        String foundContractNum = null;
        for (Pattern pattern : contractPatterns) {
            Matcher matcher = pattern.matcher(lowerInput);
            while (matcher.find()) {
                String contractNum = matcher.group(1);
                // Don't extract if it's likely a part number (2-3 letters + 5 digits)
                if (contractNum.matches("\\d{6,}") && !filterNumbers.contains(contractNum) &&
                    !lowerInput.matches(".*\\b[A-Z]{2,3}" + contractNum + "\\b.*")) {
                    foundContractNum = contractNum;
                    break;
                }
            }
            if (foundContractNum != null)
                break;
        }

        if (foundContractNum !=
            null) {
            // FIXED: Determine the appropriate filter based on context
            if (lowerInput.contains("parts") &&
                (lowerInput.contains("failed") || lowerInput.contains("error") || lowerInput.contains("failure"))) {
                entities.add(new EntityFilter("LOADED_CP_NUMBER", "=", foundContractNum, "user_input"));
            } else if (lowerInput.contains("parts") || lowerInput.contains("invoice part") ||
                       lowerInput.contains("invoice parts") || lowerInput.contains("part") ||
                       lowerInput.contains("parts")) {
                entities.add(new EntityFilter("LOADED_CP_NUMBER", "=", foundContractNum, "user_input"));
            } else {
                entities.add(new EntityFilter("AWARD_NUMBER", "=", foundContractNum, "user_input"));
            }
        }
    }

    /**
     * Enhanced part number extraction with improved pattern recognition
     */
    private void extractPartNumbers(String lowerInput, List<EntityFilter> entities) {
        // ENHANCED: Multiple part number patterns for different formats
        Pattern[] partPatterns = { PART_PATTERN_1, PART_PATTERN_2, PART_PATTERN_3, PART_PATTERN_4, PART_PATTERN_5 };

        for (Pattern pattern : partPatterns) {
            Matcher matcher = pattern.matcher(lowerInput);
            if (matcher.find()) {
                String partNum = matcher.group().toUpperCase();
                if (isValidPartNumber(partNum)) {
                    entities.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNum, "user_input"));
                    return; // Found a valid part number, don't look for others
                }
            }
        }

        // Enhanced pattern for part numbers (2-3 letters + 5 digits format) - HIGHEST PRIORITY
        Pattern partNumberPattern = Pattern.compile("\\b([A-Z]{2,3}\\d{5})\\b", Pattern.CASE_INSENSITIVE);
        Matcher partMatcher = partNumberPattern.matcher(lowerInput);
        if (partMatcher.find()) {
            String partNum = partMatcher.group(1).toUpperCase();
            entities.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNum, "user_input"));
            return; // Found a proper part number, don't look for others
        }

        // Pattern 1: "part [number]" - but avoid common words
        Pattern partPattern = Pattern.compile("part\\s+([A-Za-z0-9_/-]{3,})", Pattern.CASE_INSENSITIVE);
        Matcher partMatcher2 = partPattern.matcher(lowerInput);
        if (partMatcher2.find()) {
            String partNum = partMatcher2.group(1).toUpperCase();
            // ENHANCED: Don't extract common words as part numbers
            if (!isCommonWord(partNum) && partNum.matches("[A-Z0-9_/-]{3,}") && !isCommandWord(partNum)) {
                entities.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNum, "user_input"));
            }
        }

        // Pattern 2: "price of [number]", "item [number]", etc.
        Pattern priceOfPartMatcher =
            Pattern.compile("(?:price of|item|for part|of part|for item|of item)\\s+([A-Za-z0-9_/-]{3,})",
                            Pattern.CASE_INSENSITIVE);
        Matcher priceMatcher = priceOfPartMatcher.matcher(lowerInput);
        if (priceMatcher.find()) {
            String partNum = priceMatcher.group(1).toUpperCase();
            if (!isCommonWord(partNum) && partNum.matches("[A-Z0-9_/-]{3,}") && !isCommandWord(partNum)) {
                entities.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNum, "user_input"));
            }
        }

        // Pattern 3: Specific part number patterns from test cases
        Pattern specificPartPattern = Pattern.compile("\\b([A-Z]{2,3}\\d{5})\\b", Pattern.CASE_INSENSITIVE);
        Matcher specificMatcher = specificPartPattern.matcher(lowerInput);
        if (specificMatcher.find()) {
            String partNum = specificMatcher.group(1).toUpperCase();
            entities.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNum, "user_input"));
        }

        // Pattern 4: Enhanced detection for parts queries without explicit "part" keyword
        // Look for part-specific attributes followed by alphanumeric patterns
        if (lowerInput.matches(".*\\b(price|moq|uom|lead time|status|classification|class)\\b.*")) {
            Pattern attrPartPattern = Pattern.compile("\\b([A-Z]{2,3}\\d{5})\\b", Pattern.CASE_INSENSITIVE);
            Matcher attrMatcher = attrPartPattern.matcher(lowerInput);
            if (attrMatcher.find()) {
                String partNum = attrMatcher.group(1).toUpperCase();
                entities.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNum, "user_input"));
            }
        }

        // Special handling for part files
        if (lowerInput.contains("part files") || lowerInput.contains("part file")) {
            entities.add(new EntityFilter("PART_FILE", "=", "true", "user_input"));
        }
    }

    // ENHANCED: Check if a word is a command word that shouldn't be extracted as a part number
    private boolean isCommandWord(String word) {
        Set<String> commandWords =
            new HashSet<>(Arrays.asList("LIST", "SHOW", "GET", "DISPLAY", "FIND", "RETRIEVE", "GIVE", "PROVIDE", "WHAT",
                                        "HOW", "WHY", "WHEN", "WHERE", "WHICH", "WHO", "IS", "ARE", "CAN", "WILL",
                                        "THE", "OF", "FOR", "IN", "ON", "AT", "BY", "WITH", "FROM", "TO", "AND", "OR",
                                        "CONTRACT", "CONTRACTS", "PART", "PARTS", "CUSTOMER", "ACCOUNT", "INFO",
                                        "DETAILS", "STATUS", "DATA", "ALL", "ANY", "SOME", "MANY", "MUCH", "MORE",
                                        "MOST", "LESS", "CREATED", "EXPIRED", "ACTIVE", "INACTIVE", "FAILED", "PASSED",
                                        "LOADED", "MISSING", "UNDER", "NAME", "NUMBER", "AFTER", "BEFORE", "BETWEEN",
                                        "DURING", "WITHIN"));
        return commandWords.contains(word.toUpperCase());
    }

    // ENHANCED: Additional validation methods from ContractsModel
    private boolean isValidPartNumber(String partNumber) {
        return partNumber != null && partNumber.length() >= 3 && partNumber.matches("[A-Za-z0-9\\-_]+");
    }

    private boolean isValidContractNumber(String number) {
        return number != null && number.matches("\\d{6,}") && number.length() >= 6 && number.length() <= 12;
    }

    private boolean isValidCustomerIdentifier(String identifier) {
        return identifier != null &&
               (identifier.matches("\\d{4,}") || (identifier.length() >= 3 && identifier.matches("[A-Za-z0-9\\s]+")));
    }

    private boolean isValidDateRange(String startDate, String endDate) {
        // Simplified date validation - in real implementation, use proper date parsing
        return startDate != null && endDate != null && !startDate.equals(endDate);
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extract customer numbers
     */
    private void extractCustomerNumbers(String lowerInput, List<EntityFilter> entities) {
        Matcher customerMatcher = Pattern.compile("customer\\s*(\\d{4,8})").matcher(lowerInput);
        if (customerMatcher.find()) {
            String custNum = customerMatcher.group(1);
            if (custNum.matches("\\d{4,8}")) {
                entities.add(new EntityFilter("CUSTOMER_NUMBER", "=", custNum, "user_input"));
            }
        }
    }

    // ENHANCED: Context-aware entity extraction from ContractsModel
    private List<String> extractContextIndicators(String query) {
        List<String> indicators = new ArrayList<>();

        // Check for various context indicators
        if (containsAny(query, WordDatabase.getCustomerContextWords())) {
            indicators.add("CUSTOMER_CONTEXT");
        }
        if (containsAny(query, WordDatabase.getPriceContextWords())) {
            indicators.add("PRICE_CONTEXT");
        }
        if (containsAny(query, WordDatabase.getStatusContextWords())) {
            indicators.add("STATUS_CONTEXT");
        }
        if (containsAny(query, WordDatabase.getDateContextWords())) {
            indicators.add("DATE_CONTEXT");
        }
        if (query.contains("failed") || query.contains("error")) {
            indicators.add("ERROR_CONTEXT");
        }

        return indicators;
    }

    private boolean containsAny(String text, Set<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cleanup and deduplicate filters
     */
    private List<EntityFilter> cleanupAndDeduplicateFilters(List<EntityFilter> entities) {
        // Remove invalid filters
        entities.removeIf(f -> (f.attribute.equals("CONTRACT_NUMBER") && !f.value.matches("\\d{6,}")) ||
                          (f.attribute.equals("PART_NUMBER") && !f.value.matches("[A-Z0-9_/-]{3,}")) ||
                          (f.attribute.equals("CUSTOMER_NUMBER") && !f.value.matches("\\d{4,8}")));

        // Deduplicate
        Set<String> seen = new HashSet<>();
        List<EntityFilter> deduped = new ArrayList<>();
        for (EntityFilter f : entities) {
            String key = f.attribute + ":" + f.operation + ":" + f.value;
            if (!seen.contains(key)) {
                deduped.add(f);
                seen.add(key);
            }
        }
        return deduped;
    }

    /**
     * FIXED: Post-process failed parts queries to ensure proper filter assignment
     */
    private void postProcessFailedPartsFilters(String lowerInput,
                                               List<EntityFilter> entities) {
        // FIXED: Enhanced failed parts query detection
        boolean isFailedPartsQuery =
            lowerInput.contains("failed") || lowerInput.contains("error") || lowerInput.contains("failure") ||
            lowerInput.contains("error reason") || lowerInput.contains("failure reason") ||
            lowerInput.contains("what caused") || lowerInput.contains("caused") || lowerInput.contains("why did") ||
            lowerInput.contains("failing parts") || lowerInput.contains("failing part") ||
            lowerInput.contains("error column") || lowerInput.contains("error columns");

        if (isFailedPartsQuery) {
            // Look for contract numbers in the input
            Pattern contractPattern = Pattern.compile("\\b(\\d{6,})\\b");
            Matcher matcher = contractPattern.matcher(lowerInput);

            if (matcher.find()) {
                String contractNum = matcher.group(1);

                // FIXED: Business rule - Remove AWARD_NUMBER if present for failed parts queries
                entities.removeIf(e -> e.attribute.equals("AWARD_NUMBER"));

                // Check if we already have LOADED_CP_NUMBER filter
                boolean hasLoadedCpFilter = entities.stream().anyMatch(e -> e.attribute.equals("LOADED_CP_NUMBER"));

                // If not, add it
                if (!hasLoadedCpFilter) {
                    entities.add(new EntityFilter("LOADED_CP_NUMBER", "=", contractNum, "user_input"));
                }

                // Also add HAS_FAILED_PARTS if not present
                boolean hasFailedPartsFilter = entities.stream().anyMatch(e -> e.attribute.equals("HAS_FAILED_PARTS"));

                if (!hasFailedPartsFilter) {
                    entities.add(new EntityFilter("HAS_FAILED_PARTS", "=", "true", "user_input"));
                }
            }
        }
    }

    /**
     * FIXED: Enhance multi-intent query processing
     */
    private void enhanceMultiIntentProcessing(String input, List<String> displayEntities, List<EntityFilter> entities) {
        String lowerInput = input.toLowerCase();

        // FIXED: Enhanced multi-intent patterns with better detection
        boolean hasContractAndParts =
            lowerInput.contains("contract") &&
            (lowerInput.contains("part") || lowerInput.contains("parts") || lowerInput.contains("invoice parts"));
        boolean hasCustomerAndParts =
            (lowerInput.contains("customer") || lowerInput.contains("customer name") ||
             lowerInput.contains("customer info")) && (lowerInput.contains("part") || lowerInput.contains("parts"));
        boolean hasContractAndFailed =
            lowerInput.contains("contract") &&
            (lowerInput.contains("failed") || lowerInput.contains("error") || lowerInput.contains("faild"));
        boolean hasContractAndDetails =
            lowerInput.contains("contract") &&
            (lowerInput.contains("details") || lowerInput.contains("info") || lowerInput.contains("information"));
        boolean hasCustomerAndErrors =
            (lowerInput.contains("customer") || lowerInput.contains("customer name")) &&
            (lowerInput.contains("error") || lowerInput.contains("failed") || lowerInput.contains("reason"));

        // ENHANCED: Comprehensive multi-intent handling
        if (hasContractAndParts || hasCustomerAndParts) {
            // Add contract-related display entities if missing
            if (!displayEntities.contains("CONTRACT_NAME")) {
                displayEntities.add("CONTRACT_NAME");
            }
            if (!displayEntities.contains("CUSTOMER_NAME")) {
                displayEntities.add("CUSTOMER_NAME");
            }
            if (!displayEntities.contains("EFFECTIVE_DATE")) {
                displayEntities.add("EFFECTIVE_DATE");
            }
            if (!displayEntities.contains("EXPIRATION_DATE")) {
                displayEntities.add("EXPIRATION_DATE");
            }
            if (!displayEntities.contains("STATUS")) {
                displayEntities.add("STATUS");
            }

            // Add parts-related display entities
            if (!displayEntities.contains("INVOICE_PART_NUMBER")) {
                displayEntities.add("INVOICE_PART_NUMBER");
            }
            if (!displayEntities.contains("PRICE")) {
                displayEntities.add("PRICE");
            }
            if (!displayEntities.contains("MOQ")) {
                displayEntities.add("MOQ");
            }
            if (!displayEntities.contains("UOM")) {
                displayEntities.add("UOM");
            }
            if (!displayEntities.contains("LEAD_TIME")) {
                displayEntities.add("LEAD_TIME");
            }
        }

        if (hasContractAndFailed) {
            // Add failed parts related display entities
            if (!displayEntities.contains("PART_NUMBER")) {
                displayEntities.add("PART_NUMBER");
            }
            if (!displayEntities.contains("ERROR_COLUMN")) {
                displayEntities.add("ERROR_COLUMN");
            }
            if (!displayEntities.contains("REASON")) {
                displayEntities.add("REASON");
            }
            if (!displayEntities.contains("LOADED_CP_NUMBER")) {
                displayEntities.add("LOADED_CP_NUMBER");
            }

            // Add contract context for failed parts
            if (!displayEntities.contains("CONTRACT_NAME")) {
                displayEntities.add("CONTRACT_NAME");
            }
            if (!displayEntities.contains("CUSTOMER_NAME")) {
                displayEntities.add("CUSTOMER_NAME");
            }
        }

        if (hasContractAndDetails) {
            // Add comprehensive contract details
            if (!displayEntities.contains("CONTRACT_NAME")) {
                displayEntities.add("CONTRACT_NAME");
            }
            if (!displayEntities.contains("CUSTOMER_NAME")) {
                displayEntities.add("CUSTOMER_NAME");
            }
            if (!displayEntities.contains("EFFECTIVE_DATE")) {
                displayEntities.add("EFFECTIVE_DATE");
            }
            if (!displayEntities.contains("EXPIRATION_DATE")) {
                displayEntities.add("EXPIRATION_DATE");
            }
            if (!displayEntities.contains("STATUS")) {
                displayEntities.add("STATUS");
            }
            if (!displayEntities.contains("PAYMENT_TERMS")) {
                displayEntities.add("PAYMENT_TERMS");
            }
            if (!displayEntities.contains("INCOTERMS")) {
                displayEntities.add("INCOTERMS");
            }
        }

        // FIXED: Enhanced customer and errors multi-intent handling
        if (hasCustomerAndErrors) {
            // Add customer information
            if (!displayEntities.contains("CUSTOMER_NAME")) {
                displayEntities.add("CUSTOMER_NAME");
            }
            if (!displayEntities.contains("CUSTOMER_NUMBER")) {
                displayEntities.add("CUSTOMER_NUMBER");
            }
            // Add error information
            if (!displayEntities.contains("PART_NUMBER")) {
                displayEntities.add("PART_NUMBER");
            }
            if (!displayEntities.contains("ERROR_COLUMN")) {
                displayEntities.add("ERROR_COLUMN");
            }
            if (!displayEntities.contains("REASON")) {
                displayEntities.add("REASON");
            }
        }

        // ENHANCED: Handle specific business term queries in multi-intent context
        if (lowerInput.contains("pricing") || lowerInput.contains("price") || lowerInput.contains("cost")) {
            if (!displayEntities.contains("PRICE")) {
                displayEntities.add("PRICE");
            }
        }

        if (lowerInput.contains("minimum order") || lowerInput.contains("moq")) {
            if (!displayEntities.contains("MOQ")) {
                displayEntities.add("MOQ");
            }
        }

        if (lowerInput.contains("unit of measure") || lowerInput.contains("uom")) {
            if (!displayEntities.contains("UOM")) {
                displayEntities.add("UOM");
            }
        }

        if (lowerInput.contains("lead time")) {
            if (!displayEntities.contains("LEAD_TIME")) {
                displayEntities.add("LEAD_TIME");
            }
        }
    }

    private String extractYear(String input) {
        java.util.regex.Matcher matcher = YEAR_PATTERN.matcher(input);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractDateRange(String input) {
        // Extract date ranges like "after 1-Jan-2020", "between Jan and June 2024"
        Pattern dateRangePattern = Pattern.compile("(\\d{1,2}-\\w{3}-\\d{4}|\\w{3}\\s+\\d{4}|\\d{4})");
        java.util.regex.Matcher matcher = dateRangePattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractStatusEnhanced(String input) {
        if (input.contains("expired"))
            return "EXPIRED";
        if (input.contains("active"))
            return "ACTIVE";
        if (input.contains("inactive"))
            return "INACTIVE";
        if (input.contains("pending"))
            return "PENDING";
        if (input.contains("failed"))
            return "FAILED";
        return null;
    }

    /**
     * Improved validation - less strict for general queries
     */
    private List<ValidationError> validateInput(HeaderResult headerResult, List<EntityFilter> entities, String input) {
        List<ValidationError> errors = new ArrayList<>();

        // Add header issues
        headerResult.issues.forEach(issue -> errors.add(new ValidationError("INVALID_HEADER", issue, "BLOCKER")));

        // Check if we have at least one header or entity
        Header header = headerResult.header;
        boolean hasValidHeader =
            header.contractNumber != null || header.partNumber != null || header.customerNumber != null ||
            header.customerName != null || header.createdBy != null;

        // Enhanced general query detection - be more permissive
        String lowerInput = input.toLowerCase();
        boolean isGeneralQuery =
            lowerInput.contains("all") || lowerInput.contains("list") || lowerInput.contains("show") ||
            lowerInput.contains("status") || lowerInput.contains("details") || lowerInput.contains("expired") ||
            lowerInput.contains("active") || lowerInput.contains("created") || lowerInput.contains("contracts") ||
            lowerInput.contains("parts") || !entities.isEmpty(); // If we have entities, it's a valid query

        // More lenient validation for ambiguous cases
        if (!hasValidHeader && entities.isEmpty() &&
            !isGeneralQuery) {
            // Check if input contains any domain keywords that suggest intent
            boolean hasDomainKeywords =
                lowerInput.contains("contract") || lowerInput.contains("part") || lowerInput.contains("customer") ||
                lowerInput.contains("account") || lowerInput.contains("number") ||
                lowerInput.matches(".*\\b[a-z]{2,3}\\d+.*") || // Part-like patterns
                lowerInput.matches(".*\\d{4,}.*"); // Number patterns

            // Only add error if it's clearly not a domain-related query
            if (!hasDomainKeywords) {
                errors.add(new ValidationError("MISSING_HEADER",
                                               "Provide at least one identifier (contract/part/customer) or filter (date/status)",
                                               "BLOCKER"));
            }
        }

        return errors;
    }

    /**
     * Determine query metadata
     */


    /**
     * FIXED: Enhanced action type determination with proper routing
     */
    private String determineActionType(String originalPrompt, String normalizedPrompt, String queryType) {
        String lowerPrompt = originalPrompt.toLowerCase();
        
        // CRITICAL FIX: Handle failed parts queries with proper action type
        // Check for failed parts keywords first
        String[] failedPartsKeywords = {
            "failed parts", "failed part", "faild parts", "faild part", "error reasons", "error reason",
            "error columns", "error column", "error details", "error detail", "error info", "error information",
            "parts with issues", "parts with issue", "parts with errors", "parts with error", "parts with problems",
            "parts with problem", "parts with failures", "parts with failure", "missing data", "no data",
            "incomplete data", "invalid data", "parts failed", "part failed", "failed", "failures", "errors", "issues",
            "failure reasons", "failure reason", "why did parts fail", "why did part fail", "what caused failure",
            "what caused error", "what went wrong", "what happened", "validation errors", "validation issues",
            "validation problems", "validation failures", "loading errors", "loading issues", "load errors",
            "load problems", "load failures"
        };

        boolean hasFailedPartsKeywords = false;
        for (String keyword : failedPartsKeywords) {
            if (lowerPrompt.contains(keyword)) {
                hasFailedPartsKeywords = true;
                break;
            }
        }

        // CRITICAL FIX: Handle failed parts queries with proper action type
        if (hasFailedPartsKeywords || "PARTS".equals(queryType)) {
            boolean hasContractNumber = lowerPrompt.matches(".*\\b\\d{6,}\\b.*");

            if (hasFailedPartsKeywords && hasContractNumber) {
                    return FAILED_PARTS_BY_CONTRACT_NUMBER; // Use standardized constant
            } else if (hasFailedPartsKeywords) {
                return FAILED_PARTS_BY_FILTER; // Use standardized constant
            }
        }
        
        // CRITICAL FIX: Handle HELP queries with proper action type distinction
        if ("HELP".equals(queryType)) {
            String[] createWords = {
                "create", "make", "generate", "initiate", "build", "draft", "establish", "form", "set up" };
            String[] helpWords = {
                "help", "steps", "guide", "instruction", "how to", "walk me", "explain", "process", "show me how",
                "need guidance", "teach", "assist", "support"
            };
            
            boolean hasCreateWord = false;
            for (String w : createWords) {
                if (lowerPrompt.contains(w)) {
                    hasCreateWord = true;
                    break;
                }
            }
            
            boolean hasHelpWord = false;
            for (String w : helpWords) {
                if (lowerPrompt.contains(w)) {
                    hasHelpWord = true;
                    break;
                }
            }
            
            // Prefer _BOT if both are present
            if (hasCreateWord) {
                return HELP_CONTRACT_CREATE_BOT;
            } else if (hasHelpWord) {
                return HELP_CONTRACT_CREATE_USER;
            } else {
                // Default to _USER if ambiguous
                return HELP_CONTRACT_CREATE_USER;
            }
        }
        
        // CRITICAL FIX: Handle CONTRACTS queries with proper action type
        if ("CONTRACTS".equals(queryType)) {
            // Check for contract number
            boolean hasContractNumber = lowerPrompt.matches(".*\\b\\d{6,}\\b.*");
            
            // PRIORITY: Price expiration is a contract field, not parts - use contracts action
            if ((lowerPrompt.contains("price") && lowerPrompt.contains("expir")) ||
                (lowerPrompt.contains("price") && lowerPrompt.contains("expiry")) ||
                (lowerPrompt.contains("price") && lowerPrompt.contains("experation")) ||
                (lowerPrompt.contains("price") && lowerPrompt.contains("expire"))) {
                return CONTRACTS_BY_CONTRACT_NUMBER;
            }
            
            if (hasContractNumber) {
                return CONTRACTS_BY_CONTRACT_NUMBER;
            } else {
                return CONTRACTS_BY_FILTER;
            }
        }
        
        // Handle PARTS queries
        if ("PARTS".equals(queryType)) {
            boolean hasContractNumber = lowerPrompt.matches(".*\\b\\d{6,}\\b.*");
            boolean hasPartNumber =
                lowerPrompt.matches(".*\\b[A-Za-z]{2}\\d{4,6}\\b.*") ||
                                   lowerPrompt.matches(".*\\b[A-Za-z]\\d{4,8}\\b.*") ||
                                   lowerPrompt.matches(".*\\b\\d{4,8}[A-Za-z]{1,3}\\b.*") ||
                                   lowerPrompt.matches(".*\\b[A-Za-z]{3,4}-\\d{3,6}\\b.*");
            
            // CRITICAL FIX: Ensure parts queries with part numbers get parts_by_part_number (standardized)
            if (hasPartNumber) {
                return PARTS_BY_PART_NUMBER;
            } else if (hasContractNumber) {
                return PARTS_BY_CONTRACT_NUMBER;
            } else {
                return PARTS_BY_PART_NUMBER; // Default to parts_by_part_number for PARTS queries
            }
        }
        
        // Default fallback
        return CONTRACTS_BY_CONTRACT_NUMBER;
    }

    /**
     * Generate standard JSON following JSON_DESIGN.md exactly
     */
    private String generateStandardJSON(String originalInput, InputTrackingResult inputTracking,
                                        HeaderResult headerResult, QueryMetadata metadata, List<EntityFilter> entities,
                                        List<String> displayEntities, List<ValidationError> errors) {
        StringBuilder json = new StringBuilder();

        json.append("{\n");

        // Header section with inputTracking
        json.append("  \"header\": {\n");
        json.append("    \"contractNumber\": ")
            .append(quote(headerResult.header.contractNumber))
            .append(",\n");
        json.append("    \"partNumber\": ")
            .append(quote(headerResult.header.partNumber))
            .append(",\n");
        json.append("    \"customerNumber\": ")
            .append(quote(headerResult.header.customerNumber))
            .append(",\n");
        json.append("    \"customerName\": ")
            .append(quote(headerResult.header.customerName))
            .append(",\n");
        json.append("    \"createdBy\": ")
            .append(quote(headerResult.header.createdBy))
            .append(",\n");

        // InputTracking section (NEW as per JSON_DESIGN.md)
        json.append("    \"inputTracking\": {\n");
        json.append("      \"originalInput\": ")
            .append(quote(inputTracking.originalInput))
            .append(",\n");
        json.append("      \"correctedInput\": ")
            .append(quote(inputTracking.correctedInput))
            .append(",\n");
        json.append("      \"correctionConfidence\": ")
            .append(inputTracking.correctionConfidence)
            .append("\n");
        json.append("    }\n");
        json.append("  },\n");

        // QueryMetadata section
        json.append("  \"queryMetadata\": {\n");
        json.append("    \"queryType\": ")
            .append(quote(metadata.queryType))
            .append(",\n");
        json.append("    \"actionType\": ")
            .append(quote(metadata.actionType))
            .append(",\n");
        json.append("    \"processingTimeMs\": ")
            .append(String.format("%.3f", metadata.processingTimeMs))
            .append("\n");
        json.append("  },\n");

        // Entities section
        json.append("  \"entities\": [\n");
        for (int i = 0; i < entities.size(); i++) {
            EntityFilter entity = entities.get(i);
            json.append("    {\n");
            json.append("      \"attribute\": ")
                .append(quote(entity.attribute))
                .append(",\n");
            json.append("      \"operation\": ")
                .append(quote(entity.operation))
                .append(",\n");
            json.append("      \"value\": ")
                .append(quote(entity.value))
                .append(",\n");
            json.append("      \"source\": ")
                .append(quote(entity.source))
                .append("\n");
            json.append("    }")
                .append(i < entities.size() - 1 ? "," : "")
                .append("\n");
        }
        json.append("  ],\n");

        // DisplayEntities section
        json.append("  \"displayEntities\": [\n");
        for (int i = 0; i < displayEntities.size(); i++) {
            json.append("    ").append(quote(displayEntities.get(i)));
            json.append(i < displayEntities.size() - 1 ? "," : "").append("\n");
        }
        json.append("  ],\n");

        // Errors section
        json.append("  \"errors\": [\n");
        for (int i = 0; i < errors.size(); i++) {
            ValidationError error = errors.get(i);
            json.append("    {\n");
            json.append("      \"code\": ")
                .append(quote(error.code))
                .append(",\n");
            json.append("      \"message\": ")
                .append(quote(error.message))
                .append(",\n");
            json.append("      \"severity\": ")
                .append(quote(error.severity))
                .append("\n");
            json.append("    }")
                .append(i < errors.size() - 1 ? "," : "")
                .append("\n");
        }
        json.append("  ]\n");

        json.append("}");

        return json.toString();
    }

    /**
     * Generate error JSON
     */
    private String generateErrorJSON(String originalInput, String errorMessage, double processingTime) {
        return String.format("{\n" + "  \"header\": {\n" + "    \"contractNumber\": null,\n" +
                             "    \"partNumber\": null,\n" + "    \"customerNumber\": null,\n" +
                             "    \"customerName\": null,\n" + "    \"createdBy\": null,\n" +
                             "    \"inputTracking\": {\n" + "      \"originalInput\": %s,\n" +
                             "      \"correctedInput\": null,\n" + "      \"correctionConfidence\": 0\n" + "    }\n" +
                             "  },\n" + "  \"queryMetadata\": {\n" + "    \"queryType\": \"CONTRACTS\",\n" +
                             "    \"actionType\": \"error\",\n" + "    \"processingTimeMs\": %.3f\n" + "  },\n" +
                             "  \"entities\": [],\n" + "  \"displayEntities\": [],\n" + "  \"errors\": [\n" +
                             "    {\n" + "      \"code\": \"PROCESSING_ERROR\",\n" + "      \"message\": %s,\n" +
                             "      \"severity\": \"BLOCKER\"\n" + "    }\n" + "  ]\n" + "}", quote(originalInput),
                             processingTime, quote(errorMessage));
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"";
    }

    // Data classes

    public static class InputTrackingResult {
        public final String originalInput;
        public final String correctedInput;
        public final double correctionConfidence;

        public InputTrackingResult(String originalInput, String correctedInput, double correctionConfidence) {
            this.originalInput = originalInput;
            this.correctedInput = correctedInput;
            this.correctionConfidence = correctionConfidence;
        }
    }

    public static class Header {
        public String contractNumber;
        public String partNumber;
        public String customerNumber;
        public String customerName;
        public String createdBy;
    }

    public static class HeaderResult {
        public final Header header;
        public final List<String> issues;

        public HeaderResult(Header header, List<String> issues) {
            this.header = header;
            this.issues = issues;
        }
    }

    public static class QueryMetadata {
        public String queryType;
        public String actionType;
        public double processingTimeMs;

        public QueryMetadata(String queryType, String actionType, double processingTimeMs) {
            this.queryType = queryType;
            this.actionType = actionType;
            this.processingTimeMs = processingTimeMs;
        }
    }

    public static class EntityFilter {
        public final String attribute;
        public final String operation;
        public final String value;
        public final String source;

        public EntityFilter(String attribute, String operation, String value, String source) {
            this.attribute = attribute;
            this.operation = operation;
            this.value = value;
            this.source = source;
        }
    }

    public static class ValidationError {
        public final String code;
        public final String message;
        public final String severity;

        public ValidationError(String code, String message, String severity) {
            this.code = code;
            this.message = message;
            this.severity = severity;
        }
    }

    /**
     * QueryResult class for easy Java object access
     * Contains all parsed components from JSON response
     */
    public static class QueryResult {
        public InputTrackingResult inputTracking;
        public Header header;
        public QueryMetadata metadata;
        public List<EntityFilter> entities;
        public List<String> displayEntities;
        public List<ValidationError> errors;

        public QueryResult() {
            this.entities = new ArrayList<>();
            this.displayEntities = new ArrayList<>();
            this.errors = new ArrayList<>();
        }

        // Convenience methods for easy access
        public String getContractNumber() {
            return header != null ? header.contractNumber : null;
        }

        public String getPartNumber() {
            return header != null ? header.partNumber : null;
        }

        public String getCustomerNumber() {
            return header != null ? header.customerNumber : null;
        }

        public String getCustomerName() {
            return header != null ? header.customerName : null;
        }

        public String getCreatedBy() {
            return header != null ? header.createdBy : null;
        }

        public String getOriginalInput() {
            return inputTracking != null ? inputTracking.originalInput : null;
        }

        public String getCorrectedInput() {
            return inputTracking != null ? inputTracking.correctedInput : null;
        }

        public double getCorrectionConfidence() {
            return inputTracking != null ? inputTracking.correctionConfidence : 0.0;
        }

        public String getQueryType() {
            return metadata != null ? metadata.queryType : null;
        }

        public String getActionType() {
            return metadata != null ? metadata.actionType : null;
        }

        public double getProcessingTimeMs() {
            return metadata != null ? metadata.processingTimeMs : 0.0;
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        public boolean hasBlockingErrors() {
            return errors != null && errors.stream().anyMatch(e -> "BLOCKER".equals(e.severity));
        }

        public boolean hasSpellCorrections() {
            return inputTracking != null && inputTracking.correctedInput != null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("QueryResult {\n");
            sb.append("  Original Input: ")
              .append(getOriginalInput())
              .append("\n");
            sb.append("  Corrected Input: ")
              .append(getCorrectedInput())
              .append("\n");
            sb.append("  Contract Number: ")
              .append(getContractNumber())
              .append("\n");
            sb.append("  Part Number: ")
              .append(getPartNumber())
              .append("\n");
            sb.append("  Customer Number: ")
              .append(getCustomerNumber())
              .append("\n");
            sb.append("  Query Type: ")
              .append(getQueryType())
              .append("\n");
            sb.append("  Action Type: ")
              .append(getActionType())
              .append("\n");
            sb.append("  Processing Time: ")
              .append(getProcessingTimeMs())
              .append(" ms\n");
            sb.append("  Has Errors: ")
              .append(hasErrors())
              .append("\n");
            sb.append("  Has Spell Corrections: ")
              .append(hasSpellCorrections())
              .append("\n");
            sb.append("}");
            return sb.toString();
        }
    }


    // Minimal QueryFilter class for filters

    public static class QueryFilter {
        public String attribute;
        public String operator;
        public String value;

        public QueryFilter(String attribute, String operator, String value) {
            this.attribute = attribute;
            this.operator = operator;
            this.value = value;
        }
    }


    // Stub for extractFieldNamesFromPrompt
    private List<String> extractFieldNamesFromPrompt(String prompt) {
        List<String> fields = new ArrayList<>();
        // Simple extraction: split on comma, and, or, space
        String[] tokens = prompt.split(",| and | or | ");
        for (String token : tokens) {
            String t = token.trim().toUpperCase();
            if (isValidColumn(t, TableColumnConfig.TABLE_CONTRACTS))
                fields.add(t);
        }
        return fields;
    }


    // ENHANCED: Direct business term to display entity mapping
    private void extractBusinessTermDisplayEntities(String lowerPrompt,
                                                    List<String> displayEntities) {
        // Direct mapping for common business terms
        if (lowerPrompt.contains("pricing") || lowerPrompt.contains("price") || lowerPrompt.contains("cost") ||
            lowerPrompt.contains("prise")) {
            if (!displayEntities.contains("PRICE"))
                displayEntities.add("PRICE");
        }

        if (lowerPrompt.contains("minimum order") || lowerPrompt.contains("min order") ||
            lowerPrompt.contains("min order qty") || lowerPrompt.contains("minimum order quantity") ||
            lowerPrompt.contains("moq")) {
            if (!displayEntities.contains("MOQ"))
                displayEntities.add("MOQ");
        }

        if (lowerPrompt.contains("unit of measure") || lowerPrompt.contains("unit measure") ||
            lowerPrompt.contains("uom")) {
            if (!displayEntities.contains("UOM"))
                displayEntities.add("UOM");
        }

        if (lowerPrompt.contains("lead time") || lowerPrompt.contains("leadtime") ||
            lowerPrompt.contains("delivery time")) {
            if (!displayEntities.contains("LEAD_TIME"))
                displayEntities.add("LEAD_TIME");
        }

        if (lowerPrompt.contains("status") || lowerPrompt.contains("staus") || lowerPrompt.contains("condition")) {
            if (!displayEntities.contains("STATUS"))
                displayEntities.add("STATUS");
        }

        if (lowerPrompt.contains("classification") || lowerPrompt.contains("class") ||
            lowerPrompt.contains("item class")) {
            if (!displayEntities.contains("ITEM_CLASSIFICATION"))
                displayEntities.add("ITEM_CLASSIFICATION");
        }

        // ENHANCED: Additional business term patterns for failed cases
        if (lowerPrompt.contains("order qty") || lowerPrompt.contains("order quantity") ||
            lowerPrompt.contains("min order qty")) {
            if (!displayEntities.contains("MOQ"))
                displayEntities.add("MOQ");
        }

        if (lowerPrompt.contains("measure") && !lowerPrompt.contains("unit of measure")) {
            if (!displayEntities.contains("UOM"))
                displayEntities.add("UOM");
        }

        // ENHANCED: Handle payment terms specifically with comprehensive patterns
        if (lowerPrompt.matches(".*\\bpayment\\s+terms?\\b.*") || lowerPrompt.matches(".*\\bpayment\\s+term\\b.*") ||
            lowerPrompt.matches(".*\\bwhat.*payment.*terms?\\b.*") ||
            lowerPrompt.matches(".*\\bshow.*payment.*terms?\\b.*") ||
            lowerPrompt.matches(".*\\bpaymet\\s+terms?\\b.*") || lowerPrompt.matches(".*\\bpayement\\s+terms?\\b.*") ||
            lowerPrompt.matches(".*\\bwhat.*payment\\b.*") || lowerPrompt.matches(".*\\bshow.*payment\\b.*")) {
            if (!displayEntities.contains("PAYMENT_TERMS"))
                displayEntities.add("PAYMENT_TERMS");
            return; // Early return to prioritize specific field
        }

        // ENHANCED: Handle incoterms specifically with comprehensive patterns
        if (lowerPrompt.matches(".*\\bincoterms?\\b.*") || lowerPrompt.matches(".*\\bincoterm\\b.*") || lowerPrompt.matches(".*\\bincotems\\b.*")) { // Handle typo
            if (!displayEntities.contains("INCOTERMS"))
                displayEntities.add("INCOTERMS");
            return; // Early return to prioritize specific field
        }

        // ENHANCED: Handle contract length specifically with comprehensive patterns
        if (lowerPrompt.matches(".*\\bcontract\\s+length\\b.*") ||
            lowerPrompt.matches(".*\\bcontract\\s+lenght\\b.*") || // Handle typo
            lowerPrompt.matches(".*\\blength\\b.*contract.*")) {
            if (!displayEntities.contains("CONTRACT_LENGTH"))
                displayEntities.add("CONTRACT_LENGTH");
            return; // Early return to prioritize specific field
        }

        // ENHANCED: Handle failure reasons specifically
        if (lowerPrompt.contains("why") || lowerPrompt.contains("reason") || lowerPrompt.contains("caused") ||
            lowerPrompt.contains("failure")) {
            if (!displayEntities.contains("REASON"))
                displayEntities.add("REASON");
        }

        // ENHANCED: Handle "failing parts" specifically
        if (lowerPrompt.contains("failing parts") || lowerPrompt.contains("failing part")) {
            if (!displayEntities.contains("PART_NUMBER"))
                displayEntities.add("PART_NUMBER");
            if (!displayEntities.contains("ERROR_COLUMN"))
                displayEntities.add("ERROR_COLUMN");
            if (!displayEntities.contains("REASON"))
                displayEntities.add("REASON");
        }

        // ENHANCED: Handle "parts with issues" pattern
        if (lowerPrompt.contains("parts with issues") || lowerPrompt.contains("part with issues")) {
            if (!displayEntities.contains("PART_NUMBER"))
                displayEntities.add("PART_NUMBER");
            if (!displayEntities.contains("ERROR_COLUMN"))
                displayEntities.add("ERROR_COLUMN");
            if (!displayEntities.contains("REASON"))
                displayEntities.add("REASON");
        }
    }

    /**
     * FIXED: Enhanced natural language field extraction with comprehensive business term mapping
     */
    private void extractNaturalLanguageFields(String lowerPrompt,
                                              List<String> displayEntities) {
        // FIXED: Contract-specific field detection with enhanced patterns
        if (lowerPrompt.contains("effective date") || lowerPrompt.contains("start date") ||
            lowerPrompt.contains("begin date")) {
            if (!displayEntities.contains("EFFECTIVE_DATE"))
                displayEntities.add("EFFECTIVE_DATE");
        }
        if (lowerPrompt.contains("expiration date") || lowerPrompt.contains("expiry date") ||
            lowerPrompt.contains("end date") || lowerPrompt.contains("expire") || lowerPrompt.contains("expiring")) {
            if (!displayEntities.contains("EXPIRATION_DATE"))
                displayEntities.add("EXPIRATION_DATE");
        }
        if (lowerPrompt.contains("customer name") || lowerPrompt.matches(".*who('?| is|s)? the customer.*") ||
            lowerPrompt.contains("what customer") || lowerPrompt.contains("show customer")) {
            if (!displayEntities.contains("CUSTOMER_NAME"))
                displayEntities.add("CUSTOMER_NAME");
        }
        if (lowerPrompt.contains("customer number") || lowerPrompt.contains("custmer number")) {
            if (!displayEntities.contains("CUSTOMER_NUMBER"))
                displayEntities.add("CUSTOMER_NUMBER");
        }
        // PRIORITY: Payment terms detection with comprehensive patterns
        if (lowerPrompt.matches(".*\\bpayment\\s+terms?\\b.*") || lowerPrompt.matches(".*\\bpayment\\s+term\\b.*") ||
            lowerPrompt.matches(".*\\bwhat.*payment.*terms?\\b.*") ||
            lowerPrompt.matches(".*\\bshow.*payment.*terms?\\b.*") ||
            lowerPrompt.matches(".*\\bpaymet\\s+terms?\\b.*") || lowerPrompt.matches(".*\\bpayement\\s+terms?\\b.*") ||
            lowerPrompt.matches(".*\\bwhat.*payment\\b.*") || lowerPrompt.matches(".*\\bshow.*payment\\b.*")) {
            if (!displayEntities.contains("PAYMENT_TERMS"))
                displayEntities.add("PAYMENT_TERMS");
            return; // Early return to prioritize specific field
        }
        
        // PRIORITY: Incoterms detection with comprehensive patterns
        if (lowerPrompt.matches(".*\\bincoterms?\\b.*") || lowerPrompt.matches(".*\\bincoterm\\b.*") || lowerPrompt.matches(".*\\bincotems\\b.*")) { // Handle typo
            if (!displayEntities.contains("INCOTERMS"))
                displayEntities.add("INCOTERMS");
            return; // Early return to prioritize specific field
        }
        
        // PRIORITY: Contract length detection with comprehensive patterns
        if (lowerPrompt.matches(".*\\bcontract\\s+length\\b.*") ||
            lowerPrompt.matches(".*\\bcontract\\s+lenght\\b.*") || // Handle typo
            lowerPrompt.matches(".*\\blength\\b.*contract.*")) {
            if (!displayEntities.contains("CONTRACT_LENGTH"))
                displayEntities.add("CONTRACT_LENGTH");
            return; // Early return to prioritize specific field
        }
        if (lowerPrompt.contains("price expiration") || lowerPrompt.contains("price experation") ||
            lowerPrompt.contains("price expire") || lowerPrompt.contains("price expiry")) {
            if (!displayEntities.contains("PRICE_EXPIRATION_DATE"))
                displayEntities.add("PRICE_EXPIRATION_DATE");
        }
        if (lowerPrompt.contains("creation date") || lowerPrompt.contains("create date") ||
            lowerPrompt.contains("created") || lowerPrompt.contains("when created") ||
            lowerPrompt.contains("when was")) {
            if (!displayEntities.contains("CREATE_DATE"))
                displayEntities.add("CREATE_DATE");
        }
        if (lowerPrompt.contains("contract type") || lowerPrompt.contains("type of contract") ||
            lowerPrompt.contains("contract typ") || lowerPrompt.contains("what type") ||
            lowerPrompt.contains("what kind")) {
            if (!displayEntities.contains("CONTRACT_TYPE"))
                displayEntities.add("CONTRACT_TYPE");
        }
        if (lowerPrompt.contains("status") || lowerPrompt.contains("staus") || lowerPrompt.contains("is active") ||
            lowerPrompt.contains("is it active")) {
            if (!displayEntities.contains("STATUS"))
                displayEntities.add("STATUS");
        }

        // FIXED: Enhanced Parts-specific field detection with comprehensive business term mapping
        if (lowerPrompt.contains("lead time") || lowerPrompt.contains("lead tim") ||
            lowerPrompt.contains("leed time") || lowerPrompt.contains("leadtime")) {
            if (!displayEntities.contains("LEAD_TIME"))
                displayEntities.add("LEAD_TIME");
        }
        if (lowerPrompt.contains("price") || lowerPrompt.contains("pric") || lowerPrompt.contains("cost") ||
            lowerPrompt.contains("pricing") || lowerPrompt.contains("prise")) {
            if (!displayEntities.contains("PRICE"))
                displayEntities.add("PRICE");
        }
        if (lowerPrompt.contains("moq") || lowerPrompt.contains("minimum order") ||
            lowerPrompt.contains("min order qty") || lowerPrompt.contains("minimum order quantity")) {
            if (!displayEntities.contains("MOQ"))
                displayEntities.add("MOQ");
        }
        if (lowerPrompt.contains("uom") || lowerPrompt.contains("unit of measure") ||
            lowerPrompt.contains("unit measure") || lowerPrompt.contains("what unit") || lowerPrompt.contains("unit")) {
            if (!displayEntities.contains("UOM"))
                displayEntities.add("UOM");
        }
        if (lowerPrompt.contains("item classification") || lowerPrompt.contains("item class") ||
            lowerPrompt.contains("classification") || lowerPrompt.contains("class")) {
            if (!displayEntities.contains("ITEM_CLASSIFICATION"))
                displayEntities.add("ITEM_CLASSIFICATION");
        }

        // FIXED: Enhanced Failed parts-specific field detection with comprehensive patterns
        if (lowerPrompt.contains("error") || lowerPrompt.contains("errors") || lowerPrompt.contains("error column") ||
            lowerPrompt.contains("error columns") || lowerPrompt.contains("which columns") ||
            lowerPrompt.contains("column errors") || lowerPrompt.contains("column error")) {
            if (!displayEntities.contains("ERROR_COLUMN"))
                displayEntities.add("ERROR_COLUMN");
        }
        if (lowerPrompt.contains("reason") || lowerPrompt.contains("reasons") || lowerPrompt.contains("why") ||
            lowerPrompt.contains("failure reason") || lowerPrompt.contains("error reason") ||
            lowerPrompt.contains("what caused") || lowerPrompt.contains("caused")) {
            if (!displayEntities.contains("REASON"))
                displayEntities.add("REASON");
        }
        if (lowerPrompt.contains("failed part") || lowerPrompt.contains("failed parts") ||
            lowerPrompt.contains("part error") || lowerPrompt.contains("part errors") ||
            lowerPrompt.contains("failing parts") || lowerPrompt.contains("failing part")) {
            if (!displayEntities.contains("PART_NUMBER"))
                displayEntities.add("PART_NUMBER");
        }

        // FIXED: General field detection for mixed queries with enhanced context awareness
        if (lowerPrompt.contains("details") || lowerPrompt.contains("detials") || lowerPrompt.contains("info") ||
            lowerPrompt.contains("information") || lowerPrompt.contains("summary") ||
            lowerPrompt.contains("overview")) {
            // Add default fields based on context
            if (lowerPrompt.contains("part")) {
                if (!displayEntities.contains("LINE_NO"))
                    displayEntities.add("LINE_NO");
                if (!displayEntities.contains("INVOICE_PART_NUMBER"))
                    displayEntities.add("INVOICE_PART_NUMBER");
            } else {
                if (!displayEntities.contains("CONTRACT_NAME"))
                    displayEntities.add("CONTRACT_NAME");
                if (!displayEntities.contains("CUSTOMER_NAME"))
                    displayEntities.add("CUSTOMER_NAME");
            }
        }
    }

    /**
     * Process query and return JSON string following JSON_DESIGN.md standards
     * Enhanced with SpellCorrector add-on for comprehensive spell correction
     */
    public String processQuery(String originalInput) {
        long startTime = System.nanoTime();

        try {
            // Step 1: Input tracking and comprehensive spell correction (SpellCorrector + WordDatabase)
            InputTrackingResult inputTracking = processInputTracking(originalInput);
            String processedInput = inputTracking.correctedInput != null ? inputTracking.correctedInput : originalInput;

            // Step 1.5: Normalize prompt for action mapping
            String normalizedPrompt = normalizePrompt(processedInput);
            String mappedActionType = findoutTheActionType(normalizedPrompt); // FIXED METHOD CALL

            // Step 2: Header analysis
            HeaderResult headerResult = analyzeHeaders(processedInput);

            // Step 3: Entity extraction
            List<EntityFilter> entities = extractEntities(processedInput);

            // --- FIX 1: Remove redundant filters for failed parts by contract number ---
            if (mappedActionType != null && mappedActionType.equals("parts_failed_by_contract_number")) {
                // Only keep LOADED_CP_NUMBER filter
                List<EntityFilter> filtered = new ArrayList<>();
                for (EntityFilter e : entities) {
                    if ("LOADED_CP_NUMBER".equals(e.attribute)) {
                        filtered.add(e);
                    }
                }
                entities = filtered;
            }

            // --- FIX 2: Add CREATED_BY filter if present in header for 'created by' queries ---
            if (headerResult != null && headerResult.header != null && headerResult.header.createdBy != null) {
                boolean hasCreatedBy = false;
                for (EntityFilter e : entities) {
                    if ("CREATED_BY".equalsIgnoreCase(e.attribute)) {
                        hasCreatedBy = true;
                        break;
                    }
                }
                if (!hasCreatedBy) {
                    entities.add(new EntityFilter("CREATED_BY", "=", headerResult.header.createdBy, "user_input"));
                }
            }

            // Step 4: Validation
            List<ValidationError> errors = validateInput(headerResult, entities, processedInput);

            // FIXED: Enhanced processing with proper method calls
            String queryType = determineQueryType(processedInput, normalizedPrompt);
            String actionType = determineActionType(processedInput, normalizedPrompt, queryType);
            List<String> displayEntities = determineDisplayEntitiesFromPrompt(processedInput, normalizedPrompt);
            List<EntityFilter> filterEntities = determineFilterEntities(processedInput, normalizedPrompt, queryType);

            // CRITICAL FIX: Remove override that causes action type inconsistencies
            // The findoutTheActionType method returns inconsistent action type names
            // We should use the standardized action types from determineActionType
            // if (mappedActionType != null && !mappedActionType.trim().isEmpty()) {
            //     actionType = mappedActionType;
            // }
            
            // FINAL FALLBACK: For HELP queries, force actionType to HELP_CONTRACT_CREATE_USER unless already HELP_CONTRACT_CREATE_USER or HELP_CONTRACT_CREATE_BOT
            if ("HELP".equals(queryType) &&
                !("HELP_CONTRACT_CREATE_USER".equals(actionType) || "HELP_CONTRACT_CREATE_BOT".equals(actionType))) {
                actionType = "HELP_CONTRACT_CREATE_USER";
            }

            // FIXED: Create metadata with fixed values
            QueryMetadata metadata = new QueryMetadata(queryType, actionType, 0);

            // FIXED: Use filter entities instead of entities for consistency
            entities = filterEntities;

            // FIXED: Enhance multi-intent query processing
            enhanceMultiIntentProcessing(processedInput, displayEntities, entities);

            // Calculate processing time
            long endTime = System.nanoTime();
            double processingTime = (endTime - startTime) / 1_000_000.0;
            metadata.processingTimeMs = processingTime;

            // Step 7: Generate JSON according to JSON_DESIGN.md
            return generateStandardJSON(originalInput, inputTracking, headerResult, metadata, entities, displayEntities,
                                        errors);

        } catch (Exception e) {
            // Error fallback
            long endTime = System.nanoTime();
            double processingTime = (endTime - startTime) / 1_000_000.0;

            return generateErrorJSON(originalInput, e.getMessage(), processingTime);
        }
    }

    /**
     * Analyze PARTS action type
     */
    private String analyzePartsActionType(String lowerInput) {
        // Check if asking for parts by contract number
        if (lowerInput.contains("parts for contract") || (lowerInput.contains("parts") && Pattern.compile("\\b\\d{6,}\\b")
                                                                                                 .matcher(lowerInput)
                                                                                                 .find())) {
            return "PARTS_BY_CONTRACT_NUMBER";
        }

        // Check if asking for specific part details
        if (lowerInput.contains("for part") || lowerInput.contains("for parts") || Pattern.compile("\\b[A-Z]{2,4}[0-9-]+[A-Z]*\\b")
                                                                                          .matcher(lowerInput)
                                                                                          .find()) {
            return "PARTS_BY_PART_NUMBER";
        }

        // Check for parts with filters
        if (lowerInput.contains("failed parts") || lowerInput.contains("active parts") ||
            lowerInput.contains("expired parts")) {
            return "PARTS_BY_FILTER";
        }

        // Default parts action
        return "PARTS_BY_CONTRACT_NUMBER";
    }

    /**
     * Analyze CREATE action type
     */
    private String analyzeCreateActionType(String lowerInput) {
        // Check for help keywords (steps, help, how, guide, tell me, instructions)
        if (lowerInput.contains("steps") || lowerInput.contains("help") || lowerInput.contains("how") ||
            lowerInput.contains("guide") || lowerInput.contains("tell me") || lowerInput.contains("instructions")) {
            return "HELP_CONTRACT_CREATE_USER";
        }

        // Check if asking system to create for specific account/customer
        if (lowerInput.contains("for account") || lowerInput.contains("for customer") || Pattern.compile("\\b\\d{4,8}\\b")
                                                                                                .matcher(lowerInput)
                                                                                                .find()) {
            return "HELP_CONTRACT_CREATE_BOT";
        }

        // Default create action
        return "HELP_CONTRACT_CREATE_USER";
    }

    /**
     * Analyze CONTRACT action type
     */
    private String analyzeContractActionType(String lowerInput) {
        // Check for contract number (6+ digits)
        if (Pattern.compile("\\b\\d{6,}\\b")
                   .matcher(lowerInput)
                   .find()) {
            return "CONTRACTS_BY_CONTRACT_NUMBER";
        }

        // Check for customer number (4-8 digits)
        if (lowerInput.contains("customer") && Pattern.compile("\\b\\d{4,8}\\b")
                                                      .matcher(lowerInput)
                                                      .find()) {
            return "CONTRACTS_BY_CUSTOMER_NUMBER";
        }

        // Check for customer name (quoted or after "customer name")
        if (lowerInput.contains("customer name") || lowerInput.contains("account name") || Pattern.compile("'[^']+'|\"[^\"]+\"")
                                                                                                  .matcher(lowerInput)
                                                                                                  .find()) {
            return "CONTRACTS_BY_CUSTOMER_NAME";
        }

        // Check for created by
        if (lowerInput.contains("created by") || lowerInput.contains("by ")) {
            return "CONTRACTS_BY_CREATED_BY";
        }

        // Check for date filters
        if (lowerInput.contains("created in") || lowerInput.contains("after") || lowerInput.contains("before") ||
            lowerInput.contains("between")) {
            return "CONTRACTS_BY_DATE";
        }

        // Check for status filters
        if (lowerInput.contains("active") || lowerInput.contains("expired") || lowerInput.contains("failed") ||
            lowerInput.contains("status")) {
            return "CONTRACTS_BY_STATUS";
        }

        // Default contract action
        return "CONTRACTS_BY_CONTRACT_NUMBER";
    }


    /**
     * Helper method to detect parts keywords
     */
    private boolean containsPartsKeyword(String input) {
        return input.contains("parts") || input.contains("part ");
    }

    /**
     * Helper method to detect contract indicators
     */
    private boolean containsContractIndicators(String input) {
        return input.contains("contract") || Pattern.compile("\\b\\d{6,}\\b")
                                                    .matcher(input)
                                                    .find();
    }


    public String processUserRequest(String userInput) {
        try {
            // Step 1: Process user input through NLP
            String jsonResponse = processQuery(userInput);
            QueryResult queryResult = parseJSONToObject(jsonResponse);
            
            // Step 2: Extract action type and route to appropriate handler
            String actionType = queryResult.metadata.actionType;
            List<EntityFilter> filters = queryResult.entities;
            List<String> displayEntities = queryResult.displayEntities;
            
            // Step 3: Route to appropriate action method based on action type
            String response = routeToActionHandler(actionType, filters, displayEntities, userInput, queryResult);
            
            return response;
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error processing user request: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Routes to appropriate action handler based on action type
     */
    private String routeToActionHandler(String actionType, List<EntityFilter> filters, List<String> displayEntities,
                                        String userInput, QueryResult queryResult) {
        
        ActionTypeDataProvider dataProvider = new ActionTypeDataProvider();
        
        switch (actionType) {
            case "contracts_by_contractnumber":
                return handleContractByContractNumber(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "parts_by_contract_number":
                return handlePartsByContractNumber(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "parts_failed_by_contract_number":
                return handleFailedPartsByContractNumber(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "parts_by_part_number":
                return handlePartsByPartNumber(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "contracts_by_filter":
                return handleContractsByFilter(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "parts_by_filter":
                return handlePartsByFilter(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "update_contract":
                return handleUpdateContract(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "create_contract":
                return handleCreateContract(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "HELP_CONTRACT_CREATE_USER":
                return handleHelpContractCreateUser(dataProvider, filters, displayEntities, userInput, queryResult);
                
            case "HELP_CONTRACT_CREATE_BOT":
                return handleHelpContractCreateBot(dataProvider, filters, displayEntities, userInput, queryResult);
                
            default:
                return generateErrorJSON(userInput, "Unknown action type: " + actionType, 0);
        }
    }
    
    // ============================================================================
    // ACTION HANDLERS
    // ============================================================================
    
    private String handleContractByContractNumber(ActionTypeDataProvider dataProvider, List<EntityFilter> filters, 
                                                  List<String> displayEntities, String userInput,
                                                  QueryResult queryResult) {
        try {
            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
            if (contractNumber == null) {
                return generateErrorJSON(userInput, "Contract number not found in query", 0);
            }
            
            // Call data provider method
            List<Map<String, Object>> results =
                dataProvider.getContractByContractNumber(contractNumber, displayEntities);
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, results, "Contract query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in contract query: " + e.getMessage(), 0);
        }
    }
    
    private String handlePartsByContractNumber(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                               List<String> displayEntities, String userInput,
                                               QueryResult queryResult) {
        try {
            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
            if (contractNumber == null) {
                return generateErrorJSON(userInput, "Contract number not found in query", 0);
            }
            
            // Call data provider method
            List<Map<String, Object>> results = dataProvider.getPartsByContractNumber(contractNumber, displayEntities);
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, results,
                                       "Parts by contract query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in parts by contract query: " + e.getMessage(), 0);
        }
    }
    
    private String handleFailedPartsByContractNumber(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                                     List<String> displayEntities, String userInput,
                                                     QueryResult queryResult) {
        try {
            String contractNumber = extractFilterValue(filters, "LOADED_CP_NUMBER");
            if (contractNumber == null) {
                return generateErrorJSON(userInput, "Contract number not found in failed parts query", 0);
            }
            
            // Call data provider method
            List<Map<String, Object>> results =
                dataProvider.getFailedPartsByContractNumber(contractNumber, displayEntities);
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, results, "Failed parts query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in failed parts query: " + e.getMessage(), 0);
        }
    }
    
    private String handlePartsByPartNumber(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                          List<String> displayEntities, String userInput, QueryResult queryResult) {
        try {
            String partNumber = extractFilterValue(filters, "INVOICE_PART_NUMBER");
            if (partNumber == null) {
                return generateErrorJSON(userInput, "Part number not found in query", 0);
            }
            
            // Call data provider method
            List<Map<String, Object>> results = dataProvider.getPartsByPartNumber(partNumber, displayEntities);
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, results,
                                       "Parts by part number query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in parts by part number query: " + e.getMessage(), 0);
        }
    }
    
    private String handleContractsByFilter(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                          List<String> displayEntities, String userInput, QueryResult queryResult) {
        try {
            // Call data provider method
            List<Map<String, Object>> results = dataProvider.getContractsByFilter(filters, displayEntities);
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, results, "Contract filter query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in contract filter query: " + e.getMessage(), 0);
        }
    }
    
    private String handlePartsByFilter(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                      List<String> displayEntities, String userInput, QueryResult queryResult) {
        try {
            // Call data provider method
            List<Map<String, Object>> results = dataProvider.getPartsByFilter(filters, displayEntities);
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, results, "Parts filter query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in parts filter query: " + e.getMessage(), 0);
        }
    }
    
    private String handleUpdateContract(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                       List<String> displayEntities, String userInput, QueryResult queryResult) {
        try {
            String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
            if (contractNumber == null) {
                return generateErrorJSON(userInput, "Contract number not found in update query", 0);
            }
            
            // Call data provider method
            int result = dataProvider.updateContract(contractNumber, createParameterMapFromFilters(filters));
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, null, "Contract update query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in contract update query: " + e.getMessage(), 0);
        }
    }
    
    private String handleCreateContract(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                       List<String> displayEntities, String userInput, QueryResult queryResult) {
        try {
            // Call data provider method
            int result = dataProvider.createContract(createParameterMapFromFilters(filters));
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, null, "Contract creation query processed successfully");
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in contract creation query: " + e.getMessage(), 0);
        }
    }
    
    private String handleHelpContractCreateUser(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                                List<String> displayEntities, String userInput,
                                                QueryResult queryResult) {
        try {
            // Call data provider method
            String helpContent = dataProvider.getHelpContractCreateUser();
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, null, helpContent);
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in help request: " + e.getMessage(), 0);
        }
    }
    
    private String handleHelpContractCreateBot(ActionTypeDataProvider dataProvider, List<EntityFilter> filters,
                                               List<String> displayEntities, String userInput,
                                               QueryResult queryResult) {
        try {
            // Call data provider method
            String helpContent = dataProvider.getHelpContractCreateBot();
            
            // Generate response
            return generateSuccessJSON(userInput, queryResult, null, helpContent);
            
        } catch (Exception e) {
            return generateErrorJSON(userInput, "Error in help request: " + e.getMessage(), 0);
        }
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    private String extractFilterValue(List<EntityFilter> filters, String attributeName) {
        for (EntityFilter filter : filters) {
            if (filter.attribute.equals(attributeName)) {
                return filter.value;
            }
        }
        return null;
    }
    
    private Map<String, Object> createParameterMapFromFilters(List<EntityFilter> filters) {
        Map<String, Object> parameters = new HashMap<>();
        if (filters != null) {
            for (EntityFilter filter : filters) {
                parameters.put(filter.attribute, filter.value);
            }
        }
        return parameters;
    }
    
    private String generateSuccessJSON(String userInput, QueryResult queryResult, List<Map<String, Object>> results,
                                       String message) {
        // Generate success response JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": true,\n");
        json.append("  \"message\": \"")
            .append(message)
            .append("\",\n");
        json.append("  \"userInput\": \"")
            .append(userInput)
            .append("\",\n");
        json.append("  \"queryType\": \"")
            .append(queryResult.metadata.queryType)
            .append("\",\n");
        json.append("  \"actionType\": \"")
            .append(queryResult.metadata.actionType)
            .append("\",\n");
        json.append("  \"processingTimeMs\": ")
            .append(queryResult.metadata.processingTimeMs)
            .append(",\n");
        if (results != null && !results.isEmpty()) {
            json.append("  \"results\": [\n");
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> result = results.get(i);
                json.append("    {\n");
                int j = 0;
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    json.append("      \"")
                        .append(entry.getKey())
                        .append("\": \"")
                        .append(entry.getValue())
                        .append("\"");
                    if (++j < result.size())
                        json.append(",");
                    json.append("\n");
                }
                json.append("    }");
                if (i < results.size() - 1)
                    json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
        } else {
            json.append("  \"results\": []\n");
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Testing purpose created main method
     */


    // Business field synonyms - NOW USING CENTRALIZED CONFIG
    // BUSINESS_FIELD_SYNONYMS are now managed by TableColumnConfig

    /**
     * Map user field name to canonical DB column name for display extraction
     * Returns null if not recognized.
     */
    private String mapToColumn(String field, boolean isPartsQuery) {
        if (field == null || field.trim().isEmpty())
            return null;
        String cleaned = field.trim()
                              .replaceAll("[^a-zA-Z0-9_]", "")
                              .toUpperCase();
        // Direct match
        if (isValidColumn(cleaned, TableColumnConfig.TABLE_CONTRACTS))
            return cleaned;
        // Business synonyms (single field) - NOW USING CENTRALIZED CONFIG
        String lower = field.trim().toLowerCase();
        String mappedColumn = TABLE_CONFIG.getColumnForSynonym(TableColumnConfig.TABLE_CONTRACTS, lower);
        if (mappedColumn == null) {
            mappedColumn = TABLE_CONFIG.getColumnForSynonym(TableColumnConfig.TABLE_PARTS, lower);
        }
        if (mappedColumn == null) {
            mappedColumn = TABLE_CONFIG.getColumnForSynonym(TableColumnConfig.TABLE_FAILED_PARTS, lower);
        }
        if (mappedColumn != null) {
            return mappedColumn;
        }
        // Common part fields
        if (isPartsQuery) {
            if (cleaned.equals("PRICE") || cleaned.equals("EAU") || cleaned.equals("MOQ") ||
                cleaned.equals("LEADTIME") || cleaned.equals("UOM") || cleaned.equals("COST")) {
                return cleaned;
            }
            if (cleaned.equals("PARTNUMBER"))
                return "PART_NUMBER";
        } else {
            if (cleaned.equals("CONTRACTNAME"))
                return "CONTRACT_NAME";
            if (cleaned.equals("CUSTOMERNAME"))
                return "CUSTOMER_NAME";
        }
        // Fallback: try underscore version
        String underscored = cleaned.replace(" ", "_");
        if (isValidColumn(underscored, TableColumnConfig.TABLE_CONTRACTS))
            return underscored;
        return null;
    }
    // Robust, rule-based action type detection (restored for high accuracy)
    private String findoutTheActionType(String normalizedInput) {
        String lowerInput = normalizedInput.toLowerCase();
        boolean hasParts = lowerInput.contains("part") || lowerInput.contains("parts");
        boolean hasContract = lowerInput.contains("contract") || lowerInput.contains("contracts");
        boolean hasContractNumber = lowerInput.matches(".*\\b\\d{6,}\\b.*");

        // Enhanced part number detection per token
        boolean hasPartNumber = false;
        String[] tokens = normalizedInput.split("[\\s,]+");
        for (String token : tokens) {
            if (token.matches("(?=[A-Za-z0-9_/-]{3,})(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9_/-]+$")) {
                hasPartNumber = true;
                break;
            }
        }

        // Enhanced part attribute detection
        boolean hasPartAttribute =
            lowerInput.matches(".*\\b(price|eau|moq|lead time|uom|cost|future price|item classification|quote cost|comments|gt_25|part_file|kitting|pl_3|is_program|is_hpp_unpriced_contract)\\b.*");

        // Check for specific part-related patterns
        boolean hasPriceQuery = lowerInput.matches(".*\\b(price|pricing|cost|prise)\\b.*");
        boolean hasMoqQuery = lowerInput.matches(".*\\b(moq|minimum order|min order qty|minimum order quantity)\\b.*");
        boolean hasUomQuery = lowerInput.matches(".*\\b(uom|unit of measure|unit measure|unit)\\b.*");
        boolean hasStatusQuery = lowerInput.matches(".*\\b(status|staus)\\b.*");
        boolean hasClassificationQuery =
            lowerInput.matches(".*\\b(classification|class|item class|item classification)\\b.*");

        boolean hasCustomerNumber = lowerInput.matches(".*customer\\s*\\d{4,8}.*");
        boolean hasCreateIntent =
            lowerInput.contains("create contract for account") || lowerInput.contains("create contract for customer") ||
            lowerInput.matches(".*\\bcreate contract for\\b.*");
        boolean hasUpdateIntent =
            lowerInput.matches(".*\\b(update|change|modify|set|approve|reject|archive|extend|renew|terminate|reactivate|clone|resubmit|reprice|notify|send|put)\\b.*");
        boolean hasFilter =
            lowerInput.matches(".*(>|<|=|between|after|before|with|where|by|type|status|moq|rebate|minimum|fee|expir|active|failed|program|vmi|cmi|kitting|pl_3|hpp|unpriced|customer) .*");

        // Step 0: Check for failed parts queries FIRST (highest priority)
        boolean hasFailed =
            lowerInput.contains("failed") || lowerInput.contains("failure") || lowerInput.contains("fail") ||
            lowerInput.contains("failed parts") || lowerInput.contains("error") || lowerInput.contains("errors") ||
            lowerInput.contains("error column") || lowerInput.contains("error columns") ||
            lowerInput.contains("which columns") || lowerInput.contains("column errors");
        if (hasFailed && (hasParts || hasPartAttribute)) {
            if (hasContractNumber) {
                return "failed_parts_by_contractnumber";
            } else {
                return "failed_parts_by_filter";
            }
        }

        // Step 1: Check for update/action/command verbs
        if (hasUpdateIntent) {
            if (hasParts || hasPartNumber || hasPartAttribute) {
                if (lowerInput.contains("price"))
                    return "update_part_price";
                return "update_part";
            }
            return "update_contract";
        }

        // PRIORITY: Price expiration is a contract field, not parts - handle first
        if (hasPriceQuery &&
            (lowerInput.contains("expir") || lowerInput.contains("expiry") || lowerInput.contains("experation") ||
             lowerInput.contains("expire"))) {
            if (hasContractNumber) {
                return "contracts_by_contractnumber";
            } else {
                return "contracts_by_filter";
            }
        }
        
        // Step 2: Enhanced parts query detection (HIGH PRIORITY)
        // Check for part-specific attribute queries with part numbers
        if ((hasPriceQuery || hasMoqQuery || hasUomQuery || hasStatusQuery || hasClassificationQuery) &&
            hasPartNumber) {
            return "parts_by_part_number";
        }

        // Check for parts queries with part numbers
        if (hasParts && hasPartNumber) {
            return "parts_by_partnumber";
        }

        // Check for parts queries with contract numbers
        if (hasParts && hasContractNumber) {
            return "parts_by_contractnumber";
        }

        // Check for parts queries with part attributes (even without explicit "part" keyword)
        if (hasPartAttribute && hasPartNumber) {
            return "parts_by_partnumber";
        }

        // Check for general parts queries
        if (hasParts || hasPartAttribute) {
            return "parts_by_partnumber"; // Default to parts_by_partnumber for parts queries
        }

        // Step 3: Check for contract queries with contract number (PRIORITY)
        if (!hasParts && hasContractNumber && !hasPartNumber && !hasPartAttribute) {
            return "contracts_by_contractnumber";
        }

        // Step 4: Check for other contract queries
        if (hasFilter && !hasContractNumber) {
            return "contracts_by_filter";
        }
        if (hasCustomerNumber && !hasContractNumber) {
            return "contracts_by_filter";
        }
        if (hasContract &&
            (lowerInput.contains("program") || lowerInput.contains("vmi enabled") || lowerInput.contains("rebate") ||
             lowerInput.contains("type ") || lowerInput.contains("expiring") || lowerInput.contains("expire") ||
             lowerInput.contains("notice") || lowerInput.contains("within ") || lowerInput.contains("minimum") ||
             lowerInput.contains("fee"))) {
            return "contracts_by_filter";
        }

        // Step 5: Check for create/help queries (HIGHEST PRIORITY for HELP)
        if (lowerInput.contains("create contract") || lowerInput.contains("create contarct") ||
            lowerInput.contains("make a contract") || lowerInput.contains("generate a contract") ||
            lowerInput.contains("set up a contract") || lowerInput.contains("draft a contract") ||
            lowerInput.contains("initiate contract") || lowerInput.contains("start a new contract") ||
            lowerInput.contains("create for me") || lowerInput.contains("need you to create") ||
            lowerInput.contains("make me a contract") ||
            lowerInput.contains("i need you to create")) {
            // Check for instruction requests
            if (lowerInput.contains("how to") || lowerInput.contains("steps to") ||
                lowerInput.contains("tell me how") || lowerInput.contains("guide") ||
                lowerInput.contains("instructions") || lowerInput.contains("process") ||
                lowerInput.contains("steps for") || lowerInput.contains("guide to") ||
                lowerInput.contains("show me how") || lowerInput.contains("what's the process") ||
                lowerInput.contains("need guidance") || lowerInput.contains("walk me through") ||
                lowerInput.contains("explain how to") || lowerInput.contains("need help understanding") ||
                lowerInput.contains("guidance on") || lowerInput.contains("understanding") ||
                lowerInput.contains("through contract") || lowerInput.contains("help understanding")) {
                return "HELP_CONTRACT_CREATE_USER";
            }
            // Check for system creation requests
            if (lowerInput.contains("can you") || lowerInput.contains("please") || lowerInput.contains("for me") ||
                lowerInput.contains("create a") || lowerInput.contains("need you to") ||
                lowerInput.contains("make me") || lowerInput.contains("set up") || lowerInput.contains("initiate") ||
                lowerInput.contains("start a new") || lowerInput.contains("draft") || lowerInput.contains("make a") ||
                lowerInput.contains("generate") || lowerInput.contains("could you") ||
                lowerInput.contains("i need you to")) {
            return "HELP_CONTRACT_CREATE_BOT";
        }
            // Default for create contract queries
                return "HELP_CONTRACT_CREATE_USER";
            }
        if (hasCreateIntent) {
            return "HELP_CONTRACT_CREATE_BOT";
        }

        // Step 6: General contract queries (lowest priority)
        if (hasContract) {
            return "contracts_list";
        }

        // Step 7: Search queries
        if (hasContractNumber && !hasCreateIntent && !hasUpdateIntent) {
            return "contracts_by_contractnumber";
        }
        // FINAL FALLBACK: Default to contracts search
        return "contracts_search";
    }


    private static List<String> getAllTestQueries() {
        return Arrays.asList(
               // CONTRACT QUERIES
               "What is the effective date for contarct 123456?", "Show me contract detials for 789012",
               "When does contrat 456789 expire?", "What's the experation date for 234567?",
               "Get contarct informaton for 345678", "Whats the efective date for 567890?",
               "Show contarct 678901 details", "What is efective date of 789012?", "Get contract info for 890123",
               "Show me contarct 123456", "effective date for 123456", "show 789012 details", "when does 456789 end",
               "234567 expiration", "345678 info", "567890 effective date", "678901 contract details",
               "789012 expiry date", "890123 start date", "123456 begin date", "whos the customer for contarct 123456?",
               "customer name for 234567", "what customer for contrat 345678?", "show custmer for 456789",
               "customer detials for 567890", "who is custommer for 678901?", "get customer info for contarct 789012",
               "customer number for 890123", "show custmer number for 123456", "what custommer number for 234567?",
               "payment terms for contarct 123456", "what are paymet terms for 234567?", "show payment term for 345678",
               "payement terms for 456789", "what payment for contrat 567890?", "incoterms for contarct 678901",
               "what incoterm for 789012?", "show incotems for 890123", "contract lenght for 123456",
               "what contract length for 234567?", "price experation date for 123456", "when price expire for 234567?",
               "price expiry for contarct 345678", "show price experation for 456789",
               "what price expire date for 567890?", "creation date for contarct 678901", "when was 789012 created?",
               "create date for 890123", "show creation for contarct 123456", "when created 234567?",
               "what type of contarct 123456?", "contract typ for 234567", "show contarct type for 345678",
               "what kind contract 456789?", "type of contrat 567890", "status of contarct 678901",
               "what status for 789012?", "show contarct status for 890123", "is 123456 active?",
               "contract staus for 234567", "show all details for 123456", "get everything for contarct 234567",
               "full info for 345678", "complete details contrat 456789", "show summary for 567890",
               "overview of contarct 678901", "brief for 789012", "quick info 890123", "details about 123456", "information on contarct 234567",

               // PARTS QUERIES
               "What is the lead time for part AE12345?", "Show me part detials for BC67890",
               "What lead tim for part DE23456?", "Show leadtime for FG78901", "What's the leed time for part HI34567?",
               "Get part informaton for JK89012", "Show part info for LM45678", "What part details for NO90123?",
               "Get part data for PQ56789", "Show part summary for RS12345", "What's the price for part AE12345?",
               "Show pric for part BC67890", "What cost for part DE23456?", "Get price info for FG78901",
               "Show pricing for part HI34567", "What's the prise for JK89012", "Cost of part LM45678",
               "Price details for NO90123", "Show part price for PQ56789", "What pricing for RS12345?",
               "What's the MOQ for part AE12345?", "Show minimum order for BC67890", "What min order qty for DE23456?",
               "MOQ for part FG78901", "Minimum order quantity for HI34567", "What UOM for part JK89012?",
               "Show unit of measure for LM45678", "UOM for NO90123", "Unit measure for PQ56789",
               "What unit for part RS12345?", "What's the status of part AE12345?", "Show part staus for BC67890",
               "Status for part DE23456", "What status FG78901?", "Show part status for HI34567",
               "Is part JK89012 active?", "Part status for LM45678", "What's status of NO90123?",
               "Show status for PQ56789", "Status info for RS12345", "What's the item classification for AE12345?",
               "Show item class for BC67890", "Classification for part DE23456", "What class for FG78901?",
               "Item classification HI34567", "Show classification for JK89012", "What item class for LM45678?",
               "Classification of NO90123", "Item class for PQ56789", "Show class for RS12345",
               "Show me invoice parts for 123456", "What invoice part for 234567?", "List invoce parts for 345678",
               "Show invoice part for 456789", "What invoice parts in 567890?", "Get invoice part for 678901",
               "Show invoic parts for 789012", "List invoice part for 890123", "What invoice parts for 123456?",
               "Show all invoice part for 234567", "Show me all parts for contarct 123456", "What parts in 234567?",
               "List part for contract 345678", "Show parts for contrat 456789", "What parts loaded in 567890?",
               "Get parts for contarct 678901", "Show all part for 789012", "List parts in contract 890123",
               "What parts for 123456?", "Show part list for 234567",
               // FAILED PARTS QUERIES (continued)
               "What parts failed in 567890?", "Get failed parts for 678901", "Show failing parts for 789012",
               "List failed part for 890123", "What failed parts for 123456?", "Show all failed part for 234567",
               "Why did parts fail for 123456?", "Show error reasons for 234567",
               "What errors for failed parts 345678?", "Why parts failed in 456789?", "Show failure reasons for 567890",
               "What caused parts to fail for 678901?", "Error details for failed parts 789012",
               "Why failed parts in 890123?", "Show error info for 123456", "What errors caused failure for 234567?",
               "Show me part errors for 123456", "What part error for 234567?", "List parts with errors for 345678",
               "Show error parts for 456789", "What parts have errors in 567890?", "Get parts errors for 678901",
               "Show parts with issues for 789012", "List error parts for 890123", "What parts errors for 123456?",
               "Show all error parts for 234567", "What columns have errors for 123456?",
               "Show error columns for 234567", "Which columns failed for 345678?", "What column errors for 456789?",
               "Show failed columns for 567890", "Error column details for 678901",
               "What columns with errors for 789012?", "Show column failures for 890123",
               "Which columns error for 123456?", "Error column info for 234567",
               "Show me parts with missing data for 123456", "What parts missing info for 234567?",
               "List parts with no data for 345678", "Show incomplete parts for 456789",
               "What parts missing data in 567890?", "Get parts with missing info for 678901",
               "Show parts missing data for 789012", "List incomplete parts for 890123",
               "What parts no data for 123456?", "Show missing data parts for 234567",
               "What errors occurred during loading for 123456?", "Show loading errors for 234567",
               "What load errors for 345678?", "Show loading issues for 456789",
               "What happened during load for 567890?", "Loading error details for 678901",
               "What load problems for 789012?", "Show load failures for 890123", "Loading issues for 123456",
               "What errors during loading for 234567?", "List validation issues for 123456",
               "Show validation errors for 234567", "What validation problems for 345678?",
               "Show validation issues for 456789", "What validation errors in 567890?",
               "Get validation problems for 678901", "Show validation failures for 789012",
               "List validation errors for 890123", "What validation issues for 123456?", "Show validation problems for 234567",

               // MIXED AND COMPLEX QUERIES
               "Show me contarct details and failed parts for 123456",
               "What's the effective date and part errors for 234567?",
               "List all parts and customer info for contrat 345678", "Show contract info and failed part for 456789",
               "Get customer name and parts errors for 567890", "Show contarct details and part issues for 678901",
               "What effective date and failed parts for 789012?", "List contract info and error parts for 890123",
               "tell me about contract 123456", "i need info on 234567", "can you show me 345678",
               "what do you know about contarct 456789", "give me details for 567890", "i want to see 678901",
               "please show 789012", "can i get info on 890123", "help me with contract 123456",
               "i need help with 234567", "whats up with 123456?", "hows 234567 looking?",
               "anything wrong with 345678?", "is 456789 ok?", "problems with 567890?", "issues with 678901?",
               "troubles with 789012?", "concerns about 890123?", "status on 123456?", "update on 234567?");
    }


    // ENHANCED: Comprehensive business rule validation from ContractsModel
    private List<String> validateBusinessRules(String queryType, List<EntityFilter> entities) {
        List<String> validationErrors = new ArrayList<>();

        // Validate contract numbers
        for (EntityFilter entity : entities) {
            if ("AWARD_NUMBER".equals(entity.attribute) || "LOADED_CP_NUMBER".equals(entity.attribute)) {
                if (!isValidContractNumber(entity.value)) {
                    validationErrors.add("Invalid contract number format: " + entity.value);
                }
            }
        }

        // Validate part numbers
        for (EntityFilter entity : entities) {
            if ("INVOICE_PART_NUMBER".equals(entity.attribute)) {
                if (!isValidPartNumber(entity.value)) {
                    validationErrors.add("Invalid part number format: " + entity.value);
                }
            }
        }

        // Validate customer identifiers
        for (EntityFilter entity : entities) {
            if ("CUSTOMER_NUMBER".equals(entity.attribute) || "CUSTOMER_NAME".equals(entity.attribute)) {
                if (!isValidCustomerIdentifier(entity.value)) {
                    validationErrors.add("Invalid customer identifier: " + entity.value);
                }
            }
        }

        // Query type specific validations
        switch (queryType) {
        case "FAILED_PARTS":
            boolean hasContractFilter =
                entities.stream()
                .anyMatch(e -> "AWARD_NUMBER".equals(e.attribute) || "LOADED_CP_NUMBER".equals(e.attribute));
            if (!hasContractFilter) {
                validationErrors.add("Failed parts queries require a contract number");
            }
            break;

        case "PARTS":
            boolean hasPartOrContractFilter =
                entities.stream()
                .anyMatch(e -> "INVOICE_PART_NUMBER".equals(e.attribute) || "AWARD_NUMBER".equals(e.attribute) ||
                          "LOADED_CP_NUMBER".equals(e.attribute));
            if (!hasPartOrContractFilter) {
                validationErrors.add("Parts queries require either a part number or contract number");
            }
            break;
        }

        return validationErrors;
    }

    /**
     * Check if a word is a common word that shouldn't be extracted as a part number
     */
    private boolean isCommonWord(String word) {
        Set<String> commonWords =
            new HashSet<>(Arrays.asList("DETAILS", "INFO", "INFORMATION", "DATA", "SUMMARY", "STATUS", "STAUS", "PRICE",
                                        "COST", "MOQ", "UOM", "LEAD", "TIME", "CLASS", "CLASSIFICATION", "FOR", "OF",
                                        "THE", "AND", "OR", "WITH", "FROM", "TO", "IN", "ON", "AT", "BY", "FOR",
                                        "ABOUT", "WITH", "WITHOUT", "BETWEEN", "AMONG", "DURING", "BEFORE", "AFTER",
                                        "SINCE", "UNTIL", "WHILE", "WHEN", "WHERE", "WHY", "HOW", "WHAT", "WHICH",
                                        "WHO", "WHOSE", "WHOM", "THAT", "THIS", "THESE", "THOSE", "ALL", "ANY", "EACH",
                                        "EVERY", "SOME", "MANY", "MOST", "FEW", "SEVERAL", "VARIOUS", "DIFFERENT",
                                        "SAME", "SIMILAR", "OTHER", "ANOTHER", "NEXT", "PREVIOUS", "CURRENT", "RECENT",
                                        "OLD", "NEW", "LATEST", "EARLIEST"));
        return commonWords.contains(word.toUpperCase());
    }

        /**
     * FIXED: Enhanced display entity extraction with precision targeting
     */
    private List<String> determineDisplayEntitiesFromPrompt(String originalPrompt, String normalizedPrompt) {
        List<String> displayEntities = new ArrayList<>();
        String lowerPrompt = originalPrompt.toLowerCase();
        String correctedQuery = normalizedPrompt.toLowerCase();
        String queryType = determineQueryType(originalPrompt, normalizedPrompt);

        // ENHANCED: Specific field request detection for precise responses
        
        // CRITICAL FIX: Failed parts display entity detection
        if ("FAILED_PARTS"
            .equals(queryType)) {
            // Error reasons/columns specific requests
            if (correctedQuery.matches(".*\\b(error\\s+reasons?|error\\s+columns?|error\\s+details?)\\b.*") ||
                correctedQuery.matches(".*\\b(show|get|what)\\s+error\\b.*")) {
                displayEntities.clear();
                displayEntities.add("PART_NUMBER");
                displayEntities.add("ERROR_COLUMN");
                displayEntities.add("REASON");
                return displayEntities;
            }
            
            // Failed parts general requests
            if (correctedQuery.matches(".*\\b(failed\\s+parts?|faild\\s+parts?|parts?\\s+with\\s+(issues?|errors?|problems?|failures?))\\b.*")) {
                displayEntities.clear();
                displayEntities.add("PART_NUMBER");
                displayEntities.add("ERROR_COLUMN");
                displayEntities.add("REASON");
                return displayEntities;
            }
            
            // Missing data requests
            if (correctedQuery.matches(".*\\b(missing\\s+data|no\\s+data|incomplete\\s+data|invalid\\s+data)\\b.*")) {
                displayEntities.clear();
                displayEntities.add("PART_NUMBER");
                displayEntities.add("ERROR_COLUMN");
                displayEntities.add("REASON");
                return displayEntities;
            }
            
            // Default for failed parts queries
            displayEntities.clear();
            displayEntities.add("PART_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
            return displayEntities;
        }
        
        // Customer number specific requests
        if (correctedQuery.matches(".*\\b(what|show|get).*customer\\s+number\\b.*") ||
            correctedQuery.matches(".*\\bcustomer\\s+number\\s+for\\b.*")) {
            displayEntities.clear();
            displayEntities.add("CUSTOMER_NUMBER");
            return displayEntities;
        }
        
        // Customer name specific requests (who is customer)
        if (correctedQuery.matches(".*\\b(who|what).*customer\\b.*") && !correctedQuery.contains("number") &&
            !correctedQuery.matches(".*\\b(show|get)\\s+customer\\s+(info|details)\\b.*")) {
            displayEntities.clear();
            displayEntities.add("CUSTOMER_NAME");
            return displayEntities;
        }
        
        // Price expiration specific requests
        if (correctedQuery.matches(".*\\bprice\\s+(expir|expiration).*date\\b.*") ||
            correctedQuery.matches(".*\\bprice\\s+expire\\s+date\\b.*")) {
            displayEntities.clear();
            displayEntities.add("PRICE_EXPIRATION_DATE");
            return displayEntities;
        }

        // CRITICAL FIX: Enhanced generic detail patterns for contract info (but not for parts)
        if ("CONTRACTS".equals(queryType) && 
            (correctedQuery.matches(".*\\b(show|get|display).*\\d+.*\\b(details|detail)\\b.*") ||
             correctedQuery.matches(".*\\b\\d+\\s+(info|information|details|detail)\\b.*") ||
             correctedQuery.matches(".*\\b(all|complete|full)\\s+(details|detail|info|information)\\b.*") ||
             correctedQuery.matches(".*\\b(summary|overview|brief|quick\\s+info)\\b.*") ||
             correctedQuery.matches(".*\\b(details|information)\\s+(about|on|for)\\b.*"))) {
            displayEntities.clear();
            displayEntities.add("CONTRACT_NAME");
            displayEntities.add("CUSTOMER_NAME");
            displayEntities.add("EFFECTIVE_DATE");
            displayEntities.add("EXPIRATION_DATE");
            displayEntities.add("STATUS");
            displayEntities.add("CONTRACT_TYPE");
            displayEntities.add("PAYMENT_TERMS");
            return displayEntities;
        }
        
        // 2. EXPIRATION_DATE
        if ((correctedQuery.matches(".*\\b(expire|expir|end)\\b.*") || correctedQuery.contains("expiration date") ||
             correctedQuery.contains("expiry date")) && !correctedQuery.contains("price")) {
            displayEntities.add("EXPIRATION_DATE");
            return displayEntities;
        }
        
        // 3. CONTRACT_TYPE
        if ((correctedQuery.matches(".*\\b(type|typ|kind)\\b.*contract.*") ||
             correctedQuery.matches(".*contract\\s+(type|typ|kind)\\b.*") || correctedQuery.contains("contract type") ||
             correctedQuery.contains("contract kind"))) {
            displayEntities.add("CONTRACT_TYPE");
            return displayEntities;
        }
        
        // FIXED: Enhanced status display logic for parts queries
        if ("PARTS".equals(queryType) && 
            (correctedQuery.matches(".*\\bstatus\\b.*") || correctedQuery.contains("status"))) {
            displayEntities.clear();
            displayEntities.add("INVOICE_PART_NUMBER");
            displayEntities.add("STATUS");
            return displayEntities;
        }
        
        // 4. STATUS (for non-parts queries)
        if ((correctedQuery.matches(".*\\bstatus\\b.*") || correctedQuery.contains("status")) &&
            !correctedQuery.contains("part")) {
            displayEntities.add("STATUS");
            return displayEntities;
        }
        
        // 5. CREATE_DATE
        if (correctedQuery.matches(".*\\b(creation|create)\\s+date\\b.*") ||
            correctedQuery.matches(".*\\bwhen.*created\\b.*") || correctedQuery.contains("creation date") ||
            correctedQuery.contains("create date")) {
            displayEntities.add("CREATE_DATE");
            return displayEntities;
        }
        
        // CRITICAL FIX: Enhanced parts display entity detection
        if ("PARTS"
            .equals(queryType)) {
            // FIXED: Consistent generic info display for parts
            if (lowerPrompt.matches(".*\\bpart\\s+(details?|info|information|data|summary)\\b.*") ||
                lowerPrompt.matches(".*\\bget\\s+part\\s+(info|information|data).*") ||
                lowerPrompt.matches(".*\\bpart\\s+(info|information|data)\\s+for.*")) {
                displayEntities.clear();
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("PRICE");
                displayEntities.add("LEAD_TIME");
                displayEntities.add("MOQ");
                displayEntities.add("UOM");
                displayEntities.add("STATUS");
                return displayEntities;
            }
            
            // Handle active status queries
            if (lowerPrompt.matches(".*\\bis\\s+part\\s+.*\\bactive\\b.*")) {
                displayEntities.clear();
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("STATUS");
                return displayEntities;
            }
            
            // Handle lead time queries
            if (lowerPrompt.contains("leadtime") || lowerPrompt.contains("lead time")) {
                displayEntities.clear();
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("LEAD_TIME");
                return displayEntities;
            }
            
            // Handle price queries (but not price expiration which is contracts)
            if (lowerPrompt.contains("price") && !lowerPrompt.contains("expir")) {
                displayEntities.clear();
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("PRICE");
                return displayEntities;
            }
            
            // Handle MOQ queries
            if (lowerPrompt.contains("moq")) {
                displayEntities.clear();
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("MOQ");
                return displayEntities;
            }
            
            // Handle UOM queries
            if (lowerPrompt.contains("uom")) {
                displayEntities.clear();
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("UOM");
                return displayEntities;
            }
            
            // Default for parts queries
            if (displayEntities.isEmpty()) {
                displayEntities.add("INVOICE_PART_NUMBER");
            }
        }
        
        // FIXED: Enhanced invoice parts display for CONTRACTS queries
        if ("CONTRACTS".equals(queryType) && 
            (lowerPrompt.contains("invoice part") || 
             (lowerPrompt.contains("parts") && !lowerPrompt.contains("contract")))) {
            displayEntities.clear();
            displayEntities.add("CONTRACT_NAME");
            displayEntities.add("CUSTOMER_NAME");
            displayEntities.add("STATUS");
            displayEntities.add("INVOICE_PART_NUMBER");
            displayEntities.add("PRICE");
            displayEntities.add("LEAD_TIME");
            displayEntities.add("MOQ");
            displayEntities.add("UOM");
            return displayEntities;
        }
        
        // PRIORITY: Call helper methods for comprehensive field detection
        // These methods have early returns for specific fields
        extractBusinessTermDisplayEntities(lowerPrompt, displayEntities);
        extractNaturalLanguageFields(lowerPrompt, displayEntities);
        
        // Fallback: if no specific entity detected, use generic contract fields for CONTRACTS
        if (displayEntities.isEmpty() && "CONTRACTS".equals(queryType)) {
            displayEntities.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "CREATE_DATE",
                                                 "EXPIRATION_DATE", "STATUS"));
        }
        return displayEntities;
    }

    /**
     * FIXED: Enhanced business rule validation with standardized filter entity assignment
     */
    private void applyBusinessRuleValidation(List<EntityFilter> entities) {
        // CRITICAL FIX: Standardize filter entity assignment
        // For failed parts queries, always use LOADED_CP_NUMBER
        // For contract queries, always use AWARD_NUMBER

        boolean hasAward = false, hasLoadedCp = false;
        for (EntityFilter e : entities) {
            if (e.attribute.equals("AWARD_NUMBER"))
                hasAward = true;
            if (e.attribute.equals("LOADED_CP_NUMBER"))
                hasLoadedCp = true;
        }

        // CRITICAL FIX: If both present, determine which to keep based on context
        if (hasAward &&
            hasLoadedCp) {
            // Check if this is a failed parts context
            boolean isFailedPartsContext =
    entities.stream()
    .anyMatch(e -> e.attribute.equals("HAS_FAILED_PARTS") || e.attribute.equals("ERROR_COLUMN") ||
                             e.attribute.equals("REASON"));

            if (isFailedPartsContext) {
                // For failed parts, keep only LOADED_CP_NUMBER
                entities.removeIf(e -> e.attribute.equals("AWARD_NUMBER"));
            } else {
                // For regular contracts, keep only AWARD_NUMBER
                entities.removeIf(e -> e.attribute.equals("LOADED_CP_NUMBER"));
            }
        }

        // CRITICAL FIX: Remove duplicate filters for the same attribute
        Set<String> seen = new HashSet<>();
        Iterator<EntityFilter> it = entities.iterator();
        while (it.hasNext()) {
            EntityFilter e = it.next();
            String key = e.attribute + ":" + e.value;
            if (seen.contains(key)) {
                it.remove();
            } else {
                seen.add(key);
            }
        }
    }

    /**
     * FIXED: Enhanced query type classification with proper invoice parts handling
     */
    private String determineQueryType(String originalPrompt, String normalizedPrompt) {
        String lowerPrompt = originalPrompt.toLowerCase();
        
        // CRITICAL BUSINESS RULE: Detect count/statistics queries
        boolean isCountQuery =
            lowerPrompt.contains("how many") || lowerPrompt.contains("count") || lowerPrompt.contains("number of") ||
            lowerPrompt.contains("total") || lowerPrompt.contains("statistics") || lowerPrompt.contains("summary");
        
        // CRITICAL BUSINESS RULE: Detect parts-related count queries
        boolean isPartsCountQuery =
            isCountQuery &&
            (lowerPrompt.contains("parts failed") || lowerPrompt.contains("failed parts") ||
            lowerPrompt.contains("parts loaded") || lowerPrompt.contains("loaded parts") ||
            lowerPrompt.contains("parts success") || lowerPrompt.contains("successful parts") ||
            lowerPrompt.contains("parts count") || lowerPrompt.contains("count parts") ||
             lowerPrompt.contains("how many parts") || lowerPrompt.contains("total parts"));
        
        // CRITICAL BUSINESS RULE: Prioritize FAILED_PARTS detection over CONTRACTS
        // Check for failed parts indicators first
        String[] failedPartsKeywords = {
            "failed parts", "failed part", "faild parts", "faild part", "error reasons", "error reason",
            "error columns", "error column", "error details", "error detail", "error info", "error information",
            "parts with issues", "parts with issue", "parts with errors", "parts with error", "parts with problems",
            "parts with problem", "parts with failures", "parts with failure", "why parts failed", "why part failed",
            "what caused failure", "what caused the failure", "failure reasons", "failure reason", "error caused",
            "errors caused", "caused failure", "parts loaded", "parts with missing data", "parts missing info",
            "parts with no data", "incomplete parts", "parts missing data", "parts with missing info",
            "parts missing data", "incomplete parts", "missing data parts", "parts no data", "missing data parts",
            "errors occurred during loading", "loading errors", "load errors", "loading issues",
            "what happened during load", "loading error details", "load problems", "load failures", "loading issues",
            "errors during loading", "validation issues", "validation errors", "validation problems",
            "validation failures", "list validation issues", "show validation errors", "what validation problems",
            "show validation issues", "what validation errors", "get validation problems", "show validation failures",
            "list validation errors", "what validation issues", "show validation problems"
        };
        
        boolean hasFailedPartsKeywords = false;
        for (String keyword : failedPartsKeywords) {
            if (lowerPrompt.contains(keyword)) {
                hasFailedPartsKeywords = true;
                break;
            }
        }
        
        // CRITICAL BUSINESS RULE: All parts queries MUST have contract number
        // Extract contract number from query
        Pattern contractPattern = Pattern.compile("\\b(\\d{6,})\\b");
        Matcher contractMatcher = contractPattern.matcher(originalPrompt);
        boolean hasContractNumber = contractMatcher.find();
        
        // CRITICAL BUSINESS RULE: If parts query without contract number, ask user
        if ((isPartsCountQuery || hasFailedPartsKeywords || lowerPrompt.contains("parts") ||
             lowerPrompt.contains("part")) && !hasContractNumber) {
            return "HELP"; // Return HELP to ask user for contract number
        }
        
        // CRITICAL FIX: If failed parts keywords detected, classify as PARTS (not FAILED_PARTS)
        if (hasFailedPartsKeywords) {
            return "PARTS";
        }
        
        // CRITICAL FIX: Prioritize contract information queries over HELP
        // Check for contract numbers first (6+ digits)
        boolean hasContractNumber1 = lowerPrompt.matches(".*\\b\\d{6,}\\b.*");
        
        // CRITICAL FIX: Price expiration queries should be CONTRACTS, not PARTS
        if (hasContractNumber1 && lowerPrompt.contains("price") && 
            (lowerPrompt.contains("expir") || lowerPrompt.contains("expiry") || lowerPrompt.contains("experation"))) {
            return "CONTRACTS";
        }
        
        // CRITICAL FIX: More restrictive HELP detection - only explicit creation/help intent
        String[] explicitCreateWords = {
            "create", "make", "generate", "initiate", "build", "draft", "establish", "form", "set up"
        };
        String[] explicitHelpWords = {
            "help", "steps", "guide", "instruction", "how to", "walk me", "explain", "process", "show me how",
            "need guidance", "teach", "assist", "support"
        };
        
        boolean hasExplicitCreateWord = false;
        for (String w : explicitCreateWords) {
            if (lowerPrompt.contains(w)) {
                hasExplicitCreateWord = true;
                break;
            }
        }
        
        boolean hasExplicitHelpWord = false;
        for (String w : explicitHelpWords) {
            if (lowerPrompt.contains(w)) {
                hasExplicitHelpWord = true;
                break;
            }
        }
        
        // CRITICAL FIX: Only classify as HELP if explicit creation/help intent
        if (hasExplicitCreateWord || hasExplicitHelpWord) {
            return "HELP";
        }
        
        // CRITICAL FIX: Enhanced parts detection with better context
        String[] partsKeywords = { "part", "parts", "component", "components", "item", "items" };
        String[] contractKeywords = { "contract", "agreement", "deal", "terms", "conditions", "customer", "account" };
        
        boolean hasPartsKeywords = false;
        for (String keyword : partsKeywords) {
            if (lowerPrompt.contains(keyword)) {
                hasPartsKeywords = true;
                break;
            }
        }
        
        boolean hasContractKeywords = false;
        for (String keyword : contractKeywords) {
            if (lowerPrompt.contains(keyword)) {
                hasContractKeywords = true;
                break;
            }
        }
        
        // CRITICAL FIX: Better logic for parts vs contracts classification
        if (hasPartsKeywords && !hasContractKeywords) {
            return "PARTS";
        } else if (hasContractKeywords || hasContractNumber) {
            return "CONTRACTS";
        } else if (hasPartsKeywords) {
            // If both parts and contract keywords, default to parts for now
            return "PARTS";
        }
        
        // Default fallback
        return "CONTRACTS";
    }

    /**
     * FIXED: Enhanced filter entity assignment with consistent patterns
     */
    private List<EntityFilter> determineFilterEntities(String originalPrompt, String normalizedPrompt,
                                                       String queryType) {
        List<EntityFilter> filters = new ArrayList<>();
        String lowerPrompt = originalPrompt.toLowerCase();
        
        // CRITICAL FIX: Extract contract numbers and determine correct filter type
        Pattern contractPattern = Pattern.compile("\\b(\\d{6,})\\b");
        Matcher contractMatcher = contractPattern.matcher(originalPrompt);
        if (contractMatcher.find()) {
            String contractNumber = contractMatcher.group(1);
            
            // CRITICAL FIX: Only exclude filters for explicit HELP queries
            boolean isExplicitHelpQuery =
                lowerPrompt.contains("how to create") || lowerPrompt.contains("steps to create") ||
                lowerPrompt.contains("help me create") || lowerPrompt.contains("guide for creating") ||
                                         lowerPrompt.contains("walk me through creating");
            
            if (!isExplicitHelpQuery) {
                // CRITICAL FIX: Determine correct filter type based on query content
                boolean isFailedPartsQuery =
                    lowerPrompt.contains("failed") || lowerPrompt.contains("error") ||
                    lowerPrompt.contains("failure") || lowerPrompt.contains("reason") || lowerPrompt.contains("why") ||
                    lowerPrompt.contains("caused") || lowerPrompt.contains("problem") ||
                    lowerPrompt.contains("issue") || lowerPrompt.contains("failing parts") ||
                    lowerPrompt.contains("parts with issues") || lowerPrompt.contains("parts with errors") ||
                    lowerPrompt.contains("faild") || // Handle typo
                    lowerPrompt.contains("column errors") || lowerPrompt.contains("column failures") ||
                    lowerPrompt.contains("validation errors") || lowerPrompt.contains("validation issues") ||
                    lowerPrompt.contains("validation problems") || lowerPrompt.contains("validation failures") ||
                    lowerPrompt.contains("error reasons") || lowerPrompt.contains("failure reasons") ||
                    lowerPrompt.contains("error info") || lowerPrompt.contains("errors caused failure") ||
                    lowerPrompt.contains("parts loaded") || lowerPrompt.contains("parts with missing data") ||
                    lowerPrompt.contains("parts missing info") || lowerPrompt.contains("parts with no data") ||
                    lowerPrompt.contains("incomplete parts") || lowerPrompt.contains("parts missing data") ||
                    lowerPrompt.contains("parts with missing info") || lowerPrompt.contains("parts missing data") ||
                    lowerPrompt.contains("incomplete parts") || lowerPrompt.contains("missing data parts") ||
                    lowerPrompt.contains("parts no data") || lowerPrompt.contains("missing data parts") ||
                    lowerPrompt.contains("errors occurred during loading") || lowerPrompt.contains("loading errors") ||
                    lowerPrompt.contains("load errors") || lowerPrompt.contains("loading issues") ||
                    lowerPrompt.contains("what happened during load") ||
                    lowerPrompt.contains("loading error details") || lowerPrompt.contains("load problems") ||
                    lowerPrompt.contains("load failures") || lowerPrompt.contains("loading issues") ||
                    lowerPrompt.contains("errors during loading") || lowerPrompt.contains("validation issues") ||
                    lowerPrompt.contains("validation errors") || lowerPrompt.contains("validation problems") ||
                    lowerPrompt.contains("validation failures") || lowerPrompt.contains("list validation issues") ||
                    lowerPrompt.contains("show validation errors") ||
                    lowerPrompt.contains("what validation problems") ||
                    lowerPrompt.contains("show validation issues") || lowerPrompt.contains("what validation errors") ||
                    lowerPrompt.contains("get validation problems") ||
                    lowerPrompt.contains("show validation failures") ||
                    lowerPrompt.contains("list validation errors") || lowerPrompt.contains("what validation issues") ||
                    lowerPrompt.contains("show validation problems");

                // CRITICAL FIX: Enhanced logic to ensure consistent filter assignment
                if (isFailedPartsQuery || "PARTS".equals(queryType)) {
                    // CRITICAL FIX: All parts queries (including failed parts) use CONTRACT_NO
                    filters.add(new EntityFilter("CONTRACT_NO", "=", contractNumber, "extracted"));
                } else if (lowerPrompt.contains("invoice parts") || lowerPrompt.contains("invoce parts") ||
                           lowerPrompt.contains("parts for") || lowerPrompt.contains("parts in") ||
                           lowerPrompt.contains("all parts")) {
                    // FIXED: Invoice parts should use AWARD_NUMBER
                    filters.add(new EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
                } else {
                    // FIXED: Default contract queries use AWARD_NUMBER
                    filters.add(new EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
                }
            }
        }
        
        // FIXED: HELP queries should have no filter entities (only for explicit help)
        if ("HELP".equals(queryType) && 
            (lowerPrompt.contains("how to create") || lowerPrompt.contains("steps to create") ||
             lowerPrompt.contains("help me create") || lowerPrompt.contains("guide for creating"))) {
            return filters; // Return empty list for explicit HELP queries only
        }

        // CRITICAL FIX: Enhanced filter assignment based on query type and content
        // Check if we already have a contract number from the earlier extraction
        boolean hasContractNumber =
            !filters.isEmpty() && filters.stream().anyMatch(f -> "AWARD_NUMBER".equals(f.attribute));

        if (!hasContractNumber) {
            // Re-extract contract number if not already found
            Pattern contractPattern2 = Pattern.compile("\\b(\\d{6,})\\b");
            Matcher contractMatcher2 = contractPattern2.matcher(originalPrompt);
            if (contractMatcher2.find()) {
                String contractNumber = contractMatcher2.group(1);

                boolean isFailedPartsQuery =
                    lowerPrompt.contains("failed") || lowerPrompt.contains("error") ||
                    lowerPrompt.contains("failure") || lowerPrompt.contains("reason") || lowerPrompt.contains("why") ||
                    lowerPrompt.contains("caused") || lowerPrompt.contains("problem") ||
                    lowerPrompt.contains("issue") || lowerPrompt.contains("failing parts") ||
                    lowerPrompt.contains("parts with issues") || lowerPrompt.contains("parts with errors") ||
                    lowerPrompt.contains("faild") || // Handle typo
                    lowerPrompt.contains("column errors") || lowerPrompt.contains("column failures") ||
                    lowerPrompt.contains("validation errors") || lowerPrompt.contains("validation issues") ||
                    lowerPrompt.contains("validation problems") || lowerPrompt.contains("validation failures") ||
                    lowerPrompt.contains("error reasons") || lowerPrompt.contains("failure reasons") ||
                    lowerPrompt.contains("error info") || lowerPrompt.contains("errors caused failure") ||
                    lowerPrompt.contains("parts loaded") || lowerPrompt.contains("parts with missing data") ||
                    lowerPrompt.contains("parts missing info") || lowerPrompt.contains("parts with no data") ||
                    lowerPrompt.contains("incomplete parts") || lowerPrompt.contains("parts missing data") ||
                    lowerPrompt.contains("parts with missing info") || lowerPrompt.contains("parts missing data") ||
                    lowerPrompt.contains("incomplete parts") || lowerPrompt.contains("missing data parts") ||
                    lowerPrompt.contains("parts no data") || lowerPrompt.contains("missing data parts") ||
                    lowerPrompt.contains("errors occurred during loading") || lowerPrompt.contains("loading errors") ||
                    lowerPrompt.contains("load errors") || lowerPrompt.contains("loading issues") ||
                    lowerPrompt.contains("what happened during load") ||
                    lowerPrompt.contains("loading error details") || lowerPrompt.contains("load problems") ||
                    lowerPrompt.contains("load failures") || lowerPrompt.contains("loading issues") ||
                    lowerPrompt.contains("errors during loading") || lowerPrompt.contains("validation issues") ||
                    lowerPrompt.contains("validation errors") || lowerPrompt.contains("validation problems") ||
                    lowerPrompt.contains("validation failures") || lowerPrompt.contains("list validation issues") ||
                    lowerPrompt.contains("show validation errors") ||
                    lowerPrompt.contains("what validation problems") ||
                    lowerPrompt.contains("show validation issues") || lowerPrompt.contains("what validation errors") ||
                    lowerPrompt.contains("get validation problems") ||
                    lowerPrompt.contains("show validation failures") ||
                    lowerPrompt.contains("list validation errors") || lowerPrompt.contains("what validation issues") ||
                    lowerPrompt.contains("show validation problems");

                // CRITICAL FIX: Enhanced logic to ensure consistent filter assignment
                if (isFailedPartsQuery || "PARTS".equals(queryType)) {
                    // CRITICAL FIX: All parts queries (including failed parts) use CONTRACT_NO
                    filters.add(new EntityFilter("CONTRACT_NO", "=", contractNumber, "extracted"));
                } else if (lowerPrompt.contains("invoice parts") || lowerPrompt.contains("invoce parts") ||
                           lowerPrompt.contains("parts for") || lowerPrompt.contains("parts in") ||
                           lowerPrompt.contains("all parts")) {
                    // FIXED: Invoice parts should use AWARD_NUMBER
                    filters.add(new EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
                } else {
                    // FIXED: Default contract queries use AWARD_NUMBER
                    filters.add(new EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
                }
            }
        }

        // FIXED: Enhanced part number extraction with broader patterns and case-insensitive matching
        Pattern partPattern1 = Pattern.compile("\\b([A-Za-z]{2}\\d{5})\\b"); // AE12345, ae12345
        Pattern partPattern2 = Pattern.compile("\\b([A-Za-z]{2}\\d{4,6})\\b"); // BC67890, bc67890, DE23456, de23456
        Pattern partPattern3 =
            Pattern.compile("\\bfor\\s+([A-Za-z]{2}\\d{4,6})\\b", Pattern.CASE_INSENSITIVE); // "for BC67890"
        
        Matcher partMatcher1 = partPattern1.matcher(originalPrompt);
        Matcher partMatcher2 = partPattern2.matcher(originalPrompt);
        Matcher partMatcher3 = partPattern3.matcher(originalPrompt);
        
        if (partMatcher1.find()) {
            String partNumber = partMatcher1.group(1).toUpperCase(); // Convert to uppercase
            filters.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNumber, "extracted"));
        } else if (partMatcher2.find()) {
            String partNumber = partMatcher2.group(1).toUpperCase(); // Convert to uppercase
            filters.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNumber, "extracted"));
        } else if (partMatcher3.find()) {
            String partNumber = partMatcher3.group(1).toUpperCase(); // Convert to uppercase
            filters.add(new EntityFilter("INVOICE_PART_NUMBER", "=", partNumber, "extracted"));
        }

        // FIXED: Extract customer numbers (if present)
        Pattern customerPattern = Pattern.compile("\\bcustomer\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher customerMatcher = customerPattern.matcher(originalPrompt);
        if (customerMatcher.find()) {
            String customerNumber = customerMatcher.group(1);
            filters.add(new EntityFilter("CUSTOMER_NUMBER", "=", customerNumber, "extracted"));
        }

        // FIXED: Extract status values
        if (lowerPrompt.contains("active") || lowerPrompt.contains("actv")) {
            filters.add(new EntityFilter("STATUS", "=", "ACTIVE", "extracted"));
        } else if (lowerPrompt.contains("inactive") || lowerPrompt.contains("inactv")) {
            filters.add(new EntityFilter("STATUS", "=", "INACTIVE", "extracted"));
        } else if (lowerPrompt.contains("expired") || lowerPrompt.contains("expire")) {
            filters.add(new EntityFilter("STATUS", "=", "EXPIRED", "extracted"));
        }

        // FIXED: Extract date ranges if present
        if (lowerPrompt.contains("this year") || lowerPrompt.contains("current year")) {
            filters.add(new EntityFilter("CREATE_DATE", "=", "CURRENT_YEAR", "extracted"));
        } else if (lowerPrompt.contains("last year") || lowerPrompt.contains("previous year")) {
            filters.add(new EntityFilter("CREATE_DATE", "=", "LAST_YEAR", "extracted"));
        }

        return filters;
    }
    
    // Helper method to detect multi-intent queries
    private boolean containsMultiIntent(String lowerPrompt) {
        return lowerPrompt.contains(" and ") || lowerPrompt.contains(" plus ") || lowerPrompt.contains(" also ") ||
               lowerPrompt.contains(" with ") || lowerPrompt.contains(" details about ") ||
               lowerPrompt.contains(" info about ");
    }
    
    // Helper method to extract text after a keyword
    private String extractAfterKeyword(String lowerPrompt, String keyword) {
        int index = lowerPrompt.indexOf(keyword);
        if (index != -1 && index + keyword.length() < lowerPrompt.length()) {
            return lowerPrompt.substring(index + keyword.length()).trim();
        }
        return "";
    }
    
    // Helper method to extract entities from a phrase
    private List<String> extractEntitiesFromPhrase(String phrase) {
        List<String> entities = new ArrayList<>();
        String lowerPhrase = phrase.toLowerCase();
        
        if (lowerPhrase.contains("part error") || lowerPhrase.contains("part errors")) {
            entities.addAll(Arrays.asList("PART_NUMBER", "ERROR_COLUMN", "REASON"));
        } else if (lowerPhrase.contains("error") || lowerPhrase.contains("errors")) {
            entities.addAll(Arrays.asList("ERROR_COLUMN", "REASON"));
        } else if (lowerPhrase.contains("part") || lowerPhrase.contains("parts")) {
            entities.addAll(Arrays.asList("PART_NUMBER", "PRICE", "MOQ", "UOM"));
        } else if (lowerPhrase.contains("customer")) {
            entities.add("CUSTOMER_NAME");
        } else if (lowerPhrase.contains("contract")) {
            entities.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE",
                                          "STATUS"));
        }
        
        return entities;
    }
    
    // FIXED: Specialized error display entities for error analysis
    private List<String> addSpecializedErrorEntities(String lowerPrompt,
                                                     List<String> displayEntities) {
        // Add specialized entities based on error context
        if (lowerPrompt.contains("missing data") || lowerPrompt.contains("no data") ||
            lowerPrompt.contains("incomplete")) {
            displayEntities.add("DATA_QUALITY_ISSUE");
        }
        
        if (lowerPrompt.contains("loading") || lowerPrompt.contains("load") || lowerPrompt.contains("during load")) {
            displayEntities.add("LOADING_ERROR");
        }
        
        if (lowerPrompt.contains("validation") || lowerPrompt.contains("invalid") || lowerPrompt.contains("validate")) {
            displayEntities.add("VALIDATION_ERROR");
        }
        
        if (lowerPrompt.contains("import") || lowerPrompt.contains("upload") || lowerPrompt.contains("processing")) {
            displayEntities.add("PROCESSING_ERROR");
        }
        
        // Ensure core error entities are always present
        if (!displayEntities.contains("PART_NUMBER")) {
            displayEntities.add("PART_NUMBER");
        }
        if (!displayEntities.contains("ERROR_COLUMN")) {
            displayEntities.add("ERROR_COLUMN");
        }
        if (!displayEntities.contains("REASON")) {
            displayEntities.add("REASON");
        }
        
        return displayEntities;
    }
}


