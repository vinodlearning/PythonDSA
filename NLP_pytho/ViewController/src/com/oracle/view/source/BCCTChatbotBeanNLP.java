//package com.ben.view.nlp;
//
//import com.ben.view.model.ParsedQuery;
//import com.ben.view.model.BCCTContractCreationHelper;
//import com.ben.view.service.ChatbotNLPService;
//import com.ben.view.nlp.ChatMessage;
//import com.ben.view.service.ImageService;
//
//import com.oracle.view.source.ChatMessage;
//
//import com.oracle.view.source.NLPUserActionHandler;
//
//import java.io.FileWriter;
//import java.io.IOException;
//
//import java.text.SimpleDateFormat;
//
//import java.util.List;
//
//import javax.faces.event.ActionEvent;
//
//import oracle.adf.model.binding.DCBindingContainer;
//import oracle.adf.model.binding.DCIteratorBinding;
//import oracle.adf.model.BindingContext;
//
//import oracle.adf.view.rich.render.ClientEvent;
//
//import oracle.jbo.Row;
//import oracle.jbo.ViewObject;
//
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.List;
//import java.util.ArrayList;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import oracle.jbo.RowSetIterator;
//
//
//public class BCCTChatbotBeanNLP {
//
//    private ChatbotNLPService nlpService;
//    private String chatResponse;
//    private String userInput;
//    private String userMessage;
//    private List<ChatMessage> chatHistory;
//    private boolean waitingForUserConfirmation = false;
//    private String pendingQuery;
//    private List<String> matchedUsers;
//    private String searchedUserName;
//    private List<String> pendingUserMatches = null;
//    private  NLPUserActionHandler userActionHandler=null;
//    
//
//    public BCCTChatbotBeanNLP() {
//        super();
//        userActionHandler=new NLPUserActionHandler();
//        nlpService = new ChatbotNLPService();
//        chatHistory = new ArrayList<ChatMessage>();
//        // Enhanced welcome message
//        chatHistory.add(new ChatMessage("Bot",
//                                        "Hello! I'm your Contract Assistant with enhanced NLP capabilities. You can ask me about:\n" +
//                                        "• Contract information (e.g., 'Show contract ABC123')\n" +
//                                        "• Parts information (e.g., 'How many parts for XYZ456')\n" +
//                                        "• Contract status (e.g., 'Status of ABC123')\n" +
//                                        "• Customer details (e.g., 'account 10840607(HONEYWELL INTERNATIONAL INC.)')\n" +
//                                        "• Failed contracts\n" +
//                                        "• **Contract creation help** (e.g., 'How to create contract')\n" +
//                                        "How can I help you today?", new Date(), true));
//    }
//
//
//    protected String processUserMessage(String message) {
//        try {
//            System.out.println("=== DEBUG: Processing message: " + message);
//
//            // Check if we're waiting for user confirmation
//            if (waitingForUserConfirmation) {
//                return handleUserConfirmation(message);
//            }
//
//            // Parse the user input using NLP
//            ParsedQuery parsedQuery = nlpService.parseUserInput(message);
//
//            // Enhanced logging
//            System.out.println("=== DEBUG: Intent detected: " + parsedQuery.getIntent());
//            System.out.println("=== DEBUG: Entities found: " + parsedQuery.getEntities());
//            System.out.println("=== DEBUG: Normalized input: " + parsedQuery.getNormalizedInput());
//
//            // Process based on intent and entities
//            String response = processNLPQuery(parsedQuery);
//            System.out.println("=== DEBUG: Generated response: " +
//                               response.substring(0, Math.min(100, response.length())) + "...");
//
//            return response;
//        } catch (Exception e) {
//            System.err.println("NLP processing error: " + e.getMessage());
//            e.printStackTrace();
//            return "Sorry, I encountered an error while processing your request. Please try again.";
//        }
//    }
//
//    //    private String processNLPQuery(ParsedQuery query) {
//    //        String intent = query.getIntent().getCode();
//    //
//    //        switch (intent.toUpperCase()) {
//    //        case "CONTRACT_INFO":
//    //            return handleContractInfoQuery(query);
//    //        case "CONTRACT_CREATE_HELP":
//    //                    return handleCreateContractHelpQuery(query);
//    //        case "STATUS_CHECK":
//    //            return handleStatusCheckQuery(query);
//    //        case "PARTS_INFO":
//    //            return handlePartsInfoQuery(query);
//    //        case "CUSTOMER_INFO":
//    //            return handleCustomerInfoQuery(query);
//    //        default:
//    //            return handleGeneralQuery(query);
//    //        }
//    //    }
//
//    private String processNLPQuery(ParsedQuery query) {
//        String intent = query.getIntent();
//        String originalInput = query.getOriginalInput().toLowerCase().trim();
//        
//        System.out.println("=== DEBUG: Processing intent in bean: " + intent);
//        
//        // Safety check: If USER_CONTRACT_QUERY but input looks like creation help
//        if ("USER_CONTRACT_QUERY".equals(intent.toUpperCase())) {
//            if (originalInput.equals("create contract") || 
//                (originalInput.contains("how") && originalInput.contains("create") && originalInput.contains("contract")) ||
//                (originalInput.contains("help") && originalInput.contains("create") && originalInput.contains("contract"))) {
//                System.out.println("=== DEBUG: Safety override - redirecting to CREATE_CONTRACT_HELP");
//                return handleCreateContractHelpQuery(query);
//            }
//        }
//        
//        switch (intent.toUpperCase()) {
//            case "PARTS_DETAILED_INFO":
//                return handleDetailedPartsQuery(query);
//            case "CREATE_CONTRACT_HELP":
//                System.out.println("=== DEBUG: Calling handleCreateContractHelpQuery");
//                return handleCreateContractHelpQuery(query);
//            case "USER_CONTRACT_QUERY":
//                return handleUserContractQuery(query);
//            case "CONTRACT_INFO":
//                return handleContractInfoQuery(query);
//            case "STATUS_CHECK":
//                return handleStatusCheckQuery(query);
//            case "PARTS_INFO":
//                return handlePartsInfoQuery(query);
//            case "CUSTOMER_INFO":
//                return handleCustomerInfoQuery(query);
//            case "GENERAL_QUERY":
//                return handleGeneralQuery(query);
//            default:
//                System.out.println("=== DEBUG: Unknown intent, calling handleGeneralQuery");
//                return handleGeneralQuery(query);
//        }
//    }
//
//    private String handleContractInfoQuery(ParsedQuery query) {
//        System.out.println("DEBUG: Intent: " + query.getIntent());
//        System.out.println("DEBUG: All entities: " + query.getEntities());
//
//        String contractNumber = query.extractNumber();
//
//        System.out.println("handleContractInfoQuery-------------->" + contractNumber);
//
//        if (contractNumber != null) {
//            return getContractInfo(contractNumber);
//        } else {
//            String customerName = query.getFirstEntity("CUSTOMER_NAME");
//            String status = query.getFirstEntity("STATUS");
//            if (customerName != null) {
//                return getContractsByCustomer(customerName);
//            } else if (status != null) {
//                return getContractsByStatus(status);
//            } else {
//                return "Please provide a contract number to get contract information.";
//            }
//        }
//    }
//
//
//    //    private String handleContractInfoQuery(ParsedQuery query) {
//    //        String contractNumber = query.getFirstEntity("CONTRACT_NUMBER");
//    //System.out.println("handleContractInfoQuery-------------->"+contractNumber);
//    //        if (contractNumber != null) {
//    //            // User specified a contract number
//    //            return getContractInfo(contractNumber);
//    //        } else {
//    //            // General contract query
//    //            String customerName = query.getFirstEntity("CUSTOMER_NAME");
//    //            String status = query.getFirstEntity("STATUS");
//    //
//    //            if (customerName != null) {
//    //                return getContractsByCustomer(customerName);
//    //            } else if (status != null) {
//    //                return getContractsByStatus(status);
//    //            } else {
//    //                return "I can help you with contract information. Please specify:\n" +
//    //                       "• A contract number (e.g., 'ABC123')\n" + "• A customer name (e.g., 'Boeing')\n" +
//    //                       "• A status (e.g., 'active', 'pending')";
//    //            }
//    //        }
//    //    }
//
//    private String handleStatusCheckQuery(ParsedQuery query) {
//        String contractNumber = query.getFirstEntity("CONTRACT_NUMBER");
//        String customerName = query.getFirstEntity("CUSTOMER_NAME");
//
//        if (contractNumber != null) {
//            return getContractStatus(contractNumber);
//        } else if (customerName != null) {
//            return getCustomerContractStatus(customerName);
//        } else {
//            return "To check status, please specify:\n" + "• A contract number (e.g., 'status of ABC123')\n" +
//                   "• A customer name (e.g., 'Boeing status')";
//        }
//    }
//
//    private String handlePartsInfoQuery(ParsedQuery query) {
//        System.out.println("=== DEBUG: handlePartsInfoQuery called");
//        System.out.println("=== DEBUG: All entities: " + query.getEntities());
//
//        // FIX: Use extractNumber() instead of getFirstEntity("CONTRACT_NUMBER")
//        String contractNumber = query.extractNumber();
//        System.out.println("=== DEBUG: extractNumber() returned: " + contractNumber);
//
//        String customerName = query.getFirstEntity("CUSTOMER_NAME");
//
//        if (contractNumber != null) {
//            System.out.println("=== DEBUG: Calling getPartsByContract with: " + contractNumber);
//            return getPartsByContract(contractNumber);
//        } else if (customerName != null) {
//            return getPartsByCustomer(customerName);
//        } else {
//            return getTotalPartsInfo();
//        }
//    }
//
//
//    private String handleCustomerInfoQuery(ParsedQuery query) {
//        String customerName = query.getFirstEntity("CUSTOMER_NAME");
//
//        if (customerName != null) {
//            return getCustomerInfo(customerName);
//        } else {
//            return getAllCustomersInfo();
//        }
//    }
//
//    private String handleGeneralQuery(ParsedQuery query) {
//        // Try to extract any useful entities and provide relevant information
//        String contractNumber = query.getFirstEntity("CONTRACT_NUMBER");
//        String customerName = query.getFirstEntity("CUSTOMER_NAME");
//
//        if (contractNumber != null) {
//            return "I found contract number '" + contractNumber + "' in your message. " +
//                   getContractInfo(contractNumber);
//        } else if (customerName != null) {
//            return "I found customer '" + customerName + "' in your message. " + getCustomerInfo(customerName);
//        } else {
//            return generateSmartSuggestion(query);
//        }
//    }
//
//    private String generateSmartSuggestion(ParsedQuery query) {
//        StringBuilder response = new StringBuilder();
//        response.append("I understand you're looking for information, but I need more details. ");
//
//        // Analyze what the user might be asking for based on keywords
//        String normalizedInput = query.getNormalizedInput();
//
//        if (normalizedInput.contains("how many") || normalizedInput.contains("count")) {
//            response.append("For counts, try:\n");
//            response.append("• 'How many parts loaded for [contract]'\n");
//            response.append("• 'Total contracts for [customer]'\n");
//        } else if (normalizedInput.contains("show") || normalizedInput.contains("list")) {
//            response.append("To show information, try:\n");
//            response.append("• 'Show contract ABC123'\n");
//            response.append("• 'List Boeing contracts'\n");
//        } else if (normalizedInput.contains("when") || normalizedInput.contains("date")) {
//            response.append("For date information, try:\n");
//            response.append("• 'When does contract ABC123 expire'\n");
//            response.append("• 'Show contracts expiring soon'\n");
//        } else {
//            response.append("Try asking about:\n");
//            response.append("• Contract information: 'Show contract ABC123'\n");
//            response.append("• Parts data: 'How many parts for XYZ456'\n");
//            response.append("• Customer info: 'Customer status'\n");
//        }
//        response.append(addProfessionalNote());
//        return response.toString();
//    }
//
//    // Enhanced database query methods with better error handling
//    private String getContractInfo(String contractNumber) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("ContractsInfoRVO1Iterator");
//
//            //DCIteratorBinding contactsIter = bindings.findIteratorBinding("AwardContactsView1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setNamedWhereClauseParam("BINDAWARD", contractNumber);
//                vo.executeQuery();
//                if (vo.hasNext()) {
//
//                    System.out.println("Row found-->");
//                    StringBuilder response = new StringBuilder();
//                    response.append("<h3>Contract Information</h3>");
//                    response.append("<hr>");
//
//                    response.append("<pre>");
//                    response.append("<small>");
//                    response.append("<b>");
//                    response.append(String.format("%-15s %-20s %-12s %-20s %-12s %-15s %-12s", 
//                                                  "AWARD#", "AWARD NAME", "CUSTOMER#", "CUSTOMER NAME", 
//                                                  "EXPIRATION", "PRICE EXP", "OWNER"));
//                    response.append("</b>");
//                    response.append("<br>");
//                    response.append("=================================================================================================================");
//                    response.append("<br>");
//
//                    RowSetIterator rs = vo.createRowSetIterator(null);
//                    rs.reset();
//                    while (rs.hasNext()) {
//                        Row row = rs.next();
//                        
//                        String awardNumber = row.getAttribute("AwardNumber") != null ? 
//                                            row.getAttribute("AwardNumber").toString() : "N/A";
//                        String awardName = row.getAttribute("ContractName") != null ? 
//                                          row.getAttribute("ContractName").toString() : "N/A";
//                        String customerNumber = row.getAttribute("CustomerNumber") != null ? 
//                                               row.getAttribute("CustomerNumber").toString() : "N/A";
//                        String customerName = row.getAttribute("CustomerName") != null ? 
//                                             row.getAttribute("CustomerName").toString() : "N/A";
//                        String expirationDate = row.getAttribute("ExpirationDate") != null ? 
//                                               row.getAttribute("ExpirationDate").toString() : "N/A";
//                        String priceExpirationDate = row.getAttribute("PriceExpirationDate") != null ? 
//                                                    row.getAttribute("PriceExpirationDate").toString() : "N/A";
//                        String owner = row.getAttribute("AwardRep") != null ? 
//                                      row.getAttribute("AwardRep").toString() : "N/A";
//                        
//                        // Format dates
//                        expirationDate = formatDate(expirationDate);
//                        priceExpirationDate = formatDate(priceExpirationDate);
//                        
//                        // Truncate strings - significantly reduced award name length
//                        awardNumber = truncateString(awardNumber, 13);
//                        awardName = truncateString(awardName, 18);  // Reduced from 33 to 18
//                        customerNumber = truncateString(customerNumber, 10);
//                        customerName = truncateString(customerName, 18);  // Reduced from 26 to 18
//                        expirationDate = truncateString(expirationDate, 10);
//                        priceExpirationDate = truncateString(priceExpirationDate, 13);
//                        owner = truncateString(owner, 10);                        
//                        response.append(String.format("%-15s %-20s %-12s %-20s %-12s %-15s %-12s", 
//                                                      awardNumber, awardName, customerNumber, customerName, 
//                                                      expirationDate, priceExpirationDate, owner));
//                        response.append("<br>");
//                    }
//                    rs.closeRowSetIterator();
//
//                    response.append("=================================================================================================================");
//                    response.append("</small>");
//                    response.append("</pre>");
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return "Contract '" + contractNumber +
//                           "' not found. Please check the contract number and try again.";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving contract info: " + e.getMessage());
//        }
//
//        return "Unable to retrieve contract information at this time.";
//    }
//
//    private String getContractStatus(String contractNumber) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("ContractsView1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setWhereClause("UPPER(AWARD_NUMBER) = '" + contractNumber.toUpperCase() + "'");
//                vo.executeQuery();
//
//                if (vo.hasNext()) {
//                    Row row = vo.next();
//                    String status = (String) row.getAttribute("Status");
//                    Date expirationDate = (Date) row.getAttribute("ExpirationDate");
//
//                    StringBuilder response = new StringBuilder();
//                    response.append("Status for Contract ")
//                            .append(contractNumber)
//                            .append(":\n\n");
//                    response.append("Current Status: ")
//                            .append(status != null ? status : "ACTIVE")
//                            .append("\n");
//
//                    if (expirationDate != null) {
//                        long daysUntilExpiry =
//                            (expirationDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24);
//                        response.append("Expiration Date: ")
//                                .append(new java.text.SimpleDateFormat("MM/dd/yyyy").format(expirationDate))
//                                .append("\n");
//
//                        if (daysUntilExpiry > 0) {
//                            response.append("Days Until Expiry: ")
//                                    .append(daysUntilExpiry)
//                                    .append("\n");
//                            if (daysUntilExpiry <= 30) {
//                                response.append("WARNING: Contract expires soon!\n");
//                            }
//                        } else {
//                            response.append("Contract has expired!\n");
//                        }
//                    }
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return "Contract '" + contractNumber + "' not found.";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving contract status: " + e.getMessage());
//        }
//
//        return "Unable to retrieve contract status at this time.";
//    }
//
//    private String getPartsByContract(String contractNumber) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause("UPPER(LOADED_CP_NUMBER) = '" + contractNumber.toUpperCase() + "'");
//                vo.executeQuery();
//
//                int totalParts = vo.getRowCount();
//
//                if (totalParts > 0) {
//                    StringBuilder response = new StringBuilder();
//                    response.append("<h2>Parts Information for Contract #"+contractNumber+"</h2>");
//                    response.append("<hr>");
//
//                    response.append("<pre>");
//                    response.append("<b>");
//                    response.append(String.format("%-30s %-20s %-12s %-20s %-15s", "PARTS", "PRIME", "PRICE",
//                                                  "SAP NUMBER", "STATUS"));
//                    response.append("</b>");
//                    response.append("<br>");
//                    response.append("================================================================================================");
//                    response.append("<br>");
//
//                    int count = 0;
//                    double totalValue = 0.0;
//
//                    while (vo.hasNext() && count < 25) {
//                        Row row = vo.next();
//
//                        String partNumber =
//                            row.getAttribute("InvoicePartNumber") != null ?
//                            row.getAttribute("InvoicePartNumber").toString() : "N/A";
//                        String prime = row.getAttribute("Prime") != null ? row.getAttribute("Prime").toString() : "N/A";
//                        String priceStr = "N/A";
//                        Object priceObj = row.getAttribute("Price");
//                        if (priceObj != null) {
//                            double price = ((Number) priceObj).doubleValue();
//                            totalValue += price;
//                            priceStr = "$" + String.format("%.2f", price);
//                        }
//                        String sapNumber =
//                            row.getAttribute("SapNumber") != null ? row.getAttribute("SapNumber").toString() : "N/A";
//                        String status =
//                            row.getAttribute("Status") != null ? row.getAttribute("Status").toString() : "N/A";
//
//                        // Truncate only if absolutely necessary, but keep prime complete
//                        partNumber = truncateString(partNumber, 28);
//                        // prime - no truncation
//                        sapNumber = truncateString(sapNumber, 18);
//                        status = truncateString(status, 13);
//
//                        response.append(String.format("%-30s %-20s %-12s %-20s %-15s", partNumber, prime, priceStr,
//                                                      sapNumber, status));
//                        response.append("<br>");
//                        count++;
//                    }
//
//                    response.append("================================================================================================");
//                    response.append("</pre>");
//                    response.append("<hr>");
//                    if (totalParts > 25) {
//                        response.append("... and ")
//                                .append(totalParts - 25)
//                                .append(" more parts\n");
//                    }
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return "No parts found for contract '" + contractNumber + "'.";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving parts info: " + e.getMessage());
//        }
//
//        return " Unable to retrieve parts information at this time.";
//    }
//
//    private String getCustomerInfo(String customerName) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("ContractsView1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setWhereClause("UPPER(CUSTOMER_NAME) LIKE '%" + customerName.toUpperCase() + "%'");
//                vo.executeQuery();
//
//                int contractCount = vo.getRowCount();
//
//                if (contractCount > 0) {
//                    StringBuilder response = new StringBuilder();
//                    response.append(" Customer Information for ")
//                            .append(customerName)
//                            .append(":\n\n");
//                    response.append("Total Contracts: ")
//                            .append(contractCount)
//                            .append("\n\n");
//
//                    // Show recent contracts
//                    response.append("Recent Contracts:\n");
//                    int count = 0;
//                    while (vo.hasNext() && count < 3) {
//                        Row row = vo.next();
//                        response.append("• ").append(row.getAttribute("AwardNumber"));
//                        response.append(" - ").append(row.getAttribute("ContractName"));
//                        response.append(" (")
//                                .append(row.getAttribute("Status"))
//                                .append(")\n");
//                        count++;
//                    }
//
//                    if (contractCount > 3) {
//                        response.append("... and ")
//                                .append(contractCount - 3)
//                                .append(" more contracts\n");
//                    }
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return " No contracts found for customer '" + customerName + "'.";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving customer info: " + e.getMessage());
//        }
//
//        return " Unable to retrieve customer information at this time.";
//    }
//
//    private int getPartsCountForContract(String contractNumber) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause("UPPER(LOADED_CP_NUMBER) = '" + contractNumber.toUpperCase() + "'");
//                vo.executeQuery();
//                return vo.getRowCount();
//            }
//        } catch (Exception e) {
//            System.err.println("Error getting parts count: " + e.getMessage());
//        }
//        return 0;
//    }
//
//    private void addFieldToResponse(StringBuilder response, String fieldName, Object fieldValue) {
//        if (fieldValue != null) {
//            response.append(fieldName)
//                    .append(": ")
//                    .append(fieldValue)
//                    .append("\n");
//        }
//    }
//
//    // Additional helper methods for other query types
//    private String getContractsByCustomer(String customerName) {
//        return getCustomerInfo(customerName);
//    }
//
//    private String getContractsByStatus(String status) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("ContractsView1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setWhereClause("UPPER(STATUS) = '" + status.toUpperCase() + "'");
//                vo.executeQuery();
//
//                int count = vo.getRowCount();
//                StringBuilder response = new StringBuilder();
//                response.append(" Contracts with status '")
//                        .append(status)
//                        .append("': ")
//                        .append(count)
//                        .append("\n\n");
//
//                int displayCount = 0;
//                while (vo.hasNext() && displayCount < 5) {
//                    Row row = vo.next();
//                    response.append("• ").append(row.getAttribute("AwardNumber"));
//                    response.append(" - ").append(row.getAttribute("ContractName"));
//                    response.append(" (")
//                            .append(row.getAttribute("CustomerName"))
//                            .append(")\n");
//                    displayCount++;
//                }
//
//                if (count > 5) {
//                    response.append("... and ")
//                            .append(count - 5)
//                            .append(" more contracts\n");
//                }
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving contracts by status: " + e.getMessage());
//        }
//
//        return " Unable to retrieve contracts by status at this time.";
//    }
//
//    private String getCustomerContractStatus(String customerName) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("ContractsView1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setWhereClause("UPPER(CUSTOMER_NAME) LIKE '%" + customerName.toUpperCase() + "%'");
//                vo.executeQuery();
//
//                if (vo.hasNext()) {
//                    StringBuilder response = new StringBuilder();
//                    response.append(" Contract Status Summary for ")
//                            .append(customerName)
//                            .append(":\n\n");
//
//                    Map<String, Integer> statusCounts = new HashMap<>();
//                    int totalContracts = 0;
//
//                    while (vo.hasNext()) {
//                        Row row = vo.next();
//                        String status = (String) row.getAttribute("Status");
//                        if (status == null)
//                            status = "ACTIVE";
//
//                        statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
//                        totalContracts++;
//                    }
//
//                    response.append("Total Contracts: ")
//                            .append(totalContracts)
//                            .append("\n\n");
//                    response.append("Status Breakdown:\n");
//
//                    for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
//                        response.append("• ")
//                                .append(entry.getKey())
//                                .append(": ")
//                                .append(entry.getValue())
//                                .append("\n");
//                    }
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return " No contracts found for customer '" + customerName + "'.";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving customer contract status: " + e.getMessage());
//        }
//
//        return " Unable to retrieve customer contract status at this time.";
//    }
//
//    private String getPartsByCustomer(String customerName) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("awardcontactsRVO1Iterator");
//
//            if (partsIter != null && contractIter != null) {
//                // First get contract numbers for this customer
//                ViewObject contractVo = contractIter.getViewObject();
//                contractVo.setWhereClause("UPPER(AwardRep) LIKE '%" + customerName.toUpperCase() + "%'");
//                contractVo.executeQuery();
//
//                List<String> contractNumbers = new ArrayList<>();
//                while (contractVo.hasNext()) {
//                    Row row = contractVo.next();
//                    contractNumbers.add((String) row.getAttribute("AwardNumber"));
//                }
//
//                if (contractNumbers.isEmpty()) {
//                    return " No contracts found for customer '" + customerName + "'.";
//                }
//
//                // Build WHERE clause for parts query
//                StringBuilder whereClause = new StringBuilder("UPPER(LOADED_CP_NUMBER) IN (");
//                for (int i = 0; i < contractNumbers.size(); i++) {
//                    if (i > 0)
//                        whereClause.append(",");
//                    whereClause.append("'")
//                               .append(contractNumbers.get(i).toUpperCase())
//                               .append("'");
//                }
//                whereClause.append(")");
//
//                ViewObject partsVo = partsIter.getViewObject();
//                partsVo.setWhereClause(whereClause.toString());
//                partsVo.executeQuery();
//
//                int totalParts = partsVo.getRowCount();
//
//                StringBuilder response = new StringBuilder();
//                response.append(" Parts Summary for ")
//                        .append(customerName)
//                        .append(":\n\n");
//                response.append("Total Parts Across All Contracts: ")
//                        .append(totalParts)
//                        .append("\n");
//                response.append("Number of Contracts: ")
//                        .append(contractNumbers.size())
//                        .append("\n\n");
//
//                // Group parts by contract
//                Map<String, Integer> partsByContract = new HashMap<>();
//                double totalValue = 0;
//
//                while (partsVo.hasNext()) {
//                    Row row = partsVo.next();
//                    String contractNum = (String) row.getAttribute("LoadedCpNumber");
//                    partsByContract.put(contractNum, partsByContract.getOrDefault(contractNum, 0) + 1);
//
//                    Object priceObj = row.getAttribute("Price");
//                    if (priceObj != null) {
//                        totalValue += ((Number) priceObj).doubleValue();
//                    }
//                }
//
//                response.append("Parts by Contract:\n");
//                for (Map.Entry<String, Integer> entry : partsByContract.entrySet()) {
//                    response.append("• ")
//                            .append(entry.getKey())
//                            .append(": ")
//                            .append(entry.getValue())
//                            .append(" parts\n");
//                }
//
//                if (totalValue > 0) {
//                    response.append("\n Total Parts Value: $").append(String.format("%.2f", totalValue));
//                }
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving parts by customer: " + e.getMessage());
//        }
//
//        return " Unable to retrieve parts information for customer at this time.";
//    }
//
//    private String getTotalPartsInfo() {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause(null); // Get all parts
//                vo.executeQuery();
//
//                int totalParts = vo.getRowCount();
//                double totalValue = 0;
//                Map<String, Integer> partsByContract = new HashMap<>();
//
//                while (vo.hasNext()) {
//                    Row row = vo.next();
//                    String contractNum = (String) row.getAttribute("LoadedCpNumber");
//                    if (contractNum != null) {
//                        partsByContract.put(contractNum, partsByContract.getOrDefault(contractNum, 0) + 1);
//                    }
//
//                    Object priceObj = row.getAttribute("Price");
//                    if (priceObj != null) {
//                        totalValue += ((Number) priceObj).doubleValue();
//                    }
//                }
//
//                StringBuilder response = new StringBuilder();
//                response.append(" Total Parts Information:\n\n");
//                response.append("Total Parts in System: ")
//                        .append(totalParts)
//                        .append("\n");
//                response.append("Contracts with Parts: ")
//                        .append(partsByContract.size())
//                        .append("\n");
//
//                if (totalValue > 0) {
//                    response.append(" Total Parts Value: $")
//                            .append(String.format("%.2f", totalValue))
//                            .append("\n");
//                }
//
//                // Show top contracts by parts count
//                response.append("\nTop Contracts by Parts Count:\n");
//                partsByContract.entrySet()
//                               .stream()
//                               .sorted(Map.Entry
//                                          .<String, Integer>comparingByValue()
//                                          .reversed())
//                               .limit(5)
//                               .forEach(entry -> response.append("• ")
//                                                         .append(entry.getKey())
//                                                         .append(": ")
//                                                         .append(entry.getValue())
//                                                         .append(" parts\n"));
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving total parts info: " + e.getMessage());
//        }
//
//        return " Unable to retrieve total parts information at this time.";
//    }
//
//    private String getAllCustomersInfo() {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("ContractsView1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setWhereClause(null); // Get all contracts
//                vo.executeQuery();
//
//                Map<String, Integer> customerCounts = new HashMap<>();
//                int totalContracts = 0;
//
//                while (vo.hasNext()) {
//                    Row row = vo.next();
//                    String customerName = (String) row.getAttribute("CustomerName");
//                    if (customerName != null) {
//                        customerCounts.put(customerName, customerCounts.getOrDefault(customerName, 0) + 1);
//                    }
//                    totalContracts++;
//                }
//
//                StringBuilder response = new StringBuilder();
//                response.append(" All Customers Summary:\n\n");
//                response.append("Total Customers: ")
//                        .append(customerCounts.size())
//                        .append("\n");
//                response.append("Total Contracts: ")
//                        .append(totalContracts)
//                        .append("\n\n");
//
//                response.append("Top Customers by Contract Count:\n");
//                customerCounts.entrySet()
//                              .stream()
//                              .sorted(Map.Entry
//                                         .<String, Integer>comparingByValue()
//                                         .reversed())
//                              .limit(10)
//                              .forEach(entry -> response.append("• ")
//                                                        .append(entry.getKey())
//                                                        .append(": ")
//                                                        .append(entry.getValue())
//                                                        .append(" contracts\n"));
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving all customers info: " + e.getMessage());
//        }
//
//        return " Unable to retrieve customers information at this time.";
//    }
//
//    // Getter and setter for chatResponse
//    public String getChatResponse() {
//        return chatResponse;
//    }
//
//    public void setChatResponse(String chatResponse) {
//        this.chatResponse = chatResponse;
//    }
//
//    // Getter and setter for userInput
//    public String getUserInput() {
//        return userInput;
//    }
//
//    public void setUserInput(String userInput) {
//        this.userInput = userInput;
//    }
//
//    public void clearChat() {
//        this.chatResponse = "";
//        this.userInput = "";
//        this.userMessage = "";
//        this.chatHistory.clear();
//        // Add welcome message back
//        chatHistory.add(new ChatMessage("Bot",
//                                        "Hello! I'm your Contract Assistant with enhanced NLP capabilities. You can ask me about:\n" +
//                                        "• Contract information (e.g., 'Show contract ABC123')\n" +
//                                        "• Parts loaded count (e.g., 'How many parts for XYZ456')\n" +
//                                        "• Contract status (e.g., 'Status of ABC123')\n" +
//                                        "• Customer details (e.g., 'Boeing contracts')\n" + "• Failed contracts\n" +
//                                        "How can I help you today?", new Date(), true));
//    }
//
//
//    // Method to get chat history (if implementing chat history feature)
//    public List<ChatMessage> getChatHistory() {
//        // Implementation depends on whether you want to store chat history
//        return chatHistory;
//    }
//
//    // JSF Action Listener method called from the UI
//    public void processUserInput() {
//        try {
//            if (userInput != null && !userInput.trim().isEmpty()) {
//                System.out.println("ProcessUserInput--->" + userInput);
//                chatHistory.add(new ChatMessage("You", userInput, new Date(), false));
//
//                // Process the message and get bot response
//                String botResponse = userActionHandler.processUserInputCompleteResponse(userInput);//processUserMessage(userInput.toLowerCase().trim());
//
//                // Add bot response to chat
//                chatHistory.add(new ChatMessage("Bot", botResponse, new Date(), true));
//
//                for (ChatMessage ch : chatHistory) {
//                    System.out.println(ch.getMessage());
//
//                }
//                // Clear input
//                userInput = ""; // Log for debugging
//                System.out.println("User Input: " + userInput);
//                System.out.println("Bot Response: " + botResponse);
//
//                // Clear input
//                userInput = "";
//
//            } else {
//                String errorMsg = "Please enter a valid question or command.";
//                setChatResponse(errorMsg);
//                chatHistory.add(new ChatMessage("Bot", errorMsg, new Date(), true));
//            }
//        } catch (Exception e) {
//            System.err.println("Error in processUserInput: " + e.getMessage());
//            e.printStackTrace();
//            String errorMsg = "Sorry, I encountered an error processing your request. Please try again.";
//            setChatResponse(errorMsg);
//            chatHistory.add(new ChatMessage("Bot", errorMsg, new Date(), true));
//        }
//    }
//
//    //    public void sendMessage() {
//    //        System.out.println("================>" + userMessage);
//    //        if (userMessage != null && !userMessage.trim().isEmpty()) {
//    //            // Add user message to chat
//    //            chatHistory.add(new ChatMessage("You", userMessage, new Date(), false));
//    //
//    //            // Parse the user input using NLP
//    //            ParsedQuery parsedQuery = nlpService.parseUserInput(userMessage.toLowerCase().trim());
//    //            String intent = parsedQuery.getIntent().getCode();
//    //
//    //            // Handle contract creation help specially
//    //            if ("CONTRACT_CREATE_HELP".equals(intent.toUpperCase())) {
//    //                handleCreateContractHelpQuery(parsedQuery);
//    //            } else {
//    //                // Process other messages normally
//    //                String botResponse = processUserMessage(userMessage.toLowerCase().trim());
//    //                chatHistory.add(new ChatMessage("Bot", botResponse, new Date(), true));
//    //            }
//    //
//    //            // Clear input
//    //            userMessage = "";
//    //        }
//    //    }
//    private boolean isContractCreationQuery(String input) {
//        String normalized = input.toLowerCase();
//        return normalized.contains("create") && normalized.contains("contract") ||
//               normalized.contains("how") && normalized.contains("create") ||
//               normalized.contains("help") && normalized.contains("create");
//    }
//
//    // Method to provide image URLs for contract creation steps
//    private List<String> getContractCreationImages() {
//        List<String> imageUrls = new ArrayList<>();
//        // Add your image URLs here - these should be accessible from your application
//        imageUrls.add("/images/contract-help/step1-login-bcct");
//        imageUrls.add("/images/contract-help/step2-implementations-chart.png");
//        imageUrls.add("/images/contract-help/step3-create-award-link.png");
//        imageUrls.add("/images/contract-help/step4-create-award.png");
//        return imageUrls;
//    }
//
//   
//
//    public void setChatHistory(List<ChatMessage> chatHistory) {
//        this.chatHistory = chatHistory;
//    }
//
//    // Getter and setter for userMessage
//    public String getUserMessage() {
//        return userMessage;
//    }
//
//    public void setUserMessage(String userMessage) {
//        this.userMessage = userMessage;
//    }
//
//
//    public String test() {
//        // Add event code here...
//        return null;
//    }
//
//    private String handleCreateContractHelpQuery(ParsedQuery query) {
//        System.out.println("=== DEBUG: Handling create contract help query");
//
//        StringBuilder response = new StringBuilder();
//        response.append("<div style='font-family: Arial, sans-serif; line-height: 2.0; padding: 15px; background: #f8f9fa; border-radius: 8px;'>")
//                .append("<h2 style='color: #2c5aa0; margin-bottom: 15px;'>*** How to Create a New Contract***</h2>")
//                .append("<div style='font-size: 14px;'>")
//                .append("1.<strong>Login to BCCT Application</strong><br>")
//                .append("<div style='margin: 8px 0 15px 25px; color: #666;'>Enter your credentials and access the system</div>")
//                .append("<br><br>")
//                .append("2.<strong>Click on Award Management Link</strong><br>")
//                .append("<div style='margin: 8px 0 15px 25px; color: #666;'>Find and click 'Award Management' from the main menu</div>")
//                .append("<br><br>")
//                .append("3.<strong>Click on Award Implementation Pie Chart</strong><br>")
//                .append("<div style='margin: 8px 0 15px 25px; color: #666;'>Click anywhere on the pie chart or the count number</div>")
//                .append("<br><br>")
//                .append("4.<strong>Click 'Create Award Implementation' Button</strong><br>")
//                .append("<div style='margin: 8px 0 15px 25px; color: #666;'>This opens the contract creation form</div>")
//                .append("<br><br>")
//                .append("5.<strong>Fill Out Contract Details</strong><br>")
//                .append("<div style='margin: 8px 0 15px 25px; color: #666;'>Complete all required fields and submit</div>")
//                .append("</div>")
//                .append("</div>");
//        //
//        //        ChatMessage helpMessage = new ChatMessage("Bot", response.toString(), new Date(), true);
//        //        chatHistory.add(helpMessage);
//        response.append(addProfessionalNote());
//        return response.toString();
//    }
//
//
//    // Navigation methods
//    public void nextStep(ChatMessage message) {
//        if (message != null && message.isStepByStepMessage()) {
//            message.nextStep();
//        }
//    }
//
//    public void previousStep(ChatMessage message) {
//        if (message != null && message.isStepByStepMessage()) {
//            message.previousStep();
//        }
//    }
//
//    public void showAllSteps(ChatMessage message) {
//        if (message != null && message.isStepByStepMessage()) {
//            // Create a new message with all steps formatted
//            String allStepsFormatted = message.getAllStepsFormatted();
//            ChatMessage allStepsMessage = new ChatMessage("Bot", allStepsFormatted, new Date(), true);
//            chatHistory.add(allStepsMessage);
//        }
//    }
//
//
//    private ImageService imageService;
//
//    public void setImageService(ImageService imageService) {
//        this.imageService = imageService;
//    }
//
//    public void processUserInput(ClientEvent clientEvent) {
//        System.out.println("This is server listener call-->");
//        try {
//            if (userInput != null && !userInput.trim().isEmpty()) {
//                System.out.println("ProcessUserInput--->" + userInput);
//                chatHistory.add(new ChatMessage("You", userInput, new Date(), false));
//
//                // Process the message and get bot response
//                NLPUserActionHandler user=new NLPUserActionHandler().processUserInputCompleteResponse(userInput)
//                String botResponse = processUserMessage(userInput.toLowerCase().trim());
//
//                // Add bot response to chat
//                chatHistory.add(new ChatMessage("Bot", botResponse, new Date(), true));
//
//                for (ChatMessage ch : chatHistory) {
//                    System.out.println(ch.getMessage());
//
//                }
//                // Clear input
//                userInput = ""; // Log for debugging
//                System.out.println("User Input: " + userInput);
//                System.out.println("Bot Response: " + botResponse);
//
//                // Clear input
//                userInput = "";
//
//            } else {
//                String errorMsg = "Please enter a valid question or command.";
//                setChatResponse(errorMsg);
//                chatHistory.add(new ChatMessage("Bot", errorMsg, new Date(), true));
//            }
//        } catch (Exception e) {
//            System.err.println("Error in processUserInput: " + e.getMessage());
//            e.printStackTrace();
//            String errorMsg = "Sorry, I encountered an error processing your request. Please try again.";
//            setChatResponse(errorMsg);
//            chatHistory.add(new ChatMessage("Bot", errorMsg, new Date(), true));
//        }
//    }
//    // Add these new methods
//
//    private String extractUsername(ParsedQuery query) {
//        // Try entities first
//        Map<String, List<String>> entities = query.getEntities();
//        if (entities != null) {
//            // Try 'username' first (current format)
//            if (entities.containsKey("username")) {
//                List<String> usernames = entities.get("username");
//                if (usernames != null && !usernames.isEmpty()) {
//                    return usernames.get(0);
//                }
//            }
//            
//            // Try 'USER_NAME' for backward compatibility
//            if (entities.containsKey("USER_NAME")) {
//                List<String> usernames = entities.get("USER_NAME");
//                if (usernames != null && !usernames.isEmpty()) {
//                    return usernames.get(0);
//                }
//            }
//        }
//        
//        // Try parameters
//        Map<String, String> parameters = query.getParameters();
//        if (parameters != null) {
//            if (parameters.containsKey("username")) {
//                return parameters.get("username");
//            }
//            if (parameters.containsKey("USER_NAME")) {
//                return parameters.get("USER_NAME");
//            }
//        }
//        
//        return null;
//    }
//
//
//
//    private List<String> findMatchingUsers(String searchName) {
//        List<String> matchingUsers = new ArrayList<>();
//
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("awardcontactsRVO1Iterator");
//
//            if (contractIter != null) {
//                System.out.println("Searched user name searchName");
//                ViewObject vo = contractIter.getViewObject();
//                // Search for users whose names contain the search term
//                vo.setWhereClause("UPPER(AWARD_REP) LIKE '%" + searchName.toUpperCase() + "%'");
//                System.out.println(vo.getQuery());
//                vo.executeQuery();
//
//                // Collect unique user names
//                java.util.Set<String> uniqueUsers = new java.util.HashSet<>();
//                while (vo.hasNext()) {
//                    System.out.println("Found row-->");
//                    Row row = vo.next();
//                    String createdBy = (String) row.getAttribute("AwardRep");
//                    if (createdBy != null) {
//                        uniqueUsers.add(createdBy);
//                    }
//                }
//                matchingUsers.addAll(uniqueUsers);
//            }
//        } catch (Exception e) {
//            System.err.println("Error finding matching users: " + e.getMessage());
//        }
//
//        return matchingUsers;
//    }
//
//    private String requestUserConfirmation(String searchName, List<String> matchingUsers) {
//        // Set state for waiting confirmation
//        waitingForUserConfirmation = true;
//        searchedUserName = searchName;
//        matchedUsers = new ArrayList<>(matchingUsers);
//
//        StringBuilder response = new StringBuilder();
//        response.append("I found multiple users matching '")
//                .append(searchName)
//                .append("'. ");
//        response.append("Which user are you referring to?\n\n");
//
//        for (int i = 0; i < matchingUsers.size(); i++) {
//            response.append((i + 1))
//                    .append(". ")
//                    .append(matchingUsers.get(i))
//                    .append("\n");
//        }
//
//        response.append("\nPlease reply with the number or full name of the user you want.");
//        response.append(addProfessionalNote());
//        return response.toString();
//    }
//
//    private String handleUserConfirmation(String input) {
//        try {
//            // Check if input is a number (selection)
//            if (input.matches("\\d+")) {
//                int selection = Integer.parseInt(input.trim());
//                if (selection > 0 && selection <= matchedUsers.size()) { // Use matchedUsers instead of pendingUserMatches
//                    String selectedUser = matchedUsers.get(selection - 1);
//
//                    // Reset confirmation state
//                    waitingForUserConfirmation = false;
//                    searchedUserName = null;
//                    matchedUsers = null; // Clear the list
//
//                    return getContractsByUser(selectedUser);
//                } else {
//                    return "Invalid selection. Please enter a number between 1 and " + matchedUsers.size();
//                }
//            } else {
//                // User entered full name instead of number
//                String inputName = input.trim();
//
//                // Find exact match in the matched users list
//                for (String user : matchedUsers) {
//                    if (user.equalsIgnoreCase(inputName)) {
//                        // Reset confirmation state
//                        waitingForUserConfirmation = false;
//                        searchedUserName = null;
//                        matchedUsers = null;
//
//                        return getContractsByUser(user);
//                    }
//                }
//
//                // If no exact match found, show options again
//                return "User '" + inputName + "' not found in the list. Please select from:\n" +
//                       formatUserOptions(matchedUsers) + "\nReply with the number or exact full name.";
//            }
//        } catch (NumberFormatException e) {
//            // Handle non-numeric input that's not a valid name
//            return "Please enter a valid number (1-" + matchedUsers.size() +
//                   ") or the exact full name from the list above.";
//        }
//    }
//
//    private String formatUserOptions(List<String> users) {
//        StringBuilder options = new StringBuilder();
//        for (int i = 0; i < users.size(); i++) {
//            options.append((i + 1))
//                   .append(". ")
//                   .append(users.get(i))
//                   .append("\n");
//        }
//        return options.toString();
//    }
//
//
//    private String getContractsByUser(String userName) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding contractIter = bindings.findIteratorBinding("awardcontactsRVO1Iterator");
//
//            if (contractIter != null) {
//                ViewObject vo = contractIter.getViewObject();
//                vo.setWhereClause("UPPER(AWARD_REP) = '" + userName.toUpperCase() + "'");
//                vo.executeQuery();
//
//                int contractCount = vo.getRowCount();
//
//                if (contractCount == 0) {
//                    return "No contracts found created by '" + userName + "'.";
//                }
//
//                StringBuilder response = new StringBuilder();
//                response.append("<h2>Contracts created by ")
//                        .append(userName)
//                        .append("</h2>");
//                response.append("<hr>");
//                response.append("<p><big><b>Total Contracts: ")
//                        .append(contractCount)
//                        .append("</b></big></p>");
//                response.append("<hr>");
//                response.append("<pre>");
//                response.append("<b>");
//                response.append(String.format("%-10s %-20s %-18s %-10s %-12s %-12s", "CONTRACT#", "CONTRACT NAME",
//                                              "CUSTOMER NAME", "CUSTOMER#", "EXPIRATION", "PRICE EXP"));
//                response.append("</b>");
//                response.append("<br>");
//                response.append("=====================================================================================");
//                response.append("<br>");
//
//                RowSetIterator rs = vo.createRowSetIterator(null);
//                rs.reset();
//                while (rs.hasNext()) {
//                    Row row = rs.next();
//                    String awardNumber =
//                        row.getAttribute("AwardNumber") != null ? row.getAttribute("AwardNumber").toString() : "N/A";
//                    String contractName =
//                        row.getAttribute("ContractName") != null ? row.getAttribute("ContractName").toString() : "N/A";
//                    String customerName =
//                        row.getAttribute("CustomerName") != null ? row.getAttribute("CustomerName").toString() : "N/A";
//                    String customerNumber =
//                        row.getAttribute("CustomerNumber") != null ? row.getAttribute("CustomerNumber").toString() :
//                        "N/A";
//                    String expirationDate =
//                        row.getAttribute("ExpirationDate") != null ? row.getAttribute("ExpirationDate").toString() :
//                        "N/A";
//                    String priceExpirationDate =
//                        row.getAttribute("PriceExpirationDate") != null ?
//                        row.getAttribute("PriceExpirationDate").toString() : "N/A";
//
//                    // Format dates if needed (remove time portion if present)
//                    expirationDate = formatDate(expirationDate);
//                    priceExpirationDate = formatDate(priceExpirationDate);
//
//                    // Truncate strings to fit smaller columns
//                    awardNumber = truncateString(awardNumber, 9);
//                    contractName = truncateString(contractName, 19);
//                    customerName = truncateString(customerName, 17);
//                    customerNumber = truncateString(customerNumber, 9);
//                    expirationDate = truncateString(expirationDate, 11);
//                    priceExpirationDate = truncateString(priceExpirationDate, 11);
//
//                    response.append(String.format("%-10s %-20s %-18s %-10s %-12s %-12s", awardNumber, contractName,
//                                                  customerName, customerNumber, expirationDate, priceExpirationDate));
//                    response.append("<br>");
//                }
//                rs.closeRowSetIterator();
//                response.append("=====================================================================================");
//                response.append("</pre>");
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving contracts by user: " + e.getMessage());
//        }
//
//        return "Unable to retrieve contracts for user '" + userName + "' at this time.";
//    }
//
//    private String handleDetailedPartsQuery(ParsedQuery query) {
//        String contractNumber = query.extractNumber();
//        String lineNumber = query.getFirstEntity("LINE_NUMBER");
//        String partNumber = query.getFirstEntity("PART_NUMBER");
//        String originalInput = query.getOriginalInput().toLowerCase();
//
//        if (contractNumber == null) {
//            return "Please specify a contract number for parts information.";
//        }
//
//        // Determine specific parts query type
//        if (lineNumber != null) {
//            return handleLineSpecificQuery(contractNumber, lineNumber, originalInput);
//        } else if (partNumber != null) {
//            return handlePartSpecificQuery(contractNumber, partNumber, originalInput);
//        } else if (originalInput.contains("fully loaded")) {
//            return checkContractFullyLoaded(contractNumber);
//        } else if (originalInput.contains("duplicate")) {
//            return checkDuplicateParts(contractNumber);
//        } else if (originalInput.contains("summary")) {
//            return getPartsLoadingSummary(contractNumber);
//        } else {
//            // Default to existing parts method
//            return handlePartsInfoQuery(query);
//        }
//    }
//
//    // Line-specific query handler
//    private String handleLineSpecificQuery(String contractNumber, String lineNumber, String queryType) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause("UPPER(LOADED_CP_NUMBER) = '" + contractNumber.toUpperCase() +
//                                  "' AND LINE_NUMBER = " + lineNumber);
//                vo.executeQuery();
//
//                if (vo.hasNext()) {
//                    Row row = vo.next();
//                    StringBuilder response = new StringBuilder();
//                    response.append("Line ")
//                            .append(lineNumber)
//                            .append(" Details for Contract ")
//                            .append(contractNumber)
//                            .append(":\n\n");
//
//                    if (queryType.contains("quantity")) {
//                        addFieldToResponse(response, "Quantity", row.getAttribute("Eau"));
//                    } else if (queryType.contains("part number")) {
//                        addFieldToResponse(response, "Part Number", row.getAttribute("InvoicePartNumber"));
//                    } else if (queryType.contains("description")) {
//                        addFieldToResponse(response, "Description", row.getAttribute("Description"));
//                    } else if (queryType.contains("status") || queryType.contains("shipped")) {
//                        addFieldToResponse(response, "Status", row.getAttribute("Status"));
//                        addFieldToResponse(response, "Shipped Status", row.getAttribute("ShippedStatus"));
//                    } else {
//                        // Show all details
//                        addFieldToResponse(response, "Part Number", row.getAttribute("InvoicePartNumber"));
//                        addFieldToResponse(response, "Description", row.getAttribute("Description"));
//                        addFieldToResponse(response, "Quantity", row.getAttribute("Eau"));
//                        addFieldToResponse(response, "Price", row.getAttribute("Price"));
//                        addFieldToResponse(response, "Status", row.getAttribute("Status"));
//                    }
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return "Line " + lineNumber + " not found in contract " + contractNumber + ".";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving line details: " + e.getMessage());
//        }
//        return "Unable to retrieve line details at this time.";
//    }
//
//    // Part-specific query handler
//    private String handlePartSpecificQuery(String contractNumber, String partNumber, String queryType) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause("UPPER(LOADED_CP_NUMBER) = '" + contractNumber.toUpperCase() +
//                                  "' AND UPPER(INVOICE_PART_NUMBER) = '" + partNumber.toUpperCase() + "'");
//                vo.executeQuery();
//
//                if (vo.hasNext()) {
//                    StringBuilder response = new StringBuilder();
//                    response.append("Part ")
//                            .append(partNumber)
//                            .append(" in Contract ")
//                            .append(contractNumber)
//                            .append(":\n\n");
//
//                    int lineCount = 0;
//                    while (vo.hasNext()) {
//                        Row row = vo.next();
//                        lineCount++;
//
//                        if (queryType.contains("price")) {
//                            response.append("Line ")
//                                    .append(row.getAttribute("LineNumber"))
//                                    .append(": $")
//                                    .append(String.format("%.2f", ((Number) row.getAttribute("Price")).doubleValue()))
//                                    .append("\n");
//                        } else if (queryType.contains("lines include")) {
//                            response.append("Line ")
//                                    .append(row.getAttribute("LineNumber"))
//                                    .append("\n");
//                        } else {
//                            response.append("Line ")
//                                    .append(row.getAttribute("LineNumber"))
//                                    .append(" - Qty: ")
//                                    .append(row.getAttribute("Eau"))
//                                    .append(" - Price: $")
//                                    .append(String.format("%.2f", ((Number) row.getAttribute("Price")).doubleValue()))
//                                    .append("\n");
//                        }
//                    }
//
//                    response.append("\nTotal occurrences: ").append(lineCount);
//                    response.append(addProfessionalNote());
//                    return response.toString();
//                } else {
//                    return "Part number " + partNumber + " not found in contract " + contractNumber + ".";
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error retrieving part details: " + e.getMessage());
//        }
//        return "Unable to retrieve part details at this time.";
//    }
//
//    // Check if contract is fully loaded
//    private String checkContractFullyLoaded(String contractNumber) {
//        // Implementation depends on your business logic for "fully loaded"
//        // This is a placeholder - you'll need to define what constitutes "fully loaded"
//        return "Contract loading status check for " + contractNumber + " - Feature under development.";
//    }
//
//    // Check for duplicate parts
//    private String checkDuplicateParts(String contractNumber) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause("UPPER(LOADED_CP_NUMBER) = '" + contractNumber.toUpperCase() + "'");
//                vo.executeQuery();
//
//                Map<String, Integer> partCounts = new HashMap<>();
//                while (vo.hasNext()) {
//                    Row row = vo.next();
//                    String partNumber = (String) row.getAttribute("InvoicePartNumber");
//                    if (partNumber != null) {
//                        partCounts.put(partNumber, partCounts.getOrDefault(partNumber, 0) + 1);
//                    }
//                }
//
//                StringBuilder response = new StringBuilder();
//                response.append("Duplicate Parts Analysis for Contract ")
//                        .append(contractNumber)
//                        .append(":\n\n");
//
//                boolean foundDuplicates = false;
//                for (Map.Entry<String, Integer> entry : partCounts.entrySet()) {
//                    if (entry.getValue() > 1) {
//                        foundDuplicates = true;
//                        response.append("• ")
//                                .append(entry.getKey())
//                                .append(": ")
//                                .append(entry.getValue())
//                                .append(" occurrences\n");
//                    }
//                }
//
//                if (!foundDuplicates) {
//                    response.append("No duplicate parts found in contract ")
//                            .append(contractNumber)
//                            .append(".");
//                }
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error checking duplicates: " + e.getMessage());
//        }
//        return "Unable to check for duplicate parts at this time.";
//    }
//
//    private String getPartsLoadingSummary(String contractNumber) {
//        try {
//            DCBindingContainer bindings = (DCBindingContainer) BindingContext.getCurrent().getCurrentBindingsEntry();
//            DCIteratorBinding partsIter = bindings.findIteratorBinding("AwardsFinalPartsView1Iterator");
//
//            if (partsIter != null) {
//                ViewObject vo = partsIter.getViewObject();
//                vo.setWhereClause("UPPER(LOADED_CP_NUMBER) = '" + contractNumber.toUpperCase() + "'");
//                vo.executeQuery();
//
//                Map<String, Integer> partCounts = new HashMap<>();
//                while (vo.hasNext()) {
//                    Row row = vo.next();
//                    String partNumber = (String) row.getAttribute("InvoicePartNumber");
//                    if (partNumber != null) {
//                        partCounts.put(partNumber, partCounts.getOrDefault(partNumber, 0) + 1);
//                    }
//                }
//
//                StringBuilder response = new StringBuilder();
//                response.append("Duplicate Parts Analysis for Contract ")
//                        .append(contractNumber)
//                        .append(":\n\n");
//
//                boolean foundDuplicates = false;
//                for (Map.Entry<String, Integer> entry : partCounts.entrySet()) {
//                    if (entry.getValue() > 1) {
//                        foundDuplicates = true;
//                        response.append("• ")
//                                .append(entry.getKey())
//                                .append(": ")
//                                .append(entry.getValue())
//                                .append(" occurrences\n");
//                    }
//                }
//
//                if (!foundDuplicates) {
//                    response.append("No duplicate parts found in contract ")
//                            .append(contractNumber)
//                            .append(".");
//                }
//                response.append(addProfessionalNote());
//                return response.toString();
//            }
//        } catch (Exception e) {
//            System.err.println("Error checking duplicates: " + e.getMessage());
//        }
//        return "Unable to check for duplicate parts at this time.";
//    }
//
//    private String truncateString(String str, int maxLength) {
//        if (str == null)
//            return "N/A";
//        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
//    }
//
//
//    private String formatDate(String dateStr) {
//        if (dateStr == null || dateStr.equals("N/A"))
//            return "N/A";
//        // If date contains time, extract only date part
//        if (dateStr.contains(" ")) {
//            return dateStr.split(" ")[0];
//        }
//        return dateStr;
//    }
//    private String handleUserContractQuery(ParsedQuery query) {
//        System.out.println("handleUserContractQuery--------------->");
//        
//        // Check if we're waiting for confirmation and this is a numeric selection
//        if (waitingForUserConfirmation) {
//            String input = query.getOriginalInput().trim();
//            if (input.matches("\\d+")) {
//                // User entered a number, handle the selection
//                return handleUserConfirmation(input);
//            }
//        }
//        
//        // FIXED: Try multiple sources to get the username
//        String userName = null;
//        
//        // Method 1: Try parameters first (from new parsing method)
//        Map<String, String> parameters = query.getParameters();
//        if (parameters != null && parameters.containsKey("username")) {
//            userName = parameters.get("username");
//            System.out.println("User name from parameters--->" + userName);
//        }
//        
//        // Method 2: Try "username" entity
//        if (userName == null) {
//            Map<String, List<String>> entities = query.getEntities();
//            if (entities != null && entities.containsKey("username")) {
//                List<String> usernames = entities.get("username");
//                if (usernames != null && !usernames.isEmpty()) {
//                    userName = usernames.get(0);
//                    System.out.println("User name from username entity--->" + userName);
//                }
//            }
//        }
//        
//        // Method 3: Try "USER_NAME" entity (backward compatibility)
//        if (userName == null) {
//            Map<String, List<String>> entities = query.getEntities();
//            if (entities != null && entities.containsKey("USER_NAME")) {
//                List<String> usernames = entities.get("USER_NAME");
//                if (usernames != null && !usernames.isEmpty()) {
//                    userName = usernames.get(0);
//                    System.out.println("User name from USER_NAME entity--->" + userName);
//                }
//            }
//        }
//        
//        // Method 4: Try extractUserName method from ParsedQuery
//        if (userName == null) {
//            userName = query.extractUserName();
//            System.out.println("User name from extractUserName method--->" + userName);
//        }
//        
//        // Method 5: Last resort - extract from original input
//        if (userName == null) {
//            userName = extractUserNameFromInput(query.getOriginalInput());
//            System.out.println("User name from direct extraction--->" + userName);
//        }
//        
//        System.out.println("Final User name--->" + userName);
//        
//        if (userName == null || userName.trim().isEmpty()) {
//            return "Please specify a user name to search for contracts.";
//        }
//        
//        // Search for matching users
//        List<String> matchingUsers = findMatchingUsers(userName.trim());
//        
//        if (matchingUsers.isEmpty()) {
//            return "No users found matching '" + userName + "'. Please check the name and try again.";
//        } else if (matchingUsers.size() == 1) {
//            // Exact match found, get contracts directly
//            return getContractsByUser(matchingUsers.get(0));
//        } else {
//            // Multiple matches found, ask for confirmation
//            return requestUserConfirmation(userName, matchingUsers);
//        }
//    }
//
//    // HELPER: Extract username from input as fallback
//    private String extractUserNameFromInput(String input) {
//        String normalizedInput = input.toLowerCase().trim();
//        
//        // Pattern 1: "contracts by [username]"
//        Pattern byPattern = Pattern.compile("contracts?\\s+by\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
//        Matcher byMatcher = byPattern.matcher(normalizedInput);
//        if (byMatcher.find()) {
//            return byMatcher.group(1);
//        }
//        
//        // Pattern 2: "contracts for [username]"
//        Pattern forPattern = Pattern.compile("contracts?\\s+for\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
//        Matcher forMatcher = forPattern.matcher(normalizedInput);
//        if (forMatcher.find()) {
//            return forMatcher.group(1);
//        }
//        
//        // Pattern 3: "contracts created by [username]"
//        Pattern createdPattern = Pattern.compile("contracts?\\s+created\\s+by\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
//        Matcher createdMatcher = createdPattern.matcher(normalizedInput);
//        if (createdMatcher.find()) {
//            return createdMatcher.group(1);
//        }
//        
//        // Pattern 4: "[username] contracts"
//        Pattern userFirstPattern = Pattern.compile("([a-zA-Z]+)\\s+contracts?", Pattern.CASE_INSENSITIVE);
//        Matcher userFirstMatcher = userFirstPattern.matcher(normalizedInput);
//        if (userFirstMatcher.find()) {
//            String possibleUser = userFirstMatcher.group(1);
//            // Make sure it's not a common word
//            if (!possibleUser.matches("(?i)(show|get|find|all|my|the|a|an)")) {
//                return possibleUser;
//            }
//        }
//        
//        return null;
//    }
//    
//    private String addProfessionalNote() {
//        return "<hr>" +
//               "<h4>Need More Information?</h4>" +
//               "<ul>" +
//               "<li><b>Award Management</b> - View complete contract details</li>" +
//               "<li><b>Contract Analytics</b> - Access comprehensive reports</li>" +
//               "</ul>" +
//               "<p><small><i>Visit these screens for full functionality &amp; detailed analysis.</i></small></p>" +
//               "<hr>";
//    }
//
//    public void testingAction(ActionEvent actionEvent) {
//        try {
//                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                FileWriter writer = new FileWriter("C:\\BCCT_WORK_BEN\\ChatBot\\8July2025\\BCCTChatBot\\test_responses_" + timestamp + ".md");
//                
//                int count = 0;
//                for (String input : TestQueries.PARTS_QUERIES) {
//                    count++;
//                    
//                    try {
//                        String res = userActionHandler.processUserInputCompleteResponse(input);
//                        String dataProviderRes = BCCTChatBotUtility.extractDataProviderResponseFromJson(res);
//                        
//                        writer.write("=== QUERY " + count + " ===\n");
//                        writer.write("INPUT: " + input + "\n");
//                        writer.write("COMPLETE RESPONSE:\n" + res + "\n");
//                      //  writer.write("DATA PROVIDER RESPONSE:\n" + dataProviderRes + "\n");
//                        writer.write("\n" + "================================" + "\n\n");
//                        
//                        System.out.println("Query " + count + " processed and written to file");
//                        
//                    } catch (Exception e) {
//                        writer.write("=== QUERY " + count + " (ERROR) ===\n");
//                        writer.write("INPUT: " + input + "\n");
//                        writer.write("ERROR: " + e.getMessage() + "\n");
//                        writer.write("\n" + "=======================================================" + "\n\n");
//                        
//                        System.out.println("Error in Query " + count + ": " + e.getMessage());
//                    }
//                }
//                
//                writer.close();
//                System.out.println("All responses written to file: test_responses_" + timestamp + ".txt");
//                
//            } catch (Exception e) {
//                System.err.println("Error writing to file: " + e.getMessage());
//            }
//    }
//    
//    public static void main(String v[]){
//            try {
//                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                    FileWriter writer = new FileWriter("test_responses_" + timestamp + ".txt");
//                    
//                    int count = 0;
//                 NLPUserActionHandler   ob=new NLPUserActionHandler();
//                    for (String input : TestQueries.GETALL_QUERIES()) {
//                        count++;
//                        
//                        try {
//                    UserActionResponse res = ob.processUserInputCompleteObject(input);
//                            
//                   // String res = ob.processUserInputCompleteResponse(input);
//                    //        String dataProviderRes = BCCTChatBotUtility.extractDataProviderResponseFromJson(res);
//                            
//                            writer.write("=== QUERY " + count + " ===\n");
//                            writer.write("INPUT: " + input + "\n");
//                            writer.write("Corrected INPUT:\n" + res.getCorrectedInput() + "\n");
//                            writer.write("Message :\n" + res.getMessage() + "\n");
//                            writer.write("Query Type :"+res.getQueryType()+"Action Type:" + res.getActionType() + "\n");
//                            writer.write("Display Items:"+res.getDisplayEntities());
//                            String filters="";
//                            writer.write("Filters ===>") ;
//                            for(NLPEntityProcessor.EntityFilter fl:res.getFilters()){
//                          String   S="Attribute :"+fl.attribute+" Operation :"+fl.operation+", Value :"+fl.value;
//                                
//                                writer.write(S) ;
//                            
//                            }
//                            writer.write("Filters ===>\n") ;
//                            for(Map<String, Object> data:res.getResultSet()){
//                                writer.write("\n===============" + data + "========================\n\n");
//                            }
//                            writer.write("\n" + "================================" + "\n\n");
//                            
//                            System.out.println("Query " + count + " processed and written to file");
//                            
//                        } catch (Exception e) {
//                            writer.write("=== QUERY " + count + " (ERROR) ===\n");
//                            writer.write("INPUT: " + input + "\n");
//                            writer.write("ERROR: " + e.getMessage() + "\n");
//                            writer.write("\n" + "=======================================================" + "\n\n");
//                            
//                            System.out.println("Error in Query " + count + ": " + e.getMessage());
//                        }
//                    }
//                    
//                    writer.close();
//                    System.out.println("All responses written to file: test_responses_" + timestamp + ".txt");
//                    
//                } catch (Exception e) {
//                    System.err.println("Error writing to file: " + e.getMessage());
//                }
//            
//        } 
//}
//
