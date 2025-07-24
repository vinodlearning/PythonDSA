package com.oracle.view.source;

/**
 * Contract Creation Demo
 * 
 * Demonstrates the contract creation workflow without requiring Oracle ADF dependencies.
 * This is a standalone demonstration of the workflow logic.
 */
public class ContractCreationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== CONTRACT CREATION WORKFLOW DEMO ===");
        System.out.println();
        
        // Test the configuration system
        testConfiguration();
        
        System.out.println("\n" + "============================================================\n");
        
        // Test the workflow logic
        testWorkflowLogic();
        
        System.out.println("\n" + "============================================================\n");
        
        // Test validation scenarios
        testValidationScenarios();
        
        System.out.println("\n=== CONTRACT CREATION DEMO COMPLETED ===");
    }
    
    /**
     * Test the configuration system
     */
    private static void testConfiguration() {
        System.out.println("TEST 1: CONFIGURATION SYSTEM");
        System.out.println("=============================");
        
        ContractCreationConfig config = ContractCreationConfig.getInstance();
        
        // Show configuration summary
        System.out.println(config.getConfigurationSummary());
        
        // Test required attributes
        System.out.println("Required Attributes:");
        for (String attr : config.getRequiredAttributes()) {
            ContractCreationConfig.AttributeConfig attrConfig = config.getAttributeConfig(attr);
            System.out.println("  - " + attrConfig.getDisplayName() + " (" + attrConfig.getDataType() + ")");
        }
        
        // Test optional attributes
        System.out.println("\nOptional Attributes:");
        for (String attr : config.getOptionalAttributes()) {
            ContractCreationConfig.AttributeConfig attrConfig = config.getAttributeConfig(attr);
            System.out.println("  - " + attrConfig.getDisplayName() + " (" + attrConfig.getDataType() + ")");
        }
        
        // Test prompts
        System.out.println("\nAttribute Prompts:");
        for (String attr : config.getRequiredAttributes()) {
            System.out.println("  " + attr + ": " + config.getAttributePrompt(attr));
        }
    }
    
    /**
     * Test the workflow logic
     */
    private static void testWorkflowLogic() {
        System.out.println("TEST 2: WORKFLOW LOGIC");
        System.out.println("=======================");
        
        ContractCreationConfig config = ContractCreationConfig.getInstance();
        
        // Simulate a contract creation workflow
        String sessionId = "demo_session_1";
        String[] userInputs = {
            "create contract",
            "12345",
            "ABC Aerospace Contract",
            "Aerospace Parts Supply Agreement",
            "Long-term supply agreement for aerospace parts and components",
            "01-01-2024",
            "12-31-2026"
        };
        
        System.out.println("Simulating Contract Creation Workflow:");
        System.out.println("=====================================");
        
        for (int i = 0; i < userInputs.length; i++) {
            String input = userInputs[i];
            System.out.println("\nStep " + (i + 1) + ":");
            System.out.println("User Input: " + input);
            
            if (i == 0) {
                // Initial request
                System.out.println("Bot Response: " + config.getAttributePrompt(config.getRequiredAttributes().get(0)));
            } else {
                // Follow-up inputs
                String currentStep = config.getRequiredAttributes().get(i - 1);
                String validationResult = config.validateAttributeInput(currentStep, input);
                
                if (validationResult != null) {
                    System.out.println("Bot Response: " + validationResult);
                } else {
                    String extractedValue = config.extractAttributeValue(currentStep, input);
                    System.out.println("Extracted Value: " + extractedValue);
                    
                    if (i < userInputs.length - 1) {
                        String nextStep = config.getRequiredAttributes().get(i);
                        System.out.println("Bot Response: " + config.getAttributePrompt(nextStep));
                    } else {
                        System.out.println("Bot Response: Contract creation data complete. Processing contract creation...");
                    }
                }
            }
        }
    }
    
    /**
     * Test validation scenarios
     */
    private static void testValidationScenarios() {
        System.out.println("TEST 3: VALIDATION SCENARIOS");
        System.out.println("============================");
        
        ContractCreationConfig config = ContractCreationConfig.getInstance();
        
        // Test validation scenarios
        String[][] testCases = {
            {"CUSTOMER_NUMBER", "123", "Invalid customer number (too short)"},
            {"CUSTOMER_NUMBER", "12345", "Valid customer number"},
            {"CUSTOMER_NUMBER", "123456789", "Invalid customer number (too long)"},
            {"CONTRACT_NAME", "", "Empty contract name"},
            {"CONTRACT_NAME", "AB", "Contract name too short"},
            {"CONTRACT_NAME", "Valid Contract Name", "Valid contract name"},
            {"EFFECTIVE_DATE", "2024-01-01", "Invalid date format"},
            {"EFFECTIVE_DATE", "01-01-2024", "Valid date format"},
            {"CONTRACT_TYPE", "INVALID", "Invalid contract type"},
            {"CONTRACT_TYPE", "SERVICE", "Valid contract type"},
            {"CURRENCY", "USD", "Valid currency"},
            {"CURRENCY", "INVALID", "Invalid currency"}
        };
        
        System.out.println("Validation Test Cases:");
        System.out.println("=====================");
        
        for (String[] testCase : testCases) {
            String attribute = testCase[0];
            String input = testCase[1];
            String expected = testCase[2];
            
            String validationResult = config.validateAttributeInput(attribute, input);
            String result = validationResult == null ? "VALID" : "INVALID: " + validationResult;
            
            System.out.println("  " + attribute + " = \"" + input + "\" -> " + result);
        }
        
        // Test value extraction
        System.out.println("\nValue Extraction Test Cases:");
        System.out.println("============================");
        
        String[][] extractionTests = {
            {"CUSTOMER_NUMBER", "Customer 12345 account", "12345"},
            {"EFFECTIVE_DATE", "Effective from 01-01-2024", "01-01-2024"},
            {"CONTRACT_TYPE", "service", "SERVICE"},
            {"CURRENCY", "usd", "USD"},
            {"CONTRACT_NAME", "  ABC Contract  ", "ABC Contract"}
        };
        
        for (String[] testCase : extractionTests) {
            String attribute = testCase[0];
            String input = testCase[1];
            String expected = testCase[2];
            
            String extractedValue = config.extractAttributeValue(attribute, input);
            System.out.println("  " + attribute + " = \"" + input + "\" -> \"" + extractedValue + "\"");
        }
    }
    
    /**
     * Test chain break detection
     */
    private static void testChainBreakDetection() {
        System.out.println("TEST 4: CHAIN BREAK DETECTION");
        System.out.println("=============================");
        
        String[] contractCreationInputs = {
            "create contract",
            "new contract",
            "start contract",
            "I want to create a contract",
            "help me create a contract",
            "begin contract creation",
            "start new contract",
            "create a new contract"
        };
        
        String[] nonContractInputs = {
            "show contract 100476",
            "what is the lead time for EN6114V4-13",
            "help",
            "show me contracts",
            "list all contracts",
            "contract information"
        };
        
        System.out.println("Contract Creation Inputs (should return true):");
        System.out.println("=============================================");
        for (String input : contractCreationInputs) {
            boolean isRelated = isContractCreationRelated(input);
            System.out.println("  \"" + input + "\" -> " + isRelated);
        }
        
        System.out.println("\nNon-Contract Inputs (should return false):");
        System.out.println("==========================================");
        for (String input : nonContractInputs) {
            boolean isRelated = isContractCreationRelated(input);
            System.out.println("  \"" + input + "\" -> " + isRelated);
        }
    }
    
    /**
     * Check if input is related to contract creation
     */
    private static boolean isContractCreationRelated(String userInput) {
        if (userInput == null) return false;
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("create") && lowerInput.contains("contract") ||
               lowerInput.contains("new contract") ||
               lowerInput.contains("start contract") ||
               lowerInput.matches(".*\\b(create|new|start)\\b.*\\bcontract\\b.*");
    }
} 