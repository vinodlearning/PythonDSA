package com.oracle.view.source;

import java.util.List;
import java.util.Map;

/**
 * UserActionResponse - Structured response for user actions
 * Contains SQL queries, parameters, display entities, and metadata
 */
public class UserActionResponse {
    
    // Core response data
    private boolean success;
    private String message;
    private String actionType;
    private String sqlQuery;
    private Map<String, Object> parameters;
    private List<String> displayEntities;
    private List<StandardJSONProcessor.EntityFilter> filters;
    
    // NLP processing metadata
    private String originalInput;
    private String correctedInput;
    private String queryType;
    private double nlpProcessingTime;
    
    // Database execution metadata
    private double sqlExecutionTime;
    private int rowsAffected;
    private List<Map<String, Object>> resultSet;
    
    // Error handling
    private String errorCode;
    private String errorDetails;
    
    public UserActionResponse() {
        this.success = false;
        this.parameters = new java.util.HashMap<>();
    }
    
    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getSqlQuery() {
        return sqlQuery;
    }
    
    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public List<String> getDisplayEntities() {
        return displayEntities;
    }
    
    public void setDisplayEntities(List<String> displayEntities) {
        this.displayEntities = displayEntities;
    }
    
    public List<StandardJSONProcessor.EntityFilter> getFilters() {
        return filters;
    }
    
    public void setFilters(List<StandardJSONProcessor.EntityFilter> filters) {
        this.filters = filters;
    }
    
