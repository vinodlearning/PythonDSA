# Quick Action Button Prompts

## 🎯 **Overview**

This document defines the fixed prompts for each command button that will be processed by NLPQueryClassifier to return the expected query and action types.

## 📋 **Button Prompts**

### **1. Recent Contracts Button**
- **Fixed Prompt**: `"show me contracts created in the last 24 hours"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_RECENT_CONTRACTS`

### **2. Parts Count Button**
- **Fixed Prompt**: `"what is the total count of parts loaded in the system"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_PARTS_COUNT`

### **3. Failed Contracts Button**
- **Fixed Prompt**: `"show me contracts with failed parts and their counts"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_FAILED_CONTRACTS`

### **4. Expiring Soon Button**
- **Fixed Prompt**: `"show me contracts expiring in the next 30 days"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_EXPIRING_SOON`

### **5. Award Reps Button**
- **Fixed Prompt**: `"list all award representatives and their contract counts"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_AWARD_REPS`

### **6. Help Button**
- **Fixed Prompt**: `"show me help and available features"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_HELP`

### **7. Create Contract Button**
- **Fixed Prompt**: `"show me the steps to create a contract"`
- **Expected Query Type**: `QUICK_ACTION`
- **Expected Action Type**: `QUICK_ACTION_CREATE_CONTRACT`

## 🔧 **Implementation Flow**

```
Button Click → Fixed Prompt → NLPQueryClassifier → ConversationalNLPManager → Quick Action Method
```

## 📊 **Prompt Processing**

Each button click will:
1. Use the fixed prompt as user input
2. Process through NLPQueryClassifier
3. Return QUICK_ACTION query type
4. Return specific QUICK_ACTION_* action type
5. Route through ConversationalNLPManager
6. Call appropriate method in NLPUserActionHandler

## 🎯 **Benefits**

1. **Consistent Processing**: All buttons use the same NLP pipeline
2. **Standardized Responses**: Fixed prompts ensure consistent classification
3. **Integration**: Seamlessly integrates with existing NLP infrastructure
4. **Maintainability**: Easy to modify prompts or add new buttons
5. **Debugging**: Clear trace from button click to action execution 