package com.oracle.view;

/**
 * Entity filter class representing database filter conditions
 * Used in the entities array of the JSON response
 */
public class EntityFilter {
    private String attribute;    // DB column name
    private String operation;    // =, >, <, between, etc.
    private String value;        // Filter value
    private String source;       // user_input or inferred
    
    // Constructors
    public EntityFilter() {}
    
    public EntityFilter(String attribute, String operation, String value, String source) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
        this.source = source;
    }
    
    // Getters and Setters
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    @Override
    public String toString() {
        return String.format("EntityFilter{attribute='%s', operation='%s', value='%s', source='%s'}", 
                           attribute, operation, value, source);
    }
}