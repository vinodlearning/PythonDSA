# NLP Operations Separation Design

## Overview

The NLP system is designed with a **modular, processor-based architecture** that separates different types of natural language queries into specialized processors. This design follows the **Single Responsibility Principle** and enables **extensibility**, **maintainability**, and **high accuracy** for each domain.

## Architecture Design

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                 NLPQueryClassifier                      │
│                    (Main Router)                        │
├─────────────────────────────────────────────────────────┤
│  • Query Type Detection                                 │
│  • Spell Correction                                     │
│  • Input Preprocessing                                  │
│  • Processor Routing                                    │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
    ┌─────────────────┬─────────────────┬─────────────────┐
    │                 │                 │                 │
    ▼                 ▼                 ▼                 ▼
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│Contract │    │ Parts   │    │Failed   │    │ Help    │
│Processor│    │Processor│    │Parts    │    │Processor│
│         │    │         │    │Processor│    │         │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
    │                 │                 │                 │
    ▼                 ▼                 ▼                 ▼
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│Contract │    │ Parts   │    │Failed   │    │ Help    │
│Queries  │    │Queries  │    │Parts    │    │Queries  │
│         │    │         │    │Queries  │    │         │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
```

## Core Components

### 1. NLPQueryClassifier (Main Router)

**Purpose**: Central coordinator that routes queries to appropriate specialized processors.

**Responsibilities**:
- Query type detection and classification
- Spell correction and input preprocessing
- Processor routing based on query content
- Result aggregation and metadata management

**Key Methods**:
```java
public QueryResult processQuery(String userInput)
private String determineQueryType(String userInput)
private String preprocessInput(String userInput)
```

**Dependencies**:
```java
private final FailedPartsProcessor failedPartsProcessor;
private final PartsProcessor partsProcessor;
private final ContractProcessor contractProcessor;
private final HelpProcessor helpProcessor;
private final SpellCorrector spellCorrector;
private final Lemmatizer lemmatizer;
```

### 2. ContractProcessor

**Purpose**: Handles all contract-related queries including contract lookup, customer information, status, and "created by" queries.

**Supported Query Types**:
- Contract lookup by number (`show100476`)
- Contract lookup by customer (`contracts for customer 123456`)
- "Created by" queries (`contracts created by vinod`)
- Contract status queries (`contract 123456 status`)
- Contract date queries (`contracts created in 2025`)

**Key Features**:
- Enhanced contract number extraction (handles concatenated text like "show100476")
- "Created by" pattern detection with user search
- Customer name and number extraction
- Date filtering support
- Multiple action types based on query content

**Action Types**:
- `contracts_by_contractnumber` - Direct contract lookup
- `contracts_by_user` - "Created by" queries
- `contracts_by_filter` - General contract filtering

**Entity Extraction**:
- Contract numbers (6-digit patterns)
- Customer numbers (4-8 digit patterns)
- Customer names
- Creator names ("created by" pattern)
- Date filters

### 3. PartsProcessor

**Purpose**: Handles all parts-related queries including pricing, lead times, MOQ, and UOM information.

**Supported Query Types**:
- Parts by contract number (`parts for contract 123456`)
- Parts by part number (`part AB12345`)
- Parts pricing queries (`price for part AB12345`)
- Lead time queries (`lead time for part AB12345`)
- MOQ queries (`minimum order quantity for part AB12345`)

**Key Features**:
- Part number pattern recognition (2 letters + 4-6 digits)
- Contract number association
- Dynamic display entity selection based on query content
- Price, lead time, and quantity field detection

**Action Types**:
- `parts_by_contract_number` - Parts for specific contract
- `parts_by_part_number` - Specific part lookup

**Entity Extraction**:
- Part numbers (AB12345 pattern)
- Contract numbers
- Price-related keywords
- Lead time keywords
- MOQ/UOM keywords

### 4. FailedPartsProcessor

**Purpose**: Handles all failed parts, error, and validation-related queries.

**Supported Query Types**:
- Failed parts by contract (`failed parts for contract 123456`)
- Failed parts by part number (`failed part AB12345`)
- Validation errors (`validation errors for contract 123456`)
- Business rule violations (`business rule violations`)
- Processing errors (`processing errors`)
- Loading errors (`loading errors`)

**Key Features**:
- Error type classification (validation, business rules, processing, loading)
- Specific error pattern detection
- Enhanced error reporting
- Multiple error categories

**Action Types**:
- `parts_failed_by_contract_number`
- `failed_parts_by_part_number`
- `validation_errors_by_contract_number`
- `validation_errors_by_part_number`
- `business_rule_violations_by_contract_number`
- `business_rule_violations_by_part_number`
- `processing_errors_by_contract_number`
- `processing_errors_by_part_number`
- `loading_errors_by_contract_number`
- `loading_errors_by_part_number`

**Error Categories**:
- **Validation Errors**: Data validation issues
- **Business Rule Violations**: Rule compliance failures
- **Processing Errors**: System processing issues
- **Loading Errors**: Data loading problems

### 5. HelpProcessor

**Purpose**: Handles help queries, contract creation guidance, and user assistance.

**Supported Query Types**:
- Contract creation help (`how to create contract`)
- Contract creation bot requests (`create contract for me`)
- General help queries (`help with contracts`)
- Step-by-step guidance (`steps to create contract`)

**Key Features**:
- Contract creation flow management
- Bot vs. user assistance detection
- Structured guidance provision
- Comma-separated format instructions

**Action Types**:
- `HELP_CONTRACT_CREATE_BOT` - Automated contract creation
- `HELP_CONTRACT_CREATE_USER` - Manual guidance

**Guidance Types**:
- **Bot Assistance**: Automated contract creation with structured input
- **User Guidance**: Manual step-by-step instructions
- **Format Instructions**: Comma-separated input requirements

## Query Processing Flow

### 1. Input Reception
```
User Input → NLPQueryClassifier.processQuery()
```

### 2. Preprocessing
```
Input → Spell Correction → Lemmatization → Normalized Input
```

### 3. Query Type Detection
```
Normalized Input → determineQueryType() → Query Type Classification
```

**Detection Priority**:
1. **CONTRACTS** - Highest priority for "created by" queries
2. **HELP** - Contract creation and assistance queries
3. **FAILED_PARTS** - Error and validation queries
4. **PARTS** - Parts and pricing queries
5. **CONTRACTS** - Default fallback

### 4. Processor Routing
```
Query Type → Specialized Processor → Detailed Processing
```

### 5. Result Generation
```
Processor Result → QueryResult → Response Generation
```

## Query Type Detection Logic

### Contract Detection (Highest Priority)
```java
private boolean isContractQuery(String input) {
    String[] contractKeywords = {
        "contract", "agreement", "customer", "effective date", "expiration",
        "payment terms", "incoterms", "status", "active", "expired"
    };
    
    // Special check for "created by" pattern
    if (input.contains("created") && input.contains("by")) {
        return true;
    }
    
    for (String keyword : contractKeywords) {
        if (input.contains(keyword)) {
            return true;
        }
    }
    return false;
}
```

### Help Detection
```java
private boolean isHelpQuery(String input) {
    String[] helpKeywords = {
        "help", "how to", "steps", "guide", "instruction", "walk me",
        "explain", "process", "show me how", "need guidance", "teach",
        "assist", "support", "create", "make", "generate", "initiate"
    };
    
    for (String keyword : helpKeywords) {
        if (input.contains(keyword)) {
            return true;
        }
    }
    return false;
}
```

### Failed Parts Detection
```java
private boolean isFailedPartsQuery(String input) {
    String[] failedKeywords = {
        "failed parts", "failed part", "parts failed", "part failed",
        "error parts", "error part", "parts error", "part error",
        "failed", "errors", "failures", "problems", "issues"
    };
    
    for (String keyword : failedKeywords) {
        if (input.contains(keyword)) {
            return true;
        }
    }
    return false;
}
```

### Parts Detection
```java
private boolean isPartsQuery(String input) {
    String[] partsKeywords = {
        "part", "parts", "lead time", "price", "cost", "moq", "uom",
        "unit of measure", "minimum order", "leadtime", "pricing"
    };
    
    for (String keyword : partsKeywords) {
        if (input.contains(keyword)) {
            return true;
        }
    }
    return false;
}
```

## Entity Extraction Patterns

### Contract Numbers
```java
// Pattern 1: 6-digit numbers with word boundaries
Pattern.compile("\\b\\d{6}\\b")

