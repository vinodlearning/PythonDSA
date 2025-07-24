# Contract Creation Workflow Integration Guide

## Overview

This guide explains how to integrate the Contract Creation Workflow system with your existing Oracle ADF chat application. The system provides a comprehensive solution for handling multi-step contract creation processes with centralized configuration and session management.

## Integration Steps

### Step 1: Add the Contract Creation Classes

Add the following classes to your project:

1. **ContractCreationWorkflowManager.java** - Main workflow orchestrator
2. **ContractCreationConfig.java** - Centralized configuration management
3. **ContractCreationIntegration.java** - Integration with existing NLP system
4. **ContractCreationTest.java** - Comprehensive test suite

### Step 2: Modify NLPUserActionHandler

Update your `NLPUserActionHandler` class to integrate contract creation workflow:

```java
// Add this import
import com.oracle.view.source.ContractCreationIntegration;

// Add this field to the class
private ContractCreationIntegration contractCreationIntegration;

// Initialize in constructor or init method
public NLPUserActionHandler() {
    // ... existing initialization ...
    this.contractCreationIntegration = new ContractCreationIntegration();
}

// Modify the processUserInputJSONResponse method
public String processUserInputJSONResponse(String userInput, String sessionId) {
    try {
        // Check if this is a contract creation request
        if (contractCreationIntegration.isContractCreationRelated(userInput)) {
            return contractCreationIntegration.processContractCreationInput(userInput, sessionId);
        }
        
        // ... existing processing logic ...
        
    } catch (Exception e) {
        // ... error handling ...
    }
}
```

### Step 3: Update UI Components

The contract creation workflow returns JSON responses that are compatible with your existing UI. The responses include:

- `requiresFollowUp: true` - Indicates the workflow needs more input
- `workflowType: "CONTRACT_CREATION"` - Identifies the workflow type
- `nextStep` - The next step in the workflow
- `sessionId` - Session identifier for tracking

Your UI should handle these responses appropriately:

```javascript
// Example UI handling
function handleContractCreationResponse(response) {
    if (response.moduleSpecificData && response.moduleSpecificData.requiresFollowUp) {
        // Show the prompt to the user
        displayMessage(response.processingResult);
        
        // Store session information for follow-up
        storeSessionInfo(response.moduleSpecificData.sessionId, response.moduleSpecificData.workflowType);
    } else {
        // Process the final result
        displayResult(response.processingResult);
    }
}
```

### Step 4: Configure Contract Creation Attributes

The system uses centralized configuration that can be easily modified:

```java
// Example: Add a new required attribute
ContractCreationConfig config = ContractCreationConfig.getInstance();
config.addAttributeConfig(new ContractCreationConfig.AttributeConfig(
    "CONTRACT_VALUE",
    "Contract Value",
    "Please provide the contract value:",
    "\\d+(\\.\\d{2})?",
    "Please provide a valid contract value.",
    true,
    1,
    20,
    "NUMBER",
    null
));
```

## Configuration Management

### Current Required Attributes

The system comes with these pre-configured required attributes:

1. **CUSTOMER_NUMBER** - Customer account number (4-8 digits)
2. **CONTRACT_NAME** - Contract name (3-100 characters)
3. **TITLE** - Contract title (3-200 characters)
4. **DESCRIPTION** - Contract description (10-500 characters)
5. **EFFECTIVE_DATE** - Effective date (MM-DD-YYYY format)
6. **EXPIRATION_DATE** - Expiration date (MM-DD-YYYY format)

### Optional Attributes

1. **CONTRACT_TYPE** - Contract type (SERVICE, SUPPLY, LICENSE, MAINTENANCE, SUPPORT)
2. **PAYMENT_TERMS** - Payment terms (3-100 characters)
3. **CURRENCY** - Currency code (USD, EUR, GBP, CAD, AUD, JPY)

### Modifying Configuration

You can modify the configuration at runtime:

```java
// Add new attribute
config.addAttributeConfig(newAttributeConfig);

// Update existing attribute
config.updateAttributeConfig(updatedAttributeConfig);

// Remove attribute
config.removeAttributeConfig("ATTRIBUTE_NAME");
```

## Workflow Flow

### 1. Initial Request Detection

The system automatically detects contract creation requests using patterns:
- "create contract"
- "new contract"
- "start contract"
- "I want to create a contract"

### 2. Progressive Data Collection

The system guides users through each required attribute:
- Validates input according to configuration rules
- Provides clear error messages for invalid input
- Extracts and stores values appropriately
- Moves to the next required attribute

### 3. Completion

When all required attributes are collected:
- Builds a complete contract creation query
- Processes through your existing NLP system
- Clears the session
- Returns the final result

## Chain Management

### Chain Break Detection

The system handles conversation interruptions gracefully:

1. **Unrelated Queries**: If user asks something unrelated, the system informs them that contract creation is not active
2. **New Requests**: If user starts a new contract creation, the previous session is cleared
3. **Session Timeout**: Sessions expire after 10 minutes of inactivity

### Chain Resumption

The system can resume previous contract creation sessions if the user returns to the workflow.

