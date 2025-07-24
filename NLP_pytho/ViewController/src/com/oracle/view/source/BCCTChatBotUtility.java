package com.oracle.view.source;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCIteratorBinding;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

public class BCCTChatBotUtility {

    // ========================================
    // STATIC PREDEFINED PROMPTS FOR COMMAND BUTTONS
    // These prompts are fixed and cannot be modified by users
    // ========================================

    /**
     * Static predefined prompts for each command button
     * These are used by NLPQueryClassifier to identify quick actions
     * and cannot be modified by users to ensure security and consistency
     */
    public static final String QUICK_ACTION_RECENT_CONTRACTS_PROMPT = "show me contracts created in the last 24 hours";
    public static final String QUICK_ACTION_PARTS_COUNT_PROMPT =
        "what is the total count of parts loaded in the system";
    public static final String QUICK_ACTION_FAILED_CONTRACTS_PROMPT =
        "show me contracts with failed parts and their counts";
    public static final String QUICK_ACTION_EXPIRING_SOON_PROMPT = "show me contracts expiring in the next 30 days";
    public static final String QUICK_ACTION_AWARD_REPS_PROMPT =
        "list all award representatives and their contract counts";
    public static final String QUICK_ACTION_HELP_PROMPT = "show me help and available features";
    public static final String QUICK_ACTION_CREATE_CONTRACT_PROMPT = "show me the steps to create a contract";

    /**
     * Array of all quick action prompts for easy iteration
     */
    public static final String[] ALL_QUICK_ACTION_PROMPTS = {
        QUICK_ACTION_RECENT_CONTRACTS_PROMPT, QUICK_ACTION_PARTS_COUNT_PROMPT, QUICK_ACTION_FAILED_CONTRACTS_PROMPT,
        QUICK_ACTION_EXPIRING_SOON_PROMPT, QUICK_ACTION_AWARD_REPS_PROMPT, QUICK_ACTION_HELP_PROMPT,
        QUICK_ACTION_CREATE_CONTRACT_PROMPT
    };

    /**
     * Map of prompts to their corresponding action types
     */
    public static final Map<String, String> PROMPT_TO_ACTION_TYPE_MAP = new HashMap<String, String>() {
        {
            put(QUICK_ACTION_RECENT_CONTRACTS_PROMPT, "QUICK_ACTION_RECENT_CONTRACTS");
            put(QUICK_ACTION_PARTS_COUNT_PROMPT, "QUICK_ACTION_PARTS_COUNT");
            put(QUICK_ACTION_FAILED_CONTRACTS_PROMPT, "QUICK_ACTION_FAILED_CONTRACTS");
            put(QUICK_ACTION_EXPIRING_SOON_PROMPT, "QUICK_ACTION_EXPIRING_SOON");
            put(QUICK_ACTION_AWARD_REPS_PROMPT, "QUICK_ACTION_AWARD_REPS");
            put(QUICK_ACTION_HELP_PROMPT, "QUICK_ACTION_HELP");
            put(QUICK_ACTION_CREATE_CONTRACT_PROMPT, "QUICK_ACTION_CREATE_CONTRACT");
        }
    };

    /**
     * Get the predefined prompt for a specific quick action
     * @param actionType The quick action type (e.g., "QUICK_ACTION_RECENT_CONTRACTS")
     * @return The corresponding predefined prompt
     */
    public static String getPromptForActionType(String actionType) {
        for (Map.Entry<String, String> entry : PROMPT_TO_ACTION_TYPE_MAP.entrySet()) {
            if (entry.getValue().equals(actionType)) {
                return entry.getKey();
            }
        }
        return QUICK_ACTION_HELP_PROMPT; // Default to help
    }

