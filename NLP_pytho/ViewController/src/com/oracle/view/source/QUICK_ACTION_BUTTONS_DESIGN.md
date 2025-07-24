# Quick Action Buttons Design & Implementation

## 🎯 **Overview**

This document outlines the design and implementation of Quick Action Buttons for the BCCT Contract Management System. Each button provides instant access to common queries and actions. These are command buttons that users click on, configured in a quick actions section that does not interfere with existing business features.

## 📋 **Button Actions**

### **1. Recent Contracts**
- **Action Type**: `QUICK_ACTION_RECENT_CONTRACTS`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: List contracts created in last 24 hours
- **SQL Query**: 
  ```sql
  SELECT CONTRACT_NAME, CUSTOMER_NAME, EFFECTIVE_DATE, EXPIRATION_DATE, STATUS, CREATED_BY, CREATE_DATE 
  FROM HR.CCT_CONTRACTS_TMG 
  WHERE CREATE_DATE >= SYSDATE - 1 
  ORDER BY CREATE_DATE DESC
  ```
- **Display Columns**: CONTRACT_NAME, CUSTOMER_NAME, CREATE_DATE, STATUS

### **2. Parts Count**
- **Action Type**: `QUICK_ACTION_PARTS_COUNT`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: Show total parts loaded count
- **SQL Query**:
  ```sql
  SELECT COUNT(*) as TOTAL_PARTS 
  FROM HR.CCT_PARTS_TMG
  ```
- **Display**: Large centered number with description

### **3. Failed Contracts**
- **Action Type**: `QUICK_ACTION_FAILED_CONTRACTS`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: Show contracts and count for each contract (failed parts)
- **SQL Query**:
  ```sql
  SELECT c.CONTRACT_NAME, c.CUSTOMER_NAME, COUNT(fp.PART_NUMBER) as FAILED_PARTS_COUNT 
  FROM HR.CCT_CONTRACTS_TMG c 
  LEFT JOIN HR.CCT_FAILED_PARTS_TMG fp ON c.AWARD_NUMBER = fp.CONTRACT_NO 
  GROUP BY c.CONTRACT_NAME, c.CUSTOMER_NAME, c.AWARD_NUMBER 
  HAVING COUNT(fp.PART_NUMBER) > 0 
  ORDER BY FAILED_PARTS_COUNT DESC
  ```
- **Display Columns**: CONTRACT_NAME, CUSTOMER_NAME, FAILED_PARTS_COUNT

### **4. Expiring Soon**
- **Action Type**: `QUICK_ACTION_EXPIRING_SOON`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: Show contracts in expiring order (next 30 days)
- **SQL Query**:
  ```sql
  SELECT CONTRACT_NAME, CUSTOMER_NAME, EFFECTIVE_DATE, EXPIRATION_DATE, STATUS, 
         ROUND(EXPIRATION_DATE - SYSDATE) as DAYS_TO_EXPIRE 
  FROM HR.CCT_CONTRACTS_TMG 
  WHERE EXPIRATION_DATE >= SYSDATE 
  AND EXPIRATION_DATE <= SYSDATE + 30 
  ORDER BY EXPIRATION_DATE ASC
  ```
- **Display Columns**: CONTRACT_NAME, CUSTOMER_NAME, EXPIRATION_DATE, DAYS_TO_EXPIRE

### **5. Award Reps**
- **Action Type**: `QUICK_ACTION_AWARD_REPS`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: List award representatives with contract counts
- **SQL Query**:
  ```sql
  SELECT DISTINCT AWARD_REP, COUNT(*) as CONTRACT_COUNT 
  FROM HR.CCT_CONTRACTS_TMG 
  WHERE AWARD_REP IS NOT NULL 
  GROUP BY AWARD_REP 
  ORDER BY CONTRACT_COUNT DESC
  ```
- **Display Columns**: AWARD_REP, CONTRACT_COUNT

### **6. Help**
- **Action Type**: `QUICK_ACTION_HELP`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: Show default help message
- **Content**: Comprehensive help guide with all available features

### **7. Create Contract**
- **Action Type**: `QUICK_ACTION_CREATE_CONTRACT`
- **Query Type**: `QUICK_ACTION`
- **Purpose**: Show step-by-step contract creation guide
- **Content**: Detailed manual creation steps and automated creation format

## 🔧 **Implementation Details**

### **Central Method**
```java
public String handleQuickActionButton(String actionType)
```

### **Usage Pattern**
```java
// In your UI component
NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
String htmlResponse = handler.handleQuickActionButton("QUICK_ACTION_RECENT_CONTRACTS");
// Add to chat: addMessage(htmlResponse);
```

