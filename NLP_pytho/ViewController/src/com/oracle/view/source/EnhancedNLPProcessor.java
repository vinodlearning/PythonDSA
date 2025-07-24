package com.oracle.view.source;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Enhanced NLP Processor using comprehensive word databases
 * Provides advanced text analysis and classification without internet connectivity
 */
public class EnhancedNLPProcessor {
    
    // Patterns for text analysis
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[*@#$%^&+=]");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[-_;]");
    private static final Pattern BRACKET_PATTERN = Pattern.compile("[()\\[\\]{}]");
    private static final Pattern TRAILING_PUNCTUATION_PATTERN = Pattern.compile("[?.,!]+$");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    
    /**
     * Comprehensive text normalization using WordDatabase
     */
    public static String normalizeText(String input) {
        if (input == null) return null;
        
        // Basic normalization
        String normalized = input.trim()
                                 .toLowerCase()
                                 // Remove special characters that break word boundaries
                                 .replaceAll(SPECIAL_CHARS_PATTERN.pattern(), " ")
                                 // Replace common separators with spaces
                                 .replaceAll(SEPARATOR_PATTERN.pattern(), " ")
                                 // Handle parentheses and brackets
                                 .replaceAll(BRACKET_PATTERN.pattern(), " ")
                                 // Remove trailing punctuation but keep internal ones
                                 .replaceAll(TRAILING_PUNCTUATION_PATTERN.pattern(), "")
                                 // Normalize multiple spaces
                                 .replaceAll(MULTIPLE_SPACES_PATTERN.pattern(), " ")
                                 .trim();
        
        // Apply word-by-word normalization using WordDatabase
        String[] words = normalized.split("\\s+");
        List<String> normalizedWords = new ArrayList<>();
        
        for (String word : words) {
            String normalizedWord = WordDatabase.normalizeWord(word);
            if (normalizedWord != null) {
                normalizedWords.add(normalizedWord);
            }
        }
        
        return String.join(" ", normalizedWords);
    }
    
    /**
     * Enhanced query type detection using WordDatabase
     */
    public static String determineQueryType(String originalInput, String normalizedInput) {
        if (originalInput == null || normalizedInput == null) {
            return "CONTRACTS"; // Default fallback
        }
        
        String lowerOriginal = originalInput.toLowerCase();
        String lowerNormalized = normalizedInput.toLowerCase();
        
        // PRIORITY 0: HELP detection for contract creation/instruction queries (ENGLISH & SPANISH)
        // This MUST be checked FIRST before any other classification
        String[] helpKeywordsEN = {"how to", "steps", "process", "guide", "tell me", "show me", "instructions", "walk me through", "explain", "need guidance", "what's the process", "help", "guidance", "create", "make", "build", "generate", "set up", "setup", "draft", "initiate", "start", "produce", "prepare", "compose", "write", "construct", "form", "develop", "assemble", "manufacture", "fabricate", "establish"};
        String[] helpKeywordsES = {"cómo crear", "pasos", "proceso", "guía", "instrucciones", "explícame", "puedes mostrarme", "camíname por", "necesito", "cuál es el proceso", "ayuda", "guía", "crear", "hacer", "generar", "construir", "iniciar", "empezar", "producir", "preparar", "componer", "escribir", "formar", "desarrollar", "ensamblar", "fabricar", "establecer"};
        boolean hasHelpEN = false;
        for (String kw : helpKeywordsEN) {
            if (lowerOriginal.contains(kw)) { hasHelpEN = true; break; }
        }
        boolean hasHelpES = false;
        for (String kw : helpKeywordsES) {
            if (lowerOriginal.contains(kw)) { hasHelpES = true; break; }
        }
        if ((hasHelpEN && lowerOriginal.contains("contract")) || (hasHelpES && lowerOriginal.contains("contrato"))) {
            return "HELP";
        }
        
        // PRIORITY 1: Check for specific data retrieval patterns that should NOT be HELP
        // These patterns should override HELP classification when specific data identifiers are present
        boolean hasContractNumber = lowerOriginal.matches(".*\\b\\d{6,}\\b.*") || lowerNormalized.matches(".*\\b\\d{6,}\\b.*") ||
                                   lowerOriginal.matches(".*\\d{6,}.*") || lowerNormalized.matches(".*\\d{6,}.*");
        // Enhanced part number detection to handle various formats including AIR-A320-001
        boolean hasPartNumber = lowerOriginal.matches(".*\\b[A-Za-z]{2}\\d{4,6}\\b.*") || lowerNormalized.matches(".*\\b[A-Za-z]{2}\\d{4,6}\\b.*") ||
                               lowerOriginal.matches(".*\\b[A-Za-z]{2,4}-[A-Za-z0-9]+\\b.*") || lowerNormalized.matches(".*\\b[A-Za-z]{2,4}-[A-Za-z0-9]+\\b.*") ||
                               lowerOriginal.matches(".*\\b[A-Za-z0-9-]+\\b.*") || lowerNormalized.matches(".*\\b[A-Za-z0-9-]+\\b.*");
        boolean hasDataRetrievalWords = lowerOriginal.contains("show me") || lowerOriginal.contains("get") || 
                                       lowerOriginal.contains("what") || lowerOriginal.contains("tell") ||
                                       lowerOriginal.contains("list") || lowerOriginal.contains("display") ||
                                       lowerOriginal.contains("find") || lowerOriginal.contains("search") ||
                                       lowerOriginal.contains("show") || lowerOriginal.contains("get me") ||
                                       lowerOriginal.contains("find me") || lowerOriginal.contains("search for") ||
                                       lowerOriginal.contains("look for") || lowerOriginal.contains("tell me") ||
                                       lowerNormalized.contains("show me") || lowerNormalized.contains("get") || 
                                       lowerNormalized.contains("what") || lowerNormalized.contains("tell") ||
                                       lowerNormalized.contains("list") || lowerNormalized.contains("display") ||
                                       lowerNormalized.contains("find") || lowerNormalized.contains("search") ||
                                       lowerNormalized.contains("show") || lowerNormalized.contains("get me") ||
                                       lowerNormalized.contains("find me") || lowerNormalized.contains("search for") ||
                                       lowerNormalized.contains("look for") || lowerNormalized.contains("tell me");
        
        // CRITICAL FIX: Check for specific creation date/information queries that should be CONTRACTS, not HELP
        boolean isCreationDateQuery = (lowerOriginal.contains("when") || lowerOriginal.contains("what date")) && 
                                     (lowerOriginal.contains("created") || lowerOriginal.contains("made") || 
                                      lowerOriginal.contains("started") || lowerOriginal.contains("initiated")) &&
                                     hasContractNumber;
        
        boolean isInformationQuery = lowerOriginal.contains("information on") || lowerOriginal.contains("info on") ||
                                    lowerOriginal.contains("details on") || lowerOriginal.contains("tell me about") ||
                                    lowerOriginal.contains("what about") || lowerOriginal.contains("status on") ||
                                    lowerOriginal.contains("update on") || lowerOriginal.contains("how is") &&
                                    hasContractNumber;
        
        // PRIORITY 1: If it has failed/error keywords, it's likely a FAILED_PARTS query
        if (lowerOriginal.contains("failed") || lowerOriginal.contains("error") || 
            lowerOriginal.contains("failure") || lowerOriginal.contains("reason") ||
            lowerOriginal.contains("why") || lowerOriginal.contains("caused")) {
            return "FAILED_PARTS";
        }
        
        // CRITICAL FIX: Creation date and information queries should be CONTRACTS, not HELP
        if (isCreationDateQuery || isInformationQuery) {
            return "CONTRACTS";
        }
        
        // PRIORITY 2: Lead time queries should always be PARTS queries
        if (lowerOriginal.contains("lead time") || lowerOriginal.contains("leadtime")) {
            return "PARTS";
        }
        
        // PRIORITY 3: If it has part number and data retrieval words, it's likely a PARTS query
        if (hasPartNumber && hasDataRetrievalWords) {
            return "PARTS";
        }
        
        // CRITICAL FIX: Specific part-related queries should be PARTS, not CONTRACTS
        boolean isPartSpecificQuery = (lowerOriginal.contains("cost of part") || lowerOriginal.contains("price of part") ||
                                      lowerOriginal.contains("minimum order quantity for") || lowerOriginal.contains("moq for") ||
                                      lowerOriginal.contains("lead time for part") || lowerOriginal.contains("uom for part")) &&
                                     hasPartNumber;
        
        if (isPartSpecificQuery) {
            return "PARTS";
        }
        
        // PRIORITY 4: If it has contract number and data retrieval words, it's likely a CONTRACTS query
        if (hasContractNumber && hasDataRetrievalWords) {
            return "CONTRACTS";
        }
        
        // PRIORITY 5: If it has data retrieval words and contract/part context, classify appropriately
        if (hasDataRetrievalWords) {
            // Enhanced part context detection with typo handling and lead time context
            boolean hasPartContext = lowerOriginal.contains("part") || lowerOriginal.contains("parts") ||
                                    lowerOriginal.contains("pasts") || lowerOriginal.contains("past") || // Handle typos
                                    lowerOriginal.contains("lead time") || lowerOriginal.contains("leadtime") || // Lead time context
                                    lowerOriginal.contains("price") || lowerOriginal.contains("cost") || // Price/cost context
                                    lowerOriginal.contains("moq") || lowerOriginal.contains("uom"); // Part attributes
            
            if (hasPartContext || hasPartNumber) {
                return "PARTS";
            } else if (lowerOriginal.contains("contract") || hasContractNumber) {
                return "CONTRACTS";
            }
        }
        
        // Define creation context check
        boolean hasCreationContext = lowerOriginal.contains("contract") || lowerOriginal.contains("contarct") ||
                                    lowerNormalized.contains("contract") || lowerNormalized.contains("contarct");
        
        // Define explicit creation context check
        boolean hasExplicitCreationContext = lowerOriginal.contains("create contract") || 
                                            lowerOriginal.contains("make contract") ||
                                            lowerOriginal.contains("generate contract") ||
                                            lowerOriginal.contains("build contract") ||
                                            lowerOriginal.contains("draft contract") ||
                                            lowerOriginal.contains("initiate contract") ||
                                            lowerOriginal.contains("start contract") ||
                                            lowerOriginal.contains("produce contract") ||
                                            lowerOriginal.contains("prepare contract") ||
                                            lowerOriginal.contains("compose contract") ||
                                            lowerOriginal.contains("write contract") ||
                                            lowerOriginal.contains("construct contract") ||
                                            lowerOriginal.contains("form contract") ||
                                            lowerOriginal.contains("develop contract") ||
                                            lowerOriginal.contains("assemble contract") ||
                                            lowerOriginal.contains("manufacture contract") ||
                                            lowerOriginal.contains("fabricate contract") ||
                                            lowerOriginal.contains("establish contract") ||
                                            lowerOriginal.contains("setup contract") ||
                                            lowerNormalized.contains("create contract") || 
                                            lowerNormalized.contains("make contract") ||
                                            lowerNormalized.contains("generate contract") ||
                                            lowerNormalized.contains("build contract") ||
                                            lowerNormalized.contains("draft contract") ||
                                            lowerNormalized.contains("initiate contract") ||
                                            lowerNormalized.contains("start contract") ||
                                            lowerNormalized.contains("produce contract") ||
                                            lowerNormalized.contains("prepare contract") ||
                                            lowerNormalized.contains("compose contract") ||
                                            lowerNormalized.contains("write contract") ||
                                            lowerNormalized.contains("construct contract") ||
                                            lowerNormalized.contains("form contract") ||
                                            lowerNormalized.contains("develop contract") ||
                                            lowerNormalized.contains("assemble contract") ||
                                            lowerNormalized.contains("manufacture contract") ||
                                            lowerNormalized.contains("fabricate contract") ||
                                            lowerNormalized.contains("establish contract") ||
                                            lowerNormalized.contains("setup contract");
        
        // Check for explicit HELP indicators
        if (lowerOriginal.contains("help") || lowerOriginal.contains("how") || 
            lowerOriginal.contains("guide") || lowerOriginal.contains("steps") ||
            lowerOriginal.contains("process") || lowerOriginal.contains("method") ||
            lowerOriginal.contains("procedure") || lowerOriginal.contains("approach") ||
            lowerOriginal.contains("technique") || lowerOriginal.contains("strategy") ||
            lowerOriginal.contains("plan") || lowerOriginal.contains("scheme") ||
            lowerOriginal.contains("design") || lowerOriginal.contains("layout") ||
            lowerOriginal.contains("structure") || lowerOriginal.contains("framework") ||
            lowerOriginal.contains("system") || lowerOriginal.contains("mechanism") ||
            lowerOriginal.contains("workflow") || lowerOriginal.contains("pipeline") ||
            lowerOriginal.contains("sequence") || lowerOriginal.contains("series") ||
            lowerOriginal.contains("chain") || lowerOriginal.contains("line") ||
            lowerOriginal.contains("path") || lowerOriginal.contains("route") ||
            lowerOriginal.contains("way") || lowerOriginal.contains("means") ||
            lowerOriginal.contains("manner") || lowerOriginal.contains("mode") ||
            lowerOriginal.contains("style") || lowerOriginal.contains("format") ||
            lowerOriginal.contains("pattern") || lowerOriginal.contains("template") ||
            lowerOriginal.contains("model") || lowerOriginal.contains("example") ||
            lowerOriginal.contains("sample") || lowerOriginal.contains("instance") ||
            lowerOriginal.contains("case") || lowerOriginal.contains("scenario") ||
            lowerOriginal.contains("situation") || lowerOriginal.contains("circumstance") ||
            lowerOriginal.contains("context") || lowerOriginal.contains("environment") ||
            lowerOriginal.contains("setting") || lowerOriginal.contains("background")) {
            return "HELP";
        }
        
        // Check for creation words using WordDatabase only in explicit creation context
        boolean hasCreationWords = WordDatabase.containsCreationWords(lowerOriginal) || 
                                  WordDatabase.containsCreationWords(lowerNormalized);
        
        if (hasCreationWords && hasExplicitCreationContext) {
            return "HELP";
        }
        
        // Check for question words only in explicit creation context
        boolean hasQuestionWords = WordDatabase.containsQuestionWords(lowerOriginal) || 
                                  WordDatabase.containsQuestionWords(lowerNormalized);
        
        if (hasQuestionWords && hasExplicitCreationContext) {
            return "HELP";
        }
        
        // Check for imperative indicators only in explicit creation context
        // But exclude common data retrieval phrases
        boolean hasImperativeIndicators = WordDatabase.containsImperativeIndicators(lowerOriginal) || 
                                         WordDatabase.containsImperativeIndicators(lowerNormalized);
        
        // Exclude common data retrieval phrases that shouldn't be classified as HELP
        boolean isDataRetrievalPhrase = lowerOriginal.contains("show me") || 
                                       lowerOriginal.contains("tell me") ||
                                       lowerOriginal.contains("get me") ||
                                       lowerOriginal.contains("find me") ||
                                       lowerOriginal.contains("search for") ||
                                       lowerOriginal.contains("look for") ||
                                       lowerOriginal.contains("display") ||
                                       lowerOriginal.contains("list") ||
                                       lowerNormalized.contains("show me") || 
                                       lowerNormalized.contains("tell me") ||
                                       lowerNormalized.contains("get me") ||
                                       lowerNormalized.contains("find me") ||
                                       lowerNormalized.contains("search for") ||
                                       lowerNormalized.contains("look for") ||
                                       lowerNormalized.contains("display") ||
                                       lowerNormalized.contains("list");
        
        if (hasImperativeIndicators && hasExplicitCreationContext && !isDataRetrievalPhrase) {
            return "HELP";
        }
        
        // Check for specific patterns that indicate HELP (more restrictive)
        // Only classify as HELP if these words are used in creation context
        if (hasCreationContext && 
            (lowerOriginal.matches(".*\\b(create|make|generate|build|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish|setup)\\b.*") ||
             lowerNormalized.matches(".*\\b(create|make|generate|build|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish|setup)\\b.*"))) {
            return "HELP";
        }
        
        // Check for explicit help requests
        if (lowerOriginal.contains("help") || lowerOriginal.contains("how to") || 
            lowerOriginal.contains("guide") || lowerOriginal.contains("steps") ||
            lowerOriginal.contains("instructions") || lowerOriginal.contains("process") ||
            lowerOriginal.contains("method") || lowerOriginal.contains("procedure")) {
            return "HELP";
        }
        
        // Check for word boundary issues
        if (lowerOriginal.contains("contractcreation") || lowerOriginal.contains("contractcreate") ||
            lowerOriginal.contains("createcontract") || lowerOriginal.contains("makecontract") ||
            lowerOriginal.contains("generatecontract") || lowerOriginal.contains("draftcontract") ||
            lowerOriginal.contains("initiatecontract") || lowerOriginal.contains("startcontract") ||
            lowerOriginal.contains("producecontract") || lowerOriginal.contains("buildcontract") ||
            lowerOriginal.contains("preparecontract") || lowerOriginal.contains("composecontract") ||
            lowerOriginal.contains("writecontract") || lowerOriginal.contains("constructcontract") ||
            lowerOriginal.contains("formcontract") || lowerOriginal.contains("developcontract") ||
            lowerOriginal.contains("assemblecontract") || lowerOriginal.contains("manufacturecontract") ||
            lowerOriginal.contains("fabricatecontract") || lowerOriginal.contains("establishcontract") ||
            lowerOriginal.contains("setupcontract") || lowerOriginal.contains("docontract") ||
            lowerOriginal.contains("drawcontract") || lowerOriginal.contains("putcontract") ||
            lowerOriginal.contains("getcontract") || lowerOriginal.contains("givecontract") ||
            lowerOriginal.contains("sendcontract") || lowerOriginal.contains("providecontract") ||
            lowerOriginal.contains("helpcontract") || lowerOriginal.contains("assistcontract") ||
            lowerOriginal.contains("needcontract") || lowerOriginal.contains("wantcontract") ||
            lowerOriginal.contains("requirecontract") || lowerOriginal.contains("requestcontract") ||
            lowerOriginal.contains("ordercontract") || lowerOriginal.contains("askcontract") ||
            lowerOriginal.contains("demandcontract") || lowerOriginal.contains("wishcontract") ||
            lowerOriginal.contains("likecontract")) {
            return "HELP";
        }
        
        // Check for verb tense variations
        if (lowerOriginal.matches(".*\\b(made|generated|created|built|drafted|initiated|started|produced|prepared|composed|wrote|constructed|formed|developed|assembled|manufactured|fabricated|established|set up|did|drew|put|got|gave|sent|provided|helped|assisted|needed|wanted|required|requested|ordered|asked|demanded|wished|liked)\\b.*") ||
            lowerNormalized.matches(".*\\b(made|generated|created|built|drafted|initiated|started|produced|prepared|composed|wrote|constructed|formed|developed|assembled|manufactured|fabricated|established|set up|did|drew|put|got|gave|sent|provided|helped|assisted|needed|wanted|required|requested|ordered|asked|demanded|wished|liked)\\b.*")) {
            return "HELP";
        }
        
        // Check for progressive tense variations
        if (lowerOriginal.matches(".*\\b(creating|making|generating|building|drafting|initiating|starting|producing|preparing|composing|writing|constructing|forming|developing|assembling|manufacturing|fabricating|establishing|setting up|doing|drawing|putting|getting|giving|sending|providing|helping|assisting|needing|wanting|requiring|requesting|ordering|asking|demanding|wishing|liking)\\b.*") ||
            lowerNormalized.matches(".*\\b(creating|making|generating|building|drafting|initiating|starting|producing|preparing|composing|writing|constructing|forming|developing|assembling|manufacturing|fabricating|establishing|setting up|doing|drawing|putting|getting|giving|sending|providing|helping|assisting|needing|wanting|requiring|requesting|ordering|asking|demanding|wishing|liking)\\b.*")) {
            return "HELP";
        }
        
        // Check for noun forms
        if (lowerOriginal.matches(".*\\b(creation|generation|making|building|drafting|initiation|starting|production|preparation|composition|writing|construction|formation|development|assembly|manufacturing|fabrication|establishment|setup|doing|drawing|putting|getting|giving|sending|providing|helping|assisting|needing|wanting|requiring|requesting|ordering|asking|demanding|wishing|liking)\\b.*") ||
            lowerNormalized.matches(".*\\b(creation|generation|making|building|drafting|initiation|starting|production|preparation|composition|writing|construction|formation|development|assembly|manufacturing|fabrication|establishment|setup|doing|drawing|putting|getting|giving|sending|providing|helping|assisting|needing|wanting|requiring|requesting|ordering|asking|demanding|wishing|liking)\\b.*")) {
            return "HELP";
        }
        
        // Check for short inputs that should be HELP
        if (lowerOriginal.length() <= 5) {
            if (WordDatabase.isCreationVerb(lowerOriginal) || 
                WordDatabase.isQuestionWord(lowerOriginal) ||
                lowerOriginal.equals("help")) {
                return "HELP";
            }
        }
        


        // PRIORITY 5: Check for specific part-related queries that should be PARTS
        // But exclude contract creation queries that contain "part" words
        boolean isContractCreationQuery = lowerOriginal.contains("create") || lowerOriginal.contains("make") || 
                                         lowerOriginal.contains("generate") || lowerOriginal.contains("build") ||
                                         lowerOriginal.contains("how to") || lowerOriginal.contains("steps") ||
                                         lowerOriginal.contains("process") || lowerOriginal.contains("guide");
        
        // Only match 'part' or 'parts' as standalone words
        boolean hasStandalonePart = lowerOriginal.matches(".*\\bpart\\b.*") || lowerOriginal.matches(".*\\bparts\\b.*");
        
        if ((hasStandalonePart || lowerOriginal.matches(".*\\b(price|cost|moq|lead time|uom)\\b.*")) && !isContractCreationQuery) {
            return "PARTS";
        }
        
        return "CONTRACTS"; // Default fallback
    }
    
    /**
     * Enhanced action type detection using WordDatabase
     * FIXED: Now returns action types that match NLPEntityProcessor expectations
     */
    public static String determineActionType(String originalInput, String normalizedInput, String queryType) {
        String lowerOriginal = originalInput.toLowerCase();
        String lowerNormalized = normalizedInput.toLowerCase();
        
        // Handle PARTS queries
        if ("PARTS".equals(queryType)) {
            // Check for contract number (6+ digits)
            boolean hasContractNumber = lowerOriginal.matches(".*\\b\\d{6,}\\b.*") || lowerNormalized.matches(".*\\b\\d{6,}\\b.*");
            
            // Check for part number patterns
            boolean hasPartNumber = lowerOriginal.matches(".*\\b[A-Za-z0-9]{3,}(?:-[A-Za-z0-9]+)*\\b.*") || 
                                   lowerNormalized.matches(".*\\b[A-Za-z0-9]{3,}(?:-[A-Za-z0-9]+)*\\b.*");
            
            if (hasPartNumber && hasContractNumber) {
                // Specific part query with contract number
                return "parts_by_part_number";
            } else if (hasContractNumber) {
                // Parts by contract number
                return "parts_by_contract_number";
            } else if (hasPartNumber) {
                // Parts by part number (without contract)
                return "parts_by_part_number";
            } else {
                // Default parts query
                return "parts_by_contract_number";
            }
        }
        
        // Handle FAILED_PARTS queries
        if ("FAILED_PARTS".equals(queryType)) {
            return "parts_failed_by_contract_number";
        }
        
        // Handle HELP queries
        if ("HELP".equals(queryType)) {
            // Refined logic for distinguishing USER vs BOT intent
            String lower = lowerOriginal;
            // Explicit instructional/question forms (USER)
            boolean isUserInstruction =
                lower.matches(".*(how to|steps to|steps for|walk me through|explain how to|instructions for|process for|guide for|show me how to|what's the process|need guidance|help understanding|understanding|guidance|explain|instructions|process|guide|walk me through|need help understanding|i need guidance|explain how|explain).*contract.*")
                || lower.matches(".*can you show me.*contract.*")
                || lower.matches(".*can you explain.*contract.*");
            // Imperative/command forms (BOT)
            boolean isBotCreate =
                lower.matches(".*(create|make|generate|set up|setup|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish).*contract.*")
                && !isUserInstruction;
            // If both, prioritize USER if question/instructional context is present
            if (isUserInstruction) {
                return "HELP_CONTRACT_CREATE_USER";
            } else if (isBotCreate) {
                return "HELP_CONTRACT_CREATE_BOT";
            } else {
                // Fallback: if 'for me', treat as BOT
                if (lower.contains("for me") || lower.contains("can you") || lower.contains("please") || lower.contains("could you") || lower.contains("need you to") || lower.contains("want you to")) {
                    return "HELP_CONTRACT_CREATE_BOT";
                }
                // Default to USER
                return "HELP_CONTRACT_CREATE_USER";
            }
        }
        
        // Handle CONTRACTS queries
        if ("CONTRACTS".equals(queryType)) {
            // Check for contract number (6+ digits)
            boolean hasContractNumber = lowerOriginal.matches(".*\\b\\d{6,}\\b.*") || lowerNormalized.matches(".*\\b\\d{6,}\\b.*");
            
            if (hasContractNumber) {
                return "contracts_by_contractnumber";
            } else {
                return "contracts_by_filter";
            }
        }
        
        // Default fallback
        return "contracts_by_contractnumber";
    }
    
    /**
     * Comprehensive text analysis using WordDatabase
     */
    public static TextAnalysisResult analyzeText(String input) {
        if (input == null) {
            return new TextAnalysisResult(null, null, "CONTRACTS", "contracts_by_contractnumber", 0.0);
        }
        
        String normalized = normalizeText(input);
        String queryType = determineQueryType(input, normalized);
        String actionType = determineActionType(input, normalized, queryType);
        
        // Calculate confidence based on analysis
        double confidence = calculateConfidence(input, normalized, queryType, actionType);
        
        return new TextAnalysisResult(input, normalized, queryType, actionType, confidence);
    }
    
    /**
     * Calculate confidence score for the analysis
     */
    private static double calculateConfidence(String original, String normalized, String queryType, String actionType) {
        double confidence = 0.5; // Base confidence
        
        // Boost confidence for clear patterns
        if (WordDatabase.containsCreationWords(original)) confidence += 0.2;
        if (WordDatabase.containsImperativeIndicators(original)) confidence += 0.15;
        if (WordDatabase.containsQuestionWords(original)) confidence += 0.15;
        if (original.length() > 10) confidence += 0.1; // Longer inputs are usually clearer
        
        // Reduce confidence for ambiguous cases
        if (original.length() < 3) confidence -= 0.2;
        if (original.contains("*") || original.contains("@") || original.contains("#")) confidence += 0.1; // Special chars handled well
        
        return Math.min(1.0, Math.max(0.0, confidence));
    }
    
    /**
     * Result class for text analysis
     */
    public static class TextAnalysisResult {
        public final String originalInput;
        public final String normalizedInput;
        public final String queryType;
        public final String actionType;
        public final double confidence;
        
        public TextAnalysisResult(String originalInput, String normalizedInput, 
                                String queryType, String actionType, double confidence) {
            this.originalInput = originalInput;
            this.normalizedInput = normalizedInput;
            this.queryType = queryType;
            this.actionType = actionType;
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return String.format("TextAnalysisResult{original='%s', normalized='%s', queryType='%s', actionType='%s', confidence=%.2f}",
                               originalInput, normalizedInput, queryType, actionType, confidence);
        }
    }
} 