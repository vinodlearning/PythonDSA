# 🔧 **NLP Method Reference Guide**

## 📋 **Quick Reference for Key Methods**

### **🔄 Main Processing Flow**

| Method | File | Line | Purpose | Input | Output |
|--------|------|------|---------|-------|--------|
| `processQuery()` | StandardJSONProcessor.java | 2363 | **Main Entry Point** | `String originalInput` | `String JSON` |
| `processInputTracking()` | StandardJSONProcessor.java | 450 | Spell Correction & Tracking | `String originalInput` | `InputTrackingResult` |
| `normalizePrompt()` | StandardJSONProcessor.java | 105 | Text Normalization | `String input` | `String normalized` |

### **🎯 Query Classification**

| Method | File | Line | Purpose | Logic |
|--------|------|------|---------|-------|
| `determineQueryType()` | StandardJSONProcessor.java | 3455 | **Query Type Classification** | CONTRACTS, PARTS, FAILED_PARTS, HELP |
| `findoutTheActionType()` | StandardJSONProcessor.java | 2934 | **Action Type Override** | Final action type determination |

### **🔍 Entity Extraction**

| Method | File | Line | Purpose | Patterns |
|--------|------|------|---------|----------|
| `extractContractNumbers()` | StandardJSONProcessor.java | 1162 | **Contract Number Extraction** | `\b\d{6,}\b` |
| `extractPartNumbers()` | StandardJSONProcessor.java | 1204 | **Part Number Extraction** | Multiple patterns for different formats |
| `extractCustomerNumbers()` | StandardJSONProcessor.java | 1324 | **Customer Number Extraction** | `\b\d{4,8}\b` |
| `extractDateFilters()` | StandardJSONProcessor.java | 1037 | **Date Range Extraction** | `between`, `after`, `before` |
| `extractStatusFilters()` | StandardJSONProcessor.java | 1145 | **Status Extraction** | `active`, `expired`, `failed` |

### **📊 Action & Display Logic**

| Method | File | Line | Purpose | Logic |
|--------|------|------|---------|-------|
| `determineActionType()` | StandardJSONProcessor.java | 1677 | **Action Type Mapping** | Based on query type and entities |
| `determineDisplayEntitiesFromPrompt()` | StandardJSONProcessor.java | 3274 | **Display Field Selection** | Context-aware field selection |
| `determineFilterEntities()` | StandardJSONProcessor.java | 3609 | **Filter Assignment** | Entity-based filtering |

### **🔧 Helper Methods**

| Method | File | Line | Purpose |
|--------|------|------|---------|
| `tokenizeInput()` | StandardJSONProcessor.java | 701 | Input tokenization |
| `splitConcatenatedWords()` | StandardJSONProcessor.java | 735 | Word boundary correction |
| `isValidPartNumber()` | StandardJSONProcessor.java | 1291 | Part number validation |
| `isValidContractNumber()` | StandardJSONProcessor.java | 1295 | Contract number validation |
| `validateInput()` | StandardJSONProcessor.java | 1628 | Business rule validation |

### **📝 JSON Generation**

| Method | File | Line | Purpose |
|--------|------|------|---------|
| `generateStandardJSON()` | StandardJSONProcessor.java | 1768 | **Success JSON Generation** |
| `generateErrorJSON()` | StandardJSONProcessor.java | 1879 | **Error JSON Generation** |

---

## 🎯 **Query Type Classification Logic**

### **CONTRACTS Classification**
```java
// Triggers:
- Contract number (6+ digits) present
- Contract-specific keywords: "effective date", "expiration", "payment terms"
- Price expiration queries (price + expir/expiry/experation)

// Method: determineQueryType() - Line 3455
```

### **PARTS Classification**
```java
// Triggers:
- Part number patterns present
- "invoice parts" keywords
- "parts for contract" patterns
- Part-specific attributes: "lead time", "MOQ", "UOM"

// Method: determineQueryType() - Line 3455
```

### **FAILED_PARTS Classification**
```java
// Triggers:
- "failed", "error", "failure" keywords
- "loaded", "missing data" keywords
- Part context present

// Method: determineQueryType() - Line 3455
```

### **HELP Classification**
```java
// Triggers:
- Explicit creation intent: "create", "make", "generate"
- Explicit help intent: "help", "steps", "guide"
- No part numbers present (prevents misclassification)

// Method: determineQueryType() - Line 3455
```

---

## 🔄 **Action Type Mapping**

### **CONTRACTS Actions**
| Pattern | Action Type | Method |
|---------|-------------|--------|
| Contract number present | `contracts_by_contractnumber` | `determineActionType()` |
| Price expiration | `contracts_by_contractnumber` | `determineActionType()` |
| No contract number | `contracts_by_filter` | `determineActionType()` |

### **PARTS Actions**
| Pattern | Action Type | Method |
|---------|-------------|--------|
| Part number present | `parts_by_partnumber` | `determineActionType()` |
| Contract number + parts | `parts_by_contractnumber` | `determineActionType()` |
| Default | `parts_by_partnumber` | `determineActionType()` |

