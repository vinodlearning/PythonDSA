package com.oracle.view.source;

import java.util.*;

/**
 * Specialized processor for Help/Creation queries
 * Handles all queries related to contract creation, help, guidance, etc.
 */
public class HelpProcessor {
    
    // Action type constants - now using centralized constants
    private static final String HELP_CONTRACT_CREATE_BOT = NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT;
    private static final String HELP_CONTRACT_CREATE_USER = NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_USER;
    
    /**
     * Process help query
     */
    public NLPQueryClassifier.QueryResult process(String originalInput, String correctedInput, String normalizedInput) {
        NLPQueryClassifier.QueryResult result = new NLPQueryClassifier.QueryResult();

        // --- NEW: Use OpenNLP-powered entity extraction ---
        EntityExtractor extractor = new EntityExtractor();
        Map<String, String> nlpEntities = extractor.extractAllEntities(originalInput);

        // Set input tracking
        result.inputTracking = new NLPQueryClassifier.InputTrackingResult(originalInput, correctedInput, 0.85);

        // Set header (usually empty for help queries)
        result.header = new NLPQueryClassifier.Header();
        if (nlpEntities.containsKey("CUSTOMER_NUMBER")) {
            result.header.customerNumber = nlpEntities.get("CUSTOMER_NUMBER");
        }
        if (nlpEntities.containsKey("CUSTOMER_NAME")) {
            result.header.customerName = nlpEntities.get("CUSTOMER_NAME");
        }
        // ... add more as needed

        // Determine action type
        String actionType = determineActionType(originalInput, correctedInput);
        result.metadata = new NLPQueryClassifier.QueryMetadata("HELP", actionType, 0.0);

        // Extract entities (use OpenNLP-powered extraction for filters/entities)
        result.entities = new ArrayList<>();
        if (nlpEntities.containsKey("CUSTOMER_NUMBER")) {
            result.entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NUMBER", "=", nlpEntities.get("CUSTOMER_NUMBER"), "nlp"));
        }
        if (nlpEntities.containsKey("CUSTOMER_NAME")) {
            result.entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NAME", "=", nlpEntities.get("CUSTOMER_NAME"), "nlp"));
        }
        // ... add more as needed

        // --- Account number extraction (legacy, can be removed if NER is sufficient) ---
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{6,})").matcher(correctedInput);
        if (m.find() && nlpEntities.get("CUSTOMER_NUMBER") == null) {
            String accountNumber = m.group(1);
            result.entities.add(new NLPQueryClassifier.EntityFilter("CUSTOMER_NUMBER", "=", accountNumber, "user_input"));
        }
        // --- End account number extraction ---

        // Determine display entities
        result.displayEntities = determineDisplayEntities(originalInput, correctedInput);

        // Validate input
        result.errors = validateInput();

