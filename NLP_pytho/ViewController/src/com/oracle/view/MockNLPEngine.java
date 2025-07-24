package com.oracle.view;




import java.util.*;
import java.util.regex.Pattern;

/**
 * Mock NLP Engine implementation for testing the comprehensive test suite
 * Simulates the complete processing pipeline without requiring actual ML models
 */

public class MockNLPEngine {
    
    // Pattern definitions for entity recognition
    private final Pattern contractNumberPattern = Pattern.compile("\\b\\d{4,}\\b");
    private final Pattern partNumberPattern = Pattern.compile("\\b[A-Z]{2,3}\\d{3,}\\b", Pattern.CASE_INSENSITIVE);
    private final Pattern accountNumberPattern = Pattern.compile("\\b\\d{6,12}\\b");
    
    // Spell correction mappings
    private final Map<String, String> spellCorrections = new HashMap<>();
    
    public MockNLPEngine() {
        initializeSpellCorrections();
    }
    
    /**
     * Main processing method that simulates the NLP pipeline
     */
    public NLPResponse processQuery(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return createErrorResponse("Empty input provided", userInput);
        }
        
        try {
            // Step 1: Tokenization and normalization
            String normalizedInput = normalizeInput(userInput);
            
            // Step 2: Spell correction
            String correctedInput = applySpellCorrection(normalizedInput);
            double correctionConfidence = calculateCorrectionConfidence(normalizedInput, correctedInput);
            
            // Step 3: Entity extraction
            Map<String, String> extractedEntities = extractEntities(correctedInput);
            
            // Step 4: Domain routing
            DomainType domain = determineDomain(correctedInput, extractedEntities);
            
            // Step 5: Generate response based on domain
            return generateDomainResponse(userInput, correctedInput, correctionConfidence, 
                                        extractedEntities, domain);
            
        } catch (Exception e) {
            return createErrorResponse("Processing error: " + e.getMessage(), userInput);
        }
    }
    
    /**
     * Normalize input to handle edge cases
     */
    private String normalizeInput(String input) {
        String normalized = input;
        
        // Handle camelCase splitting
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // Handle letter-number splitting
        normalized = normalized.replaceAll("([a-zA-Z])(\\d)", "$1 $2");
        normalized = normalized.replaceAll("(\\d)([a-zA-Z])", "$1 $2");
        
        // Handle symbols and special characters
        normalized = normalized.replaceAll("[#@$%&*()+=\\[\\]{}|\\\\:;\"'<>,.?/]+", " ");
        
        // Handle multiple spaces
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Handle non-Latin characters
        normalized = normalized.replace("contract", "contract");
        normalized = normalized.replace("part", "part");
        
        return normalized.trim().toLowerCase();
    }
    
    /**
     * Apply spell corrections
     */
    private String applySpellCorrection(String input) {
        String corrected = input;
        
        for (Map.Entry<String, String> correction : spellCorrections.entrySet()) {
            corrected = corrected.replaceAll("\\b" + correction.getKey() + "\\b", correction.getValue());
        }
        
        return corrected;
    }
    
    /**
     * Calculate spell correction confidence
     */
    private double calculateCorrectionConfidence(String original, String corrected) {
        if (original.equals(corrected)) {
            return 0.0; // No corrections made
        }
        
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
    
    /**
     * Extract entities from corrected input
     */
    private Map<String, String> extractEntities(String input) {
        Map<String, String> entities = new HashMap<>();
        
        // Extract contract numbers
        java.util.regex.Matcher contractMatcher = contractNumberPattern.matcher(input);
        if (contractMatcher.find()) {
            entities.put("contractNumber", contractMatcher.group());
        }
        
        // Extract part numbers
        java.util.regex.Matcher partMatcher = partNumberPattern.matcher(input);
        if (partMatcher.find()) {
            entities.put("partNumber", partMatcher.group().toUpperCase());
        }
        
        // Extract account numbers (different from contract numbers)
        java.util.regex.Matcher accountMatcher = accountNumberPattern.matcher(input);
        if (accountMatcher.find()) {
            String number = accountMatcher.group();
            if (number.length() >= 7) { // Account numbers are typically longer
                entities.put("accountNumber", number);
            }
        }
        
        // Extract creator names
        if (input.contains("vinod")) {
            entities.put("createdBy", "vinod");
        } else if (input.contains("mary")) {
            entities.put("createdBy", "mary");
        }
        
        // Extract customer names
        if (input.contains("siemens")) {
            entities.put("customerName", "Siemens");
        } else if (input.contains("boeing")) {
            entities.put("customerName", "Boeing");
        } else if (input.contains("honeywell")) {
            entities.put("customerName", "Honeywell");
        }
        
        return entities;
    }
    
    /**
     * Determine domain based on input analysis
     */
    private DomainType determineDomain(String input, Map<String, String> entities) {
        // Count domain indicators
        int contractScore = 0;
        int partScore = 0;
        int helpScore = 0;
        
        // Contract indicators
        if (input.contains("contract") || input.contains("contrct") || input.contains("cntrct")) {
            contractScore += 3;
        }
        if (entities.containsKey("contractNumber")) {
            contractScore += 2;
        }
        if (input.contains("customer") || input.contains("account") || input.contains("created by")) {
            contractScore += 1;
        }
        if (input.contains("status") || input.contains("expired") || input.contains("active")) {
            contractScore += 1;
        }
        
        // Parts indicators
        if (input.contains("part") || input.contains("prt") || input.contains("pasrt")) {
            partScore += 3;
        }
        if (entities.containsKey("partNumber")) {
            partScore += 2;
        }
        if (input.contains("failed") || input.contains("validation") || input.contains("error")) {
            partScore += 2;
        }
        if (input.contains("loading") || input.contains("missing") || input.contains("rejected")) {
            partScore += 1;
        }
        
        // Help indicators - prioritize help for creation/guidance requests
        if (input.contains("how") || input.contains("help") || input.contains("steps")) {
            helpScore += 3;
        }
        if (input.contains("create") || input.contains("guide")) {
            helpScore += 2;
        }
        if (input.contains("?") || input.startsWith("how ") || input.startsWith("hw ")) {
            helpScore += 1;
        }
        
        // Determine domain based on highest score
        if (contractScore > partScore && contractScore > helpScore) {
            return DomainType.CONTRACTS;
        } else if (partScore > contractScore && partScore > helpScore) {
            return DomainType.PARTS;
        } else if (helpScore > 0) {
            return DomainType.HELP;
        } else {
            // Default routing for ambiguous cases
            if (entities.containsKey("contractNumber")) {
                return DomainType.CONTRACTS;
            } else if (entities.containsKey("partNumber")) {
                return DomainType.PARTS;
            } else {
                return DomainType.HELP;
            }
        }
    }
    
    /**
     * Generate domain-specific response
     */
    private NLPResponse generateDomainResponse(String originalInput, String correctedInput, 
                                             double correctionConfidence, Map<String, String> entities, 
                                             DomainType domain) {
        
        // Create response header
        ResponseHeader header = ResponseHeader.builder()
            .contractNumber(entities.get("contractNumber"))
            .partNumber(entities.get("partNumber"))
            .customerNumber(entities.get("accountNumber"))
            .customerName(entities.get("customerName"))
            .createdBy(entities.get("createdBy"))
            .inputTracking(InputTracking.builder()
                .originalInput(originalInput)
                .correctedInput(correctedInput.equals(originalInput.toLowerCase()) ? null : correctedInput)
                .correctionConfidence(correctionConfidence)
                .build())
            .build();
        
        // Create query metadata
        String queryType = domain.name();
        String actionType = generateActionType(domain, correctedInput, entities);
        
        QueryMetadata metadata = QueryMetadata.builder()
            .queryType(queryType)
            .actionType(actionType)
            .processingTimeMs(Math.random() * 50 + 10) // Simulate processing time
            .selectedModule(domain.name())
            .routingConfidence(0.85 + Math.random() * 0.15) // Simulate confidence
            .build();
        
        // Generate display entities
        List<String> displayEntities = generateDisplayEntities(domain, entities);
        
        // Generate module-specific data
        Object moduleSpecificData = generateModuleSpecificData(domain, entities, originalInput);
        
        // Calculate overall confidence
        double confidence = calculateOverallConfidence(domain, entities, correctionConfidence);
        
        return NLPResponse.builder()
            .header(header)
            .queryMetadata(metadata)
            .entities(new ArrayList<>()) // Simplified for mock
            .displayEntities(displayEntities)
            .moduleSpecificData(moduleSpecificData)
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
    
    /**
     * Generate action type based on domain and input analysis
     */
    private String generateActionType(DomainType domain, String input, Map<String, String> entities) {
        String lowerInput = input.toLowerCase();
        
        switch (domain) {
            case CONTRACTS:
                // Check for specific contract-related actions based on user specification
                if (entities.containsKey("contractNumber")) {
                    return "contracts_by_contractNumber";
                } else if (entities.containsKey("createdBy")) {
                    return "contracts_by_user";
                } else if (entities.containsKey("accountNumber")) {
                    return "contracts_by_accountNumber";
                } else if (entities.containsKey("customerName")) {
                    return "contracts_by_customerName";
                } else if (lowerInput.contains("part") || lowerInput.contains("prt")) {
                    return "contracts_by_parts";
                } else if (lowerInput.contains("date") || lowerInput.contains("after") || lowerInput.contains("before") || 
                          lowerInput.contains("between") || lowerInput.contains("expired") || lowerInput.contains("created in")) {
                    return "contracts_by_dates";
                } else {
                    return "contracts_by_contractNumber"; // Default for contract queries
                }
                
            case PARTS:
                // Check for specific parts-related actions based on user specification
                if (entities.containsKey("createdBy")) {
                    return "parts_by_user";
                } else if (entities.containsKey("contractNumber")) {
                    return "parts_by_contract";
                } else if (entities.containsKey("partNumber")) {
                    return "parts_by_partNumber";
                } else if (entities.containsKey("customerName") || entities.containsKey("accountNumber")) {
                    return "parts_by_customer";
                } else {
                    return "parts_by_contract"; // Default for parts queries
                }
                
            case HELP:
                if (lowerInput.contains("create") || lowerInput.contains("creat")) {
                    return "help_create_contract";
                } else if (lowerInput.contains("load")) {
                    return "help_load_parts";
                } else {
                    return "help_general_guidance";
                }
                
            default:
                return "unknown_action";
        }
    }
    
    /**
     * Generate display entities based on domain
     */
    private List<String> generateDisplayEntities(DomainType domain, Map<String, String> entities) {
        List<String> displayEntities = new ArrayList<>();
        
        switch (domain) {
            case CONTRACTS:
                displayEntities.add("CONTRACT_NUMBER");
                displayEntities.add("CUSTOMER_NAME");
                if (entities.containsKey("createdBy")) {
                    displayEntities.add("CREATED_BY");
                }
                if (entities.containsKey("accountNumber")) {
                    displayEntities.add("ACCOUNT_NUMBER");
                }
                displayEntities.add("EFFECTIVE_DATE");
                displayEntities.add("STATUS");
                break;
                
            case PARTS:
                displayEntities.add("PART_NUMBER");
                if (entities.containsKey("contractNumber")) {
                    displayEntities.add("CONTRACT_NUMBER");
                }
                displayEntities.add("ERROR_COLUMN");
                displayEntities.add("REASON");
                displayEntities.add("STATUS");
                break;
                
            case HELP:
                displayEntities.add("HELP_CONTENT");
                displayEntities.add("STEPS");
                break;
        }
        
        return displayEntities;
    }
    
    /**
     * Generate module-specific data
     */
    private Object generateModuleSpecificData(DomainType domain, Map<String, String> entities, String input) {
        Map<String, Object> data = new HashMap<>();
        
        switch (domain) {
            case CONTRACTS:
                if (entities.containsKey("contractNumber")) {
                    data.put("contractNumber", entities.get("contractNumber"));
                    data.put("effectiveDate", "2024-01-01");
                    data.put("expirationDate", "2024-12-31");
                    data.put("status", "ACTIVE");
                }
                if (entities.containsKey("customerName")) {
                    data.put("customerName", entities.get("customerName"));
                }
                break;
                
            case PARTS:
                if (entities.containsKey("partNumber")) {
                    Map<String, Object> failureDetails = new HashMap<>();
                    failureDetails.put("partNumber", entities.get("partNumber"));
                    failureDetails.put("errorColumn", "PRICE");
                    failureDetails.put("reason", "No cost data available");
                    failureDetails.put("status", "FAILED_VALIDATION");
                    data.put("failureDetails", failureDetails);
                }
                break;
                
            case HELP:
                List<String> steps = Arrays.asList(
                    "Navigate to Contract Creation section",
                    "Enter required contract details",
                    "Validate all mandatory fields",
                    "Submit for approval",
                    "Monitor status until completion"
                );
                data.put("steps", steps);
                data.put("helpType", "CONTRACT_CREATION");
                break;
        }
        
        return data;
    }
    
    /**
     * Calculate overall confidence
     */
    private double calculateOverallConfidence(DomainType domain, Map<String, String> entities, 
                                            double correctionConfidence) {
        double baseConfidence = 0.7;
        
        // Boost confidence based on entity extraction
        if (!entities.isEmpty()) {
            baseConfidence += 0.1 * entities.size();
        }
        
        // Adjust based on domain clarity
        switch (domain) {
            case CONTRACTS:
                if (entities.containsKey("contractNumber")) {
                    baseConfidence += 0.15;
                }
                break;
            case PARTS:
                if (entities.containsKey("partNumber")) {
                    baseConfidence += 0.15;
                }
                break;
            case HELP:
                baseConfidence += 0.05; // Help queries are generally easier
                break;
        }
        
        // Factor in correction confidence (if corrections were made successfully)
        if (correctionConfidence > 0) {
            baseConfidence += 0.05; // Successful correction boosts confidence
        }
        
        return Math.min(0.98, Math.max(0.50, baseConfidence));
    }
    
    /**
     * Create error response
     */
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
    
    /**
     * Initialize spell correction mappings
     */
    private void initializeSpellCorrections() {
        // Contract domain corrections
        spellCorrections.put("contrct", "contract");
        spellCorrections.put("cntrct", "contract");
        spellCorrections.put("contrakt", "contract");
        spellCorrections.put("kontract", "contract");
        spellCorrections.put("kontrakt", "contract");
        spellCorrections.put("shwo", "show");
        spellCorrections.put("shw", "show");
        spellCorrections.put("statuz", "status");
        spellCorrections.put("statuss", "status");
        spellCorrections.put("exipred", "expired");
        spellCorrections.put("custmer", "customer");
        spellCorrections.put("cstomer", "customer");
        spellCorrections.put("acount", "account");
        spellCorrections.put("accnt", "account");
        spellCorrections.put("accunt", "account");
        spellCorrections.put("detials", "details");
        spellCorrections.put("detals", "details");
        spellCorrections.put("summry", "summary");
        spellCorrections.put("sumry", "summary");
        spellCorrections.put("creatd", "created");
        spellCorrections.put("aftr", "after");
        spellCorrections.put("efective", "effective");
        
        // Parts domain corrections
        spellCorrections.put("pasrt", "part");
        spellCorrections.put("prt", "part");
        spellCorrections.put("prts", "parts");
        spellCorrections.put("partz", "parts");
        spellCorrections.put("faild", "failed");
        spellCorrections.put("faield", "failed");
        spellCorrections.put("filde", "failed");
        spellCorrections.put("fialed", "failed");
        spellCorrections.put("validdation", "validation");
        spellCorrections.put("loadded", "loaded");
        spellCorrections.put("misssing", "missing");
        spellCorrections.put("addedd", "added");
        spellCorrections.put("pricng", "pricing");
        spellCorrections.put("discntinued", "discontinued");
        spellCorrections.put("successfull", "successful");
        spellCorrections.put("passd", "passed");
        spellCorrections.put("becasue", "because");
        spellCorrections.put("loadding", "loading");
        spellCorrections.put("specifcations", "specifications");
        spellCorrections.put("compatble", "compatible");
        spellCorrections.put("avalable", "available");
        spellCorrections.put("manufacterer", "manufacturer");
        spellCorrections.put("warrenty", "warranty");
        
        // Help domain corrections
        spellCorrections.put("hw", "how");
        spellCorrections.put("creat", "create");
        spellCorrections.put("2", "to");
        
        // General corrections
        spellCorrections.put("giv", "give");
        spellCorrections.put("givme", "give me");
        spellCorrections.put("pls", "please");
        spellCorrections.put("chk", "check");
        spellCorrections.put("chek", "check");
        spellCorrections.put("lst", "list");
        spellCorrections.put("wht", "what");
        spellCorrections.put("wat", "what");
        spellCorrections.put("wats", "what is");
        spellCorrections.put("whts", "what is");
        spellCorrections.put("whch", "which");
        spellCorrections.put("whr", "where");
        spellCorrections.put("yu", "you");
        spellCorrections.put("provid", "provide");
        spellCorrections.put("isses", "issues");
        spellCorrections.put("numbr", "number");
        spellCorrections.put("numer", "number");
        spellCorrections.put("reasn", "reason");
        spellCorrections.put("failr", "failure");
        spellCorrections.put("falure", "failure");
        spellCorrections.put("mesage", "message");
        spellCorrections.put("colum", "column");
        spellCorrections.put("resn", "reason");
        spellCorrections.put("voltag", "voltage");
        spellCorrections.put("ovrheating", "overheating");
    }
    
    /**
     * Get processing metrics (mock implementation)
     */
    public ProcessingMetrics getMetrics() {
        return new ProcessingMetrics();
    }
    
    /**
     * Health check
     */
    public boolean isHealthy() {
        return true;
    }
    
    /**
     * Get available domains
     */
    public Set<DomainType> getAvailableDomains() {
        return Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(DomainType.CONTRACTS, DomainType.PARTS, DomainType.HELP))
        );
    }
    
    /**
     * Mock processing metrics class
     */
    public static class ProcessingMetrics {
        public String getMetricsSummary() {
            return "=== Mock NLP Engine Metrics ===\n" +
                   "Total Requests: 0\n" +
                   "Domain Distribution: Mock implementation\n";
        }
    }
}