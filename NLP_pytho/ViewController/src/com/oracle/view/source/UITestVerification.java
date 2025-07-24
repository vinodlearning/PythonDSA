package com.oracle.view.source;

import java.util.*;

/**
 * UI Test Verification - Quick verification of UI integration
 * This class helps verify that the Oracle ADF UI integration is working correctly
 */
public class UITestVerification {
    
    private ConversationalNLPManager conversationalNLPManager;
    
    public static void main(String[] args) {
        UITestVerification verifier = new UITestVerification();
        verifier.runVerification();
    }
    
    public UITestVerification() {
        initializeSystem();
    }
    
    /**
     * Initialize the system for testing
     */
    private void initializeSystem() {
        System.out.println("🔧 Initializing UI Test Verification System...");
        
        try {
            this.conversationalNLPManager = new ConversationalNLPManager();
            System.out.println("✅ System initialized successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run comprehensive UI integration verification
     */
    public void runVerification() {
        System.out.println("\n🚀 UI Integration Verification");
        System.out.println("=============================");
        
        // Test 1: Verify basic contract creation
        verifyBasicContractCreation();
        
        // Test 2: Verify contract creation with account
        verifyContractCreationWithAccount();
        
        // Test 3: Verify spell correction
        verifySpellCorrection();
        
        // Test 4: Verify "created by" queries
        verifyCreatedByQueries();
        
        // Test 5: Verify date filtering
        verifyDateFiltering();
        
        // Test 6: Verify multi-turn flow
        verifyMultiTurnFlow();
        
        System.out.println("\n🎉 UI Integration Verification Complete!");
        System.out.println("Your Oracle ADF UI is ready for testing!");
    }
    
    /**
     * Verify basic contract creation
     */
    private void verifyBasicContractCreation() {
        System.out.println("\n📋 Test 1: Basic Contract Creation");
        System.out.println("Input: 'create contract'");
        
        try {
            String input = "create contract";
            String sessionId = "ui-test-1";
            String userId = "ui-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            boolean success = response.isSuccess && 
                             response.data.toString().contains("Account Number") &&
                             response.metadata.actionType.equals("HELP_CONTRACT_CREATE_BOT");
            
            if (success) {
                System.out.println("✅ PASSED: Basic contract creation works correctly");
                System.out.println("   Response: " + response.data.toString().substring(0, Math.min(100, response.data.toString().length())) + "...");
            } else {
                System.out.println("❌ FAILED: Basic contract creation failed");
                System.out.println("   Response: " + response.data);
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in basic contract creation: " + e.getMessage());
        }
    }
    
    /**
     * Verify contract creation with account number
     */
    private void verifyContractCreationWithAccount() {
        System.out.println("\n📋 Test 2: Contract Creation with Account Number");
        System.out.println("Input: 'create contract 123456789'");
        
        try {
            String input = "create contract 123456789";
            String sessionId = "ui-test-2";
            String userId = "ui-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            boolean success = response.isSuccess && 
                             response.data.toString().contains("Contract Name") &&
                             response.data.toString().contains("123456789");
            
            if (success) {
                System.out.println("✅ PASSED: Contract creation with account number works correctly");
                System.out.println("   Response: " + response.data.toString().substring(0, Math.min(100, response.data.toString().length())) + "...");
            } else {
                System.out.println("❌ FAILED: Contract creation with account number failed");
                System.out.println("   Response: " + response.data);
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in contract creation with account: " + e.getMessage());
        }
    }
    
    /**
     * Verify spell correction
     */
    private void verifySpellCorrection() {
        System.out.println("\n📋 Test 3: Spell Correction");
        System.out.println("Input: 'create contarct'");
        
        try {
            String input = "create contarct";
            String sessionId = "ui-test-3";
            String userId = "ui-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            boolean success = response.isSuccess && 
                             response.inputTracking.correctedInput.contains("contract");
            
            if (success) {
                System.out.println("✅ PASSED: Spell correction works correctly");
                System.out.println("   Original: " + response.inputTracking.originalInput);
                System.out.println("   Corrected: " + response.inputTracking.correctedInput);
            } else {
                System.out.println("❌ FAILED: Spell correction failed");
                System.out.println("   Original: " + response.inputTracking.originalInput);
                System.out.println("   Corrected: " + response.inputTracking.correctedInput);
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in spell correction: " + e.getMessage());
        }
    }
    
    /**
     * Verify "created by" queries
     */
    private void verifyCreatedByQueries() {
        System.out.println("\n📋 Test 4: 'Created By' Queries");
        System.out.println("Input: 'contracts created by vinod'");
        
        try {
            String input = "contracts created by vinod";
            String sessionId = "ui-test-4";
            String userId = "ui-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            boolean success = response.isSuccess && 
                             response.metadata.queryType.equals("CONTRACTS") &&
                             response.metadata.actionType.equals("contracts_by_user");
            
            if (success) {
                System.out.println("✅ PASSED: 'Created by' query works correctly");
                System.out.println("   Query Type: " + response.metadata.queryType);
                System.out.println("   Action Type: " + response.metadata.actionType);
            } else {
                System.out.println("❌ FAILED: 'Created by' query failed");
                System.out.println("   Query Type: " + response.metadata.queryType);
                System.out.println("   Action Type: " + response.metadata.actionType);
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in 'created by' query: " + e.getMessage());
        }
    }
    
    /**
     * Verify date filtering
     */
    private void verifyDateFiltering() {
        System.out.println("\n📋 Test 5: Date Filtering");
        System.out.println("Input: 'contracts created by vinod and in 2025'");
        
        try {
            String input = "contracts created by vinod and in 2025";
            String sessionId = "ui-test-5";
            String userId = "ui-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            boolean hasDateFilter = response.entities.stream()
                .anyMatch(e -> e.attribute.equals("CREATE_DATE") && e.operation.equals("IN_YEAR"));
            
            if (response.isSuccess && hasDateFilter) {
                System.out.println("✅ PASSED: Date filtering works correctly");
                System.out.println("   Date filter entities found: " + response.entities.size());
            } else {
                System.out.println("❌ FAILED: Date filtering failed");
                System.out.println("   Entities: " + response.entities);
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in date filtering: " + e.getMessage());
        }
    }
    
    /**
     * Verify multi-turn flow
     */
    private void verifyMultiTurnFlow() {
        System.out.println("\n📋 Test 6: Multi-turn Flow");
        System.out.println("Testing session-based conversation flow");
        
        try {
            String sessionId = "ui-test-6";
            String userId = "ui-user";
            
            // Turn 1: Start contract creation
            System.out.println("   Turn 1: 'create contract'");
            ConversationalNLPManager.ChatbotResponse response1 = 
                conversationalNLPManager.processUserInput("create contract", sessionId, userId);
            
            // Check session state
            boolean sessionWaiting = conversationalNLPManager.isSessionWaitingForUserInput(sessionId);
            
            if (response1.isSuccess && sessionWaiting) {
                System.out.println("✅ PASSED: Multi-turn flow works correctly");
                System.out.println("   Session waiting: " + sessionWaiting);
            } else {
                System.out.println("❌ FAILED: Multi-turn flow failed");
                System.out.println("   Session waiting: " + sessionWaiting);
            }
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: Exception in multi-turn flow: " + e.getMessage());
        }
    }
    
    /**
     * Generate test commands for UI testing
     */
    public void generateUITestCommands() {
        System.out.println("\n📝 UI Test Commands for Oracle ADF Page");
        System.out.println("=====================================");
        System.out.println("Use these commands in your Oracle UI:");
        System.out.println();
        
        System.out.println("1. Basic Contract Creation:");
        System.out.println("   create contract");
        System.out.println();
        
        System.out.println("2. Contract Creation with Account:");
        System.out.println("   create contract 123456789");
        System.out.println();
        
        System.out.println("3. Spell Correction Test:");
        System.out.println("   create contarct");
        System.out.println();
        
        System.out.println("4. 'Created By' Queries:");
        System.out.println("   contracts created by vinod");
        System.out.println("   contracts created by vinod and in 2025");
        System.out.println("   contracts created by vinod and after 2024");
        System.out.println();
        
        System.out.println("5. Other Test Commands:");
        System.out.println("   help");
        System.out.println("   show contract 123456");
        System.out.println("   show parts for contract 123456");
        System.out.println("   show failed parts");
        System.out.println("   show opportunities");
        System.out.println();
        
        System.out.println("🎯 Expected Results:");
        System.out.println("- All commands should work without errors");
        System.out.println("- Responses should appear in the chat interface");
        System.out.println("- Multi-turn conversations should maintain state");
        System.out.println("- Spell correction should work automatically");
    }
} 