// Pattern 2: 6-digit numbers that might be concatenated
Pattern.compile("\\d{6}")

// Example: "show100476" → "100476"
```

### Part Numbers
```java
// Pattern: 2 letters + 4-6 digits
Pattern.compile("\\b[A-Za-z]{2}\\d{4,6}\\b")

// Example: "AB12345" → "AB12345"
```

### Customer Numbers
```java
// Pattern: 4-8 digit numbers
Pattern.compile("\\b\\d{4,8}\\b")

// Example: "customer 123456" → "123456"
```

### "Created By" Pattern
```java
// Pattern: "created" + "by" + name
if (input.contains("created") && input.contains("by")) {
    // Extract name after "by"
    String creatorName = extractCreatorName(input);
}

// Example: "contracts created by vinod" → "vinod"
```

## Display Entity Selection

### Contract Display Entities
```java
// Default entities
displayEntities.add("CONTRACT_NAME");
displayEntities.add("CUSTOMER_NAME");
displayEntities.add("CUSTOMER_NUMBER");
displayEntities.add("CREATE_DATE");
displayEntities.add("EXPIRATION_DATE");
displayEntities.add("STATUS");

// Conditional entities based on query content
if (containsDateKeywords(input)) {
    displayEntities.add("EFFECTIVE_DATE");
    displayEntities.add("EXPIRATION_DATE");
    displayEntities.add("CREATED_DATE");
}
```

### Parts Display Entities
```java
// Default entities
displayEntities.add("PART_NUMBER");
displayEntities.add("CONTRACT_NO");
displayEntities.add("PRICE");
displayEntities.add("LEAD_TIME");
displayEntities.add("STATUS");

