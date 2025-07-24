package com.oracle.view.source;

import java.util.*;

/**
 * Migration utility to migrate all hardcoded table column definitions and mappings
 * to the new centralized TableColumnConfig system.
 * 
 * This utility migrates data from:
 * - StandardJSONProcessor.java (FIELD_NAMES, BUSINESS_FILTER_TERMS, ENHANCED_BUSINESS_TERMS, BUSINESS_FIELD_SYNONYMS)
 * - ContractsModel.java (table columns, business terms, field synonyms)
 * 
 * After migration, all table-related information will be centralized in TableColumnConfig.
 */
public class TableColumnMigrationUtil {

    /**
     * Migrate all hardcoded columns from StandardJSONProcessor to TableColumnConfig
     */
    public static void migrateFromStandardJSONProcessor() {
        System.out.println("Starting migration from StandardJSONProcessor...");
        
        TableColumnConfig config = TableColumnConfig.getInstance();

        // Migrate FIELD_NAMES (all field names for validation)
        migrateFieldNames(config);
        
        // Migrate business term mappings
        migrateBusinessFilterTerms(config);
        migrateEnhancedBusinessTerms(config);
        
        // Migrate business field synonyms
        migrateBusinessFieldSynonyms(config);
        
        // Migrate existing table columns (from previous migration)
        migrateContractsColumns(config);
        migratePartsColumns(config);
        migrateFailedPartsColumns(config);
        
        System.out.println("Migration from StandardJSONProcessor completed successfully!");
    }

    /**
     * Migrate FIELD_NAMES set to all table types for validation
     */
    private static void migrateFieldNames(TableColumnConfig config) {
        System.out.println("Migrating FIELD_NAMES...");
        
        Set<String> fieldNames = new HashSet<>(Arrays.asList(
            "CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE",
            "PAYMENT_TERMS", "INCOTERMS", "STATUS", "CONTRACT_TYPE", "PRICE_EXPIRATION_DATE",
            "CONTRACT_LENGTH", "CURRENCY", "MIN_INV_OBLIGATION", "IS_PROGRAM",
            "IS_HPP_UNPRICED_CONTRACT", "CMI", "VMI", "EDI", "MIN_MAX", "BAILMENT",
            "CONSIGNMENT", "KITTING", "PL_3", "FSL_LOCATION", "VENDING_MACHINES",
            "SERVICE_FEE_APPLIES", "E_COMMERCE_ACCESS", "GT_25", "PART_FILE", "PMA_TSO_APPLIES",
            "DFAR_APPLIES", "ASL_APPLIES", "ASL_DETAIL", "STOCKING_STRATEGY",
            "LIABILITY_ON_INVESTMENT", "HPP_LANGUAGE", "CSM_LANGUAGE", "REBATE", "LINE_MIN",
            "ORDER_MIN", "EFFECTIVE_LOL", "PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER",
            "RIGHTS_OF_RETURN", "INCENTIVES_CREDITS", "CANCELLATION_PRIVILEGES",
            "BILL_AND_HOLD", "BUY_BACK", "CREATE_DATE", "PROJECT_TYPE", "DESCRIPTION",
            "TOTAL_VALUE", "AWARD_NUMBER", "CUSTOMER_NUMBER"
        ));

        // Add to all table types for validation purposes
        config.addColumns(TableColumnConfig.TABLE_CONTRACTS, fieldNames.toArray(new String[0]));
        config.addColumns(TableColumnConfig.TABLE_PARTS, fieldNames.toArray(new String[0]));
        config.addColumns(TableColumnConfig.TABLE_FAILED_PARTS, fieldNames.toArray(new String[0]));
        
        System.out.println("  Added " + fieldNames.size() + " field names to all table types");
    }