### **FAILED_PARTS Actions**
| Pattern | Action Type | Method |
|---------|-------------|--------|
| Contract number present | `failed_parts_by_contractnumber` | `determineActionType()` |
| No contract number | `failed_parts_by_filter` | `determineActionType()` |

---

## 📋 **Display Entity Selection**

### **CONTRACTS Display Entities**
```java
// Method: determineDisplayEntitiesFromPrompt() - Line 3274

// Generic detail requests
["CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE", "STATUS", "CONTRACT_TYPE", "PAYMENT_TERMS"]

// Specific field requests
"effective date" → ["EFFECTIVE_DATE"]
"expiration" → ["EXPIRATION_DATE"]
"contract type" → ["CONTRACT_TYPE"]
"status" → ["STATUS"]
"payment terms" → ["PAYMENT_TERMS"]
"incoterms" → ["INCOTERMS"]
```

### **PARTS Display Entities**
```java
// Method: determineDisplayEntitiesFromPrompt() - Line 3320

// Part details queries
["INVOICE_PART_NUMBER", "PRICE", "LEAD_TIME", "MOQ", "UOM", "STATUS"]

// Specific queries
"lead time" → ["INVOICE_PART_NUMBER", "LEAD_TIME"]
"price" → ["INVOICE_PART_NUMBER", "PRICE"]
"MOQ" → ["INVOICE_PART_NUMBER", "MOQ"]
"UOM" → ["INVOICE_PART_NUMBER", "UOM"]
"active" → ["INVOICE_PART_NUMBER", "STATUS"]
```

---

## ✏️ **Spell Correction**

### **WordDatabase Corrections**
```java
// Method: processInputTracking() - Line 450
// Examples:
"detials" → "details"
"staus" → "status"
"efective" → "effective"
"informaton" → "information"
```

### **Additional Corrections**
```java
// Method: processInputTracking() - Line 520
"contrat" → "contract"
"experation" → "expiration"
"expiry" → "expiration"
```

---

## ✅ **Validation Rules**

### **Business Rule Validation**
```java
// Method: validateInput() - Line 1628

// Rule 1: Contract numbers must be 6+ digits
// Rule 2: Part numbers must be 3+ alphanumeric characters
// Rule 3: Customer numbers must be 4-8 digits
```

### **Filter Entity Assignment**
```java
// Method: determineFilterEntities() - Line 3609

// CONTRACTS queries: AWARD_NUMBER
// FAILED_PARTS queries: LOADED_CP_NUMBER
// PARTS queries: INVOICE_PART_NUMBER
```

---

## 🧪 **Test Classes**

| Test Class | Purpose | Key Methods Tested |
|------------|---------|-------------------|
| `PartsQueryFixTest` | Parts query validation | `determineQueryType()`, `determineActionType()` |
| `FinalValidationTest` | Contract query validation | `determineDisplayEntitiesFromPrompt()` |
| `ComprehensiveTest` | End-to-end validation | All methods |

---

## 🚨 **Error Handling**

### **Exception Handling**
```java
// Method: processQuery() - Line 2363
try {
    // All processing logic
} catch (Exception e) {
    return generateErrorJSON(originalInput, e.getMessage(), processingTime);
}
```

### **Error JSON Structure**
```json
{
  "header": { "inputTracking": { "originalInput": "...", "correctedInput": null, "correctionConfidence": 0 } },
  "queryMetadata": { "queryType": "CONTRACTS", "actionType": "error", "processingTimeMs": 0.0 },
  "entities": [],
  "displayEntities": [],
  "errors": [{ "code": "PROCESSING_ERROR", "message": "...", "severity": "BLOCKER" }]
}
```

---

## 📈 **Performance Tracking**

### **Processing Time**
```java
// Method: processQuery() - Line 2363
long startTime = System.nanoTime();
// ... processing logic ...
long endTime = System.nanoTime();
double processingTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
```

### **Current Performance**
- **Query Type Accuracy**: 97.1% (68/70 test cases)
- **Action Type Accuracy**: 97.1% (68/70 test cases)
- **Display Entity Accuracy**: 100% (70/70 test cases)

---

## 🔄 **Maintenance Checklist**

### **When Adding New Query Types**
- [ ] Update `determineQueryType()` method
- [ ] Add action types in `determineActionType()`
- [ ] Define display entities in `determineDisplayEntitiesFromPrompt()`
- [ ] Add filter entity logic in `determineFilterEntities()`
- [ ] Update test cases

### **When Adding New Entities**
- [ ] Create extraction method following existing patterns
- [ ] Add validation rules in `validateInput()`
- [ ] Update business rule validation
- [ ] Add test coverage

### **When Modifying Spell Correction**
- [ ] Update `WordDatabase.getSpellCorrections()`
- [ ] Add additional corrections in `processInputTracking()`
- [ ] Test with typo scenarios

---

**Last Updated**: December 2024  
**Version**: 1.0  
**Status**: Production Ready ✅ 