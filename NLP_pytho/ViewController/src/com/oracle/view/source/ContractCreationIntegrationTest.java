package com.oracle.view.source;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive Integration Test Suite for Contract Creation
 * Tests all aspects of the contract creation functionality
 */
public class ContractCreationIntegrationTest {
    
    private ConversationalNLPManager conversationalNLPManager;
    private NLPQueryClassifier nlpQueryClassifier;
    private NLPUserActionHandler nlpUserActionHandler;
    
    // Test data
    private static final String VALID_ACCOUNT_NUMBER = "123456789";
    private static final String INVALID_ACCOUNT_NUMBER = "999999999";
    private static final String TEST_SESSION_ID = "test-session-001";
    private static final String TEST_USER_ID = "test-user-001";
    
    public static void main(String[] args) {
        ContractCreationIntegrationTest test = new ContractCreationIntegrationTest();
        test.runAllTests();
    }
    
    public ContractCreationIntegrationTest() {
        initializeComponents();
    }
    
    /**
     * Initialize all components for testing
     */
    private void initializeComponents() {
        System.out.println("🔧 Initializing components...");
        
        try {
            this.nlpQueryClassifier = new NLPQueryClassifier();
            this.nlpUserActionHandler = NLPUserActionHandler.getInstance();
            this.conversationalNLPManager = new ConversationalNLPManager();
            
            System.out.println("✅ Components initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run all integration tests
     */
    public void runAllTests() {
        System.out.println("\n🚀 Starting Contract Creation Integration Tests");
        System.out.println("============================================================");
        
        int totalTests = 0;
        int passedTests = 0;
        
        // Test 1: Basic Contract Creation
        totalTests++;
        if (testBasicContractCreation()) passedTests++;
        
        // Test 2: Contract Creation with Account Number
        totalTests++;
        if (testContractCreationWithAccount()) passedTests++;
        
        // Test 3: Spell Correction
        totalTests++;
        if (testSpellCorrection()) passedTests++;
        
        // Test 4: "Created By" Queries
        totalTests++;
        if (testCreatedByQuery()) passedTests++;
        
        // Test 5: Date Filtered Queries
        totalTests++;
        if (testDateFilteredQuery()) passedTests++;
        
        // Test 6: Multi-turn Data Collection
        totalTests++;
        if (testMultiTurnDataCollection()) passedTests++;
        
        // Test 7: Account Number Validation
        totalTests++;
        if (testAccountNumberValidation()) passedTests++;
        
        // Test 8: Complete Contract Creation Flow
        totalTests++;
        if (testCompleteContractCreationFlow()) passedTests++;
        
        // Test 9: Error Handling
        totalTests++;
        if (testErrorHandling()) passedTests++;
        
        // Test 10: Session Management
        totalTests++;
        if (testSessionManagement()) passedTests++;
        
        // Print results
        System.out.println("\n============================================================");
        System.out.println("📊 TEST RESULTS SUMMARY");
        System.out.println("============================================================");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.1f%%", (double) passedTests / totalTests * 100));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 ALL TESTS PASSED! Integration is successful!");
        } else {
            System.out.println("\n⚠️ Some tests failed. Please review the errors above.");
        }
    }
    
    /**
     * Test 1: Basic Contract Creation
     */
    private boolean testBasicContractCreation() {
        System.out.println("\n🧪 Test 1: Basic Contract Creation");
        System.out.println("Input: 'create contract'");
        
        try {
            String input = "create contract";
            String sessionId = TEST_SESSION_ID + "-1";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, TEST_USER_ID);
            
            boolean success = response.isSuccess && 
                             response.data.toString().contains("Account Number") &&
                             response.metadata.actionType.equals("HELP_CONTRACT_CREATE_BOT");
            
            if (success) {
                System.out.println("✅ PASSED: Basic contract creation works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Basic contract creation failed");
                System.out.println("Response: " + response.data);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in basic contract creation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 2: Contract Creation with Account Number
     */
    private boolean testContractCreationWithAccount() {
        System.out.println("\n🧪 Test 2: Contract Creation with Account Number");
        System.out.println("Input: 'create contract 123456789'");
        
        try {
            String input = "create contract " + VALID_ACCOUNT_NUMBER;
            String sessionId = TEST_SESSION_ID + "-2";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, TEST_USER_ID);
            
            boolean success = response.isSuccess && 
                             response.data.toString().contains("Contract Name") &&
                             response.data.toString().contains(VALID_ACCOUNT_NUMBER);
            
            if (success) {
                System.out.println("✅ PASSED: Contract creation with account number works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Contract creation with account number failed");
                System.out.println("Response: " + response.data);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in contract creation with account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 3: Spell Correction
     */
    private boolean testSpellCorrection() {
        System.out.println("\n🧪 Test 3: Spell Correction");
        System.out.println("Input: 'create contarct'");
        
        try {
            String input = "create contarct";
            String sessionId = TEST_SESSION_ID + "-3";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, TEST_USER_ID);
            
            boolean success = response.isSuccess && 
                             response.inputTracking.correctedInput.contains("contract");
            
            if (success) {
                System.out.println("✅ PASSED: Spell correction works correctly");
                System.out.println("Corrected: " + response.inputTracking.correctedInput);
                return true;
            } else {
                System.out.println("❌ FAILED: Spell correction failed");
                System.out.println("Original: " + response.inputTracking.originalInput);
                System.out.println("Corrected: " + response.inputTracking.correctedInput);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in spell correction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 4: "Created By" Queries
     */
    private boolean testCreatedByQuery() {
        System.out.println("\n🧪 Test 4: 'Created By' Queries");
        System.out.println("Input: 'contracts created by vinod'");
        
        try {
            String input = "contracts created by vinod";
            String sessionId = TEST_SESSION_ID + "-4";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, TEST_USER_ID);
            
            boolean success = response.isSuccess && 
                             response.metadata.queryType.equals("CONTRACTS") &&
                             response.metadata.actionType.equals("contracts_by_user");
            
            if (success) {
                System.out.println("✅ PASSED: 'Created by' query works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: 'Created by' query failed");
                System.out.println("Query Type: " + response.metadata.queryType);
                System.out.println("Action Type: " + response.metadata.actionType);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in 'created by' query: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 5: Date Filtered Queries
     */
    private boolean testDateFilteredQuery() {
        System.out.println("\n🧪 Test 5: Date Filtered Queries");
        System.out.println("Input: 'contracts created by vinod and in 2025'");
        
        try {
            String input = "contracts created by vinod and in 2025";
            String sessionId = TEST_SESSION_ID + "-5";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, TEST_USER_ID);
            
            boolean hasDateFilter = response.entities.stream()
                .anyMatch(e -> e.attribute.equals("CREATE_DATE") && e.operation.equals("IN_YEAR"));
            
            if (response.isSuccess && hasDateFilter) {
                System.out.println("✅ PASSED: Date filtered query works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Date filtered query failed");
                System.out.println("Entities: " + response.entities);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in date filtered query: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 6: Multi-turn Data Collection
     */
    private boolean testMultiTurnDataCollection() {
        System.out.println("\n🧪 Test 6: Multi-turn Data Collection");
        System.out.println("Testing session-based data collection");
        
        try {
            String sessionId = TEST_SESSION_ID + "-6";
            
            // First turn: Start contract creation
            ConversationalNLPManager.ChatbotResponse response1 = 
                conversationalNLPManager.processUserInput("create contract", sessionId, TEST_USER_ID);
            
            // Check if session is waiting for input
            boolean sessionWaiting = conversationalNLPManager.isSessionWaitingForUserInput(sessionId);
            
            if (response1.isSuccess && sessionWaiting) {
                System.out.println("✅ PASSED: Multi-turn data collection works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Multi-turn data collection failed");
                System.out.println("Session waiting: " + sessionWaiting);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in multi-turn data collection: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 7: Account Number Validation
     */
    private boolean testAccountNumberValidation() {
        System.out.println("\n🧪 Test 7: Account Number Validation");
        System.out.println("Testing account number extraction and validation");
        
        try {
            // Test account number extraction
            String input = "create contract " + VALID_ACCOUNT_NUMBER;
            String sessionId = TEST_SESSION_ID + "-7";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, TEST_USER_ID);
            
            // Check if account number was extracted
            boolean accountExtracted = response.data.toString().contains(VALID_ACCOUNT_NUMBER);
            
            if (response.isSuccess && accountExtracted) {
                System.out.println("✅ PASSED: Account number validation works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Account number validation failed");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in account number validation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 8: Complete Contract Creation Flow
     */
    private boolean testCompleteContractCreationFlow() {
        System.out.println("\n🧪 Test 8: Complete Contract Creation Flow");
        System.out.println("Testing end-to-end contract creation");
        
        try {
            String sessionId = TEST_SESSION_ID + "-8";
            
            // Simulate complete contract creation input
            String completeInput = VALID_ACCOUNT_NUMBER + ", testcontract, testtitle, testdesc, nocomments, no";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(completeInput, sessionId, TEST_USER_ID);
            
            boolean success = response.isSuccess && 
                             response.data.toString().contains("Contract Created Successfully");
            
            if (success) {
                System.out.println("✅ PASSED: Complete contract creation flow works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Complete contract creation flow failed");
                System.out.println("Response: " + response.data);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in complete contract creation flow: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 9: Error Handling
     */
    private boolean testErrorHandling() {
        System.out.println("\n🧪 Test 9: Error Handling");
        System.out.println("Testing error scenarios");
        
        try {
            String sessionId = TEST_SESSION_ID + "-9";
            
            // Test with invalid input
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput("", sessionId, TEST_USER_ID);
            
            // Should handle empty input gracefully
            if (response != null) {
                System.out.println("✅ PASSED: Error handling works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Error handling failed");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in error handling: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Test 10: Session Management
     */
    private boolean testSessionManagement() {
        System.out.println("\n🧪 Test 10: Session Management");
        System.out.println("Testing session creation and management");
        
        try {
            String sessionId = TEST_SESSION_ID + "-10";
            
            // Test session creation
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput("create contract", sessionId, TEST_USER_ID);
            
            // Test session state
            boolean sessionWaiting = conversationalNLPManager.isSessionWaitingForUserInput(sessionId);
            
            if (response.isSuccess && sessionWaiting) {
                System.out.println("✅ PASSED: Session management works correctly");
                return true;
            } else {
                System.out.println("❌ FAILED: Session management failed");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in session management: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Utility method to print test results
     */
    private void printTestResult(String testName, boolean passed) {
        if (passed) {
            System.out.println("✅ " + testName + ": PASSED");
        } else {
            System.out.println("❌ " + testName + ": FAILED");
        }
    }
} 