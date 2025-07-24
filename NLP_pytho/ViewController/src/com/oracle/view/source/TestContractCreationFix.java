package com.oracle.view.source;

/**
 * Test class to verify contract creation detection fixes
 */
public class TestContractCreationFix {
    
    public static void main(String[] args) {
        ConversationalNLPManager manager = new ConversationalNLPManager();
        
        // Test cases that should NOT be contract creation queries
        String[] nonCreationQueries = {
            "show100476",
            "show contract 100476",
            "get contract 123456",
            "list contracts",
            "find contract 789012",
            "display contract 345678",
            "what is contract 901234",
            "contract 567890 status",
            "contract 234567 details"
        };
        
        // Test cases that SHOULD be contract creation queries
        String[] creationQueries = {
            "create contract",
            "create contract 123456789",
            "make contract",
            "generate contract",
            "new contract",
            "123456789, testcontract, testtitle, testdesc, nocomments, no",
            "create contract for account 123456789"
        };
        
        System.out.println("=== Testing Non-Creation Queries ===");
        for (String query : nonCreationQueries) {
            boolean isCreation = manager.isExplicitContractCreationQuery(query);
            System.out.println("Query: '" + query + "' -> Is Creation: " + isCreation + " (should be false)");
        }
        
        System.out.println("\n=== Testing Creation Queries ===");
        for (String query : creationQueries) {
            boolean isCreation = manager.isExplicitContractCreationQuery(query);
            System.out.println("Query: '" + query + "' -> Is Creation: " + isCreation + " (should be true)");
        }
    }
} 