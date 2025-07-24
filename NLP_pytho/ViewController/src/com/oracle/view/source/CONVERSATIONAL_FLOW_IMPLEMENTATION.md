# Conversational Flow Implementation

## Overview

This document describes the implementation of conversational flow management for handling missing information in user queries, specifically for parts queries that require contract numbers.

## Problem Statement

When users ask about parts without specifying a contract number, the system needs to:
1. Detect the missing contract number
2. Ask the user for the contract number
3. Process the user's response
4. Reconstruct the original query with the contract number
5. Execute the query with proper filters

## Solution Implementation

### 1. ConversationalFlowManager

The `ConversationalFlowManager` class manages multi-turn conversations:

```java
public class ConversationalFlowManager {
    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();
    
    // Conversation types
    public static final String CONVERSATION_TYPE_PARTS_CONTRACT = "PARTS_CONTRACT_REQUEST";
    public static final String CONVERSATION_TYPE_CUSTOMER_ACCOUNT = "CUSTOMER_ACCOUNT_REQUEST";
    public static final String CONVERSATION_TYPE_CONTRACT_CREATION = "CONTRACT_CREATION_REQUEST";
}
```

### 2. Conversation State Management

Each conversation is tracked with a unique session ID:

```java
public static class ConversationState {
    public String conversationType;
    public String originalQuery;
    public String partNumber;
    public String sessionId;
    public long createdAt;
}
```

### 3. Flow Detection

The system detects follow-up responses:

```java
public boolean isFollowUpResponse(String userInput, String sessionId) {
    ConversationState state = conversationStates.get(sessionId);
    if (state == null) return false;
    
    if (state.conversationType.equals(CONVERSATION_TYPE_PARTS_CONTRACT)) {
        return isContractNumberResponse(userInput);
    }
    
    return false;
}
```

### 4. Contract Number Response Detection

Multiple patterns are supported:

```java
private boolean isContractNumberResponse(String userInput) {
    // Pattern 1: Just numbers (6+ digits)
    if (trimmed.matches("\\d{6,}")) return true;
    
    // Pattern 2: "contract" + number
    if (trimmed.toLowerCase().matches(".*contract\\s+\\d{6,}.*")) return true;
    
    // Pattern 3: Any 4-8 digit number (fallback)
    if (trimmed.matches("\\d{4,8}")) return true;
    
    return false;
}
```

## Usage Flow

### Step 1: User Query Without Contract Number

**User Input:** `"What is the lead time for part EN6114V4-13?"`

**System Response:**
```html
<div style='padding: 15px; border: 1px solid #ffc107; border-radius: 5px; background-color: #fff3cd;'>
    <h4 style='color: #856404; margin-top: 0;'>Contract Number Required</h4>
    <p style='color: #856404; margin-bottom: 10px;'>Can you specify the contract number because parts number (invoice part number) is loaded w.r.t Contract.</p>
    <div style='background-color: #f8f9fa; padding: 10px; border-radius: 3px;'>
        <strong>For part EN6114V4-13, please provide:</strong><br>
          Contract number (e.g., 100476, 123456)<br>
          Or say "contract 100476"
    </div>
</div>
```

### Step 2: User Provides Contract Number

**User Input:** `"100476"`

**System Processing:**
1. Detects this is a follow-up response
2. Extracts contract number: `"100476"`
3. Reconstructs original query: `"What is the lead time for part EN6114V4-13? in contract 100476"`
4. Processes the reconstructed query
5. Returns parts data filtered by both part number and contract number

### Step 3: Database Query


