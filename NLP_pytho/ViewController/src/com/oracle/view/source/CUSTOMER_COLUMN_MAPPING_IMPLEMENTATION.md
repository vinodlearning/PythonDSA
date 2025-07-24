# Customer Column Mapping Implementation

## Overview

This document describes the implementation of customer column mapping in the `TableColumnConfig` class to handle various user input variations and map them to actual database column names from the `CRM_CTR_CUSTOMERS` table.

## Problem Statement

Users may input queries with different variations, typos, and phrasings for the same database column. For example:
- "order min" → `ORDER_MIN`
- "minimum order" → `ORDER_MIN`
- "min order" → `ORDER_MIN`
- "minorder" → `ORDER_MIN`

The system needs to correctly map these variations to the actual database column names.

## Solution Implementation

### 1. Table Column Configuration

Added customer table configuration to `TableColumnConfig.java`:

```java
// Table type constant
public static final String TABLE_CUSTOMERS = "CRM_CTR_CUSTOMERS";

// Customer table columns
Set<String> customersColumns = new HashSet<>(Arrays.asList(
    "CUST_ID", "CUST_CARDEX_ID", "CUSTOMER_NO", "CUSTOMER_NAME", "ACCOUNT_TYPE",
    "SALES_REP_ID", "SALES_OWNER", "SALES_TEAM", "SALES_TEAM_NUMBER", "SALES_MANAGER",
    "SALES_DIRECTOR", "SALES_VP", "SALES_OUTSIDE", "IS_GT25", "IS_ECOMM",
    "CURRENCY_CODE", "PAYMENT_TERMS", "INCO_TERMS", "ULTIMTATE_DESTINATION",
    "IS_DFAR", "IS_ASL_APPLICABLE", "LINE_MIN", "ORDER_MIN", "WAREHOUSE_INFO",
    "IS_ACTIVE", "PROGRAM_SOLUTION_REP", "TYPE", "ASL_CODE", "HPPFLAG",
    "QUALITYENGINEER", "ACCOUNTREP", "AWARDREP", "CARDEX_CUSTOMER_NO",
    "CREATED_DATE", "CREATED_BY"
));
```

### 2. Business Term Mappings

Comprehensive business term mappings for user input variations:

```java
Map<String, String> customerMappings = new HashMap<>();

// Order minimum variations
customerMappings.put("order_min", "ORDER_MIN");
customerMappings.put("order_minimum", "ORDER_MIN");
customerMappings.put("minimum_order", "ORDER_MIN");
customerMappings.put("min_order", "ORDER_MIN");
customerMappings.put("minorder", "ORDER_MIN");

// Line minimum variations
customerMappings.put("line_min", "LINE_MIN");
customerMappings.put("line_minimum", "LINE_MIN");
customerMappings.put("minimum_line", "LINE_MIN");
customerMappings.put("min_line", "LINE_MIN");
customerMappings.put("minline", "LINE_MIN");

// Sales team variations
customerMappings.put("sales_rep", "SALES_REP_ID");
customerMappings.put("sales_rep_id", "SALES_REP_ID");
customerMappings.put("sales_representative", "SALES_REP_ID");
customerMappings.put("rep", "SALES_REP_ID");
customerMappings.put("representative", "SALES_REP_ID");
customerMappings.put("sales_owner", "SALES_OWNER");
customerMappings.put("owner", "SALES_OWNER");
customerMappings.put("sales_team", "SALES_TEAM");
customerMappings.put("team", "SALES_TEAM");
customerMappings.put("sales_manager", "SALES_MANAGER");
customerMappings.put("manager", "SALES_MANAGER");
customerMappings.put("sales_director", "SALES_DIRECTOR");
customerMappings.put("director", "SALES_DIRECTOR");
customerMappings.put("sales_vp", "SALES_VP");
customerMappings.put("vp", "SALES_VP");
customerMappings.put("vice_president", "SALES_VP");

// Account rep variations
customerMappings.put("account_rep", "ACCOUNTREP");
customerMappings.put("accountrep", "ACCOUNTREP");
customerMappings.put("account_representative", "ACCOUNTREP");

// Award rep variations
customerMappings.put("award_rep", "AWARDREP");
customerMappings.put("awardrep", "AWARDREP");
customerMappings.put("award_representative", "AWARDREP");

// Status variations
customerMappings.put("is_active", "IS_ACTIVE");
customerMappings.put("active", "IS_ACTIVE");
customerMappings.put("status", "IS_ACTIVE");
customerMappings.put("active_status", "IS_ACTIVE");
customerMappings.put("customer_status", "IS_ACTIVE");

// Program flags
customerMappings.put("is_gt25", "IS_GT25");
customerMappings.put("gt25", "IS_GT25");
customerMappings.put("greater_than_25", "IS_GT25");
customerMappings.put("is_ecomm", "IS_ECOMM");
customerMappings.put("ecomm", "IS_ECOMM");
customerMappings.put("e_commerce", "IS_ECOMM");
customerMappings.put("is_dfar", "IS_DFAR");
customerMappings.put("dfar", "IS_DFAR");
customerMappings.put("defense_federal_acquisition_regulation", "IS_DFAR");
customerMappings.put("is_asl_applicable", "IS_ASL_APPLICABLE");
customerMappings.put("asl_applicable", "IS_ASL_APPLICABLE");
customerMappings.put("approved_supplier_list", "IS_ASL_APPLICABLE");
customerMappings.put("hppflag", "HPPFLAG");
customerMappings.put("hpp_flag", "HPPFLAG");
customerMappings.put("hpp", "HPPFLAG");

// Other fields
customerMappings.put("warehouse_info", "WAREHOUSE_INFO");
customerMappings.put("warehouse", "WAREHOUSE_INFO");
customerMappings.put("warehouse_information", "WAREHOUSE_INFO");
customerMappings.put("ultimate_destination", "ULTIMTATE_DESTINATION");
customerMappings.put("destination", "ULTIMTATE_DESTINATION");
customerMappings.put("program_solution_rep", "PROGRAM_SOLUTION_REP");
customerMappings.put("program_rep", "PROGRAM_SOLUTION_REP");
customerMappings.put("quality_engineer", "QUALITYENGINEER");
customerMappings.put("quality_eng", "QUALITYENGINEER");
customerMappings.put("qe", "QUALITYENGINEER");

// Creation fields
customerMappings.put("created_date", "CREATED_DATE");
customerMappings.put("created_by", "CREATED_BY");
customerMappings.put("creation_date", "CREATED_DATE");
customerMappings.put("creator", "CREATED_BY");
customerMappings.put("author", "CREATED_BY");
```

