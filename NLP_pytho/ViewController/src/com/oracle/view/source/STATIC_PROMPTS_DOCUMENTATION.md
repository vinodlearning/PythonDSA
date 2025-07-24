# Static Predefined Prompts Documentation

## 🎯 **Overview**

This document describes the **static predefined prompts** for command buttons that are defined in `BCCTChatBotUtility.java` and used by `NLPQueryClassifier.java`. These prompts are **fixed and cannot be modified by users** to ensure security and consistency.

## 📋 **Static Prompt Definitions**

### **Location**: `BCCTChatBotUtility.java`

All static prompts are defined as `public static final String` constants at the top of the class:

```java
// ========================================
// STATIC PREDEFINED PROMPTS FOR COMMAND BUTTONS
// These prompts are fixed and cannot be modified by users
// ========================================

public static final String QUICK_ACTION_RECENT_CONTRACTS_PROMPT = "show me contracts created in the last 24 hours";
public static final String QUICK_ACTION_PARTS_COUNT_PROMPT = "what is the total count of parts loaded in the system";
public static final String QUICK_ACTION_FAILED_CONTRACTS_PROMPT = "show me contracts with failed parts and their counts";
public static final String QUICK_ACTION_EXPIRING_SOON_PROMPT = "show me contracts expiring in the next 30 days";
public static final String QUICK_ACTION_AWARD_REPS_PROMPT = "list all award representatives and their contract counts";
public static final String QUICK_ACTION_HELP_PROMPT = "show me help and available features";
public static final String QUICK_ACTION_CREATE_CONTRACT_PROMPT = "show me the steps to create a contract";
```

## 🎯 **Command Button Mapping**

| **Button** | **Static Prompt** | **Action Type** | **Description** |
|------------|-------------------|-----------------|-----------------|
| **Recent Contracts** | `"show me contracts created in the last 24 hours"` | `QUICK_ACTION_RECENT_CONTRACTS` | Shows contracts created in last 24 hours |
| **Parts Count** | `"what is the total count of parts loaded in the system"` | `QUICK_ACTION_PARTS_COUNT` | Shows total parts count in system |
| **Failed Contracts** | `"show me contracts with failed parts and their counts"` | `QUICK_ACTION_FAILED_CONTRACTS` | Shows contracts with failed parts |
| **Expiring Soon** | `"show me contracts expiring in the next 30 days"` | `QUICK_ACTION_EXPIRING_SOON` | Shows contracts expiring soon |
| **Award Reps** | `"list all award representatives and their contract counts"` | `QUICK_ACTION_AWARD_REPS` | Shows award representatives |
| **Help** | `"show me help and available features"` | `QUICK_ACTION_HELP` | Shows help information |
| **Create Contract** | `"show me the steps to create a contract"` | `QUICK_ACTION_CREATE_CONTRACT` | Shows contract creation steps |

## 🔧 **Static Collections**

### **1. Array of All Prompts**
```java
public static final String[] ALL_QUICK_ACTION_PROMPTS = {
    QUICK_ACTION_RECENT_CONTRACTS_PROMPT,
    QUICK_ACTION_PARTS_COUNT_PROMPT,
    QUICK_ACTION_FAILED_CONTRACTS_PROMPT,
    QUICK_ACTION_EXPIRING_SOON_PROMPT,
    QUICK_ACTION_AWARD_REPS_PROMPT,
    QUICK_ACTION_HELP_PROMPT,
    QUICK_ACTION_CREATE_CONTRACT_PROMPT
};
```

### **2. Prompt to Action Type Mapping**
```java
public static final Map<String, String> PROMPT_TO_ACTION_TYPE_MAP = new HashMap<String, String>() {{
    put(QUICK_ACTION_RECENT_CONTRACTS_PROMPT, "QUICK_ACTION_RECENT_CONTRACTS");
    put(QUICK_ACTION_PARTS_COUNT_PROMPT, "QUICK_ACTION_PARTS_COUNT");
    put(QUICK_ACTION_FAILED_CONTRACTS_PROMPT, "QUICK_ACTION_FAILED_CONTRACTS");
    put(QUICK_ACTION_EXPIRING_SOON_PROMPT, "QUICK_ACTION_EXPIRING_SOON");
    put(QUICK_ACTION_AWARD_REPS_PROMPT, "QUICK_ACTION_AWARD_REPS");
    put(QUICK_ACTION_HELP_PROMPT, "QUICK_ACTION_HELP");
    put(QUICK_ACTION_CREATE_CONTRACT_PROMPT, "QUICK_ACTION_CREATE_CONTRACT");
}};
```

## 🛠 **Static Utility Methods**

### **1. Get Prompt for Action Type**
```java
public static String getPromptForActionType(String actionType)
```
- **Purpose**: Get the predefined prompt for a specific action type
- **Parameters**: `actionType` (e.g., "QUICK_ACTION_RECENT_CONTRACTS")
- **Returns**: The corresponding predefined prompt
- **Default**: Returns help prompt if not found

### **2. Get Action Type for Prompt**
```java
public static String getActionTypeForPrompt(String prompt)
```
- **Purpose**: Get the action type for a given prompt
- **Parameters**: `prompt` (the predefined prompt)
- **Returns**: The corresponding action type
- **Default**: Returns "QUICK_ACTION_HELP" if not found

