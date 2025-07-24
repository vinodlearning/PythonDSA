package com.oracle.view.source;

import java.util.*;

/**
 * Contract Creation Configuration
 * 
 * Centralized configuration for contract creation attributes and validation rules.
 * This allows business users to modify contract creation requirements without code changes.
 */
public class ContractCreationConfig {
    
    private static volatile ContractCreationConfig instance;
    private final TableColumnConfig tableConfig;
    
    // Configuration constants
    private static final String WORKFLOW_TYPE_CONTRACT_CREATION = "CONTRACT_CREATION";
    private static final long SESSION_TIMEOUT_MS = 600000; // 10 minutes
    
    // Attribute configuration
    private final Map<String, AttributeConfig> attributeConfigs;
    
    private ContractCreationConfig() {
        this.tableConfig = TableColumnConfig.getInstance();
        this.attributeConfigs = initializeAttributeConfigs();
    }
    
    public static ContractCreationConfig getInstance() {
        if (instance == null) {
            synchronized (ContractCreationConfig.class) {
                if (instance == null) {
                    instance = new ContractCreationConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * Attribute Configuration - defines validation rules and prompts for each attribute
     */
    public static class AttributeConfig {
        private final String attributeName;
        private final String displayName;
        private final String prompt;
        private final String validationPattern;
        private final String validationMessage;
        private final boolean required;
        private final int minLength;
        private final int maxLength;
        private final String dataType;
        private final List<String> allowedValues;
        
        public AttributeConfig(String attributeName, String displayName, String prompt, 
                             String validationPattern, String validationMessage, boolean required,
                             int minLength, int maxLength, String dataType, List<String> allowedValues) {
            this.attributeName = attributeName;
            this.displayName = displayName;
            this.prompt = prompt;
            this.validationPattern = validationPattern;
            this.validationMessage = validationMessage;
            this.required = required;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.dataType = dataType;
            this.allowedValues = allowedValues != null ? allowedValues : new ArrayList<>();
        }
        
        // Getters
        public String getAttributeName() { return attributeName; }
        public String getDisplayName() { return displayName; }
        public String getPrompt() { return prompt; }
        public String getValidationPattern() { return validationPattern; }
        public String getValidationMessage() { return validationMessage; }
        public boolean isRequired() { return required; }
        public int getMinLength() { return minLength; }
        public int getMaxLength() { return maxLength; }
        public String getDataType() { return dataType; }
        public List<String> getAllowedValues() { return allowedValues; }
    }
    
    /**
     * Initialize attribute configurations
     * This is where business users can modify contract creation requirements
     */
    private Map<String, AttributeConfig> initializeAttributeConfigs() {
        Map<String, AttributeConfig> configs = new LinkedHashMap<>();
        
        // Customer Number Configuration
        configs.put("CUSTOMER_NUMBER", new AttributeConfig(
            "CUSTOMER_NUMBER",
            "Customer Account Number",
            "Please provide the customer account number:",
            "\\d{4,8}",
            "Please provide a valid customer account number (4-8 digits).",
            true,
            4,
            8,
            "NUMBER",
            null
        ));
        
        // Contract Name Configuration
        configs.put("CONTRACT_NAME", new AttributeConfig(
            "CONTRACT_NAME",
            "Contract Name",
            "Please provide a name for the contract:",
            "[A-Za-z0-9\\s\\-_]+",
            "Please provide a valid contract name (letters, numbers, spaces, hyphens, underscores only).",
            true,
            3,
            100,
            "TEXT",
            null
        ));
        
        // Title Configuration
        configs.put("TITLE", new AttributeConfig(
            "TITLE",
            "Contract Title",
            "Please provide a title for the contract:",
            "[A-Za-z0-9\\s\\-_]+",
            "Please provide a valid contract title (letters, numbers, spaces, hyphens, underscores only).",
            true,
            3,
            200,
            "TEXT",
            null
        ));
        
        // Description Configuration
        configs.put("DESCRIPTION", new AttributeConfig(
            "DESCRIPTION",
            "Contract Description",
            "Please provide a description for the contract:",
            "[A-Za-z0-9\\s\\-_.,!?()]+",
            "Please provide a valid contract description.",
            true,
            10,
            500,
            "TEXT",
            null
        ));
        
        // Effective Date Configuration
        configs.put("EFFECTIVE_DATE", new AttributeConfig(
            "EFFECTIVE_DATE",
            "Effective Date",
            "Please provide the effective date (MM-DD-YYYY):",
            "\\d{2}-\\d{2}-\\d{4}",
            "Please provide a valid date in MM-DD-YYYY format.",
            true,
            10,
            10,
            "DATE",
            null
        ));
        
        // Expiration Date Configuration
        configs.put("EXPIRATION_DATE", new AttributeConfig(
            "EXPIRATION_DATE",
            "Expiration Date",
            "Please provide the expiration date (MM-DD-YYYY):",
            "\\d{2}-\\d{2}-\\d{4}",
            "Please provide a valid date in MM-DD-YYYY format.",
            true,
            10,
            10,
            "DATE",
            null
        ));
        
        // Contract Type Configuration (Optional)
        configs.put("CONTRACT_TYPE", new AttributeConfig(
            "CONTRACT_TYPE",
            "Contract Type",
            "Please select the contract type:",
            null,
            "Please select a valid contract type.",
            false,
            0,
            50,
            "SELECT",
            Arrays.asList("SERVICE", "SUPPLY", "LICENSE", "MAINTENANCE", "SUPPORT")
        ));
        
        // Payment Terms Configuration (Optional)
        configs.put("PAYMENT_TERMS", new AttributeConfig(
            "PAYMENT_TERMS",
            "Payment Terms",
            "Please specify payment terms:",
            "[A-Za-z0-9\\s\\-_]+",
            "Please provide valid payment terms.",
            false,
            3,
            100,
            "TEXT",
            null
        ));
        
        // Currency Configuration (Optional)
        configs.put("CURRENCY", new AttributeConfig(
            "CURRENCY",
            "Currency",
            "Please specify the currency:",
            "[A-Z]{3}",
            "Please provide a valid 3-letter currency code (e.g., USD, EUR).",
            false,
            3,
            3,
            "CURRENCY",
            Arrays.asList("USD", "EUR", "GBP", "CAD", "AUD", "JPY")
        ));
        
        return configs;
    }
    
    /**
     * Get all required attributes in order
     */
    public List<String> getRequiredAttributes() {
        List<String> required = new ArrayList<>();
        for (Map.Entry<String, AttributeConfig> entry : attributeConfigs.entrySet()) {
            if (entry.getValue().isRequired()) {
                required.add(entry.getKey());
            }
        }
        return required;
    }
    
    /**
     * Get all optional attributes
     */
    public List<String> getOptionalAttributes() {
        List<String> optional = new ArrayList<>();
        for (Map.Entry<String, AttributeConfig> entry : attributeConfigs.entrySet()) {
            if (!entry.getValue().isRequired()) {
                optional.add(entry.getKey());
            }
        }
        return optional;
    }
    
    /**
     * Get all attributes (required + optional)
     */
    public List<String> getAllAttributes() {
        return new ArrayList<>(attributeConfigs.keySet());
    }
    
    /**
     * Get attribute configuration by name
     */
    public AttributeConfig getAttributeConfig(String attributeName) {
        return attributeConfigs.get(attributeName);
    }
    
    /**
     * Get next required attribute after completed ones
     */
    public String getNextRequiredAttribute(List<String> completedAttributes) {
        List<String> requiredAttributes = getRequiredAttributes();
        
        for (String attribute : requiredAttributes) {
            if (!completedAttributes.contains(attribute)) {
                return attribute;
            }
        }
        
        return null; // All required attributes completed
    }
    
    /**
     * Validate input for a specific attribute
     */
    public String validateAttributeInput(String attributeName, String input) {
        AttributeConfig config = attributeConfigs.get(attributeName);
        if (config == null) {
            return "Unknown attribute: " + attributeName;
        }
        
        // Check if required
        if (config.isRequired() && (input == null || input.trim().isEmpty())) {
            return "Please provide a value for " + config.getDisplayName() + ".";
        }
        
        // If not required and empty, it's valid
        if (!config.isRequired() && (input == null || input.trim().isEmpty())) {
            return null;
        }
        
        String trimmed = input.trim();
        
        // Check length constraints
        if (trimmed.length() < config.getMinLength()) {
            return config.getDisplayName() + " must be at least " + config.getMinLength() + " characters.";
        }
        
        if (config.getMaxLength() > 0 && trimmed.length() > config.getMaxLength()) {
            return config.getDisplayName() + " must be no more than " + config.getMaxLength() + " characters.";
        }
        
        // Check pattern validation
        if (config.getValidationPattern() != null && !trimmed.matches(config.getValidationPattern())) {
            return config.getValidationMessage();
        }
        
        // Check allowed values for select type
        if ("SELECT".equals(config.getDataType()) && !config.getAllowedValues().isEmpty()) {
            if (!config.getAllowedValues().contains(trimmed.toUpperCase())) {
                return "Please select from: " + String.join(", ", config.getAllowedValues());
            }
        }
        
        // Check allowed values for currency type
        if ("CURRENCY".equals(config.getDataType()) && !config.getAllowedValues().isEmpty()) {
            if (!config.getAllowedValues().contains(trimmed.toUpperCase())) {
                return "Please select from: " + String.join(", ", config.getAllowedValues());
            }
        }
        
        return null; // Validation passed
    }
    
    /**
     * Extract value for a specific attribute
     */
    public String extractAttributeValue(String attributeName, String input) {
        AttributeConfig config = attributeConfigs.get(attributeName);
        if (config == null) {
            return input;
        }
        
        String trimmed = input.trim();
        
        // Handle different data types
        switch (config.getDataType()) {
            case "NUMBER":
                return extractNumber(trimmed);
                
            case "DATE":
                return extractDate(trimmed);
                
            case "CURRENCY":
                return trimmed.toUpperCase();
                
            case "SELECT":
                return trimmed.toUpperCase();
                
            default:
                return trimmed;
        }
    }
    
    /**
     * Extract number from input
     */
    private String extractNumber(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : input;
    }
    
    /**
     * Extract date from input
     */
    private String extractDate(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{2}-\\d{2}-\\d{4})");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : input;
    }
    
    /**
     * Get prompt for a specific attribute
     */
    public String getAttributePrompt(String attributeName) {
        AttributeConfig config = attributeConfigs.get(attributeName);
        return config != null ? config.getPrompt() : "Please provide the required information:";
    }
    
    /**
     * Get display name for a specific attribute
     */
    public String getAttributeDisplayName(String attributeName) {
        AttributeConfig config = attributeConfigs.get(attributeName);
        return config != null ? config.getDisplayName() : attributeName;
    }
    
    /**
     * Check if all required attributes are completed
     */
    public boolean hasAllRequiredAttributes(List<String> completedAttributes) {
        List<String> requiredAttributes = getRequiredAttributes();
        return completedAttributes.containsAll(requiredAttributes);
    }
    
    /**
     * Get configuration summary
     */
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Contract Creation Configuration Summary:\n");
        summary.append("========================================\n\n");
        
        summary.append("Required Attributes (").append(getRequiredAttributes().size()).append("):\n");
        for (String attr : getRequiredAttributes()) {
            AttributeConfig config = attributeConfigs.get(attr);
            summary.append("  - ").append(config.getDisplayName())
                   .append(" (").append(config.getDataType()).append(")\n");
        }
        
        summary.append("\nOptional Attributes (").append(getOptionalAttributes().size()).append("):\n");
        for (String attr : getOptionalAttributes()) {
            AttributeConfig config = attributeConfigs.get(attr);
            summary.append("  - ").append(config.getDisplayName())
                   .append(" (").append(config.getDataType()).append(")\n");
        }
        
        summary.append("\nTotal Attributes: ").append(attributeConfigs.size()).append("\n");
        summary.append("Session Timeout: ").append(SESSION_TIMEOUT_MS / 1000).append(" seconds\n");
        
        return summary.toString();
    }
    
    /**
     * Add new attribute configuration (for runtime configuration)
     */
    public void addAttributeConfig(AttributeConfig config) {
        attributeConfigs.put(config.getAttributeName(), config);
    }
    
    /**
     * Remove attribute configuration (for runtime configuration)
     */
    public void removeAttributeConfig(String attributeName) {
        attributeConfigs.remove(attributeName);
    }
    
    /**
     * Update attribute configuration (for runtime configuration)
     */
    public void updateAttributeConfig(AttributeConfig config) {
        attributeConfigs.put(config.getAttributeName(), config);
    }
} 