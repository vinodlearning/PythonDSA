# Complete Response System Documentation

## Overview
The NLP system now returns a **complete response format** that is consistent across all query types and ready for UI integration.

## Response Format
Every response follows this structure:

```json
{
  "success": boolean,
  "message": "string",
  "nlpResponse": {
    "originalInput": "string",
    "correctedInput": "string", 
    "queryType": "string",
    "actionType": "string",
    "processingTimeMs": number
  },
  "dataProviderResponse": "string"
}
```

## Response Fields

### 1. `success` (boolean)
- `true`: Query was processed successfully
- `false`: Query failed (missing data, validation error, etc.)

### 2. `message` (string)
- Success: "Request processed successfully"
- Error: Descriptive error message (e.g., "Contract number is required for all part queries")

### 3. `nlpResponse` (object)
Contains NLP processing details:
- `originalInput`: User's original input
- `correctedInput`: Spell-corrected input
- `queryType`: Detected query type (CONTRACTS, PARTS, CUSTOMERS, etc.)
- `actionType`: Specific action to perform
- `processingTimeMs`: Processing time in milliseconds

### 4. `dataProviderResponse` (string)
- **Success**: HTML-formatted data ready for UI display
- **Error**: HTML-formatted error message with styling

## Example Responses

### Success Response (Contract Query)
```json
{
  "success": true,
  "message": "Request processed successfully",
  "nlpResponse": {
    "originalInput": "What is the effective date for contract 100476?",
    "correctedInput": "What is the effective date for contract 100476?",
    "queryType": "CONTRACTS",
    "actionType": "contracts_by_contractnumber",
    "processingTimeMs": 50.873
  },
  "dataProviderResponse": "<h3>Contract Dates</h3><hr><div style='font-family: monospace; font-size: 12px;'><div style='margin-bottom: 15px; padding: 10px; border: 1px solid #ccc; border-radius: 5px;'><h4 style='margin: 0 0 10px 0; color: #333;'>Contract 100476</h4><div style='margin-bottom: 5px;'><span style='font-weight: bold; color: #555;'>Effective Date:</span> <span style='color: #000;'>2024-01-01</span></div></div></div>"
}
```

### Error Response (Missing Contract Number)
```json
{
  "success": false,
  "message": "Contract number is required for all part queries. Please provide the contract number for faster results.",
  "nlpResponse": {
    "originalInput": "What is the lead time for EN6114V4-13?",
    "correctedInput": "What is the lead time for EN6114V4-13?",
    "queryType": "PARTS",
    "actionType": "parts_lead_time",
    "processingTimeMs": 0
  },
  "dataProviderResponse": "<div style='color: red; padding: 15px; border: 1px solid #ffc107; border-radius: 5px; background-color: #fff3cd;'><h4 style='color: #856404; margin-top: 0;'>Contract Number Required</h4><p style='color: #856404; margin-bottom: 10px;'>Contract number is required for all part queries. Please provide the contract number for faster results.</p></div>"
}
```

## UI Integration Steps

### 1. Parse JSON Response
```java
// Parse the complete response
JSONObject response = new JSONObject(jsonResponse);
boolean success = response.getBoolean("success");
String message = response.getString("message");
JSONObject nlpResponse = response.getJSONObject("nlpResponse");
String dataProviderResponse = response.getString("dataProviderResponse");
```

### 2. Handle Success/Error
```java
if (success) {
    // Display the data
    displayData(dataProviderResponse);
} else {
    // Show error message
    showError(message, dataProviderResponse);
}
```

### 3. Extract Data for Display
```java
// The dataProviderResponse contains HTML-ready content
// You can directly inject it into your UI component
webView.loadHtml(dataProviderResponse);
// or
label.setText(dataProviderResponse);
```

### 4. Create ChatMessage Object (Optional)
```java
ChatMessage chatMessage = new ChatMessage();
chatMessage.setUserInput(nlpResponse.getString("originalInput"));
chatMessage.setBotResponse(dataProviderResponse);
chatMessage.setSuccess(success);
chatMessage.setMessage(message);
chatMessage.setQueryType(nlpResponse.getString("queryType"));
```

## Method to Call

Use the `processUserInputJSONResponse()` method:

```java
NLPUserActionHandler handler = new NLPUserActionHandler();
String response = handler.processUserInputJSONResponse("What is the lead time for EN6114V4-13?");
```

## Business Rules

### Contract Number Requirements
- **Parts queries**: Contract number is **mandatory**
- **Contract queries**: Contract number is **optional** (can use filters)
- **Customer queries**: No contract number required

### Error Handling
- Missing contract number → Returns error response with helpful message
- Invalid part number → Returns error response
- Database errors → Returns error response with technical details

### Success Scenarios
- Valid contract query → Returns contract data
- Valid parts query with contract → Returns part data
- Valid customer query → Returns customer data

## Testing

Run the test files to verify the system:
- `CompleteResponseTest.java` - Basic response format
- `SystemIntegrationTest.java` - Complete system scenarios
- `LeadTimeTest.java` - Lead time specific testing

## Benefits

1. **Consistent Format**: All responses follow the same structure
2. **UI Ready**: HTML-formatted content for immediate display
3. **Error Handling**: Clear success/failure indicators
4. **Debugging**: NLP processing details included
5. **Extensible**: Easy to add new fields or modify format 