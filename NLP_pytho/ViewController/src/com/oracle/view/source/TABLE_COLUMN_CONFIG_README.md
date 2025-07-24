# Table Column Configuration System

## Overview

The **TableColumnConfig** system provides a centralized, maintainable approach to managing table columns across the NLP application. This replaces the previous hardcoded approach where columns were defined in multiple places throughout the codebase.

## Key Benefits

- **Single Point of Maintenance**: All table columns are managed in one place
- **Runtime Column Management**: Add/remove columns without code changes
- **Easy Data Design Changes**: Update columns based on business requirements
- **Consistent Validation**: All components use the same column definitions
- **Thread-Safe Operations**: Safe for concurrent access
- **Business Term Mapping**: Easy management of user-friendly terms
- **Field Synonyms**: Support for alternative field names

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    TableColumnConfig                        │
│                     (Singleton)                             │
├─────────────────────────────────────────────────────────────┤
│  • Table Columns (CONTRACTS, PARTS, FAILED_PARTS)          │
│  • Business Term Mappings                                   │
│  • Field Synonyms                                           │
│  • Displayable/Filterable Categories                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Application Components                       │
├─────────────────────────────────────────────────────────────┤
│  • StandardJSONProcessor                                    │
│  • ContractsModel                                           │
│  • UserActionHandler                                        │
│  • Other NLP Components                                     │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Get the Configuration Instance

```java
TableColumnConfig config = TableColumnConfig.getInstance();
```

### 2. Add New Columns

```java
// Add a single column
config.addColumn("CONTRACTS", "NEW_COLUMN_NAME");

// Add multiple columns
config.addColumns("PARTS", "COLUMN1", "COLUMN2", "COLUMN3");

// Add with specific category (DISPLAY, FILTER, or BOTH)
config.addColumn("CONTRACTS", "DISPLAY_ONLY_COLUMN", "DISPLAY");
config.addColumn("PARTS", "FILTER_ONLY_COLUMN", "FILTER");
```

### 3. Add Business Term Mappings

```java
// Map user-friendly terms to column names
config.addBusinessTermMapping("CONTRACTS", "priority", "CONTRACT_PRIORITY");
config.addBusinessTermMapping("CONTRACTS", "contract_priority", "CONTRACT_PRIORITY");
config.addBusinessTermMapping("PARTS", "part_code", "INVOICE_PART_NUMBER");
```

### 4. Add Field Synonyms

```java
// Add alternative names for fields
config.addFieldSynonym("CONTRACTS", "deal_name", "CONTRACT_NAME");
config.addFieldSynonym("CONTRACTS", "agreement_name", "CONTRACT_NAME");
config.addFieldSynonym("PARTS", "item_code", "INVOICE_PART_NUMBER");
```

### 5. Validate Columns

```java
// Check if column exists
boolean isValid = config.isValidColumn("CONTRACTS", "CONTRACT_NAME");

// Check if column is displayable
boolean isDisplayable = config.isDisplayableColumn("CONTRACTS", "CONTRACT_NAME");

// Check if column is filterable
boolean isFilterable = config.isFilterableColumn("CONTRACTS", "CONTRACT_NAME");
```

## Table Types

The system supports three main table types:

- `TableColumnConfig.TABLE_CONTRACTS` - Contract-related columns
- `TableColumnConfig.TABLE_PARTS` - Parts-related columns  
- `TableColumnConfig.TABLE_FAILED_PARTS` - Failed parts columns

## Column Categories

Columns can be categorized for different purposes:

- `TableColumnConfig.CATEGORY_DISPLAY` - Columns that can be displayed in results
- `TableColumnConfig.CATEGORY_FILTER` - Columns that can be used for filtering
- `TableColumnConfig.CATEGORY_BOTH` - Columns that can be both displayed and filtered (default)

## Migration from Hardcoded Approach

### Step 1: Run Migration Utility

```java
// Migrate all existing hardcoded columns
TableColumnMigrationUtil.migrateFromStandardJSONProcessor();

// Generate migration report
TableColumnMigrationUtil.generateMigrationReport();

// Validate migration
boolean isValid = TableColumnMigrationUtil.validateMigration();
```

### Step 2: Update Components

Components like `StandardJSONProcessor` have been updated to use the centralized config:

```java
// Old approach (hardcoded)
private static final Set<String> CONTRACTS_TABLE_COLUMNS = new HashSet<>(Arrays.asList(...));

// New approach (centralized)
private static final TableColumnConfig TABLE_CONFIG = TableColumnConfig.getInstance();
```

## Real-World Usage Examples

### Example 1: Adding Contract Approval Workflow

```java
TableColumnConfig config = TableColumnConfig.getInstance();

// Add new approval columns
config.addColumn("CONTRACTS", "APPROVAL_STATUS");
config.addColumn("CONTRACTS", "APPROVED_BY");
config.addColumn("CONTRACTS", "APPROVAL_DATE");
config.addColumn("CONTRACTS", "APPROVAL_COMMENTS");

// Add business term mappings
config.addBusinessTermMapping("CONTRACTS", "approval", "APPROVAL_STATUS");
config.addBusinessTermMapping("CONTRACTS", "approver", "APPROVED_BY");
config.addBusinessTermMapping("CONTRACTS", "approval_date", "APPROVAL_DATE");

// Add field synonyms
config.addFieldSynonym("CONTRACTS", "approval_state", "APPROVAL_STATUS");
config.addFieldSynonym("CONTRACTS", "approved_by", "APPROVED_BY");
```