    /**
     * Get the action type for a given prompt
     * @param prompt The predefined prompt
     * @return The corresponding action type
     */
    public static String getActionTypeForPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "QUICK_ACTION_HELP";
        }
        String input = prompt.trim();
        for (Map.Entry<String, String> entry : PROMPT_TO_ACTION_TYPE_MAP.entrySet()) {
            if (input.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "QUICK_ACTION_HELP"; // Default to help if no exact match
    }

    /**
     * Check if a given input matches any predefined prompt
     * @param userInput The user input to check
     * @return true if it matches a predefined prompt, false otherwise
     */
    public static boolean isPredefinedPrompt(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        String input = userInput.trim();
        for (String prompt : ALL_QUICK_ACTION_PROMPTS) {
            if (input.equalsIgnoreCase(prompt)) {
                return true;
            }
        }
        return false;
    }

    // ========================================
    // END STATIC PREDEFINED PROMPTS
    // ========================================

    public BCCTChatBotUtility() {
        super();
    }

    public static String truncateString(String str, int maxLength) {
        if (str == null)
            return "N/A";
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    public static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String escapeHtml(String input) {
        if (input == null)
            return "N/A";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    public static OperationBinding findOperationBinding(String operationName) {
        return getDCBindingContainer().getOperationBinding(operationName);
    }

    public static DCBindingContainer getDCBindingContainer() {
        return (DCBindingContainer) getBindingContainer();
    }

    public static BindingContainer getBindingContainer() {
        return (BindingContainer) resolveExpression("#{bindings}");
    }

    public static Object resolveExpression(String expression) {
        FacesContext facesContext = getFacesContext();
        Application app = facesContext.getApplication();
        ExpressionFactory elFactory = app.getExpressionFactory();
        ELContext elContext = facesContext.getELContext();
        ValueExpression valueExp = elFactory.createValueExpression(elContext, expression, Object.class);
        return valueExp.getValue(elContext);
    }

    public static FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    public static DCIteratorBinding findIterator(String name) {
        DCIteratorBinding iter = getDCBindingContainer().findIteratorBinding(name);
        if (iter == null) {
            throw new RuntimeException("Iterator '" + name + "' not found");
        }
        return iter;
    }


    public static String getHelpContent(String actionType) {
        switch (actionType) {
        case "HELP_CONTRACT_CREATE_USER":
            return "<h4>Manual Contract Creation</h4>" +
                   "<p>To create a contract manually, please follow these steps:</p>" + "<ol>" +
                   "<li><b>Login to BCCT Application:</b> You will land on the Opportunities screen</li>" +
                   "<li><b>Navigate to Award Management:</b> Click on the 'Award Management' link</li>" +
                   "<li><b>Access Award Management Dashboard:</b> You will be taken to the Award Management Dashboard</li>" +
                   "<li><b>Select Implementation:</b> Click on any area of the Award Implementation Pie Chart or click on the Total Implementation number</li>" +
                   "<li><b>Navigate to Awards Result:</b> You will be taken to the Awards Result screen</li>" +
                   "<li><b>Create Award:</b> Click on the 'Create Award' button</li>" +
                   "<li><b>Fill Contract Details:</b> You will be taken to the Contract Creation/Award screen where you can fill the following data:" +
                   "<ul>" + "<li>Account Number (6+ digits)</li>" + "<li>Contract Name</li>" + "<li>Title</li>" +
                   "<li>Description</li>" + "<li>Comments (optional)</li>" + "<li>Price List Contract (Yes/No)</li>" +
                   "</ul></li>" + "<li><b>Save the Data:</b> Review all details and save the contract</li>" + "</ol>" +
                   "<p><b>Required Fields:</b></p>" + "<ul>" + "<li>Account Number (mandatory)</li>" +
                   "<li>Contract Name</li>" + "<li>Title</li>" + "<li>Description</li>" + "</ul>" +
                   "<p><i>Tip: For automated assistance, use the comma-separated format: '123456789, contractname, title, description, comments, no'</i></p>";
        case "HELP_CONTRACT_CREATE_BOT_ACCOUNT":
            return "<div style='padding: 20px; border: 2px solid #2c5aa0; border-radius: 8px; background: linear-gradient(135deg, #f8f9ff 0%, #e8f2ff 100%); box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                   "<h3 style='color: #2c5aa0; margin-top: 0; font-weight: bold; border-bottom: 2px solid #2c5aa0; padding-bottom: 8px;'>Contract Assistant</h3>" +

                   "<p style='font-size: 16px; color: #333; margin-bottom: 15px;'>Welcome! I'm here to help you create contracts efficiently. Please provide your contract information using any of the formats below:</p>" +

                   "<div style='background-color: #ffffff; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #28a745;'>" +
                   "<h4 style='color: #28a745; margin-top: 0;'>Required Contract Information:</h4>" +
                   "<ul style='margin: 10px 0; padding-left: 20px;'>" + "<li><strong>Contract Name</strong></li>" +
                   "<li><strong>Customer Number and Name</strong></li>" + "<li><strong>Effective Date</strong></li>" +
                   "<li><strong>Expiration Date</strong></li>" + "<li><strong>Price Expiration Date</strong></li>" +
                   "<li><strong>Award Representative</strong></li>" + "</ul>" +
                   "</div>" +

                   "<div style='background-color: #fff3cd; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #ffc107;'>" +
                   "<h4 style='color: #856404; margin-top: 0;'>Supported Input Formats:</h4>" +
                   "<p style='margin: 5px 0;'><strong>Label = Value:</strong> <tt>contract number = GSA-123456, effective date = 01/15/24</tt></p>" +
                   "<p style='margin: 5px 0;'><strong>Label: Value:</strong> <tt>contract name: IT Services, customer: 12345 - Tech Corp</tt></p>" +
                   "<p style='margin: 5px 0;'><strong>Comma Separated:</strong> <tt>GSA-123456, IT Services Contract, 12345 - Tech Corp, 01/15/24</tt></p>" +
                   "</div>" +

                   "<div style='background-color: #d1ecf1; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #17a2b8;'>" +
                   "<h4 style='color: #0c5460; margin-top: 0;'>Example Input:</h4>" +
                   "<pre style='background-color: #ffffff; padding: 10px; border-radius: 4px; font-size: 14px; overflow-x: auto;'>" +
                   "contract number: GSA-47QSWA19D0024\n" + "contract name: Professional IT Services\n" +
                   "customer: 12345 - Department of Defense\n" + "effective date: 01/15/2024\n" +
                   "expiration date: 12/31/2026\n" + "price expiration: 06/30/2025\n" + "award rep: Sarah Johnson" +
                   "</pre>" +
                   "</div>" +

                   "<div style='background-color: #d4edda; padding: 12px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #28a745;'>" +
                   "<p style='margin: 0; color: #155724;'><strong>? Ready to assist:</strong> Simply paste your contract details in any supported format above, and I'll process and create your contract automatically!</p>" + "</div>" +

                   "</div>";

        default:
            return "<div style='padding: 15px; border: 1px solid #ffc107; border-radius: 5px; background-color: #fff3cd;'>" +
                   "<h4 style='color: #856404; margin-top: 0;'>Help Available</h4>" + "<p>I can help you with:</p>" +
                   "<ul>" + "<li>Contract queries and information</li>" + "<li>Parts information and details</li>" +
                   "<li>Failed parts analysis</li>" + "<li>Contract creation guidance</li>" + "</ul>" +
                   "<p>Please ask me specific questions about contracts or parts.</p>" + "</div>";
        }
    }

    public static String createCompleteResponseJSONWithHelpContent(Object queryResult, String helpContent) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"success\": true,\n");
        json.append("  \"message\": \"Help request processed successfully\",\n");
        json.append("  \"isHelpQuery\": true,\n");
        json.append("  \"nlpResponse\": {},\n");
        json.append("  \"helpContent\": \"")
            .append(escapeJson(helpContent))
            .append("\"\n");
        json.append("}");
        return json.toString();
    }

    public static String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    public static String extractDataProviderResponseFromJson(String jsonResponse) {
        try {
            String searchKey = "\"dataProviderResponse\": \"";
            int startIndex = jsonResponse.indexOf(searchKey);
            if (startIndex == -1) {
                return "<div style='color: orange;'>No data content found in response</div>";
            }
            startIndex += searchKey.length();
            int endIndex = findEndOfJsonStringValue(jsonResponse, startIndex);
            if (endIndex == -1) {
                return "<div style='color: red;'>Malformed JSON response</div>";
            }
            String htmlContent = jsonResponse.substring(startIndex, endIndex);
            return unescapeJsonString(htmlContent);
        } catch (Exception e) {
            return "<div style='color: red;'>Error extracting data content: " + e.getMessage() + "</div>";
        }
    }

    public static String extractHelpContentFromJson(String jsonResponse) {
        try {
            String searchKey = "\"helpContent\": \"";
            int startIndex = jsonResponse.indexOf(searchKey);
            if (startIndex == -1) {
                return "<div style='color: orange;'>No help content found in response</div>";
            }
            startIndex += searchKey.length();
            int endIndex = findEndOfJsonStringValue(jsonResponse, startIndex);
            if (endIndex == -1) {
                return "<div style='color: red;'>Malformed JSON response</div>";
            }
            String htmlContent = jsonResponse.substring(startIndex, endIndex);
            return unescapeJsonString(htmlContent);
        } catch (Exception e) {
            return "<div style='color: red;'>Error extracting help content: " + e.getMessage() + "</div>";
        }
    }

    public static int findEndOfJsonStringValue(String json, int startIndex) {
        boolean escape = false;
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && !escape) {
                escape = true;
            } else if (c == '"' && !escape) {
                return i;
            } else {
                escape = false;
            }
        }
        return -1;
    }

    public static String unescapeJsonString(String escapedString) {
        if (escapedString == null)
            return null;
        return escapedString.replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\r", "\r")
                            .replace("\\t", "\t")
                            .replace("\\\\", "\\");
    }

    public static String extractHtmlContentFromJsonResponse(String jsonResponse) {
        String dataContent = extractDataProviderResponseFromJson(jsonResponse);
        if (dataContent != null && !dataContent.contains("Error")) {
            return dataContent;
        }
        String helpContent = extractHelpContentFromJson(jsonResponse);
        if (helpContent != null && !helpContent.contains("Error")) {
            return helpContent;
        }
        return "<div style='color: orange;'>No HTML content found in response</div>";
    }

    public static void printChatbotResponse(ConversationalNLPManager.ChatbotResponse response) {
        if (response == null) {
            System.out.println("ChatbotResponse: null");
            return;
        }
        System.out.println("--- ConversationalNLPManager.ChatbotResponse ---");
        System.out.println("isSuccess: " + response.isSuccess);
        System.out.println("dataProviderResponse: " + response.dataProviderResponse);
        System.out.println("data: " + response.data);
        if (response.inputTracking != null) {
            System.out.println("inputTracking.originalInput: " + response.inputTracking.originalInput);
            System.out.println("inputTracking.correctedInput: " + response.inputTracking.correctedInput);
            System.out.println("inputTracking.correctionConfidence: " + response.inputTracking.correctionConfidence);
        } else {
            System.out.println("inputTracking: null");
        }
        if (response.metadata != null) {
            System.out.println("metadata.queryType: " + response.metadata.queryType);
            System.out.println("metadata.actionType: " + response.metadata.actionType);
            System.out.println("metadata.processingTimeMs: " + response.metadata.processingTimeMs);
        } else {
            System.out.println("metadata: null");
        }
        if (response.entities != null) {
            System.out.println("entities:");
            for (ConversationalNLPManager.EntityFilter e : response.entities) {
                System.out.println("  attribute: " + e.attribute + ", operation: " + e.operation + ", value: " +
                                   e.value + ", source: " + e.source);
            }
        } else {
            System.out.println("entities: null");
        }
        if (response.displayEntities != null) {
            System.out.println("displayEntities: " + response.displayEntities);
        } else {
            System.out.println("displayEntities: null");
        }
        if (response.errors != null) {
            System.out.println("errors:");
            for (ConversationalNLPManager.ValidationError err : response.errors) {
                System.out.println("  code: " + err.code + ", message: " + err.message + ", severity: " + err.severity);
            }
        } else {
            System.out.println("errors: null");
        }
        System.out.println("----------------------------------------------");
    }

    public static void printQueryResult(NLPEntityProcessor.QueryResult queryResult) {
        if (queryResult == null) {
            System.out.println("QueryResult: null");
            return;
        }
        System.out.println("--- NLPEntityProcessor.QueryResult ---");
        if (queryResult.inputTracking != null) {
            System.out.println("inputTracking.originalInput: " + queryResult.inputTracking.originalInput);
            System.out.println("inputTracking.correctedInput: " + queryResult.inputTracking.correctedInput);
            System.out.println("inputTracking.correctionConfidence: " + queryResult.inputTracking.correctionConfidence);
        } else {
            System.out.println("inputTracking: null");
        }
        if (queryResult.header != null) {
            System.out.println("header.contractNumber: " + queryResult.header.contractNumber);
            System.out.println("header.partNumber: " + queryResult.header.partNumber);
            System.out.println("header.customerNumber: " + queryResult.header.customerNumber);
            System.out.println("header.customerName: " + queryResult.header.customerName);
            System.out.println("header.createdBy: " + queryResult.header.createdBy);
        } else {
            System.out.println("header: null");
        }
        if (queryResult.metadata != null) {
            System.out.println("metadata.queryType: " + queryResult.metadata.queryType);
            System.out.println("metadata.actionType: " + queryResult.metadata.actionType);
            System.out.println("metadata.processingTimeMs: " + queryResult.metadata.processingTimeMs);
        } else {
            System.out.println("metadata: null");
        }
        if (queryResult.entities != null) {
            System.out.println("entities:");
            for (NLPEntityProcessor.EntityFilter e : queryResult.entities) {
                System.out.println("  attribute: " + e.attribute + ", operation: " + e.operation + ", value: " +
                                   e.value + ", source: " + e.source);
            }
        } else {
            System.out.println("entities: null");
        }
        if (queryResult.displayEntities != null) {
            System.out.println("displayEntities: " + queryResult.displayEntities);
        } else {
            System.out.println("displayEntities: null");
        }
        if (queryResult.errors != null) {
            System.out.println("errors:");
            for (NLPEntityProcessor.ValidationError err : queryResult.errors) {
                System.out.println("  code: " + err.code + ", message: " + err.message + ", severity: " + err.severity);
            }
        } else {
            System.out.println("errors: null");
        }
        System.out.println("--------------------------------------");
    }

    public static NLPEntityProcessor.QueryResult convertMyObjects(ConversationalNLPManager.ChatbotResponse response) {
        printChatbotResponse(response);
        if (response == null) {
            return null;
        }
        NLPEntityProcessor.QueryResult queryResult = new NLPEntityProcessor.QueryResult();
        // Convert InputTrackingInfo to InputTrackingResult
        if (response.inputTracking != null) {
            queryResult.inputTracking =
                new NLPEntityProcessor.InputTrackingResult(response.inputTracking.originalInput,
                                                           response.inputTracking.correctedInput,
                                                           response.inputTracking.correctionConfidence);
        }
        // Convert ResponseMetadata to QueryMetadata
        if (response.metadata != null) {
            queryResult.metadata =
                new NLPEntityProcessor.QueryMetadata(response.metadata.queryType, response.metadata.actionType,
                                                     response.metadata.processingTimeMs);
        }
        // Convert EntityFilter list
        if (response.entities != null) {
            for (ConversationalNLPManager.EntityFilter sourceFilter : response.entities) {
                NLPEntityProcessor.EntityFilter targetFilter =
                    new NLPEntityProcessor.EntityFilter(sourceFilter.attribute, sourceFilter.operation,
                                                        sourceFilter.value, sourceFilter.source);
                queryResult.entities.add(targetFilter);
            }
        }
        // Convert display entities
        if (response.displayEntities != null) {
            queryResult.displayEntities.addAll(response.displayEntities);
        }
        // Convert ValidationError list
        if (response.errors != null) {
            for (ConversationalNLPManager.ValidationError sourceError : response.errors) {
                NLPEntityProcessor.ValidationError targetError =
                    new NLPEntityProcessor.ValidationError(sourceError.code, sourceError.message, sourceError.severity);
                queryResult.errors.add(targetError);
            }
        }
        // Note: Header is not present in ChatbotResponse, so it remains null
        // This is expected as ChatbotResponse focuses on conversational flow rather than entity extraction
        printQueryResult(queryResult);
        return queryResult;
    }

    /**
     * Extract only the dataProviderResponse from a complete JSON response
     * @param completeJsonResponse The full JSON response string
     * @return Only the dataProviderResponse content, or null if not found
     */
    public static String extractDataProviderResponse(String completeJsonResponse) {
        if (completeJsonResponse == null || completeJsonResponse.trim().isEmpty()) {
            return null;
        }

        try {
            // Find the start of dataProviderResponse
            int startIndex = completeJsonResponse.indexOf("\"dataProviderResponse\":");
            if (startIndex == -1) {
                return null;
            }

            // Move past the key and opening quote
            startIndex = completeJsonResponse.indexOf("\"", startIndex + 22); // 22 is length of "dataProviderResponse":
            if (startIndex == -1) {
                return null;
            }
            startIndex++; // Move past the opening quote

            // Find the closing quote (handle escaped quotes)
            int endIndex = startIndex;
            boolean inEscapedQuote = false;

            while (endIndex < completeJsonResponse.length()) {
                char currentChar = completeJsonResponse.charAt(endIndex);

                if (currentChar == '\\' && endIndex + 1 < completeJsonResponse.length()) {
                    // Skip escaped character
                    endIndex += 2;
                    continue;
                }

                if (currentChar == '"' && !inEscapedQuote) {
                    break;
                }

                endIndex++;
            }

            if (endIndex > startIndex) {
                String extractedResponse = completeJsonResponse.substring(startIndex, endIndex);
                // Unescape common JSON escape sequences
                return unescapeJsonString(extractedResponse);
            }

        } catch (Exception e) {
            System.err.println("Error extracting dataProviderResponse: " + e.getMessage());
        }

        return null;
    }

    /**
     * Print all data from NLPQueryClassifier.QueryResult object for debugging
     */
    public static void printNLPQueryClassifierResult(NLPQueryClassifier.QueryResult queryResult) {
        if (queryResult == null) {
            System.out.println("=== NLPQueryClassifier.QueryResult: null ===");
            return;
        }

        System.out.println("=== NLPQueryClassifier.QueryResult Details ===");

        // Print InputTracking information
        if (queryResult.inputTracking != null) {
            System.out.println("�? InputTracking:");
            System.out.println("   Original Input: " + queryResult.inputTracking.originalInput);
            System.out.println("   Corrected Input: " + queryResult.inputTracking.correctedInput);
            System.out.println("   Confidence Score: " + queryResult.inputTracking.correctionConfidence);
        } else {
            System.out.println("�? InputTracking: null");
        }

        // Print QueryMetadata information
        if (queryResult.metadata != null) {
            System.out.println("=== QueryMetadata:");
            System.out.println("   Query Type: " + queryResult.metadata.queryType);
            System.out.println("   Action Type: " + queryResult.metadata.actionType);
            System.out.println("   Processing Time (ms): " + queryResult.metadata.processingTimeMs);
        } else {
            System.out.println("=== QueryMetadata: null");
        }

        // Print Header information
        if (queryResult.header != null) {
            System.out.println("=== Header:");
            System.out.println("   Contract Number: " + queryResult.header.contractNumber);
            System.out.println("   Part Number: " + queryResult.header.partNumber);
            System.out.println("   Customer Number: " + queryResult.header.customerNumber);
            System.out.println("   Customer Name: " + queryResult.header.customerName);
            System.out.println("   Created By: " + queryResult.header.createdBy);
        } else {
            System.out.println("=== Header: null");
        }

        // Print Entity Filters
        if (queryResult.entities != null && !queryResult.entities.isEmpty()) {
            System.out.println("�? Entity Filters (" + queryResult.entities.size() + " items):");
            for (int i = 0; i < queryResult.entities.size(); i++) {
                NLPQueryClassifier.EntityFilter filter = queryResult.entities.get(i);
                System.out.println("   [" + (i + 1) + "] Attribute: " + filter.attribute);
                System.out.println("       Operation: " + filter.operation);
                System.out.println("       Value: " + filter.value);
                System.out.println("       Source: " + filter.source);
            }
        } else {
            System.out.println("�? Entity Filters: null or empty");
        }

        // Print Display Entities
        if (queryResult.displayEntities != null && !queryResult.displayEntities.isEmpty()) {
            System.out.println("=== Display Entities (" + queryResult.displayEntities.size() + " items):");
            for (int i = 0; i < queryResult.displayEntities.size(); i++) {
                System.out.println("   [" + (i + 1) + "] " + queryResult.displayEntities.get(i));
            }
        } else {
            System.out.println("=== Display Entities: null or empty");
        }

        // Print Validation Errors
        if (queryResult.errors != null && !queryResult.errors.isEmpty()) {
            System.out.println("�?� Validation Errors (" + queryResult.errors.size() + " items):");
            for (int i = 0; i < queryResult.errors.size(); i++) {
                NLPQueryClassifier.ValidationError error = queryResult.errors.get(i);
                System.out.println("   [" + (i + 1) + "] Error Type: " + error.message);
                System.out.println("       Message: " + error.message);
                System.out.println("       Field: " + error.code);
            }
        } else {
            System.out.println("�?� Validation Errors: null or empty");
        }

        // Print any additional fields if they exist
        if (queryResult.displayEntities != null && queryResult.displayEntities.size() > 0) {
            System.out.println("=== Summary:");
            System.out.println("   Total Entities: " +
                               (queryResult.entities != null ? queryResult.entities.size() : 0));
            System.out.println("   Total Display Entities: " + queryResult.displayEntities.size());
            System.out.println("   Total Errors: " + (queryResult.errors != null ? queryResult.errors.size() : 0));
        }

        System.out.println("=== End NLPQueryClassifier.QueryResult Details ===");
    }

    /**
     * Generic method to build SQL query based on filters
     * @param filters List of EntityFilter objects
     * @return SQL query string
     */
    public static String buildContractQuery(List<NLPEntityProcessor.EntityFilter> filters) {
        StringBuilder query = new StringBuilder();

        // Base SELECT clause
        query.append("SELECT contracts.AWARD_NUMBER, contracts_Contacts.award_rep, ");
        query.append("contracts.EFFECTIVE_DATE, contracts.EXPIRATION_DATE, contracts.CREATE_DATE ");

        // FROM clause
        query.append(" FROM ")
             .append(TableColumnConfig.TABLE_CONTRACT_CONTACTS)
             .append(", ");
        query.append(TableColumnConfig.TABLE_CONTRACTS).append(" ");

        // WHERE clause with join condition
        query.append("WHERE contracts_Contacts.AWARD_NUMBER = contracts.AWARD_NUMBER");

        // Add filter conditions
        if (filters != null && !filters.isEmpty()) {
            for (NLPEntityProcessor.EntityFilter filter : filters) {
                query.append(" AND ");

                String fullAttribute =
                    filter.source != null && !filter.source.isEmpty() ? filter.source + "." + filter.attribute :
                    filter.attribute;

                switch (filter.operation.toUpperCase()) {
                case "EQUALS":
                case "=":
                    query.append(fullAttribute)
                         .append(" = ")
                         .append(formatValue(filter.value, filter.attribute));
                    break;
                case "LIKE":
                    query.append("UPPER(")
                         .append(fullAttribute)
                         .append(") LIKE UPPER(")
                         .append(formatValue("%" + filter.value + "%", filter.attribute))
                         .append(")");
                    break;
                case "IN_YEAR":
                    query.append("EXTRACT(YEAR FROM ")
                         .append(fullAttribute)
                         .append(") = ")
                         .append(filter.value);
                    break;
                case "AFTER_YEAR":
                    query.append("EXTRACT(YEAR FROM ")
                         .append(fullAttribute)
                         .append(") > ")
                         .append(filter.value);
                    break;
                case "DATE_RANGE":
                    String[] dates = filter.value.split(",");
                    if (dates.length == 2) {
                        query.append(fullAttribute)
                             .append(" >= DATE '")
                             .append(dates[0].trim())
                             .append("'");
                        query.append(" AND ").append(fullAttribute);
                        if ("SYSDATE".equalsIgnoreCase(dates[1].trim())) {
                            query.append(" <= SYSDATE");
                        } else {
                            query.append(" <= DATE '")
                                 .append(dates[1].trim())
                                 .append("'");
                        }
                    }
                    break;
                case "GREATER_THAN":
                    query.append(fullAttribute)
                         .append(" > ")
                         .append(formatValue(filter.value, filter.attribute));
                    break;
                case "LESS_THAN":
                    query.append(fullAttribute)
                         .append(" < ")
                         .append(formatValue(filter.value, filter.attribute));
                    break;
                case "GREATER_EQUAL":
                    query.append(fullAttribute)
                         .append(" >= ")
                         .append(formatValue(filter.value, filter.attribute));
                    break;
                case "LESS_EQUAL":
                    query.append(fullAttribute)
                         .append(" <= ")
                         .append(formatValue(filter.value, filter.attribute));
                    break;
                default:
                    query.append(fullAttribute)
                         .append(" = ")
                         .append(formatValue(filter.value, filter.attribute));
                    break;
                }
            }
        }

        // ORDER BY clause
        query.append(" ORDER BY contracts.CREATE_DATE DESC");

        return query.toString();
    }

    private static String formatValue(String value, String attribute) {
        if (value == null)
            return "NULL";

        if (attribute.toUpperCase().contains("DATE") || attribute.toUpperCase().contains("TIME")) {
            if ("SYSDATE".equalsIgnoreCase(value)) {
                return "SYSDATE";
            } else if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return "DATE '" + value + "'";
            }
        }

        if (value.matches("^-?\\d+(\\.\\d+)?$")) {
            return value;
        }

        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * Print complete ConversationSession object details
     * @param session The ConversationSession object to print
     */
    public static void printConversationSession(ConversationSession session) {
        if (session == null) {
            System.out.println("=== CONVERSATION SESSION DEBUG ===");
            System.out.println("Session is NULL");
            System.out.println("=== END CONVERSATION SESSION DEBUG ===");
            return;
        }

        System.out.println("=== CONVERSATION SESSION DEBUG ===");
        System.out.println("Session ID: " + session.getSessionId());
        System.out.println("User ID: " + session.getUserId());
        System.out.println("Creation Time: " + new java.util.Date(session.getCreationTime()));
        System.out.println("Last Activity: " + new java.util.Date(session.getLastActivityTime()));
        System.out.println("Session State: " + session.getState());
        System.out.println("Current Flow Type: " + session.getCurrentFlowType());
        System.out.println("Is Waiting For Input: " + session.isWaitingForUserInput());
        System.out.println("Is Completed: " + session.isCompleted());

        // Print collected data
        System.out.println("\nCOLLECTED DATA:");
        Map<String, Object> collectedData = session.getCollectedData();
        if (collectedData.isEmpty()) {
            System.out.println("   (No data collected)");
        } else {
            for (Map.Entry<String, Object> entry : collectedData.entrySet()) {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue());
            }
        }

        // Print expected fields
        System.out.println("\nEXPECTED FIELDS:");
        List<String> remainingFields = session.getRemainingFields();
        if (remainingFields.isEmpty()) {
            System.out.println("   (All fields completed)");
        } else {
            for (String field : remainingFields) {
                System.out.println("   - " + field);
            }
        }

        // Print user search results
        System.out.println("\nUSER SEARCH RESULTS:");
        Map<String, List<Map<String, String>>> userResults = session.getUserSearchResults();
        if (userResults.isEmpty()) {
            System.out.println("   (No user search results)");
        } else {
            System.out.println("   Valid: " + session.isUserSearchValid());
            System.out.println("   Available Users: " + session.getAvailableUsers());
            for (Map.Entry<String, List<Map<String, String>>> entry : userResults.entrySet()) {
                System.out.println("   User: " + entry.getKey() + " -> " + entry.getValue().size() + " contracts");
            }
        }

        // Print contract search results
        System.out.println("\nCONTRACT SEARCH RESULTS:");
        Map<String, List<Map<String, Object>>> contractResults = session.getContractSearchResults();
        if (contractResults.isEmpty()) {
            System.out.println("   (No contract search results)");
        } else {
            System.out.println("   Valid: " + session.isContractSearchValid());
            System.out.println("   Display Order: " + session.getContractDisplayOrder());
            for (Map.Entry<String, List<Map<String, Object>>> entry : contractResults.entrySet()) {
                System.out.println("   Key: " + entry.getKey() + " -> " + entry.getValue().size() + " items");
            }
        }

        // Print conversation history
        System.out.println("\nCONVERSATION HISTORY:");
        List<ConversationSession.ConversationTurn> history = session.getConversationHistory();
        if (history.isEmpty()) {
            System.out.println("   (No conversation history)");
        } else {
            for (int i = 0; i < history.size(); i++) {
                ConversationSession.ConversationTurn turn = history.get(i);
                System.out.println("   " + (i + 1) + ". [" + turn.speaker + "] " + new java.util.Date(turn.timestamp) +
                                   ": " + truncateString(turn.message, 100));
            }
        }

        // Print validation results
        System.out.println("\nVALIDATION RESULTS:");
        // Note: validationResults is private, so we can't access it directly
        // This would need to be exposed through a getter method

        System.out.println("=== END CONVERSATION SESSION DEBUG ===");
    }

    /**
     * Print ConversationSession object in a compact format
     * @param session The ConversationSession object to print
     */
    public static void printConversationSessionCompact(ConversationSession session) {
        if (session == null) {
            System.out.println("Session: NULL");
            return;
        }

        System.out.println("Session: " + session.getSessionId() + " | State: " + session.getState() + " | Flow: " +
                           session.getCurrentFlowType() + " | Waiting: " + session.isWaitingForUserInput() +
                           " | Completed: " + session.isCompleted());
    }

    /**
     * Print ConversationSession object with focus on contract creation data
     * @param session The ConversationSession object to print
     */
    public static void printContractCreationSession(ConversationSession session) {
        if (session == null) {
            System.out.println("=== CONTRACT CREATION SESSION DEBUG ===");
            System.out.println("Session is NULL");
            System.out.println("=== END CONTRACT CREATION SESSION DEBUG ===");
            return;
        }

        System.out.println("=== CONTRACT CREATION SESSION DEBUG ===");
        System.out.println("Session ID: " + session.getSessionId());
        System.out.println("Session State: " + session.getState());
        System.out.println("Flow Type: " + session.getCurrentFlowType());
        System.out.println("Waiting For Input: " + session.isWaitingForUserInput());

        // Print contract creation specific data
        System.out.println("\nCONTRACT CREATION DATA:");
        Map<String, Object> collectedData = session.getCollectedData();
        String[] contractFields = {
            "ACCOUNT_NUMBER", "CONTRACT_NAME", "TITLE", "DESCRIPTION", "COMMENTS", "IS_PRICELIST" };

        for (String field : contractFields) {
            Object value = collectedData.get(field);
            if (value != null) {
                System.out.println("   " + field + ": " + value);
            } else {
                System.out.println("   " + field + ": (not set)");
            }
        }

        // Print remaining fields
        System.out.println("\nREMAINING FIELDS:");
        List<String> remainingFields = session.getRemainingFields();
        if (remainingFields.isEmpty()) {
            System.out.println("   All fields completed!");
        } else {
            for (String field : remainingFields) {
                System.out.println("   Missing: " + field);
            }
        }

        // Print recent conversation
        System.out.println("\nRECENT CONVERSATION:");
        List<ConversationSession.ConversationTurn> history = session.getConversationHistory();
        int startIndex = Math.max(0, history.size() - 3); // Show last 3 turns
        for (int i = startIndex; i < history.size(); i++) {
            ConversationSession.ConversationTurn turn = history.get(i);
            System.out.println("   " + turn.speaker + ": " + truncateString(turn.message, 80));
        }

        System.out.println("=== END CONTRACT CREATION SESSION DEBUG ===");
    }

    public static String checkListMsg() {

        return "<hr>" +
               "<h3 style='color: #28a745; text-align: center; background-color: #f0f8f0; padding: 20px; border: 2px solid #28a745; border-radius: 8px;'>" +
               "<span style='font-size: 18px;'></span> <big><b>Contract Checklist: Date Information Required</b></big>" +
               "</h3>" +
               "<p style='text-align: center; padding: 10px; background-color: #f8f9fa; border-left: 4px solid #007bff;'>" +
               "<b>Please provide the following contract dates in MM/DD/YY format</b><br>" +
               "<small><i>All dates are required for checklist completion</i></small>" + "</p>" + "<ol>" +
               "<li><b>System Date: </b> MM/DD/YY</li>" + "<li><b>Effective Date:</b> MM/DD/YY</li>" +
               "<li><b>Expiration Date:</b> MM/DD/YY</li>" + "<li><b>Flow Down Date:</b> MM/DD/YY</li>" +
               "<li><b>Price Expiration Date:</b> MM/DD/YY</li>" + "</ol>" + "<p><b>Format Options:</b></p>" + "<ul>" +
               "<li><b>Comma Separated:</b> 01/15/24, 02/01/24, 12/31/25, 03/01/24, 06/30/25</li>" +
               "<li><b>Attribute Format:</b> Date of Signature:01/15/24, Effective Date:02/01/24, Expiration Date:12/31/25, Flow Down Date:03/01/24, Price Expiration Date:06/30/25</li>" +
               "</ul>";
    }

    public static NLPQueryClassifier.QueryResult convertChatbotResponseToQueryResult(ConversationalNLPManager.ChatbotResponse response) {
        NLPQueryClassifier.QueryResult result = new NLPQueryClassifier.QueryResult();

        // Input tracking
        if (response.inputTracking != null) {
            result.inputTracking =
                new NLPQueryClassifier.InputTrackingResult(response.inputTracking.originalInput,
                                                           response.inputTracking.correctedInput,
                                                           response.inputTracking.correctionConfidence);
        }

        // Header (if available in data or metadata, otherwise leave as new Header)
        result.header = new NLPQueryClassifier.Header();
        // Optionally extract contract/part/customer info from response.data if present

        // Metadata
        if (response.metadata != null) {
            result.metadata =
                new NLPQueryClassifier.QueryMetadata(response.metadata.queryType, response.metadata.actionType,
                                                     response.metadata.processingTimeMs);
        }

        // Entities: map ConversationalNLPManager.EntityFilter to NLPQueryClassifier.EntityFilter
        if (response.entities != null) {
            List<NLPQueryClassifier.EntityFilter> mappedEntities = new ArrayList<>();
            for (ConversationalNLPManager.EntityFilter ef : response.entities) {
                mappedEntities.add(new NLPQueryClassifier.EntityFilter(ef.attribute, ef.operation, ef.value,
                                                                       ef.source));
            }
            result.entities = mappedEntities;
        } else {
            result.entities = new ArrayList<>();
        }

        // Display entities
        result.displayEntities = response.displayEntities != null ? response.displayEntities : new ArrayList<>();

        // Errors: map ConversationalNLPManager.ValidationError to NLPQueryClassifier.ValidationError
        if (response.errors != null) {
            List<NLPQueryClassifier.ValidationError> mappedErrors = new ArrayList<>();
            for (ConversationalNLPManager.ValidationError ve : response.errors) {
                mappedErrors.add(new NLPQueryClassifier.ValidationError(ve.code, ve.message, ve.severity));
            }
            result.errors = mappedErrors;
        } else {
            result.errors = new ArrayList<>();
        }

        // Optionally, you can store the raw data or dataProviderResponse in a custom field or context if needed

        return result;
    }

    public static String confirmationCreateCOntract(String accountNumber, String contractName, String description,
                                                    String comments, String contractNo, String title,
                                                    String isPricelist) {


        return "<h4>Contract Created Successfully!</h4>" + "<p><b>Account Number:</b> " + accountNumber + "</p>" +
               "<p><b>Contract NUmber:</b> " + contractNo + "</p>" + "<p><b>Contract Name:</b> " + contractName +
               "</p>" + "<p><b>Title:</b> " + title + "</p>" + "<p><b>Description:</b> " + description + "</p>" +
               "<p><b>Comments:</b> " + (comments.isEmpty() ? "None" : comments) + "</p>" +
               "<p><b>Price List Contract:</b> " + isPricelist + "</p>" + "<hr>" +
               "<h3 style='color: #007bff; text-align: center; background-color: #f8f9fa; padding: 15px; border: 2px solid #007bff;'>" +
               "<big><b>Would you like to create a checklist for this contract? (Yes/No)</b></big>" + "</h3>";
    }

    public static boolean hasUnsetAttributes(ConversationSession session) {
        System.out.println("BCCTCHATBOTUTILITY---hasUnsetAttributes=================>");
        Map<String, Object> collectedData = session.getCollectedData();
        System.out.println(collectedData);

        // Check if keys exist and have valid values
        String contractName =
            collectedData.containsKey("CONTRACT_NAME") ? (String) collectedData.get("CONTRACT_NAME") : null;
        String title = collectedData.containsKey("TITLE") ? (String) collectedData.get("TITLE") : null;
        String description =
            collectedData.containsKey("DESCRIPTION") ? (String) collectedData.get("DESCRIPTION") : null;
        String comments = collectedData.containsKey("COMMENTS") ? (String) collectedData.get("COMMENTS") : null;
        String isPricelist =
            collectedData.containsKey("IS_PRICELIST") ? (String) collectedData.get("IS_PRICELIST") : null;

        return (!collectedData.containsKey("CONTRACT_NAME") || contractName == null ||
                contractName.equals("(not set)") || contractName.isEmpty()) ||
               (!collectedData.containsKey("TITLE") || title == null || title.equals("(not set)") || title.isEmpty()) ||
               (!collectedData.containsKey("DESCRIPTION") || description == null || description.equals("(not set)") ||
                description.isEmpty()) ||
               (!collectedData.containsKey("COMMENTS") || comments == null || comments.isEmpty()) ||
               (!collectedData.containsKey("IS_PRICELIST") || isPricelist == null || isPricelist.equals("NO") ||
                isPricelist.isEmpty());
    }


    public static String getBotPromptWithAccount(String accountNumber) {
        return "<h4>Contract Creation</h4>" + "<p><b>Account:</b> " + accountNumber + "</p>" +
               "<p>Please provide details in this format:</p>" +
               "<p><b>Contract Name, Title, Description, Comments, Price List (Yes/No)</b></p>" +
               "<p><b>Example:</b> <i>testcontract, testtitle, testdesc, nocomments, no</i></p>";
    }
    // Returns the list of required contract fields that are not yet set, in order
    public static List<String> getUnsetAttributesList(ConversationSession session) {
        List<String> requiredFields =
            Arrays.asList("CONTRACT_NAME", "TITLE", "DESCRIPTION", "COMMENTS", "IS_PRICELIST");
        List<String> missing = new ArrayList<>();
        Map<String, Object> data = session.getCollectedData();
        for (String field : requiredFields) {
            Object val = data.get(field);
            if (val == null || val.toString()
                                  .trim()
                                  .isEmpty()) {
                missing.add(field);
            }
        }
        return missing;
    }

    // Utility to generate contract summary for confirmation
    public static String getContractSummary(ConversationSession session) {
        if (session == null) {
            return "";
        }
        Map<String, Object> data = session.getCollectedData();
        if (data == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='padding: 15px; border: 1px solid #ddd; border-radius: 5px; background-color: #f9f9f9;'>");
        sb.append("<h4 style='color: #333; margin-top: 0;'>Contract Summary</h4>");

        sb.append("<p><b>Account Number:</b> ")
          .append(sanitizeHtml(data.getOrDefault("ACCOUNT_NUMBER", "")))
          .append("</p>");

        sb.append("<p><b>Contract Name:</b> ")
          .append(sanitizeHtml(data.getOrDefault("CONTRACT_NAME", "")))
          .append("</p>");

        sb.append("<p><b>Title:</b> ")
          .append(sanitizeHtml(data.getOrDefault("TITLE", "")))
          .append("</p>");

        sb.append("<p><b>Description:</b> ")
          .append(sanitizeHtml(data.getOrDefault("DESCRIPTION", "")))
          .append("</p>");

        sb.append("<p><b>Comments:</b> ")
          .append(sanitizeHtml(data.getOrDefault("COMMENTS", "")))
          .append("</p>");

        sb.append("<p><b>Is Pricelist:</b> ")
          .append(sanitizeHtml(data.getOrDefault("IS_PRICELIST", "")))
          .append("</p>");

        sb.append("</div>");

        return sb.toString();
    }


    private static String sanitizeHtml(Object value) {
        if (value == null) {
            return "";
        }

        String input = String.valueOf(value);

        // Allow only specific HTML elements: <br><hr><li><ol><ul><p><b><i><tt><big><small><pre><span><a><h1><h2><h3><h4><h5><h6>
        // Allow only specific entities: &lt;&gt;&amp;&reg;&copy;&nbsp;&quot;
        // Allow only CSS attributes: class, style, href

        return input.replaceAll("<(?!/?(?:br|hr|li|ol|ul|p|b|i|tt|big|small|pre|span|a|h[1-6])(?:\\s+(?:class|style|href)\\s*=\\s*[\"'][^\"']*[\"'])*\\s*>)[^>]*>",
                                "").replaceAll("&(?!(?:lt|gt|amp|reg|copy|nbsp|quot);)", "&amp;");
    }


    // Utility to create a bot response for confirmation prompt
    public static ConversationalNLPManager.ChatbotResponse createBotResponse(String message,
                                                                             ConversationSession session) {
        ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
        response.isSuccess = true;
        response.dataProviderResponse = message;
        // Optionally set other fields as needed
        return response;
    }

    public static String constructPromptMessage(List<String> requiredFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><b>Please provide the following information to create your contract:</b></p><ul>");
        for (String field : requiredFields) {
            sb.append("<li>")
              .append(ContractFieldConfig.getDisplayName(field))
              .append("</li>");
        }
        sb.append("</ul><p><i>You must provide all fields. No defaults will be used for Comments or Price List.</i></p>");
        sb.append("<div style='background-color: #fff3cd; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #ffc107;'>" +
                  "<h4 style='color: #856404; margin-top: 0;'>Supported Input Formats:</h4>" +
                  "<p style='margin: 5px 0;'><strong>Label = Value:</strong> <tt>contract number = GSA-123456, effective date = 01/15/24</tt></p>" +
                  "<p style='margin: 5px 0;'><strong>Label: Value:</strong> <tt>contract name: IT Services, customer: 12345 - Tech Corp</tt></p>" +
                  "<p style='margin: 5px 0;'><strong>Comma Separated:</strong> <tt>GSA-123456, IT Services Contract, 12345 - Tech Corp, 01/15/24</tt></p>" +
                  "</div>");
        sb.append("<div style='background-color: #d1ecf1; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #17a2b8;'>" +
                  "<h4 style='color: #0c5460; margin-top: 0;'>Example Input:</h4>" +
                  "<pre style='background-color: #ffffff; padding: 10px; border-radius: 4px; font-size: 14px; overflow-x: auto;'>" +
                  "contract number: GSA-47QSWA19D0024\n" + "contract name: Professional IT Services\n" +
                  "customer: 12345 - Department of Defense\n" + "effective date: 01/15/2024\n" +
                  "expiration date: 12/31/2026\n" + "price expiration: 06/30/2025\n" + "award rep: Sarah Johnson" +
                  "</pre>" + "</div>");
        return sb.toString();
    }

    public static String constructCheckListPromptMessage(List<String> requiredFields, String award) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><b>Please provide the following information to create your check List for :</b></p><ul>");
        for (String field : requiredFields) {
            sb.append("<li>")
              .append(ContractFieldConfig.getDisplayName(field))
              .append("</li>");
        }
        sb.append("</ul><p><i>You must provide all fields.</i></p>");
        sb.append("</ul><p><i></i></p>");
        sb.append("<div style='background-color: #fff3cd; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #ffc107;'>" +
                  "<h4 style='color: #856404; margin-top: 0;'>Supported Input Formats:</h4>" +
                  "<p style='margin: 5px 0;'><strong>Label = Value:</strong> <tt>effective date = mm/dd/yy, expiration date=mm/dd/yy</tt></p>" +
                  "<p style='margin: 5px 0;'><strong>Label: Value:</strong> <tt>effective date: mm/dd/yy, expiration date:" +
                  "</div>");

        sb.append("<div style='background-color: #d1ecf1; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #17a2b8;'>" +
                  "<h4 style='color: #0c5460; margin-top: 0;'>Example Input:</h4>" +
                  "<pre style='background-color: #ffffff; padding: 10px; border-radius: 4px; font-size: 14px; overflow-x: auto;'>" +
                  "contract number: GSA-47QSWA19D0024\n" + "effective date: 01/15/2024\n" +
                  "expiration date: 12/31/2026\n" + "price expiration= 06/30/2025\n" + "</pre>" + "</div>");

        return sb.toString();
    }

    /**
     * Construct a prompt message for required checklist fields.
     */
    public static String constructChecklistPromptMessage(List<String> checklistFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><b>Please provide the following checklist details:</b></p><ul>");
        for (String field : checklistFields) {
            sb.append("<li>")
              .append(ContractFieldConfig.getChecklistDisplayName(field))
              .append("</li>");
        }
        sb.append("</ul><p><i>Provide all fields in the format: Field: value, ...</i></p>");
        return sb.toString();
    }

    public static Map<String, String> validateCheckListDates(Map<String, String> dateMap) {
        Map<String, String> validationResults = new HashMap<>();

        if (dateMap == null || dateMap.isEmpty()) {
            validationResults.put("ERROR", "No dates to validate");
            return validationResults;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        LocalDate today = LocalDate.now();

        for (Map.Entry<String, String> entry : dateMap.entrySet()) {
            String dateKey = entry.getKey();
            String dateValue = entry.getValue();

            try {
                LocalDate parsedDate = LocalDate.parse(dateValue, formatter);
                boolean isValid = false;
                String errorMessage = "";

                switch (dateKey) {
                case "EXPIRATION_DATE":
                    isValid = !parsedDate.isAfter(today); // Error if future
                    errorMessage = "Expiration date cannot be in the future: " + dateValue;
                    break;
                case "PRICE_EXPIRATION_DATE":
                    isValid = !parsedDate.isAfter(today); // Error if future
                    errorMessage = "Price expiration date cannot be in the future: " + dateValue;
                    break;
                case "SYSTEM_LOADED_DATE":
                    isValid = !parsedDate.isAfter(today); // Error if future
                    errorMessage = "System loaded date cannot be in the future: " + dateValue;
                    break;
                case "QUATAR":
                    isValid = !parsedDate.isAfter(today); // Error if future
                    errorMessage = "Qatar date cannot be in the future: " + dateValue;
                    break;
                case "DATE_OF_SIGNATURE":
                    isValid = !parsedDate.isAfter(today); // Error if future
                    errorMessage = "Signature date cannot be in the future: " + dateValue;
                    break;
                default:
                    isValid = !parsedDate.isAfter(today); // Error if future for unknown keys
                    errorMessage = "Date cannot be in the future: " + dateValue;
                    break;
                }

                validationResults.put(dateKey, isValid ? "success" : errorMessage);

            } catch (Exception e) {
                validationResults.put(dateKey, "Invalid date format: " + dateValue);
            }
        }

        return validationResults;
    }

    public static Map createCheckLits(Map params) {
        System.out.println("createCheckLits================>" + params);
        return null;
    }

    /**
     * Normalize a date string to 'dd MMM yyyy' format, disambiguating month/day if needed.
     * If the first value is 12, treat as day; otherwise, treat as month.
     * Accepts D/M/YY, M/D/YY, D/M/YYYY, M/D/YYYY, etc.
     */
    public static String normalizeDate(String input) {
        if (input == null)
            return "";
        String[] parts = input.split("/");
        if (parts.length != 3)
            return input;
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            String yearStr = parts[2];
            int year = yearStr.length() == 2 ? 2000 + Integer.parseInt(yearStr) : Integer.parseInt(yearStr);
            int day, month;
            if (first > 12) {
                day = first;
                month = second;
            } else {
                month = first;
                day = second;
            }
            java.time.LocalDate date = java.time
                                           .LocalDate
                                           .of(year, month, day);
            return date.format(java.time
                                   .format
                                   .DateTimeFormatter
                                   .ofPattern("dd MMM yyyy"));
        } catch (Exception e) {
            return input;
        }
    }

}