## Testing

### Running Tests

```java
// Run the comprehensive test suite
ContractCreationTest.main(new String[0]);

// Or test specific scenarios
ContractCreationIntegration integration = new ContractCreationIntegration();
String response = integration.processContractCreationInput("create contract", "test_session");
```

### Test Scenarios

The test suite covers:
1. Complete workflow from start to finish
2. Chain break and resumption
3. Validation and error handling
4. Session management
5. Multiple concurrent sessions

## Example Usage

### Basic Contract Creation Flow

```
User: create contract
Bot: Please provide the customer account number:

User: 12345
Bot: Please provide a name for the contract:

User: ABC Aerospace Contract
Bot: Please provide a title for the contract:

User: Aerospace Parts Supply Agreement
Bot: Please provide a description for the contract:

User: Long-term supply agreement for aerospace parts and components
Bot: Please provide the effective date (MM-DD-YYYY):

User: 01-01-2024
Bot: Please provide the expiration date (MM-DD-YYYY):

User: 12-31-2026
Bot: Contract creation data complete. Processing contract creation...
```

### Chain Break Example

```
User: create contract
Bot: Please provide the customer account number:

User: show contract 100476
Bot: Contract creation workflow not active. Please start a new contract creation request.

User: 12345
Bot: Please provide a name for the contract:
```

## Response Format

The system returns JSON responses compatible with your existing UI:

```json
{
  "header": {
    "contractNumber": null,
    "partNumber": null,
    "customerNumber": null,
    "customerName": null,
    "createdBy": null,
    "inputTracking": {
      "originalInput": "create contract",
      "correctedInput": null,
      "correctionConfidence": 1.0
    }
  },
  "queryMetadata": {
    "queryType": "HELP",
    "actionType": "HELP_CONTRACT_CREATE_BOT",
    "processingTimeMs": 25,
    "selectedModule": "CONTRACT_CREATION",
    "routingConfidence": 0.95
  },
  "entities": [],
  "displayEntities": [],
  "moduleSpecificData": {
    "requiresFollowUp": true,
    "workflowType": "CONTRACT_CREATION",
    "nextStep": "CUSTOMER_NUMBER",
    "sessionId": "user_session_123"
  },
  "errors": [],
  "confidence": 0.95,
  "processingResult": "Please provide the customer account number:"
}
```

## Session Management

### Session Lifecycle

1. **Creation**: When contract creation is initiated
2. **Active**: During data collection process
3. **Complete**: When all required data is collected
4. **Cancelled**: When user cancels or starts new session
5. **Timeout**: When session expires due to inactivity

### Session Cleanup

- **Automatic**: Background thread cleans up expired sessions every minute
- **Manual**: Sessions can be cleared programmatically
- **Timeout**: Sessions expire after 10 minutes of inactivity

## Debugging

### Session Information

Get detailed session information for debugging:

```java
String sessionInfo = integration.getSessionInfo(sessionId);
System.out.println(sessionInfo);
```

### Active Sessions

Monitor active sessions:

```java
Map<String, ContractCreationWorkflowManager.ContractCreationSession> activeSessions = 
    integration.getActiveSessions();
for (String id : activeSessions.keySet()) {
    System.out.println("Active session: " + id);
}
```

## Benefits

### For Business Users

1. **Centralized Configuration**: All contract creation requirements in one place
2. **Easy Modification**: Add/remove/modify attributes without code changes
3. **Flexible Validation**: Custom validation rules and error messages
4. **Business Rule Enforcement**: Ensures data quality and consistency

### For Developers

1. **Modular Design**: Clean separation of concerns
2. **Extensible Architecture**: Easy to add new workflow types
3. **Comprehensive Testing**: Built-in test suite for validation
4. **Backward Compatibility**: Works with existing system

### For Users

1. **Guided Experience**: Step-by-step contract creation process
2. **Clear Prompts**: User-friendly questions and error messages
3. **Flexible Input**: Handles various input formats and patterns
4. **Chain Management**: Handles conversation interruptions gracefully

## Troubleshooting

### Common Issues

1. **Session Not Found**: Check if session ID is correct and session hasn't expired
2. **Validation Errors**: Verify input format matches configuration requirements
3. **Chain Break**: Ensure user input is related to current workflow step
4. **Configuration Issues**: Check attribute configuration for syntax errors

### Debug Information

Use the session info method to get detailed debug information:

```java
String sessionInfo = integration.getSessionInfo(sessionId);
System.out.println(sessionInfo);
```

## Conclusion

The Contract Creation Workflow system provides a robust, flexible, and user-friendly solution for handling multi-step contract creation processes. Its centralized configuration approach makes it easy to modify business requirements without code changes, while its comprehensive session management ensures reliable conversation handling.

The system integrates seamlessly with your existing Oracle ADF chat application while providing advanced workflow capabilities for complex business processes. The modular design makes it easy to extend and maintain, and the comprehensive test suite ensures reliability.

For support or questions, refer to the comprehensive documentation in `ContractCreationWorkflowDocumentation.md`. 