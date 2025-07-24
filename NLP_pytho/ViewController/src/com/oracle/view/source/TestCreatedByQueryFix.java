package com.oracle.view.source;

/**
 * Test class to verify "created by" query detection fixes
 */
public class TestCreatedByQueryFix {
    
    public static void main(String[] args) {
        ConversationalNLPManager manager = new ConversationalNLPManager();
        
        // Test cases for "created by" queries
        String[] createdByQueries = {
            "contracts created by vinod",
            "contract created by john",
            "show contracts created by mary",
            "get contracts created by user123",
            "list contracts created by admin",
            "find contracts created by manager",
            "display contracts created by supervisor",
            "what contracts created by vinod",
            "contracts by vinod",
            "created by vinod"
        };
        
        // Test cases for user selections (should be detected as user selections)
        String[] userSelections = {
            "1",
            "2",
            "3",
            "John Smith",
            "Mary Johnson",
            "Vinod Kumar",
            "Admin User",
            "Manager User"
        };
        
        // Test cases for other queries (should be detected as new queries)
        String[] otherQueries = {
            "show100476",
            "show contract 100476",
            "get parts",
            "list failed parts",
            "find customer 123456",
            "display status",
            "what is the price"
        };
        
        System.out.println("=== Testing 'Created By' Queries (Should be New Queries) ===");
        for (String query : createdByQueries) {
            boolean isNewQuery = manager.isNewQueryAttempt(query);
            boolean isUserSelection = manager.isUserSelection(query);
            System.out.println("Query: '" + query + "'");
            System.out.println("  -> Is New Query: " + isNewQuery + " (should be true)");
            System.out.println("  -> Is User Selection: " + isUserSelection + " (should be false)");
            System.out.println();
        }
        
        System.out.println("=== Testing User Selections (Should be User Selections) ===");
        for (String query : userSelections) {
            boolean isNewQuery = manager.isNewQueryAttempt(query);
            boolean isUserSelection = manager.isUserSelection(query);
            System.out.println("Query: '" + query + "'");
            System.out.println("  -> Is New Query: " + isNewQuery + " (should be false)");
            System.out.println("  -> Is User Selection: " + isUserSelection + " (should be true)");
            System.out.println();
        }
        
        System.out.println("=== Testing Other Queries (Should be New Queries) ===");
        for (String query : otherQueries) {
            boolean isNewQuery = manager.isNewQueryAttempt(query);
            boolean isUserSelection = manager.isUserSelection(query);
            System.out.println("Query: '" + query + "'");
            System.out.println("  -> Is New Query: " + isNewQuery + " (should be true)");
            System.out.println("  -> Is User Selection: " + isUserSelection + " (should be false)");
            System.out.println();
        }
    }
} 