package com.oracle.view.source;

/**
 * Test to verify comma-separated contract creation input handling
 */
public class CommaSeparatedContractTest {
    
    public static void main(String[] args) {
        System.out.println("=== Comma-Separated Contract Creation Test ===\n");
        
        try {
            ConversationalNLPManager manager = new ConversationalNLPManager();
            
            // Test 1: Check if comma-separated input is detected as complete contract creation
            System.out.println("--- Test 1: Check complete contract creation detection ---");
            String commaInput = "1000585412,TestContarctbyvinod,createtitle,testdescription,testcommenst,no";
            boolean isComplete = manager.isCompleteContractCreationInput(commaInput);
            System.out.println("Input: " + commaInput);
            System.out.println("Is Complete Contract Creation: " + isComplete);
            System.out.println();
            
            // Test 2: Check if it's not treated as user selection
            System.out.println("--- Test 2: Check user selection detection ---");
            boolean isUserSelection = manager.isUserSelection(commaInput);
            System.out.println("Input: " + commaInput);
            System.out.println("Is User Selection: " + isUserSelection);
            System.out.println();
            
            // Test 3: Test the complete flow
            System.out.println("--- Test 3: Complete flow test ---");
            String sessionId = "test-session-" + System.currentTimeMillis();
            String userId = "test-user";
            
            // Start contract creation
            ConversationalNLPManager.ChatbotResponse response1 = manager.processUserInput("create contract", sessionId, userId);
            System.out.println("Step 1 - Start contract creation:");
            System.out.println("Response: " + (response1 != null ? "Success" : "Failed"));
            System.out.println();
            
            // Provide complete data
            ConversationalNLPManager.ChatbotResponse response2 = manager.processUserInput(commaInput, sessionId, userId);
            System.out.println("Step 2 - Provide complete data:");
            System.out.println("Response: " + (response2 != null ? "Success" : "Failed"));
            if (response2 != null) {
                System.out.println("Is Success: " + response2.isSuccess);
                System.out.println("Action Type: " + (response2.metadata != null ? response2.metadata.actionType : "null"));
            }
            System.out.println();
            
            System.out.println("=== Test completed! ===");
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 