// Conditional entities based on query content
if (containsPriceKeywords(input)) {
    displayEntities.add("PRICE");
    displayEntities.add("FUTURE_PRICE");
    displayEntities.add("QUOTE_COST");
}
```

## Error Handling and Validation

### Input Validation
Each processor implements validation logic:

```java
private List<ValidationError> validateInput(HeaderInfo headerInfo, List<EntityFilter> entities) {
    List<ValidationError> errors = new ArrayList<>();
    
    // Check for required identifiers
    if (headerInfo.header.contractNumber == null && headerInfo.header.partNumber == null) {
        errors.add(new ValidationError("MISSING_IDENTIFIER", 
            "Please provide a contract number or part number", "WARNING"));
    }
    
    return errors;
}
```

### Error Categories
- **MISSING_IDENTIFIER**: No contract/part number provided
- **INVALID_FORMAT**: Incorrect number format
- **AMBIGUOUS_QUERY**: Multiple possible interpretations
- **PROCESSING_ERROR**: System processing issues

## Benefits of Separation

### 1. **Single Responsibility**
- Each processor handles one specific domain
- Clear separation of concerns
- Focused functionality

### 2. **Extensibility**
- Easy to add new processors
- Simple to extend existing processors
- Modular architecture

### 3. **Maintainability**
- Isolated changes
- Clear code organization
- Easy debugging

### 4. **Accuracy**
- Domain-specific logic
- Specialized pattern recognition
- Optimized entity extraction

### 5. **Performance**
- Targeted processing
- Reduced complexity
- Efficient routing

## Integration Points

### 1. ConversationalNLPManager
```java
// Routes to NLPQueryClassifier
NLPQueryClassifier.QueryResult nlpResult = nlpClassifier.processQuery(userInput);
```

### 2. NLPUserActionHandler
```java
// Uses processor results for action execution
String actionType = nlpResult.metadata.actionType;
List<EntityFilter> entities = nlpResult.entities;
```

### 3. StandardJSONProcessor
```java
// Legacy integration for backward compatibility
// Can be replaced with processor-based approach
```

## Future Enhancements

### 1. **Additional Processors**
- **CustomerProcessor**: Customer-specific queries
- **DateProcessor**: Date range and temporal queries
- **StatusProcessor**: Status and workflow queries

### 2. **Enhanced Pattern Recognition**
- Machine learning-based classification
- Context-aware entity extraction
- Semantic similarity matching

### 3. **Processor Chaining**
- Multi-domain query support
- Cross-processor collaboration
- Complex query decomposition

### 4. **Performance Optimization**
- Processor caching
- Parallel processing
- Query result caching

## Conclusion

The NLP operations separation design provides a **robust, scalable, and maintainable** architecture for natural language query processing. Each processor specializes in its domain while the main classifier provides intelligent routing and coordination. This design enables **high accuracy**, **easy extension**, and **clear maintenance** of the NLP system. 