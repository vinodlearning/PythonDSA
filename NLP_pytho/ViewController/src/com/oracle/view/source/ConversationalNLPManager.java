package com.oracle.view.source;

import com.oracle.view.source.ConversationSession.DataExtractionResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import java.util.stream.Collectors;

/**
 * Conversational NLP Manager for ADF Chatbot
 * Integrates modular NLP system with conversational flow management
 *
 * Architecture:
 * UI (JSFF) → BCCTContractManagementNLPBean → ConversationalNLPManager →
 * [NLPQueryClassifier + ConversationalFlowManager] → DB
 */
public class ConversationalNLPManager {
    private static final String CONFIRMATION_MESSAGE =
        "<br/><br/><p><b>Please confirm the contract details above.</b></p>" +
        "<p>Type <b>'Yes'</b> to confirm or <b>'No'</b> to modify.</p>";
    private final NLPQueryClassifier nlpClassifier;
    private final ConversationalFlowManager flowManager;
    private final SpellCorrector spellCorrector;
    private final Lemmatizer lemmatizer;

    // Session management for multi-turn conversations
    private final ConversationSessionManager sessionManager = ConversationSessionManager.getInstance();

    public ConversationalNLPManager() {
        this.nlpClassifier = new NLPQueryClassifier();
        this.flowManager = new ConversationalFlowManager();
        this.spellCorrector = new SpellCorrector();
        this.lemmatizer = Lemmatizer.getInstance();
    }