### **3. Check if Input is Predefined Prompt**
```java
public static boolean isPredefinedPrompt(String userInput)
```
- **Purpose**: Check if user input matches any predefined prompt
- **Parameters**: `userInput` (the user input to check)
- **Returns**: `true` if it matches a predefined prompt, `false` otherwise
- **Logic**: Case-insensitive exact matching

## 🔄 **Integration with NLPQueryClassifier**

### **Updated Methods in NLPQueryClassifier.java**

#### **1. processQuery() - Static Prompt Check**
```java
// Step 1: FIRST CHECK - Is this a static predefined prompt?
if (BCCTChatBotUtility.isPredefinedPrompt(userInput)) {
    System.out.println("=== Static Predefined Prompt Detected ===");
    
    // Get the action type directly from static prompts
    String actionType = BCCTChatBotUtility.getActionTypeForPrompt(userInput);
    
    // Create quick action result without any preprocessing
    result.inputTracking = new InputTrackingResult(userInput, userInput, 1.0);
    result.metadata = new QueryMetadata("QUICK_ACTION", actionType, System.currentTimeMillis() - startTime);
    // ... rest of quick action processing
    return result;
}
```

## 🔄 **Integration with BCCTContractManagementNLPBean**

### **Button Getter Methods**
The bean now provides getter methods that reference `BCCTChatBotUtility`:

```java
public String getButtonRecentContracts() {
    return BCCTChatBotUtility.QUICK_ACTION_RECENT_CONTRACTS_PROMPT;
}

public String getButtonPartsCount() {
    return BCCTChatBotUtility.QUICK_ACTION_PARTS_COUNT_PROMPT;
}

// ... similar methods for all buttons
```

## 🎯 **Architecture Benefits**

### **1. Centralized Location**
- **Single Source**: All static prompts in `BCCTChatBotUtility`
- **Shared Access**: Multiple classes can access the same prompts
- **Easy Maintenance**: Update prompts in one place

### **2. Separation of Concerns**
- **BCCTChatBotUtility**: Contains static data and utility methods
- **BCCTContractManagementNLPBean**: UI logic and button getters
- **NLPQueryClassifier**: NLP processing logic

### **3. Reusability**
- **Multiple Usage**: Both bean and classifier use the same prompts
- **Consistency**: Same prompts across all components
- **No Duplication**: Single definition, multiple references

## 🎯 **Security Benefits**

### **1. Immutable Prompts**
- All prompts are `public static final String`
- Cannot be modified at runtime
- Prevents user tampering

### **2. Centralized Definition**
- Single source of truth in `BCCTChatBotUtility`
- Consistent across all components
- Easy to maintain and update

### **3. Validation**
- `isPredefinedPrompt()` method validates input
- Only accepts exact matches of predefined prompts
- Rejects any user-generated prompts

## 🚀 **Usage Examples**

### **Example 1: Button Click Handler**
```java
// When user clicks "Recent Contracts" button
String fixedPrompt = BCCTChatBotUtility.QUICK_ACTION_RECENT_CONTRACTS_PROMPT;
// Send to ConversationalNLPManager
```

### **Example 2: Validation**
```java
// Check if user input is a predefined prompt
if (BCCTChatBotUtility.isPredefinedPrompt(userInput)) {
    // Process as quick action
    String actionType = BCCTChatBotUtility.getActionTypeForPrompt(userInput);
}
```

### **Example 3: Bean Getter**
```java
// In BCCTContractManagementNLPBean
public String getButtonRecentContracts() {
    return BCCTChatBotUtility.QUICK_ACTION_RECENT_CONTRACTS_PROMPT;
}
```

### **Example 4: Iteration**
```java
// Iterate through all predefined prompts
for (String prompt : BCCTChatBotUtility.ALL_QUICK_ACTION_PROMPTS) {
    System.out.println("Available prompt: " + prompt);
}
```

## 📊 **Flow Diagram**

```
Button Click → BCCTChatBotUtility Static Prompt → NLPQueryClassifier → ConversationalNLPManager → Quick Action Handler
```

## ✅ **Benefits Achieved**

1. **🔒 Security**: Prompts cannot be modified by users
2. **🔄 Consistency**: Single source of truth for all prompts
3. **🛠 Maintainability**: Easy to update prompts in one place
4. **🎯 Validation**: Built-in validation prevents unauthorized prompts
5. **📋 Documentation**: Clear mapping between buttons and prompts
6. **🚀 Performance**: Static constants for fast access
7. **🔧 Flexibility**: Easy to add new prompts or modify existing ones
8. **🏗️ Architecture**: Proper separation of concerns
9. **♻️ Reusability**: Multiple classes can use the same prompts
10. **📦 Centralization**: All static data in utility class

## 🎯 **Next Steps**

1. **UI Integration**: Use these static prompts in button click handlers
2. **Testing**: Verify all prompts work correctly with NLP pipeline
3. **Documentation**: Update UI documentation with button-to-prompt mapping
4. **Monitoring**: Add logging to track prompt usage and performance 