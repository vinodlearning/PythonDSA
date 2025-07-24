package com.oracle.view.source;

import java.util.*;

/**
 * Integration Runner - Demonstrates the integrated contract creation system
 * This class shows how to use the ConversationalNLPManager for contract creation
 */
public class IntegrationRunner {
    
    private ConversationalNLPManager conversationalNLPManager;
    
    public static void main(String[] args) {
        IntegrationRunner runner = new IntegrationRunner();
        runner.runIntegrationDemo();
    }
    
    public IntegrationRunner() {
        initializeSystem();
    }
    
    /**
     * Initialize the integrated system
     */
    private void initializeSystem() {
        System.out.println("🔧 Initializing Contract Creation Integration System...");
        
        try {
            this.conversationalNLPManager = new ConversationalNLPManager();
            System.out.println("✅ System initialized successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run integration demonstration
     */
    public void runIntegrationDemo() {
        System.out.println("\n🚀 Contract Creation Integration Demo");
        System.out.println("=====================================");
        
        // Demo 1: Basic Contract Creation
        demoBasicContractCreation();
        
        // Demo 2: Contract Creation with Account Number
        demoContractCreationWithAccount();
        
        // Demo 3: Spell Correction
        demoSpellCorrection();
        
        // Demo 4: "Created By" Queries
        demoCreatedByQueries();
        
        // Demo 5: Date Filtered Queries
        demoDateFilteredQueries();
        
        // Demo 6: Multi-turn Data Collection
        demoMultiTurnDataCollection();
        
        System.out.println("\n🎉 Integration Demo Complete!");
    }
    
    /**
     * Demo 1: Basic Contract Creation
     */
    private void demoBasicContractCreation() {
        System.out.println("\n📋 Demo 1: Basic Contract Creation");
        System.out.println("Input: 'create contract'");
        
        try {
            String input = "create contract";
            String sessionId = "demo-session-1";
            String userId = "demo-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            System.out.println("✅ Response: " + response.data);
            System.out.println("Query Type: " + response.metadata.queryType);
            System.out.println("Action Type: " + response.metadata.actionType);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo 2: Contract Creation with Account Number
     */
    private void demoContractCreationWithAccount() {
        System.out.println("\n📋 Demo 2: Contract Creation with Account Number");
        System.out.println("Input: 'create contract 123456789'");
        
        try {
            String input = "create contract 123456789";
            String sessionId = "demo-session-2";
            String userId = "demo-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            System.out.println("✅ Response: " + response.data);
            System.out.println("Query Type: " + response.metadata.queryType);
            System.out.println("Action Type: " + response.metadata.actionType);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo 3: Spell Correction
     */
    private void demoSpellCorrection() {
        System.out.println("\n📋 Demo 3: Spell Correction");
        System.out.println("Input: 'create contarct'");
        
        try {
            String input = "create contarct";
            String sessionId = "demo-session-3";
            String userId = "demo-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            System.out.println("✅ Original Input: " + response.inputTracking.originalInput);
            System.out.println("✅ Corrected Input: " + response.inputTracking.correctedInput);
            System.out.println("✅ Correction Confidence: " + response.inputTracking.correctionConfidence);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo 4: "Created By" Queries
     */
    private void demoCreatedByQueries() {
        System.out.println("\n📋 Demo 4: 'Created By' Queries");
        System.out.println("Input: 'contracts created by vinod'");
        
        try {
            String input = "contracts created by vinod";
            String sessionId = "demo-session-4";
            String userId = "demo-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            System.out.println("✅ Response: " + response.data);
            System.out.println("Query Type: " + response.metadata.queryType);
            System.out.println("Action Type: " + response.metadata.actionType);
            
            // Show entities
            if (response.entities != null && !response.entities.isEmpty()) {
                System.out.println("✅ Entities found:");
                for (ConversationalNLPManager.EntityFilter entity : response.entities) {
                    System.out.println("   - " + entity.attribute + " " + entity.operation + " " + entity.value);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo 5: Date Filtered Queries
     */
    private void demoDateFilteredQueries() {
        System.out.println("\n📋 Demo 5: Date Filtered Queries");
        System.out.println("Input: 'contracts created by vinod and in 2025'");
        
        try {
            String input = "contracts created by vinod and in 2025";
            String sessionId = "demo-session-5";
            String userId = "demo-user";
            
            ConversationalNLPManager.ChatbotResponse response = 
                conversationalNLPManager.processUserInput(input, sessionId, userId);
            
            System.out.println("✅ Response: " + response.data);
            System.out.println("Query Type: " + response.metadata.queryType);
            System.out.println("Action Type: " + response.metadata.actionType);
            
            // Show date filter entities
            if (response.entities != null && !response.entities.isEmpty()) {
                System.out.println("✅ Date Filter Entities:");
                for (ConversationalNLPManager.EntityFilter entity : response.entities) {
                    if (entity.attribute.equals("CREATE_DATE")) {
                        System.out.println("   - Date Filter: " + entity.operation + " " + entity.value);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo 6: Multi-turn Data Collection
     */
    private void demoMultiTurnDataCollection() {
        System.out.println("\n📋 Demo 6: Multi-turn Data Collection");
        System.out.println("Simulating a complete contract creation conversation");
        
        try {
            String sessionId = "demo-session-6";
            String userId = "demo-user";
            
            // Turn 1: Start contract creation
            System.out.println("\n🔄 Turn 1: User says 'create contract'");
            ConversationalNLPManager.ChatbotResponse response1 = 
                conversationalNLPManager.processUserInput("create contract", sessionId, userId);
            System.out.println("Bot: " + response1.data);
            
            // Check session state
            boolean sessionWaiting = conversationalNLPManager.isSessionWaitingForUserInput(sessionId);
            System.out.println("Session waiting for input: " + sessionWaiting);
            
            // Turn 2: User provides account number
            System.out.println("\n🔄 Turn 2: User says '123456789'");
            ConversationalNLPManager.ChatbotResponse response2 = 
                conversationalNLPManager.processUserInput("123456789", sessionId, userId);
            System.out.println("Bot: " + response2.data);
            
            // Turn 3: User provides contract details
            System.out.println("\n🔄 Turn 3: User says 'testcontract, testtitle, testdesc, nocomments, no'");
            ConversationalNLPManager.ChatbotResponse response3 = 
                conversationalNLPManager.processUserInput("testcontract, testtitle, testdesc, nocomments, no", sessionId, userId);
            System.out.println("Bot: " + response3.data);
            
            System.out.println("✅ Multi-turn conversation completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Utility method to format response for display
     */
    private String formatResponse(ConversationalNLPManager.ChatbotResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Success: ").append(response.isSuccess).append("\n");
        sb.append("Data: ").append(response.data).append("\n");
        sb.append("Query Type: ").append(response.metadata.queryType).append("\n");
        sb.append("Action Type: ").append(response.metadata.actionType).append("\n");
        return sb.toString();
    }
} 