    /**
     * Migrate BUSINESS_FILTER_TERMS from StandardJSONProcessor
     */
    private static void migrateBusinessFilterTerms(TableColumnConfig config) {
        System.out.println("Migrating BUSINESS_FILTER_TERMS...");
        
        Map<String, String> businessFilterTerms = new HashMap<>();
        businessFilterTerms.put("rebates", "REBATE");
        businessFilterTerms.put("rebate", "REBATE");
        businessFilterTerms.put("line minimums", "LINE_MIN");
        businessFilterTerms.put("line min", "LINE_MIN");

        // Add to contracts table (these are contract-related terms)
        for (Map.Entry<String, String> entry : businessFilterTerms.entrySet()) {
            config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, entry.getKey(), entry.getValue());
        }
        
        System.out.println("  Added " + businessFilterTerms.size() + " business filter terms to contracts table");
    }

    /**
     * Migrate ENHANCED_BUSINESS_TERMS from StandardJSONProcessor
     */
    private static void migrateEnhancedBusinessTerms(TableColumnConfig config) {
        System.out.println("Migrating ENHANCED_BUSINESS_TERMS...");
        
        Map<String, String> enhancedBusinessTerms = new HashMap<>();

        // Contract number mappings
        enhancedBusinessTerms.put("contract_number", "AWARD_NUMBER");
        enhancedBusinessTerms.put("contract_id", "AWARD_NUMBER");
        enhancedBusinessTerms.put("award_number", "AWARD_NUMBER");
        enhancedBusinessTerms.put("award_id", "AWARD_NUMBER");
        enhancedBusinessTerms.put("contract", "AWARD_NUMBER");

        // Part number mappings
        enhancedBusinessTerms.put("part_number", "INVOICE_PART_NUMBER");
        enhancedBusinessTerms.put("part_id", "INVOICE_PART_NUMBER");
        enhancedBusinessTerms.put("invoice_part_number", "INVOICE_PART_NUMBER");
        enhancedBusinessTerms.put("part", "INVOICE_PART_NUMBER");

        // Customer mappings
        enhancedBusinessTerms.put("customer_number", "CUSTOMER_NUMBER");
        enhancedBusinessTerms.put("customer_id", "CUSTOMER_NUMBER");
        enhancedBusinessTerms.put("customer", "CUSTOMER_NAME");
        enhancedBusinessTerms.put("client", "CUSTOMER_NAME");

        // Date mappings
        enhancedBusinessTerms.put("effective_date", "EFFECTIVE_DATE");
        enhancedBusinessTerms.put("expiration_date", "EXPIRATION_DATE");
        enhancedBusinessTerms.put("creation_date", "CREATE_DATE");
        enhancedBusinessTerms.put("created_date", "CREATE_DATE");

        // Status mappings
        enhancedBusinessTerms.put("status", "STATUS");
        enhancedBusinessTerms.put("state", "STATUS");
        enhancedBusinessTerms.put("condition", "STATUS");

        // Enhanced business term to column mappings
        enhancedBusinessTerms.put("pricing", "PRICE");
        enhancedBusinessTerms.put("price", "PRICE");
        enhancedBusinessTerms.put("cost", "PRICE");
        enhancedBusinessTerms.put("minimum order", "MOQ");
        enhancedBusinessTerms.put("min order", "MOQ");
        enhancedBusinessTerms.put("min order qty", "MOQ");
        enhancedBusinessTerms.put("minimum order quantity", "MOQ");
        enhancedBusinessTerms.put("moq", "MOQ");
        enhancedBusinessTerms.put("unit of measure", "UOM");
        enhancedBusinessTerms.put("unit measure", "UOM");
        enhancedBusinessTerms.put("uom", "UOM");
        enhancedBusinessTerms.put("lead time", "LEAD_TIME");
        enhancedBusinessTerms.put("leadtime", "LEAD_TIME");
        enhancedBusinessTerms.put("delivery time", "LEAD_TIME");

        // Add to appropriate tables
        for (Map.Entry<String, String> entry : enhancedBusinessTerms.entrySet()) {
            String column = entry.getValue();
            
            // Determine which table this column belongs to
            if (isContractColumn(column)) {
                config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, entry.getKey(), column);
            } else if (isPartColumn(column)) {
                config.addBusinessTermMapping(TableColumnConfig.TABLE_PARTS, entry.getKey(), column);
            } else if (isFailedPartColumn(column)) {
                config.addBusinessTermMapping(TableColumnConfig.TABLE_FAILED_PARTS, entry.getKey(), column);
            }
        }
        
        System.out.println("  Added " + enhancedBusinessTerms.size() + " enhanced business terms to appropriate tables");
    }

    /**
     * Migrate BUSINESS_FIELD_SYNONYMS from StandardJSONProcessor
     */
    private static void migrateBusinessFieldSynonyms(TableColumnConfig config) {
        System.out.println("Migrating BUSINESS_FIELD_SYNONYMS...");
        
        Map<String, List<String>> businessFieldSynonyms = new HashMap<>();
        
        // Contract-related synonyms
        businessFieldSynonyms.put("flags", Arrays.asList("IS_PROGRAM", "IS_HPP_UNPRICED_CONTRACT"));
        businessFieldSynonyms.put("contract flags", Arrays.asList("IS_PROGRAM", "IS_HPP_UNPRICED_CONTRACT"));
        businessFieldSynonyms.put("inventory methods", Arrays.asList("CMI", "VMI"));
        businessFieldSynonyms.put("cmi,vmi", Arrays.asList("CMI", "VMI"));
        businessFieldSynonyms.put("order settings", Arrays.asList("EDI", "MIN_MAX"));
        businessFieldSynonyms.put("edi,min_max", Arrays.asList("EDI", "MIN_MAX"));
        businessFieldSynonyms.put("inventory terms", Arrays.asList("BAILMENT", "CONSIGNMENT"));
        businessFieldSynonyms.put("bailment,consignment", Arrays.asList("BAILMENT", "CONSIGNMENT"));
        businessFieldSynonyms.put("parts processing", Arrays.asList("KITTING", "PL_3"));
        businessFieldSynonyms.put("kitting,pl_3", Arrays.asList("KITTING", "PL_3"));
        businessFieldSynonyms.put("location details", Arrays.asList("FSL_LOCATION", "VENDING_MACHINES"));
        businessFieldSynonyms.put("fsl_location,vending_machines", Arrays.asList("FSL_LOCATION", "VENDING_MACHINES"));
        businessFieldSynonyms.put("fee and access", Arrays.asList("SERVICE_FEE_APPLIES", "E_COMMERCE_ACCESS"));
        businessFieldSynonyms.put("service_fee_applies,e_commerce_access", Arrays.asList("SERVICE_FEE_APPLIES", "E_COMMERCE_ACCESS"));
        businessFieldSynonyms.put("compliance flags", Arrays.asList("PMA_TSO_APPLIES", "DFAR_APPLIES"));
        businessFieldSynonyms.put("pma_tso_applies,dfar_applies", Arrays.asList("PMA_TSO_APPLIES", "DFAR_APPLIES"));
        businessFieldSynonyms.put("supplier list", Arrays.asList("ASL_APPLIES", "ASL_DETAIL"));
        businessFieldSynonyms.put("asl_applies,asl_detail", Arrays.asList("ASL_APPLIES", "ASL_DETAIL"));
        businessFieldSynonyms.put("inventory strategy", Arrays.asList("STOCKING_STRATEGY", "LIABILITY_ON_INVESTMENT"));
        businessFieldSynonyms.put("stocking_strategy,liability_on_investment", Arrays.asList("STOCKING_STRATEGY", "LIABILITY_ON_INVESTMENT"));
        businessFieldSynonyms.put("language settings", Arrays.asList("HPP_LANGUAGE", "CSM_LANGUAGE"));
        businessFieldSynonyms.put("hpp_language,csm_language", Arrays.asList("HPP_LANGUAGE", "CSM_LANGUAGE"));
        businessFieldSynonyms.put("pricing terms", Arrays.asList("REBATE", "LINE_MIN"));
        businessFieldSynonyms.put("rebate,line_min", Arrays.asList("REBATE", "LINE_MIN"));
        businessFieldSynonyms.put("order terms", Arrays.asList("ORDER_MIN", "EFFECTIVE_LOL"));
        businessFieldSynonyms.put("order_min,effective_lol", Arrays.asList("ORDER_MIN", "EFFECTIVE_LOL"));
        businessFieldSynonyms.put("legal terms", Arrays.asList("PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER"));
        businessFieldSynonyms.put("penalties_damages,unusual_title_transfer", Arrays.asList("PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER"));
        businessFieldSynonyms.put("return policies", Arrays.asList("RIGHTS_OF_RETURN", "INCENTIVES_CREDITS"));
        businessFieldSynonyms.put("rights_of_return,incentives_credits", Arrays.asList("RIGHTS_OF_RETURN", "INCENTIVES_CREDITS"));
        businessFieldSynonyms.put("order modifications", Arrays.asList("CANCELLATION_PRIVILEGES", "BILL_AND_HOLD"));
        businessFieldSynonyms.put("cancellation_privileges,bill_and_hold", Arrays.asList("CANCELLATION_PRIVILEGES", "BILL_AND_HOLD"));
        businessFieldSynonyms.put("buyback terms", Arrays.asList("BUY_BACK", "CREATE_DATE"));
        businessFieldSynonyms.put("buy_back,create_date", Arrays.asList("BUY_BACK", "CREATE_DATE"));
        businessFieldSynonyms.put("start and end", Arrays.asList("EFFECTIVE_DATE", "EXPIRATION_DATE"));
        businessFieldSynonyms.put("start", Arrays.asList("EFFECTIVE_DATE"));
        businessFieldSynonyms.put("end", Arrays.asList("EXPIRATION_DATE"));
        
        // Parts-related synonyms
        businessFieldSynonyms.put("price", Arrays.asList("PRICE"));
        businessFieldSynonyms.put("eau", Arrays.asList("EAU"));
        businessFieldSynonyms.put("moq", Arrays.asList("MOQ"));
        businessFieldSynonyms.put("lead time", Arrays.asList("LEAD_TIME"));
        businessFieldSynonyms.put("unit of measure", Arrays.asList("UOM"));

        // Add to appropriate tables
        for (Map.Entry<String, List<String>> entry : businessFieldSynonyms.entrySet()) {
            String synonym = entry.getKey();
            List<String> columns = entry.getValue();
            
            for (String column : columns) {
                if (isContractColumn(column)) {
                    config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, synonym, column);
                } else if (isPartColumn(column)) {
                    config.addFieldSynonym(TableColumnConfig.TABLE_PARTS, synonym, column);
                } else if (isFailedPartColumn(column)) {
                    config.addFieldSynonym(TableColumnConfig.TABLE_FAILED_PARTS, synonym, column);
                }
            }
        }
        
        System.out.println("  Added " + businessFieldSynonyms.size() + " business field synonyms to appropriate tables");
    }

    /**
     * Helper method to determine if a column belongs to contracts table
     */
    private static boolean isContractColumn(String column) {
        Set<String> contractColumns = new HashSet<>(Arrays.asList(
            "CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE",
            "PAYMENT_TERMS", "INCOTERMS", "STATUS", "CONTRACT_TYPE", "PRICE_EXPIRATION_DATE",
            "CONTRACT_LENGTH", "CURRENCY", "MIN_INV_OBLIGATION", "IS_PROGRAM",
            "IS_HPP_UNPRICED_CONTRACT", "CMI", "VMI", "EDI", "MIN_MAX", "BAILMENT",
            "CONSIGNMENT", "KITTING", "PL_3", "FSL_LOCATION", "VENDING_MACHINES",
            "SERVICE_FEE_APPLIES", "E_COMMERCE_ACCESS", "GT_25", "PART_FILE", "PMA_TSO_APPLIES",
            "DFAR_APPLIES", "ASL_APPLIES", "ASL_DETAIL", "STOCKING_STRATEGY",
            "LIABILITY_ON_INVESTMENT", "HPP_LANGUAGE", "CSM_LANGUAGE", "REBATE", "LINE_MIN",
            "ORDER_MIN", "EFFECTIVE_LOL", "PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER",
            "RIGHTS_OF_RETURN", "INCENTIVES_CREDITS", "CANCELLATION_PRIVILEGES",
            "BILL_AND_HOLD", "BUY_BACK", "CREATE_DATE", "PROJECT_TYPE", "DESCRIPTION",
            "TOTAL_VALUE", "AWARD_NUMBER", "CUSTOMER_NUMBER"
        ));
        return contractColumns.contains(column);
    }

    /**
     * Helper method to determine if a column belongs to parts table
     */
    private static boolean isPartColumn(String column) {
        Set<String> partColumns = new HashSet<>(Arrays.asList(
            "MOQ", "EAU", "LEAD_TIME", "PRICE", "COST", "UOM", "ITEM_CLASSIFICATION",
            "INVOICE_PART_NUMBER", "NSN_PART_NUMBER", "SAP_NUMBER", "LOADED_CP_NUMBER",
            "AWARD_ID", "LINE_NO", "CUSTOMER_REFERENCE", "ASL_CODES", "PRIME",
            "FUTURE_PRICE", "F_PRICE_EFFECTIVE_DATE", "FUTURE_PRICE2",
            "F_PRICE_EFFECTIVE_DATE2", "FUTURE_PRICE3", "F_PRICE_EFFECTIVE_DATE3",
            "PART_EXPIRATION_DATE", "TOT_CON_QTY_REQ", "QUOTE_COST", "QUOTE_COST_SOURCE",
            "PURCHASE_COMMENTS", "SALES_COMMENTS", "CUSTOMER_RESPONSE", "PL4_VENDOR",
            "APPLICABLE_CONTRACT", "CUST_EXCLUDE_PN", "PLANNING_COMMENTS", "DATE_LOADED",
            "CREATION_DATE", "LAST_UPDATE_DATE", "LAST_UPDATED_BY", "AWARD_TAGS",
            "PREV_PRICE", "REPRICE_EFFECTIVE_DATE", "EXTERNAL_CONTRACT_NO",
            "EXTERNAL_LINE_NO", "PLANT", "VALUATION_TYPE", "CSM_STATUS",
            "TEST_REPORTS_REQUIRED", "INCOTERMS_LOCATION", "CSM_MONITORED",
            "AWARD_REP_COMMENTS", "STATUS"
        ));
        return partColumns.contains(column);
    }

    /**
     * Helper method to determine if a column belongs to failed parts table
     */
    private static boolean isFailedPartColumn(String column) {
        Set<String> failedPartColumns = new HashSet<>(Arrays.asList(
            "PASRT_NUMEBR", "ERROR_COLUMN", "LAODED_CP_NUMBER", "REASON", "AWARD_NUMBER",
            "LINE_NO", "VALIDATION_ERROR", "DATA_QUALITY_ISSUE", "LOADING_ERROR",
            "PROCESSING_ERROR", "BUSINESS_RULE_VIOLATION", "PART_NUMBER"
        ));
        return failedPartColumns.contains(column);
    }
    
    /**
     * Migrate contracts columns from hardcoded sets
     */
    private static void migrateContractsColumns(TableColumnConfig config) {
        // These are the original hardcoded columns from StandardJSONProcessor
        String[] contractsColumns = {
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
        };
        
        config.addColumns(TableColumnConfig.TABLE_CONTRACTS, contractsColumns);
        System.out.println("Migrated " + contractsColumns.length + " contracts columns");
    }
    
    /**
     * Migrate parts columns from hardcoded sets
     */
    private static void migratePartsColumns(TableColumnConfig config) {
        // These are the original hardcoded columns from StandardJSONProcessor
        String[] partsColumns = {
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
        };
        
        config.addColumns(TableColumnConfig.TABLE_PARTS, partsColumns);
        System.out.println("Migrated " + partsColumns.length + " parts columns");
    }
    
    /**
     * Migrate failed parts columns from hardcoded sets
     */
    private static void migrateFailedPartsColumns(TableColumnConfig config) {
        // These are the original hardcoded columns from StandardJSONProcessor
        String[] failedPartsColumns = {
            "PART_NUMBER", "ERROR_COLUMN", "LOADED_CP_NUMBER", "REASON"
        };
        
        config.addColumns(TableColumnConfig.TABLE_FAILED_PARTS, failedPartsColumns);
        System.out.println("Migrated " + failedPartsColumns.length + " failed parts columns");
    }
    
    /**
     * Migrate business term mappings from hardcoded maps
     */
    private static void migrateBusinessTermMappings(TableColumnConfig config) {
        // Contract business term mappings
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
        
        // Parts business term mappings
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
        
        // Add contract mappings
        for (Map.Entry<String, String> entry : contractMappings.entrySet()) {
            config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, entry.getKey(), entry.getValue());
        }
        
        // Add parts mappings
        for (Map.Entry<String, String> entry : partsMappings.entrySet()) {
            config.addBusinessTermMapping(TableColumnConfig.TABLE_PARTS, entry.getKey(), entry.getValue());
        }
        
        System.out.println("Migrated " + contractMappings.size() + " contract business term mappings");
        System.out.println("Migrated " + partsMappings.size() + " parts business term mappings");
    }
    
    /**
     * Generate a migration report
     */
    public static void generateMigrationReport() {
        TableColumnConfig config = TableColumnConfig.getInstance();
        
        System.out.println("=== TABLE COLUMN MIGRATION REPORT ===");
        System.out.println();
        
        for (String tableType : config.getTableTypes()) {
            Map<String, Object> stats = config.getTableStatistics(tableType);
            
            System.out.println("Table: " + tableType);
            System.out.println("  Total Columns: " + stats.get("totalColumns"));
            System.out.println("  Displayable Columns: " + stats.get("displayableColumns"));
            System.out.println("  Filterable Columns: " + stats.get("filterableColumns"));
            System.out.println("  Business Term Mappings: " + stats.get("businessTermMappings"));
            System.out.println("  Field Synonyms: " + stats.get("fieldSynonyms"));
            System.out.println();
        }
        
        System.out.println("=== MIGRATION COMPLETE ===");
    }
    
    /**
     * Validate that all hardcoded columns have been migrated
     */
    public static boolean validateMigration() {
        TableColumnConfig config = TableColumnConfig.getInstance();
        
        // Check contracts columns
        Set<String> expectedContracts = new HashSet<>(Arrays.asList(
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
        
        Set<String> actualContracts = config.getColumns(TableColumnConfig.TABLE_CONTRACTS);
        boolean contractsValid = actualContracts.containsAll(expectedContracts);
        
        // Check parts columns
        Set<String> expectedParts = new HashSet<>(Arrays.asList(
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
        
        Set<String> actualParts = config.getColumns(TableColumnConfig.TABLE_PARTS);
        boolean partsValid = actualParts.containsAll(expectedParts);
        
        // Check failed parts columns
        Set<String> expectedFailedParts = new HashSet<>(Arrays.asList(
            "PART_NUMBER", "ERROR_COLUMN", "LOADED_CP_NUMBER", "REASON"
        ));
        
        Set<String> actualFailedParts = config.getColumns(TableColumnConfig.TABLE_FAILED_PARTS);
        boolean failedPartsValid = actualFailedParts.containsAll(expectedFailedParts);
        
        boolean allValid = contractsValid && partsValid && failedPartsValid;
        
        System.out.println("Migration Validation Results:");
        System.out.println("  Contracts: " + (contractsValid ? "PASS" : "FAIL"));
        System.out.println("  Parts: " + (partsValid ? "PASS" : "FAIL"));
        System.out.println("  Failed Parts: " + (failedPartsValid ? "PASS" : "FAIL"));
        System.out.println("  Overall: " + (allValid ? "PASS" : "FAIL"));
        
        return allValid;
    }
    
    /**
     * Main method for testing migration
     */
    public static void main(String[] args) {
        System.out.println("Starting Table Column Migration...");
        
        // Perform migration
        migrateFromStandardJSONProcessor();
        
        // Generate report
        generateMigrationReport();
        
        // Validate migration
        boolean isValid = validateMigration();
        
        if (isValid) {
            System.out.println("Migration completed successfully!");
        } else {
            System.out.println("Migration completed with issues. Please review the report above.");
        }
    }
} 