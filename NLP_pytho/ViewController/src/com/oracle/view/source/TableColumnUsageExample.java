package com.oracle.view.source;

import java.util.*;

/**
 * TABLE COLUMN USAGE EXAMPLE
 * 
 * This class demonstrates how to use the centralized TableColumnConfig system
 * for easy column management based on data design changes.
 * 
 * KEY BENEFITS:
 * - Single point of maintenance for all table columns
 * - Runtime column addition/removal
 * - Easy business term mapping management
 * - Consistent validation across the application
 * - Thread-safe operations
 */
public class TableColumnUsageExample {
    
    public static void main(String[] args) {
        System.out.println("=== TABLE COLUMN CONFIGURATION USAGE EXAMPLE ===\n");
        
        // Get the centralized configuration instance
        TableColumnConfig config = TableColumnConfig.getInstance();
        
        // Example 1: Adding new columns based on data design changes
        demonstrateAddingColumns(config);
        
        // Example 2: Removing deprecated columns
        demonstrateRemovingColumns(config);
        
        // Example 3: Managing business term mappings
        demonstrateBusinessTermMappings(config);
        
        // Example 4: Managing field synonyms
        demonstrateFieldSynonyms(config);
        
        // Example 5: Column validation and queries
        demonstrateValidationAndQueries(config);
        
        // Example 6: Getting configuration statistics
        demonstrateStatistics(config);
        
        System.out.println("=== EXAMPLE COMPLETED ===");
    }
    
