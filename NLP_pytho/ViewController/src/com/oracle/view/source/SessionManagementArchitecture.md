# Session Management Architecture

## Overview

The new **Conversation Session Management** architecture provides a comprehensive solution for handling multi-turn conversations in the Oracle ADF chat application. It addresses the core problem of maintaining conversation context across multiple user interactions.

## Problem Statement

### Original Issues:
1. **No Session Tracking**: Each user input was treated as a standalone request
2. **No Context Preservation**: Previous conversation state was lost between turns
3. **No Follow-up Detection**: System couldn't distinguish between new requests and follow-up responses
4. **No State Management**: No way to track what information was missing and what was expected from the user

### Example Problem Scenario:
```
User: "What is the lead time for part AIR-A320-001?"
Bot: "Please provide contract number"
User: "100476"
Bot: ❌ Treats "100476" as new contract query instead of follow-up
```

## Solution Architecture

### 1. Core Components

#### ConversationSessionManager
- **Purpose**: Central session management and conversation state tracking
- **Features**:
  - User session creation and management
  - Conversation state tracking
  - Follow-up detection and validation
  - Automatic cleanup and timeout handling

#### ConversationSessionIntegration
- **Purpose**: Integration layer between session manager and existing NLP system
- **Features**:
  - Seamless integration with existing NLPUserActionHandler
  - Conversation type detection and routing
  - Query merging and processing
  - JSON response formatting

### 2. Key Concepts

#### User Session
```java
public class UserSession {
    private String sessionId;
    private String userId;
    private Instant createdAt;
    private Instant lastActivity;
    private List<ChatMessage> messageHistory;
    private ConversationState currentConversation;
    private Map<String, Object> sessionData;
}
```

#### Conversation State
```java
public class ConversationState {
    private String conversationId;
    private String conversationType;
    private String status; // "waiting_for_input", "in_progress", "complete"
    private String expectedResponseType;
    private String originalQuery;
    private Map<String, String> collectedData;
    private int turnCount;
    private Map<String, Object> context;
}
```

### 3. Conversation Types

| Type | Description | Expected Responses |
|------|-------------|-------------------|
| `PARTS_CONTRACT_REQUEST` | Parts queries missing contract number | Contract number |
| `CUSTOMER_ACCOUNT_REQUEST` | Customer queries missing account | Customer account/name |
| `CONTRACT_CREATION_REQUEST` | Contract creation workflow | Name, duration, etc. |
| `PART_SEARCH_REQUEST` | Part search queries | Part number |
| `LEAD_TIME_REQUEST` | Lead time queries | Part + contract |

### 4. Expected Response Types

| Type | Description | Validation Pattern |
|------|-------------|-------------------|
| `CONTRACT_NUMBER` | 6+ digit contract numbers | `\d{6,}` |
| `PART_NUMBER` | Part identifiers | `[A-Za-z0-9-]+` |
| `CUSTOMER_ACCOUNT` | Account numbers or names | `\d{4,8}` or text |
| `CONTRACT_NAME` | Contract names | Text with letters |
| `CONTRACT_DURATION` | Time periods | `\d+\s*(month|year|day)` |
| `CONFIRMATION` | Yes/no responses | `yes|no|ok|cancel` |

## Flow Architecture

### 1. New Request Flow
```
User Input → Session Manager → Analyze Type → Determine Missing Info → 
If Missing Info → Create Conversation State → Return Follow-up Prompt
If Complete → Process Directly → Return Results
```

### 2. Follow-up Response Flow
```
User Input → Session Manager → Check Expected Response → Validate Input →
If Valid → Add to Collected Data → Check if Complete → 
If Complete → Merge Query → Process → Return Results
If Incomplete → Determine Next Expected → Return Follow-up Prompt
```

### 3. Conversation State Transitions
```
waiting_for_input → in_progress → complete
waiting_for_input → cancelled (if user starts new request)
waiting_for_input → timeout (after 10 minutes)
```

## Implementation Examples

### Example 1: Parts Lead Time Conversation

#### Step 1: Initial Query
```java
// User: "What is the lead time for part AIR-A320-001?"
ConversationResult result = sessionManager.processUserInput(userInput, sessionId);
// Result: requiresFollowUp = true, expectedResponseType = "CONTRACT_NUMBER"
```

#### Step 2: Follow-up Response
```java
// User: "100476"
ConversationResult result = sessionManager.processUserInput(userInput, sessionId);
// Result: isComplete = true, conversation has all required data
```

