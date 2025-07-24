package com.oracle.view.source;

/**
 * Debug test to check corrected input issue
 */
public class DebugCorrectedInput {
    
    public static void main(String[] args) {
        StandardJSONProcessor processor = new StandardJSONProcessor();
        
        String testInput = "Tell me how to create a contract";
        
        System.out.println("Testing corrected input issue:");
        System.out.println("===============================");
        System.out.println("Original Input: " + testInput);
        
        // Test the full process
        String jsonResponse = processor.processQuery(testInput);
        System.out.println("\nFull JSON Response:");
        System.out.println(jsonResponse);
        
        // Parse the JSON back
        StandardJSONProcessor.QueryResult queryResult = processor.parseJSONToObject(jsonResponse);
        System.out.println("\nParsed Result:");
        System.out.println("  Original: " + queryResult.inputTracking.originalInput);
        System.out.println("  Corrected: " + queryResult.inputTracking.correctedInput);
        System.out.println("  Confidence: " + queryResult.inputTracking.correctionConfidence);
    }
} 