# Contract Creation Workflow Documentation

## Overview

The Contract Creation Workflow system provides a comprehensive solution for handling multi-step contract creation processes in the Oracle ADF chat application. It uses centralized configuration, session management, and chain validation to ensure a smooth user experience.

## Architecture

### Core Components

1. **ContractCreationWorkflowManager** - Main workflow orchestrator
2. **ContractCreationConfig** - Centralized configuration management
3. **ContractCreationIntegration** - Integration with existing NLP system
4. **ContractCreationTest** - Comprehensive test suite

### Key Features

- **Centralized Configuration**: All contract creation attributes and validation rules are managed in one place
- **Session Management**: Tracks conversation state and handles timeouts
- **Chain Validation**: Ensures proper conversation flow and handles chain breaks
- **Progressive Data Collection**: Collects required information step by step
- **Business Rule Validation**: Validates input according to business rules
- **Backward Compatibility**: Works seamlessly with existing single-turn queries

## Configuration

### ContractCreationConfig

The configuration system allows business users to modify contract creation requirements without code changes:

```java
// Example: Adding a new required attribute
ContractCreationConfig config = ContractCreationConfig.getInstance();
config.addAttributeConfig(new AttributeConfig(
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

### Attribute Configuration

Each attribute has the following configuration:

- **attributeName**: Internal identifier
- **displayName**: User-friendly name
- **prompt**: Question to ask the user
- **validationPattern**: Regex pattern for validation
- **validationMessage**: Error message for invalid input
- **required**: Whether the attribute is mandatory
- **minLength/maxLength**: Length constraints
- **dataType**: Type of data (TEXT, NUMBER, DATE, SELECT, CURRENCY)
- **allowedValues**: List of valid values for SELECT/CURRENCY types

### Current Required Attributes

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

## Workflow Flow

### 1. Initial Request Detection

The system detects contract creation requests using patterns:
- "create contract"
- "new contract"
- "start contract"
- "I want to create a contract"

### 2. Session Creation

When a contract creation request is detected:
- Creates a new ContractCreationSession
- Initializes with required attributes from configuration
- Sets current step to first required attribute
- Returns prompt for first step

### 3. Progressive Data Collection

For each step:
- Validates user input against configuration rules
- Extracts and stores the value
- Moves to next required attribute
- Returns prompt for next step

### 4. Completion

When all required attributes are collected:
- Builds complete contract creation query
- Processes through existing NLP system
- Clears the session
- Returns final result

## Chain Management

### Chain Validation

The system tracks conversation chains to ensure proper flow:

1. **Active Session Check**: Checks if user has an active contract creation session
2. **Input Validation**: Validates input against current step requirements
3. **Chain Break Detection**: Detects when user breaks the conversation chain
4. **Resumption Handling**: Allows resumption of previous sessions

### Chain Break Scenarios

1. **Unrelated Query**: User asks something unrelated to contract creation
2. **New Contract Creation**: User starts a new contract creation while one is in progress
3. **Session Timeout**: Session expires due to inactivity

### Chain Break Responses

- **System Message**: Informs user that contract creation workflow is not active
- **Session Clearance**: Clears any existing sessions
- **New Session**: Starts fresh contract creation workflow

## Integration

### With Existing NLP System

The ContractCreationIntegration class provides seamless integration:

```java
ContractCreationIntegration integration = new ContractCreationIntegration();

// Process contract creation input
String response = integration.processContractCreationInput(userInput, sessionId);

// Check if input is contract creation related
boolean isRelated = integration.isContractCreationRelated(userInput);
```

### Response Format

The system returns JSON responses compatible with the existing UI:

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

### Session Data

Each session tracks:
- Session ID and workflow ID
- Current step and status
- Collected data and validation errors
- Creation and last activity timestamps
- Required attributes and completed steps
- Context information

### Session Cleanup

- **Automatic**: Background thread cleans up expired sessions every minute
- **Manual**: Sessions can be cleared programmatically
- **Timeout**: Sessions expire after 10 minutes of inactivity

## Testing

### Test Scenarios

The ContractCreationTest class provides comprehensive testing:

1. **Complete Workflow**: Full contract creation from start to finish
2. **Chain Break and Resumption**: Handling interrupted conversations
3. **Validation and Errors**: Testing various validation scenarios
4. **Session Management**: Testing session lifecycle and cleanup
5. **Multiple Sessions**: Testing concurrent contract creation sessions

### Running Tests

```java
// Run all tests
ContractCreationTest.main(new String[0]);

// Or run specific test methods
ContractCreationIntegration integration = new ContractCreationIntegration();
integration.processContractCreationInput("create contract", "test_session");
```

## Usage Examples

### Basic Contract Creation

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

### Validation Error Example

```
User: create contract
Bot: Please provide the customer account number:

User: 123
Bot: Please provide a valid customer account number (4-8 digits).

User: 12345
Bot: Please provide a name for the contract:
```

## Configuration Management

### Adding New Attributes

To add a new required attribute:

```java
ContractCreationConfig config = ContractCreationConfig.getInstance();

// Add new attribute
config.addAttributeConfig(new AttributeConfig(
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

### Modifying Existing Attributes

To modify an existing attribute:

```java
ContractCreationConfig config = ContractCreationConfig.getInstance();

// Update attribute
config.updateAttributeConfig(new AttributeConfig(
    "CUSTOMER_NUMBER",
    "Customer Account Number",
    "Please provide the customer account number (5-10 digits):",
    "\\d{5,10}",
    "Please provide a valid customer account number (5-10 digits).",
    true,
    5,
    10,
    "NUMBER",
    null
));
```

### Removing Attributes

To remove an attribute:

```java
ContractCreationConfig config = ContractCreationConfig.getInstance();
config.removeAttributeConfig("CONTRACT_TYPE");
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

## Future Enhancements

### Planned Features

1. **Dynamic Configuration**: Load configuration from database or files
2. **Workflow Templates**: Predefined contract creation templates
3. **Conditional Logic**: Show/hide attributes based on previous answers
4. **Multi-language Support**: Internationalization for prompts and messages
5. **Advanced Validation**: Complex business rule validation
6. **Integration APIs**: REST APIs for external system integration

### Extension Points

1. **Custom Validators**: Plugin system for custom validation logic
2. **Workflow Hooks**: Pre/post processing hooks for custom logic
3. **Notification System**: Email/SMS notifications for workflow events
4. **Audit Trail**: Complete audit trail of workflow activities
5. **Reporting**: Workflow analytics and reporting capabilities

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

### Logging

The system provides comprehensive debug logging:
- Session creation and updates
- Input validation results
- Chain break detection
- Configuration loading
- Error conditions

## Conclusion

The Contract Creation Workflow system provides a robust, flexible, and user-friendly solution for handling multi-step contract creation processes. Its centralized configuration approach makes it easy to modify business requirements without code changes, while its comprehensive session management ensures reliable conversation handling.

The system is designed to be extensible and maintainable, with clear separation of concerns and comprehensive testing. It integrates seamlessly with the existing NLP system while providing advanced workflow capabilities for complex business processes. 