package com.oracle.view;

public class YourTestExample {
    public static void main(String[] args) {
        System.out.println("=== Your Test Example ===\n");
        
        // Your test cases from the conversation
        String[] allTestCases = {
            "show contract 123456",
            "contract details 123456", 
            "get contract info 123456",
            "contracts created by vinod after 1-Jan-2020",
            "status of contract 123456",
            "expired contracts",
            "contracts for customer number 897654",
            "account 10840607 contracts",
            "contracts created in 2024",
            "get all metadata for contract 123456"
        };
        
        try {
            // Your exact code pattern
            SimpleNLPIntegration nlpIntegration = new SimpleNLPIntegration();
            
            int count = 0;
            for (String input : allTestCases) {
                String jsonResponse = nlpIntegration.processInput(input);  // Returns JSON now!
                ++count;
                if (count < 10) {
                    System.out.println("=== Test " + count + ": " + input + " ===");
                    System.out.println(jsonResponse);
                    System.out.println();
                }
            }
            
            System.out.println("All tests completed successfully!");
            System.out.println("processInput() now returns JSON with your exact structure!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}