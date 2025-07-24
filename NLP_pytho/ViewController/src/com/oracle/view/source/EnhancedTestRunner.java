package com.oracle.view.source;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Enhanced Test Runner for StandardJSONProcessor
 * Tests the improved display and filter entity extraction functionality
 */
public class EnhancedTestRunner {

    public static void main(String[] args) {
        StandardJSONProcessor processor = new StandardJSONProcessor();
        
        // Test queries based on the successful NLPQueryProcessor results
        String[] TEST_QUERIES = {
            // Contract queries with specific fields
            "What is the effective date for contract 123456?",
            "Show me contract details for 789012",
            "When does contract 456789 expire?",
            "What's the expiration date for 234567?",
            "Get contract information for 345678",
            "Whats the effective date for 567890?",
            "Show contract 678901 details",
            "What is effective date of 789012?",
            "Get contract info for 890123",
            "Show me contract 123456",
            
            // Customer-related queries
            "whos the customer for contract 123456?",
            "customer name for 234567",
            "what customer for contract 345678?",
            "show customer for 456789",
            "customer details for 567890",
            "who is customer for 678901?",
            "get customer info for contract 789012",
            "customer number for 890123",
            "show customer number for 123456",
            "what customer number for 234567?",
            
            // Payment and terms queries
            "payment terms for contract 123456",
            "what are payment terms for 234567?",
            "show payment term for 345678",
            "payment terms for 456789",
            "what payment for contract 567890?",
            "incoterms for contract 678901",
            "what incoterms for 789012?",
            "show incoterms for 890123",
            
            // Contract attributes
            "contract length for 123456",
            "what contract length for 234567?",
            "price expiration date for 123456",
            "when price expire for 234567?",
            "price expiry for contract 345678",
            "show price expiration for 456789",
            "what price expire date for 567890?",
            "creation date for contract 678901",
            "when was 789012 created?",
            "create date for 890123",
            "show creation for contract 123456",
            "when created 234567?",
            "what type of contract 123456?",
            "contract type for 234567",
            "show contract type for 345678",
            "what kind contract 456789?",
            "type of contract 567890",
            "status of contract 678901",
            "what status for 789012?",
            "show contract status for 890123",
            "is 123456 active?",
            "contract status for 234567",
            
            // General info queries
            "show all details for 123456",
            "get everything for contract 234567",
            "full info for 345678",
            "complete details contract 456789",
            "show summary for 567890",
            "overview of contract 678901",
            "brief for 789012",
            "quick info 890123",
            "details about 123456",
            "information on contract 234567",
            
            // Parts queries
            "What is the lead time for part AE12345?",
            "Show me part details for BC67890",
            "What lead time for part DE23456?",
            "Show lead time for FG78901",
            "What's the lead time for part HI34567?",
            "Get part information for JK89012",
            "Show part info for LM45678",
            "What part details for NO90123?",
            "Get part data for PQ56789",
            "Show part summary for RS12345",
            "What's the price for part AE12345?",
            "Show price for part BC67890",
            "What cost for part DE23456?",
            "Get price info for FG78901",
            "Show pricing for part HI34567",
            "What's the price for JK89012",
            "Cost of part LM45678",
            "Price details for NO90123",
            "Show part price for PQ56789",
            "What pricing for RS12345?",
            "What's the MOQ for part AE12345?",
            "Show minimum order for BC67890",
            "What min order qty for DE23456?",
            "MOQ for part FG78901",
            "Minimum order quantity for HI34567",
            "What UOM for part JK89012?",
            "Show unit of measure for LM45678",
            "UOM for NO90123",
            "Unit measure for PQ56789",
            "What unit for part RS12345?",
            "What's the status of part AE12345?",
            "Show part status for BC67890",
            "Status for part DE23456",
            "What status FG78901?",
            "Show part status for HI34567",
            "Is part JK89012 active?",
            "Part status for LM45678",
            "What's status of NO90123?",
            "Show status for PQ56789",
            "Status info for RS12345",
            "What's the item classification for AE12345?",
            "Show item class for BC67890",
            "Classification for part DE23456",
            "What class for FG78901?",
            "Item classification HI34567",
            "Show classification for JK89012",
            "What item class for LM45678?",
            "Classification of NO90123",
            "Item class for PQ56789",
            "Show class for RS12345",
            
            // Parts by contract queries
            "Show me invoice parts for 123456",
            "What invoice part for 234567?",
            "List invoice parts for 345678",
            "Show invoice part for 456789",
            "What invoice parts in 567890?",
            "Get invoice part for 678901",
            "Show invoice parts for 789012",
            "List invoice part for 890123",
            "What invoice parts for 123456?",
            "Show all invoice part for 234567",
            "Show me all parts for contract 123456",
            "What parts in 234567?",
            "List part for contract 345678",
            "Show parts for contract 456789",
            "What parts loaded in 567890?",
            "Get parts for contract 678901",
            "Show all part for 789012",
            "List parts in contract 890123",
            "What parts for 123456?",
            "Show part list for 234567",
            
            // Failed parts queries
            "What parts failed in 567890?",
            "Get failed parts for 678901",
            "Show failing parts for 789012",
            "List failed part for 890123",
            "What failed parts for 123456?",
            "Show all failed part for 234567",
            "Why did parts fail for 123456?",
            "Show error reasons for 234567",
            "What errors for failed parts 345678?",
            "Why parts failed in 456789?",
            "Show failure reasons for 567890",
            "What caused parts to fail for 678901?",
            "Error details for failed parts 789012",
            "Why failed parts in 890123?",
            "Show error info for 123456",
            "What errors caused failure for 234567?",
            "Show me part errors for 123456",
            "What part error for 234567?",
            "List parts with errors for 345678",
            "Show error parts for 456789",
            "What parts have errors in 567890?",
            "Get parts errors for 678901",
            "Show parts with issues for 789012",
            "List error parts for 890123",
            "What parts errors for 123456?",
            "Show all error parts for 234567",
            "What columns have errors for 123456?",
            "Show error columns for 234567",
            "Which columns failed for 345678?",
            "What column errors for 456789?",
            "Show failed columns for 567890",
            "Error column details for 678901",
            "What columns with errors for 789012?",
            "Show column failures for 890123",
            "Which columns error for 123456?",
            "Error column info for 234567",
            "Show me parts with missing data for 123456",
            "What parts missing info for 234567?",
            "List parts with no data for 345678",
            "Show incomplete parts for 456789",
            "What parts missing data in 567890?",
            "Get parts with missing info for 678901",
            "Show parts missing data for 789012",
            "List incomplete parts for 890123",
            "What parts no data for 123456?",
            "Show missing data parts for 234567",
            "What errors occurred during loading for 123456?",
            "Show loading errors for 234567",
            "What load errors for 345678?",
            "Show loading issues for 456789",
            "What happened during load for 567890?",
            "Loading error details for 678901",
            "What load problems for 789012?",
            "Show load failures for 890123",
            "Loading issues for 123456",
            "What errors during loading for 234567?",
            "List validation issues for 123456",
            "Show validation errors for 234567",
            "What validation problems for 345678?",
            "Show validation issues for 456789",
            "What validation errors in 567890?",
            "Get validation problems for 678901",
            "Show validation failures for 789012",
            "List validation errors for 890123",
            "What validation issues for 123456?",
            "Show validation problems for 234567",
            
            // Mixed queries
            "Show me contract details and failed parts for 123456",
            "What's the effective date and part errors for 234567?",
            "List all parts and customer info for contract 345678",
            "Show contract info and failed part for 456789",
            "Get customer name and parts errors for 567890",
            "Show contract details and part issues for 678901",
            "What effective date and failed parts for 789012?",
            "List contract info and error parts for 890123",
            
            // General queries
            "tell me about contract 123456",
            "i need info on 234567",
            "can you show me 345678",
            "what do you know about contract 456789",
            "give me details for 567890",
            "i want to see 678901",
            "please show 789012",
            "can i get info on 890123",
            "help me with contract 123456",
            "i need help with 234567",
            "whats up with 123456?",
            "hows 234567 looking?",
            "anything wrong with 345678?",
            "is 456789 ok?",
            "problems with 567890?",
            "issues with 678901?",
            "troubles with 789012?",
            "concerns about 890123?",
            "status on 123456?",
            "update on 234567?"
        };

        try (PrintWriter writer = new PrintWriter(new FileWriter("EnhancedTestResults.txt"))) {
            writer.println("=== ENHANCED STANDARDJSONPROCESSOR TEST RESULTS ===");
            writer.println("Testing improved display and filter entity extraction\n");
            
            int testNumber = 1;
            for (String query : TEST_QUERIES) {
                try {
                    String jsonResponse = processor.processQuery(query);
                    StandardJSONProcessor.QueryResult result = processor.parseJSONToObject(jsonResponse);
                    
                    writer.println("=== Query " + testNumber++ + ": " + query + " ===");
                    writer.println("Action Type: " + result.getActionType());
                    writer.println("Query Type: " + result.getQueryType());
                    
                    // Display entities
                    writer.println("Display Entities (" + result.displayEntities.size() + "):");
                    for (String entity : result.displayEntities) {
                        writer.println("  - " + entity);
                    }
                    
                    // Filter entities
                    writer.println("Filter Entities (" + result.entities.size() + "):");
                    for (StandardJSONProcessor.EntityFilter filter : result.entities) {
                        writer.println("  - " + filter.attribute + " " + filter.operation + " '" + filter.value + "'");
                    }
                    
                    // Spell corrections
                    if (result.hasSpellCorrections()) {
                        writer.println("Spell Corrections:");
                        writer.println("  Original: " + result.getOriginalInput());
                        writer.println("  Corrected: " + result.getCorrectedInput());
                        writer.println("  Confidence: " + result.getCorrectionConfidence());
                    }
                    
                    writer.println("---\n");
                    
                } catch (Exception e) {
                    writer.println("=== Query " + testNumber++ + ": " + query + " ===");
                    writer.println("ERROR: " + e.getMessage());
                    writer.println("---\n");
                }
            }
            
            writer.println("=== TEST COMPLETED ===");
            System.out.println("Enhanced test completed. Results saved to EnhancedTestResults.txt");
            
        } catch (IOException e) {
            System.err.println("Error writing test results: " + e.getMessage());
        }
    }
} 