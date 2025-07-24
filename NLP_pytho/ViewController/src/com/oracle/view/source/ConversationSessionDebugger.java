package com.oracle.view.source;

import java.util.*;

/**
 * Utility class for debugging ConversationSession objects
 * This class provides methods to print ConversationSession details without ADF dependencies
 */
public class ConversationSessionDebugger {
    
    /**
     * Print complete ConversationSession object details
     * @param session The ConversationSession object to print
     */
    public static void printConversationSession(ConversationSession session) {
        if (session == null) {
            System.out.println("=== CONVERSATION SESSION DEBUG ===");
            System.out.println("Session is NULL");
            System.out.println("=== END CONVERSATION SESSION DEBUG ===");
            return;
        }
        
        System.out.println("=== CONVERSATION SESSION DEBUG ===");
        System.out.println("Session ID: " + session.getSessionId());
        System.out.println("User ID: " + session.getUserId());
        System.out.println("Creation Time: " + new java.util.Date(session.getCreationTime()));
        System.out.println("Last Activity: " + new java.util.Date(session.getLastActivityTime()));
        System.out.println("Session State: " + session.getState());
        System.out.println("Current Flow Type: " + session.getCurrentFlowType());
        System.out.println("Is Waiting For Input: " + session.isWaitingForUserInput());
        System.out.println("Is Completed: " + session.isCompleted());
        
        // Print collected data
        System.out.println("\nCOLLECTED DATA:");
        Map<String, Object> collectedData = session.getCollectedData();
        if (collectedData.isEmpty()) {
            System.out.println("   (No data collected)");
        } else {
            for (Map.Entry<String, Object> entry : collectedData.entrySet()) {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // Print expected fields
        System.out.println("\nEXPECTED FIELDS:");
        List<String> remainingFields = session.getRemainingFields();
        if (remainingFields.isEmpty()) {
            System.out.println("   (All fields completed)");
        } else {
            for (String field : remainingFields) {
                System.out.println("   - " + field);
            }
        }
        
        // Print user search results
        System.out.println("\nUSER SEARCH RESULTS:");
        Map<String, List<Map<String, String>>> userResults = session.getUserSearchResults();
        if (userResults.isEmpty()) {
            System.out.println("   (No user search results)");
        } else {
            System.out.println("   Valid: " + session.isUserSearchValid());
            System.out.println("   Available Users: " + session.getAvailableUsers());
            for (Map.Entry<String, List<Map<String, String>>> entry : userResults.entrySet()) {
                System.out.println("   User: " + entry.getKey() + " -> " + entry.getValue().size() + " contracts");
            }
        }
        
        // Print contract search results
        System.out.println("\nCONTRACT SEARCH RESULTS:");
        Map<String, List<Map<String, Object>>> contractResults = session.getContractSearchResults();
        if (contractResults.isEmpty()) {
            System.out.println("   (No contract search results)");
        } else {
            System.out.println("   Valid: " + session.isContractSearchValid());
            System.out.println("   Display Order: " + session.getContractDisplayOrder());
            for (Map.Entry<String, List<Map<String, Object>>> entry : contractResults.entrySet()) {
                System.out.println("   Key: " + entry.getKey() + " -> " + entry.getValue().size() + " items");
            }
        }
        
        // Print conversation history
        System.out.println("\nCONVERSATION HISTORY:");
        List<ConversationSession.ConversationTurn> history = session.getConversationHistory();
        if (history.isEmpty()) {
            System.out.println("   (No conversation history)");
        } else {
            for (int i = 0; i < history.size(); i++) {
                ConversationSession.ConversationTurn turn = history.get(i);
                System.out.println("   " + (i + 1) + ". [" + turn.speaker + "] " + 
                                 new java.util.Date(turn.timestamp) + ": " + 
                                 truncateString(turn.message, 100));
            }
        }
        
        System.out.println("=== END CONVERSATION SESSION DEBUG ===");
    }
    
    /**
     * Print ConversationSession object in a compact format
     * @param session The ConversationSession object to print
     */
    public static void printConversationSessionCompact(ConversationSession session) {
        if (session == null) {
            System.out.println("Session: NULL");
            return;
        }
        
        System.out.println("Session: " + session.getSessionId() + 
                          " | State: " + session.getState() + 
                          " | Flow: " + session.getCurrentFlowType() + 
                          " | Waiting: " + session.isWaitingForUserInput() + 
                          " | Completed: " + session.isCompleted());
    }
    
    /**
     * Print ConversationSession object with focus on contract creation data
     * @param session The ConversationSession object to print
     */
    public static void printContractCreationSession(ConversationSession session) {
        if (session == null) {
            System.out.println("=== CONTRACT CREATION SESSION DEBUG ===");
            System.out.println("Session is NULL");
            System.out.println("=== END CONTRACT CREATION SESSION DEBUG ===");
            return;
        }
        
        System.out.println("=== CONTRACT CREATION SESSION DEBUG ===");
        System.out.println("Session ID: " + session.getSessionId());
        System.out.println("Session State: " + session.getState());
        System.out.println("Flow Type: " + session.getCurrentFlowType());
        System.out.println("Waiting For Input: " + session.isWaitingForUserInput());
        
        // Print contract creation specific data
        System.out.println("\nCONTRACT CREATION DATA:");
        Map<String, Object> collectedData = session.getCollectedData();
        String[] contractFields = {"ACCOUNT_NUMBER", "CONTRACT_NAME", "TITLE", "DESCRIPTION", "COMMENTS", "IS_PRICELIST"};
        
        for (String field : contractFields) {
            Object value = collectedData.get(field);
            if (value != null) {
                System.out.println("   " + field + ": " + value);
            } else {
                System.out.println("   " + field + ": (not set)");
            }
        }
        
        // Print remaining fields
        System.out.println("\nREMAINING FIELDS:");
        List<String> remainingFields = session.getRemainingFields();
        if (remainingFields.isEmpty()) {
            System.out.println("   All fields completed!");
        } else {
            for (String field : remainingFields) {
                System.out.println("   Missing: " + field);
            }
        }
        
        // Print recent conversation
        System.out.println("\nRECENT CONVERSATION:");
        List<ConversationSession.ConversationTurn> history = session.getConversationHistory();
        int startIndex = Math.max(0, history.size() - 3); // Show last 3 turns
        for (int i = startIndex; i < history.size(); i++) {
            ConversationSession.ConversationTurn turn = history.get(i);
            System.out.println("   " + turn.speaker + ": " + truncateString(turn.message, 80));
        }
        
        System.out.println("=== END CONTRACT CREATION SESSION DEBUG ===");
    }
    
    /**
     * Print session state summary
     * @param session The ConversationSession object to print
     */
    public static void printSessionSummary(ConversationSession session) {
        if (session == null) {
            System.out.println("Session Summary: NULL");
            return;
        }
        
        System.out.println("=== SESSION SUMMARY ===");
        System.out.println("ID: " + session.getSessionId());
        System.out.println("State: " + session.getState());
        System.out.println("Flow: " + session.getCurrentFlowType());
        System.out.println("Waiting: " + session.isWaitingForUserInput());
        System.out.println("Completed: " + session.isCompleted());
        
        // Data summary
        Map<String, Object> data = session.getCollectedData();
        System.out.println("Data Fields: " + data.size());
        if (!data.isEmpty()) {
            System.out.println("Fields: " + data.keySet());
        }
        
        // Remaining fields
        List<String> remaining = session.getRemainingFields();
        System.out.println("Remaining Fields: " + remaining.size());
        if (!remaining.isEmpty()) {
            System.out.println("Missing: " + remaining);
        }
        
        // Conversation length
        List<ConversationSession.ConversationTurn> history = session.getConversationHistory();
        System.out.println("Conversation Turns: " + history.size());
        
        System.out.println("=== END SUMMARY ===");
    }
    
    /**
     * Helper method to truncate strings
     */
    private static String truncateString(String str, int maxLength) {
        if (str == null) return "N/A";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
} 