#### Step 3: Query Processing
```java
// Merged query: "What is the lead time for part AIR-A320-001 contract 100476"
String mergedQuery = originalQuery + " contract " + contractNumber;
return nlpHandler.processUserInputJSONResponse(mergedQuery, sessionId);
```

### Example 2: Contract Creation Conversation

#### Step 1: Initial Query
```java
// User: "Create contract"
// Expected: CONTRACT_NAME
```

#### Step 2: Name Response
```java
// User: "ABC Aerospace Contract"
// Expected: CONTRACT_DURATION
```

#### Step 3: Duration Response
```java
// User: "12 months"
// Complete: All required information collected
```

## Integration with Existing System

### 1. Backward Compatibility
- Existing single-turn queries continue to work unchanged
- No modification required to existing NLPUserActionHandler
- Gradual migration path available

### 2. Enhanced Functionality
- Multi-turn conversations for complex queries
- Context preservation across interactions
- Intelligent follow-up detection
- Automatic conversation state management

### 3. Error Handling
- Graceful degradation if session management fails
- Fallback to original processing
- Comprehensive error logging and debugging

## Configuration and Customization

### 1. Timeout Settings
```java
private static final long SESSION_TIMEOUT_MS = 300000; // 5 minutes
private static final long CONVERSATION_TIMEOUT_MS = 600000; // 10 minutes
```

### 2. Conversation Types
- Easy to add new conversation types
- Configurable expected response types
- Custom validation patterns

### 3. Session Storage
- In-memory storage with automatic cleanup
- Thread-safe concurrent access
- Extensible for database persistence

## Benefits

### 1. User Experience
- **Natural Conversations**: Users can provide information incrementally
- **Context Awareness**: Bot remembers previous interactions
- **Intelligent Prompts**: Clear guidance on what information is needed
- **Error Recovery**: Graceful handling of unexpected inputs

### 2. System Reliability
- **State Management**: Robust conversation state tracking
- **Timeout Handling**: Automatic cleanup of stale sessions
- **Error Isolation**: Failures don't affect other sessions
- **Scalability**: Efficient memory usage and cleanup

### 3. Development Benefits
- **Modular Design**: Clean separation of concerns
- **Extensible Architecture**: Easy to add new conversation types
- **Comprehensive Testing**: Full test coverage for all scenarios
- **Debugging Support**: Rich logging and session inspection

## Testing Strategy

### 1. Unit Tests
- Individual component testing
- Conversation state validation
- Response type detection
- Error handling scenarios

### 2. Integration Tests
- End-to-end conversation flows
- Session management integration
- NLP system integration
- Performance testing

### 3. User Acceptance Tests
- Real-world conversation scenarios
- Edge case handling
- Error recovery testing
- Performance under load

## Migration Guide

### Phase 1: Implementation
1. Deploy ConversationSessionManager
2. Deploy ConversationSessionIntegration
3. Update BCCTContractManagementNLPBean to use new integration

### Phase 2: Testing
1. Run comprehensive test suite
2. Validate all conversation scenarios
3. Performance testing and optimization

### Phase 3: Rollout
1. Gradual rollout to users
2. Monitor session management performance
3. Collect user feedback and iterate

## Future Enhancements

### 1. Advanced Features
- **Multi-step Workflows**: Complex business processes
- **Context Switching**: Handle multiple concurrent conversations
- **Learning**: Adapt to user preferences and patterns
- **Integration**: Connect with external systems

### 2. Performance Optimizations
- **Database Persistence**: For high-availability deployments
- **Caching**: Intelligent caching of conversation patterns
- **Load Balancing**: Distributed session management
- **Monitoring**: Advanced metrics and alerting

### 3. User Experience
- **Proactive Suggestions**: Suggest next steps based on context
- **Rich Responses**: Multimedia and interactive elements
- **Personalization**: User-specific conversation patterns
- **Accessibility**: Support for various input methods

## Conclusion

The Session Management Architecture provides a robust, scalable solution for multi-turn conversations in the Oracle ADF chat application. It maintains backward compatibility while significantly enhancing the user experience through intelligent conversation management.

The architecture is designed to be:
- **Maintainable**: Clean, well-documented code
- **Extensible**: Easy to add new features
- **Reliable**: Comprehensive error handling
- **Performant**: Efficient resource usage
- **User-friendly**: Natural conversation flows

This solution addresses the core problem of maintaining conversation context while providing a foundation for future enhancements and integrations. 