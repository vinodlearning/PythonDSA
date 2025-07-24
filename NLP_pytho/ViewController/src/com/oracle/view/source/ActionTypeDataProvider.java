package com.oracle.view.source;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ActionTypeDataProvider - Data access layer for different action types
 * Handles SQL execution and data retrieval for various business operations
 */
public class ActionTypeDataProvider {
    
    // Database connection (you'll need to implement connection management)
    private Connection connection;
    
    public ActionTypeDataProvider() {
        // Initialize database connection
        // this.connection = DatabaseConnectionManager.getConnection();
    }
    
    // ============================================================================
    // MAIN EXECUTE ACTION METHOD
    // ============================================================================
    
    /**
     * Main method to execute actions based on action type
     * This method routes to appropriate data provider methods
     */
    public String executeAction(String actionType, List<StandardJSONProcessor.EntityFilter> filters, 
                               List<String> displayEntities, String userInput) {
        try {
            switch (actionType) {
                case "contracts_by_contractnumber":
                    return executeContractByContractNumber(filters, displayEntities);
                    
                case "parts_by_contract_number":
                    return executePartsByContractNumber(filters, displayEntities);
                    
                case "parts_failed_by_contract_number":
                    return executeFailedPartsByContractNumber(filters, displayEntities);
                    
                case "parts_by_part_number":
                    return executePartsByPartNumber(filters, displayEntities);
                    
                case "contracts_by_filter":
                    return executeContractsByFilter(filters, displayEntities);
                    
                case "parts_by_filter":
                    return executePartsByFilter(filters, displayEntities);
                    
                case "update_contract":
                    return executeUpdateContract(filters, displayEntities);
                    
                case "create_contract":
                    return executeCreateContract(filters, displayEntities);
                    
                case "HELP_CONTRACT_CREATE_USER":
                    return getHelpContractCreateUser();
                    
                case "HELP_CONTRACT_CREATE_BOT":
                    return getHelpContractCreateBot();
                    
                default:
                    return "Unknown action type: " + actionType;
            }
        } catch (Exception e) {
            return "Error executing action " + actionType + ": " + e.getMessage();
        }
    }
    
    // ============================================================================
    // ACTION EXECUTION METHODS
    // ============================================================================
    
    /**
     * Execute contract by contract number
     */
    private String executeContractByContractNumber(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
        if (contractNumber == null) {
            return "Contract number not found in query";
        }
        
        List<Map<String, Object>> results = getContractByContractNumber(contractNumber, displayEntities);
        return formatResultsAsString(results, "Contract data for " + contractNumber);
    }
    
    /**
     * Execute parts by contract number
     */
    private String executePartsByContractNumber(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
        if (contractNumber == null) {
            return "Contract number not found in query";
        }
        
        List<Map<String, Object>> results = getPartsByContractNumber(contractNumber, displayEntities);
        return formatResultsAsString(results, "Parts data for contract " + contractNumber);
    }
    
    /**
     * Execute failed parts by contract number
     */
    private String executeFailedPartsByContractNumber(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String contractNumber = extractFilterValue(filters, "LOADED_CP_NUMBER");
        if (contractNumber == null) {
            return "Contract number not found in query";
        }
        
        List<Map<String, Object>> results = getFailedPartsByContractNumber(contractNumber, displayEntities);
        return formatResultsAsString(results, "Failed parts data for contract " + contractNumber);
    }
    
    /**
     * Execute parts by part number
     */
    private String executePartsByPartNumber(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String partNumber = extractFilterValue(filters, "INVOICE_PART_NUMBER");
        if (partNumber == null) {
            return "Part number not found in query";
        }
        
        List<Map<String, Object>> results = getPartsByPartNumber(partNumber, displayEntities);
        return formatResultsAsString(results, "Part data for " + partNumber);
    }
    
    /**
     * Execute contracts by filter
     */
    private String executeContractsByFilter(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        List<Map<String, Object>> results = getContractsByFilter(filters, displayEntities);
        return formatResultsAsString(results, "Contract data based on filters");
    }
    