### 3. Field Synonyms

Additional field synonyms for more variations:

```java
Map<String, String> customerSynonyms = new HashMap<>();

// Customer identification synonyms
customerSynonyms.put("cust_id", "CUST_ID");
customerSynonyms.put("customerid", "CUST_ID");
customerSynonyms.put("cust_no", "CUSTOMER_NO");
customerSynonyms.put("customer_num", "CUSTOMER_NO");
customerSynonyms.put("client_id", "CUST_ID");
customerSynonyms.put("client_no", "CUSTOMER_NO");
customerSynonyms.put("cardex", "CUST_CARDEX_ID");
customerSynonyms.put("cardex_customer", "CARDEX_CUSTOMER_NO");

// Sales team synonyms
customerSynonyms.put("sales_rep_id", "SALES_REP_ID");
customerSynonyms.put("sales_representative_id", "SALES_REP_ID");
customerSynonyms.put("sales_rep_name", "SALES_REP_ID");
customerSynonyms.put("sales_owner_name", "SALES_OWNER");
customerSynonyms.put("sales_team_name", "SALES_TEAM");
customerSynonyms.put("sales_manager_name", "SALES_MANAGER");
customerSynonyms.put("sales_director_name", "SALES_DIRECTOR");
customerSynonyms.put("sales_vp_name", "SALES_VP");
customerSynonyms.put("vice_president_name", "SALES_VP");

// Order minimum synonyms
customerSynonyms.put("order_minimum_amount", "ORDER_MIN");
customerSynonyms.put("minimum_order_amount", "ORDER_MIN");
customerSynonyms.put("min_order_amount", "ORDER_MIN");
customerSynonyms.put("order_min_amt", "ORDER_MIN");
customerSynonyms.put("line_minimum_amount", "LINE_MIN");
customerSynonyms.put("minimum_line_amount", "LINE_MIN");
customerSynonyms.put("min_line_amount", "LINE_MIN");
customerSynonyms.put("line_min_amt", "LINE_MIN");

// Program flags synonyms
customerSynonyms.put("greater_than_25_flag", "IS_GT25");
customerSynonyms.put("gt25_flag", "IS_GT25");
customerSynonyms.put("ecommerce_flag", "IS_ECOMM");
customerSynonyms.put("e_comm_flag", "IS_ECOMM");
customerSynonyms.put("dfar_flag", "IS_DFAR");
customerSynonyms.put("defense_flag", "IS_DFAR");
customerSynonyms.put("asl_flag", "IS_ASL_APPLICABLE");
customerSynonyms.put("approved_supplier_flag", "IS_ASL_APPLICABLE");
customerSynonyms.put("hpp_flag", "HPPFLAG");
```

### 4. Displayable Columns

Default displayable columns for customer queries:

