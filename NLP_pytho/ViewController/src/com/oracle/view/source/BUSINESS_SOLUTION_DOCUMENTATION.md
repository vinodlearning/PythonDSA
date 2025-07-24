# NLP Chatbot Business Solution Documentation

## Overview

This document describes the comprehensive business solution for the NLP Chatbot system that handles multiple business scenarios including parts queries, contract creation, customer data queries, and contract information queries.

## Architecture Flow

```
ADF UI Screen → BCCTChatbotBeanNLP → NLPUserActionHandler → NLPEntityProcessor
                ↓
            BCCTChatBotAppModuleImpl (Model Layer)
                ↓
            TableColumnConfig & Other Utilities
```

## Business Scenarios Handled

### 1. Parts Queries with Contract Number Validation

**Problem**: Parts can be associated with multiple contracts, so users must specify a contract number to get accurate information.

**Solution**: 
- Detect parts queries using keywords and patterns
- Validate that contract number is provided
- If no contract number, ask user to provide one with examples
- Route to appropriate database action based on query type

**Example Queries**:
- "What is the lead time for part EN6114V4-13?" → Asks for contract number
- "What is the lead time for part EN6114V4-13 in contract 100476?" → Processes query
- "Show me all parts for contract 100476" → Processes query

**Implementation**:
```java
// Detection method
private boolean isPartsQuery(String userInput)

// Contract number validation
private boolean hasContractNumberInPartsQuery(String userInput)

// Main handler
private String handlePartsQuery(List<NLPEntityProcessor.EntityFilter> filters, 
                               List<String> displayEntities, 
                               String userInput)
```

### 2. Contract Creation Flow

**Problem**: Users need to create contracts with proper validation and guided input collection.

**Solution**:
- Detect contract creation queries
- Validate account number if provided
- Guide users through required fields
- Save contract and show confirmation

**Flow**:
1. **Query Detection**: "Create contract for account 12345"
2. **Account Validation**: Check if account 12345 exists
3. **Field Collection**: Ask for required fields if valid
4. **Contract Creation**: Save contract with provided data
5. **Confirmation**: Show contract details with new contract number

**Required Fields**:
- Account Number (4-8 digits)
- Contract Name
- Title
- Price List (yes/no, default: no)
- Description
- Comments
- Effective Date
- Expiration Date
- Price Expiration Date
- System Loaded Data (yes/no, default: yes)

**Implementation**:
```java
// Detection method
private boolean isContractCreationQuery(String userInput)

// Account validation
private boolean validateCustomer(String accountNumber)

// Main handler
private String handleContractCreationQuery(List<NLPEntityProcessor.EntityFilter> filters, 
                                          List<String> displayEntities, 
                                          String userInput)
```

### 3. Customer Data Queries

**Problem**: Users need to query customer information from CRM_CTR_CUSTOMERS table.

**Solution**:
- Detect customer queries using keywords and patterns
- Extract customer numbers and specific attributes
- Route to appropriate database queries
- Handle typos and variations

**Customer Table Fields**:
```
CUST_ID, CUST_CARDEX_ID, CUSTOMER_NO, CUSTOMER_NAME, ACCOUNT_TYPE,
SALES_REP_ID, SALES_OWNER, SALES_TEAM, SALES_TEAM_NUMBER, SALES_MANAGER,
SALES_DIRECTOR, SALES_VP, SALES_OUTSIDE, IS_GT25, IS_ECOMM,
CURRENCY_CODE, PAYMENT_TERMS, INCO_TERMS, ULTIMTATE_DESTINATION,
IS_DFAR, IS_ASL_APPLICABLE, LINE_MIN, ORDER_MIN, WAREHOUSE_INFO,
IS_ACTIVE, PROGRAM_SOLUTION_REP, TYPE, ASL_CODE, HPPFLAG,
QUALITYENGINEER, ACCOUNTREP, AWARDREP, CARDEX_CUSTOMER_NO,
CREATED_DATE, CREATED_BY
```

**Example Queries**:
- "Is customer CUST_ID 12345 active?"
- "Who is the SALES_REP_ID for customer 'XYZ Ltd'?"
- "What is the ACCOUNT_TYPE for CUSTOMER_NO 4567?"
- "Is customer 6789 part of IS_DFAR?"

**Implementation**:
```java
// Detection method
private boolean isCustomerQuery(String userInput)

// Column determination
private List<String> determineSpecificCustomerColumns(String userInput)

// Main handler
private String handleCustomerQuery(List<NLPEntityProcessor.EntityFilter> filters, 
                                  List<String> displayEntities, 
                                  String userInput)
```

### 4. Contract Information Queries

**Problem**: Users need to query contract details, expiration dates, and active status.

**Solution**:
- Detect contract info queries
- Handle expiration date queries
- Handle active/expired status queries
- Route to appropriate database actions

**Example Queries**:
- "details about 100476"
- "when does contract 100476 expire?"
- "is 100476 active?"

**Implementation**:
```java
// Detection method
private boolean isContractInfoQuery(String userInput)

// Expiration date detection
private boolean isExpirationDateQuery(String userInput)

// Active status detection
private boolean isActiveStatusQuery(String userInput)
```

## Technical Implementation

### Query Routing Logic

The main routing method in `NLPUserActionHandler` now includes:

```java
private String routeToActionHandlerWithDataProvider(String actionType, 
                                                   List<NLPEntityProcessor.EntityFilter> filters, 
                                                   List<String> displayEntities, 
                                                   String userInput) {
    // Check for contract information queries
    if (isContractInfoQuery(userInput)) {
        return handleContractInfoQuery(filters, displayEntities, userInput);
    }
    
    // Check for parts queries
    if (isPartsQuery(userInput)) {
        return handlePartsQuery(filters, displayEntities, userInput);
    }
    
    // Check for customer queries
    if (isCustomerQuery(userInput)) {
        return handleCustomerQuery(filters, displayEntities, userInput);
    }
    
    // Check for contract creation queries
    if (isContractCreationQuery(userInput)) {
        return handleContractCreationQuery(filters, displayEntities, userInput);
    }
    
    // Default routing based on action type
    switch (actionType) {
        // ... existing cases
    }
}
```

