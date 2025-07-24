//package com.oracle.view.source;
//
//import com.oracle.view.deep.TestQueries;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.List;
//
///**
// * BatchTestRunner - Processes all test queries and saves detailed results to file
// * Outputs UserActionResponse data in structured tabular format for model validation
// */
//public class BatchTestRunner {
//    
//    public static void main(String v[]) {
//        UserActionHandler ob = new UserActionHandler();
//        
//        // Create output file
//        String outputFile = "UserActionResponse_Results.txt";
//        
//        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
//            
//            // Write header with additional columns
//            writer.println("USER_INPUT | CORRECTED_QUERY | SQL_QUERY | QUERY_TYPE | ACTION_TYPE | DISPLAY_ENTITIES | FILTER_ENTITIES");
//            writer.println("================================================================================================================");
//            
//            // Process each query
//            for (String input : TestQueries. GETALL_QUERIES()) {
//                UserActionResponse res = ob.processUserInput(input);
//                
//                // Format the output line
//                String line = formatResponseLine(input, res);
//                writer.println(line);
//                
//                // Also print to console for immediate feedback
//                System.out.println("Processed: " + input);
//            }
//            
//            System.out.println("\nResults saved to: " + outputFile);
//            
//        } catch (IOException e) {
//            System.err.println("Error writing to file: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//    
//    /**
//     * Formats UserActionResponse data into a tabular line with additional columns
//     */
//    private static String formatResponseLine(String originalInput, UserActionResponse response) {
//        StringBuilder line = new StringBuilder();
//        
//        // USER_INPUT
//        line.append(escapeForTable(originalInput)).append(" | ");
//        
//        // CORRECTED_QUERY
//        String correctedInput = response.getCorrectedInput();
//        line.append(escapeForTable(correctedInput != null ? correctedInput : "")).append(" | ");
//        
//        // SQL_QUERY (Automatically generated in UserActionResponse)
//        String sqlQuery = response.getSqlQuery();
//        line.append(escapeForTable(sqlQuery != null ? sqlQuery : "")).append(" | ");
//        
//        // QUERY_TYPE
//        String queryType = response.getQueryType();
//        line.append(escapeForTable(queryType != null ? queryType : "")).append(" | ");
//        
//        // ACTION_TYPE
//        String actionType = response.getActionType();
//        line.append(escapeForTable(actionType != null ? actionType : "")).append(" | ");
//        
//        // DISPLAY_ENTITIES
//        List<String> displayEntities = response.getDisplayEntities();
//        String displayEntitiesStr = formatDisplayEntities(displayEntities);
//        line.append(escapeForTable(displayEntitiesStr)).append(" | ");
//        
//        // FILTER_ENTITIES
//        List<StandardJSONProcessor.EntityFilter> filters = response.getFilters();
//        String filterEntitiesStr = formatFilterEntities(filters);
//        line.append(escapeForTable(filterEntitiesStr));
//        
//        return line.toString();
//    }
//    
//    /**
//     * Formats display entities list into readable string
//     */
//    private static String formatDisplayEntities(List<String> displayEntities) {
//        if (displayEntities == null || displayEntities.isEmpty()) {
//            return "";
//        }
//        
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < displayEntities.size(); i++) {
//            if (i > 0) sb.append(", ");
//            sb.append(displayEntities.get(i));
//        }
//        return sb.toString();
//    }
//    
//    /**
//     * Formats filter entities list into readable string
//     */
//    private static String formatFilterEntities(List<StandardJSONProcessor.EntityFilter> filters) {
//        if (filters == null || filters.isEmpty()) {
//            return "";
//        }
//        
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < filters.size(); i++) {
//            if (i > 0) sb.append(", ");
//            StandardJSONProcessor.EntityFilter filter = filters.get(i);
//            sb.append(filter.attribute)
//              .append(" ")
//              .append(filter.operation)
//              .append(" ")
//              .append(filter.value);
//        }
//        return sb.toString();
//    }
//    
//    /**
//     * Escapes special characters for table format
//     */
//    private static String escapeForTable(String input) {
//        if (input == null) {
//            return "";
//        }
//        // Replace pipe characters and newlines to maintain table structure
//        return input.replace("|", "\\|")
//                   .replace("\n", " ")
//                   .replace("\r", " ")
//                   .trim();
//    }
//    
//} 