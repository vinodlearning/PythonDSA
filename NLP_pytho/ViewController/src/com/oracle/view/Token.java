package com.oracle.view;

import java.util.Objects;

/**
 * Represents a single token with its value and type classification
 */
public class Token {
    private final String value;
    private final TokenType type;
    private final double confidence;
    
    public Token(String value, TokenType type) {
        this(value, type, 1.0);
    }
    
    public Token(String value, TokenType type, double confidence) {
        this.value = value != null ? value : "";
        this.type = type != null ? type : TokenType.UNKNOWN;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    public String getValue() {
        return value;
    }
    
    public TokenType getType() {
        return type;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public boolean isEntity() {
        return type.isEntity();
    }
    
    public boolean isContractRelated() {
        return type.isContractRelated();
    }
    
    public boolean isPartRelated() {
        return type.isPartRelated();
    }
    
    public boolean isHelpRelated() {
        return type.isHelpRelated();
    }
    
    public boolean isAction() {
        return type.isAction();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(value, token.value) && type == token.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }
    
    @Override
    public String toString() {
        return String.format("Token{value='%s', type=%s, confidence=%.2f}", 
                           value, type, confidence);
    }
}