### Error Handling

The system includes comprehensive error handling:

1. **Input Validation**: Check for required parameters
2. **Business Rule Validation**: Validate account numbers, contract numbers
3. **Database Error Handling**: Handle SQL exceptions gracefully
4. **User-Friendly Messages**: Provide clear guidance to users

### Spell Correction and Typos

The system handles common typos and variations:

- "custmer" → "customer"
- "contarct" → "contract"
- "leed time" → "lead time"
- "pric" → "price"
- "staus" → "status"

## Future Extensibility

### Opportunities Module

The system is designed to be easily extended for Opportunities queries:

1. **Detection Method**: `isOpportunityQuery(String userInput)`
2. **Handler Method**: `handleOpportunityQuery(...)`
3. **Column Mapping**: Add to `TableColumnConfig`
4. **Database Actions**: Add to `BCCTChatBotAppModuleImpl`

### Additional Business Modules

The architecture supports adding:
- Inventory queries
- Sales order queries
- Invoice queries
- Payment queries

## Testing

### Comprehensive Test Suite

The `ComprehensiveBusinessTest` class tests:

1. **Parts Queries**: All variations with contract number validation
2. **Contract Creation**: With and without account numbers
3. **Customer Queries**: All field types and variations
4. **Contract Info Queries**: Details, expiration, status

### Test Coverage

- ✅ Parts queries without contract number (should ask for contract)
- ✅ Parts queries with contract number (should process)
- ✅ Contract creation without account (should ask for details)
- ✅ Contract creation with account (should validate and proceed)
- ✅ Customer queries (should process)
- ✅ Contract info queries (should process)
- ✅ Typos and variations (should handle gracefully)

## Integration Points

### Model Layer (BCCTChatBotAppModuleImpl)

The following methods need to be implemented in the Model layer:

```java
// Customer validation
public boolean validateCustomer(String accountNumber)

// Contract creation
public String saveContract(Map<String, Object> contractParams)

// Customer queries
public List<Map<String, Object>> queryCustomers(String actionType, 
                                                String filterAttributes, 
                                                String filterValues, 
                                                String filterOperations, 
                                                String displayColumns)

// Contract queries
public List<Map<String, Object>> queryContracts(String actionType, 
                                                String filterAttributes, 
                                                String filterValues, 
                                                String filterOperations, 
                                                String displayColumns)
```

### Database Actions

Required database actions:

1. `validate_customer` - Validate customer account number
2. `save_contract` - Save new contract
3. `customers_by_filter` - Query customer data
4. `contracts_by_contractnumber` - Query contract by number
5. `parts_by_contract_number` - Query parts by contract
6. `parts_by_part_number` - Query specific part

## Configuration

### TableColumnConfig

The system uses centralized column configuration for:

- Field name mapping
- Business term validation
- Display entity determination
- Column validation

### WordDatabase

Spell correction and word normalization:

- Common typos
- Business terms
- Command words
- Context indicators

## User Experience

### Response Formatting

All responses are formatted with:

1. **Clear Headers**: Indicate response type
2. **Structured Data**: Tabular or label-value format
3. **Error Messages**: User-friendly error descriptions
4. **Guidance**: Examples and next steps
5. **Professional Notes**: Additional context

### Interactive Flow

The system supports conversational flow:

1. **Initial Query**: User asks question
2. **Validation**: System validates input
3. **Clarification**: If needed, ask for more details
4. **Processing**: Execute business logic
5. **Response**: Provide formatted results
6. **Follow-up**: Suggest next actions

## Security Considerations

1. **Input Sanitization**: All user input is sanitized
2. **SQL Injection Prevention**: Use parameterized queries
3. **Access Control**: Validate user permissions
4. **Data Privacy**: Protect sensitive information

## Performance Optimization

1. **Query Optimization**: Use appropriate indexes
2. **Caching**: Cache frequently accessed data
3. **Connection Pooling**: Efficient database connections
4. **Response Caching**: Cache similar queries

## Monitoring and Logging

1. **Query Logging**: Log all user queries
2. **Performance Metrics**: Track response times
3. **Error Tracking**: Monitor and alert on errors
4. **Usage Analytics**: Track feature usage

## Deployment

### Prerequisites

1. Java 8 or higher
2. Database connection configured
3. ADF framework (for UI integration)
4. Required JAR dependencies

### Configuration Files

1. `adf-settings.xml` - ADF configuration
2. Database connection properties
3. Table column configuration
4. Word database configuration

### Build and Deploy

1. Compile all Java classes
2. Package as JAR/WAR
3. Deploy to application server
4. Configure database connections
5. Test all business scenarios

## Support and Maintenance

### Troubleshooting

1. **Query Detection Issues**: Check keyword patterns
2. **Database Errors**: Verify connection and permissions
3. **Performance Issues**: Check query optimization
4. **Integration Issues**: Verify Model layer implementation

### Maintenance Tasks

1. **Regular Updates**: Update word database with new terms
2. **Performance Monitoring**: Monitor query performance
3. **Error Analysis**: Analyze and fix common errors
4. **Feature Enhancement**: Add new business scenarios

## Conclusion

This comprehensive solution provides a robust, extensible NLP chatbot system that handles all the specified business scenarios while maintaining good user experience and system performance. The modular architecture allows for easy extension to new business domains like Opportunities in the future. 