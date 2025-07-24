package com.oracle.view.source;

/**
 * Conversational Flow Demo
 * Demonstrates the enhanced conversational flow system
 * Shows how complete queries are processed directly without follow-up questions
 */
public class ConversationalFlowDemo {
    
    public static void main(String[] args) {
        System.out.println("ENHANCED CONVERSATIONAL FLOW DEMONSTRATION");
        System.out.println("===========================================");
        System.out.println();
        
        // Demo the specific example provided by the user
        demonstrateCompleteLeadTimeQuery();
        
        // Demo incomplete query requiring follow-up
        demonstrateIncompleteQuery();
        
        // Demo follow-up response processing
        demonstrateFollowUpResponse();
        
        System.out.println("DEMONSTRATION COMPLETED!");
        System.out.println();
        System.out.println("KEY FEATURES DEMONSTRATED:");
        System.out.println("1. Complete queries processed directly (no follow-up questions)");
        System.out.println("2. Incomplete queries trigger appropriate follow-up requests");
        System.out.println("3. Follow-up responses reconstruct original queries");
        System.out.println("4. Lead time queries handled with direct SQL generation");
        System.out.println("5. Conversation context and entity extraction");
    }
    
    /**
     * Demonstrate complete lead time query processing
     * Example: "What is the lead time for part EN6114V4-13 contract 100476"
     */
    private static void demonstrateCompleteLeadTimeQuery() {
        System.out.println("=== DEMO 1: Complete Lead Time Query ===");
        
        String userInput = "What is the lead time for part EN6114V4-13 contract 100476";
        System.out.println("User Input: " + userInput);
        
        // Simulate the enhanced ChatMessage processing
        System.out.println("\nProcessing Steps:");
        System.out.println("1. Create ChatMessage with conversation tracking");
        System.out.println("2. Extract entities: PART_NUMBER=EN6114V4-13, CONTRACT_NUMBER=100476");
        System.out.println("3. Detect complete query (has both part and contract numbers)");
        System.out.println("4. Set queryType=PARTS_LEAD_TIME, actionType=parts_lead_time_query");
        System.out.println("5. Process directly without follow-up questions");
        
        // Simulate the SQL generation
        System.out.println("\nGenerated SQL Query:");
        System.out.println("SELECT lead_time FROM PARTS_TABLE WHERE invoice_part = 'EN6114V4-13' AND loaded_cp_number = '100476'");
        
        // Simulate the JSON response structure
        System.out.println("\nJSON Response Structure:");
        System.out.println("{");
        System.out.println("  \"header\": {");
        System.out.println("    \"contractNumber\": \"100476\",");
        System.out.println("    \"partNumber\": \"EN6114V4-13\",");
        System.out.println("    \"inputTracking\": {");
        System.out.println("      \"originalInput\": \"" + userInput + "\"");
        System.out.println("    }");
        System.out.println("  },");
        System.out.println("  \"queryMetadata\": {");
        System.out.println("    \"queryType\": \"PARTS_LEAD_TIME\",");
        System.out.println("    \"actionType\": \"parts_lead_time_query\",");
        System.out.println("    \"processingTimeMs\": 50");
        System.out.println("  },");
        System.out.println("  \"entities\": [");
        System.out.println("    {\"attribute\": \"INVOICE_PART\", \"value\": \"EN6114V4-13\"},");
        System.out.println("    {\"attribute\": \"LOADED_CP_NUMBER\", \"value\": \"100476\"}");
        System.out.println("  ],");
        System.out.println("  \"displayEntities\": [\"LEAD_TIME\"],");
        System.out.println("  \"moduleSpecificData\": {");
        System.out.println("    \"sqlQuery\": \"SELECT lead_time FROM PARTS_TABLE WHERE invoice_part = ? AND loaded_cp_number = ?\"");
        System.out.println("  },");
        System.out.println("  \"processingResult\": \"Lead time: 15 days\"");
        System.out.println("}");
        
        System.out.println("\nResult: Lead time for part EN6114V4-13 in contract 100476 is 15 days");
        System.out.println();
    }
    
