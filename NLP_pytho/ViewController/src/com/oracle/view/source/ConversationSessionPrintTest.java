package com.oracle.view.source;

/**
 * Test to verify ConversationSession print methods
 */
public class ConversationSessionPrintTest {
    
    public static void main(String[] args) {
        System.out.println("=== ConversationSession Print Methods Test ===\n");
        
        try {
            // Test 1: Print null session
            System.out.println("--- Test 1: Print null session ---");
            ConversationSessionDebugger.printConversationSession(null);
            System.out.println();
            
            // Test 2: Create and print a new session
            System.out.println("--- Test 2: Print new session ---");
            ConversationSession session = new ConversationSession("test-session-123", "test-user");
            ConversationSessionDebugger.printConversationSessionCompact(session);
            System.out.println();
            
            // Test 3: Start contract creation flow and print
            System.out.println("--- Test 3: Print contract creation session ---");
            session.startContractCreationFlow("123456789");
            ConversationSessionDebugger.printContractCreationSession(session);
            System.out.println();
            
            // Test 4: Add some data and print
            System.out.println("--- Test 4: Print session with data ---");
            session.addUserInput("create contract for me");
            session.addBotResponse("Please provide account number");
            session.addUserInput("123456789");
            
            // Simulate data collection
            session.processUserInput("123456789, TestContract, TestTitle, TestDescription, NoComments, No");
            ConversationSessionDebugger.printConversationSession(session);
            System.out.println();
            
            // Test 5: Print session summary
            System.out.println("--- Test 5: Print session summary ---");
            ConversationSessionDebugger.printSessionSummary(session);
            System.out.println();
            
            System.out.println("=== All tests completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 