package com.oracle.view.source;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CENTRALIZED TABLE COLUMN CONFIGURATION
 * 
 * This class serves as a single source of truth for all table column definitions.
 * It provides easy methods to add/remove columns based on data design changes
 * without modifying multiple files throughout the codebase.
 * 
 * USAGE:
 * - To add new columns: Use addColumn() methods
 * - To remove columns: Use removeColumn() methods  
 * - To get columns: Use getColumns() methods
 * - To validate columns: Use isValidColumn() methods
 * 
 * BENEFITS:
 * - Single point of maintenance
 * - Runtime column management
 * - Easy data design changes
 * - Consistent column validation
 * - Thread-safe operations
 */
public class TableColumnConfig {
    
    // Singleton instance for thread-safe access
    private static volatile TableColumnConfig instance;
    
    // Thread-safe column storage
    private final Map<String, Set<String>> tableColumns = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> businessTermMappings = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> fieldSynonyms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> displayableColumns = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> filterableColumns = new ConcurrentHashMap<>();
    
    // Table types
    public static final String TABLE_CONTRACTS = "HR.CCT_CONTRACTS_TMG";
    public static final String TABLE_PARTS = "HR.CCT_PARTS_TMG";
    public static final String TABLE_FAILED_PARTS = "HR.CCT_FAILED_PARTS_TMG";
    public static final String TABLE_CUSTOMERS = "HR.CCT_CUTSOMERS_TGM";
    public static final String TABLE_CONTRACT_CONTACTS = "HR.CCT_AWARD_CONTACTS_TMG";
    
    // Default column sets
    public static final Set<String> DEFAULT_CONTRACTS_COLMS = new HashSet<>(Arrays.asList(
        "AWARD_NUMBER", "CONTRACT_NAME", "CUSTOMER_NAME", "CUSTOMER_NUMBER", 
        "EFFECTIVE_DATE", "EXPIRATION_DATE"
    ));
    
    public static final Set<String> DEFAULT_PARTS_COLMS = new HashSet<>(Arrays.asList(
        "INVOICE_PART_NUMBER", "PRICE", "LEAD_TIME", "MOQ", "UOM", "STATUS", 
        "LOADED_CP_NUMBER", "ITEM_CLASSIFICATION"
    ));

    public static final Set<String> CHECL_LIST_COLUMNS =
        new HashSet<>(Arrays.asList("DATE_OF_SIGNATURE", "EXPIRATION_DATE", "PRICE_EXPIRATION_DATE", "QUATAR",
                                    "SYSTEM_LOADED_DATE"));


    public static final Set<String> DEFAULT_PARTS_FAILED_COLMS = new HashSet<>(Arrays.asList(
        "PART_NUMBER", "REASON", "ERROR_COLUMN", "CONTRACT_NO"
    ));
    
    // Column categories
    public static final String CATEGORY_DISPLAY = "DISPLAY";
    public static final String CATEGORY_FILTER = "FILTER";
    public static final String CATEGORY_BOTH = "BOTH";
    
    private TableColumnConfig() {
        initializeDefaultColumns();
    }
    
