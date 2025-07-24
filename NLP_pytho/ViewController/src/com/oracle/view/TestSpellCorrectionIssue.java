package com.oracle.view;

/**
 * Debug test for spell correction and contract number extraction issue
 */
public class TestSpellCorrectionIssue {
    
    public static void main(String[] args) {
        System.out.println("=== SPELL CORRECTION DEBUG TEST ===\n");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        // Test the specific failing case
        testSpellCorrectionCase(engine);
        
        // Test related cases
        testRelatedSpellCases(engine);
    }
    
    private static void testSpellCorrectionCase(AdvancedNLPEngine engine) {
        System.out.println("1. Testing Specific Spell Correction Failing Case:");
        System.out.println("   Input: 'shw contrct 12345'");
        System.out.println("   Expected: contractNumber=12345 in header");
        System.out.println("   -----------------------------------------------");
        
        String testInput = "shw contrct 12345";
        NLPResponse response = engine.processQuery(testInput);
        
        System.out.println("   ACTUAL RESULT:");
        System.out.println("   - Original Input: " + response.getOriginalInput());
        System.out.println("   - Corrected Input: " + response.getCorrectedInput());
        System.out.println("   - Correction Confidence: " + response.getCorrectionConfidence());
        System.out.println("   - Query Type: " + response.getQueryType());
        System.out.println("   - Action Type: " + response.getActionType());
        System.out.println("   - Contract Number: " + response.getHeader().getContractNumber());
        System.out.println("   - Customer Number: " + response.getHeader().getCustomerNumber());
        
        // Validation
        boolean isCorrect = "contracts_by_contractNumber".equals(response.getActionType()) &&
                           "12345".equals(response.getHeader().getContractNumber());
        
                    System.out.println("   - Test Result: " + (isCorrect ? "PASS" : "FAIL"));
        
        if (!isCorrect) {
            System.out.println("\n   DEBUGGING:");
            System.out.println("   - Spell correction is working: " + (response.getCorrectedInput() != null));
            System.out.println("   - Action type is correct: " + "contracts_by_contractNumber".equals(response.getActionType()));
            System.out.println("   - Contract number missing: " + (response.getHeader().getContractNumber() == null));
            System.out.println("   - ISSUE: Contract number extraction is failing after spell correction");
        }
        System.out.println();
    }
    
    private static void testRelatedSpellCases(AdvancedNLPEngine engine) {
        System.out.println("2. Testing Related Spell Correction Cases:");
        System.out.println("   ========================================");
        
        String[] testCases = {
            "shw contrct 12345",           // Original failing case
            "show contract 12345",         // Corrected version (should work)
            "contrct details 67890",       // Another spell error
            "get contrct info 54321",      // Mixed spell errors
            "shwo contract 98765"          // Different spell error
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Original: " + response.getOriginalInput());
            System.out.println("   -> Corrected: " + response.getCorrectedInput());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            
            boolean hasContractNumber = response.getHeader().getContractNumber() != null;
            System.out.println("   -> Contract Extracted: " + (hasContractNumber ? "YES" : "NO"));
            System.out.println();
        }
    }
}