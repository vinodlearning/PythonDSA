package com.oracle.view.source;

public class StandaloneNLPTestHarness {
    public static void main(String[] args) {
        String[] testQueries = {
            "Tell me how to create a contract",
            "How to create contarct?",
            "Steps to create contract",
            "Can you show me how to make a contract?",
            "What's the process for contract creation?",
            "I need guidance on creating a contract",
            "Walk me through contract creation",
            "Explain how to set up a contract",
            "Instructions for making a contract",
            "Need help understanding contract creation",
            "Create a contract for me",
            "Can you create contract?",
            "Please make a contract",
            "Generate a contract",
            "I need you to create a contract",
            "Set up a contract",
            "Make me a contract",
            "Initiate contract creation",
            "Start a new contract",
            "Could you draft a contract?"
        };

        System.out.println("user_input,query_type,action_type");
        for (String query : testQueries) {
            String normalized = EnhancedNLPProcessor.normalizeText(query);
            String queryType = EnhancedNLPProcessor.determineQueryType(query, normalized);
            String actionType = EnhancedNLPProcessor.determineActionType(query, normalized, queryType);
            System.out.printf("\"%s\",%s,%s\n", query.replace("\"", "'"), queryType, actionType);
        }
    }
} 