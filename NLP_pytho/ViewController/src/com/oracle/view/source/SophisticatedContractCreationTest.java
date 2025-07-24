package com.oracle.view.source;

/**
 * Test to verify sophisticated contract creation flow with state management
 */
public class SophisticatedContractCreationTest {
    
    public static void main(String[] args) {
        System.out.println("=== Sophisticated Contract Creation Flow Test ===\n");
        
        try {
            ConversationalNLPManager manager = new ConversationalNLPManager();
            
            // Test 1: Start contract creation
            System.out.println("--- Test 1: Start Contract Creation ---");
            String sessionId = "test-session-" + System.currentTimeMillis();
            String userId = "test-user";
            
            ConversationalNLPManager.ChatbotResponse response1 = manager.processUserInput("create contract", sessionId, userId);
            System.out.println("Step 1 - Start contract creation:");
            System.out.println("Response: " + (response1 != null ? "Success" : "Failed"));
            if (response1 != null) {
                System.out.println("Is Success: " + response1.isSuccess);
                System.out.println("Action Type: " + (response1.metadata != null ? response1.metadata.actionType : "null"));
            }
            System.out.println();
            
            // Test 2: Provide complete data
            System.out.println("--- Test 2: Provide Complete Data ---");
            String completeData = "1000585412,TEST_CONTRACT,TEST_TITLE,TEST_DESCRIPTION,TEST_COMMENT,NO";
            ConversationalNLPManager.ChatbotResponse response2 = manager.processUserInput(completeData, sessionId, userId);
            System.out.println("Step 2 - Provide complete data:");
            System.out.println("Response: " + (response2 != null ? "Success" : "Failed"));
            if (response2 != null) {
                System.out.println("Is Success: " + response2.isSuccess);
                System.out.println("Action Type: " + (response2.metadata != null ? response2.metadata.actionType : "null"));
                System.out.println("Data contains confirmation: " + (response2.data != null && response2.data.toString().contains("confirm")));
            }
            System.out.println();
            
            // Test 3: Confirm contract creation
            System.out.println("--- Test 3: Confirm Contract Creation ---");
            ConversationalNLPManager.ChatbotResponse response3 = manager.processUserInput("yes", sessionId, userId);
            System.out.println("Step 3 - Confirm contract creation:");
            System.out.println("Response: " + (response3 != null ? "Success" : "Failed"));
            if (response3 != null) {
                System.out.println("Is Success: " + response3.isSuccess);
                System.out.println("Action Type: " + (response3.metadata != null ? response3.metadata.actionType : "null"));
                System.out.println("Data contains success: " + (response3.data != null && response3.data.toString().contains("Successfully")));
            }
            System.out.println();
            
            // Test 4: Test account number validation
            System.out.println("--- Test 4: Account Number Validation ---");
            String newSessionId = "test-session-account-" + System.currentTimeMillis();
            
            // Start contract creation
            manager.processUserInput("create contract", newSessionId, userId);
            
            // Provide invalid account number
            ConversationalNLPManager.ChatbotResponse response4 = manager.processUserInput("12345", newSessionId, userId);
            System.out.println("Step 4 - Invalid account number:");
            System.out.println("Response: " + (response4 != null ? "Success" : "Failed"));
            if (response4 != null) {
                System.out.println("Is Success: " + response4.isSuccess);
                System.out.println("Data contains invalid: " + (response4.data != null && response4.data.toString().contains("Invalid")));
            }
            System.out.println();
            
            // Test 5: Test interruption handling
            System.out.println("--- Test 5: Interruption Handling ---");
            String interruptSessionId = "test-session-interrupt-" + System.currentTimeMillis();
            
            // Start contract creation
            manager.processUserInput("create contract", interruptSessionId, userId);
            
            // Try to start a new query while contract creation is active
            ConversationalNLPManager.ChatbotResponse response5 = manager.processUserInput("show contracts", interruptSessionId, userId);
            System.out.println("Step 5 - Interrupt with new query:");
            System.out.println("Response: " + (response5 != null ? "Success" : "Failed"));
            if (response5 != null) {
                System.out.println("Is Success: " + response5.isSuccess);
                System.out.println("Data contains progress: " + (response5.data != null && response5.data.toString().contains("Progress")));
            }
            System.out.println();
            
            // Test 6: Test cancellation
            System.out.println("--- Test 6: Cancellation ---");
            String cancelSessionId = "test-session-cancel-" + System.currentTimeMillis();
            
            // Start contract creation
            manager.processUserInput("create contract", cancelSessionId, userId);
            
            // Cancel contract creation
            ConversationalNLPManager.ChatbotResponse response6 = manager.processUserInput("no", cancelSessionId, userId);
            System.out.println("Step 6 - Cancel contract creation:");
            System.out.println("Response: " + (response6 != null ? "Success" : "Failed"));
            if (response6 != null) {
                System.out.println("Is Success: " + response6.isSuccess);
                System.out.println("Data contains cancelled: " + (response6.data != null && response6.data.toString().contains("Cancelled")));
            }
            System.out.println();
            
            System.out.println("=== Test completed! ===");
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 