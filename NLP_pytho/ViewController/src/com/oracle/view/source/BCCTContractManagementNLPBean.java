package com.oracle.view.source;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ValueChangeEvent;

import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.output.RichOutputText;
import oracle.adf.view.rich.render.ClientEvent;

/**
 * BCCTContractManagementNLPBean - ADF Managed Bean for Comprehensive Contract Management
 * Enhanced with real conversational chatbot functionality
 * Handles all NLP interactions for contracts, parts, failed parts, opportunities, and customers
 * Supports multi-turn conversations with session management and state tracking
 */
@ManagedBean(name = "bcctContractManagementNLPBean")
@SessionScoped
public class BCCTContractManagementNLPBean {
    private static final int DEFAULT_SCREEN_WIDTH = 400;
    
    // ========================================
    // BUTTON GETTER METHODS - Reference BCCTChatBotUtility
    // These methods provide access to static prompts from BCCTChatBotUtility
    // ========================================
    
    private String buttonRecentContracts;
    private String buttonPartsCount;
    private String buttonFailedContracts;
    private String buttonExpiringSoon;
    private String buttonAwardReps;
    private String buttonHelpPrompt;
    private String buttonCreateContract;

    public void setButtonRecentContracts(String buttonRecentContracts) {
        this.buttonRecentContracts = buttonRecentContracts;
    }

    public String getButtonRecentContracts() {
        return BCCTChatBotUtility.QUICK_ACTION_RECENT_CONTRACTS_PROMPT;
    }

    public void setButtonPartsCount(String buttonPartsCount) {
        this.buttonPartsCount = buttonPartsCount;
    }

    public String getButtonPartsCount() {
        return BCCTChatBotUtility.QUICK_ACTION_PARTS_COUNT_PROMPT;
    }

    public void setButtonFailedContracts(String buttonFailedContracts) {
        this.buttonFailedContracts = buttonFailedContracts;
    }

    public String getButtonFailedContracts() {
        return BCCTChatBotUtility.QUICK_ACTION_FAILED_CONTRACTS_PROMPT;
    }

    public void setButtonExpiringSoon(String buttonExpiringSoon) {
        this.buttonExpiringSoon = buttonExpiringSoon;
    }

    public String getButtonExpiringSoon() {
        return BCCTChatBotUtility.QUICK_ACTION_EXPIRING_SOON_PROMPT;
    }

    public void setButtonAwardReps(String buttonAwardReps) {
        this.buttonAwardReps = buttonAwardReps;
    }

    public String getButtonAwardReps() {
        return BCCTChatBotUtility.QUICK_ACTION_AWARD_REPS_PROMPT;
    }

    public void setButtonHelpPrompt(String buttonHelpPrompt) {
        this.buttonHelpPrompt = buttonHelpPrompt;
    }

    public String getButtonHelpPrompt() {
        return BCCTChatBotUtility.QUICK_ACTION_HELP_PROMPT;
    }

    public void setButtonCreateContract(String buttonCreateContract) {
        this.buttonCreateContract = buttonCreateContract;
    }

    public String getButtonCreateContract() {
        return BCCTChatBotUtility.QUICK_ACTION_CREATE_CONTRACT_PROMPT;
    }

    // ========================================
    // END BUTTON GETTER METHODS
    // ========================================
    
    // Core NLP components
    private ConversationalNLPManager conversationalNLPManager;
    private NLPUserActionHandler nlpHandler; // Legacy - for backward compatibility
    private ContractCreationFlowManager flowManager;
    private ConversationalFlowManager conversationalFlowManager;
    private ConversationSessionManager sessionManager;

    // UI Components
    private RichInputText userInputText;
    private RichOutputText botResponseText;


    // Data properties
    private String userInput;
    private String botResponse;
    private String sessionId;
    private String currentFlowStep;
    private boolean isInContractCreationFlow;
    private List<ChatMessage> chatHistory = new ArrayList<>();
    private Map<String, Object> flowState;

    // Contract creation specific properties
    private String accountNumber;
    private String contractName;
    private String contractTitle;
    private String contractDescription;
    private String priceList;
    private String isEM;
    private String effectiveDate;
    private String expirationDate;
    private String priceExpirationDate;

    // Validation and status properties
    private boolean isAccountValid;
    private String validationMessage;
    private String currentActionType;
    private boolean isProcessing;

    // Enhanced conversational properties
    private boolean isWaitingForUserInput;
    private String expectedResponseType;
    private String currentConversationId;
    private Map<String, Object> conversationContext;

    // Checklist process fields
    private Map<String, String> checklistData = new HashMap<>();
    private List<String> checklistFields = Arrays.asList(
        "DATE_OF_SIGNATURE", "EFFECTIVE_DATE", "EXPIRATION_DATE", "FLOW_DOWN_DATE", "PRICE_EXPIRATION_DATE"
    );
    private int checklistFieldIndex = 0;

    // Add a field to store extracted account number from initial command
    private String initialAccountNumber = null;

    // Add a flag to track if waiting for valid account number
    private boolean waitingForValidAccountNumber = false;

    private boolean isAccountNumber(String token) {
        return token != null && token.matches("\\d{6,}");
    }

    public void initiateCheckListProcess() {
        checklistData.clear();
        checklistFieldIndex = 0;
        promptNextChecklistField();
    }

    private void promptNextChecklistField() {
        if (checklistFieldIndex < checklistFields.size()) {
            isWaitingForUserInput = true;
        } else {
            validateAndProcessChecklist();
        }
    }

    public void handleChecklistUserInput(String userInput) {
        // Ensure conversation ID is set for checklist
        if (currentConversationId == null || currentConversationId.isEmpty()) {
            currentConversationId = "conv_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }

        // Try to parse as attribute format (Field: value or Field value)
        Map<String, String> parsed = parseFlexibleChecklistInput(userInput);
        if (parsed.size() == checklistFields.size()) {
            checklistData.putAll(parsed);
            checklistFieldIndex = checklistFields.size();
            validateAndProcessChecklist();
            return;
        }

        // Try to parse as comma separated, only accept valid date patterns
        String[] parts = userInput.split(",");
        List<String> validDates = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.matches("\\d{2}/\\d{2}/\\d{2}")) {
                validDates.add(trimmed);
            }
        }
        if (validDates.size() == checklistFields.size()) {
            for (int i = 0; i < checklistFields.size(); i++) {
                checklistData.put(checklistFields.get(i), validDates.get(i));
            }
            checklistFieldIndex = checklistFields.size();
            validateAndProcessChecklist();
            return;
        }

