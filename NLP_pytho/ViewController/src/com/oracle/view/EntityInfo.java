package com.oracle.view;

/**
 * Information about extracted entities
 */
public class EntityInfo {
    private String type;
    private String value;
    private double confidence;
    private String source;
    
    public EntityInfo(String type, String value, double confidence, String source) {
        this.type = type;
        this.value = value;
        this.confidence = confidence;
        this.source = source;
    }
    
    // Getters
    public String getType() { return type; }
    public String getValue() { return value; }
    public double getConfidence() { return confidence; }
    public String getSource() { return source; }
    
    @Override
    public String toString() {
        return String.format("EntityInfo{type='%s', value='%s', confidence=%.2f}", 
                           type, value, confidence);
    }
}