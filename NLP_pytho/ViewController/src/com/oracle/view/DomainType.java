package com.oracle.view;

/**
 * Enumeration of domain types supported by the NLP system
 */
public enum DomainType {
    CONTRACTS("Contracts Management Domain"),
    PARTS("Parts Validation Domain"),
    HELP("Help and Guidance Domain"),
    AMBIGUOUS("Ambiguous - requires resolution"),
    UNKNOWN("Unknown domain");
    
    private final String description;
    
    DomainType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}