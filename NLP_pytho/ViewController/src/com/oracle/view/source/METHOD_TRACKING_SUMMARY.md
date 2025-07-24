# Method Tracking Summary

This document lists all the methods where System.out.println tracking has been added for debugging and parameter tracking purposes.

## ChatMessage.java

### Methods with Tracking Added:
1. **hasCompleteInformation()**
   - Tracks: message parameter
   - Outputs: Method entry, parameters, and result

2. **extractEntities()**
   - Tracks: message parameter
   - Outputs: Method entry, parameters, extraction process, and final context values

## NLPUserActionHandler.java

### Methods with Tracking Added:
1. **processUserInputJSONResponse(String userInput)**
   - Tracks: userInput parameter
   - Outputs: Method entry, parameters, and result

2. **processUserInputJSONResponse(String userInput, String sessionId)**
   - Tracks: userInput and sessionId parameters
   - Outputs: Method entry, parameters, and error messages

3. **needsConversationalFlow(String userInput)**
   - Tracks: userInput parameter
   - Outputs: Method entry, parameters, and result with reasoning

4. **getQueryResultFromNLP(String userInput)**
   - Tracks: userInput parameter
   - Outputs: Method entry, parameters, and result

5. **handleFollowUpResponse(String userInput, String sessionId)**
   - Tracks: userInput and sessionId parameters
   - Outputs: Method entry, parameters, and error messages

6. **buildMergedLeadTimeQuery(String partNumber, String contractNumber)**
   - Tracks: partNumber and contractNumber parameters
   - Outputs: Method entry, parameters, and result

7. **executeDataProviderAction(NLPEntityProcessor.QueryResult queryResult)**
   - Tracks: actionType and entities count
   - Outputs: Method entry, parameters, and result

8. **processCompleteQueryDirectly(ChatMessage chatMessage)**
   - Tracks: message and queryType
   - Outputs: Method entry, parameters, and error messages

9. **handleLeadTimeQuery(ChatMessage chatMessage)**
   - Tracks: partNumber and contractNumber from context
   - Outputs: Method entry, parameters, and error messages

10. **pullData(Map<String, Object> inputParams)**
    - Tracks: actionType and filters
    - Outputs: Method entry and parameters

## ConversationalFlowManager.java

### Methods with Tracking Added:
1. **processUserInput(String userInput, String sessionId)**
   - Tracks: userInput and sessionId parameters
   - Outputs: Method entry and parameters

2. **processCompleteQuery(ChatMessage chatMessage)**
   - Tracks: message and queryType
   - Outputs: Method entry and parameters

3. **requiresFollowUpInformation(ChatMessage chatMessage)**
   - Tracks: message parameter
   - Outputs: Method entry and parameters

4. **createFollowUpRequest(ChatMessage chatMessage)**
   - Tracks: message and partNumber
   - Outputs: Method entry and parameters

5. **processFollowUpResponse(ChatMessage chatMessage)**
   - Tracks: message and sessionId
   - Outputs: Method entry, parameters, and result

## NLPEntityProcessor.java

### Methods with Tracking Added:
1. **processQuery(String originalInput)**
   - Tracks: originalInput parameter
   - Outputs: Method entry, parameters, and error messages

## StandardJSONProcessor.java

### Methods with Tracking Added:
1. **processQuery(String originalInput)**
   - Tracks: originalInput parameter
   - Outputs: Method entry, parameters, and error messages

## ActionTypeDataProvider.java

### Methods with Tracking Added:
1. **executeAction(String actionType, List<StandardJSONProcessor.EntityFilter> filters, List<String> displayEntities, String userInput)**
   - Tracks: actionType, filters count, displayEntities count, and userInput
   - Outputs: Method entry, parameters, and error messages

## Tracking Format

All tracking follows this consistent format:

```java
System.out.println("=== METHOD: methodName(parameters) ===");
System.out.println("Parameters: param1=value1, param2=value2");
// ... method logic ...
System.out.println("RESULT: result description");
// or
System.out.println("ERROR: error message");
```

## Excluded Methods

The following types of methods were excluded from tracking:
- Getters and setters
- HTML code generators
- Simple utility methods
- Private helper methods that don't contain business logic

## Benefits

This tracking system provides:
1. **Method Entry Tracking**: Know exactly which methods are being called
2. **Parameter Validation**: Verify that correct parameters are being passed
3. **Flow Analysis**: Understand the execution path through the application
4. **Error Identification**: Quickly identify where errors occur
5. **Performance Monitoring**: Track method execution patterns
6. **Debugging Support**: Easier troubleshooting of issues

## Usage

When running the application, you will see output like:
```
=== METHOD: processUserInputJSONResponse(String userInput, String sessionId) ===
Parameters: userInput=what is the lead time for part AIR-A320-001, sessionId=12345
=== METHOD: needsConversationalFlow(String userInput) ===
Parameters: userInput=what is the lead time for part AIR-A320-001
RESULT: true (isPartQuery=true, hasContractNumber=false)
```

This makes it easy to trace the execution flow and identify any issues in the processing pipeline. 