```

## Supported Response Formats

### Valid Contract Number Responses:
- `"100476"` - Just the number
- `"contract 100476"` - With "contract" keyword
- `"Contract 100476"` - Case insensitive
- `"CONTRACT 100476"` - All caps
- `"123456"` - Any 6+ digit number
- `"567890"` - Any 4-8 digit number (fallback)

### Invalid Responses:
- `"I don't know"` - Not a number
- `"What do you mean?"` - Not a number
- `"123"` - Too short
- `"abcdef"` - Not numeric
- `""` - Empty
- `null` - Null value

## Integration with NLPUserActionHandler

### 1. Session-Aware Processing

```java
public String processUserInputJSONResponse(String userInput, String sessionId) {
    // Clean up old conversation states
    flowManager.cleanupOldStates();
    
    // Check if this is a follow-up response
    if (sessionId != null && flowManager.isFollowUpResponse(userInput, sessionId)) {
        return handleFollowUpResponse(userInput, sessionId);
    }
    
    // Normal processing...
}
```

### 2. Enhanced Parts Query Handler

```java
private String handlePartsQuery(List<NLPEntityProcessor.EntityFilter> filters, 
                               List<String> displayEntities, 
                               String userInput,
                               String sessionId) {
    // Check if parts query has contract number
    if (!hasContractNumberInPartsQuery(userInput)) {
        // Extract part number for the request
        String partNumber = extractPartNumberFromInput(userInput, filters);
        
        // If we have a session ID, create a conversational flow
        if (sessionId != null) {
            return flowManager.createPartsContractRequest(userInput, partNumber, sessionId);
        } else {
            // Fallback to static message if no session
            return generateStaticContractRequestMessage();
        }
    }
    
    // Continue with normal processing...
}
```

### 3. Follow-Up Response Handler

```java
private String handleFollowUpResponse(String userInput, String sessionId) {
    // Process the contract number response
    String reconstructedQuery = flowManager.processContractNumberResponse(userInput, sessionId);
    
    if (reconstructedQuery == null) {
        return createErrorStructuredJSON(userInput, "Failed to process contract number response");
    }
    
    // Process the reconstructed query
    String preprocessedInput = preprocessInput(reconstructedQuery);
    Map<String, Object> contractsValidation = processWithContractsModel(preprocessedInput);
    NLPEntityProcessor.QueryResult queryResult = getQueryResultFromNLP(preprocessedInput);
    
    // Return structured JSON response
    return createStructuredJSONResponse(queryResult, contractsValidation);
}
```

## Session Management

### 1. Session Lifecycle

1. **Creation**: Session ID generated when conversation starts
2. **Storage**: Conversation state stored with session ID
3. **Processing**: Follow-up responses processed using session ID
4. **Cleanup**: Old sessions automatically cleaned up after 5 minutes
5. **Manual Clear**: Sessions can be manually cleared

### 2. Automatic Cleanup

```java
public void cleanupOldStates() {
    long currentTime = System.currentTimeMillis();
    long fiveMinutes = 5 * 60 * 1000; // 5 minutes in milliseconds
    
    conversationStates.entrySet().removeIf(entry -> 
        (currentTime - entry.getValue().createdAt) > fiveMinutes
    );
}
```

## Example Conversations

### Example 1: Basic Flow

```
User: "What is the lead time for part EN6114V4-13?"
Bot: "Contract Number Required. Can you specify the contract number because parts number (invoice part number) is loaded w.r.t Contract. For part EN6114V4-13, please provide:   Contract number (e.g., 100476, 123456)   Or say 'contract 100476'"
User: "100476"
Bot: [Returns lead time data for part EN6114V4-13 in contract 100476]
```

### Example 2: Different Response Format

```
User: "Show me part details for EN6114V4-13"
Bot: "Contract Number Required. Can you specify the contract number because parts number (invoice part number) is loaded w.r.t Contract. For part EN6114V4-13, please provide:   Contract number (e.g., 100476, 123456)   Or say 'contract 100476'"
User: "contract 123456"
Bot: [Returns part details for EN6114V4-13 in contract 123456]
```

### Example 3: Invalid Response

```
User: "What's the price for part EN6114V4-13"
Bot: "Contract Number Required. Can you specify the contract number because parts number (invoice part number) is loaded w.r.t Contract. For part EN6114V4-13, please provide:   Contract number (e.g., 100476, 123456)   Or say 'contract 100476'"
User: "I don't know"
Bot: "I didn't understand the contract number. Please provide a valid contract number (e.g., 100476, 123456)."
```

## Benefits

1. **User-Friendly**: Natural conversation flow
2. **Robust**: Handles multiple response formats
3. **Session-Managed**: Maintains conversation context
4. **Automatic Cleanup**: Prevents memory leaks
5. **Error Handling**: Graceful handling of invalid responses
6. **Extensible**: Easy to add new conversation types

## Future Enhancements

1. **Multiple Missing Fields**: Handle multiple missing pieces of information
2. **Context Memory**: Remember previous queries in the same session
3. **Smart Suggestions**: Suggest contract numbers based on user history
4. **Voice Integration**: Support for voice-based responses
5. **Multi-language**: Support for different languages
6. **Advanced Validation**: More sophisticated response validation

## Testing

The `ConversationalFlowTest` class provides comprehensive testing:

```java
// Test basic flow
testConversationalFlow(flowManager);

// Test different contract formats
testDifferentContractFormats(flowManager);

// Test invalid responses
testInvalidResponses(flowManager);

// Test session management
testSessionManagement(flowManager);
```

## Integration Points

### UI Integration

The UI should:
1. Generate and maintain session IDs
2. Pass session IDs to `processUserInputJSONResponse(userInput, sessionId)`
3. Handle the conversational responses appropriately
4. Clear sessions when user starts a new conversation

### Database Integration

The system automatically:
1. Filters parts by both part number and contract number
2. Uses the correct table (`XXCCT.CCT_AWARDS_FINAL_PARTS`)
3. Applies proper WHERE clauses
4. Returns formatted results

## Conclusion

The conversational flow implementation provides a natural, user-friendly way to handle missing contract numbers in parts queries. The system maintains conversation context, handles multiple response formats, and automatically processes the reconstructed queries to provide accurate results. 