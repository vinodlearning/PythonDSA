# NLPUserActionHandler - TableColumnConfig Integration Summary

## Overview
This document summarizes the integration of the centralized `TableColumnConfig` system into `NLPUserActionHandler.java` to eliminate hardcoded table names and column references.

## Current Status: PARTIALLY INTEGRATED

### ✅ What Has Been Done

1. **Added TableColumnConfig Instance**
   ```java
   private static final TableColumnConfig TABLE_CONFIG = TableColumnConfig.getInstance();
   ```

2. **Updated SQL Query Generation**
   - Replaced hardcoded table names (`XXCCT.CCT_CONTRACTS`, etc.) with `TABLE_CONFIG.getTableName()`
   - Replaced hardcoded column names with `TABLE_CONFIG.getPrimaryKeyColumn()` and `TABLE_CONFIG.getColumnForBusinessTerm()`
   - Added validation in WHERE clause generation

3. **Enhanced WHERE Clause Building**
   - Added `buildWhereClauseWithValidation()` method that validates columns against the centralized configuration
   - Only includes valid columns in SQL queries

4. **Updated Column Extraction Methods**
   - `extractContractNumber()` now uses `TABLE_CONFIG.getPrimaryKeyColumn()`
   - `extractPartNumber()` now uses `TABLE_CONFIG.getColumnForBusinessTerm()`

5. **Enhanced Display Name Resolution**
   - `getDisplayName()` method now first checks centralized configuration before falling back to hardcoded mappings

### ❌ What Still Needs to Be Done

1. **Fix Package Declaration**
   ```java
   // Current (incorrect):
   package com.ben.view.nlp;
   
   // Should be:
   package com.oracle.view.source;
   ```

2. **Add Missing Imports**
   ```java
   import com.oracle.view.source.TableColumnConfig;
   import com.oracle.view.source.TableColumnMigrationUtil;
   ```

3. **Update Remaining Hardcoded References**
   - Line 528: `extractFilterValue(filters, "AWARD_NUMBER")` → Use centralized mapping
   - Line 589: `extractFilterValue(filters, "AWARD_NUMBER")` → Use centralized mapping  
   - Line 619: `extractFilterValue(filters, "INVOICE_PART_NUMBER")` → Use centralized mapping
   - Line 714: `extractFilterValue(filters, "AWARD_NUMBER")` → Use centralized mapping

4. **Add Missing Methods to TableColumnConfig**
   The integration assumes these methods exist in `TableColumnConfig`:
   - `getTableName(String tableType)`
   - `getPrimaryKeyColumn(String tableType)`
   - `getDisplayName(String tableType, String columnName)`

5. **Resolve Missing Dependencies**
   - `NLPEntityProcessor` class
   - `ContractsModel` class
   - `oracle.binding.OperationBinding` import

## Benefits of Integration

1. **Centralized Management**: All table and column definitions in one place
2. **Runtime Flexibility**: Can add/remove columns without code changes
3. **Validation**: Automatic column validation prevents SQL errors
4. **Consistency**: Ensures all parts of the system use the same column mappings
5. **Maintainability**: Easier to update table structures across the application

## Migration Steps Required

1. **Run TableColumnMigrationUtil** to populate the centralized configuration
2. **Fix package declaration** and imports
3. **Update remaining hardcoded column references**
4. **Add missing methods** to TableColumnConfig if they don't exist
5. **Test the integration** with various query types

## Example of Before vs After

### Before (Hardcoded):
```java
return "SELECT * FROM XXCCT.CCT_CONTRACTS WHERE AWARD_NUMBER = ?";
String contractNumber = extractFilterValue(filters, "AWARD_NUMBER");
```

### After (Centralized):
```java
return "SELECT * FROM " + TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS) + 
       " WHERE " + TABLE_CONFIG.getPrimaryKeyColumn(TableColumnConfig.TABLE_CONTRACTS) + " = ?";
String contractNumber = extractFilterValue(filters, TABLE_CONFIG.getPrimaryKeyColumn(TableColumnConfig.TABLE_CONTRACTS));
```

## Next Steps

1. **Complete the integration** by fixing the remaining issues
2. **Test thoroughly** with various query scenarios
3. **Update documentation** to reflect the new centralized approach
4. **Consider migrating other classes** to use the same centralized system

## Files Involved

- `NLPUserActionHandler.java` - Main file being integrated
- `TableColumnConfig.java` - Centralized configuration system
- `TableColumnMigrationUtil.java` - Migration utility
- `StandardJSONProcessor.java` - Already integrated (reference for patterns)

## Notes

- The integration maintains backward compatibility by keeping fallback hardcoded mappings
- All SQL generation now validates columns against the centralized configuration
- The system is more robust and maintainable with this centralized approach 