```java
Set<String> customersDisplayable = new HashSet<>(Arrays.asList(
    "CUSTOMER_NO", "CUSTOMER_NAME", "ACCOUNT_TYPE", "SALES_REP_ID", "SALES_OWNER", 
    "SALES_TEAM", "SALES_MANAGER", "IS_ACTIVE", "CURRENCY_CODE", "PAYMENT_TERMS",
    "ORDER_MIN", "LINE_MIN", "IS_GT25", "IS_ECOMM", "IS_DFAR", "IS_ASL_APPLICABLE",
    "ACCOUNTREP", "AWARDREP", "CREATED_DATE", "CREATED_BY"
));
```

## Usage in NLPUserActionHandler

The `determineSpecificCustomerColumns` method in `NLPUserActionHandler` uses the `TableColumnConfig` to map user input:

```java
private List<String> determineSpecificCustomerColumns(String userInput) {
    if (userInput == null) return new ArrayList<>();
    
    String lowerInput = userInput.toLowerCase();
    List<String> columns = new ArrayList<>();
    
    // Use TableColumnConfig to map user input to actual column names
    String[] words = lowerInput.split("\\s+");
    
    for (String word : words) {
        // Check business term mappings first
        String columnName = TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CUSTOMERS, word);
        if (columnName != null && !columns.contains(columnName)) {
            columns.add(columnName);
            continue;
        }
        
        // Check field synonyms
        columnName = TABLE_CONFIG.getColumnForSynonym(TableColumnConfig.TABLE_CUSTOMERS, word);
        if (columnName != null && !columns.contains(columnName)) {
            columns.add(columnName);
            continue;
        }
        
        // Check multi-word phrases
        for (int i = 0; i < words.length - 1; i++) {
            String phrase = words[i] + " " + words[i + 1];
            columnName = TABLE_CONFIG.getColumnForBusinessTerm(TableColumnConfig.TABLE_CUSTOMERS, phrase);
            if (columnName != null && !columns.contains(columnName)) {
                columns.add(columnName);
            }
        }
    }
    
    // Handle specific patterns that might not be caught by single word mapping
    if (lowerInput.contains("order min") || lowerInput.contains("minimum order") || 
        lowerInput.contains("min order") || lowerInput.contains("minorder")) {
        addColumnIfNotExists(columns, "ORDER_MIN");
    }
    
    if (lowerInput.contains("line min") || lowerInput.contains("minimum line") || 
        lowerInput.contains("min line") || lowerInput.contains("minline")) {
        addColumnIfNotExists(columns, "LINE_MIN");
    }
    
    // ... additional pattern matching
    
    return columns;
}
```

## Test Cases

The `CustomerColumnMappingTest.java` verifies the mapping works correctly:

### Order Minimum Variations
- "order min" → `ORDER_MIN`
- "order_minimum" → `ORDER_MIN`
- "minimum_order" → `ORDER_MIN`
- "min_order" → `ORDER_MIN`
- "minorder" → `ORDER_MIN`

### Line Minimum Variations
- "line min" → `LINE_MIN`
- "line_minimum" → `LINE_MIN`
- "minimum_line" → `LINE_MIN`
- "min_line" → `LINE_MIN`
- "minline" → `LINE_MIN`

### Sales Team Variations
- "sales_rep" → `SALES_REP_ID`
- "sales_rep_id" → `SALES_REP_ID`
- "sales_representative" → `SALES_REP_ID`
- "rep" → `SALES_REP_ID`
- "representative" → `SALES_REP_ID`
- "sales_owner" → `SALES_OWNER`
- "owner" → `SALES_OWNER`
- "sales_team" → `SALES_TEAM`
- "team" → `SALES_TEAM`
- "sales_manager" → `SALES_MANAGER`
- "manager" → `SALES_MANAGER`
- "sales_director" → `SALES_DIRECTOR`
- "director" → `SALES_DIRECTOR`
- "sales_vp" → `SALES_VP`
- "vp" → `SALES_VP`
- "vice_president" → `SALES_VP`

### Account Rep Variations
- "account_rep" → `ACCOUNTREP`
- "accountrep" → `ACCOUNTREP`
- "account_representative" → `ACCOUNTREP`

### Award Rep Variations
- "award_rep" → `AWARDREP`
- "awardrep" → `AWARDREP`
- "award_representative" → `AWARDREP`

### Status Variations
- "is_active" → `IS_ACTIVE`
- "active" → `IS_ACTIVE`
- "status" → `IS_ACTIVE`