    /**
     * Execute parts by filter
     */
    private String executePartsByFilter(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        List<Map<String, Object>> results = getPartsByFilter(filters, displayEntities);
        return formatResultsAsString(results, "Parts data based on filters");
    }
    
    /**
     * Execute update contract
     */
    private String executeUpdateContract(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        // Extract contract number and update data from filters
        String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
        if (contractNumber == null) {
            return "Contract number not found in query";
        }
        
        Map<String, Object> updateData = createParameterMapFromFilters(filters);
        updateData.remove("AWARD_NUMBER"); // Remove contract number from update data
        
        int rowsAffected = updateContract(contractNumber, updateData);
        return "Contract " + contractNumber + " updated successfully. Rows affected: " + rowsAffected;
    }
    
    /**
     * Execute create contract
     */
    private String executeCreateContract(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        Map<String, Object> contractData = createParameterMapFromFilters(filters);
        
        int rowsAffected = createContract(contractData);
        return "Contract created successfully. Rows affected: " + rowsAffected;
    }
    
    /**
     * Extract filter value by attribute name
     */
    private String extractFilterValue(List<StandardJSONProcessor.EntityFilter> filters, String attributeName) {
        for (StandardJSONProcessor.EntityFilter filter : filters) {
            if (filter.attribute.equals(attributeName)) {
                return filter.value;
            }
        }
        return null;
    }
    
    /**
     * Format results as string
     */
    private String formatResultsAsString(List<Map<String, Object>> results, String title) {
        if (results == null || results.isEmpty()) {
            return title + ": No data found";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(":\n");
        sb.append("Found ").append(results.size()).append(" record(s)\n\n");
        
        for (int i = 0; i < Math.min(results.size(), 5); i++) { // Show max 5 records
            Map<String, Object> row = results.get(i);
            sb.append("Record ").append(i + 1).append(":\n");
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }
        
        if (results.size() > 5) {
            sb.append("... and ").append(results.size() - 5).append(" more records\n");
        }
        
        return sb.toString();
    }
    
    // ============================================================================
    // CONTRACT DATA METHODS
    // ============================================================================
    
    /**
     * Retrieves contract data by contract number
     */
    public List<Map<String, Object>> getContractByContractNumber(String contractNumber, List<String> displayEntities) {
        String sql = buildContractQuery(displayEntities, contractNumber);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("AWARD_NUMBER", contractNumber);
        
        return executeQuery(sql, parameters);
    }
    
    /**
     * Retrieves contracts by filter criteria
     */
    public List<Map<String, Object>> getContractsByFilter(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String sql = buildContractsByFilterQuery(displayEntities, filters);
        Map<String, Object> parameters = createParameterMapFromFilters(filters);
        
        return executeQuery(sql, parameters);
    }
    
    /**
     * Updates contract data
     */
    public int updateContract(String contractNumber, Map<String, Object> updateData) {
        String sql = buildUpdateContractQuery(updateData, contractNumber);
        Map<String, Object> parameters = new HashMap<>(updateData);
        parameters.put("AWARD_NUMBER", contractNumber);
        
        return executeUpdate(sql, parameters);
    }
    
    /**
     * Creates new contract
     */
    public int createContract(Map<String, Object> contractData) {
        String sql = buildCreateContractQuery(contractData);
        
        return executeUpdate(sql, contractData);
    }
    
    // ============================================================================
    // HELP DATA METHODS
    // ============================================================================
    
    /**
     * Provides help content for contract creation (user instructions)
     * No SQL needed - returns help text
     */
    public String getHelpContractCreateUser() {
        // TODO: Add the necessary help text here
        return "Please follow below steps to create a contract:\n" +
               "1. Step one\n" +
               "2. Step two\n" +
               "3. Step three\n" +
               "4. Step four";
    }
    
    /**
     * Provides help content for contract creation (bot assistance)
     * No SQL needed - returns help text
     */
    public String getHelpContractCreateBot() {
        // TODO: Add the necessary help text here
        return "I can help you create a contract. Please provide the necessary details:\n" +
               "- Contract name\n" +
               "- Customer information\n" +
               "- Effective dates\n" +
               "- Other required fields";
    }
    
    // ============================================================================
    // PARTS DATA METHODS
    // ============================================================================
    
    /**
     * Retrieves parts data by contract number
     */
    public List<Map<String, Object>> getPartsByContractNumber(String contractNumber, List<String> displayEntities) {
        String sql = buildPartsByContractQuery(displayEntities, contractNumber);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("AWARD_NUMBER", contractNumber);
        
        return executeQuery(sql, parameters);
    }
    
    /**
     * Retrieves parts data by part number
     */
    public List<Map<String, Object>> getPartsByPartNumber(String partNumber, List<String> displayEntities) {
        String sql = buildPartsByPartNumberQuery(displayEntities, partNumber);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("INVOICE_PART_NUMBER", partNumber);
        
        return executeQuery(sql, parameters);
    }
    
    /**
     * Retrieves parts data by filter criteria
     */
    public List<Map<String, Object>> getPartsByFilter(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String sql = buildPartsByFilterQuery(displayEntities, filters);
        Map<String, Object> parameters = createParameterMapFromFilters(filters);
        
        return executeQuery(sql, parameters);
    }
    
    // ============================================================================
    // FAILED PARTS DATA METHODS
    // ============================================================================
    
    /**
     * Retrieves failed parts data by contract number
     */
    public List<Map<String, Object>> getFailedPartsByContractNumber(String contractNumber, List<String> displayEntities) {
        String sql = buildFailedPartsQuery(displayEntities, contractNumber);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LOADED_CP_NUMBER", contractNumber);
        
        return executeQuery(sql, parameters);
    }
    
    /**
     * Retrieves failed parts data by filter criteria
     */
    public List<Map<String, Object>> getFailedPartsByFilter(List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities) {
        String sql = buildFailedPartsByFilterQuery(displayEntities, filters);
        Map<String, Object> parameters = createParameterMapFromFilters(filters);
        
        return executeQuery(sql, parameters);
    }
    
    // ============================================================================
    // SQL QUERY BUILDERS
    // ============================================================================
    
    /**
     * Builds contract query SQL
     */
    private String buildContractQuery(List<String> displayEntities, String contractNumber) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        return String.format("SELECT %s FROM contracts WHERE AWARD_NUMBER = ?", displayFields);
    }
    
