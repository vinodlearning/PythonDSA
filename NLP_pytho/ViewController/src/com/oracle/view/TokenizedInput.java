package com.oracle.view;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents tokenized input with classified tokens
 * Contains the original input and the list of extracted tokens
 */
public class TokenizedInput {
    
    private final List<Token> tokens;
    private final String originalInput;
    
    /**
     * Constructor
     * 
     * @param tokens List of classified tokens
     * @param originalInput Original user input
     */
    public TokenizedInput(List<Token> tokens, String originalInput) {
        this.tokens = tokens != null ? new ArrayList<>(tokens) : new ArrayList<>();
        this.originalInput = originalInput != null ? originalInput : "";
    }
    
    /**
     * Get the list of tokens
     * 
     * @return Unmodifiable list of tokens
     */
    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }
    
    /**
     * Get the original input string
     * 
     * @return Original input
     */
    public String getOriginalInput() {
        return originalInput;
    }
    
    /**
     * Get the number of tokens
     * 
     * @return Token count
     */
    public int getTokenCount() {
        return tokens.size();
    }
    
    /**
     * Check if tokenization is empty
     * 
     * @return True if no tokens found
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }
    
    /**
     * Get tokens of a specific type
     * 
     * @param type Token type to filter by
     * @return List of tokens of the specified type
     */
    public List<Token> getTokensByType(TokenType type) {
        List<Token> filtered = new ArrayList<>();
        for (Token token : tokens) {
            if (token.getType() == type) {
                filtered.add(token);
            }
        }
        return filtered;
    }
    
    /**
     * Check if contains tokens of a specific type
     * 
     * @param type Token type to check
     * @return True if contains at least one token of the type
     */
    public boolean hasTokenType(TokenType type) {
        for (Token token : tokens) {
            if (token.getType() == type) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all token values as a list of strings
     * 
     * @return List of token values
     */
    public List<String> getTokenValues() {
        List<String> values = new ArrayList<>();
        for (Token token : tokens) {
            values.add(token.getValue());
        }
        return values;
    }
    
    /**
     * Reconstruct text from tokens
     * 
     * @return Reconstructed text
     */
    public String getReconstructedText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(tokens.get(i).getValue());
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "TokenizedInput{" +
                "tokenCount=" + tokens.size() +
                ", originalInput='" + originalInput + '\'' +
                ", tokens=" + tokens +
                '}';
    }
}