    /**
     * Example 1: Adding new columns based on data design changes
     */
    private static void demonstrateAddingColumns(TableColumnConfig config) {
        System.out.println("1. ADDING NEW COLUMNS BASED ON DATA DESIGN CHANGES");
        System.out.println("==================================================");
        
        // Scenario: Business needs to add new contract priority field
        System.out.println("Scenario: Adding CONTRACT_PRIORITY column to contracts table");
        
        // Add the new column
        config.addColumn(TableColumnConfig.TABLE_CONTRACTS, "CONTRACT_PRIORITY");
        
        // Add business term mapping for the new column
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "priority", "CONTRACT_PRIORITY");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "contract_priority", "CONTRACT_PRIORITY");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "importance", "CONTRACT_PRIORITY");
        
        // Add field synonyms
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "priority_level", "CONTRACT_PRIORITY");
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "contract_importance", "CONTRACT_PRIORITY");
        
        // Verify the column was added
        boolean isValid = config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "CONTRACT_PRIORITY");
        System.out.println("Column CONTRACT_PRIORITY added successfully: " + isValid);
        
        // Test business term mapping
        String columnName = config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "priority");
        System.out.println("Business term 'priority' maps to column: " + columnName);
        
        System.out.println();
    }
    
    /**
     * Example 2: Removing deprecated columns
     */
    private static void demonstrateRemovingColumns(TableColumnConfig config) {
        System.out.println("2. REMOVING DEPRECATED COLUMNS");
        System.out.println("==============================");
        
        // Scenario: Business decides to deprecate some old columns
        System.out.println("Scenario: Removing deprecated columns from parts table");
        
        // Add a temporary column first (for demonstration)
        config.addColumn(TableColumnConfig.TABLE_PARTS, "DEPRECATED_COLUMN");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_PARTS, "old_field", "DEPRECATED_COLUMN");
        
        System.out.println("Before removal - Column exists: " + 
                          config.isValidColumn(TableColumnConfig.TABLE_PARTS, "DEPRECATED_COLUMN"));
        
        // Remove the deprecated column
        config.removeColumn(TableColumnConfig.TABLE_PARTS, "DEPRECATED_COLUMN");
        
        System.out.println("After removal - Column exists: " + 
                          config.isValidColumn(TableColumnConfig.TABLE_PARTS, "DEPRECATED_COLUMN"));
        
        // Business term mapping should also be removed
        String columnName = config.getColumnForBusinessTerm(TableColumnConfig.TABLE_PARTS, "old_field");
        System.out.println("Business term mapping removed: " + (columnName == null));
        
        System.out.println();
    }
    
    /**
     * Example 3: Managing business term mappings
     */
    private static void demonstrateBusinessTermMappings(TableColumnConfig config) {
        System.out.println("3. MANAGING BUSINESS TERM MAPPINGS");
        System.out.println("==================================");
        
        // Add new business term mappings for existing columns
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "deal", "CONTRACT_NAME");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "agreement", "CONTRACT_NAME");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "contract_id", "AWARD_NUMBER");
        
        // Test the mappings
        System.out.println("Business term 'deal' maps to: " + 
                          config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "deal"));
        System.out.println("Business term 'agreement' maps to: " + 
                          config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "agreement"));
        System.out.println("Business term 'contract_id' maps to: " + 
                          config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "contract_id"));
        
        // Remove a mapping
        config.removeBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "deal");
        System.out.println("After removing 'deal' mapping: " + 
                          (config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "deal") == null));
        
        System.out.println();
    }
    
    /**
     * Example 4: Managing field synonyms
     */
    private static void demonstrateFieldSynonyms(TableColumnConfig config) {
        System.out.println("4. MANAGING FIELD SYNONYMS");
        System.out.println("===========================");
        
        // Add field synonyms for better user experience
        config.addFieldSynonym(TableColumnConfig.TABLE_PARTS, "part_code", "INVOICE_PART_NUMBER");
        config.addFieldSynonym(TableColumnConfig.TABLE_PARTS, "item_code", "INVOICE_PART_NUMBER");
        config.addFieldSynonym(TableColumnConfig.TABLE_PARTS, "product_code", "INVOICE_PART_NUMBER");
        
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "deal_name", "CONTRACT_NAME");
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "agreement_name", "CONTRACT_NAME");
        
        // Test the synonyms
        System.out.println("Synonym 'part_code' maps to: " + 
                          config.getColumnForSynonym(TableColumnConfig.TABLE_PARTS, "part_code"));
        System.out.println("Synonym 'deal_name' maps to: " + 
                          config.getColumnForSynonym(TableColumnConfig.TABLE_CONTRACTS, "deal_name"));
        
        // Remove a synonym
        config.removeFieldSynonym(TableColumnConfig.TABLE_PARTS, "part_code");
        System.out.println("After removing 'part_code' synonym: " + 
                          (config.getColumnForSynonym(TableColumnConfig.TABLE_PARTS, "part_code") == null));
        
        System.out.println();
    }
    
    /**
     * Example 5: Column validation and queries
     */
    private static void demonstrateValidationAndQueries(TableColumnConfig config) {
        System.out.println("5. COLUMN VALIDATION AND QUERIES");
        System.out.println("================================");
        
        // Validate existing columns
        System.out.println("CONTRACT_NAME is valid: " + 
                          config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "CONTRACT_NAME"));
        System.out.println("INVALID_COLUMN is valid: " + 
                          config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "INVALID_COLUMN"));
        
        // Check if columns are displayable
        System.out.println("CONTRACT_NAME is displayable: " + 
                          config.isDisplayableColumn(TableColumnConfig.TABLE_CONTRACTS, "CONTRACT_NAME"));
        System.out.println("CONTRACT_NAME is filterable: " + 
                          config.isFilterableColumn(TableColumnConfig.TABLE_CONTRACTS, "CONTRACT_NAME"));
        
        // Get all columns for a table
        Set<String> contractsColumns = config.getColumns(TableColumnConfig.TABLE_CONTRACTS);
        System.out.println("Total contracts columns: " + contractsColumns.size());
        
        // Get displayable columns
        Set<String> displayableColumns = config.getDisplayableColumns(TableColumnConfig.TABLE_CONTRACTS);
        System.out.println("Displayable contracts columns: " + displayableColumns.size());
        
        // Get filterable columns
        Set<String> filterableColumns = config.getFilterableColumns(TableColumnConfig.TABLE_CONTRACTS);
        System.out.println("Filterable contracts columns: " + filterableColumns.size());
        
        System.out.println();
    }
    
    /**
     * Example 6: Getting configuration statistics
     */
    private static void demonstrateStatistics(TableColumnConfig config) {
        System.out.println("6. CONFIGURATION STATISTICS");
        System.out.println("===========================");
        
        // Get statistics for each table
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
        
        // Get overall configuration summary
        Map<String, Object> summary = config.getConfigurationSummary();
        System.out.println("Configuration Summary:");
        System.out.println("  Total Tables: " + summary.size());
        System.out.println("  Table Types: " + config.getTableTypes());
        
        System.out.println();
    }
    
    /**
     * Example: Real-world scenario - Adding a new business requirement
     */
    public static void demonstrateRealWorldScenario() {
        System.out.println("=== REAL-WORLD SCENARIO: ADDING NEW BUSINESS REQUIREMENT ===");
        
        TableColumnConfig config = TableColumnConfig.getInstance();
        
        // Scenario: Business wants to add contract approval workflow
        System.out.println("Business Requirement: Add contract approval workflow with new fields");
        
        // Step 1: Add new columns
        config.addColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVAL_STATUS");
        config.addColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVED_BY");
        config.addColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVAL_DATE");
        config.addColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVAL_COMMENTS");
        
        // Step 2: Add business term mappings
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "approval", "APPROVAL_STATUS");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "approved", "APPROVAL_STATUS");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "approver", "APPROVED_BY");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "approval_date", "APPROVAL_DATE");
        config.addBusinessTermMapping(TableColumnConfig.TABLE_CONTRACTS, "approval_comments", "APPROVAL_COMMENTS");
        
        // Step 3: Add field synonyms
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "approval_state", "APPROVAL_STATUS");
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "approval_status", "APPROVAL_STATUS");
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "approved_by", "APPROVED_BY");
        config.addFieldSynonym(TableColumnConfig.TABLE_CONTRACTS, "approval_notes", "APPROVAL_COMMENTS");
        
        // Step 4: Verify the implementation
        System.out.println("New columns added successfully:");
        System.out.println("  APPROVAL_STATUS: " + config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVAL_STATUS"));
        System.out.println("  APPROVED_BY: " + config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVED_BY"));
        System.out.println("  APPROVAL_DATE: " + config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVAL_DATE"));
        System.out.println("  APPROVAL_COMMENTS: " + config.isValidColumn(TableColumnConfig.TABLE_CONTRACTS, "APPROVAL_COMMENTS"));
        
        System.out.println("Business term mappings work:");
        System.out.println("  'approval' -> " + config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "approval"));
        System.out.println("  'approver' -> " + config.getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, "approver"));
        
        System.out.println("Field synonyms work:");
        System.out.println("  'approval_state' -> " + config.getColumnForSynonym(TableColumnConfig.TABLE_CONTRACTS, "approval_state"));
        System.out.println("  'approval_notes' -> " + config.getColumnForSynonym(TableColumnConfig.TABLE_CONTRACTS, "approval_notes"));
        
        System.out.println("=== REAL-WORLD SCENARIO COMPLETED ===");
    }
} 