        // Fallback: treat as single field entry if valid
        String field = checklistFields.get(checklistFieldIndex);
        if (userInput.trim().matches("\\d{2}/\\d{2}/\\d{2}")) {
            checklistData.put(field, userInput.trim());
            checklistFieldIndex++;
        }
        promptNextChecklistField();
    }

    private Map<String, String> parseFlexibleChecklistInput(String userInput) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = userInput.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("[: ]", 2); // Split on colon or first space
            if (kv.length == 2) {
                String key = kv[0].trim().replace(" ", "_").toUpperCase();
                String value = kv[1].trim();
                if (value.matches("\\d{2}/\\d{2}/\\d{2}")) {
                    for (String field : checklistFields) {
                        if (field.equalsIgnoreCase(key)) {
                            result.put(field, value);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void validateAndProcessChecklist() {
        Map<String, String> validationResult = nlpHandler.validateCheckListDates(checklistData);
        List<String> invalidFields = new ArrayList<>();
        StringBuilder errorMsg = new StringBuilder();
        for (String field : checklistFields) {
            String result = validationResult.get(field);
            if (!"success".equalsIgnoreCase(result)) {
                invalidFields.add(field);
                errorMsg.append(field.replace('_', ' ')).append(": ").append(result).append("\n");
            }
        }
        if (invalidFields.isEmpty()) {
            // All valid, proceed to creation
            Map response = nlpHandler.checkListCreation(checklistData);
            String creationResult=""+response.get("STATUS");
            String msg="" + response.get("MESSAGE");
            if ("SUCCESS".equalsIgnoreCase(creationResult)) {
                addBotMessage("Checklist creation successful.");
            } else {
                addBotMessage("Checklist creation failed: " + msg);
            }
            isWaitingForUserInput = false;
        } else {
            // Prompt for invalid fields again
            addBotMessage("Please correct the following:\n" + errorMsg.toString());
            checklistFields = new ArrayList<>(invalidFields);
            checklistFieldIndex = 0;
            promptNextChecklistField();
        }
    }

    private String heading;
    private String heading1;

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public void setHeading1(String heading1) {
        this.heading1 = heading1;
    }

    public String getHeading() {
        return "<h2 style='color: #ffffff; " +
               "font-family: -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, Roboto, sans-serif; " +
               "font-size: 24px; " + "font-weight: 700; " + "margin: 0; " + "letter-spacing: -0.5px;'>" +
               "AI Contract Assistant" + "</h2>";
    }

    public String getHeading1() {
        return "<p style='color: rgba(255,255,255,0.9); " +
               "font-family: -apple-system, BlinkMacSystemFont, &quot;Segoe UI&quot;, Roboto, sans-serif; " +
               "font-size: 14px; " + "font-weight: 400; " + "margin: 4px 0 0 0; " + "opacity: 0.95;'>" +
               "Your intelligent assistant for contract and parts information" + "</p>";
    }

    /**
     * Constructor - Initialize all components
     */
    public BCCTContractManagementNLPBean() {
        // Initialize all fields first
        this.chatHistory = new ArrayList<>();
        initializeComponents();
        initializeSession();
    }

    /**
     * Initialize core NLP components
     */
    private void initializeComponents() {
        try {
            // Initialize new conversational NLP manager
            this.conversationalNLPManager = new ConversationalNLPManager();

            // Initialize legacy components for backward compatibility
            this.nlpHandler = NLPUserActionHandler.getInstance();
            this.flowManager = ContractCreationFlowManager.getInstance();
            this.conversationalFlowManager = ConversationalFlowManager.getInstance();
            this.sessionManager = ConversationSessionManager.getInstance();

            this.flowState = new HashMap<>();
            this.conversationContext = new HashMap<>();

            // Set default values
            this.priceList = "NO";
            this.isEM = "NO";
            this.isAccountValid = false;
            this.isProcessing = false;
            this.isWaitingForUserInput = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize session-specific data
     */
    private void initializeSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.currentFlowStep = "INITIAL";
        this.isInContractCreationFlow = false;

        // Add initial greeting
        addBotMessage("<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                      "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                      "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                      "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                      "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                      "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                      "<li>Failed contracts</li>" +
                      "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                      "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                      "<p><i>How can I help you today?</i></p>");
    }


    /**
     * Process new user input (not a follow-up response)
     */
    private void processNewUserInput() {
        try {
            // Retrieve the session at the start of the method
            ConversationSession session = conversationalNLPManager.getSession(sessionId, "ADF_USER");
            if (session == null) {
                addBotMessage("Session not found. Please start a new contract creation process.");
                return;
            }

            // Gating: If contract creation is in progress, only accept contract-related input
            if (session.isCreateContractInitiated() && !allContractFieldsFilled(session)) {
                if (!isContractCreationQuery(userInput)) {
                    addBotMessage("Contract creation is in progress. Please complete it before asking other questions.");
                    return;
                }
            }
            // Gating: If checklist is in progress, only accept checklist-related input
            if (session.isCheckListInitiated() && !allChecklistFieldsFilled(session)) {
                if (!isChecklistInput(userInput)) {
                    addBotMessage("Checklist process is in progress. Please complete it before asking other questions.");
                    return;
                }
            }

            // Identify contract creation query/action
            if (isContractCreationQuery(userInput)) {
                session.setCreateContractInitiated(true);
            }

            // Use new ConversationalNLPManager for all processing
            ConversationalNLPManager.ChatbotResponse response =
                conversationalNLPManager.processUserInput(userInput, sessionId, "ADF_USER");

            if (response.isSuccess) {
                // Handle successful response
                handleSuccessfulResponse(response);
            } else {
                // Handle error response
                handleErrorResponse(response);
            }

            // After getting the QueryResult from the classifier:
            NLPQueryClassifier.QueryResult queryResult = BCCTChatBotUtility.convertChatbotResponseToQueryResult(response);
            for (NLPQueryClassifier.EntityFilter filter : queryResult.entities) {
                if ("CUSTOMER_NUMBER".equalsIgnoreCase(filter.attribute)) {
                    String accountNumber = filter.value;
                    if (validateCustomer(accountNumber)) {
                        // ConversationSession session = conversationalNLPManager.getOrCreateSession(sessionId, "ADF_USER"); // REMOVED
                        // session.getCollectedData().put("ACCOUNT_NUMBER", accountNumber); // REMOVED
                        // session.getExpectedFields().remove("ACCOUNT_NUMBER"); // REMOVED
                    } else {
                        addBotMessage("The account number you entered is invalid. Please provide a valid account number, or type 'Cancel' to terminate the contract creation process.");
                        isWaitingForUserInput = true;
                        waitingForValidAccountNumber = true;
                        return;
                    }
                }
            }

        } catch (Exception e) {
            addBotMessage("Error processing query: " + e.getMessage());
        }
    }

    // Helper to generate contract creation prompt for missing fields
    private String generateContractCreationPrompt(ConversationSession session) {
        List<String> requiredFields = Arrays.asList("ACCOUNT_NUMBER", "CONTRACT_NAME", "TITLE", "DESCRIPTION", "COMMENTS", "IS_PRICELIST");
        Map<String, String> fieldMap = session.getContractFieldMap();
        StringBuilder prompt = new StringBuilder("Please provide the following information to create your contract:\n");
        for (String key : requiredFields) {
            String value = fieldMap.get(key);
            if (key.equals("ACCOUNT_NUMBER") || key.equals("CUSTOMER_NUMBER")) {
                if (value != null && value.matches("\\d{6,}")) {
                    continue; // Already present and valid, skip
                }
            }
            if (value == null || value.isEmpty()) {
                prompt.append("- ").append(fieldDisplayName(key)).append("\n");
            }
        }
        return prompt.toString();
    }
    // Helper to check if all contract fields are filled and valid
    private boolean allContractFieldsFilled(ConversationSession session) {
        List<String> requiredFields = Arrays.asList("ACCOUNT_NUMBER", "CONTRACT_NAME", "TITLE", "DESCRIPTION", "COMMENTS", "IS_PRICELIST");
        Map<String, String> fieldMap = session.getContractFieldMap();
        for (String key : requiredFields) {
            String value = fieldMap.get(key);
            if ((key.equals("ACCOUNT_NUMBER") || key.equals("CUSTOMER_NUMBER"))) {
                if (value == null || !value.matches("\\d{6,}")) {
                    return false;
                }
            } else if (value == null || value.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    // Helper to get display name for a field
    private String fieldDisplayName(String key) {
        switch (key) {
            case "ACCOUNT_NUMBER":
            case "CUSTOMER_NUMBER":
                return "Account Number (6+ digits)";
            case "CONTRACT_NAME":
                return "Contract Name";
            case "TITLE":
                return "Title";
            case "DESCRIPTION":
                return "Description";
            case "COMMENTS":
                return "Comments (optional)";
            case "IS_PRICELIST":
                return "Price List Contract? (Yes/No)";
            default:
                return key;
        }
    }
    // Helper to check if all checklist fields are filled
    private boolean allChecklistFieldsFilled(ConversationSession session) {
        for (String key : session.getChecklistFieldMap().keySet()) {
            if (session.getChecklistFieldMap().get(key) == null || session.getChecklistFieldMap().get(key).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    // Helper to check if input is checklist-related (implement as needed)
    private boolean isChecklistInput(String userInput) {
        // Implement logic to detect checklist input (e.g., date formats, field names, etc.)
        return userInput.matches(".*\\d{2}/\\d{2}/\\d{2}.*") || userInput.toLowerCase().contains("date") || userInput.toLowerCase().contains("checklist");
    }

    /**
     * Handle successful response from ConversationalNLPManager
     */
    private void handleSuccessfulResponse(ConversationalNLPManager.ChatbotResponse response) {
        System.out.println("handleSuccessfulResponse==============>");
        String dataProviderResponseFromJson = null;
        System.out.println("Query Type :" + response.metadata.queryType);
        System.out.println("Action Type :" + response.metadata.actionType);
        if (NLPConstants.isHelpQuery(response.metadata.queryType) &&
            NLPConstants.isBotContractCreationAction(response.metadata.actionType)) {
            System.out.println("Query Type :" + response.metadata.queryType);
            System.out.println("Action Type :" + response.metadata.actionType);
            // Use the data field directly for contract creation responses
            dataProviderResponseFromJson =
                response.data != null ? response.data.toString() : response.dataProviderResponse;

        } else if (NLPConstants.isHelpQuery(response.metadata.queryType) &&
                   NLPConstants.isUserContractCreationAction(response.metadata.actionType)) {
            // Extract message from response
            String dataProviderResponse = extractMessageFromResponse(response);
            System.out.println("Contarcts creation by User babu dataProviderResponse===========>" +
                               dataProviderResponse);
            dataProviderResponseFromJson = BCCTChatBotUtility.extractDataProviderResponseFromJson(dataProviderResponse);
        }else if (NLPConstants.isHelpQuery(response.metadata.queryType) &&
                   NLPConstants.isUserCheckListCreationAction(response.metadata.actionType)){
            // Extract message from response
            System.out.println("CONTRACT_CREATION_CHECKLIST==============");
            String dataProviderResponse = extractMessageFromResponse(response);
            System.out.println("dataProviderResponse===========>" + dataProviderResponse);
            dataProviderResponseFromJson=dataProviderResponse;
            
        } else {
            // Extract message from response
            String dataProviderResponse = extractMessageFromResponse(response);
            System.out.println("dataProviderResponse===========>" + dataProviderResponse);
            dataProviderResponseFromJson = BCCTChatBotUtility.extractDataProviderResponseFromJson(dataProviderResponse);
        }

        // Add bot response to chat
        addBotMessage(dataProviderResponseFromJson);

        // Update conversation state if needed
        if (response.metadata != null) {
            this.currentActionType = response.metadata.actionType;

            // Check if this is a conversational response
            if (response.metadata
                        .queryType
                        .equals("HELP") && response.metadata
                                                   .actionType
                                                   .contains("HELP_CONTRACT_CREATE")) {
                this.isWaitingForUserInput = true;
                this.isInContractCreationFlow = true;
            }
        }

        // After processing the response, check for contract creation initiation
        if (response != null && response.metadata != null &&
            "HELP".equalsIgnoreCase(response.metadata.queryType) &&
            response.metadata.actionType != null &&
            response.metadata.actionType.contains("HELP_CONTRACT_CREATE_BOT")) {
            // Initiate and track contract creation session
            this.currentConversationId = this.sessionId;
            this.isWaitingForUserInput = true;
        }

        // Extract data provider response for ADF
        if (response.dataProviderResponse != null) {
            // This can be used by ADF components
            this.botResponse = response.dataProviderResponse;
        }
    }

    /**
     * Handle error response from ConversationalNLPManager
     */
    private void handleErrorResponse(ConversationalNLPManager.ChatbotResponse response) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("I encountered some issues:\n\n");

        if (response.errors != null) {
            for (ConversationalNLPManager.ValidationError error : response.errors) {
                errorMessage.append("")
                            .append(error.message)
                            .append("\n");
            }
        }

        addBotMessage(errorMessage.toString());
    }

    private String handlingDatOPerationsWIthDEntityEXtraction(ConversationalNLPManager.ChatbotResponse response) {
        System.out.println("handlingDatOPerationsWIthDEntityEXtraction======>");
        NLPEntityProcessor.QueryResult queryResult = BCCTChatBotUtility.convertMyObjects(response);

        // Check if queryResult is null and return HTML error message
        if (queryResult == null) {
            return "<div style=\"color: #d32f2f; padding: 10px; border: 1px solid #f44336; border-radius: 4px; background-color: #ffebee;\">" +
                   "<h3><i>Processing Error</i></h3>" + "<p><b>Unable to process your request.</b></p>" +
                   "<p>The system encountered an issue while analyzing your message. Please try:</p>" + "<ul>" +
                   "<li>Rephrasing your question</li>" + "<li>Using simpler terms</li>" +
                   "<li>Checking your input format</li>" + "</ul>" +
                   "<p><small>If the problem persists, please contact support.</small></p>" + "</div>";
        }

        // Step 4: Check if this is a HELP query - handle directly without DB calls
        if (queryResult.metadata.actionType != null && queryResult.metadata
                                                                  .actionType
                                                                  .startsWith("HELP_")) {
            String helpContent = BCCTChatBotUtility.getHelpContent(queryResult.metadata.actionType);
            return BCCTChatBotUtility.createCompleteResponseJSONWithHelpContent(queryResult, helpContent);
        }

        // Step 5: For non-HELP queries, use singleton instance and execute DataProvider actions with database integration
        NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
        if (queryResult.getQueryType().equalsIgnoreCase("INQUIRY")) {
            return handleUserBlidActions(queryResult);
        }
        if(queryResult.getQueryType().equalsIgnoreCase("HELP")&&queryResult.getActionType().equalsIgnoreCase(NLPConstants.ACTION_TYPE_HELP_CHECK_LIST)){
             initiateCheckListProcess();
             return BCCTChatBotUtility.checkListMsg();
        }
        
        System.out.println("handlingDatOPerationsWIthDEntityEXtraction==================>");
        String dataProviderResult =
            handler.executeDataProviderActionWithDB(queryResult, DEFAULT_SCREEN_WIDTH, sessionId);
        if (hasUserActionRequired(dataProviderResult)) {
            dataProviderResult = removeUserActionRequiredTags(dataProviderResult);
            this.isWaitingForUserInput = true;

            // Set currentConversationId to sessionId when user action is required
            // This ensures the conversation state is properly tracked
            if (this.currentConversationId == null) {
                this.currentConversationId = sessionId;
                System.out.println("Setting currentConversationId to sessionId: " + sessionId);
            }

            // Store user search results in ConversationalNLPManager session
            Map<String, List<Map<String, String>>> userResults = handler.getUserSearchCache();
            if (userResults != null && !userResults.isEmpty()) {
                // Get the actual display order from NLPUserActionHandler (the order shown to user)
                List<String> userDisplayOrder = handler.getUserDisplayOrder();

                // If display order is empty, fall back to keySet order
                if (userDisplayOrder == null || userDisplayOrder.isEmpty()) {
                    userDisplayOrder = new ArrayList<>(userResults.keySet());
                }

                // Store user search results in ConversationalNLPManager session using currentConversationId
                String conversationId = (currentConversationId != null) ? currentConversationId : sessionId;
                conversationalNLPManager.storeUserSearchResultsInSession(conversationId, userResults, userDisplayOrder);
                System.out.println("User search results stored in session: " + userDisplayOrder +
                                   " with conversation ID: " + conversationId);
            }
        }
        // Step 6: Return complete response as JSON string
        return handler.createCompleteResponseJSON(queryResult, dataProviderResult);


    }

    /**
     * Handle user blind actions for INQUIRY queries
     * This method handles cases where the user has already been processed by ConversationalNLPManager
     * but the system still processes it through the legacy flow
     */
    private String handleUserBlidActions(NLPEntityProcessor.QueryResult queryResult) {
        System.out.println("handleUserBlidActions called with actionType: " + queryResult.metadata.actionType);

        // Check if this is a meaningless input after user selection (ENQUERY_FAILED)
        if ("ENQUERY_FAILED".equals(queryResult.metadata.actionType)) {
            // This is a meaningless input (like "3" after user selection)
            // Return the help message that was already generated by ConversationalNLPManager
            String helpMessage = null;
            if (helpMessage == null ||
                helpMessage.isEmpty()) {
                // Fallback help message
                helpMessage =
                                      "<p><b>You can ask me about:</b></p>" + "<ul>" +
                                      "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                                      "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                                      "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                                      "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                                      "<li>Failed contracts</li>" +
                                      "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                                      "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>";
            }

            // Create a proper response JSON using the existing method
            return BCCTChatBotUtility.createCompleteResponseJSONWithHelpContent(queryResult, helpMessage);
        }

        // For other INQUIRY types, return a generic response
        String genericMessage =
            "<p><b>I understand you're asking a question.</b></p>" +
            "<p>Please try rephrasing your question or use one of these examples:</p>" + "<ul>" +
            "<li>Show contract ABC123</li>" + "<li>Show parts for contract XYZ456</li>" +
            "<li>Show contracts created by Balayya</li>" + "<li>Show failed parts</li>" + "<li>Create contract</li>" +
            "</ul>";

        return BCCTChatBotUtility.createCompleteResponseJSONWithHelpContent(queryResult, genericMessage);
    }

    /**
     * Extract message from response
     */
    private String extractMessageFromResponse(ConversationalNLPManager.ChatbotResponse response) {

        return handlingDatOPerationsWIthDEntityEXtraction(response);
    }


    /**
     * Process conversational response (follow-up to previous question)
     */
    private void processConversationalResponse() {
        System.out.println("Starting of processConversationalResponse");
        try {
            // Check if this is a user selection from ConversationalNLPManager
            // Use the same conversation ID logic as in the storage
            String conversationId = (currentConversationId != null) ? currentConversationId : sessionId;
            System.out.println("processConversationalResponse================>"+conversationId);

            if (conversationalNLPManager.isSessionWaitingForUserInput(conversationId)) {
                System.out.println("==================processConversationalResponse==waiting");
                // Handle user selection through ConversationalNLPManager
                ConversationalNLPManager.ChatbotResponse response =
                    conversationalNLPManager.processUserInput(userInput, conversationId, "ADF_USER");

                // FIXED LOGIC: Always process the response when waiting for user input
                if (response.isSuccess) {
                    System.out.println("User selection processed successfully");
                    addBotMessage(response.dataProviderResponse);
                    // Only reset if the entire flow is complete (e.g., after checklist is created)
                    if (response.dataProviderResponse != null 
                        && response.dataProviderResponse.toLowerCase().contains("checklist created")) {
                        this.isWaitingForUserInput = false;
                        this.currentConversationId = null;
                    } else {
                        // Still waiting for user input (e.g., checklist prompt)
                        this.isWaitingForUserInput = true;
                        // DO NOT reset currentConversationId here!
                    }
                } else {
                    System.out.println("User selection failed");
                    for (ConversationalNLPManager.ValidationError error : response.errors) {
                        System.out.println("Error message--->" + error.message);
                        
                        addBotMessage("<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                                      "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                                      "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                                      "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                                      "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                                      "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                                      "<li>Failed contracts</li>" +
                                      "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                                      "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                                      "<p><i>How can I help you today?</i></p>");
                    }
                    // Keep waiting for user input if there was an error
                }
                return; // Exit early - don't process legacy flow
            }

            // Handle legacy conversation flow only if ConversationalNLPManager is not handling it
            // Get current conversation state
            ConversationSessionManager.ConversationState conversation =
                sessionManager.getConversationState(currentConversationId);
            
            System.out.println("currentConversationId====>"+currentConversationId);
            
            System.out.println("conversation object is emoty or not ;;"+conversation==null?"empty":"not");

            if (conversation == null) {
                // Only show expired message if user actually asked something
                if (userInput != null && !userInput.trim().isEmpty()) {
                    System.out.println("jai balayya===");
                    addBotMessage("Conversation session expired. Please start over.");
                }
                resetConversationState();
                return;
            }

            // Process the response through session manager
            ConversationSessionManager.ConversationResult result =
                sessionManager.processUserInput(userInput, sessionId);

            if (result.requiresFollowUp()) {
                // Still waiting for more input
                expectedResponseType = result.getExpectedResponseType();
                addBotMessage(result.getMessage());

            } else if (result.isComplete()) {
                // Conversation is complete, process the final result
                processCompleteConversationalQuery(result);
                resetConversationState();

            } else {
                // Error in processing
                addBotMessage("Error processing response: " + result.getError());
                resetConversationState();
            }

        } catch (Exception e) {
            e.printStackTrace();
            addBotMessage("Error processing conversational response: " + e.getMessage());
            resetConversationState();
        }
    }

    /**
     * Process complete conversational query
     */
    private void processCompleteConversationalQuery(ConversationSessionManager.ConversationResult result) {
        try {
            // Extract the complete query from conversation context
            String completeQuery = result.getConversation().getOriginalQuery();

            // Process using the main NLP handler
            String response = nlpHandler.processUserInputJSONResponse(completeQuery, sessionId);
            addBotMessage(extractDataProviderResponse(response));

        } catch (Exception e) {
            addBotMessage("Error processing complete query: " + e.getMessage());
        }
    }


    /**
     * Reset conversation state
     */
    private void resetConversationState() {
        isWaitingForUserInput = false;
        expectedResponseType = null;
        currentConversationId = null;
        conversationContext.clear();
    }

    /**
     * Process contract creation flow step
     */
    private void processContractCreationFlowStep() {
        try {
            if (flowState == null) {
                addBotMessage("Contract creation session has expired. Please start over.");
                endContractCreationFlow();
                return;
            }

            // Process the current step
            String stepResult = nlpHandler.processContractCreationStep(userInput, flowState);
            addBotMessage(stepResult);

            // Update flow state
            updateFlowState();

            // Check if flow is complete
            String step = (String) flowState.get("step");
            if ("CONFIRM_CREATION".equals(step)) {
                if (stepResult.contains("Contract Creation Failed")) {
                    endContractCreationFlow();
                } else if (stepResult.contains("Contract Creation Cancelled")) {
                    endContractCreationFlow();
                } else if (stepResult.contains("Contract number")) {
                    endContractCreationFlow();
                }
            }

        } catch (Exception e) {
            addBotMessage("Error in contract creation step: " + e.getMessage());
        }
    }

    /**
     * Check if input is a contract creation query
     */
    private boolean isContractCreationQuery(String userInput) {
        if (userInput == null)
            return false;
        String lowerInput = userInput.toLowerCase();

        // Check for creation keywords
        boolean hasCreationKeywords =
            lowerInput.contains("create") || lowerInput.contains("new") || lowerInput.contains("add") ||
            lowerInput.contains("make") || lowerInput.contains("build") || lowerInput.contains("generate") ||
            lowerInput.contains("setup") || lowerInput.contains("establish");

        // Check for contract keywords
        boolean hasContractKeywords = lowerInput.contains("contract") || lowerInput.contains("contarct") || // misspelling
            lowerInput.contains("agreement") || lowerInput.contains("deal");

        // Check for account number pattern (7+ digits per business rules)
        boolean hasAccountNumber = lowerInput.matches(".*\\b\\d{7,}\\b.*");

        // Check for account keywords
        boolean hasAccountKeywords = lowerInput.contains("account") || lowerInput.contains("acount") || // misspelling
            lowerInput.contains("customer");

        // Must have creation keywords AND (contract keywords OR account keywords OR account number)
        return hasCreationKeywords && (hasContractKeywords || hasAccountKeywords || hasAccountNumber);
    }

    /**
     * End contract creation flow
     */
    private void endContractCreationFlow() {
        isInContractCreationFlow = false;
        flowState = null;
        clearContractFormFields();

        addBotMessage("<p><b>Contract creation session ended.</b></p>" +
                      "<p>You can start a new contract creation anytime by saying 'create contract' with an account number.</p>");
    }

    /**
     * Update flow state
     */
    private void updateFlowState() {
        if (flowState == null)
            return;

        try {
            // Update current step based on collected data
            String currentStep = (String) flowState.get("step");

            if ("CONTRACT_NAME".equals(currentStep) && flowState.containsKey("contractName")) {
                flowState.put("step", "CONTRACT_TITLE");
                addBotMessage("<p><b>Contract Name:</b> " + flowState.get("contractName") + " OK</p>" +
                              "<p><b>Step 2:</b> What title would you like for this contract?</p>" +
                              "<p><i>Please provide a brief title.</i></p>");
            } else if ("CONTRACT_TITLE".equals(currentStep) && flowState.containsKey("contractTitle")) {
                flowState.put("step", "CONTRACT_DESCRIPTION");
                addBotMessage("<p><b>Contract Title:</b> " + flowState.get("contractTitle") + " OK</p>" +
                              "<p><b>Step 3:</b> Please provide a description for this contract.</p>" +
                              "<p><i>Add detailed information about what this contract covers.</i></p>");
            } else if ("CONTRACT_DESCRIPTION".equals(currentStep) && flowState.containsKey("contractDescription")) {
                flowState.put("step", "OPTIONAL_FIELDS");
                addBotMessage("<p><b>Contract Description:</b> " + flowState.get("contractDescription") + " OK</p>" +
                              "<p><b>Step 4:</b> Optional settings (you can skip these with 'skip' or 'no'):</p>" +
                              "<p><b>Price List Required?</b> (YES/NO, default: NO)</p>" +
                              "<p><b>EM Required?</b> (YES/NO, default: NO)</p>" +
                              "<p><i>Please specify for each, or say 'skip' to use defaults.</i></p>");
            } else if ("OPTIONAL_FIELDS".equals(currentStep) &&
                       (flowState.containsKey("priceList") || flowState.containsKey("isEM"))) {
                flowState.put("step", "DATES");
                addBotMessage("<p><b>Optional Fields:</b> OK</p>" +
                              "<p><b>Step 5:</b> Contract dates (you can skip with 'skip' or 'default'):</p>" +
                              "<p><b>Effective Date:</b> (YYYY-MM-DD, default: today)</p>" +
                              "<p><b>Expiration Date:</b> (YYYY-MM-DD, default: 2 years from today)</p>" +
                              "<p><b>Price Expiration Date:</b> (YYYY-MM-DD, optional)</p>" +
                              "<p><i>Please specify dates in YYYY-MM-DD format, or say 'skip' for defaults.</i></p>");
            } else if ("DATES".equals(currentStep) &&
                       (flowState.containsKey("effectiveDate") || flowState.containsKey("expirationDate"))) {
                flowState.put("step", "CONFIRM_CREATION");
                addBotMessage("<p><b>Contract Dates:</b> OK</p>" +
                              "<p><b>Step 6:</b> Please review the contract details:</p>" + formatContractSummary() +
                              "<p><b>Ready to create?</b> Type 'yes' to create the contract, or 'no' to cancel.</p>");
            }

        } catch (Exception e) {
            addBotMessage("Error updating flow state: " + e.getMessage());
        }
    }

    /**
     * Format contract summary for confirmation
     */
    private String formatContractSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0;'>");
        summary.append("<h5>Contract Summary:</h5>");
        summary.append("<p><b>Account Number:</b> ")
               .append(flowState.get("accountNumber"))
               .append("</p>");
        summary.append("<p><b>Contract Name:</b> ")
               .append(flowState.get("contractName"))
               .append("</p>");
        summary.append("<p><b>Contract Title:</b> ")
               .append(flowState.get("contractTitle"))
               .append("</p>");
        summary.append("<p><b>Description:</b> ")
               .append(flowState.get("contractDescription"))
               .append("</p>");
        summary.append("<p><b>Price List:</b> ")
               .append(flowState.getOrDefault("priceList", "NO"))
               .append("</p>");
        summary.append("<p><b>EM Required:</b> ")
               .append(flowState.getOrDefault("isEM", "NO"))
               .append("</p>");
        summary.append("<p><b>Effective Date:</b> ")
               .append(flowState.getOrDefault("effectiveDate", "Today"))
               .append("</p>");
        summary.append("<p><b>Expiration Date:</b> ")
               .append(flowState.getOrDefault("expirationDate", "2 years from today"))
               .append("</p>");
        if (flowState.containsKey("priceExpirationDate")) {
            summary.append("<p><b>Price Expiration:</b> ")
                   .append(flowState.get("priceExpirationDate"))
                   .append("</p>");
        }
        summary.append("</div>");
        return summary.toString();
    }

    /**
     * Extract account number from input
     */
    private String extractAccountNumberFromInput(String userInput) {
        if (userInput == null)
            return null;

        // Pattern for 7+ digit account numbers
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b(\\d{7,})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Clear contract form fields
     */
    private void clearContractFormFields() {
        this.accountNumber = null;
        this.contractName = null;
        this.contractTitle = null;
        this.contractDescription = null;
        this.priceList = "NO";
        this.isEM = "NO";
        this.effectiveDate = null;
        this.expirationDate = null;
        this.priceExpirationDate = null;
        this.isAccountValid = false;
        this.validationMessage = null;
    }

    /**
     * Add user message to chat history
     */
    private void addUserMessage(String message) {
        ChatMessage userMsg = new ChatMessage("You", message, new Date(), false);
        chatHistory.add(userMsg);
    }

    /**
     * Add bot message to chat history
     */
    private void addBotMessage(String message) {
        if (this.chatHistory == null) {
            this.chatHistory = new ArrayList<>();
        }
        ChatMessage botMsg = new ChatMessage("Bot", message, new Date(), true);
        chatHistory.add(botMsg);
    }

    /**
     * Extract data provider response from JSON
     */
    private String extractDataProviderResponse(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return "No response received from the system.";
            }

            // Check if it's a structured JSON response
            if (json.contains("\"dataProviderResponse\"")) {
                // Extract dataProviderResponse field
                int startIndex = json.indexOf("\"dataProviderResponse\":\"");
                if (startIndex != -1) {
                    startIndex += "\"dataProviderResponse\":\"".length();
                    int endIndex = json.indexOf("\"", startIndex);
                    if (endIndex != -1) {
                        String dataResponse = json.substring(startIndex, endIndex);
                        // Unescape JSON string
                        return dataResponse.replace("\\\"", "\"")
                                           .replace("\\n", "\n")
                                           .replace("\\t", "\t");
                    }
                }
            }

            // If not structured JSON, return as is
            return json;

        } catch (Exception e) {
            return "Error extracting response: " + e.getMessage();
        }
    }


    /**
     * Add professional note
     */
    private String addProfessionalNote() {
        return "<hr><p><i><small>Tip: You can ask me about contracts, parts, customers, or help with contract creation. " +
               "For complex queries, I'll guide you through the process step by step.</small></i></p>";
    }

    /**
     * Validate account number
     */
    private void validateAccountNumber(String accountNumber) {
        try {
            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                this.isAccountValid = false;
                this.validationMessage = "Account number is required.";
                return;
            }

            // Check if it's a valid number format (7+ digits)
            if (!accountNumber.matches("\\d{7,}")) {
                this.isAccountValid = false;
                this.validationMessage = "Account number must be 7 or more digits.";
                return;
            }

            // Validate using business logic
            this.isAccountValid = validateCustomer(accountNumber);
            if (this.isAccountValid) {
                this.validationMessage = "Account number is valid.";
            } else {
                this.validationMessage = "Account number not found in system.";
            }

        } catch (Exception e) {
            this.isAccountValid = false;
            this.validationMessage = "Error validating account number: " + e.getMessage();
        }
    }

    /**
     * Handle value change event
     */
    public void handleValueChange(ValueChangeEvent valueChangeEvent) {
        try {
            String newValue = (String) valueChangeEvent.getNewValue();
            String componentId = valueChangeEvent.getComponent().getId();

            if ("accountNumber".equals(componentId)) {
                validateAccountNumber(newValue);
            }

        } catch (Exception e) {
            addBotMessage("Error handling value change: " + e.getMessage());
        }
    }

    /**
     * Reset session
     */
    public void resetSession() {
        try {
            // Clear all session data
            chatHistory.clear();
            flowState = null;
            conversationContext.clear();

            // Reset flags
            isInContractCreationFlow = false;
            isWaitingForUserInput = false;
            expectedResponseType = null;
            currentConversationId = null;

            // Clear form fields
            clearContractFormFields();

            // Reinitialize session
            initializeSession();

        } catch (Exception e) {
            addBotMessage("Error resetting session: " + e.getMessage());
        }
    }

    /**
     * Clear chat history
     */
    public void clearChat() {
        try {
            chatHistory.clear();
            addBotMessage("<p><b>Chat history cleared.</b></p>" + "<p>How can I help you today?</p>");
        } catch (Exception e) {
            addBotMessage("Error clearing chat: " + e.getMessage());
        }
    }

    /**
     * Get contract creation form fields from configuration
     */
    private Map<String, Object> getContractCreationFormFields() {
        Map<String, Object> formFields = new HashMap<>();

        // Required fields
        formFields.put("accountNumber", "Customer Account Number (7+ digits)");
        formFields.put("contractName", "Contract Name (descriptive)");
        formFields.put("contractTitle", "Contract Title (brief)");
        formFields.put("contractDescription", "Contract Description (detailed)");

        // Optional fields with defaults
        formFields.put("priceList", "Price List Required (YES/NO, default: NO)");
        formFields.put("isEM", "EM Required (YES/NO, default: NO)");
        formFields.put("effectiveDate", "Effective Date (YYYY-MM-DD, default: today)");
        formFields.put("expirationDate", "Expiration Date (YYYY-MM-DD, default: 2 years)");
        formFields.put("priceExpirationDate", "Price Expiration Date (YYYY-MM-DD, optional)");

        return formFields;
    }

    /**
     * Validate customer account number
     */
    private boolean validateCustomer(String accountNumber) {
        try {
            // Use the existing validation logic from NLPUserActionHandler
            return nlpHandler.isCustomerNumberValid(accountNumber);
        } catch (Exception e) {
            return false;
        }
    }
    private String _htmInput;


    public void setHtmInput(String _htmInput) {
        this._htmInput = _htmInput;
    }

    public String getHtmInput() {
        return _htmInput;
    }

    /**
     * Process user input (ClientEvent version)
     */
    public void processUserInput(ClientEvent clientEvent) {

        System.out.println("Jai Balayya ---> user input :" + userInput);
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                addBotMessage("Please enter your question or request.");
                return;
            }

            isProcessing = true;
            addUserMessage(userInput);

            // Check if we're waiting for user input in a conversation
            System.out.println("Check if we're waiting for user input in a conversation isWaitingForUserInput: " +
                               isWaitingForUserInput + " , and currentConversationId :" + currentConversationId);
            if (isWaitingForUserInput && currentConversationId != null) {
                System.out.println("waiting for user response---->");
                processConversationalResponse();
            } else {
                // Check if ConversationalNLPManager session is waiting for user input
                // This handles user selections like "1", "2", "3" for "created by" queries
                String conversationId = (currentConversationId != null) ? currentConversationId : sessionId;
                if (conversationalNLPManager.isSessionWaitingForUserInput(conversationId)) {
                    processConversationalResponse();
                } else {
                    // Check if this is a new conversation or direct query
                    processNewUserInput();
                }
            }

            // Clear input after processing
            userInput = "";

        } catch (Exception e) {
            e.printStackTrace();
            addBotMessage("Error processing your request: " + e.getMessage());
        } finally {
            isProcessing = false;
        }
    }

    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================

    public List<ChatMessage> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatMessage> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getBotResponse() {
        return botResponse;
    }

    public void setBotResponse(String botResponse) {
        this.botResponse = botResponse;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCurrentFlowStep() {
        return currentFlowStep;
    }

    public void setCurrentFlowStep(String currentFlowStep) {
        this.currentFlowStep = currentFlowStep;
    }

    public boolean isInContractCreationFlow() {
        return isInContractCreationFlow;
    }

    public void setInContractCreationFlow(boolean inContractCreationFlow) {
        this.isInContractCreationFlow = inContractCreationFlow;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getContractTitle() {
        return contractTitle;
    }

    public void setContractTitle(String contractTitle) {
        this.contractTitle = contractTitle;
    }

    public String getContractDescription() {
        return "Jai Balayya";
    }

    public void setContractDescription(String contractDescription) {
        this.contractDescription = contractDescription;
    }

    public String getPriceList() {
        return priceList;
    }

    public void setPriceList(String priceList) {
        this.priceList = priceList;
    }

    public String getIsEM() {
        return isEM;
    }

    public void setIsEM(String isEM) {
        this.isEM = isEM;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getPriceExpirationDate() {
        return priceExpirationDate;
    }

    public void setPriceExpirationDate(String priceExpirationDate) {
        this.priceExpirationDate = priceExpirationDate;
    }

    public boolean isAccountValid() {
        return isAccountValid;
    }

    public void setAccountValid(boolean accountValid) {
        this.isAccountValid = accountValid;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public String getCurrentActionType() {
        return currentActionType;
    }

    public void setCurrentActionType(String currentActionType) {
        this.currentActionType = currentActionType;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setProcessing(boolean processing) {
        this.isProcessing = processing;
    }

    public RichInputText getUserInputText() {
        return userInputText;
    }

    public void setUserInputText(RichInputText userInputText) {
        this.userInputText = userInputText;
    }

    public RichOutputText getBotResponseText() {
        return botResponseText;
    }

    public void setBotResponseText(RichOutputText botResponseText) {
        this.botResponseText = botResponseText;
    }

    // Enhanced conversational properties
    public boolean isWaitingForUserInput() {
        return isWaitingForUserInput;
    }

    public void setWaitingForUserInput(boolean waitingForUserInput) {
        this.isWaitingForUserInput = waitingForUserInput;
    }

    public String getExpectedResponseType() {
        return expectedResponseType;
    }

    public void setExpectedResponseType(String expectedResponseType) {
        this.expectedResponseType = expectedResponseType;
    }

    public String getCurrentConversationId() {
        return currentConversationId;
    }

    public void setCurrentConversationId(String currentConversationId) {
        this.currentConversationId = currentConversationId;
    }

    public Map<String, Object> getConversationContext() {
        return conversationContext;
    }

    public void setConversationContext(Map<String, Object> conversationContext) {
        this.conversationContext = conversationContext;
    }

    /**
     * Check if the HTML response contains useractionrequired="true" div
     * @param htmlResponse The HTML response string to check
     * @return true if useractionrequired="true" is found, false otherwise
     */
    public boolean hasUserActionRequired(String htmlResponse) {
        if (htmlResponse == null || htmlResponse.trim().isEmpty()) {
            return false;
        }

        // Check for the specific div with useractionrequired="true"
        return htmlResponse.contains("<div useractionrequired=\"true\">");
    }

    /**
     * Remove the useractionrequired div tags completely from HTML response
     * @param htmlResponse The HTML response string to process
     * @return The HTML response with useractionrequired div tags removed
     */
    public String removeUserActionRequiredTags(String htmlResponse) {
        if (htmlResponse == null || htmlResponse.trim().isEmpty()) {
            return htmlResponse;
        }

        // Remove the opening div tag
        String result = htmlResponse.replaceAll("<div useractionrequired=\"true\">", "");

        // Remove the closing div tag
        result = result.replaceAll("</div>", "");

        return result;
    }

    /**
     * for testing inyternal as testing from UI will take longer time like start the internal server and deploy the application and navigates to screen and test
     *
     */

   

    public String processUserInputAction() {

        System.out.println("Jai Balayya --->" + getHtmInput());
        try {
            if (userInput == null || userInput.trim().isEmpty()) {
                addBotMessage("Please enter your question or request.");
                return null;
            }

            isProcessing = true;
            addUserMessage(userInput);

            // --- Robust contract creation session management ---
            if (isContractCreationQuery(userInput)) {
                if (currentConversationId == null || currentConversationId.isEmpty()) {
                    currentConversationId = "conv_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
                }
            }
            if (isInContractCreationFlow && (currentConversationId == null || currentConversationId.isEmpty())) {
                addBotMessage("Session error: Please start contract creation again.");
                return null;
            }
            // --- End robust session management ---

            // Check if we're waiting for user input in a conversation
            System.out.println("Check if we're waiting for user input in a conversation isWaitingForUserInput: " +
                               isWaitingForUserInput + " , and currentConversationId :" + currentConversationId);
            if (isWaitingForUserInput && currentConversationId != null) {
                System.out.println("waiting for user response---->");
                processConversationalResponse();
            } else {
                // Check if ConversationalNLPManager session is waiting for user input
                // This handles user selections like "1", "2", "3" for "created by" queries
                String conversationId = (currentConversationId != null) ? currentConversationId : sessionId;
                if (conversationalNLPManager.isSessionWaitingForUserInput(conversationId)) {
                    processConversationalResponse();
                } else {
                    // Check if this is a new conversation or direct query
                    processNewUserInput();
                }
            }

            // Clear input after processing
            userInput = "";

        } catch (Exception e) {
            addBotMessage("Error processing your request: " + e.getMessage());
        } finally {
            isProcessing = false;
        }
        return null;
    }

    public static void main(String v[]) {
        BCCTContractManagementNLPBean ben = new BCCTContractManagementNLPBean();
        ben.userInput = "create contract";
        ben.sessionId = "e16902bf-273f-418c-86ca-970b79b31ebc";
        //ben.processNewUserInput();
        
        // Test the data extraction
        ben.userInput = "contarcts created by vinod";
        ben.processNewUserInput();
    }
}
