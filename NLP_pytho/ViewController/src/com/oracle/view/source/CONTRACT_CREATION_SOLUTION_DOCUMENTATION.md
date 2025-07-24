# BCCT Contract Management NLP Solution Documentation

## Overview

This document describes the comprehensive contract management solution that handles all NLP interactions for contracts, parts, failed parts, opportunities, and customers. The solution includes a new ADF managed bean (`BCCTContractManagementNLPBean`) that can be registered with ADF taskflow and handles multiple user inputs with robust state management.

## Architecture Components

### 1. BCCTContractManagementNLPBean.java
**Purpose**: Main ADF managed bean for comprehensive contract management
**Key Features**:
- Session-scoped managed bean with `@ManagedBean(name = "bcctContractManagementNLPBean")`
- Handles all NLP interactions for contracts, parts, failed parts, opportunities, and customers
- Integrates with existing `NLPUserActionHandler` and `ConversationalFlowManager`
- Manages `ChatMessage` objects with sender, message, time, and isBot properties
- Provides UI binding for ADF components

### 2. ContractCreationFlowManager.java
**Purpose**: Manages stateful contract creation flows
**Key Features**:
- 300-second session timeout
- Flow state management with ConversationalFlowManager integration
- Support for flow interruption and cancellation
- In-memory storage for flow states (production-ready for database/cache)

### 3. Enhanced NLPUserActionHandler.java
**Purpose**: Enhanced NLP processing with contract creation capabilities
**Key Features**:
- Intent classification for manual vs automated contract creation
- Business rule validation (7+ digit account numbers, 6-digit contract numbers)
- ADF-compatible HTML output
- Progressive field collection for contract creation

## Key Features

### 1. User Intent Classification
The system intelligently distinguishes between two types of contract creation requests:

**Manual Steps Requests** (HELP_CONTRACT_CREATE_USER):
- Trigger phrases: "steps to create contract", "how to create", "need help with contract steps"
- Response: Provides step-by-step instructions for manual contract creation

**Automated Creation Requests** (HELP_CONTRACT_CREATE_BOT):
- Trigger phrases: "create contract", "make contract [account]", "why can't you create contract"
- Response: Initiates automated contract creation flow

### 2. Business Rules Implementation
- **Account Numbers**: 7+ digits (customer numbers)
- **Contract Numbers**: Exactly 6 digits
- **Part Numbers**: Alphanumeric combinations (e.g., "AE1337-ERT")
- **Opportunities**: "CRF" + digits format
- **Column Mappings**: 
  - Contracts: `AWARD_NUMBER`
  - Parts: `LOADED_CP_NUMBER`
  - Failed Parts: `CONTRACT_NO`

### 3. Flow Management
- **Session Timeout**: 300 seconds (5 minutes) of inactivity
- **Flow Interruption**: Detects when users change topics mid-flow
- **Progressive Collection**: Step-by-step field collection
- **State Persistence**: Maintains flow state across user interactions

## Integration Points

### 1. ADF Taskflow Registration
```xml
<!-- In your taskflow definition -->
<managed-bean>
    <managed-bean-name>bcctContractManagementNLPBean</managed-bean-name>
    <managed-bean-class>com.oracle.view.source.BCCTContractManagementNLPBean</managed-bean-class>
    <managed-bean-scope>session</managed-bean-scope>
</managed-bean>
```

### 2. UI Component Binding
```xml
<!-- User input field -->
<af:inputText id="userInputText" 
              value="#{bcctContractManagementNLPBean.userInput}"
              label="Enter your question:">
    <af:clientListener method="handleUserInput" type="keyUp"/>
</af:inputText>

<!-- Send button -->
<af:commandButton text="Send" 
                  actionListener="#{bcctContractManagementNLPBean.processUserInput}"/>

<!-- Chat history -->
<af:table value="#{bcctContractManagementNLPBean.chatHistory}" 
          var="message"
          rowBandingInterval="0">
    <af:column headerText="Sender">
        <af:outputText value="#{message.sender}"/>
    </af:column>
    <af:column headerText="Message">
        <af:outputText value="#{message.message}" escape="false"/>
    </af:column>
    <af:column headerText="Time">
        <af:outputText value="#{message.time}"/>
    </af:column>
</af:table>
```

### 3. Client Event Handling
```javascript
// JavaScript for real-time processing
function handleUserInput(event) {
    if (event.keyCode === 13) { // Enter key
        var input = event.getSource().getValue();
        AdfCustomEvent.queue(event.getSource(), "userInputSubmitted", 
                           {userInput: input}, true);
    }
}
```

## Usage Examples

### 1. Contract Creation - Manual Steps
**User Input**: "steps to create contract"
**Response**: 
```
Here are the steps to create a contract manually:

1. Navigate to Contract Management
   Go to the main menu and select "Contract Management"

2. Click "Create New Contract"
   In the Contract Management screen, click the "Create" button

3. Enter Account Information
   Provide the customer account number (7+ digits)

4. Fill Contract Details
   Enter contract name, title, description, and other required fields

5. Set Dates
   Specify effective date, expiration date, and price expiration date

6. Configure Options
   Set price list, EM status, and other optional parameters

7. Review and Save
   Review all information and click "Save" to create the contract

Would you like me to help you create a contract automatically instead?
Just say "Create contract for account [number]" and I'll guide you through it!
```

