package com.oracle.view.source;

import java.util.List;
import java.util.Map;

/**
 * QueryResult - Represents the result of a database query
 * Extracted from StandardJSONProcessor for standalone use
 */
public class QueryResult {
    public final boolean success;
    public final String message;
    public final List<Map<String, Object>> data;
    public final String sqlQuery;
    public final List<Object> parameters;
    public final Map<String, Object> metadata;

    public QueryResult(boolean success, String message, List<Map<String, Object>> data, 
                      String sqlQuery, List<Object> parameters, Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.sqlQuery = sqlQuery;
        this.parameters = parameters;
        this.metadata = metadata;
    }

    public static QueryResult success(String message, List<Map<String, Object>> data, 
                                    String sqlQuery, List<Object> parameters) {
        return new QueryResult(true, message, data, sqlQuery, parameters, null);
    }

    public static QueryResult error(String message) {
        return new QueryResult(false, message, null, null, null, null);
    }

    @Override
    public String toString() {
        return String.format("QueryResult{success=%s, message='%s', dataSize=%d, sqlQuery='%s'}", 
                           success, message, data != null ? data.size() : 0, sqlQuery);
    }
} 