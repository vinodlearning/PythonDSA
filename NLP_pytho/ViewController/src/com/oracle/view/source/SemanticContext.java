package com.oracle.view.source;
import java.util.List;

/**
 * Represents the semantic context of a query
 */
public class SemanticContext {
    public final List<SemanticEntity> entities;
    public final String primaryIntent;
    public final List<String> secondaryIntents;
    public final double confidence;
    
    public SemanticContext(List<SemanticEntity> entities, String primaryIntent, 
                          List<String> secondaryIntents, double confidence) {
        this.entities = entities;
        this.primaryIntent = primaryIntent;
        this.secondaryIntents = secondaryIntents;
        this.confidence = confidence;
    }
}