package com.oracle.view;

import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive test class for Advanced NLP Engine
 * Tests the specific scenario where customer numbers are properly distinguished from contract numbers
 */
public class TestAdvancedNLPEngine {
    
    public static void main(String[] args) {
        TestAdvancedNLPEngine tester = new TestAdvancedNLPEngine();
        
        System.out.println("=== Advanced NLP Engine Test Suite ===\n");
        
        // Test the specific failing case
        tester.testSpecificFailingCase();
        
        // Test various customer vs contract scenarios
        tester.testCustomerVsContractScenarios();
        
        // Test parts with contract scenarios
        tester.testPartsWithContractScenarios();
        
        // Test help scenarios
        tester.testHelpScenarios();
        
        // Test complex scenarios
        tester.testComplexScenarios();
        
        System.out.println("\n=== Test Suite Complete ===");
    }
    
    /**
     * Test the specific failing case: "contracts for customer number 897654"
     */
    private void testSpecificFailingCase() {
        System.out.println("1. Testing Specific Failing Case:");
        System.out.println("   Input: 'contracts for customer number 897654'");
        System.out.println("   Expected: customerNumber=897654, actionType=contracts_by_customerNumber");
        System.out.println("   ------------------------------------------------");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        String testInput = "contracts for customer number 897654";
        
        NLPResponse response = engine.processQuery(testInput);
        
        System.out.println("   RESULT:");
        System.out.println("   - Query Type: " + response.getQueryType());
        System.out.println("   - Action Type: " + response.getActionType());
        System.out.println("   - Contract Number: " + response.getHeader().getContractNumber());
        System.out.println("   - Customer Number: " + response.getHeader().getCustomerNumber());
        System.out.println("   - Customer Name: " + response.getHeader().getCustomerName());
        System.out.println("   - Selected Module: " + response.getSelectedModule());
        System.out.println("   - Routing Confidence: " + String.format("%.2f", response.getRoutingConfidence()));
        
        // Validation
        boolean isCorrect = response.getHeader().getCustomerNumber() != null &&
                           response.getHeader().getCustomerNumber().equals("897654") &&
                           response.getActionType().equals("contracts_by_customerNumber") &&
                           response.getHeader().getContractNumber() == null;
        
        System.out.println("   - Test Result: " + (isCorrect ? "PASS" : "FAIL"));
        System.out.println();
    }
    
    /**
     * Test various customer vs contract number scenarios
     */
    private void testCustomerVsContractScenarios() {
        System.out.println("2. Testing Customer vs Contract Number Scenarios:");
        System.out.println("   ================================================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "contracts for customer number 897654",
            "show contracts for account 123456",
            "list all contracts for customer 567890",
            "display contract number 123456",
            "show contract 789012",
            "get contract ABC-123456",
            "contracts created by customer 445566",
            "find contracts for account number 998877"
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
     * Test parts with contract scenarios
     */
    private void testPartsWithContractScenarios() {
        System.out.println("3. Testing Parts with Contract Scenarios:");
        System.out.println("   ========================================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "show parts for contract 123456",
            "list all parts for contract ABC-789",
            "parts validation failed for contract 445566",
            "get parts information for contract number 778899",
            "parts count for contract 556677",
            "show part ABC123 for contract 998877"
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Query Type: " + response.getQueryType());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            System.out.println("   -> Part Number: " + response.getHeader().getPartNumber());
            System.out.println("   -> Module: " + response.getSelectedModule());
            System.out.println();
        }
    }
    
    /**
     * Test help scenarios
     */
    private void testHelpScenarios() {
        System.out.println("4. Testing Help Scenarios:");
        System.out.println("   ==========================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "how to create contract",
            "help me create a contract",
            "steps to create contract",
            "guide for contract creation",
            "how to create part",
            "steps for part creation",
            "help with contract creation process"
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Query Type: " + response.getQueryType());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Module: " + response.getSelectedModule());
            System.out.println();
        }
    }
    
    /**
     * Test complex scenarios with typos and mixed contexts
     */
    private void testComplexScenarios() {
        System.out.println("5. Testing Complex Scenarios:");
        System.out.println("   =============================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "shw contrcts for custmer number 897654",  // With typos
            "list all partz for contract 123456",      // Parts with typos
            "contrcts created by vinod after 2023",    // With creator and date
            "help me creat contract",                   // Help with typos
            "what is the expiration date for contract 123456", // Default contract query
            "parts validation error for customer 445566",      // Parts without contract
            "show contracts for customer siemens",             // Customer name
            "contracts created by admin in 2024"               // Creator with year
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Query Type: " + response.getQueryType());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Contract Number: " + response.getHeader().getContractNumber());
            System.out.println("   -> Customer Number: " + response.getHeader().getCustomerNumber());
            System.out.println("   -> Customer Name: " + response.getHeader().getCustomerName());
            System.out.println("   -> Created By: " + response.getHeader().getCreatedBy());
            System.out.println("   -> Module: " + response.getSelectedModule());
            System.out.println("   -> Corrected Input: " + response.getCorrectedInput());
            System.out.println("   -> Correction Confidence: " + String.format("%.2f", response.getCorrectionConfidence()));
            System.out.println();
        }
    }
    
