package com.oracle.view.source;

/**
 * Centralized constants for NLP action types and query types
 * This ensures consistency across the entire application
 */
public class NLPConstants {
    
    // ============================================================================
    // QUERY TYPES
    // ============================================================================
    
    /** General help/inquiry queries */
    public static final String QUERY_TYPE_HELP = "HELP";
    
    /** Contract-related queries */
    public static final String QUERY_TYPE_CONTRACTS = "CONTRACTS";
    
    /** Parts-related queries */
    public static final String QUERY_TYPE_PARTS = "PARTS";
    
    /** Failed parts queries */
    public static final String QUERY_TYPE_FAILED_PARTS = "FAILED_PARTS";
    
    /** Opportunities queries */
    public static final String QUERY_TYPE_OPPORTUNITIES = "OPPORTUNITIES";
    
    /** Customer queries */
    public static final String QUERY_TYPE_CUSTOMERS = "CUSTOMERS";
    
    /** Error queries */
    public static final String QUERY_TYPE_ERROR = "ERROR";
    
    /** Inquiry queries */
    public static final String QUERY_TYPE_INQUIRY = "INQUIRY";
    
    // ============================================================================
    // CONTRACT ACTION TYPES
    // ============================================================================
    
    /** Contract creation by bot (user asks system to create) */
    public static final String ACTION_TYPE_HELP_CONTRACT_CREATE_BOT = "HELP_CONTRACT_CREATE_BOT";
    
    /** Contract creation help for user (user wants steps/guidance) */
    public static final String ACTION_TYPE_HELP_CONTRACT_CREATE_USER = "HELP_CONTRACT_CREATE_USER";
    
    /** Contract by contract number */
    public static final String ACTION_TYPE_CONTRACT_BY_CONTRACT_NUMBER = "contracts_by_contractnumber";
    
    /** Contracts by filter criteria */
    public static final String ACTION_TYPE_CONTRACTS_BY_FILTER = "contracts_by_filter";
    
    /** Update contract */
    public static final String ACTION_TYPE_UPDATE_CONTRACT = "update_contract";
    
    /** Create contract */
    public static final String ACTION_TYPE_CREATE_CONTRACT = "create_contract";
    
    public static final String ACTION_TYPE_HELP_CHECK_LIST = "CONTRACT_CREATION_CHECKLIST";
    
    // ============================================================================
    // PARTS ACTION TYPES
    // ============================================================================
    
    /** Parts by contract number */
    public static final String ACTION_TYPE_PARTS_BY_CONTRACT_NUMBER = "parts_by_contract_number";
    
    /** Parts by part number */
    public static final String ACTION_TYPE_PARTS_BY_PART_NUMBER = "parts_by_part_number";
    
    /** Parts by filter criteria */
    public static final String ACTION_TYPE_PARTS_BY_FILTER = "parts_by_filter";
    
    /** Update part */
    public static final String ACTION_TYPE_UPDATE_PART = "update_part";
    
    /** Update part price */
    public static final String ACTION_TYPE_UPDATE_PART_PRICE = "update_part_price";
    
    // ============================================================================
    // FAILED PARTS ACTION TYPES
    // ============================================================================
    
    /** Failed parts by contract number */
    public static final String ACTION_TYPE_FAILED_PARTS_BY_CONTRACT_NUMBER = "parts_failed_by_contract_number";
    
    /** Failed parts by filter criteria */
    public static final String ACTION_TYPE_FAILED_PARTS_BY_FILTER = "parts_failed_by_filter";
    
    // ============================================================================
    // ERROR ACTION TYPES
    // ============================================================================
    
    /** Parse error */
    public static final String ACTION_TYPE_PARSE_ERROR = "PARSE_ERROR";
    
    /** Enquiry failed */
    public static final String ACTION_TYPE_ENQUIRY_FAILED = "ENQUERY_FAILED";
    
    // ============================================================================
    // VALIDATION ERROR CODES
    // ============================================================================
    
    /** Missing contract number error */
    public static final String ERROR_CODE_MISSING_CONTRACT_NUMBER = "MISSING_CONTRACT_NUMBER";
    
    /** Missing customer number error */
    public static final String ERROR_CODE_MISSING_CUSTOMER_NUMBER = "MISSING_CUSTOMER_NUMBER";
    
