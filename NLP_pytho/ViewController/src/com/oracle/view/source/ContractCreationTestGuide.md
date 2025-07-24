# Contract Creation Test Guide

## 🚀 Ready to Test!

Your contract creation workflow is now fully integrated and ready to test from the ADF UI. Here's how to test it:

## 📋 Testing Steps

### Step 1: Access the Chat UI
1. Open your Oracle ADF application
2. Navigate to the chat interface (`chat.jsff`)
3. You should see the chat interface with the new "📝 Create Contract" button

### Step 2: Test Contract Creation Workflow

#### Option A: Use the Quick Action Button
1. Click the **"📝 Create Contract"** button in the Quick Actions section
2. The system will automatically start the contract creation workflow

#### Option B: Type Manually
1. Type `create contract` in the input field
2. Press Enter or click Send

### Step 3: Follow the Workflow

The system will guide you through each step:

```
Bot: Please provide the customer account number:

You: 12345

Bot: Please provide a name for the contract:

You: ABC Aerospace Contract

Bot: Please provide a title for the contract:

You: Aerospace Parts Supply Agreement

Bot: Please provide a description for the contract:

You: Long-term supply agreement for aerospace parts and components

Bot: Please provide the effective date (MM-DD-YYYY):

You: 01-01-2024

Bot: Please provide the expiration date (MM-DD-YYYY):

You: 12-31-2026

Bot: Contract creation data complete. Processing contract creation...
```

## 🧪 Test Scenarios

### 1. Complete Workflow Test
- Follow all steps from start to finish
- Provide valid data for each field
- Verify the system completes successfully

### 2. Validation Test
- Try invalid customer numbers (too short/long)
- Try invalid date formats
- Try empty responses
- Verify error messages are clear and helpful

### 3. Chain Break Test
- Start contract creation
- In the middle, ask something unrelated like "show contract 100476"
- Verify the system handles the interruption gracefully
- Try to resume contract creation

### 4. Session Management Test
- Start contract creation
- Wait for 10 minutes (or modify timeout in code)
- Try to continue - should get session expired message

### 5. Multiple Sessions Test
- Open multiple browser tabs/windows
- Start contract creation in each
- Verify sessions don't interfere with each other

## 🔧 Configuration Testing

### Test Adding New Attributes
You can test the configuration system by adding new attributes:

```java
// In your application, you can add this code to test:
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

### Test Modifying Existing Attributes
```java
// Modify customer number validation
config.updateAttributeConfig(new ContractCreationConfig.AttributeConfig(
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

## 📊 Expected Results

### Successful Workflow
- System detects "create contract" input
- Prompts for each required attribute in order
- Validates input and provides clear error messages
- Collects all required data
- Processes the final contract creation
- Clears the session

### Error Handling
- Invalid input shows specific error messages
- Chain breaks are handled gracefully
- Session timeouts are managed properly
- System remains stable throughout

### UI Integration
- Messages appear in the chat interface
- User input is displayed correctly
- Bot responses are formatted properly
- Session state is maintained

## 🐛 Troubleshooting

### If Contract Creation Doesn't Start
1. Check browser console for JavaScript errors
2. Verify the bean is properly initialized
3. Check server logs for any exceptions
4. Ensure all contract creation classes are compiled

### If Validation Doesn't Work
1. Check the ContractCreationConfig class
2. Verify attribute configurations are correct
3. Test validation patterns manually

### If Session Management Issues
1. Check session timeout settings
2. Verify session cleanup is working
3. Check for memory leaks in session storage

## 📝 Test Checklist

- [ ] Contract creation button appears in UI
- [ ] "create contract" command is recognized
- [ ] System prompts for customer number
- [ ] Customer number validation works
- [ ] System prompts for contract name
- [ ] Contract name validation works
- [ ] System prompts for title
- [ ] Title validation works
- [ ] System prompts for description
- [ ] Description validation works
- [ ] System prompts for effective date
- [ ] Date validation works
- [ ] System prompts for expiration date
- [ ] Workflow completes successfully
- [ ] Chain breaks are handled
- [ ] Session timeouts work
- [ ] Multiple sessions work independently

## 🎯 Success Criteria

The contract creation workflow is working correctly if:

1. **Detection**: System recognizes "create contract" requests
2. **Progressive Collection**: System asks for each attribute one by one
3. **Validation**: System validates input and shows clear error messages
4. **Completion**: System collects all required data and processes creation
5. **Chain Management**: System handles interruptions gracefully
6. **Session Management**: System manages sessions properly
7. **UI Integration**: Everything works seamlessly in the chat interface

## 🚀 Next Steps

Once testing is complete:

1. **Deploy to Production**: The system is ready for production use
2. **Configure Attributes**: Modify the configuration for your business needs
3. **Add More Workflows**: Use the same pattern for other multi-step processes
4. **Monitor Usage**: Track how users interact with the workflow
5. **Gather Feedback**: Collect user feedback for improvements

## 📞 Support

If you encounter any issues during testing:

1. Check the server logs for detailed error messages
2. Use the debug methods in ContractCreationIntegration
3. Verify all classes are properly compiled and deployed
4. Test with the standalone demo first to isolate issues

The contract creation workflow is now fully integrated and ready for testing! 🎉 