    /**
     * Builds contracts by filter query SQL
     */
    private String buildContractsByFilterQuery(List<String> displayEntities, List<StandardJSONProcessor.EntityFilter> filters) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        String whereClause = buildWhereClause(filters);
        return String.format("SELECT %s FROM contracts WHERE %s", displayFields, whereClause);
    }
    
    /**
     * Builds parts by contract query SQL
     */
    private String buildPartsByContractQuery(List<String> displayEntities, String contractNumber) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        return String.format("SELECT %s FROM parts WHERE AWARD_NUMBER = ?", displayFields);
    }
    
    /**
     * Builds parts by part number query SQL
     */
    private String buildPartsByPartNumberQuery(List<String> displayEntities, String partNumber) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        return String.format("SELECT %s FROM parts WHERE INVOICE_PART_NUMBER = ?", displayFields);
    }
    
    /**
     * Builds parts by filter query SQL
     */
    private String buildPartsByFilterQuery(List<String> displayEntities, List<StandardJSONProcessor.EntityFilter> filters) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        String whereClause = buildWhereClause(filters);
        return String.format("SELECT %s FROM parts WHERE %s", displayFields, whereClause);
    }
    
    /**
     * Builds failed parts query SQL
     */
    private String buildFailedPartsQuery(List<String> displayEntities, String contractNumber) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        return String.format("SELECT %s FROM failed_parts WHERE LOADED_CP_NUMBER = ?", displayFields);
    }
    
    /**
     * Builds failed parts by filter query SQL
     */
    private String buildFailedPartsByFilterQuery(List<String> displayEntities, List<StandardJSONProcessor.EntityFilter> filters) {
        String displayFields = displayEntities.isEmpty() ? "*" : String.join(", ", displayEntities);
        String whereClause = buildWhereClause(filters);
        return String.format("SELECT %s FROM failed_parts WHERE %s", displayFields, whereClause);
    }
    
    /**
     * Builds update contract query SQL
     */
    private String buildUpdateContractQuery(Map<String, Object> updateData, String contractNumber) {
        StringBuilder setClause = new StringBuilder();
        for (String field : updateData.keySet()) {
            if (!field.equals("AWARD_NUMBER")) {
                if (setClause.length() > 0) setClause.append(", ");
                setClause.append(field).append(" = ?");
            }
        }
        return String.format("UPDATE contracts SET %s WHERE AWARD_NUMBER = ?", setClause.toString());
    }
    
    /**
     * Builds create contract query SQL
     */
    private String buildCreateContractQuery(Map<String, Object> contractData) {
        StringBuilder fields = new StringBuilder();
        StringBuilder values = new StringBuilder();
        
        for (String field : contractData.keySet()) {
            if (fields.length() > 0) {
                fields.append(", ");
                values.append(", ");
            }
            fields.append(field);
            values.append("?");
        }
        
        return String.format("INSERT INTO contracts (%s) VALUES (%s)", fields.toString(), values.toString());
    }
    
    // ============================================================================
    // DATABASE EXECUTION METHODS
    // ============================================================================
    
    /**
     * Executes a SELECT query and returns results
     */
    private List<Map<String, Object>> executeQuery(String sql, Map<String, Object> parameters) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Set parameters
            int paramIndex = 1;
            for (Object value : parameters.values()) {
                stmt.setObject(paramIndex++, value);
            }
            
            // Execute query
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object columnValue = rs.getObject(i);
                        row.put(columnName, columnValue);
                    }
                    results.add(row);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Executes an UPDATE/INSERT/DELETE query and returns rows affected
     */
    private int executeUpdate(String sql, Map<String, Object> parameters) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Set parameters
            int paramIndex = 1;
            for (Object value : parameters.values()) {
                stmt.setObject(paramIndex++, value);
            }
            
            // Execute update
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Error executing update: " + e.getMessage(), e);
        }
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Builds WHERE clause from filters
     */
    private String buildWhereClause(List<StandardJSONProcessor.EntityFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "1=1"; // Default condition
        }
        
        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            StandardJSONProcessor.EntityFilter filter = filters.get(i);
            if (i > 0) whereClause.append(" AND ");
            whereClause.append(filter.attribute).append(" ").append(filter.operation).append(" ?");
        }
        return whereClause.toString();
    }
    
    /**
     * Creates parameter map from filters
     */
    private Map<String, Object> createParameterMapFromFilters(List<StandardJSONProcessor.EntityFilter> filters) {
        Map<String, Object> parameters = new HashMap<>();
        if (filters != null) {
                    for (StandardJSONProcessor.EntityFilter filter : filters) {
            parameters.put(filter.attribute, filter.value);
        }
        }
        return parameters;
    }
    
    /**
     * Validates SQL query for security
     */
    private boolean isValidSqlQuery(String sql) {
        // Basic SQL injection prevention
        String lowerSql = sql.toLowerCase();
        return !lowerSql.contains("drop") && 
               !lowerSql.contains("delete") && 
               !lowerSql.contains("truncate") &&
               !lowerSql.contains("alter") &&
               !lowerSql.contains("create") &&
               !lowerSql.contains("insert") &&
               !lowerSql.contains("update") ||
               (lowerSql.contains("insert") && lowerSql.contains("into")) ||
               (lowerSql.contains("update") && lowerSql.contains("set"));
    }
    
    /**
     * Sanitizes column names for SQL injection prevention
     */
    private String sanitizeColumnName(String columnName) {
        // Remove any non-alphanumeric characters except underscore
        return columnName.replaceAll("[^a-zA-Z0-9_]", "");
    }
    
    /**
     * Closes database connection
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Log error but don't throw
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
} 