    /** Invalid contract number error */
    public static final String ERROR_CODE_INVALID_CONTRACT_NUMBER = "INVALID_CONTRACT_NUMBER";
    
    /** Invalid customer number error */
    public static final String ERROR_CODE_INVALID_CUSTOMER_NUMBER = "INVALID_CUSTOMER_NUMBER";
    
    /** Query execution error */
    public static final String ERROR_CODE_QUERY_EXECUTION_ERROR = "QUERY_EXECUTION_ERROR";
    
    /** Parse error */
    public static final String ERROR_CODE_PARSE_ERROR = "PARSE_ERROR";
    
    // ============================================================================
    // ERROR SEVERITY LEVELS
    // ============================================================================
    
    /** Error severity */
    public static final String ERROR_SEVERITY_ERROR = "ERROR";
    
    /** Warning severity */
    public static final String ERROR_SEVERITY_WARNING = "WARNING";
    
    /** Info severity */
    public static final String ERROR_SEVERITY_INFO = "INFO";
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if action type is a contract creation action
     */
    public static boolean isContractCreationAction(String actionType) {
        return ACTION_TYPE_HELP_CONTRACT_CREATE_BOT.equals(actionType) || 
               ACTION_TYPE_HELP_CONTRACT_CREATE_USER.equals(actionType);
    }
    
    /**
     * Check if action type is a bot contract creation action
     */
    public static boolean isBotContractCreationAction(String actionType) {
        return ACTION_TYPE_HELP_CONTRACT_CREATE_BOT.equalsIgnoreCase(actionType);
    }
    
    /**
     * Check if action type is a user contract creation action
     */
    public static boolean isUserContractCreationAction(String actionType) {
        return ACTION_TYPE_HELP_CONTRACT_CREATE_USER.equals(actionType);
    }

    public static boolean isUserCheckListCreationAction(String actionType) {
        return ACTION_TYPE_HELP_CHECK_LIST.equals(actionType);
    }
    /**
     * Check if query type is a help query
     */
    public static boolean isHelpQuery(String queryType) {
        return QUERY_TYPE_HELP.equalsIgnoreCase(queryType);
    }
    
    /**
     * Check if query type is a contract query
     */
    public static boolean isContractQuery(String queryType) {
        return QUERY_TYPE_CONTRACTS.equals(queryType);
    }
    
    /**
     * Check if query type is a parts query
     */
    public static boolean isPartsQuery(String queryType) {
        return QUERY_TYPE_PARTS.equals(queryType);
    }
    
    /**
     * Check if query type is a failed parts query
     */
    public static boolean isFailedPartsQuery(String queryType) {
        return QUERY_TYPE_FAILED_PARTS.equals(queryType);
    }
    
    /**
     * Get all contract creation action types
     */
    public static String[] getContractCreationActionTypes() {
        return new String[] {
            ACTION_TYPE_HELP_CONTRACT_CREATE_BOT,
            ACTION_TYPE_HELP_CONTRACT_CREATE_USER
        };
    }
    
    /**
     * Get all contract action types
     */
    public static String[] getContractActionTypes() {
        return new String[] {
            ACTION_TYPE_HELP_CONTRACT_CREATE_BOT,
            ACTION_TYPE_HELP_CONTRACT_CREATE_USER,
            ACTION_TYPE_CONTRACT_BY_CONTRACT_NUMBER,
            ACTION_TYPE_CONTRACTS_BY_FILTER,
            ACTION_TYPE_UPDATE_CONTRACT,
            ACTION_TYPE_CREATE_CONTRACT
        };
    }
    
    /**
     * Get all parts action types
     */
    public static String[] getPartsActionTypes() {
        return new String[] {
            ACTION_TYPE_PARTS_BY_CONTRACT_NUMBER,
            ACTION_TYPE_PARTS_BY_PART_NUMBER,
            ACTION_TYPE_PARTS_BY_FILTER,
            ACTION_TYPE_UPDATE_PART,
            ACTION_TYPE_UPDATE_PART_PRICE
        };
    }
    
    /**
     * Get all failed parts action types
     */
    public static String[] getFailedPartsActionTypes() {
        return new String[] {
            ACTION_TYPE_FAILED_PARTS_BY_CONTRACT_NUMBER,
            ACTION_TYPE_FAILED_PARTS_BY_FILTER
        };
    }
} 