### 2. Contract Creation - Automated Flow
**User Input**: "create contract for account 12345678"
**Flow Steps**:
1. **Account Validation**: Validates 7+ digit account number
2. **Contract Name**: "Contract name: ABC Project Services"
3. **Title**: "Title: Software Development Services"
4. **Description**: "Description: This contract covers software development services"
5. **Optional Fields**: Price List, isEM, Effective Date, Expiration Date
6. **Confirmation**: Final review and contract creation

### 3. Contract Information Queries
**User Input**: "show contract ABC123"
**Response**: Contract details with ADF-compatible HTML formatting

### 4. Parts Queries
**User Input**: "how many parts for contract ABC123"
**Response**: Parts count and details for the specified contract

### 5. Customer Queries
**User Input**: "account 10840607(HONEYWELL INTERNATIONAL INC.)"
**Response**: Customer details and information

### 6. Opportunity Queries
**User Input**: "show opportunities"
**Response**: List of opportunities with CRF format

## Testing

### BCCTContractManagementTestSuite.java
Comprehensive test suite that demonstrates all functionality:

```java
// Run all tests
BCCTContractManagementTestSuite testSuite = new BCCTContractManagementTestSuite();
testSuite.runAllTests();

// Test specific scenarios
testSuite.testBusinessRules();
testSuite.testFlowManagement();
```

**Test Categories**:
1. Contract Creation - Manual Steps
2. Contract Creation - Automated Flow
3. Contract Information Queries
4. Parts Queries
5. Failed Parts Queries
6. Customer Queries
7. Opportunity Queries
8. Mixed Queries
9. Business Rules Validation
10. Flow Management

## Configuration

### 1. Business Rules Configuration
```java
// Account number validation (7+ digits)
private boolean isCustomerNumberValid(String accountNo) {
    return accountNo != null && accountNo.matches("\\d{7,}");
}

// Contract number validation (exactly 6 digits)
private boolean isContractNumberValid(String contractNo) {
    return contractNo != null && contractNo.matches("\\d{6}");
}

// Part number extraction (alphanumeric)
private String extractPartNumber(String input) {
    // Regex for alphanumeric part numbers
    Pattern pattern = Pattern.compile("\\b[A-Z0-9]+(?:-[A-Z0-9]+)*\\b");
    // Implementation details...
}
```

### 2. Flow Configuration
```java
// Session timeout (300 seconds)
private static final long SESSION_TIMEOUT_MS = 300000;

// Flow steps
private static final String[] FLOW_STEPS = {
    "COLLECT_CONTRACT_NAME",
    "COLLECT_TITLE", 
    "COLLECT_DESCRIPTION",
    "COLLECT_OPTIONAL_FIELDS",
    "CONFIRM_CREATION"
};
```

## Error Handling

### 1. Validation Errors
- Invalid account numbers with clear error messages
- Missing required fields with contextual prompts
- Business rule violations with explanation

### 2. Flow Errors
- Session timeout handling
- Flow interruption detection
- Graceful error recovery

### 3. System Errors
- Comprehensive exception handling
- User-friendly error messages
- Logging for debugging

## Performance Considerations

### 1. Memory Management
- Session-scoped beans for user-specific data
- Proper cleanup of expired flows
- Efficient chat history management

### 2. Response Time
- Optimized NLP processing
- Cached business rules
- Efficient database queries

### 3. Scalability
- Stateless NLP processing
- Session management for multiple users
- Database connection pooling

## Security

### 1. Input Validation
- SQL injection prevention
- XSS protection
- Input sanitization

### 2. Session Security
- Secure session management
- Timeout enforcement
- Access control

### 3. Data Protection
- Sensitive data encryption
- Audit logging
- Access monitoring

## Deployment

### 1. ADF Application Integration
1. Add the bean classes to your ADF project
2. Register the managed bean in taskflow
3. Configure UI bindings
4. Test with ContractCreationTestSuite

### 2. Database Integration
1. Configure database connections
2. Set up required tables and views
3. Configure business rule validations
4. Test data access

### 3. Production Considerations
1. Configure logging and monitoring
2. Set up error handling and alerts
3. Configure performance tuning
4. Implement backup and recovery

## Maintenance

### 1. Regular Tasks
- Monitor session timeouts
- Clean up expired flows
- Review error logs
- Update business rules

### 2. Updates and Enhancements
- Add new query types
- Enhance business rules
- Improve NLP accuracy
- Add new flow steps

### 3. Troubleshooting
- Check session state
- Review flow logs
- Validate business rules
- Test NLP processing

## Conclusion

The BCCTContractManagementNLPBean provides a comprehensive, production-ready solution for contract management with robust NLP capabilities. The solution is designed to be easily integrated into existing ADF applications while maintaining backward compatibility and providing a solid foundation for future enhancements.

Key benefits:
- **Comprehensive Coverage**: Handles all contract-related queries
- **Robust Flow Management**: Stateful conversation handling
- **Business Rule Compliance**: Enforces all business rules
- **ADF Integration**: Seamless integration with existing ADF applications
- **Extensible Design**: Easy to add new features and capabilities
- **Production Ready**: Includes error handling, security, and performance considerations 