    /**
     * Test edge cases and boundary conditions
     */
    private void testEdgeCases() {
        System.out.println("6. Testing Edge Cases:");
        System.out.println("   ====================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testCases = {
            "",                                         // Empty input
            "   ",                                      // Whitespace only
            "123456",                                   // Number only
            "contract",                                 // Keyword only
            "customer number",                          // Incomplete phrase
            "show contracts for customer number",       // Missing number
            "contracts for customer 12",               // Short number
            "parts for contract customer 123456",      // Mixed context
            "create parts for contract 123456"         // Create parts (should show error)
        };
        
        for (String testCase : testCases) {
            System.out.println("   Input: '" + testCase + "'");
            NLPResponse response = engine.processQuery(testCase);
            
            System.out.println("   -> Query Type: " + response.getQueryType());
            System.out.println("   -> Action Type: " + response.getActionType());
            System.out.println("   -> Module: " + response.getSelectedModule());
            System.out.println("   -> Errors: " + response.getErrors());
            System.out.println();
        }
    }
    
    /**
     * Performance test with multiple queries
     */
    private void performanceTest() {
        System.out.println("7. Performance Test:");
        System.out.println("   ==================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        
        String[] testInputs = {
            "contracts for customer number 897654",
            "show parts for contract 123456",
            "help me create contract",
            "contracts created by vinod",
            "parts validation failed for contract ABC-123"
        };
        
        int iterations = 100;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            for (String input : testInputs) {
                engine.processQuery(input);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / (iterations * testInputs.length);
        
        System.out.println("   Total queries processed: " + (iterations * testInputs.length));
        System.out.println("   Total time: " + totalTime + " ms");
        System.out.println("   Average time per query: " + String.format("%.2f", avgTime) + " ms");
        System.out.println("   Queries per second: " + String.format("%.0f", 1000.0 / avgTime));
        System.out.println();
    }
    
    /**
     * Test JSON response format validation
     */
    private void testJSONResponseFormat() {
        System.out.println("8. Testing JSON Response Format:");
        System.out.println("   ===============================");
        
        AdvancedNLPEngine engine = new AdvancedNLPEngine();
        String testInput = "contracts for customer number 897654";
        
        NLPResponse response = engine.processQuery(testInput);
        
        System.out.println("   Sample JSON Response Structure:");
        System.out.println("   {");
        System.out.println("     \"header\": {");
        System.out.println("       \"contractNumber\": \"" + response.getHeader().getContractNumber() + "\",");
        System.out.println("       \"customerNumber\": \"" + response.getHeader().getCustomerNumber() + "\",");
        System.out.println("       \"customerName\": \"" + response.getHeader().getCustomerName() + "\",");
        System.out.println("       \"inputTracking\": {");
        System.out.println("         \"originalInput\": \"" + response.getOriginalInput() + "\",");
        System.out.println("         \"correctedInput\": \"" + response.getCorrectedInput() + "\",");
        System.out.println("         \"correctionConfidence\": " + response.getCorrectionConfidence());
        System.out.println("       }");
        System.out.println("     },");
        System.out.println("     \"queryMetadata\": {");
        System.out.println("       \"queryType\": \"" + response.getQueryType() + "\",");
        System.out.println("       \"actionType\": \"" + response.getActionType() + "\",");
        System.out.println("       \"selectedModule\": \"" + response.getSelectedModule() + "\",");
        System.out.println("       \"routingConfidence\": " + String.format("%.2f", response.getRoutingConfidence()));
        System.out.println("     },");
        System.out.println("     \"displayEntities\": " + response.getDisplayEntities() + ",");
        System.out.println("     \"errors\": " + response.getErrors() + ",");
        System.out.println("     \"confidence\": " + String.format("%.2f", response.getConfidence()));
        System.out.println("   }");
        System.out.println();
    }
}