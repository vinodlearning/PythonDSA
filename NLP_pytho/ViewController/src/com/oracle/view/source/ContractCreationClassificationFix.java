package com.oracle.view.source;

public class ContractCreationClassificationFix {
    
    public static void main(String[] args) {
        System.out.println("=== CONTRACT CREATION CLASSIFICATION FIX TEST ===\n");
        
        // Test the specific scenarios that are failing
        String[] testQueries = {
            // Scenario 1: User wants instructions (HELP_CONTRACT_CREATE_USER)
            "Tell me how to create a contract",
            "How to create contarct?",
            "Steps to create contract",
            "Can you show me how to make a contract?",
            "What's the process for contract creation?",
            "I need guidance on creating a contract",
            "Walk me through contract creation",
            "Explain how to set up a contract",
            "Instructions for making a contract",
            "Need help understanding contract creation",
            
            // Scenario 2: User wants system to create (HELP_CONTRACT_CREATE_BOT)
            "Create a contract for me",
            "Can you create contract?",
            "Please make a contract",
            "Generate a contract",
            "I need you to create a contract",
            "Set up a contract",
            "Make me a contract",
            "Initiate contract creation",
            "Start a new contract",
            "Could you draft a contract?"
        };
        
        System.out.println("Testing EnhancedNLPProcessor classification logic:\n");
        
        for (String query : testQueries) {
            testClassification(query);
        }
        
        System.out.println("\n=== ANALYSIS ===\n");
        System.out.println("If any queries are classified as CONTRACTS instead of HELP, we need to fix the logic.");
        System.out.println("The issue is likely in the HELP detection pattern matching.");
    }
    
    private static void testClassification(String query) {
        try {
            // Test the EnhancedNLPProcessor directly
            String normalized = EnhancedNLPProcessor.normalizeText(query);
            String queryType = EnhancedNLPProcessor.determineQueryType(query, normalized);
            String actionType = EnhancedNLPProcessor.determineActionType(query, normalized, queryType);
            
            // Determine expected classification
            String expectedQueryType = getExpectedQueryType(query);
            String expectedActionType = getExpectedActionType(query);
            
            // Check if classification is correct
            boolean queryTypeCorrect = expectedQueryType.equals(queryType);
            boolean actionTypeCorrect = expectedActionType.equals(actionType);
            
            System.out.printf("Query: \"%s\"\n", query);
            System.out.printf("  Expected: %s | %s\n", expectedQueryType, expectedActionType);
            System.out.printf("  Actual:   %s | %s\n", queryType, actionType);
            System.out.printf("  Status:   %s | %s\n", 
                queryTypeCorrect ? "PASS" : "FAIL", 
                actionTypeCorrect ? "PASS" : "FAIL");
            System.out.println();
            
        } catch (Exception e) {
            System.out.printf("Query: \"%s\" - ERROR: %s\n\n", query, e.getMessage());
        }
    }
    
    private static String getExpectedQueryType(String query) {
        String lowerQuery = query.toLowerCase();
        
        // All contract creation queries should be HELP
        if (lowerQuery.contains("contract") || lowerQuery.contains("contarct")) {
            return "HELP";
        }
        
        return "CONTRACTS"; // Default fallback
    }
    
    private static String getExpectedActionType(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Scenario 1: User wants instructions (HELP_CONTRACT_CREATE_USER)
        boolean wantsInstructions = 
            lowerQuery.contains("how to") ||
            lowerQuery.contains("steps") ||
            lowerQuery.contains("process") ||
            lowerQuery.contains("guide") ||
            lowerQuery.contains("instructions") ||
            lowerQuery.contains("walk me through") ||
            lowerQuery.contains("explain") ||
            lowerQuery.contains("need guidance") ||
            lowerQuery.contains("what's the process") ||
            lowerQuery.contains("help understanding") ||
            lowerQuery.contains("understanding") ||
            lowerQuery.contains("guidance");
        
        // Scenario 2: User wants system to create (HELP_CONTRACT_CREATE_BOT)
        boolean wantsBotCreation = 
            lowerQuery.contains("create") ||
            lowerQuery.contains("make") ||
            lowerQuery.contains("generate") ||
            lowerQuery.contains("set up") ||
            lowerQuery.contains("setup") ||
            lowerQuery.contains("draft") ||
            lowerQuery.contains("initiate") ||
            lowerQuery.contains("start") ||
            lowerQuery.contains("for me") ||
            lowerQuery.contains("can you") ||
            lowerQuery.contains("please") ||
            lowerQuery.contains("could you") ||
            lowerQuery.contains("need you to") ||
            lowerQuery.contains("want you to");
        
        if (wantsInstructions && !wantsBotCreation) {
            return "HELP_CONTRACT_CREATE_USER";
        } else if (wantsBotCreation) {
            return "HELP_CONTRACT_CREATE_BOT";
        } else {
            return "HELP_CONTRACT_CREATE_USER"; // Default to user instructions
        }
    }
} 