    /**
     * Main entry point for ADF chatbot
     * Handles both new queries and continuation of existing conversations
     */
    public ChatbotResponse processUserInput(String userInput, String sessionId, String userId) {

        ConversationSession session = getOrCreateSession(sessionId, userId);
        System.out.println("DEBUG: currentFlowType at start of processUserInput: " + session.getCurrentFlowType());
        // Check for cancel/break/terminate at any point
        String lowerInput = userInput.trim().toLowerCase();
        if (lowerInput.equals("cancel") || lowerInput.equals("break") || lowerInput.equals("terminate")) {
            session.setCurrentFlowType(null);
            session.setWaitingForUserInput(false);
            session.setCreateContractInitiated(false);
            session.setChecklistStatus(null);
            session.getChecklistFieldMap().clear();
            session.getContractFieldMap().clear();
            session.getContext().clear();
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = "Process cancelled. You can start a new request.";
            return response;
        }

        if ("CREATE_CHECKLIST".equals(session.getCurrentFlowType())) {
            if (userInput.trim().equalsIgnoreCase("no")) {
                // Cancel checklist flow and reset session state (but keep session id)
                session.setCurrentFlowType(null);
                session.setWaitingForUserInput(false);
                session.getContext().clear();
                session.getCollectedData().clear();
                session.clearCache();
                // Show default welcome/help message
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                    "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                    "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                    "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                    "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                    "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                    "<li>Failed contracts</li>" +
                    "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                    "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                    "<p><i>How can I help you today?</i></p>";
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_CANCELLED";
                return response;
            } else {
                System.out.println("DEBUG: Routing to processChecklistInput because currentFlowType=CREATE_CHECKLIST");
                return processChecklistInput(userInput, session);
            }
        }
        System.out.println("ConversationalNLPManager.processUserInput==========>userInput:" + userInput +
                           " , sessionId: " + sessionId + " , userId:" + userId);
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Check for active conversation session
            ConversationSessionManager.UserSession userSession = sessionManager.getUserOrCreateSession(sessionId);
            ConversationSession session1 =
                (ConversationSession) userSession.getSessionData()
                .computeIfAbsent("conversationSession", k -> new ConversationSession(sessionId, userId));

            // If contract creation flow is already active, always route to contract creation flow
            if ("CONTRACT_CREATION".equals(session.getCurrentFlowType())) {
                return handleContractCreationFlow(userInput, session);
            }

            // Step 2: NLP intent/entity extraction and disambiguation (only for new sessions)
            NLPQueryClassifier.QueryResult nlpResult = nlpClassifier.classifyWithDisambiguation(userInput);
            System.out.println("Query Result Intent===" + nlpResult.intent);
            // System.out.println(BCCTChatBotUtility.repeatString("+", 500));
            BCCTChatBotUtility.printNLPQueryClassifierResult(nlpResult);
            // System.out.println(BCCTChatBotUtility.repeatString("+", 500));
            if ("AMBIGUOUS".equals(nlpResult.intent)) {
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse = nlpResult.clarificationPrompt;
                return response;
            } else if ("CREATE_CONTRACT".equals(nlpResult.intent)) {
                // Properly initialize contract creation flow and prompt for first field
                // session.startContractCreationFlow(null); // null if no account number provided
                // Always call with empty string to force prompt
                return handleContractCreationFlow(userInput, session);
            } else if ("SEARCH_CONTRACTS".equals(nlpResult.intent)) {
                // Route to contract search flow (by creator, etc.)
                // For now, just echo the intent and entity
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "Searching contracts created by: " + nlpResult.entitiesMap.get("CREATED_BY");
                return response;
            }
            // Fallback to existing logic
            return processNewQuery(userInput, session, startTime);
        } catch (Exception e) {
            return createErrorResponse("PROCESSING_ERROR", "Failed to process query: " + e.getMessage());
        }
    }


    /**
     * Handle contract creation flow with state management
     */
    private ChatbotResponse handleContractCreationFlow(String userInput, ConversationSession session) {
        System.out.println("handleContractCreationFlow=================>");

        // Initialize session if this is the first time
        if (session.getCurrentFlowType() == null || !"CONTRACT_CREATION".equals(session.getCurrentFlowType())) {
            session.startContractCreationFlow();
        }

        // Checklist prompt logic
        Object awaitingChecklist = session.getContext().get("awaitingChecklistPrompt");
        Object collectingChecklistData = session.getContext().get("collectingChecklistData");
        if (awaitingChecklist != null && (Boolean) awaitingChecklist) {
            String lowerInput = userInput.trim().toLowerCase();
            if (lowerInput.equals("cancel") || lowerInput.equals("terminate") || lowerInput.equals("skip")) {
                session.getContext().remove("awaitingChecklistPrompt");
                session.getContext().remove("collectingChecklistData");
                session.clearCache();
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                    "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                    "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                    "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                    "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                    "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                    "<li>Failed contracts</li>" +
                    "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                    "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                    "<p><i>How can I help you today?</i></p>";
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_CANCELLED";
                return response;
            } else if (lowerInput.equals("yes")) {
                // Set context to collecting checklist data
                return intantiateTheCheckListProcess(session);

            } else if (lowerInput.equals("no")) {
                session.getContext().remove("awaitingChecklistPrompt");
                session.getContext().remove("collectingChecklistData");
                session.clearCache();
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                    "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                    "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                    "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                    "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                    "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                    "<li>Failed contracts</li>" +
                    "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                    "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                    "<p><i>How can I help you today?</i></p>";
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_DECLINED";
                return response;
            } else {
                // Invalid response, prompt again
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "<b>Please respond with 'Yes' or 'No' to start the checklist process.</b>";
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_CONFIRM";
                return response;
            }
        } else if (collectingChecklistData != null && (Boolean) collectingChecklistData) {
            String lowerInput = userInput.trim().toLowerCase();
            if (lowerInput.equals("cancel") || lowerInput.equals("terminate") || lowerInput.equals("skip")) {
                session.getContext().remove("collectingChecklistData");
                session.clearCache();
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                    "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                    "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                    "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                    "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                    "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                    "<li>Failed contracts</li>" +
                    "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                    "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                    "<p><i>How can I help you today?</i></p>";
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_CANCELLED";
                return response;
            }
            // Parse checklist input (field: value pairs)
            Map<String, String> checklistData = session.getChecklistFieldMap();
            String[] pairs = userInput.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim()
                                      .toUpperCase()
                                      .replace(" ", "_");
                    String value = kv[1].trim();
                    checklistData.put(key, value);
                }
            }
            // Validate checklist data
            Map<String, String> validationResult = BCCTChatBotUtility.validateCheckListDates(checklistData);
            List<String> invalidFields = new ArrayList<>();
            StringBuilder errorMsg = new StringBuilder();
            for (String field : ContractFieldConfig.CHECKLIST_FIELDS) {
                String result = validationResult.get(field);
                if (!"success".equalsIgnoreCase(result)) {
                    invalidFields.add(field);
                    errorMsg.append(ContractFieldConfig.getChecklistDisplayName(field))
                            .append(": ")
                            .append(result)
                            .append("<br>");
                }
            }
            if (!invalidFields.isEmpty()) {
                // Prompt for invalid fields again
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse = "<b>Please correct the following fields:</b><br>" + errorMsg.toString();
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_FIELD_ERROR";
                return response;
            } else {
                // All valid, proceed to checklist creation
                Map<String, Object> checklistResult =
                    NLPUserActionHandler.getInstance().checkListCreation(checklistData);
                System.out.println("jjjjj");
                String status = String.valueOf(checklistResult.get("STATUS"));
                String msg = String.valueOf(checklistResult.get("MESSAGE"));
                ChatbotResponse response = new ChatbotResponse();
                if ("SUCCESS".equalsIgnoreCase(status)) {
                    response.isSuccess = true;
                    response.dataProviderResponse = "<b>Checklist creation successful.</b>";
                } else {
                    response.isSuccess = false;
                    response.dataProviderResponse = "<b>Checklist creation failed:</b> " + msg;
                }
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "HELP";
                response.metadata.actionType = "CHECKLIST_COMPLETE";
                // Reset session after checklist
                session.getContext().remove("collectingChecklistData");
                session.clearCache();
                return response;
            }
        }

        // Check if awaiting confirmation
        Object awaiting = session.getContext().get("awaitingContractConfirmation");
        if (awaiting != null && (Boolean) awaiting) {
            String lowerInput = userInput.toLowerCase();
            if (lowerInput.equals("yes") || lowerInput.equals("confirm") || lowerInput.equals("ok")) {
                session.getContext().remove("awaitingContractConfirmation");
                return handleContractCreationConfirmation(session);
            } else if (lowerInput.equals("no") || lowerInput.equals("cancel")) {
                session.getContext().remove("awaitingContractConfirmation");
                session.setCreateContractInitiated(false);
                session.getCollectedData().clear();
                session.getContext().clear();
                // Show default welcome/help message
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse =
                    "<p><b>Hello! I'm your BCCT Contract Management Assistant with enhanced conversational capabilities.</b></p>" +
                    "<br>" + "<p>You can ask me about:</p>" + "<ul>" +
                    "<li>Contract information (e.g., 'Show contract ABC123')</li>" +
                    "<li>Parts information (e.g., 'How many parts for XYZ456')</li>" +
                    "<li>Contract status (e.g., 'Status of ABC123')</li>" +
                    "<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>" +
                    "<li>Failed contracts</li>" +
                    "<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>" +
                    "<li>Opportunities (e.g., 'Show opportunities')</li>" + "</ul>" + "<br>" +
                    "<p><i>How can I help you today?</i></p>";
                return response;
            } else {
                // Prompt again for valid confirmation
                String summary = BCCTChatBotUtility.getContractSummary(session);
                String confirmPrompt =
                    summary +
                    "<br/><br/><p><b>Please confirm the contract details above.</b></p><p>Type <b>'Yes'</b> to confirm or <b>'No'</b> to modify.</p>";
                session.getContext().put("awaitingContractConfirmation", true);
                System.out.println("[DEBUG] Re-set awaitingContractConfirmation=true in session context");
                ConversationalNLPManager.ChatbotResponse response = new ConversationalNLPManager.ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse = confirmPrompt;
                return response;
            }
        }
        System.out.println("Calling session method to extract the data");
        // Process user input and extract data
        ConversationSession.DataExtractionResult extractionResult = session.processUserInput(userInput);
        System.out.println("end of the session method ====");
        System.out.println("Extracted Fiels----->" + extractionResult.extractedFields);
        System.out.println("Remaining fileds Fiels----->" + extractionResult.remainingFields);
        System.out.println("Valdiation errors Count  ====" + extractionResult.validationErrors.size());
        System.out.println("Valdiation errors are there ====" + extractionResult.validationErrors);

        return contractDataExtractionHandling(session, extractionResult);
    }

    /**
     * Generate contract creation prompt for missing fields
     */
    private String generateContractCreationPrompt(List filedNames) {
        System.out.println("Start of generateContractCreationPrompt------" + filedNames);
        try {
            List userField = new ArrayList();
            for (Object fild : filedNames) {
                userField.add(ContractFieldConfig.FIELD_DISPLAY_NAMES.get(fild));
            }
            return BCCTChatBotUtility.constructPromptMessage(userField);
        } catch (Exception e) {
            // TODO: Add catch code
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Format contract summary for confirmation
     */
    private String formatContractSummary(ConversationSession session) {
        Map<String, Object> collectedData = session.getCollectedData();
        StringBuilder summary =
            new StringBuilder("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0;'>");
        summary.append("<h5>Contract Summary:</h5>");

        summary.append("<p><b>Account Number:</b> ")
               .append(collectedData.get("ACCOUNT_NUMBER"))
               .append("</p>");
        summary.append("<p><b>Contract Name:</b> ")
               .append(collectedData.get("CONTRACT_NAME"))
               .append("</p>");
        summary.append("<p><b>Title:</b> ")
               .append(collectedData.get("TITLE"))
               .append("</p>");
        summary.append("<p><b>Description:</b> ")
               .append(collectedData.get("DESCRIPTION"))
               .append("</p>");
        summary.append("<p><b>Comments:</b> ")
               .append(collectedData.get("COMMENTS"))
               .append("</p>");
        summary.append("<p><b>Price List:</b> ")
               .append(collectedData.get("IS_PRICELIST"))
               .append("</p>");

        summary.append("</div>");
        return summary.toString();
    }

    /**
     * Process new query with modular NLP system
     */
    private ChatbotResponse processNewQuery(String userInput, ConversationSession session, long startTime) {
        System.out.println("processNewQuery=====>userInput: " + userInput + ", session is session ID" +
                           session.getSessionId());

        // Step 1: Let NLPQueryClassifier handle ALL query classification and entity extraction
        System.out.println("<Calling nlpClassifier.processQuery---------------ConversationalNLPManager-------------------->");
        NLPQueryClassifier.QueryResult nlpResult = nlpClassifier.processQuery(userInput);
        System.out.println("processNewQuery method in ConversationalNLPManager==>");
        BCCTChatBotUtility.printNLPQueryClassifierResult(nlpResult);
        System.out.println("<end nlpClassifier.processQuery---------------ConversationalNLPManager-------------------->");

        // Step 2: Route based on the action type already determined by NLPQueryClassifier
        String actionType = nlpResult.metadata.actionType;
        String queryType = nlpResult.metadata.queryType;

        // Check if this is a quick action query
        if ("QUICK_ACTION".equals(queryType)) {
            System.out.println("Quick action query detected - routing to quick action handler");
            return handleQuickActionQuery(userInput, nlpResult, session, startTime);
        }

        // Check if this is a contract creation query (already classified by ContractProcessor)
        if (NLPConstants.isHelpQuery(queryType) &&
            (NLPConstants.isBotContractCreationAction(actionType) ||
             NLPConstants.isUserContractCreationAction(actionType))) {
            System.out.println("Contract creation query detected - routing to conversational flow");
            return handleConversationalQuery(userInput, nlpResult, session);
        }

        // Check if this requires conversational flow based on action type
        if (NLPConstants.isHelpQuery(queryType)) {
            System.out.println("Help query detected - routing to conversational flow");
            return handleConversationalQuery(userInput, nlpResult, session);
        }

        // Step 3: Handle direct query (non-conversational)
        System.out.println("Direct query detected - routing to direct handler");
        return handleDirectQuery(userInput, nlpResult, session, startTime);
    }

    /**
     * Handle conversational queries (contract creation, help, etc.)
     */
    private ChatbotResponse handleConversationalQuery(String userInput, NLPQueryClassifier.QueryResult nlpResult,
                                                      ConversationSession session) {
        // Check if this is a contract creation query based on NLPQueryClassifier results
        if (nlpResult.metadata.actionType != null &&
            (nlpResult.metadata
                                                               .actionType
                                                               .contains("HELP_CONTRACT_CREATE") ||
             nlpResult.metadata
                                                                                                             .actionType
                                                                                                             .contains("create"))) {
            return handleContractCreationQuery(userInput, session.getSessionId(), nlpResult.metadata.actionType);
        }

        // For other conversational queries, use flow manager
        return flowManager.startConversation(userInput, nlpResult, session);
    }

    /**
     * Handle quick action queries
     */
    private ChatbotResponse handleQuickActionQuery(String userInput, NLPQueryClassifier.QueryResult nlpResult,
                                                   ConversationSession session, long startTime) {
        System.out.println("handleQuickActionQuery================>");

        try {
            // Get the action type from NLP result
            String actionType = nlpResult.metadata.actionType;
            System.out.println("Quick Action Type: " + actionType);

            // Call the quick action handler
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
            String htmlResponse = handler.handleQuickActionButton(actionType);

            // Create response
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;

            // Set metadata
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "QUICK_ACTION";
            response.metadata.actionType = actionType;
            response.metadata.processingTimeMs = System.currentTimeMillis() - startTime;
            response.metadata.confidence = 1.0; // High confidence for fixed prompts

            // Set input tracking
            response.inputTracking = new InputTrackingInfo();
            response.inputTracking.originalInput = userInput;
            response.inputTracking.correctedInput = nlpResult.inputTracking.correctedInput;
            response.inputTracking.correctionConfidence = nlpResult.inputTracking.correctionConfidence;

            // Set entities and display entities
            response.entities = convertToEntityFilters(nlpResult.entities);
            response.displayEntities = nlpResult.displayEntities;

            // Set errors
            response.errors = convertToValidationErrors(nlpResult.errors);

            // Set the HTML response as data
            response.data = htmlResponse;
            response.dataProviderResponse = htmlResponse;

            System.out.println("Quick action response generated successfully");
            return response;

        } catch (Exception e) {
            System.out.println("Error in handleQuickActionQuery: " + e.getMessage());
            return createErrorResponse("QUICK_ACTION_ERROR", "Failed to process quick action: " + e.getMessage());
        }
    }

    /**
     * Handle direct queries (parts, contracts, failed parts)
     */
    private ChatbotResponse handleDirectQuery(String userInput, NLPQueryClassifier.QueryResult nlpResult,
                                              ConversationSession session, long startTime) {
        System.out.println("handleDirectQuery==============>");
        // Create response using existing NLPUserActionHandler logic
        ChatbotResponse response = new ChatbotResponse();

        // Set metadata
        response.metadata = new ResponseMetadata();
        response.metadata.queryType = nlpResult.metadata.queryType;
        response.metadata.actionType = nlpResult.metadata.actionType;
        response.metadata.processingTimeMs = System.currentTimeMillis() - startTime;
        response.metadata.confidence = calculateConfidence(nlpResult);

        // Set input tracking
        response.inputTracking = new InputTrackingInfo();
        response.inputTracking.originalInput = userInput;
        response.inputTracking.correctedInput = nlpResult.inputTracking.correctedInput;
        response.inputTracking.correctionConfidence = nlpResult.inputTracking.correctionConfidence;

        // Set entities and display entities
        response.entities = convertToEntityFilters(nlpResult.entities);
        response.displayEntities = nlpResult.displayEntities;

        // Set errors
        response.errors = convertToValidationErrors(nlpResult.errors);

        // Generate SQL and execute query (using existing logic)
        if (response.errors.isEmpty()) {
            try {
                // This would integrate with your existing SQL generation logic
                response.data = executeQuery(nlpResult);
                response.isSuccess = true;

                // Check if response indicates user selection is needed
                if (response.data != null && response.data
                                                     .toString()
                                                     .contains("Please select a user:")) {
                    session.setWaitingForUserInput(true);
                    response.useractionrequired = true; // Set useractionrequired to true
                    System.out.println("Session set to waiting for user input");

                    // Store user search results in session for future user selection
                    NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
                    Map<String, List<Map<String, String>>> userResults = handler.getUserSearchCache();
                    if (userResults != null && !userResults.isEmpty()) {
                        session.storeUserSearchResults(userResults);
                        System.out.println("User search results stored in session: " + userResults.keySet());
                    }
                }

            } catch (Exception e) {
                response.errors.add(new ValidationError("QUERY_EXECUTION_ERROR", e.getMessage(), "ERROR"));
                response.isSuccess = false;
            }
        } else {
            response.isSuccess = false;
        }

        return response;
    }

    /**
     * Check if user is trying to start a new query
     */
    public boolean isNewQueryAttempt(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // Keywords that indicate new query
        String[] newQueryKeywords = {
            "show", "get", "list", "find", "display", "what", "how many", "count", "failed", "error", "parts",
            "contract", "contracts", "customer", "price", "status"
        };

        // Special check for "created by" pattern
        if (lowerInput.contains("created") && lowerInput.contains("by")) {
            return true; // This is definitely a new query
        }

        for (String keyword : newQueryKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if input looks like an account number (6+ digits)
     */
    public boolean isAccountNumberInput(String userInput) {
        if (userInput == null)
            return false;

        // Check if it's a number with 6+ digits (account number pattern)
        return userInput.matches("^\\d{6,}$");
    }

    /**
     * Check if input is a user selection (number or full name)
     */
    public boolean isUserSelection(String input) {
        System.out.println("isUserSelection..................");
        if (input == null)
            return false;

        // Check if it's a comma-separated contract creation input
        if (input.contains(",")) {
            String[] parts = input.split(",");
            if (parts.length >= 4) {
                // Check if first part is account number (6+ digits)
                String firstPart = parts[0].trim();
                boolean startsWithAccountNumber = firstPart.matches("\\d{6,}");

                // Check if no field labels (not a query)
                String lowerInput = input.toLowerCase();
                boolean hasFieldLabels =
                    lowerInput.contains("account") || lowerInput.contains("name") || lowerInput.contains("title") ||
                    lowerInput.contains("description") || lowerInput.contains("comments") ||
                    lowerInput.contains("pricelist");

                if (!hasFieldLabels && startsWithAccountNumber) {
                    return false; // This is contract creation data, not user selection
                }
            }
        }

        // Check if it's a number (1, 2, 3, etc.)
        if (input.matches("^\\d+$")) {
            return true;
        }

        // Check if it's a full name (contains space) but not a query
        if (input.contains(" ")) {
            // If it contains query keywords, it's not a user selection
            String lowerInput = input.toLowerCase();
            String[] queryKeywords = {
                "show", "get", "list", "find", "display", "what", "how many", "count", "failed", "error", "parts",
                "contract", "customer", "price", "status", "created", "by", "in", "between", "from", "to", "contracts"
            };

            // Special check for "created by" pattern
            if (lowerInput.contains("created") && lowerInput.contains("by")) {
                return false; // This is definitely a query, not a user selection
            }

            // Check for contract-related queries
            if (lowerInput.contains("contract") || lowerInput.contains("contracts")) {
                return false; // This is a contract query, not a user selection
            }

            for (String keyword : queryKeywords) {
                if (lowerInput.contains(keyword)) {
                    return false; // This is a query, not a user selection
                }
            }

            // If no query keywords found, it might be a user selection
            return true;
        }

        return false;
    }

    /**
     * Check if input is meaningless (just numbers, empty, or doesn't contain meaningful content)
     */
    private boolean isMeaninglessInput(String userInput) {
        System.out.println("isMeaninglessInput=========>userInput" + userInput);
        if (userInput == null || userInput.trim().isEmpty()) {
            return true;
        }

        String trimmedInput = userInput.trim();

        // Check if it's just a number (1, 2, 3, etc.) without context
        if (trimmedInput.matches("^\\d+$")) {
            return true;
        }

        // Check if it's just a single word that's not a meaningful query
        if (trimmedInput.split("\\s+").length == 1) {
            String lowerInput = trimmedInput.toLowerCase();

            // Check if it's a meaningful single word
            String[] meaningfulWords = {
                "help", "contracts", "contract", "parts", "failed", "error", "status", "create", "show", "get", "list"
            };

            for (String word : meaningfulWords) {
                if (lowerInput.contains(word)) {
                    return false; // This is meaningful
                }
            }

            // If it's just a single word and not meaningful, it's probably meaningless
            return true;
        }

        return false;
    }


    /**
     * Explicit contract creation query detection - ONLY for clear contract creation requests
     */
    public boolean isExplicitContractCreationQuery(String userInput) {
        System.out.println("isExplicitContractCreationQuery==>");
        String lowerInput = userInput.toLowerCase();

        // Check for explicit creation keywords (PRESENT TENSE ONLY)
        String[] creationKeywords = {
            "create contract", "make contract", "generate contract", "build contract", "set up contract",
            "new contract", "start contract", "initiate contract", "draft contract", "establish contract",
            "form contract", "develop contract"
        };

        for (String keyword : creationKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }

        // Check for single word "create" with contract context ONLY (PRESENT TENSE)
        // EXCLUDE past tense words like "created", "made", "generated", etc.
        if (lowerInput.contains("create") && lowerInput.contains("contract") && !lowerInput.contains("created") &&
            !lowerInput.contains("making") && !lowerInput.contains("generated") && !lowerInput.contains("built")) {
            return true;
        }

        // Check for comma-separated format that looks like contract creation data
        if (userInput.contains(",")) {
            String[] parts = userInput.split(",");
            if (parts.length >= 4) {
                String firstPart = parts[0].trim();
                // Only if it starts with account number AND has contract creation keywords (PRESENT TENSE)
                if (firstPart.matches("\\d{6,}") &&
                    (lowerInput.contains("contract") &&
                     (lowerInput.contains("create") && !lowerInput.contains("created")))) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Enhanced contract creation intent classification
     */
    private String classifyContractCreationIntent(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // Check if account number is provided
        boolean hasAccountNumber = lowerInput.matches(".*\\d{6,}.*");

        // Check if this is a complete input with all details
        boolean isCompleteInput = isCompleteContractCreationInput(userInput);

        if (isCompleteInput) {
            return "CONTRACT_CREATION_COMPLETE";
        } else if (hasAccountNumber) {
            return "CONTRACT_CREATION_WITH_ACCOUNT";
        } else {
            return "CONTRACT_CREATION_WITHOUT_ACCOUNT";
        }
    }

    /**
     * Check if input contains complete contract creation data
     */
    public boolean isCompleteContractCreationInput(String userInput) {
        // Check for comma-separated format first
        if (userInput.contains(",")) {
            String[] parts = userInput.split(",");
            if (parts.length >= 4) {
                // Check if first part is account number (6+ digits)
                String firstPart = parts[0].trim();
                boolean hasAccountNumber = firstPart.matches("\\d{6,}");

                // Check if we have contract name, title, description
                boolean hasContractName = parts.length >= 2 && !parts[1].trim().isEmpty();
                boolean hasTitle = parts.length >= 3 && !parts[2].trim().isEmpty();
                boolean hasDescription = parts.length >= 4 && !parts[3].trim().isEmpty();

                return hasAccountNumber && hasContractName && hasTitle && hasDescription;
            }
        }

        // Check for space-separated format (backward compatibility)
        String[] words = userInput.split("\\s+");

        // Check if we have enough words for all required fields
        if (words.length >= 4) {
            // Check if first word is account number (6+ digits)
            boolean hasAccountNumber = words[0].matches("\\d{6,}");

            // Check if we have contract name, title, description
            boolean hasContractName = words.length >= 2 && !words[1].matches("\\d+");
            boolean hasTitle = words.length >= 3 && !words[2].matches("\\d+");
            boolean hasDescription = words.length >= 4 && !words[3].matches("\\d+");

            return hasAccountNumber && hasContractName && hasTitle && hasDescription;
        }

        return false;
    }

    /**
     * Enhanced contract creation query handling
     */
    private ChatbotResponse handleContractCreationQuery(String userInput, String sessionId, String actionType) {
        ChatbotResponse response = new ChatbotResponse();

        try {
            // Initialize metadata
            response.metadata = new ResponseMetadata();

            // Use the action type already determined by ContractProcessor
            System.out.println("handleContractCreationQuery - Action Type: " + actionType);

            // Route based on the action type already determined by ContractProcessor
            if (NLPConstants.isBotContractCreationAction(actionType)) {
                // Check if input contains account number
                String accountNumber = extractAccountNumberFromInput(userInput);
                if (accountNumber != null) {
                    return handleContractCreationWithAccount(userInput, sessionId, actionType);
                } else {
                    return handleContractCreationWithoutAccount(userInput, sessionId, actionType);
                }
            } else if (NLPConstants.isUserContractCreationAction(actionType)) {
                // User wants help/instructions for manual creation
                return handleContractCreationWithoutAccount(userInput, sessionId, actionType);
            } else {
                // Default to bot creation without account
                return handleContractCreationWithoutAccount(userInput, sessionId, actionType);
            }

        } catch (Exception e) {
            response.isSuccess = false;
            response.data = "<p><b>Error in contract creation:</b> " + e.getMessage() + "</p>";
            response.dataProviderResponse = response.data.toString();
            return response;
        }
    }

    /**
     * Handle contract creation with account number
     */
    private ChatbotResponse handleContractCreationWithAccount(String userInput, String sessionId, String actionType) {
        System.out.println("handleContractCreationWithAccount==============>");
        ChatbotResponse response = new ChatbotResponse();
        try {
            // Initialize metadata
            response.metadata = new ResponseMetadata();
            // Extract account number
            String accountNumber = extractAccountNumberFromInput(userInput);

            if (accountNumber == null) {
                response.data =
                    "<h4>Warning Account Number Required</h4>" +
                    "<p>Please provide a valid account number (6+ digits) to create a contract.</p>";
                response.dataProviderResponse = response.data.toString();
                return response;
            }

            // Validate account number
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
            if (!handler.isCustomerNumberValid(accountNumber)) {
                response.data =
                    "<h4>Warning Invalid Account Number</h4>" + "<p>The account number <b>" + accountNumber +
                    "</b> is not a valid customer.</p>" +
                    "<p>Please provide a valid customer account number (6+ digits).</p>";
                response.dataProviderResponse = response.data.toString();
                return response;
            }

            // Start contract creation flow in session
            if (sessionId != null) {
                ConversationSession session = getOrCreateSession(sessionId, "default");
                session.startContractCreationFlow(accountNumber);
            }

            // Return prompt for remaining details
            HelpProcessor helpProcessor = new HelpProcessor();
            response.data = helpProcessor.getContractCreationPrompt(actionType, accountNumber);
            response.dataProviderResponse = response.data.toString();

            response.isSuccess = true;
            response.metadata.queryType = NLPConstants.QUERY_TYPE_HELP;
            // Use the action type already determined by ContractProcessor
            response.metadata.actionType = actionType;
            response.metadata.processingTimeMs = 0;
            response.metadata.confidence = 0.95;

            return response;

        } catch (Exception e) {
            response.isSuccess = false;
            response.data = "<p><b>Error starting contract creation:</b> " + e.getMessage() + "</p>";
            response.dataProviderResponse = response.data.toString();
            return response;
        }
    }

    /**
     * Handle contract creation without account number
     */
    private ChatbotResponse handleContractCreationWithoutAccount(String userInput, String sessionId,
                                                                 String actionType) {
        System.out.println("handleContractCreationWithoutAccount================>");
        ChatbotResponse response = new ChatbotResponse();
        try {
            // Initialize metadata
            response.metadata = new ResponseMetadata();

            // Start contract creation flow without account number
            if (sessionId != null) {
                ConversationSession session = getOrCreateSession(sessionId, "default");
                session.startContractCreationFlow();
            }

            // Return prompt for all details
            HelpProcessor helpProcessor = new HelpProcessor();
            response.data = helpProcessor.getContractCreationPrompt(actionType, null);
            response.dataProviderResponse = response.data.toString();

            response.isSuccess = true;
            response.metadata.queryType = NLPConstants.QUERY_TYPE_HELP;
            // Use the action type already determined by ContractProcessor
            response.metadata.actionType = actionType;
            response.metadata.processingTimeMs = 0;
            response.metadata.confidence = 0.95;

            return response;

        } catch (Exception e) {
            response.isSuccess = false;
            response.data = "<p><b>Error providing contract creation steps:</b> " + e.getMessage() + "</p>";
            response.dataProviderResponse = response.data.toString();
            return response;
        }
    }

    /**
     * Extract account number from input
     */
    private String extractAccountNumberFromInput(String userInput) {
        String[] words = userInput.split("\\s+");

        for (String word : words) {
            if (word.matches("\\d{6,}")) {
                return word;
            }
        }

        return null;
    }

    /**
     * Create default help response for meaningless inputs
     */
    private ChatbotResponse createDefaultHelpResponse() {
        return createDefaultHelpResponse(null);
    }

    /**
     * Create contextual help response based on user input
     */
    private ChatbotResponse createDefaultHelpResponse(String userInput) {
        System.out.println("createDefaultHelpResponse========>");
        ChatbotResponse response = new ChatbotResponse();

        StringBuilder helpMessage = new StringBuilder();

        if (userInput != null) {
            String lowerInput = userInput.toLowerCase().trim();

            // Provide contextual help based on user input
            if (lowerInput.equals("help")) {
                helpMessage.append("<p><b>You can ask me about:</b></p>");
                helpMessage.append("<ul>");
                helpMessage.append("<li>Contract information (e.g., 'Show contract ABC123')</li>");
                helpMessage.append("<li>Parts information (e.g., 'How many parts for XYZ456')</li>");
                helpMessage.append("<li>Contract status (e.g., 'Status of ABC123')</li>");
                helpMessage.append("<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>");
                helpMessage.append("<li>Failed contracts</li>");
                helpMessage.append("<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>");
                helpMessage.append("<li>Opportunities (e.g., 'Show opportunities')</li>");
                helpMessage.append("</ul>");
            } else if (lowerInput.equals("contracts") || lowerInput.equals("contract")) {
                helpMessage.append("<p><b>For contracts information:</b></p>");
                helpMessage.append("<ul>");
                helpMessage.append("<li>Show contract 123456</li>");
                helpMessage.append("<li>Show effective, expiration dates for contract 123456</li>");
                helpMessage.append("<li>Show contracts created by Balayya</li>");
                helpMessage.append("<li>Show contracts for customer HONEYWELL</li>");
                helpMessage.append("<li>Show contract status ABC123</li>");
                helpMessage.append("</ul>");
            } else if (lowerInput.equals("parts")) {
                helpMessage.append("<p><b>For parts information:</b></p>");
                helpMessage.append("<ul>");
                helpMessage.append("<li>Show parts for contract 123456</li>");
                helpMessage.append("<li>Show part details XYZ789</li>");
                helpMessage.append("<li>Show parts count for contract ABC123</li>");
                helpMessage.append("<li>Show failed parts for contract 123456</li>");
                helpMessage.append("<li>Show parts status XYZ789</li>");
                helpMessage.append("</ul>");
            } else if (lowerInput.equals("failed")) {
                helpMessage.append("<p><b>For failed parts/contracts:</b></p>");
                helpMessage.append("<ul>");
                helpMessage.append("<li>Show failed parts</li>");
                helpMessage.append("<li>Show failed parts for contract 123456</li>");
                helpMessage.append("<li>Show failed contracts</li>");
                helpMessage.append("<li>Show failed parts count</li>");
                helpMessage.append("<li>Show failed contracts count</li>");
                helpMessage.append("</ul>");
            } else if (lowerInput.equals("create")) {
                helpMessage.append("<p><b>For contract creation:</b></p>");
                helpMessage.append("<p>I'll guide you through the contract creation process. You can:</p>");
                helpMessage.append("<ul>");
                helpMessage.append("<li>Create contract for account 10840607</li>");
                helpMessage.append("<li>Create contract with customer details</li>");
                helpMessage.append("<li>Create contract step by step</li>");
                helpMessage.append("</ul>");
                helpMessage.append("<p><i>Just say 'create contract' and I'll help you through the process!</i></p>");
            } else {
                // Default help for other meaningless inputs
                helpMessage.append("<p><b>You can ask me about:</b></p>");
                helpMessage.append("<ul>");
                helpMessage.append("<li>Contract information (e.g., 'Show contract ABC123')</li>");
                helpMessage.append("<li>Parts information (e.g., 'How many parts for XYZ456')</li>");
                helpMessage.append("<li>Contract status (e.g., 'Status of ABC123')</li>");
                helpMessage.append("<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>");
                helpMessage.append("<li>Failed contracts</li>");
                helpMessage.append("<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>");
                helpMessage.append("<li>Opportunities (e.g., 'Show opportunities')</li>");
                helpMessage.append("</ul>");
            }
        } else {
            // Default help for null/empty inputs
            helpMessage.append("<p><b>You can ask me about:</b></p>");
            helpMessage.append("<ul>");
            helpMessage.append("<li>Contract information (e.g., 'Show contract ABC123')</li>");
            helpMessage.append("<li>Parts information (e.g., 'How many parts for XYZ456')</li>");
            helpMessage.append("<li>Contract status (e.g., 'Status of ABC123')</li>");
            helpMessage.append("<li>Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')</li>");
            helpMessage.append("<li>Failed contracts</li>");
            helpMessage.append("<li>Contract creation (e.g., 'create contract' - I'll guide you through it)</li>");
            helpMessage.append("<li>Opportunities (e.g., 'Show opportunities')</li>");
            helpMessage.append("</ul>");
        }

        response.isSuccess = true;
        response.data = helpMessage.toString();
        response.dataProviderResponse = helpMessage.toString();

        // Set metadata
        response.metadata = new ResponseMetadata();
        response.metadata.queryType = "INQUIRY";
        response.metadata.actionType = "ENQUERY_FAILED";
        response.metadata.processingTimeMs = 0;
        response.metadata.confidence = 1.0;

        // Set input tracking
        response.inputTracking = new InputTrackingInfo();
        response.inputTracking.originalInput = userInput != null ? userInput : "meaningless_input";
        response.inputTracking.correctedInput = userInput != null ? userInput : "meaningless_input";
        response.inputTracking.correctionConfidence = 1.0;

        return response;
    }


    /**
     * Handle user selection using session data (no need to call NLPUserActionHandler)
     */
    private ChatbotResponse handleUserSelectionFromSession(String userInput, ConversationSession session) {
        ChatbotResponse response = new ChatbotResponse();

        try {
            // Check if session has valid user search results
            if (!session.isUserSearchValid()) {
                response.isSuccess = false;
                response.data = "<p><b>User selection expired. Please search again.</b></p>";
                response.dataProviderResponse = response.data.toString();
                session.setWaitingForUserInput(false);
                return response;
            }

            String selectedUser = null;

            // Check if input is a number (1, 2, 3, etc.)
            if (userInput.matches("^\\d+$")) {
                int index = Integer.parseInt(userInput);
                System.out.println("userInput.matches(\"^\\\\d+$\")");
                selectedUser = session.getUserByIndex(index);
                System.out.println("selectedUser==========" + selectedUser);
            } else {
                // Check if input is a user name
                selectedUser = session.getUserByName(userInput);
            }

            if (selectedUser == null) {
                response.isSuccess = false;
                // Get available users for better error message
                List<String> availableUsers = session.getAvailableUsers();
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("<p><b>Invalid selection. Please choose a valid option:</b></p>");
                errorMessage.append("<ol>");
                for (int i = 0; i < availableUsers.size(); i++) {
                    errorMessage.append("<li>")
                                .append(availableUsers.get(i))
                                .append("</li>");
                }
                errorMessage.append("</ol>");
                errorMessage.append("<p><i>Enter the number (1, 2, 3...) or the full name to select a user.</i></p>");

                response.data = errorMessage.toString();
                response.dataProviderResponse = response.data.toString();

                // Set metadata for invalid selection
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = "USER_SELECTION";
                response.metadata.actionType = "invalid_selection";
                response.metadata.processingTimeMs = 0;
                response.metadata.confidence = 1.0;

                // Keep session waiting for user input (don't clear it)
                return response;
            }

            // Get contracts for the selected user from session
            List<Map<String, String>> contracts = session.getContractsForUser(selectedUser);

            if (contracts == null || contracts.isEmpty()) {
                response.isSuccess = false;
                response.data = "<p><i>No contracts found for user '" + selectedUser + "'</i></p>";
                response.dataProviderResponse = response.data.toString();
            } else {
                // Format the contracts data with ADF OutputFormatted compatible HTML
                StringBuilder result = new StringBuilder();
                result.append("<h3>Contracts Created by &quot;")
                      .append(selectedUser)
                      .append("&quot;</h3>");
                result.append("<hr>");

                int recordNumber = 1;
                for (Map<String, String> contract : contracts) {
                    result.append("<h4>Record: ")
                          .append(recordNumber)
                          .append("</h4>");

                    // Format specific fields with proper labels
                    String effectiveDate = contract.get("EFFECTIVE_DATE");
                    String createDate = contract.get("CREATE_DATE");
                    String expirationDate = contract.get("EXPIRATION_DATE");
                    String customerName = contract.get("CUSTOMER_NAME");
                    String awardNumber = contract.get("AWARD_NUMBER");
                    String contractName = contract.get("CONTRACT_NAME");

                    if (effectiveDate != null && !effectiveDate.trim().isEmpty()) {
                        result.append("<p><b>Effective Date:</b> ")
                              .append(effectiveDate)
                              .append("</p>");
                    }
                    if (createDate != null && !createDate.trim().isEmpty()) {
                        result.append("<p><b>Created on:</b> ")
                              .append(createDate)
                              .append("</p>");
                    }
                    if (expirationDate != null && !expirationDate.trim().isEmpty()) {
                        result.append("<p><b>Expiration Date:</b> ")
                              .append(expirationDate)
                              .append("</p>");
                    }
                    if (customerName != null && !customerName.trim().isEmpty()) {
                        result.append("<p><b>Customer Name:</b> ")
                              .append(customerName)
                              .append("</p>");
                    }
                    if (awardNumber != null && !awardNumber.trim().isEmpty()) {
                        result.append("<p><b>Contract Number:</b> ")
                              .append(awardNumber)
                              .append("</p>");
                    }
                    if (contractName != null && !contractName.trim().isEmpty()) {
                        result.append("<p><b>Contract Name:</b> ")
                              .append(contractName)
                              .append("</p>");
                    }

                    result.append("<hr>");
                    recordNumber++;
                }

                response.isSuccess = true;
                response.data = result.toString();
                response.dataProviderResponse = result.toString();
            }

            // Set metadata
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "CONTRACTS";
            response.metadata.actionType = "contracts_by_filter";
            response.metadata.processingTimeMs = 0;
            response.metadata.confidence = 1.0;

            // Set input tracking
            response.inputTracking = new InputTrackingInfo();
            response.inputTracking.originalInput = userInput;
            response.inputTracking.correctedInput = userInput;
            response.inputTracking.correctionConfidence = 1.0;

            // Clear waiting state and user search results since selection is complete
            session.setWaitingForUserInput(false);
            session.clearUserSearchResults();

        } catch (Exception e) {
            response.isSuccess = false;
            response.data = "<p><b>Error processing user selection:</b> " + e.getMessage() + "</p>";
            response.dataProviderResponse = response.data.toString();
        }

        return response;
    }

    /**
     * Check if a session is waiting for user input
     */
    public boolean isSessionWaitingForUserInput(String sessionId) {
        ConversationSession session = (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                                          .getSessionData()
                                                                          .get("conversationSession");
        if (session != null)
            System.out.println("======isSessionWaitingForUserInput=========" + session.isWaitingForUserInput());
        return session != null && session.isWaitingForUserInput();
    }

    /**
     * Set session to wait for user input
     */
    public void setSessionWaitingForUserInput(String sessionId, boolean waiting) {
        ConversationSession session = (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                                          .getSessionData()
                                                                          .get("conversationSession");
        if (session != null) {
            session.setWaitingForUserInput(waiting);
            System.out.println("ConversationalNLPManager: Set session " + sessionId + " waiting for user input: " +
                               waiting);
        } else {
            System.out.println("ConversationalNLPManager: Session " + sessionId +
                               " not found for setting waiting state");
        }
    }

    /**
     * Store user search results in session and set waiting state
     * This method allows external components to store user search results
     */
    public void storeUserSearchResultsInSession(String sessionId, Map<String, List<Map<String, String>>> userResults) {
        ConversationSession session = (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                                          .getSessionData()
                                                                          .get("conversationSession");
        if (userResults != null && !userResults.isEmpty()) {
            session.storeUserSearchResults(userResults);
            session.setWaitingForUserInput(true);
            System.out.println("User search results stored in session " + sessionId + ": " + userResults.keySet());
        }
    }

    /**
     * Store user search results in session with display order
     * This method allows external components to store user search results with proper display order
     */
    public void storeUserSearchResultsInSession(String sessionId, Map<String, List<Map<String, String>>> userResults,
                                                List<String> displayOrder) {
        ConversationSession session = (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                                          .getSessionData()
                                                                          .get("conversationSession");
        if (userResults != null && !userResults.isEmpty()) {
            session.storeUserSearchResults(userResults, displayOrder);
            session.setWaitingForUserInput(true);
            System.out.println("User search results stored in session " + sessionId + " with display order: " +
                               displayOrder);
        }
    }

    /**
     * Store contract search results in session
     * This method allows external components to store contract search results for user selection
     */
    public void storeContractSearchResultsInSession(String sessionId,
                                                    Map<String, List<Map<String, Object>>> contractResults) {
        ConversationSession session = (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                                          .getSessionData()
                                                                          .get("conversationSession");
        if (contractResults != null && !contractResults.isEmpty()) {
            session.storeContractSearchResults(contractResults);
            session.setWaitingForUserInput(true);
            System.out.println("Contract search results stored in session " + sessionId + ": " +
                               contractResults.keySet());
        }
    }

    /**
     * Get or create conversation session
     */
    private ConversationSession getOrCreateSession(String sessionId, String userId) {
        return (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                   .getSessionData()
                                                   .computeIfAbsent("conversationSession",
                                                                    k -> new ConversationSession(sessionId, userId));
    }

    /**
     * Calculate confidence score
     */
    private double calculateConfidence(NLPQueryClassifier.QueryResult nlpResult) {
        double confidence = 0.8; // Base confidence

        // Adjust based on input tracking
        if (nlpResult.inputTracking != null) {
            confidence += nlpResult.inputTracking.correctionConfidence * 0.1;
        }

        // Adjust based on errors
        confidence -= nlpResult.errors.size() * 0.1;

        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * Convert NLP entities to EntityFilter format
     */
    private List<EntityFilter> convertToEntityFilters(List<NLPQueryClassifier.EntityFilter> nlpEntities) {
        List<EntityFilter> entities = new ArrayList<>();
        for (NLPQueryClassifier.EntityFilter nlpEntity : nlpEntities) {
            entities.add(new EntityFilter(nlpEntity.attribute, nlpEntity.operation, nlpEntity.value, nlpEntity.source));
        }
        return entities;
    }

    /**
     * Convert NLP validation errors to ValidationError format
     */
    private List<ValidationError> convertToValidationErrors(List<NLPQueryClassifier.ValidationError> nlpErrors) {
        List<ValidationError> errors = new ArrayList<>();
        for (NLPQueryClassifier.ValidationError nlpError : nlpErrors) {
            errors.add(new ValidationError(nlpError.code, nlpError.message, nlpError.severity));
        }
        return errors;
    }

    /**
     * Execute query using existing logic
     */
    private Object executeQuery(NLPQueryClassifier.QueryResult nlpResult) {
        System.out.println("executeQuery=================>ConversationalNLPManager.............");
        try {
            // Use the existing NLPUserActionHandler logic
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();

            // Convert NLPQueryClassifier.QueryResult to the format expected by NLPUserActionHandler
            // We'll use the existing processUserInputCompleteResponse method
            String userInput = nlpResult.inputTracking.originalInput;
            String result = handler.processUserInputCompleteResponse(userInput, 400); // Default screen width

            // Check if this is a user selection response (multiple users found)
            if (result != null && result.contains("Please select a user:")) {
                // Extract user search results from NLPUserActionHandler cache
                Map<String, List<Map<String, String>>> userResults = handler.getUserSearchCache();
                if (userResults != null && !userResults.isEmpty()) {
                    // Store in session for future user selection
                    // This will be handled by the session when user selection is detected
                    System.out.println("User search results available for session storage");
                }
            }

            return result;

        } catch (Exception e) {
            return "Error executing query: " + e.getMessage();
        }
    }

    /**
     * Create error response
     */
    private ChatbotResponse createErrorResponse(String code, String message) {
        ChatbotResponse response = new ChatbotResponse();
        response.isSuccess = false;
        response.errors = Arrays.asList(new ValidationError(code, message, "ERROR"));
        return response;
    }

    /**
     * Clean up completed sessions
     */
    public void cleanupCompletedSessions() {
        // sessionManager.cleanupCompletedSessions(); // Commented out, not implemented
    }

    /**
     * Save session to database
     */
    public void saveSessionToDatabase(String sessionId) {
        ConversationSession session = (ConversationSession) sessionManager.getUserOrCreateSession(sessionId)
                                                                          .getSessionData()
                                                                          .get("conversationSession");
        if (session != null && session.isCompleted()) {
            // Save to database logic here
            // sessionManager.removeSession(sessionId); // Commented out, not implemented



        }
    }

    private String formatCheckLIstSummary(ConversationSession session) {
        //ekkada Dipla names sarigga rabadam ledu
        Map<String, Object> collectedData = session.getCollectedData();
        StringBuilder summary = new StringBuilder();
        summary.append("<h5>Check List Summary:</h5>");

        collectedData.forEach((key, value) -> summary.append("<p><b>")
                                                     .append(SpellCorrector.DISPLAYNAMES.get(key))
                                                     .append(":</b> ")
                                                     .append(value)
                                                     .append("</p>"));

        return summary.toString();
    }

    private String generateCheckListCreationPrompt(List<String> remainingFields) {
        System.out.println("Start of generateCheckListCreationPrompt------" + remainingFields);
        try {
            List userField = new ArrayList();
            for (Object fild : remainingFields) {
                userField.add(ContractFieldConfig.FIELD_DISPLAY_NAMES.get(fild));
            }
            return BCCTChatBotUtility.constructCheckListPromptMessage(userField, "");
        } catch (Exception e) {
            // TODO: Add catch code
            e.printStackTrace();
        }
        return null;
    }

    private ChatbotResponse intantiateTheCheckListProcess(ConversationSession session) {
        session.getCollectedData().clear();
        session.getContext().remove("awaitingChecklistPrompt");
        session.getContext().put("collectingChecklistData", true);
        ChatbotResponse response = new ChatbotResponse();
        response.isSuccess = true;
        response.dataProviderResponse =
            BCCTChatBotUtility.constructChecklistPromptMessage(ContractFieldConfig.CHECKLIST_FIELDS);
        response.metadata = new ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "CHECKLIST_PROMPT";

        return response;
    }

    private ChatbotResponse instantiateContractCreationProcess(ConversationSession session) {
        session.getCollectedData().clear();
        List<String> remainingFields = session.getRemainingFields();
        String prompt = generateContractCreationPrompt(remainingFields);
        ChatbotResponse response = new ChatbotResponse();
        response.isSuccess = true;
        response.dataProviderResponse = prompt;
        response.metadata = new ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";

        return response;
    }

    private ChatbotResponse contractDataExtractionHandling(ConversationSession session,
                                                           ConversationSession.DataExtractionResult extractionResult) {
        // Check for validation errors
        if (!extractionResult.validationErrors.isEmpty()) {
            String errorMessage = "I found some issues:\n" + String.join("\n", extractionResult.validationErrors);
            String prompt = "";
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = errorMessage + "\n\n" + prompt;
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            return response;
        }

        // Check if all required fields are collected
        List<String> remainingFields = session.getRemainingFields();
        System.out.println("Remaining feilds for contarc tceration ====>" + remainingFields);
        if (!remainingFields.isEmpty()) {
            String prompt = generateContractCreationPrompt(remainingFields);
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = prompt;
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            return response;
        }

        // All data collected - show confirmation
        String summary = formatContractSummary(session);
        String confirmPrompt =
            summary +
            "\n\n<p><b>Please confirm the contract details above.</b></p><p>Type <b>'Yes'</b> to confirm or <b>'No'</b> to modify.</p>";

        // Set confirmation flag
        session.getContext().put("awaitingContractConfirmation", true);

        ChatbotResponse response = new ChatbotResponse();
        response.isSuccess = true;
        response.dataProviderResponse = confirmPrompt;
        response.metadata = new ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
        return response;
    }

    // Response classes for ADF integration

    public static class ChatbotResponse {
        public boolean isSuccess;
        public ResponseMetadata metadata;
        public InputTrackingInfo inputTracking;
        public List<EntityFilter> entities;
        public List<String> displayEntities;
        public List<ValidationError> errors;
        public Object data;
        public String dataProviderResponse; // JSON string for ADF
        public boolean useractionrequired; // Indicates if user action is required (e.g., user selection)
    }

    public static class ResponseMetadata {
        public String queryType;
        public String actionType;
        public double processingTimeMs;
        public double confidence;
    }

    public static class InputTrackingInfo {
        public String originalInput;
        public String correctedInput;
        public double correctionConfidence;
    }

    public static class EntityFilter {
        public final String attribute;
        public final String operation;
        public final String value;
        public final String source;

        public EntityFilter(String attribute, String operation, String value, String source) {
            this.attribute = attribute;
            this.operation = operation;
            this.value = value;
            this.source = source;
        }
    }

    public static class ValidationError {
        public final String code;
        public final String message;
        public final String severity;

        public ValidationError(String code, String message, String severity) {
            this.code = code;
            this.message = message;
            this.severity = severity;
        }
    }

    /**
     * Enhanced processing result class
     */
    public static class ProcessingResult {
        private String queryType;
        private String actionType;
        private String contractNumber;
        private String partNumber;
        private Map<String, Object> filterCriteria;
        private Map<String, Object> contractCreationData;
        private boolean isHelpQuery;
        private boolean isContractCreationQuery;
        private boolean isFailedPartsQuery;
        private boolean isPartsQuery;
        private boolean isContractQuery;
        private String correctedInput;
        private double confidence;

        public ProcessingResult() {
            this.filterCriteria = new HashMap<>();
            this.contractCreationData = new HashMap<>();
        }

        // Getters and setters
        public String getQueryType() {
            return queryType;
        }

        public void setQueryType(String queryType) {
            this.queryType = queryType;
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        public String getContractNumber() {
            return contractNumber;
        }

        public void setContractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
        }

        public String getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(String partNumber) {
            this.partNumber = partNumber;
        }

        public Map<String, Object> getFilterCriteria() {
            return filterCriteria;
        }

        public void setFilterCriteria(Map<String, Object> filterCriteria) {
            this.filterCriteria = filterCriteria;
        }

        public Map<String, Object> getContractCreationData() {
            return contractCreationData;
        }

        public void setContractCreationData(Map<String, Object> contractCreationData) {
            this.contractCreationData = contractCreationData;
        }

        public boolean isHelpQuery() {
            return isHelpQuery;
        }

        public void setHelpQuery(boolean helpQuery) {
            isHelpQuery = helpQuery;
        }

        public boolean isContractCreationQuery() {
            return isContractCreationQuery;
        }

        public void setContractCreationQuery(boolean contractCreationQuery) {
            isContractCreationQuery = contractCreationQuery;
        }

        public boolean isFailedPartsQuery() {
            return isFailedPartsQuery;
        }

        public void setFailedPartsQuery(boolean failedPartsQuery) {
            isFailedPartsQuery = failedPartsQuery;
        }

        public boolean isPartsQuery() {
            return isPartsQuery;
        }

        public void setPartsQuery(boolean partsQuery) {
            isPartsQuery = partsQuery;
        }

        public boolean isContractQuery() {
            return isContractQuery;
        }

        public void setContractQuery(boolean contractQuery) {
            isContractQuery = contractQuery;
        }

        public String getCorrectedInput() {
            return correctedInput;
        }

        public void setCorrectedInput(String correctedInput) {
            this.correctedInput = correctedInput;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
    }

    /**
     * Enhanced input processing with improved NLP analysis
     */
    public ProcessingResult processInput(String userInput) {
        ProcessingResult result = new ProcessingResult();

        try {
            // Step 1: Spell correction
            String correctedInput = spellCorrector.correct(userInput);
            result.setCorrectedInput(correctedInput);

            // Step 2: Query classification
            String queryType = classifyQueryType(correctedInput);
            result.setQueryType(queryType);

            // Step 3: Action type determination
            String actionType = determineActionType(correctedInput, queryType);
            result.setActionType(actionType);

            // Step 4: Entity extraction
            extractEntities(correctedInput, result);

            // Step 5: Set query type flags
            setQueryTypeFlags(result, queryType);

            // Step 6: Calculate confidence
            double confidence = calculateConfidence(correctedInput, queryType, actionType);
            result.setConfidence(confidence);

            return result;

        } catch (Exception e) {
            // Return error result
            result.setQueryType("ERROR");
            result.setActionType("ERROR");
            result.setConfidence(0.0);
            return result;
        }
    }

    /**
     * Enhanced query type classification
     */
    private String classifyQueryType(String userInput) {
        String input = userInput.toLowerCase();

        // HELP Query Detection - HIGH PRIORITY
        if (isHelpQuery(input)) {
            return "HELP";
        }

        // Contract Creation Query Detection
        if (isContractCreationQuery(input)) {
            return "CONTRACT_CREATION";
        }

        // Failed Parts Detection
        if (isFailedPartsQuery(input)) {
            return "FAILED_PARTS";
        }

        // Parts Detection
        if (isPartsQuery(input)) {
            return "PARTS";
        }

        // Contract Detection
        if (isContractQuery(input)) {
            return "CONTRACTS";
        }

        // Default fallback
        return "CONTRACTS";
    }

    /**
     * Enhanced action type determination
     */
    private String determineActionType(String userInput, String queryType) {
        System.out.println("determineActionType============>");
        String input = userInput.toLowerCase();

        // HELP Action Types
        if ("HELP".equals(queryType)) {
            if (isContractCreationHelpQuery(input)) {
                return "HELP_CONTRACT_CREATE_USER";
            } else if (isContractCreationBotQuery(input)) {

                return "HELP_CONTRACT_CREATE_BOT";
            } else {
                return "HELP_CONTRACT_CREATE_USER"; // Default HELP action
            }
        }

        // Contract Creation Action Types
        if ("CONTRACT_CREATION".equals(queryType)) {
            if (isUserInitiatedCreation(input)) {
                return "HELP_CONTRACT_CREATE_USER";
            } else {
                return "HELP_CONTRACT_CREATE_BOT";
            }
        }

        // Failed Parts Action Types
        if ("FAILED_PARTS".equals(queryType)) {
            return "parts_failed_by_contract_number";
        }

        // Parts Action Types
        if ("PARTS".equals(queryType)) {
            if (containsPartNumber(input)) {
                return "parts_by_part_number";
            } else {
                return "parts_by_contract_number";
            }
        }

        // Contract Action Types
        if ("CONTRACTS".equals(queryType)) {
            if (containsContractNumber(input)) {
                return "contracts_by_contractnumber";
            } else {
                return "contracts_by_filter";
            }
        }

        // Default fallback
        return "contracts_by_contractnumber";
    }

    /**
     * Enhanced entity extraction
     */
    private void extractEntities(String userInput, ProcessingResult result) {
        String input = userInput.toLowerCase();

        // Extract contract numbers
        List<String> contractNumbers = extractContractNumbers(input);
        if (!contractNumbers.isEmpty()) {
            result.setContractNumber(contractNumbers.get(0));
            result.getFilterCriteria().put("AWARD_NUMBER", contractNumbers.get(0));
        }

        // Extract part numbers
        List<String> partNumbers = extractPartNumbers(input);
        if (!partNumbers.isEmpty()) {
            result.setPartNumber(partNumbers.get(0));
            result.getFilterCriteria().put("INVOICE_PART_NUMBER", partNumbers.get(0));
        }

        // Extract dates
        List<String> dates = extractDates(input);
        if (!dates.isEmpty()) {
            result.getFilterCriteria().put("CREATED_DATE", dates.get(0));
        }

        // Extract customer information
        if (containsCustomerKeywords(input)) {
            result.getFilterCriteria().put("CUSTOMER_NAME", "LIKE");
        }

        // Extract contract creation data
        if (isContractCreationQuery(input)) {
            extractContractCreationData(input, result);
        }
    }

    /**
     * Set query type flags
     */
    private void setQueryTypeFlags(ProcessingResult result, String queryType) {
        result.setHelpQuery("HELP".equals(queryType));
        result.setContractCreationQuery("CONTRACT_CREATION".equals(queryType));
        result.setFailedPartsQuery("FAILED_PARTS".equals(queryType));
        result.setPartsQuery("PARTS".equals(queryType));
        result.setContractQuery("CONTRACTS".equals(queryType));
    }

    /**
     * Calculate processing confidence
     */
    private double calculateConfidence(String userInput, String queryType, String actionType) {
        double confidence = 0.8; // Base confidence

        // Increase confidence for specific patterns
        if (containsContractNumber(userInput)) {
            confidence += 0.1;
        }

        if (containsPartNumber(userInput)) {
            confidence += 0.1;
        }

        if (isHelpQuery(userInput)) {
            confidence += 0.05;
        }

        // Decrease confidence for ambiguous queries
        if (userInput.length() < 5) {
            confidence -= 0.2;
        }

        return Math.max(0.0, Math.min(1.0, confidence));
    }

    // Helper methods for query detection
    private boolean isHelpQuery(String input) {
        String[] helpKeywords = {
            "how to", "how do", "steps", "process", "guide", "instructions", "help", "assist", "create", "make",
            "generate", "build", "set up", "walk me through", "explain", "tell me", "show me how", "need guidance",
            "need help", "want to create", "would like to create", "can you create", "please create", "help me create",
            "assist me", "guide me"
        };

        for (String keyword : helpKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContractCreationQuery(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase().trim();

        // Pattern 1: "create contract" (no account number)
        if (lowerInput.equals("create contract") || lowerInput.equals("create contarct")) {
            return true;
        }

        // Pattern 2: "create contract [account]" (with account number)
        if (lowerInput.matches("create contract\\s+\\d{6,}\\s*.*")) {
            return true;
        }

        // Pattern 3: "create contract for account [number]"
        if (lowerInput.matches("create contract for account\\s+\\d{6,}\\s*.*")) {
            return true;
        }

        // Pattern 4: "help create contract" or "how to create contract"
        if (lowerInput.contains("help create contract") || lowerInput.contains("how to create contract")) {
            return true;
        }

        // Legacy patterns for backward compatibility
        String[] creationKeywords = {
            "make contract", "generate contract", "build contract", "set up contract", "new contract", "start contract",
            "initiate contract", "draft contract", "establish contract", "form contract", "develop contract"
        };

        for (String keyword : creationKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private boolean isContractCreationHelpQuery(String input) {
        String[] userHelpKeywords = {
            "how to create", "how do i create", "steps to create", "process for creating", "guide me",
            "walk me through", "explain how", "tell me how", "show me how", "need guidance", "need help",
            "want to know", "would like to know"
        };

        for (String keyword : userHelpKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContractCreationBotQuery(String input) {
        String[] botKeywords = {
            "create for me", "make for me", "generate for me", "build for me", "set up for me", "do it for me",
            "can you create", "please create", "create contract", "make contract", "generate contract", "build contract"
        };

        for (String keyword : botKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUserInitiatedCreation(String input) {
        return input.contains("how") || input.contains("steps") || input.contains("process") ||
               input.contains("guide") || input.contains("explain") || input.contains("tell me");
    }

    private boolean isFailedPartsQuery(String input) {
        String[] failedKeywords = {
            "failed parts", "failed part", "parts failed", "part failed", "error parts", "error part", "parts error",
            "part error", "failed", "errors", "failures", "problems", "issues"
        };

        for (String keyword : failedKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPartsQuery(String input) {
        String[] partsKeywords = {
            "part", "parts", "lead time", "price", "cost", "moq", "uom", "unit of measure", "minimum order", "leadtime",
            "pricing"
        };

        for (String keyword : partsKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContractQuery(String input) {
        String[] contractKeywords = {
            "contract", "agreement", "customer", "effective date", "expiration", "payment terms", "incoterms", "status",
            "active", "expired"
        };

        for (String keyword : contractKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Entity extraction helper methods
    private List<String> extractContractNumbers(String input) {
        List<String> contractNumbers = new ArrayList<>();
        // Pattern for 6-digit contract numbers
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b\\d{6}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            contractNumbers.add(matcher.group());
        }
        return contractNumbers;
    }

    private List<String> extractPartNumbers(String input) {
        List<String> partNumbers = new ArrayList<>();
        // Pattern for part numbers (letters + numbers)
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b[A-Z]{2}\\d{5}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(input.toUpperCase());
        while (matcher.find()) {
            partNumbers.add(matcher.group());
        }
        return partNumbers;
    }

    private List<String> extractDates(String input) {
        List<String> dates = new ArrayList<>();
        // Pattern for dates (various formats)
        java.util.regex.Pattern pattern = java.util
                                              .regex
                                              .Pattern
                                              .compile("\\b\\d{4}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            dates.add(matcher.group());
        }
        return dates;
    }

    private boolean containsPartNumber(String input) {
        return !extractPartNumbers(input).isEmpty();
    }

    private boolean containsContractNumber(String input) {
        return !extractContractNumbers(input).isEmpty();
    }

    private boolean containsCustomerKeywords(String input) {
        String[] customerKeywords = { "customer", "client", "who", "name" };
        for (String keyword : customerKeywords) {
            if (input.contains(keyword))
                return true;
        }
        return false;
    }

    private void extractContractCreationData(String input, ProcessingResult result) {
        // Extract customer information
        if (containsCustomerKeywords(input)) {
            result.getContractCreationData().put("CUSTOMER_NAME", "Customer");
        }

        // Extract dates
        List<String> dates = extractDates(input);
        if (!dates.isEmpty()) {
            result.getContractCreationData().put("EFFECTIVE_DATE", dates.get(0));
        }

        // Extract contract name patterns
        if (input.contains("contract") && input.contains("name")) {
            result.getContractCreationData().put("CONTRACT_NAME", "New Contract");
        }
    }

    /**
     * Process complete contract creation input
     */
    private ChatbotResponse processCompleteContractCreationInput(String userInput, ConversationSession session) {
        System.out.println("processCompleteContractCreationInput=================>");

        // Parse the comma-separated input
        String[] parts = userInput.split(",");
        String accountNumber = parts.length >= 1 ? parts[0].trim() : "";
        String contractName = parts.length >= 2 ? parts[1].trim() : "";
        String title = parts.length >= 3 ? parts[2].trim() : "";
        String description = parts.length >= 4 ? parts[3].trim() : "";
        String comments = parts.length >= 5 ? parts[4].trim() : "";
        String isPricelist = parts.length >= 6 ? parts[5].trim() : "NO";

        System.out.println("Parsed input:");
        System.out.println("  Account Number: " + accountNumber);
        System.out.println("  Contract Name: " + contractName);
        System.out.println("  Title: " + title);
        System.out.println("  Description: " + description);
        System.out.println("  Comments: " + comments);
        System.out.println("  Is Pricelist: " + isPricelist);

        // Handle pricelist
        if (isPricelist.equalsIgnoreCase("yes")) {
            isPricelist = "YES";
        } else {
            isPricelist = "NO";
        }

        // Validate account number first
        NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
        if (!handler.isCustomerNumberValid(accountNumber)) {
            ChatbotResponse response = new ChatbotResponse();
            response.metadata = new ResponseMetadata();
            response.data =
                "<h4>Warning Invalid Account Number</h4>" + "<p>The account number <b>" + accountNumber +
                "</b> is not valid.</p>" + "<p>Please provide a valid customer account number (6+ digits).</p>" +
                "<p><b>Example:</b> 123456789, TestContract, TestTitle, TestDescription, TestComment, NO</p>";
            response.dataProviderResponse = response.data.toString();
            response.isSuccess = false;
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            return response;
        }

        // Store the data in session
        Map<String, Object> collectedData = session.getCollectedData();
        System.out.println("Before storing - collectedData: " + collectedData);

        collectedData.put("ACCOUNT_NUMBER", accountNumber);
        collectedData.put("CONTRACT_NAME", contractName);
        collectedData.put("TITLE", title);
        collectedData.put("DESCRIPTION", description);
        collectedData.put("COMMENTS", comments);
        collectedData.put("IS_PRICELIST", isPricelist);

        System.out.println("After storing - collectedData: " + collectedData);

        // Mark as received
        session.setContractCreationStatus("RECEIVED");

        // Create confirmation response
        ChatbotResponse response = new ChatbotResponse();
        response.metadata = new ResponseMetadata();
        response.data =
            "<h4>Contract Creation Details</h4>" + "<p>Please confirm the following details:</p>" + "<ul>" +
            "<li><b>Account Number:</b> " + accountNumber + "</li>" + "<li><b>Contract Name:</b> " + contractName +
            "</li>" + "<li><b>Title:</b> " + title + "</li>" + "<li><b>Description:</b> " + description + "</li>" +
            "<li><b>Comments:</b> " + (comments.isEmpty() ? "None" : comments) + "</li>" +
            "<li><b>Price List Contract:</b> " + isPricelist + "</li>" + "</ul>" +
            "<p><b>Please type 'Yes' to confirm and create the contract, or 'No' to modify the details.</b></p>";
        response.dataProviderResponse = response.data.toString();
        response.isSuccess = true;
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";

        // After storing all fields, always prompt for confirmation
        if (!BCCTChatBotUtility.hasUnsetAttributes(session)) {
            String summary = BCCTChatBotUtility.getContractSummary(session);
            String confirmPrompt = summary + CONFIRMATION_MESSAGE;
            session.getContext().put("awaitingContractConfirmation", true);
            System.out.println("[DEBUG] Set awaitingContractConfirmation=true in session context (processCompleteContractCreationInput)");
            return BCCTChatBotUtility.createBotResponse(confirmPrompt, session);
        }

        return response;
    }


    private ChatbotResponse handleCheckListCreationConfirmation(ConversationSession session) {
        System.out.println("handleCheckListCreationConfirmation==============>");
        // Defensive: Ensure collectedData and auditData are never null
        if (session.getCollectedData() == null) {
            System.out.println("[WARN] collectedData was null, initializing new ConcurrentHashMap");
            // Should not happen, but just in case
            session.clearCache(); // This will re-init collectedData if needed
        }
        if (session.getAuditData() == null) {
            System.out.println("[WARN] auditData was null, initializing new HashMap");
            session.setAuditData(new HashMap<>());
        }
        Map<String, Object> collectedData = session.getCollectedData();
        try {
            System.out.println("Calling checkListCreation");
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
            Map result = handler.checkListCreation(convertToStringMap(collectedData));
            System.out.println("Calling checkListCreation");
            session.getContext().put("awaitingChecklitsConfirmation", null);
            session.getContext().put("awaitingChecklistPrompt", null);
            session.setAwaitingChecklistConfirmation(false);
            session.setCurrentFlowType(null);
            session.setWaitingForUserInput(false);
            session.clearCurrentFlow();
            // Defensive: Ensure auditData is not null before put
            if (session.getAuditData() == null) {
                session.setAuditData(new HashMap<>());
            }
            session.getAuditData()
                .put("CheckList", collectedData != null ? new HashMap<>(collectedData) : new HashMap<>());

            if (collectedData != null) {
                collectedData.clear(); //after successfully created contract we dont need to keep in collected object
            }
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = "Check List created successfully.";
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = null;
            response.metadata.actionType = null;

            // Do not clear session yet; wait for checklist response
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = false;
            response.dataProviderResponse = "Error creating check list: " + e.getMessage();
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "CONTRACT_CREATION_ERROR";
            return response;
        }
    }

    /**
     * Handle contract creation confirmation and prompt for checklist
     */
    private ChatbotResponse handleContractCreationConfirmation(ConversationSession session) {
        Map<String, Object> collectedData = session.getCollectedData();
        try {
            NLPUserActionHandler handler = NLPUserActionHandler.getInstance();
            String implCode = handler.createContractByBOT(convertToStringMap(collectedData));
            StringBuilder details = new StringBuilder();
            details.append("<div style='background-color: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0;'>");
            details.append("<h5>Contract Created Successfully!</h5>");
            details.append("<p><b>Account Number:</b> ")
                   .append(collectedData.get("ACCOUNT_NUMBER"))
                   .append("</p>");
            details.append("<p><b>Contract Name:</b> ")
                   .append(collectedData.get("CONTRACT_NAME"))
                   .append("</p>");
            details.append("<p><b>Title:</b> ")
                   .append(collectedData.get("TITLE"))
                   .append("</p>");
            details.append("<p><b>Description:</b> ")
                   .append(collectedData.get("DESCRIPTION"))
                   .append("</p>");
            details.append("<p><b>Comments:</b> ")
                   .append(collectedData.get("COMMENTS"))
                   .append("</p>");
            details.append("<p><b>Price List:</b> ")
                   .append(collectedData.get("IS_PRICELIST"))
                   .append("</p>");
            details.append("</div>");
            details.append("<br/><b>Would you like to start the checklist process for this contract? (Yes/No)</b>");
            // Set context to expect checklist response
            session.getContext().put("awaitingChecklistPrompt", true);
            session.setAwaitingChecklistConfirmation(true);
            session.setCurrentFlowType("CREATE_CHECKLIST");
            session.setWaitingForUserInput(true); // Ensure session is waiting for user input

            //clearing the collected data becuase we have to use same object for Check List if user wants
            //before that we should keep this in session object
            session.getAuditData().put(implCode, session.getCollectedData());

            session.getCollectedData()
                .clear(); //after successfully created contract we dont need to keep in collected object

            System.out.println("Collected Data===>" + session.getCollectedData());
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = details.toString();
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "CONTRACT_CREATION_COMPLETE";

            // Do not clear session yet; wait for checklist response
            return response;
        } catch (Exception e) {
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = false;
            response.dataProviderResponse = "Error creating contract: " + e.getMessage();
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "CONTRACT_CREATION_ERROR";
            return response;
        }
    }

    /**
     * Convert Map<String, Object> to Map<String, String>
     */
    private Map<String, String> convertToStringMap(Map<String, Object> data) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return result;
    }

    /**
     * Public getter for ConversationSession for use by beans and UI
     */
    public ConversationSession getSession(String sessionId, String userId) {
        return getOrCreateSession(sessionId, userId);
    }


    // Checklist input processing
    private ChatbotResponse processChecklistInput(String userInput, ConversationSession session) {
        System.out.println("Starting of processChecklistInput==========>");
        Object awaiting = session.getContext().get("awaitingChecklitsConfirmation");
        if (awaiting != null && (Boolean) awaiting) {
            String lowerInput = userInput.toLowerCase();
            if (lowerInput.equals("yes") || lowerInput.equals("confirm") || lowerInput.equals("ok")) {
                session.getContext().remove("awaitingChecklitsConfirmation");
                return handleCheckListCreationConfirmation(session);
            } else if (lowerInput.equals("no")) {
                return intantiateTheCheckListProcess(session);
                // session.getContext().remove("awaitingChecklitsConfirmation");
                //                // Reset and start over
                //                session.clearCurrentFlow();
                //                session.startContractCreationFlow();
                //                String prompt = "";vinod
                //                ChatbotResponse response = new ChatbotResponse();
                //                response.isSuccess = true;
                //                response.dataProviderResponse = "Check List  creation cancelled. Let's start over.\n\n" + prompt;
                //                response.metadata = new ResponseMetadata();
                //                response.metadata.queryType = null;
                //                response.metadata.actionType = null;
                //                return response;
            } else if (lowerInput.equals("cancel")) {
                session.getContext().remove("awaitingChecklitsConfirmation");
                // Reset and start over
                session.clearCurrentFlow();
                String prompt = "";
                ChatbotResponse response = new ChatbotResponse();
                response.isSuccess = true;
                response.dataProviderResponse = "Check List  creation cancelled. Let's start over.\n\n" + prompt;
                response.metadata = new ResponseMetadata();
                response.metadata.queryType = null;
                response.metadata.actionType = null;
                return response;

            }
            //                  else {
            //                // Invalid response - prompt again
            //                String summary = formatCheckLIstSummary(session);
            //                String confirmPrompt =
            //                    summary +
            //                    "\n\n<p><b>Please confirm the Check List  details above.</b></p><p>Type <b>'Yes'</b> to confirm or <b>'No'</b> to modify.</p>";
            //                ChatbotResponse response = new ChatbotResponse();
            //                response.isSuccess = true;
            //                response.dataProviderResponse = confirmPrompt;
            //                response.metadata = new ResponseMetadata();
            //                response.metadata.queryType = "HELP";
            //                response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            //                return response;
            //            }
        }


        Set<String> checklistColumns = TableColumnConfig.CHECL_LIST_COLUMNS;
        Map<String, String> checklistMap = session.getChecklistFieldMap();
        System.out.println("Before Collected Data=" + session.getCollectedData());
        DataExtractionResult extractionResult = session.processUserInput(userInput);
        System.out.println("After Collected Data=" + session.getCollectedData());
        // Use RobustDataExtractor for extraction
        Map<String, String> extracted = RobustDataExtractor.extractData(userInput);
        System.out.println("Required COlumns ===" + checklistColumns);
        System.out.println();


        // Check for validation errors
        if (!extractionResult.validationErrors.isEmpty()) {
            String errorMessage = "I found some issues:\n" + String.join("\n", extractionResult.validationErrors);
            String prompt = "";
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = errorMessage + "\n\n" + prompt;
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            return response;
        }
        // Check if all required fields are collected
        List<String> remainingFields = session.getRemainingCheckLIstFields();
        System.out.println("Remaining feilds for contarc tceration ====>" + remainingFields);
        if (!remainingFields.isEmpty()) {
            // Still collecting data - show prompt for missing fields
            String prompt = generateCheckListCreationPrompt(remainingFields);
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = prompt;
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            return response;
        }
        //once we got the data related to check list we should do the valdiation properly
        //vinod

        Map<String, String> stringData = session.getCollectedData()
                                                .entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue()) // Safely converts to String
                                                                          ));

        Map<String, String> datesValidation = NLPUserActionHandler.getInstance().validateCheckListDates(stringData);


        // Validate checklist data

        List<String> invalidFields = new ArrayList<>();
        StringBuilder errorMsg = new StringBuilder();
        for (String field : ContractFieldConfig.CHECKLIST_FIELDS) {
            String result = datesValidation.get(field);
            if (!"success".equalsIgnoreCase(result)) {
                invalidFields.add(field);
                errorMsg.append("<p><b>")
                        .append(ContractFieldConfig.getChecklistDisplayName(field))
                        .append(":</b> ") // Close <b> after the label
                        .append(result)
                        .append("<br>")
                        .append("</p>");
            }
        }

        System.out.println("Error message===>" + errorMsg.toString());

        if (!invalidFields.isEmpty()) {
            String errorMessage = "I found some issues:\n" + String.join("\n", extractionResult.validationErrors);
            String prompt = errorMsg.toString();
            ChatbotResponse response = new ChatbotResponse();
            response.isSuccess = true;
            response.dataProviderResponse = errorMessage + "\n\n" + prompt;
            response.metadata = new ResponseMetadata();
            response.metadata.queryType = "HELP";
            response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
            return response;
        }

        // All data collected - show confirmation
        String summary = formatCheckLIstSummary(session);
        String confirmPrompt =
            summary +
            "\n\n<p><b>Please confirm the Check List  details above.</b></p><p>Type <b>'Yes'</b> to confirm or <b>'No'</b> to modify.</p>";

        // Set confirmation flag
        session.getContext().put("awaitingChecklitsConfirmation", true);

        ChatbotResponse response = new ChatbotResponse();
        response.isSuccess = true;
        response.dataProviderResponse = confirmPrompt;
        response.metadata = new ResponseMetadata();
        response.metadata.queryType = "HELP";
        response.metadata.actionType = "HELP_CONTRACT_CREATE_BOT";
        return response;


    }

    // Flexible parsing: supports label:value and positional input
    private Map<String, String> parseChecklistDatesFlexible(String input, Collection<String> columns) {
        Map<String, String> map = new HashMap<>();
        String[] parts = input.split("[,\n]");
        int labelValueCount = 0;
        for (String part : parts) {
            String[] pair = null;
            if (part.contains(":")) {
                pair = part.split(":", 2);
            } else if (part.contains("=")) {
                pair = part.split("=", 2);
            }
            if (pair != null && pair.length == 2) {
                String label = pair[0].trim();
                String value = pair[1].trim();
                // Use SpellCorrector for normalization
                String col = SpellCorrector.normalizeFieldLabel(label);
                if (col != null && columns.contains(col)) {
                    map.put(col, value);
                    labelValueCount++;
                }
            }
        }
        // Fallback: if no label:value pairs found, use positional mapping
        if (labelValueCount == 0) {
            String[] values = input.split(",");
            int i = 0;
            for (String col : columns) {
                if (i < values.length) {
                    map.put(col, values[i].trim());
                    i++;
                }
            }
        }
        return map;
    }

    // Flexible contract data parsing: supports label:value, label=value, and positional
    private Map<String, String> parseContractDataFlexible(String input, Collection<String> columns) {
        Map<String, String> map = new HashMap<>();
        String[] lines = input.split("\n");
        int labelValueCount = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;
            String[] parts = null;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else if (line.contains("=")) {
                parts = line.split("=", 2);
            } else if (line.contains("|")) {
                parts = line.split("\\|", 2);
            } else if (line.contains("\t")) {
                parts = line.split("\t", 2);
            } else if (line.contains(" - ")) {
                parts = line.split(" - ", 2);
            } else if (line.contains(" -> ")) {
                parts = line.split(" -> ", 2);
            } else {
                // Handle space separator (find last space before value pattern)
                java.util.regex.Pattern valuePattern = java.util
                                                           .regex
                                                           .Pattern
                                                           .compile("(\\d{1,2}/\\d{1,2}/\\d{2,4}|[A-Z0-9-]+|.+)$");
                java.util.regex.Matcher matcher = valuePattern.matcher(line);
                if (matcher.find()) {
                    int valueStart = matcher.start();
                    if (valueStart > 0) {
                        parts = new String[] {
                            line.substring(0, valueStart).trim(), line.substring(valueStart).trim() };
                    }
                }
            }
            if (parts != null && parts.length == 2) {
                String keyPart = parts[0].trim();
                String valuePart = parts[1].trim();
                if (!keyPart.isEmpty() && !valuePart.isEmpty()) {
                    String standardKey = SpellCorrector.normalizeFieldLabel(keyPart);
                    if (standardKey != null && columns.contains(standardKey)) {
                        map.put(standardKey, valuePart);
                        labelValueCount++;
                    }
                }
            }
        }
        // Fallback: if no label:value pairs found, use positional mapping
        if (labelValueCount == 0) {
            String[] values = input.split(",");
            int i = 0;
            for (String col : columns) {
                if (i < values.length) {
                    map.put(col, values[i].trim());
                    i++;
                }
            }
        }
        return map;
    }

}