    /**
     * Get singleton instance
     */
    public static TableColumnConfig getInstance() {
        if (instance == null) {
            synchronized (TableColumnConfig.class) {
                if (instance == null) {
                    instance = new TableColumnConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize default columns from the original hardcoded definitions
     */
    private void initializeDefaultColumns() {
        // Initialize Contracts table columns
        Set<String> contractsColumns = new HashSet<>(Arrays.asList(
            "EXP_NOTIF_SENT_90", "EXP_NOTIF_SENT_60", "EXP_NOTIF_SENT_30", "EXP_NOTIF_FEEDBACK",
            "ADDL_OPPORTUNITIES", "MIN_INV_OBLIGATION", "CREATED_BY", "UPDATED_BY",
            "UPDATED_DATE", "IS_PROGRAM", "IS_HPP_UNPRICED_CONTRACT", "AWARD_NUMBER",
            "CONTRACT_NAME", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "ALTERNATE_CUSTOMERS",
            "EFFECTIVE_DATE", "EXPIRATION_DATE", "PRICE_EXPIRATION_DATE", "CONTRACT_LENGTH",
            "PAYMENT_TERMS", "INCOTERMS", "PROGRAM_INFORMATION", "CMI", "VMI", "BAILMENT",
            "CONSIGNMENT", "EDI", "MIN_MAX", "KITTING", "PL_3", "PL_4", "FSL_LOCATION",
            "VENDING_MACHINES", "SERVICE_FEE_APPLIES", "CURRENCY", "E_COMMERCE_ACCESS",
            "ULTIMATE_DESTINATION", "GT_25", "PART_FILE", "PMA_TSO_APPLIES", "DFAR_APPLIES",
            "ASL_APPLIES", "ASL_DETAIL", "STOCKING_STRATEGY", "LIABILITY_ON_INVESTMENT",
            "HPP_LANGUAGE", "CSM_LANGUAGE", "D_ITEM_LANGUAGE", "REBATE", "LINE_MIN",
            "ORDER_MIN", "EFFECTIVE_LOL", "PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER",
            "RIGHTS_OF_RETURN", "INCENTIVES_CREDITS", "CANCELLATION_PRIVILEGES",
            "BILL_AND_HOLD", "BUY_BACK", "CREATE_DATE", "PROCESS_FLAG", "OPPORTUNITY_NUMBER",
            "CONTRACT_TYPE", "COMPLETENESS_CHECK", "WAREHOUSE_INFO", "PROJECT_TYPE",
            "IS_TSO_PMA", "GROUP_TYPE", "PRICE_LIST", "TITLE", "DESCRIPTION", "COMMENTS",
            "STATUS", "IMPL_CODE", "S_ITEM_LANGUAGE", "EXTERNAL_CONTRACT_NUMBER",
            "ACCOUNT_TYPE", "COMPETITION", "EXISTING_CONTRACT_NUMBER", "EXISTING_CONTRACT_TYPE",
            "IS_FSL_REQ", "IS_SITE_VISIT_REQ", "LEGAL_FORMAT_TYPE", "CUSTOMER_FOCUS",
            "MOQS_AMORTIZE", "PLATFORM_INFO", "RETURN_PART_LIST_FORMAT", "SOURCE_PRODUCTS",
            "SUMMARY", "TARGET_MARGIN", "TOTAL_PART_COUNT", "CRF_ID"
        ));
        
        // Initialize Parts table columns
        Set<String> partsColumns = new HashSet<>(Arrays.asList(
            "FUTURE_PRICE2", "F_PRICE_EFFECTIVE_DATE2", "FUTURE_PRICE3",
            "F_PRICE_EFFECTIVE_DATE3", "DATE_LOADED", "COMMENTS", "CREATION_DATE", "CREATED_BY",
            "LAST_UPDATE_DATE", "LAST_UPDATED_BY", "AWARD_TAGS", "PREV_PRICE",
            "REPRICE_EFFECTIVE_DATE", "EXTERNAL_CONTRACT_NO", "EXTERNAL_LINE_NO", "PLANT",
            "VALUATION_TYPE", "OPPORTUNITY_NUMBER", "NSN_PART_NUMBER", "CSM_STATUS",
            "TEST_REPORTS_REQUIRED", "INCOTERMS", "INCOTERMS_LOCATION", "CSM_MONITORED",
            "AWARD_ID", "LINE_NO", "INVOICE_PART_NUMBER", "EAU", "UOM", "PRICE",
            "ITEM_CLASSIFICATION", "LEAD_TIME", "STATUS", "AWARD_REP_COMMENTS",
            "CUSTOMER_REFERENCE", "ASL_CODES", "PRIME", "LOADED_CP_NUMBER", "FUTURE_PRICE",
            "F_PRICE_EFFECTIVE_DATE", "EFFECTIVE_DATE", "PART_EXPIRATION_DATE", "MOQ",
            "SAP_NUMBER", "TOT_CON_QTY_REQ", "QUOTE_COST", "QUOTE_COST_SOURCE",
            "PURCHASE_COMMENTS", "SALES_COMMENTS", "CUSTOMER_RESPONSE", "PL4_VENDOR",
            "APPLICABLE_CONTRACT", "CUST_EXCLUDE_PN", "PLANNING_COMMENTS"
        ));
        
        // Initialize Failed Parts table columns
        Set<String> failedPartsColumns = new HashSet<>(Arrays.asList(
        "BUSINESS_RULE_VIOLATION",
        "CONTRACT_NO",
        "ERROR_COLUMN",
        "LINE_NO",
        "LOADING_ERROR",
        "PART_NUMBER",
        "PROCESSING_ERROR",
        "REASON",
        "VALIDATION_ERROR"
        ));
        
        // Store columns
        tableColumns.put(TABLE_CONTRACTS, contractsColumns);
        tableColumns.put(TABLE_PARTS, partsColumns);
        tableColumns.put(TABLE_FAILED_PARTS, failedPartsColumns);
        
        // Initialize business term mappings
        initializeBusinessTermMappings();
        
        // Initialize field synonyms
        initializeFieldSynonyms();
        
        // Initialize displayable and filterable columns
        initializeColumnCategories();
    }
    
    /**
     * Initialize business term mappings
     */
    private void initializeBusinessTermMappings() {
        Map<String, String> contractMappings = new HashMap<>();
        contractMappings.put("contract_number", "AWARD_NUMBER");
        contractMappings.put("contract_id", "AWARD_NUMBER");
        contractMappings.put("award_number", "AWARD_NUMBER");
        contractMappings.put("award_id", "AWARD_NUMBER");
        contractMappings.put("contract", "AWARD_NUMBER");
        contractMappings.put("customer_number", "CUSTOMER_NUMBER");
        contractMappings.put("customer_id", "CUSTOMER_NUMBER");
        contractMappings.put("customer", "CUSTOMER_NAME");
        contractMappings.put("client", "CUSTOMER_NAME");
        contractMappings.put("effective_date", "EFFECTIVE_DATE");
        contractMappings.put("expiration_date", "EXPIRATION_DATE");
        contractMappings.put("creation_date", "CREATE_DATE");
        contractMappings.put("created_date", "CREATE_DATE");
        contractMappings.put("status", "STATUS");
        contractMappings.put("state", "STATUS");
        contractMappings.put("condition", "STATUS");
        
        Map<String, String> partsMappings = new HashMap<>();
        partsMappings.put("part_number", "INVOICE_PART_NUMBER");
        partsMappings.put("part_id", "INVOICE_PART_NUMBER");
        partsMappings.put("invoice_part_number", "INVOICE_PART_NUMBER");
        partsMappings.put("part", "INVOICE_PART_NUMBER");
        partsMappings.put("pricing", "PRICE");
        partsMappings.put("price", "PRICE");
        partsMappings.put("cost", "PRICE");
        partsMappings.put("minimum order", "MOQ");
        partsMappings.put("min order", "MOQ");
        partsMappings.put("min order qty", "MOQ");
        partsMappings.put("minimum order quantity", "MOQ");
        partsMappings.put("moq", "MOQ");
        partsMappings.put("unit of measure", "UOM");
        partsMappings.put("unit measure", "UOM");
        partsMappings.put("uom", "UOM");
        partsMappings.put("lead time", "LEAD_TIME");
        partsMappings.put("leadtime", "LEAD_TIME");
        partsMappings.put("delivery time", "LEAD_TIME");
        
        businessTermMappings.put(TABLE_CONTRACTS, contractMappings);
        businessTermMappings.put(TABLE_PARTS, partsMappings);
        businessTermMappings.put(TABLE_FAILED_PARTS, new HashMap<>());
        // --- Add for opportunities and customers ---
        Map<String, String> customerMappings = new HashMap<>();
        customerMappings.put("customer_number", "CUSTOMER_NUMBER");
        customerMappings.put("customer_no", "CUSTOMER_NUMBER");
        customerMappings.put("customer id", "CUSTOMER_NUMBER");
        customerMappings.put("account_number", "CUSTOMER_NUMBER");
        customerMappings.put("account no", "CUSTOMER_NUMBER");
        customerMappings.put("account id", "CUSTOMER_NUMBER");
        customerMappings.put("customer_name", "CUSTOMER_NAME");
        customerMappings.put("customer", "CUSTOMER_NAME");
        customerMappings.put("client", "CUSTOMER_NAME");
        customerMappings.put("client_name", "CUSTOMER_NAME");
        customerMappings.put("buyer", "CUSTOMER_NAME");
        businessTermMappings.put(TABLE_CUSTOMERS, customerMappings);
        Map<String, String> oppMappings = new HashMap<>();
        oppMappings.put("opportunity_number", "OPPORTUNITY_NUMBER");
        oppMappings.put("opportunity no", "OPPORTUNITY_NUMBER");
        oppMappings.put("oppty_number", "OPPORTUNITY_NUMBER");
        oppMappings.put("oppty no", "OPPORTUNITY_NUMBER");
        oppMappings.put("opportunity id", "OPPORTUNITY_NUMBER");
        oppMappings.put("oppty id", "OPPORTUNITY_NUMBER");
        oppMappings.put("opp number", "OPPORTUNITY_NUMBER");
        oppMappings.put("opp no", "OPPORTUNITY_NUMBER");
        oppMappings.put("opportunity_name", "OPPORTUNITY_NAME");
        oppMappings.put("opportunity", "OPPORTUNITY_NAME");
        oppMappings.put("oppty", "OPPORTUNITY_NAME");
        oppMappings.put("opp name", "OPPORTUNITY_NAME");
        oppMappings.put("opportunity title", "OPPORTUNITY_NAME");
        oppMappings.put("opportunity description", "OPPORTUNITY_NAME");
        businessTermMappings.put("HR.CCT_OPPORTUNITIES_TMG", oppMappings);
    }
    
    /**
     * Initialize field synonyms
     */
    private void initializeFieldSynonyms() {
        Map<String, String> contractSynonyms = new HashMap<>();
        contractSynonyms.put("contract_name", "CONTRACT_NAME");
        contractSynonyms.put("contract_title", "CONTRACT_NAME");
        contractSynonyms.put("agreement_name", "CONTRACT_NAME");
        contractSynonyms.put("deal_name", "CONTRACT_NAME");
        contractSynonyms.put("customer_name", "CUSTOMER_NAME");
        contractSynonyms.put("client_name", "CUSTOMER_NAME");
        contractSynonyms.put("account_name", "CUSTOMER_NAME");
        contractSynonyms.put("buyer_name", "CUSTOMER_NAME");
        contractSynonyms.put("start_date", "EFFECTIVE_DATE");
        contractSynonyms.put("begin_date", "EFFECTIVE_DATE");
        contractSynonyms.put("commencement_date", "EFFECTIVE_DATE");
        contractSynonyms.put("end_date", "EXPIRATION_DATE");
        contractSynonyms.put("finish_date", "EXPIRATION_DATE");
        contractSynonyms.put("termination_date", "EXPIRATION_DATE");
        
        Map<String, String> partsSynonyms = new HashMap<>();
        partsSynonyms.put("part_code", "INVOICE_PART_NUMBER");
        partsSynonyms.put("item_number", "INVOICE_PART_NUMBER");
        partsSynonyms.put("product_number", "INVOICE_PART_NUMBER");
        partsSynonyms.put("component_number", "INVOICE_PART_NUMBER");
        partsSynonyms.put("unit_price", "PRICE");
        partsSynonyms.put("cost_price", "PRICE");
        partsSynonyms.put("selling_price", "PRICE");
        partsSynonyms.put("list_price", "PRICE");
        partsSynonyms.put("minimum_order", "MOQ");
        partsSynonyms.put("min_order_qty", "MOQ");
        partsSynonyms.put("minimum_quantity", "MOQ");
        partsSynonyms.put("min_qty", "MOQ");
        partsSynonyms.put("delivery_time", "LEAD_TIME");
        partsSynonyms.put("shipping_time", "LEAD_TIME");
        partsSynonyms.put("fulfillment_time", "LEAD_TIME");
        partsSynonyms.put("processing_time", "LEAD_TIME");
        partsSynonyms.put("unit_of_measure", "UOM");
        partsSynonyms.put("unit_measure", "UOM");
        partsSynonyms.put("measurement_unit", "UOM");
        partsSynonyms.put("unit", "UOM");
        
        fieldSynonyms.put(TABLE_CONTRACTS, contractSynonyms);
        fieldSynonyms.put(TABLE_PARTS, partsSynonyms);
        fieldSynonyms.put(TABLE_FAILED_PARTS, new HashMap<>());
        // --- Add for customers and opportunities ---
        Map<String, String> customerSynonyms = new HashMap<>();
        customerSynonyms.put("customer_number", "CUSTOMER_NUMBER");
        customerSynonyms.put("customer_no", "CUSTOMER_NUMBER");
        customerSynonyms.put("customer id", "CUSTOMER_NUMBER");
        customerSynonyms.put("account_number", "CUSTOMER_NUMBER");
        customerSynonyms.put("account no", "CUSTOMER_NUMBER");
        customerSynonyms.put("account id", "CUSTOMER_NUMBER");
        customerSynonyms.put("customer_name", "CUSTOMER_NAME");
        customerSynonyms.put("customer", "CUSTOMER_NAME");
        customerSynonyms.put("client", "CUSTOMER_NAME");
        customerSynonyms.put("client_name", "CUSTOMER_NAME");
        customerSynonyms.put("buyer", "CUSTOMER_NAME");
        fieldSynonyms.put(TABLE_CUSTOMERS, customerSynonyms);
        Map<String, String> oppSynonyms = new HashMap<>();
        oppSynonyms.put("opportunity_number", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("opportunity no", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("oppty_number", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("oppty no", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("opportunity id", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("oppty id", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("opp number", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("opp no", "OPPORTUNITY_NUMBER");
        oppSynonyms.put("opportunity_name", "OPPORTUNITY_NAME");
        oppSynonyms.put("opportunity", "OPPORTUNITY_NAME");
        oppSynonyms.put("oppty", "OPPORTUNITY_NAME");
        oppSynonyms.put("opp name", "OPPORTUNITY_NAME");
        oppSynonyms.put("opportunity title", "OPPORTUNITY_NAME");
        oppSynonyms.put("opportunity description", "OPPORTUNITY_NAME");
        fieldSynonyms.put("HR.CCT_OPPORTUNITIES_TMG", oppSynonyms);
    }
    
    /**
     * Initialize column categories (displayable vs filterable)
     */
    private void initializeColumnCategories() {
        // Default displayable columns for contracts
        Set<String> contractDisplayable = new HashSet<>(Arrays.asList(
            "CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE", "STATUS",
            "CONTRACT_TYPE", "PAYMENT_TERMS", "INCOTERMS", "CREATE_DATE", "AWARD_NUMBER"
        ));
        
        // Default displayable columns for parts
        Set<String> partsDisplayable = new HashSet<>(Arrays.asList(
            "INVOICE_PART_NUMBER", "PRICE", "LEAD_TIME", "MOQ", "UOM", "STATUS",
            "ITEM_CLASSIFICATION", "EAU", "PART_EXPIRATION_DATE"
        ));
        
        // Default displayable columns for failed parts
        Set<String> failedPartsDisplayable = new HashSet<>(Arrays.asList(
            "PART_NUMBER", "ERROR_COLUMN", "REASON", "LOADED_CP_NUMBER"
        ));
        
        displayableColumns.put(TABLE_CONTRACTS, contractDisplayable);
        displayableColumns.put(TABLE_PARTS, partsDisplayable);
        displayableColumns.put(TABLE_FAILED_PARTS, failedPartsDisplayable);
        
        // All columns are filterable by default
        filterableColumns.put(TABLE_CONTRACTS, new HashSet<>(tableColumns.get(TABLE_CONTRACTS)));
        filterableColumns.put(TABLE_PARTS, new HashSet<>(tableColumns.get(TABLE_PARTS)));
        filterableColumns.put(TABLE_FAILED_PARTS, new HashSet<>(tableColumns.get(TABLE_FAILED_PARTS)));
    }
    
    // ============================================================================
    // COLUMN MANAGEMENT METHODS
    // ============================================================================
    
    /**
     * Add a new column to a table
     */
    public void addColumn(String tableType, String columnName) {
        addColumn(tableType, columnName, CATEGORY_BOTH);
    }
    
    /**
     * Add a new column to a table with specific category
     */
    public void addColumn(String tableType, String columnName, String category) {
        if (tableType == null || columnName == null) {
            throw new IllegalArgumentException("Table type and column name cannot be null");
        }
        
        String normalizedColumnName = columnName.toUpperCase();
        String normalizedTableType = tableType.toUpperCase();
        
        // Add to main column set
        tableColumns.computeIfAbsent(normalizedTableType, k -> new HashSet<>()).add(normalizedColumnName);
        
        // Add to appropriate category
        switch (category.toUpperCase()) {
            case CATEGORY_DISPLAY:
                displayableColumns.computeIfAbsent(normalizedTableType, k -> new HashSet<>()).add(normalizedColumnName);
                break;
            case CATEGORY_FILTER:
                filterableColumns.computeIfAbsent(normalizedTableType, k -> new HashSet<>()).add(normalizedColumnName);
                break;
            case CATEGORY_BOTH:
            default:
                displayableColumns.computeIfAbsent(normalizedTableType, k -> new HashSet<>()).add(normalizedColumnName);
                filterableColumns.computeIfAbsent(normalizedTableType, k -> new HashSet<>()).add(normalizedColumnName);
                break;
        }
    }
    
    /**
     * Add multiple columns to a table
     */
    public void addColumns(String tableType, String... columnNames) {
        for (String columnName : columnNames) {
            addColumn(tableType, columnName);
        }
    }
    
    /**
     * Remove a column from a table
     */
    public void removeColumn(String tableType, String columnName) {
        if (tableType == null || columnName == null) {
            throw new IllegalArgumentException("Table type and column name cannot be null");
        }
        
        String normalizedColumnName = columnName.toUpperCase();
        String normalizedTableType = tableType.toUpperCase();
        
        // Remove from all sets
        tableColumns.get(normalizedTableType).remove(normalizedColumnName);
        displayableColumns.get(normalizedTableType).remove(normalizedColumnName);
        filterableColumns.get(normalizedTableType).remove(normalizedColumnName);
        
        // Remove from business term mappings
        businessTermMappings.get(normalizedTableType).values().removeIf(normalizedColumnName::equals);
        
        // Remove from field synonyms
        fieldSynonyms.get(normalizedTableType).values().removeIf(normalizedColumnName::equals);
    }
    
    /**
     * Remove multiple columns from a table
     */
    public void removeColumns(String tableType, String... columnNames) {
        for (String columnName : columnNames) {
            removeColumn(tableType, columnName);
        }
    }
    
    // ============================================================================
    // BUSINESS TERM MAPPING METHODS
    // ============================================================================
    
    /**
     * Add a business term mapping
     */
    public void addBusinessTermMapping(String tableType, String businessTerm, String columnName) {
        if (tableType == null || businessTerm == null || columnName == null) {
            throw new IllegalArgumentException("All parameters cannot be null");
        }
        
        String normalizedTableType = tableType.toUpperCase();
        String normalizedColumnName = columnName.toUpperCase();
        
        // Verify column exists
        if (!isValidColumn(normalizedTableType, normalizedColumnName)) {
            throw new IllegalArgumentException("Column " + normalizedColumnName + " does not exist in table " + normalizedTableType);
        }
        
        businessTermMappings.computeIfAbsent(normalizedTableType, k -> new HashMap<>())
                           .put(businessTerm.toLowerCase(), normalizedColumnName);
    }
    
    /**
     * Remove a business term mapping
     */
    public void removeBusinessTermMapping(String tableType, String businessTerm) {
        if (tableType == null || businessTerm == null) {
            throw new IllegalArgumentException("Table type and business term cannot be null");
        }
        
        String normalizedTableType = tableType.toUpperCase();
        businessTermMappings.get(normalizedTableType).remove(businessTerm.toLowerCase());
    }
    
    /**
     * Get column name for a business term
     */
    public String getColumnForBusinessTerm(String tableType, String businessTerm) {
        if (tableType == null || businessTerm == null) {
            return null;
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Map<String, String> mappings = businessTermMappings.get(normalizedTableType);
        
        if (mappings != null) {
            return mappings.get(businessTerm.toLowerCase());
        }
        
        return null;
    }
    
    // ============================================================================
    // FIELD SYNONYM METHODS
    // ============================================================================
    
    /**
     * Add a field synonym
     */
    public void addFieldSynonym(String tableType, String synonym, String columnName) {
        if (tableType == null || synonym == null || columnName == null) {
            throw new IllegalArgumentException("All parameters cannot be null");
        }
        
        String normalizedTableType = tableType.toUpperCase();
        String normalizedColumnName = columnName.toUpperCase();
        
        // Verify column exists
        if (!isValidColumn(normalizedTableType, normalizedColumnName)) {
            throw new IllegalArgumentException("Column " + normalizedColumnName + " does not exist in table " + normalizedTableType);
        }
        
        fieldSynonyms.computeIfAbsent(normalizedTableType, k -> new HashMap<>())
                     .put(synonym.toLowerCase(), normalizedColumnName);
    }
    
    /**
     * Remove a field synonym
     */
    public void removeFieldSynonym(String tableType, String synonym) {
        if (tableType == null || synonym == null) {
            throw new IllegalArgumentException("Table type and synonym cannot be null");
        }
        
        String normalizedTableType = tableType.toUpperCase();
        fieldSynonyms.get(normalizedTableType).remove(synonym.toLowerCase());
    }
    
    /**
     * Get column name for a field synonym
     */
    public String getColumnForSynonym(String tableType, String synonym) {
        if (tableType == null || synonym == null) {
            return null;
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Map<String, String> synonyms = fieldSynonyms.get(normalizedTableType);
        
        if (synonyms != null) {
            return synonyms.get(synonym.toLowerCase());
        }
        
        return null;
    }
    
    /**
     * Get column name for a field synonym with fuzzy matching (Levenshtein distance)
     */
    public String getColumnForSynonymFuzzy(String tableType, String synonym) {
        String exact = getColumnForSynonym(tableType, synonym);
        if (exact != null) return exact;
        Map<String, String> synonyms = getFieldSynonyms(tableType);
        int minDist = Integer.MAX_VALUE;
        String bestMatch = null;
        for (String key : synonyms.keySet()) {
            int dist = levenshteinDistance(synonym.toLowerCase(), key.toLowerCase());
            if (dist < minDist && dist <= 2) { // threshold can be tuned
                minDist = dist;
                bestMatch = key;
            }
        }
        return bestMatch != null ? synonyms.get(bestMatch) : null;
    }
    /**
     * Levenshtein distance implementation
     */
    private int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
    
    // ============================================================================
    // VALIDATION AND QUERY METHODS
    // ============================================================================
    
    /**
     * Check if a column exists in a table
     */
    public boolean isValidColumn(String tableType, String columnName) {
        if (tableType == null || columnName == null) {
            return false;
        }
        
        String normalizedTableType = tableType.toUpperCase();
        String normalizedColumnName = columnName.toUpperCase();
        
        Set<String> columns = tableColumns.get(normalizedTableType);
        return columns != null && columns.contains(normalizedColumnName);
    }
    
    /**
     * Check if a column is displayable
     */
    public boolean isDisplayableColumn(String tableType, String columnName) {
        if (tableType == null || columnName == null) {
            return false;
        }
        
        String normalizedTableType = tableType.toUpperCase();
        String normalizedColumnName = columnName.toUpperCase();
        
        Set<String> displayable = displayableColumns.get(normalizedTableType);
        return displayable != null && displayable.contains(normalizedColumnName);
    }
    
    /**
     * Check if a column is filterable
     */
    public boolean isFilterableColumn(String tableType, String columnName) {
        if (tableType == null || columnName == null) {
            return false;
        }
        
        String normalizedTableType = tableType.toUpperCase();
        String normalizedColumnName = columnName.toUpperCase();
        
        Set<String> filterable = filterableColumns.get(normalizedTableType);
        return filterable != null && filterable.contains(normalizedColumnName);
    }
    
    /**
     * Get all columns for a table
     */
    public Set<String> getColumns(String tableType) {
        if (tableType == null) {
            return new HashSet<>();
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Set<String> columns = tableColumns.get(normalizedTableType);
        return columns != null ? new HashSet<>(columns) : new HashSet<>();
    }
    
    /**
     * Get displayable columns for a table
     */
    public Set<String> getDisplayableColumns(String tableType) {
        if (tableType == null) {
            return new HashSet<>();
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Set<String> displayable = displayableColumns.get(normalizedTableType);
        return displayable != null ? new HashSet<>(displayable) : new HashSet<>();
    }
    
    /**
     * Get filterable columns for a table
     */
    public Set<String> getFilterableColumns(String tableType) {
        if (tableType == null) {
            return new HashSet<>();
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Set<String> filterable = filterableColumns.get(normalizedTableType);
        return filterable != null ? new HashSet<>(filterable) : new HashSet<>();
    }
    
    /**
     * Get all business term mappings for a table
     */
    public Map<String, String> getBusinessTermMappings(String tableType) {
        if (tableType == null) {
            return new HashMap<>();
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Map<String, String> mappings = businessTermMappings.get(normalizedTableType);
        return mappings != null ? new HashMap<>(mappings) : new HashMap<>();
    }
    
    /**
     * Get all field synonyms for a table
     */
    public Map<String, String> getFieldSynonyms(String tableType) {
        if (tableType == null) {
            return new HashMap<>();
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Map<String, String> synonyms = fieldSynonyms.get(normalizedTableType);
        return synonyms != null ? new HashMap<>(synonyms) : new HashMap<>();
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Get table statistics
     */
    public Map<String, Object> getTableStatistics(String tableType) {
        if (tableType == null) {
            return new HashMap<>();
        }
        
        String normalizedTableType = tableType.toUpperCase();
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> allColumns = getColumns(normalizedTableType);
        Set<String> displayable = getDisplayableColumns(normalizedTableType);
        Set<String> filterable = getFilterableColumns(normalizedTableType);
        Map<String, String> businessTerms = getBusinessTermMappings(normalizedTableType);
        Map<String, String> synonyms = getFieldSynonyms(normalizedTableType);
        
        stats.put("totalColumns", allColumns.size());
        stats.put("displayableColumns", displayable.size());
        stats.put("filterableColumns", filterable.size());
        stats.put("businessTermMappings", businessTerms.size());
        stats.put("fieldSynonyms", synonyms.size());
        stats.put("allColumns", allColumns);
        stats.put("displayableColumns", displayable);
        stats.put("filterableColumns", filterable);
        stats.put("businessTermMappings", businessTerms);
        stats.put("fieldSynonyms", synonyms);
        
        return stats;
    }
    
    /**
     * Reset to default configuration
     */
    public void resetToDefaults() {
        tableColumns.clear();
        businessTermMappings.clear();
        fieldSynonyms.clear();
        displayableColumns.clear();
        filterableColumns.clear();
        initializeDefaultColumns();
    }
    
    /**
     * Get all table types
     */
    public Set<String> getTableTypes() {
        return new HashSet<>(tableColumns.keySet());
    }
    
    /**
     * Check if table exists
     */
    public boolean tableExists(String tableType) {
        if (tableType == null) {
            return false;
        }
        
        String normalizedTableType = tableType.toUpperCase();
        return tableColumns.containsKey(normalizedTableType);
    }
    
    /**
     * Get configuration summary
     */
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        for (String tableType : getTableTypes()) {
            summary.put(tableType, getTableStatistics(tableType));
        }
        
        return summary;
    }
    
    /**
     * Get table name for the given table type
     */
    public String getTableName(String tableType) {
        switch (tableType) {
            case TABLE_CONTRACTS:
                return "HR.CCT_CONTRACTS_TMG";
            case TABLE_PARTS:
                return "HR.CCT_PARTS_TMG";
            case TABLE_FAILED_PARTS:
                return "HR.CCT_PARTS_FAILED_TMG";
            case TABLE_CUSTOMERS:
                return "HR.CCT_CUSTOMERS_TMG";
            case TABLE_CONTRACT_CONTACTS:
                return "HR.CCT_CONTRACT_CONTACTS_TMG";
            default:
                return "HR.CCT_CONTRACTS_TMG"; // Default fallback
        }
    }
    
    /**
     * Get primary key column for the given table type
     */
    public String getPrimaryKeyColumn(String tableType) {
        switch (tableType) {
            case TABLE_CONTRACTS:
                return "AWARD_NUMBER";
            case TABLE_PARTS:
                return "LOADED_CP_NUMBER";
            case TABLE_FAILED_PARTS:
                return "CONTRACT_NO";
            case TABLE_CUSTOMERS:
                return "CUSTOMER_NO";
            case TABLE_CONTRACT_CONTACTS:
                return "AWARD_NUMBER";
            default:
                return "AWARD_NUMBER"; // Default fallback
        }
    }
    
    /**
     * Get display name for a column
     */
    public String getDisplayName(String tableType, String columnName) {
        // Simple mapping - can be enhanced with a proper display name mapping
        if (columnName == null) return "Unknown";
        
        // Convert column name to display name
        String displayName = columnName.replace("_", " ").toLowerCase();
        
        // Capitalize first letter of each word
        String[] words = displayName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
} 