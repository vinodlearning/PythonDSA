package com.oracle.view;

/**
 * Test the specific account number case that was failing
 */
public class TestAccountNumberFix {
    
    public static void main(String[] args) {
        System.out.println("=== ACCOUNT NUMBER FIX TEST ===\n");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        // Test the specific failing case
        testAccountNumberCase(engine);
        
        // Test related cases
        testRelatedCases(engine);
    }
    
    private static void testAccountNumberCase(AdvancedNLPEngine engine) {
        System.out.println("1. Testing Specific Failing Case:");
        System.out.println("   Input: 'account 10840607 contracts'");
        System.out.println("   Expected: actionType=contracts_by_customerNumber");
        System.out.println("   Expected: customerNumber=10840607, contractNumber=null");
        System.out.println("   -------------------------------------------------------");
        
        String testInput = "account 10840607 contracts";
        NLPResponse response = engine.processQuery(testInput);
        
        System.out.println("   ACTUAL RESULT:");
        System.out.println("   - Query Type: " + response.getQueryType());
        System.out.println("   - Action Type: " + response.getActionType());
        System.out.println("   - Contract Number: " + response.getHeader().getContractNumber());
        System.out.println("   - Customer Number: " + response.getHeader().getCustomerNumber());
        System.out.println("   - Customer Name: " + response.getHeader().getCustomerName());
        
        // Validation
        boolean isCorrect = "CONTRACTS".equals(response.getQueryType()) &&
                           "contracts_by_customerNumber".equals(response.getActionType()) &&
                           "10840607".equals(response.getHeader().getCustomerNumber()) &&
                           response.getHeader().getContractNumber() == null;
        
                    System.out.println("   - Test Result: " + (isCorrect ? "PASS" : "FAIL"));
        
        if (!isCorrect) {
            System.out.println("   - ISSUE: Should identify 10840607 as customer number, not contract number");
        }
        System.out.println();
    }
    
    private static void testRelatedCases(AdvancedNLPEngine engine) {
        System.out.println("2. Testing Related Account/Customer Cases:");
        System.out.println("   =========================================");
        
        String[] testCases = {
            "account 10840607 contracts",
            "customer 12345678 contracts", 
            "account number 98765432",
            "customer number 11223344",
            "contracts for account 55667788",
            "show contracts for customer 99887766"
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            System.out.println("   -> Customer Number: " + response.getHeader().getCustomerNumber());
            
            // Should all be customer number queries
            boolean isCustomerQuery = "contracts_by_customerNumber".equals(response.getActionType()) &&
                                     response.getHeader().getCustomerNumber() != null &&
                                     response.getHeader().getContractNumber() == null;
            
            System.out.println("   -> Result: " + (isCustomerQuery ? "CORRECT (Customer)" : "INCORRECT"));
            System.out.println();
        }
    }
}