### Example 2: Removing Deprecated Columns

```java
TableColumnConfig config = TableColumnConfig.getInstance();

// Remove deprecated columns
config.removeColumn("PARTS", "DEPRECATED_COLUMN_1");
config.removeColumn("PARTS", "DEPRECATED_COLUMN_2");

// Remove associated business term mappings
config.removeBusinessTermMapping("PARTS", "old_business_term");

// Remove associated field synonyms
config.removeFieldSynonym("PARTS", "old_synonym");
```

### Example 3: Bulk Column Management

```java
TableColumnConfig config = TableColumnConfig.getInstance();

// Add multiple columns for a new feature
String[] newColumns = {
    "FEATURE_FLAG_1",
    "FEATURE_FLAG_2", 
    "FEATURE_FLAG_3"
};

config.addColumns("CONTRACTS", newColumns);

// Add business term mappings for all new columns
for (String column : newColumns) {
    String businessTerm = column.toLowerCase().replace("_", " ");
    config.addBusinessTermMapping("CONTRACTS", businessTerm, column);
}
```

## API Reference

### Core Methods

#### Column Management
- `addColumn(String tableType, String columnName)` - Add a column
- `addColumn(String tableType, String columnName, String category)` - Add a column with category
- `addColumns(String tableType, String... columnNames)` - Add multiple columns
- `removeColumn(String tableType, String columnName)` - Remove a column
- `removeColumns(String tableType, String... columnNames)` - Remove multiple columns

#### Business Term Mappings
- `addBusinessTermMapping(String tableType, String businessTerm, String columnName)` - Add mapping
- `removeBusinessTermMapping(String tableType, String businessTerm)` - Remove mapping
- `getColumnForBusinessTerm(String tableType, String businessTerm)` - Get column for term

#### Field Synonyms
- `addFieldSynonym(String tableType, String synonym, String columnName)` - Add synonym
- `removeFieldSynonym(String tableType, String synonym)` - Remove synonym
- `getColumnForSynonym(String tableType, String synonym)` - Get column for synonym

#### Validation
- `isValidColumn(String tableType, String columnName)` - Check if column exists
- `isDisplayableColumn(String tableType, String columnName)` - Check if displayable
- `isFilterableColumn(String tableType, String columnName)` - Check if filterable

#### Queries
- `getColumns(String tableType)` - Get all columns
- `getDisplayableColumns(String tableType)` - Get displayable columns
- `getFilterableColumns(String tableType)` - Get filterable columns
- `getBusinessTermMappings(String tableType)` - Get business term mappings
- `getFieldSynonyms(String tableType)` - Get field synonyms

#### Utilities
- `getTableStatistics(String tableType)` - Get table statistics
- `getConfigurationSummary()` - Get overall configuration summary
- `resetToDefaults()` - Reset to default configuration
- `getTableTypes()` - Get all table types
- `tableExists(String tableType)` - Check if table exists

## Best Practices

### 1. Column Naming
- Use UPPERCASE for column names (e.g., "CONTRACT_NAME")
- Use descriptive names that reflect the data content
- Follow existing naming conventions

### 2. Business Term Mapping
- Use lowercase for business terms (e.g., "contract_name")
- Include common variations and misspellings
- Map to the most appropriate column

### 3. Field Synonyms
- Include alternative names users might use
- Consider industry-specific terminology
- Include common abbreviations

### 4. Category Assignment
- Use DISPLAY for columns that should appear in results
- Use FILTER for columns used only for filtering
- Use BOTH for columns that serve both purposes

### 5. Validation
- Always validate columns before using them
- Check appropriate categories (displayable/filterable)
- Handle missing columns gracefully

## Troubleshooting

### Common Issues

1. **Column Not Found**
   ```java
   // Check if column exists
   if (!config.isValidColumn(tableType, columnName)) {
       // Handle missing column
   }
   ```

2. **Business Term Not Mapped**
   ```java
   // Get column for business term
   String columnName = config.getColumnForBusinessTerm(tableType, businessTerm);
   if (columnName == null) {
       // Handle unmapped term
   }
   ```

3. **Performance Issues**
   - The configuration is cached in memory
   - Singleton pattern ensures single instance
   - Thread-safe operations prevent conflicts

### Debugging

```java
// Get detailed statistics
Map<String, Object> stats = config.getTableStatistics("CONTRACTS");
System.out.println("Table statistics: " + stats);

// Get configuration summary
Map<String, Object> summary = config.getConfigurationSummary();
System.out.println("Configuration summary: " + summary);
```

## Migration Checklist

- [ ] Run `TableColumnMigrationUtil.migrateFromStandardJSONProcessor()`
- [ ] Verify migration with `TableColumnMigrationUtil.validateMigration()`
- [ ] Update components to use centralized config
- [ ] Test column validation and queries
- [ ] Update business term mappings as needed
- [ ] Add field synonyms for better user experience
- [ ] Document any custom configurations

## Future Enhancements

- Database-driven configuration
- Configuration persistence
- Dynamic column loading
- Column metadata (data types, constraints)
- Version control for configurations
- Configuration import/export
- Audit trail for changes 