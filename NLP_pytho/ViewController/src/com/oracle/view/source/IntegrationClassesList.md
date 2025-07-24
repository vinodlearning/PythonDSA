# NLP Integration - Java Classes List

## Core Integration Classes

### 1. EnhancedUserHandler.java
**Purpose**: Main integration class providing three different response methods
**Location**: `com/oracle/view/source/EnhancedUserHandler.java`
**Key Methods**:
- `processUserInputCompleteResponse(String userInput)` - Returns complete JSON string
- `processUserInputJSONResponse(String userInput)` - Returns structured JSON
- `processUserInputCompleteObject(String userInput)` - Returns Java object with SQL

### 2. IntegrationTest.java
**Purpose**: Test class demonstrating all three integration methods
**Location**: `com/oracle/view/source/IntegrationTest.java`
**Usage**: Run to test all integration methods with sample queries

## Existing Core Classes (Already Available)

### 3. StandardJSONProcessor.java
**Purpose**: Main NLP processing engine
**Location**: `com/oracle/view/source/StandardJSONProcessor.java`
**Key Methods**:
- `processQuery(String userInput)` - Main processing method
- `parseJSONToObject(String jsonResponse)` - Parse JSON to QueryResult
- `determineQueryType(String userInput)` - Determine query type
- `determineActionType(String userInput, String queryType)` - Determine action type

### 4. ActionTypeDataProvider.java
**Purpose**: Data provider for different action types
**Location**: `com/oracle/view/source/ActionTypeDataProvider.java`
**Key Methods**:
- `executeAction(String actionType, List entities, List displayEntities, String userInput)` - Execute data provider actions
- `getActionTypes()` - Get available action types
- `getDisplayEntities(String actionType)` - Get display entities for action type

### 5. UserActionHandler.java
**Purpose**: Original user action handler (legacy)
**Location**: `com/oracle/view/source/UserActionHandler.java`
**Key Methods**:
- `processUserInput(String userInput)` - Process user input and return UserActionResponse
- Various handler methods for different action types

### 6. UserActionResponse.java
**Purpose**: Response object for user actions
**Location**: `com/oracle/view/source/UserActionResponse.java`
**Key Methods**:
- `generateSQLQuery()` - Generate SQL query
- `addParameter(String key, Object value)` - Add parameters
- `getParameter(String key)` - Get parameter value

## Inner Classes (Defined within EnhancedUserHandler)

### 7. CompleteResponse (Inner Class)
**Purpose**: Complete response including NLP and DataProvider results
**Location**: `com/oracle/view/source/EnhancedUserHandler.java` (inner class)
**Properties**:
- `success` - boolean
- `message` - String
- `errorCode` - String
- `errorDetails` - String
- `nlpResponse` - QueryResult
- `dataProviderResponse` - String

### 8. StructuredJSONResponse (Inner Class)
**Purpose**: Structured JSON response with specified format
**Location**: `com/oracle/view/source/EnhancedUserHandler.java` (inner class)
**Properties**:
- `header` - Header object
- `queryMetadata` - QueryMetadata object
- `entities` - List<EntityFilter>
- `displayEntities` - List<String>
- `errors` - List<ValidationError>

### 9. CompleteQueryObject (Inner Class)
**Purpose**: Java object with complete information including SQL
**Location**: `com/oracle/view/source/EnhancedUserHandler.java` (inner class)
**Properties**:
- `success` - boolean
- `message` - String
- `errorCode` - String
- `errorDetails` - String
- `queryResult` - QueryResult
- `sqlQuery` - String
- `parameters` - Map<String, Object>
- `dataProviderResult` - String

## Supporting Classes (Already Available)

### 10. EntityFilter.java
**Purpose**: Filter entity for database queries
**Location**: `com/oracle/view/source/EntityFilter.java`
**Properties**:
- `attribute` - String
- `operation` - String
- `value` - String

### 11. QueryResult.java
**Purpose**: Result of NLP processing
**Location**: `com/oracle/view/source/StandardJSONProcessor.java` (inner class)
**Properties**:
- `inputTracking` - InputTracking object
- `metadata` - Metadata object
- `entities` - List<EntityFilter>
- `displayEntities` - List<String>
- `errors` - List<String>

### 12. InputTracking.java
**Purpose**: Track input processing
**Location**: `com/oracle/view/source/StandardJSONProcessor.java` (inner class)
**Properties**:
- `originalInput` - String
- `correctedInput` - String

### 13. Metadata.java
**Purpose**: Query metadata
**Location**: `com/oracle/view/source/StandardJSONProcessor.java` (inner class)
**Properties**:
- `queryType` - String
- `actionType` - String
- `processingTimeMs` - double

## Integration Dependencies

### Required External Libraries
1. **Gson** - For JSON processing
   - Version: 2.8.9 or higher
   - Purpose: JSON serialization/deserialization

### Java Version Requirements
- **Java 8** - All classes are compatible with Java 8
- **No newer Java features** - Uses only Java 8 compatible syntax

## Integration Steps

### Step 1: Compile All Classes
```bash
javac -cp ".:gson-2.8.9.jar" com/oracle/view/source/*.java
```

### Step 2: Run Integration Test
```bash
java -cp ".:gson-2.8.9.jar" com.oracle.view.source.IntegrationTest
```

### Step 3: Use in Your Application
```java
// Import the enhanced handler
import com.oracle.view.source.EnhancedUserHandler;

// Create handler instance
EnhancedUserHandler handler = new EnhancedUserHandler();

// Method 1: Complete response as string
String completeResponse = handler.processUserInputCompleteResponse("Show contract 123456");

// Method 2: Structured JSON response
String jsonResponse = handler.processUserInputJSONResponse("Show contract 123456");

// Method 3: Java object with complete information
CompleteQueryObject completeObject = handler.processUserInputCompleteObject("Show contract 123456");
```

## Class Relationships

```
EnhancedUserHandler
├── StandardJSONProcessor (uses)
├── ActionTypeDataProvider (uses)
├── CompleteResponse (creates)
├── StructuredJSONResponse (creates)
└── CompleteQueryObject (creates)

StandardJSONProcessor
├── QueryResult (creates)
├── InputTracking (creates)
├── Metadata (creates)
└── EntityFilter (creates)

ActionTypeDataProvider
└── EntityFilter (uses)

UserActionHandler (legacy)
├── StandardJSONProcessor (uses)
├── ActionTypeDataProvider (uses)
└── UserActionResponse (creates)
```

## File Structure
```
com/oracle/view/source/
├── EnhancedUserHandler.java          (NEW - Main integration class)
├── IntegrationTest.java              (NEW - Test class)
├── StandardJSONProcessor.java        (EXISTING - NLP engine)
├── ActionTypeDataProvider.java       (EXISTING - Data provider)
├── UserActionHandler.java            (EXISTING - Legacy handler)
├── UserActionResponse.java           (EXISTING - Response object)
└── EntityFilter.java                 (EXISTING - Filter entity)
```

## Notes
- All classes are designed for Java 8 compatibility
- No internet connectivity required (offline operation)
- Uses built-in Java files and local resources
- Comprehensive error handling included
- Multiple response formats for different use cases 