    /**
     * Demonstrate incomplete query requiring follow-up
     * Example: "What is the lead time for part EN6114V4-13"
     */
    private static void demonstrateIncompleteQuery() {
        System.out.println("=== DEMO 2: Incomplete Query (Requires Follow-up) ===");
        
        String userInput = "What is the lead time for part EN6114V4-13";
        System.out.println("User Input: " + userInput);
        
        // Simulate the enhanced ChatMessage processing
        System.out.println("\nProcessing Steps:");
        System.out.println("1. Create ChatMessage with conversation tracking");
        System.out.println("2. Extract entities: PART_NUMBER=EN6114V4-13");
        System.out.println("3. Detect incomplete query (missing contract number)");
        System.out.println("4. Set requiresFollowUp=true, expectedResponseType=PARTS_CONTRACT_REQUEST");
        System.out.println("5. Create conversation state for follow-up");
        
        // Simulate the follow-up request
        System.out.println("\nFollow-up Request Generated:");
        System.out.println("<div style='padding: 15px; border: 1px solid #ffc107; border-radius: 5px; background-color: #fff3cd;'>");
        System.out.println("<h4 style='color: #856404; margin-top: 0;'>Contract Number Required</h4>");
        System.out.println("<p style='color: #856404; margin-bottom: 10px;'>Can you specify the contract number because parts number (invoice part number) is loaded w.r.t Contract.</p>");
        System.out.println("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 3px;'>");
        System.out.println("<strong>For part EN6114V4-13, please provide:</strong><br>");
        System.out.println("  Contract number (e.g., 100476, 123456)<br>");
        System.out.println("  Or say \"contract 100476\"");
        System.out.println("</div>");
        System.out.println("</div>");
        
        System.out.println("\nJSON Response Structure:");
        System.out.println("{");
        System.out.println("  \"queryMetadata\": {");
        System.out.println("    \"queryType\": \"FOLLOW_UP_REQUEST\",");
        System.out.println("    \"actionType\": \"request_missing_info\"");
        System.out.println("  },");
        System.out.println("  \"moduleSpecificData\": {");
        System.out.println("    \"requiresFollowUp\": true,");
        System.out.println("    \"expectedResponseType\": \"PARTS_CONTRACT_REQUEST\"");
        System.out.println("  }");
        System.out.println("}");
        
        System.out.println();
    }
    
    /**
     * Demonstrate follow-up response processing
     * Example: User responds with "100476"
     */
    private static void demonstrateFollowUpResponse() {
        System.out.println("=== DEMO 3: Follow-up Response Processing ===");
        
        String followUpResponse = "100476";
        System.out.println("User Follow-up Response: " + followUpResponse);
        
        // Simulate the processing steps
        System.out.println("\nProcessing Steps:");
        System.out.println("1. Detect follow-up response (matches contract number pattern)");
        System.out.println("2. Extract contract number: 100476");
        System.out.println("3. Reconstruct original query: \"What is the lead time for part EN6114V4-13 in contract 100476\"");
        System.out.println("4. Clear conversation state");
        System.out.println("5. Process reconstructed query as complete query");
        
        // Simulate the reconstructed query
        String reconstructedQuery = "What is the lead time for part EN6114V4-13 in contract 100476";
        System.out.println("\nReconstructed Query: " + reconstructedQuery);
        
        // Simulate the final processing
        System.out.println("\nFinal Processing:");
        System.out.println("1. Extract entities: PART_NUMBER=EN6114V4-13, CONTRACT_NUMBER=100476");
        System.out.println("2. Generate SQL: SELECT lead_time FROM PARTS_TABLE WHERE invoice_part = 'EN6114V4-13' AND loaded_cp_number = '100476'");
        System.out.println("3. Execute query and return result");
        
        System.out.println("\nResult: Lead time for part EN6114V4-13 in contract 100476 is 15 days");
        System.out.println();
    }
} 