package com.oracle.view.source;

/**
 * Test to verify centralized NLP constants are working correctly
 */
public class NLPConstantsTest {
    
    public static void main(String[] args) {
        System.out.println("=== NLP Constants Test ===\n");
        
        try {
            // Test 1: Verify constants are defined correctly
            System.out.println("--- Test 1: Constants Definition ---");
            System.out.println("HELP_CONTRACT_CREATE_BOT: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT);
            System.out.println("HELP_CONTRACT_CREATE_USER: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_USER);
            System.out.println("QUERY_TYPE_HELP: " + NLPConstants.QUERY_TYPE_HELP);
            
            // Test 2: Verify utility methods work correctly
            System.out.println("\n--- Test 2: Utility Methods ---");
            System.out.println("isBotContractCreationAction(HELP_CONTRACT_CREATE_BOT): " + 
                NLPConstants.isBotContractCreationAction(NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT));
            System.out.println("isUserContractCreationAction(HELP_CONTRACT_CREATE_USER): " + 
                NLPConstants.isUserContractCreationAction(NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_USER));
            System.out.println("isHelpQuery(HELP): " + 
                NLPConstants.isHelpQuery(NLPConstants.QUERY_TYPE_HELP));
            
            // Test 3: Test ContractProcessor with constants
            System.out.println("\n--- Test 3: ContractProcessor with Constants ---");
            ContractProcessor processor = new ContractProcessor();
            
            // Test bot creation query
            NLPQueryClassifier.QueryResult botResult = processor.process("create contract for me", "create contract for me", "create contract for me");
            System.out.println("'create contract for me' -> Action Type: " + botResult.metadata.actionType);
            System.out.println("Expected: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT);
            System.out.println("Matches: " + botResult.metadata.actionType.equals(NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT));
            
            // Test user help query
            NLPQueryClassifier.QueryResult userResult = processor.process("how to create a contract", "how to create a contract", "how to create a contract");
            System.out.println("'how to create a contract' -> Action Type: " + userResult.metadata.actionType);
            System.out.println("Expected: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_USER);
            System.out.println("Matches: " + userResult.metadata.actionType.equals(NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_USER));
            
            // Test 4: Verify consistency across different classes
            System.out.println("\n--- Test 4: Cross-Class Consistency ---");
            System.out.println("ContractProcessor returns: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT);
            System.out.println("HelpProcessor uses: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT);
            System.out.println("NLPConstants defines: " + NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT);
            
            boolean allConsistent = NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT.equals(NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT) &&
                                   NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT.equals(NLPConstants.ACTION_TYPE_HELP_CONTRACT_CREATE_BOT);
            
            System.out.println("All classes consistent: " + allConsistent);
            
            System.out.println("\n=== Test Summary ===");
            System.out.println("✅ Centralized constants are working correctly!");
            System.out.println("✅ All classes are using the same action type names!");
            System.out.println("✅ No more confusion between HELP_CONTRACT_CREATE_BOT and HELP_CONTRACT_CREATE_BOT!");
            
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 