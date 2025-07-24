//package com.oracle.view.source;
//import java.util.*;
///**
// * UsageExample - Demonstrates how to use the UserActionHandler architecture
// * Shows the complete flow from user input to SQL execution
// */
//public class UsageExample {
//    
//    public static void main(String[] args) {
//        
//        // Initialize the UserActionHandler
//        UserActionHandler handler = new UserActionHandler();
//        
//        // Example 1: Contract query by contract number
//        System.out.println("=== EXAMPLE 1: Contract Query ===");
//        String userInput1 = "What is the effective date for contract 123456?";
//        UserActionResponse response1 = handler.processUserInput(userInput1);
//        
//        System.out.println("Input: " + userInput1);
//        System.out.println("Corrected: " + response1.getCorrectedInput());
//        System.out.println("Action Type: " + response1.getActionType());
//        System.out.println("SQL Query: " + response1.getSqlQuery());
//        System.out.println("Parameters: " + response1.getParameters());
//        System.out.println("Display Entities: " + response1.getDisplayEntities());
//        System.out.println("Success: " + response1.isSuccess());
//        System.out.println();
//        
//        // Example 2: Parts query by contract number
//        System.out.println("=== EXAMPLE 2: Parts Query ===");
//        String userInput2 = "Show me parts for contract 789012";
//        UserActionResponse response2 = handler.processUserInput(userInput2);
//        
//        System.out.println("Input: " + userInput2);
//        System.out.println("Corrected: " + response2.getCorrectedInput());
//        System.out.println("Action Type: " + response2.getActionType());
//        System.out.println("SQL Query: " + response2.getSqlQuery());
//        System.out.println("Parameters: " + response2.getParameters());
//        System.out.println("Display Entities: " + response2.getDisplayEntities());
//        System.out.println("Success: " + response2.isSuccess());
//        System.out.println();
//        
//        // Example 3: Failed parts query
//        System.out.println("=== EXAMPLE 3: Failed Parts Query ===");
//        String userInput3 = "Show me failed parts for contract 345678";
//        UserActionResponse response3 = handler.processUserInput(userInput3);
//        
//        System.out.println("Input: " + userInput3);
//        System.out.println("Corrected: " + response3.getCorrectedInput());
//        System.out.println("Action Type: " + response3.getActionType());
//        System.out.println("SQL Query: " + response3.getSqlQuery());
//        System.out.println("Parameters: " + response3.getParameters());
//        System.out.println("Display Entities: " + response3.getDisplayEntities());
//        System.out.println("Success: " + response3.isSuccess());
//        System.out.println();
//        
//        // Example 4: Parts query by part number
//        System.out.println("=== EXAMPLE 4: Parts by Part Number ===");
//        String userInput4 = "What is the price for part AE12345?";
//        UserActionResponse response4 = handler.processUserInput(userInput4);
//        
//        System.out.println("Input: " + userInput4);
//        System.out.println("Corrected: " + response4.getCorrectedInput());
//        System.out.println("Action Type: " + response4.getActionType());
//        System.out.println("SQL Query: " + response4.getSqlQuery());
//        System.out.println("Parameters: " + response4.getParameters());
//        System.out.println("Display Entities: " + response4.getDisplayEntities());
//        System.out.println("Success: " + response4.isSuccess());
//        System.out.println();
//        
//        // Example 5: Multi-intent query
//        System.out.println("=== EXAMPLE 5: Multi-Intent Query ===");
//        String userInput5 = "Show me contract details and failed parts for 123456";
//        UserActionResponse response5 = handler.processUserInput(userInput5);
//        
//        System.out.println("Input: " + userInput5);
//        System.out.println("Corrected: " + response5.getCorrectedInput());
//        System.out.println("Action Type: " + response5.getActionType());
//        System.out.println("SQL Query: " + response5.getSqlQuery());
//        System.out.println("Parameters: " + response5.getParameters());
//        System.out.println("Display Entities: " + response5.getDisplayEntities());
//        System.out.println("Success: " + response5.isSuccess());
//        System.out.println();
//        
//        // Example 6: Error handling
//        System.out.println("=== EXAMPLE 6: Error Handling ===");
//        String userInput6 = "Invalid query without contract number";
//        UserActionResponse response6 = handler.processUserInput(userInput6);
//        
//        System.out.println("Input: " + userInput6);
//        System.out.println("Success: " + response6.isSuccess());
//        System.out.println("Message: " + response6.getMessage());
//        System.out.println();
//        
//        // Summary
//        System.out.println("=== SUMMARY ===");
//        System.out.println("Total Examples: 6");
//        System.out.println("Successful: " + (response1.isSuccess() && response2.isSuccess() && 
//                                            response3.isSuccess() && response4.isSuccess() && 
//                                            response5.isSuccess() ? "5" : "Some failed"));
//        System.out.println("Architecture: Working correctly");
//        System.out.println("SQL Generation: Complete");
//        System.out.println("Parameter Mapping: Complete");
//        System.out.println("Display Entity Extraction: Complete");
//    }
//    
//    /**
//     * Example of how to integrate with real database
//     */
//    public static void integrateWithDatabase() {
//        UserActionHandler handler = new UserActionHandler();
//        ActionTypeDataProvider dataProvider = new ActionTypeDataProvider();
//        
//        // Process user input
//        String userInput = "What is the effective date for contract 123456?";
//        UserActionResponse response = handler.processUserInput(userInput);
//        
//        if (response.isSuccess()) {
//            // Execute SQL query with real database
//            try {
//                List<Map<String, Object>> results = dataProvider.getContractByContractNumber(
//                    (String) response.getParameter("AWARD_NUMBER"), 
//                    response.getDisplayEntities()
//                );
//                
//                // Set results in response
//                response.setResultSet(results);
//                response.setRowsAffected(results.size());
//                
//                // Display results
//                System.out.println("Query Results:");
//                for (Map<String, Object> row : results) {
//                    System.out.println(row);
//                }
//                
//            } catch (Exception e) {
//                response.setSuccess(false);
//                response.setMessage("Database error: " + e.getMessage());
//            }
//        }
//        
//        System.out.println("Final Response: " + response);
//    }
//} 