    public String getOriginalInput() {
        return originalInput;
    }
    
    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }
    
    public String getCorrectedInput() {
        return correctedInput;
    }
    
    public void setCorrectedInput(String correctedInput) {
        this.correctedInput = correctedInput;
    }
    
    public String getQueryType() {
        return queryType;
    }
    
    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
    
    public double getNlpProcessingTime() {
        return nlpProcessingTime;
    }
    
    public void setNlpProcessingTime(double nlpProcessingTime) {
        this.nlpProcessingTime = nlpProcessingTime;
    }
    
    public double getSqlExecutionTime() {
        return sqlExecutionTime;
    }
    
    public void setSqlExecutionTime(double sqlExecutionTime) {
        this.sqlExecutionTime = sqlExecutionTime;
    }
    
    public int getRowsAffected() {
        return rowsAffected;
    }
    
    public void setRowsAffected(int rowsAffected) {
        this.rowsAffected = rowsAffected;
    }
    
    public List<Map<String, Object>> getResultSet() {
        return resultSet;
    }
    
    public void setResultSet(List<Map<String, Object>> resultSet) {
        this.resultSet = resultSet;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Adds a parameter to the parameter map
     */
    public void addParameter(String key, Object value) {
        if (this.parameters == null) {
            this.parameters = new java.util.HashMap<>();
        }
        this.parameters.put(key, value);
    }
    
    /**
     * Gets a parameter value by key
     */
    public Object getParameter(String key) {
        return this.parameters != null ? this.parameters.get(key) : null;
    }
    
    /**
     * Checks if response has parameters
     */
    public boolean hasParameters() {
        return this.parameters != null && !this.parameters.isEmpty();
    }
    
    /**
     * Checks if response has display entities
     */
    public boolean hasDisplayEntities() {
        return this.displayEntities != null && !this.displayEntities.isEmpty();
    }
    
    /**
     * Checks if response has filters
     */
    public boolean hasFilters() {
        return this.filters != null && !this.filters.isEmpty();
    }
    
    /**
     * Checks if response has results
     */
    public boolean hasResults() {
        return this.resultSet != null && !this.resultSet.isEmpty();
    }
    
    /**
     * Gets the total processing time (NLP + SQL)
     */
    public double getTotalProcessingTime() {
        return this.nlpProcessingTime + this.sqlExecutionTime;
    }
    
    /**
     * Creates a success response
     */
    public static UserActionResponse createSuccess(String message) {
        UserActionResponse response = new UserActionResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }
    
    /**
     * Creates an error response
     */
    public static UserActionResponse createError(String message, String errorCode) {
        UserActionResponse response = new UserActionResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        return response;
    }
    
    /**
     * Creates an error response with details
     */
    public static UserActionResponse createError(String message, String errorCode, String errorDetails) {
        UserActionResponse response = createError(message, errorCode);
        response.setErrorDetails(errorDetails);
        return response;
    }
    
    // ============================================================================
    // SQL GENERATION METHODS
    // ============================================================================
    
    /**
     * Generates SQL query from the response data and sets it
     */
    public void generateSQLQuery() {
        this.sqlQuery = buildSQLQuery();
    }
    
    /**
     * Builds SQL query from action type, display entities, and filters
     */
    private String buildSQLQuery() {
        if (this.actionType == null) {
            return "";
        }
        
        StringBuilder sql = new StringBuilder();
        
        // Determine table name based on action type
        String tableName = getTableName(this.actionType);
        
        // Build SELECT clause
        sql.append("SELECT ");
        if (this.displayEntities != null && !this.displayEntities.isEmpty()) {
            for (int i = 0; i < this.displayEntities.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(this.displayEntities.get(i));
            }
        } else {
            sql.append("*");
        }
        
        // Build FROM clause
        sql.append(" FROM ").append(tableName);
        
        // Build WHERE clause
        if (this.filters != null && !this.filters.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < this.filters.size(); i++) {
                if (i > 0) sql.append(" AND ");
                StandardJSONProcessor.EntityFilter filter = this.filters.get(i);
                
                // Handle different data types properly
                if (isNumeric(filter.value)) {
                    sql.append(filter.attribute)
                       .append(" ")
                       .append(filter.operation)
                       .append(" ")
                       .append(filter.value);
                } else {
                    sql.append(filter.attribute)
                       .append(" ")
                       .append(filter.operation)
                       .append(" '")
                       .append(filter.value)
                       .append("'");
                }
            }
        }
        
        return sql.toString();
    }
    
    /**
     * Determines proper table name based on action type
     */
    private String getTableName(String actionType) {
        if (actionType == null) return "CRM_CONTRACTS";
        
        if (actionType.contains("failed")) {
            return "CRM_PARTS_ERROS_FINAL";
        } else if (actionType.contains("parts")) {
            return "CRM_PARTS_FINAL";
        } else {
            return "CRM_CONTRACTS";
        }
    }
    
    /**
     * Checks if a string is numeric
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // ============================================================================
    // TO STRING METHOD
    // ============================================================================
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UserActionResponse {\n");
        sb.append("  success: ").append(success).append("\n");
        sb.append("  message: ").append(message).append("\n");
        sb.append("  actionType: ").append(actionType).append("\n");
        sb.append("  queryType: ").append(queryType).append("\n");
        sb.append("  originalInput: ").append(originalInput).append("\n");
        sb.append("  correctedInput: ").append(correctedInput).append("\n");
        sb.append("  sqlQuery: ").append(sqlQuery).append("\n");
        sb.append("  parameters: ").append(parameters).append("\n");
        sb.append("  displayEntities: ").append(displayEntities).append("\n");
        sb.append("  filters: ").append(filters).append("\n");
        sb.append("  nlpProcessingTime: ").append(nlpProcessingTime).append("ms\n");
        sb.append("  sqlExecutionTime: ").append(sqlExecutionTime).append("ms\n");
        sb.append("  totalProcessingTime: ").append(getTotalProcessingTime()).append("ms\n");
        sb.append("  rowsAffected: ").append(rowsAffected).append("\n");
        sb.append("  resultSetSize: ").append(resultSet != null ? resultSet.size() : 0).append("\n");
        if (errorCode != null) {
            sb.append("  errorCode: ").append(errorCode).append("\n");
        }
        if (errorDetails != null) {
            sb.append("  errorDetails: ").append(errorDetails).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
} 