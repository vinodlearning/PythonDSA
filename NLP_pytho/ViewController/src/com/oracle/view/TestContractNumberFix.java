package com.oracle.view;

/**
 * Specific test to validate the contract number identification fix
 * Tests the exact failing case: "get contract info 123456"
 */
public class TestContractNumberFix {
    
    public static void main(String[] args) {
        TestContractNumberFix tester = new TestContractNumberFix();
        
        System.out.println("=== Contract Number Identification Fix Test ===\n");
        
        // Test the specific failing case
        tester.testSpecificFailingCase();
        
        // Test various contract number scenarios
        tester.testContractNumberScenarios();
        
        // Test edge cases
        tester.testEdgeCases();
        
        System.out.println("\n=== Test Complete ===");
    }
    
    /**
     * Test the specific failing case: "get contract info 123456"
     */
    private void testSpecificFailingCase() {
        System.out.println("1. Testing Specific Failing Case:");
        System.out.println("   Input: 'get contract info 123456'");
        System.out.println("   Expected: actionType=contracts_by_contractNumber");
        System.out.println("   ------------------------------------------------");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        String testInput = "get contract info 123456";
        
        NLPResponse response = engine.processQuery(testInput);
        
        System.out.println("   RESULT:");
        System.out.println("   - Query Type: " + response.getQueryType());
        System.out.println("   - Action Type: " + response.getActionType());
        System.out.println("   - Contract Number: " + response.getHeader().getContractNumber());
        System.out.println("   - Customer Number: " + response.getHeader().getCustomerNumber());
        System.out.println("   - Selected Module: " + response.getSelectedModule());
        System.out.println("   - Routing Confidence: " + String.format("%.2f", response.getRoutingConfidence()));
        
        // Validation
        boolean isCorrect = response.getHeader().getContractNumber() != null &&
                           response.getHeader().getContractNumber().equals("123456") &&
                           response.getActionType().equals("contracts_by_contractNumber") &&
                           response.getHeader().getCustomerNumber() == null;
        
        System.out.println("   - Test Result: " + (isCorrect ? "PASS" : "FAIL"));
        System.out.println();
    }
    
    /**
     * Test various contract number scenarios
     */
    private void testContractNumberScenarios() {
        System.out.println("2. Testing Contract Number Scenarios:");
        System.out.println("   ====================================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "get contract info 123456",
            "show contract 789012",
            "contract details 456789",
            "display contract number 987654",
            "find contract 111222",
            "contract 333444 status",
            "what is contract 555666",
            "contract ABC-123456",
            "get contract CON-789012"
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Query Type: " + response.getQueryType());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            System.out.println("   -> Customer Number: " + response.getHeader().getCustomerNumber());
            
            // Validation
            boolean isCorrect = response.getActionType().equals("contracts_by_contractNumber") &&
                               response.getHeader().getContractNumber() != null;
            System.out.println("   -> Test Result: " + (isCorrect ? "PASS" : "FAIL"));
            System.out.println();
        }
    }
    
    /**
     * Test edge cases to ensure we don't break existing functionality
     */
    private void testEdgeCases() {
        System.out.println("3. Testing Edge Cases:");
        System.out.println("   ====================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "contracts for customer number 897654",  // Should still work as customer
            "show parts for contract 123456",        // Should be parts with contract
            "contracts created in 2023",             // Should be dates, not contract number
            "contract with year 2024",               // Year should not be contract number
            "get contract info",                      // No number provided
            "123456",                                 // Number only, should default to contract
            "contract summary for 789012"            // Contract with number
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Query Type: " + response.getQueryType());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            System.out.println("   -> Customer Number: " + response.getHeader().getCustomerNumber());
            System.out.println("   -> Module: " + response.getSelectedModule());
            System.out.println();
        }
    }
    
    /**
     * Test comparison with StandardJSONProcessor logic
     */
    private void testComparisonWithStandardProcessor() {
        System.out.println("4. Testing Comparison with Standard Logic:");
        System.out.println("   =========================================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        // Test cases that should follow StandardJSONProcessor priority
        String[] testCases = {
            "get contract info 123456",              // Contract number priority
            "show part AE125 for contract 789012",   // Part number priority  
            "contracts for customer 897654",         // Customer number priority
            "contracts by admin",                     // Creator priority
            "contracts in 2023"                      // Date priority
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            System.out.println("   -> Part Number: " + response.getHeader().getPartNumber());
            System.out.println("   -> Customer Number: " + response.getHeader().getCustomerNumber());
            System.out.println("   -> Created By: " + response.getHeader().getCreatedBy());
            
            // Validate priority logic
            String expectedAction = "";
            if (response.getHeader().getContractNumber() != null) {
                expectedAction = "contracts_by_contractNumber";
            } else if (response.getHeader().getPartNumber() != null) {
                expectedAction = "parts_by_partNumber";
            } else if (response.getHeader().getCustomerNumber() != null) {
                expectedAction = "contracts_by_customerNumber";
            } else if (response.getHeader().getCreatedBy() != null) {
                expectedAction = "contracts_by_createdBy";
            }
            
            boolean priorityCorrect = expectedAction.isEmpty() || 
                                     response.getActionType().equals(expectedAction);
            System.out.println("   -> Priority Logic: " + (priorityCorrect ? "CORRECT" : "INCORRECT"));
            System.out.println();
        }
    }
}