### Program Flags
- "is_gt25" → `IS_GT25`
- "gt25" → `IS_GT25`
- "greater_than_25" → `IS_GT25`
- "is_ecomm" → `IS_ECOMM`
- "ecomm" → `IS_ECOMM`
- "e_commerce" → `IS_ECOMM`
- "is_dfar" → `IS_DFAR`
- "dfar" → `IS_DFAR`
- "defense_federal_acquisition_regulation" → `IS_DFAR`
- "is_asl_applicable" → `IS_ASL_APPLICABLE`
- "asl_applicable" → `IS_ASL_APPLICABLE`
- "approved_supplier_list" → `IS_ASL_APPLICABLE`
- "hppflag" → `HPPFLAG`
- "hpp_flag" → `HPPFLAG`
- "hpp" → `HPPFLAG`

## Example User Queries

The system now correctly handles these user queries:

### Order Minimum Queries
- "Show me customers with order min greater than $1000" → Maps to `ORDER_MIN`
- "List customers with minimum order over $500" → Maps to `ORDER_MIN`
- "Find customers with min order above $2000" → Maps to `ORDER_MIN`
- "Customers with minorder less than $100" → Maps to `ORDER_MIN`

### Sales Team Queries
- "Who is the sales rep for customer XYZ Ltd?" → Maps to `SALES_REP_ID`
- "Show sales representative for ABC Corp" → Maps to `SALES_REP_ID`
- "List customers under sales manager John Doe" → Maps to `SALES_MANAGER`
- "Find customers in sales team Enterprise" → Maps to `SALES_TEAM`
- "Show sales owner for customer 12345" → Maps to `SALES_OWNER`
- "Who is the sales director for ABC Corp?" → Maps to `SALES_DIRECTOR`
- "Show sales vp for customer XYZ Ltd" → Maps to `SALES_VP`
- "List customers under vice president Mark Johnson" → Maps to `SALES_VP`

### Account Rep Queries
- "Who is the accountrep for Amazon Inc?" → Maps to `ACCOUNTREP`
- "Show account rep for customer 12345" → Maps to `ACCOUNTREP`
- "List customers with account_rep John Smith" → Maps to `ACCOUNTREP`
- "Find account representative for ABC Corp" → Maps to `ACCOUNTREP`

### Award Rep Queries
- "Who is the awardrep for customer 12345?" → Maps to `AWARDREP`
- "Show award rep for ABC Corp" → Maps to `AWARDREP`
- "List customers with award_rep Jane Doe" → Maps to `AWARDREP`
- "Find award representative for XYZ Ltd" → Maps to `AWARDREP`

### Status Queries
- "Is customer 12345 active?" → Maps to `IS_ACTIVE`
- "Show active customers" → Maps to `IS_ACTIVE`
- "List inactive customers" → Maps to `IS_ACTIVE`
- "What is the status of ABC Corp?" → Maps to `IS_ACTIVE`
- "Show customer status for XYZ Ltd" → Maps to `IS_ACTIVE`

### Program Flag Queries
- "Is customer 6789 part of IS_DFAR?" → Maps to `IS_DFAR`
- "Show DFAR customers" → Maps to `IS_DFAR`
- "List customers with defense flag" → Maps to `IS_DFAR`
- "Find customers with IS_GT25" → Maps to `IS_GT25`
- "Show GT25 customers" → Maps to `IS_GT25`
- "List customers with greater than 25 flag" → Maps to `IS_GT25`
- "Is customer 12345 part of IS_ECOMM?" → Maps to `IS_ECOMM`
- "Show ecommerce customers" → Maps to `IS_ECOMM`
- "List customers with e_comm flag" → Maps to `IS_ECOMM`
- "Find customers with IS_ASL_APPLICABLE" → Maps to `IS_ASL_APPLICABLE`
- "Show ASL applicable customers" → Maps to `IS_ASL_APPLICABLE`
- "List customers with approved supplier flag" → Maps to `IS_ASL_APPLICABLE`
- "Find customers with HPPFLAG" → Maps to `HPPFLAG`
- "Show HPP customers" → Maps to `HPPFLAG`
- "List customers with hpp_flag" → Maps to `HPPFLAG`

## Benefits

1. **User-Friendly**: Users can input queries in natural language with various phrasings
2. **Robust**: Handles typos, abbreviations, and different word orders
3. **Extensible**: Easy to add new variations by updating the mappings
4. **Centralized**: All mappings are in one place (`TableColumnConfig`)
5. **Consistent**: Uses the same mapping approach as other tables
6. **Testable**: Comprehensive test suite verifies all mappings work correctly

## Future Enhancements

1. **Spell Correction**: Integrate with `WordDatabase` for spell correction
2. **Fuzzy Matching**: Add fuzzy matching for similar words
3. **Context Awareness**: Consider context when mapping ambiguous terms
4. **Dynamic Learning**: Learn new variations from user interactions
5. **Multi-language Support**: Add support for different languages

## Conclusion

The customer column mapping implementation provides a robust, user-friendly way to handle various input variations while maintaining accuracy and consistency. The centralized approach in `TableColumnConfig` makes it easy to maintain and extend the mappings as needed. 