        return result;
    }
    
    /**
     * Determine action type based on query content
     */
    private String determineActionType(String originalInput, String correctedInput) {
        String lowerInput = correctedInput.toLowerCase();
        
        // Check for creation keywords
        String[] createKeywords = {
            "create", "make", "generate", "initiate", "build", "draft", "establish", "form", "set up"
        };
        
        boolean hasCreateKeyword = false;
        for (String keyword : createKeywords) {
            if (lowerInput.contains(keyword)) {
                hasCreateKeyword = true;
                break;
            }
        }
        
        // Check for help keywords
        String[] helpKeywords = {
            "help", "steps", "guide", "instruction", "how to", "walk me", "explain",
            "process", "show me how", "need guidance", "teach", "assist", "support"
        };
        
        boolean hasHelpKeyword = false;
        for (String keyword : helpKeywords) {
            if (lowerInput.contains(keyword)) {
                hasHelpKeyword = true;
                break;
            }
        }
        
        // Prefer BOT if both are present, otherwise use appropriate type
        if (hasCreateKeyword) {
            return HELP_CONTRACT_CREATE_BOT;
        } else if (hasHelpKeyword) {
            return HELP_CONTRACT_CREATE_USER;
        } else {
            // Default to USER if ambiguous
            return HELP_CONTRACT_CREATE_USER;
        }
    }
    
    /**
     * Determine display entities based on query content
     */
    private List<String> determineDisplayEntities(String originalInput, String correctedInput) {
        List<String> displayEntities = new ArrayList<>();
        String lowerInput = correctedInput.toLowerCase();

        // If the query is about contract creation help, return the actual contract creation fields
        String[] contractHelpKeywords = {
            "contract", "create", "make", "generate", "initiate", "build", "draft", "establish", "form", "set up",
            "help", "steps", "guide", "instruction", "how to", "walk me", "explain", "process", "show me how", "need guidance", "teach", "assist", "support"
        };
        boolean isContractHelp = false;
        for (String kw : contractHelpKeywords) {
            if (lowerInput.contains(kw)) {
                isContractHelp = true;
                break;
            }
        }
        if (isContractHelp && lowerInput.contains("contract")) {
            // Return the actual contract creation fields
            displayEntities.addAll(Arrays.asList(
                "CONTRACT_NAME", "CUSTOMER_NAME", "ACCOUNT_NUMBER", "DESCRIPTION", "COMMENTS", "TITLE", "IS_PRICELIST"
            ));
            return displayEntities;
        }

        // Otherwise, use generic help topics
        displayEntities.add("HELP_TOPIC");
        displayEntities.add("STEPS");
        displayEntities.add("INSTRUCTIONS");
        if (lowerInput.contains("contract")) {
            displayEntities.add("CONTRACT_CREATION");
        }
        if (lowerInput.contains("step") || lowerInput.contains("steps")) {
            displayEntities.add("STEP_BY_STEP");
        }
        if (lowerInput.contains("guide") || lowerInput.contains("instruction")) {
            displayEntities.add("GUIDE");
        }
        if (lowerInput.contains("process") || lowerInput.contains("workflow")) {
            displayEntities.add("PROCESS");
        }
        return displayEntities;
    }
    
    /**
     * Get contract creation prompt based on action type
     */
    public String getContractCreationPrompt(String actionType, String accountNumber) {
        if ("HELP_CONTRACT_CREATE_BOT".equals(actionType)) {
            if (accountNumber != null && !accountNumber.isEmpty()) {
                return getBotPromptWithAccount(accountNumber);
            } else {
                return getBotPromptWithoutAccount();
            }
        } else {
            return getUserPrompt();
        }
    }
    
    /**
     * Get bot prompt when account number is provided
     */
    private String getBotPromptWithAccount(String accountNumber) {
      return  BCCTChatBotUtility.getBotPromptWithAccount(accountNumber);
    }


    /**
     * Get bot prompt when no account number is provided
     */
    private String getBotPromptWithoutAccount() {
        return "<h4>Automated Contract Creation</h4>" +
               "<p>I'll help you create a contract! Please provide all details in this exact order:</p>" +
               "<p><b>Required Format:</b></p>" +
               "<p style=\"background-color: #f8f9fa; padding: 15px; border-left: 4px solid #007bff; margin: 10px 0;\">" +
               "<b>Account Number, Contract Name, Title, Description, Comments, Price List (Yes/No)</b>" + "</p>" +
               "<p><b>Please provide in this order:</b></p>" + "<ol>" +
               "<li><b>Account Number:</b> (6+ digits customer number)</li>" +
               "<li><b>Contract Name:</b> (e.g., 'ABC Project Services')</li>" +
               "<li><b>Title:</b> (e.g., 'Service Agreement')</li>" +
               "<li><b>Description:</b> (e.g., 'Project implementation services')</li>" +
               "<li><b>Comments:</b> (optional, or type 'nocomments')</li>" +
               "<li><b>Price List Contract:</b> Yes/No (default: No)</li>" + "</ol>" +
               "<p><b>Example:</b> <i>'123456789, testcontract, testtitle, testdesc, nocomments, no'</i></p>" +
               "<p><b>Important:</b> Please use commas to separate each field and provide in the exact order shown above.</p>";
    }


    /**
     * Get user manual prompt
     */
    private String getUserPrompt() {
        return BCCTChatBotUtility.getHelpContent("HELP_CONTRACT_CREATE_USER");
    }
    
    /**
     * Validate input
     */
    private List<NLPQueryClassifier.ValidationError> validateInput() {
        List<NLPQueryClassifier.ValidationError> errors = new ArrayList<>();
        
        // Help queries don't typically need validation
        // But we could add validation for specific cases if needed
        
        return errors;
    }
} 