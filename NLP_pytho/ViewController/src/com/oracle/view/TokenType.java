package com.oracle.view;

/**
 * Enumeration of all token types recognized by the domain tokenizer
 * Covers contract, parts, and general domain-specific terminology
 */
public enum TokenType {
    
    // Basic types
    WORD("Generic word"),
    NUMBER("Generic number"),
    DATE("Date pattern"),
    
    // Entity types
    CONTRACT_NUMBER("Contract number pattern"),
    PART_NUMBER("Part number pattern"),
    ACCOUNT_NUMBER("Account/Customer number"),
    
    // Contract domain keywords
    CONTRACT_KEYWORD("Contract-related keyword"),
    CUSTOMER_KEYWORD("Customer-related keyword"),
    ACCOUNT_KEYWORD("Account-related keyword"),
    STATUS_KEYWORD("Status-related keyword"),
    STATUS_VALUE("Status value"),
    CREATED_KEYWORD("Creation-related keyword"),
    BY_KEYWORD("By preposition"),
    CREATOR_NAME("Creator name"),
    DATE_KEYWORD("Date-related keyword"),
    
    // Parts domain keywords
    PART_KEYWORD("Part-related keyword"),
    FAILURE_KEYWORD("Failure-related keyword"),
    ERROR_KEYWORD("Error-related keyword"),
    VALIDATION_KEYWORD("Validation-related keyword"),
    LOADING_KEYWORD("Loading-related keyword"),
    MISSING_KEYWORD("Missing-related keyword"),
    REJECTED_KEYWORD("Rejected-related keyword"),
    SUCCESS_KEYWORD("Success-related keyword"),
    SPEC_KEYWORD("Specification-related keyword"),
    COMPATIBILITY_KEYWORD("Compatibility-related keyword"),
    
    // Action keywords
    SHOW_ACTION("Show/Display action"),
    GET_ACTION("Get/Retrieve action"),
    LIST_ACTION("List action"),
    FIND_ACTION("Find/Search action"),
    CREATE_ACTION("Create action"),
    
    // Question words
    QUESTION_WORD("Question word"),
    
    // Help keywords
    HELP_KEYWORD("Help-related keyword"),
    CREATE_KEYWORD("Create-related keyword"),
    
    // Special types
    PUNCTUATION("Punctuation mark"),
    SYMBOL("Special symbol"),
    UNKNOWN("Unknown token type");
    
    private final String description;
    
    TokenType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this token type indicates a contract-related context
     */
    public boolean isContractRelated() {
        return this == CONTRACT_KEYWORD || 
               this == CONTRACT_NUMBER ||
               this == CUSTOMER_KEYWORD ||
               this == ACCOUNT_KEYWORD ||
               this == STATUS_KEYWORD ||
               this == CREATED_KEYWORD ||
               this == CREATOR_NAME ||
               this == DATE_KEYWORD;
    }
    
    /**
     * Check if this token type indicates a parts-related context
     */
    public boolean isPartRelated() {
        return this == PART_KEYWORD ||
               this == PART_NUMBER ||
               this == FAILURE_KEYWORD ||
               this == ERROR_KEYWORD ||
               this == VALIDATION_KEYWORD ||
               this == LOADING_KEYWORD ||
               this == MISSING_KEYWORD ||
               this == REJECTED_KEYWORD ||
               this == SUCCESS_KEYWORD ||
               this == SPEC_KEYWORD ||
               this == COMPATIBILITY_KEYWORD;
    }
    
    /**
     * Check if this token type indicates a help-related context
     */
    public boolean isHelpRelated() {
        return this == HELP_KEYWORD ||
               this == CREATE_KEYWORD ||
               this == QUESTION_WORD;
    }
    
    /**
     * Check if this token type indicates an action
     */
    public boolean isAction() {
        return this == SHOW_ACTION ||
               this == GET_ACTION ||
               this == LIST_ACTION ||
               this == FIND_ACTION ||
               this == CREATE_ACTION;
    }
    
    /**
     * Check if this token type is an entity (extractable information)
     */
    public boolean isEntity() {
        return this == CONTRACT_NUMBER ||
               this == PART_NUMBER ||
               this == ACCOUNT_NUMBER ||
               this == CREATOR_NAME ||
               this == DATE ||
               this == STATUS_VALUE;
    }
    
    /**
     * Get the domain priority for this token type
     * Higher values indicate stronger domain affinity
     */
    public int getDomainPriority(DomainType domain) {
        switch (domain) {
            case CONTRACTS:
                if (this == CONTRACT_NUMBER) return 10;
                if (this == CONTRACT_KEYWORD) return 8;
                if (this == CUSTOMER_KEYWORD || this == ACCOUNT_KEYWORD) return 6;
                if (this == STATUS_KEYWORD || this == CREATED_KEYWORD) return 4;
                if (this == CREATOR_NAME || this == DATE_KEYWORD) return 3;
                break;
                
            case PARTS:
                if (this == PART_NUMBER) return 10;
                if (this == PART_KEYWORD) return 8;
                if (this == FAILURE_KEYWORD || this == ERROR_KEYWORD) return 7;
                if (this == VALIDATION_KEYWORD || this == LOADING_KEYWORD) return 6;
                if (this == MISSING_KEYWORD || this == REJECTED_KEYWORD) return 5;
                if (this == SUCCESS_KEYWORD || this == SPEC_KEYWORD) return 4;
                break;
                
            case HELP:
                if (this == HELP_KEYWORD) return 10;
                if (this == CREATE_KEYWORD) return 8;
                if (this == QUESTION_WORD) return 7;
                break;
        }
        
        return 0;
    }
    
    /**
     * Get all token types related to a specific domain
     */
    public static TokenType[] getTokenTypesForDomain(DomainType domain) {
        switch (domain) {
            case CONTRACTS:
                return new TokenType[] {
                    CONTRACT_KEYWORD, CONTRACT_NUMBER, CUSTOMER_KEYWORD, 
                    ACCOUNT_KEYWORD, STATUS_KEYWORD, STATUS_VALUE,
                    CREATED_KEYWORD, BY_KEYWORD, CREATOR_NAME, DATE_KEYWORD
                };
                
            case PARTS:
                return new TokenType[] {
                    PART_KEYWORD, PART_NUMBER, FAILURE_KEYWORD, ERROR_KEYWORD,
                    VALIDATION_KEYWORD, LOADING_KEYWORD, MISSING_KEYWORD,
                    REJECTED_KEYWORD, SUCCESS_KEYWORD, SPEC_KEYWORD, COMPATIBILITY_KEYWORD
                };
                
            case HELP:
                return new TokenType[] {
                    HELP_KEYWORD, CREATE_KEYWORD, QUESTION_WORD
                };
                
            default:
                return new TokenType[] {};
        }
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}