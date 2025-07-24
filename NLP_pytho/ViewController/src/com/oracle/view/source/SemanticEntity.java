package com.oracle.view.source;

public class SemanticEntity {
    public final String type;
    public final String operation;
    public final String value;
    public final int startPosition;
    public final int endPosition;
    
    public SemanticEntity(String type, String operation, String value, int startPosition, int endPosition) {
        this.type = type;
        this.operation = operation;
        this.value = value;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    @Override
    public String toString() {
        return String.format("SemanticEntity{type='%s', operation='%s', value='%s', pos=[%d,%d]}", 
                           type, operation, value, startPosition, endPosition);
    }
}