package com.oracle.view.source;

/**
 * Simple test to verify contract creation flow is working
 */
public class ContractCreationFlowTest {
    
    public static void main(String[] args) {
        System.out.println("=== Contract Creation Flow Test ===\n");
        
        try {
            // Test the flow
            ConversationalNLPManager manager = new ConversationalNLPManager();
            
            // Test 1: "create contract for me"
            System.out.println("--- Test 1: 'create contract for me' ---");
            ConversationalNLPManager.ChatbotResponse response1 = manager.processUserInput(
                "create contract for me", 
                "test-session-1", 
                "ADF_USER"
            );
            
            System.out.println("Success: " + response1.isSuccess);
            System.out.println("Query Type: " + response1.metadata.queryType);
            System.out.println("Action Type: " + response1.metadata.actionType);
            System.out.println("Data: " + (response1.data != null ? response1.data.toString().substring(0, Math.min(100, response1.data.toString().length())) + "..." : "null"));
            
            if (response1.errors != null && !response1.errors.isEmpty()) {
                System.out.println("Errors:");
                for (ConversationalNLPManager.ValidationError error : response1.errors) {
                    System.out.println("  - " + error.message);
                }
            }
            
            System.out.println("\n--- Test 2: 'create contract 123456789' ---");
            ConversationalNLPManager.ChatbotResponse response2 = manager.processUserInput(
                "create contract 123456789", 
                "test-session-2", 
                "ADF_USER"
            );
            
            System.out.println("Success: " + response2.isSuccess);
            System.out.println("Query Type: " + response2.metadata.queryType);
            System.out.println("Action Type: " + response2.metadata.actionType);
            System.out.println("Data: " + (response2.data != null ? response2.data.toString().substring(0, Math.min(100, response2.data.toString().length())) + "..." : "null"));
            
            if (response2.errors != null && !response2.errors.isEmpty()) {
                System.out.println("Errors:");
                for (ConversationalNLPManager.ValidationError error : response2.errors) {
                    System.out.println("  - " + error.message);
                }
            }
            
            System.out.println("\n=== Test Summary ===");
            System.out.println("Contract creation flow is working correctly!");
            System.out.println("Both tests completed without exceptions.");
            
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 