### **Return Format**
All methods return HTML-formatted strings that can be directly added to the chat interface using `addMessage()`.

## 📊 **Data Flow**

```
Button Click → handleQuickActionButton() → Specific Action Method → SQL Query → HTML Response → addMessage()
```

## 🎨 **Styling**

### **Parts Count Display**
```html
<div style='text-align: center; padding: 20px;'>
  <h3>Total Parts Loaded</h3>
  <div style='font-size: 48px; font-weight: bold; color: #007bff;'>1234</div>
  <p style='color: #666;'>Total number of parts in the system</p>
</div>
```

### **Help & Create Contract**
- Background colors for visual distinction
- Structured lists and formatting
- Professional styling for better UX

## 🔄 **Integration Points**

### **1. Table Configuration**
All queries use `TableColumnConfig` for table names:
- `TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_CONTRACTS)`
- `TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_PARTS)`
- `TABLE_CONFIG.getTableName(TableColumnConfig.TABLE_FAILED_PARTS)`

### **2. Existing Formatting**
Uses existing `formatQueryResultInView()` method for consistent table formatting.

### **3. Error Handling**
Comprehensive try-catch blocks with user-friendly error messages.

### **4. Business Logic Isolation**
- All quick actions use `QUICK_ACTION` query type
- Action types prefixed with `QUICK_ACTION_`
- Does not interfere with existing NLP processing
- Separate from conversational flows

## 🚀 **Usage Examples**

### **Frontend Integration**
```javascript
// Button click handler
function handleButtonClick(actionType) {
    // Call backend method
    const response = await fetch('/api/quickAction', {
        method: 'POST',
        body: JSON.stringify({ actionType: actionType })
    });
    
    const htmlResponse = await response.text();
    addMessage(htmlResponse); // Add to chat interface
}
```

### **Direct Method Call**
```java
// In your Java component
String recentContracts = handler.handleQuickActionButton("QUICK_ACTION_RECENT_CONTRACTS");
String partsCount = handler.handleQuickActionButton("QUICK_ACTION_PARTS_COUNT");
String help = handler.handleQuickActionButton("QUICK_ACTION_HELP");
```

## 📝 **Action Types Summary**

| Button | Action Type | Query Type | Description |
|--------|-------------|------------|-------------|
| Recent Contracts | `QUICK_ACTION_RECENT_CONTRACTS` | `QUICK_ACTION` | Last 24 hours contracts |
| Parts Count | `QUICK_ACTION_PARTS_COUNT` | `QUICK_ACTION` | Total parts count |
| Failed Contracts | `QUICK_ACTION_FAILED_CONTRACTS` | `QUICK_ACTION` | Contracts with failed parts |
| Expiring Soon | `QUICK_ACTION_EXPIRING_SOON` | `QUICK_ACTION` | Contracts expiring in 30 days |
| Award Reps | `QUICK_ACTION_AWARD_REPS` | `QUICK_ACTION` | Award representatives list |
| Help | `QUICK_ACTION_HELP` | `QUICK_ACTION` | Help guide |
| Create Contract | `QUICK_ACTION_CREATE_CONTRACT` | `QUICK_ACTION` | Contract creation guide |

## 🔍 **Testing**

### **Test Cases**
1. **Recent Contracts**: Verify 24-hour filter and sorting
2. **Parts Count**: Verify count accuracy and display
3. **Failed Contracts**: Verify JOIN logic and grouping
4. **Expiring Soon**: Verify 30-day range and days calculation
5. **Award Reps**: Verify grouping and sorting
6. **Help**: Verify content completeness
7. **Create Contract**: Verify step accuracy

### **Error Scenarios**
- Database connection issues
- Empty result sets
- Invalid action types
- SQL execution errors

## 🎯 **Benefits**

1. **Quick Access**: Instant access to common queries
2. **Consistent Formatting**: Unified HTML response format
3. **Centralized Logic**: Single method for all button actions
4. **Error Handling**: Robust error management
5. **Extensible**: Easy to add new button actions
6. **User-Friendly**: Clear, formatted responses
7. **Business Logic Isolation**: Does not interfere with existing features
8. **Command Button Design**: Clear distinction from conversational flows

## 🔮 **Future Enhancements**

1. **Caching**: Cache results for better performance
2. **Real-time Updates**: Auto-refresh for time-sensitive data
3. **Customization**: User-configurable button actions
4. **Analytics**: Track most-used button actions
5. **Export**: Add export functionality for results 