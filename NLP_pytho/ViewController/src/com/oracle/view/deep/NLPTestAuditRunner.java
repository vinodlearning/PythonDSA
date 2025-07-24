package com.oracle.view.deep;

import com.oracle.view.source.NLPEntityProcessor;
import com.oracle.view.source.NLPQueryClassifier;
import com.oracle.view.source.NLPUserActionHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * NLP Test Audit Runner
 * Automates running all test queries through the NLP pipeline and generates detailed audit reports
 */
public class NLPTestAuditRunner {
    /**
     * Convert NLPQueryClassifier.EntityFilter to NLPEntityProcessor.EntityFilter
     * Both classes have the same structure: attribute, operation, value, source
     */
    private static List<NLPEntityProcessor.EntityFilter> convertEntityFilters(
            List<NLPQueryClassifier.EntityFilter> nlpFilters) {
        List<NLPEntityProcessor.EntityFilter> processorFilters = new ArrayList<>();
        if (nlpFilters != null) {
            for (NLPQueryClassifier.EntityFilter ef : nlpFilters) {
                processorFilters.add(
                    new NLPEntityProcessor.EntityFilter(
                        ef.attribute,
                        ef.operation,
                        ef.value,
                        ef.source
                    )
                );
            }
        }
        return processorFilters;
    }

    public static void main(String[] args) {
        NLPQueryClassifier classifier = new NLPQueryClassifier();
        NLPUserActionHandler userActionHandler = NLPUserActionHandler.getInstance();
        int screenWidth = 400; // or your default

        List<String> allQueries = TestQueries.FAILED_PARTS_QUERIES;
        System.out.println("Failed PARTS INFORMATION QUERIES **** Total Test Case : " + allQueries.size());
        
        // Create output file with timestamp
        String outputFileName = "NLP_Test_Audit_OnlyErrors_" + System.currentTimeMillis() + ".md";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName))) {
            writer.println("NLP TEST AUDIT - ONLY ERROR/UNHANDLED CASES");
            writer.println("============================================");
            writer.println("Generated: " + new java.util.Date());
            writer.println("Total Test Cases: " + allQueries.size());
            writer.println();
            
            int testCaseNum = 1;
            int errorCount = 0;

            for (String query : allQueries) {
                NLPQueryClassifier.QueryResult result = classifier.classifyWithDisambiguation(query);

                String userInput = query;
                String correctedInput = (result.inputTracking != null) ? result.inputTracking.correctedInput : "";
                String queryType = (result.metadata != null) ? result.metadata.queryType : "";
                String actionType = (result.metadata != null) ? result.metadata.actionType : "";
                List<NLPQueryClassifier.EntityFilter> filters = (result.entities != null) ? result.entities : new ArrayList<>();
                List<String> displayEntities = (result.displayEntities != null) ? result.displayEntities : new ArrayList<>();

                // Only write if queryType is ERROR or actionType is UNHANDLED_CASE
                boolean isError = "ERROR".equalsIgnoreCase(queryType) || "UNHANDLED_CASE".equalsIgnoreCase(actionType);
                if (isError) {
                    errorCount++;
                    // Convert EntityFilters to NLPEntityProcessor.EntityFilter format
                    List<NLPEntityProcessor.EntityFilter> processorFilters = convertEntityFilters(filters);
                    // Generate SQL using your business layer with converted filters
                    String sql = "";
                    try {
                        sql = userActionHandler.routeToActionHandlerWithDataProviderDB(
                                actionType, processorFilters, displayEntities, userInput, screenWidth
                        );
                    } catch (Exception e) {
                        sql = "ERROR: " + e.getMessage();
                    }
                    writer.println("test case " + testCaseNum + " [ERROR/UNHANDLED]");
                    writer.println("user input: " + userInput);
                    writer.println("corrected input: " + correctedInput);
                    writer.println("filters entities: " + filters);
                    writer.println("display entities: " + displayEntities);
                    writer.println("query: " + queryType);
                    writer.println("action type: " + actionType);
                    writer.println("sql: " + sql);
                    writer.println("--------------------------------------------------");
                }
                testCaseNum++;
                // Print progress every 50 test cases
                if (testCaseNum % 50 == 0) {
                    System.out.println("Processed " + testCaseNum + " test cases...");
                }
            }

            // Write summary
            writer.println();
            writer.println("SUMMARY");
            writer.println("=======" );
            writer.println("Total test cases: " + allQueries.size());
            writer.println("Total error/unhandled cases: " + errorCount);
            writer.println("Error/Unhandled rate: " + String.format("%.2f%%", (double)(errorCount) / allQueries.size() * 100));
            
            System.out.println("Test audit completed! Only error/unhandled cases written to: " + outputFileName);
            System.out.println("Total test cases: " + allQueries.size());
            System.out.println("Total error/unhandled cases: " + errorCount);
            System.out.println("Error/Unhandled rate: " + String.format("%.2f%%", (double)(errorCount) / allQueries.size() * 100));
            
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
