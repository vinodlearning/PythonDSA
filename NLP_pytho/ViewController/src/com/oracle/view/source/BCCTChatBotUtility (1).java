//package com.ben.view.nlp;
//
//import com.ben.model.module.BCCTCharBotAppModuleImpl;
//
//import javax.el.ELContext;
//import javax.el.ExpressionFactory;
//import javax.el.ValueExpression;
//
//import javax.faces.application.Application;
//import javax.faces.context.FacesContext;
//
//import oracle.adf.model.binding.DCBindingContainer;
//import oracle.adf.model.binding.DCIteratorBinding;
//
//import oracle.binding.BindingContainer;
//import oracle.binding.OperationBinding;
//
//public class BCCTChatBotUtility {
//    public BCCTChatBotUtility() {
//        super();
//    }
//
//    public static BCCTCharBotAppModuleImpl getBCCTCharBotAppModuleImpl() {
//        FacesContext fc = FacesContext.getCurrentInstance();
//        Application app = fc.getApplication();
//        ExpressionFactory elFactory = app.getExpressionFactory();
//        ELContext elContext = fc.getELContext();
//        ValueExpression valueExp =
//            elFactory.createValueExpression(elContext, "#{data.BCCTCharBotAppModuleDataControl.dataProvider}",
//                                            Object.class);
//        return (BCCTCharBotAppModuleImpl) valueExp.getValue(elContext);
//    }
//
//    public static OperationBinding findOperationBinding(String operationName) {
//        return getDCBindingContainer().getOperationBinding(operationName);
//    }
//
//    public static DCBindingContainer getDCBindingContainer() {
//        return (DCBindingContainer) getBindingContainer();
//    }
//
//    public static BindingContainer getBindingContainer() {
//        return (BindingContainer) resolveExpression("#{bindings}");
//    }
//
//    public static Object resolveExpression(String expression) {
//        FacesContext facesContext = getFacesContext();
//        Application app = facesContext.getApplication();
//        ExpressionFactory elFactory = app.getExpressionFactory();
//        ELContext elContext = facesContext.getELContext();
//        ValueExpression valueExp = elFactory.createValueExpression(elContext, expression, Object.class);
//        return valueExp.getValue(elContext);
//    }
//
//    public static FacesContext getFacesContext() {
//        return FacesContext.getCurrentInstance();
//    }
//
//    public static DCIteratorBinding findIterator(String name) {
//        DCIteratorBinding iter = getDCBindingContainer().findIteratorBinding(name);
//        if (iter == null) {
//            throw new RuntimeException("Iterator '" + name + "' not found");
//        }
//        return iter;
//    }
//
//
//    public static String getHelpContent(String actionType) {
//        switch (actionType) {
//            case "HELP_CONTRACT_CREATE_USER":
//                return "<div style='padding: 15px; border: 1px solid #28a745; border-radius: 5px; background-color: #f8fff9;'>" +
//                       "<h4 style='color: #28a745; margin-top: 0;'>Contract Creation Steps</h4>" +
//                       "<ol>" +
//                       "<li><strong>Gather Required Information:</strong>" +
//                       "<ul><li>Contract/Award Number</li><li>Contract Name</li><li>Customer Details</li><li>Dates (Effective, Expiration, Price Expiration)</li></ul></li>" +
//                       "<li><strong>Access Contract Module:</strong> Navigate to the contracts section in your system</li>" +
//                       "<li><strong>Fill Contract Details:</strong> Enter all required information accurately</li>" +
//                       "<li><strong>Review and Submit:</strong> Double-check all information before saving</li>" +
//                       "<li><strong>Confirmation:</strong> Wait for system confirmation of contract creation</li>" +
//                       "</ol>" +
//                       "<p><strong>Note:</strong> Ensure you have proper authorization before creating contracts.</p>" +
//                       "</div>";
//            case "HELP_CONTRACT_CREATE_BOT":
//                return "<div style='padding: 15px; border: 1px solid #17a2b8; border-radius: 5px; background-color: #f0fdff;'>" +
//                       "<h4 style='color: #17a2b8; margin-top: 0;'>Automated Contract Creation</h4>" +
//                       "<p>I can help you create a contract! Please provide the following details:</p>" +
//                       "<div style='margin: 10px 0;'>" +
//                       "<strong>Required Information:</strong>" +
//                       "<ul>" +
//                       "<li>Contract/Award Number</li>" +
//                       "<li>Contract Name</li>" +
//                       "<li>Customer Number and Name</li>" +
//                       "<li>Effective Date</li>" +
//                       "<li>Expiration Date</li>" +
//                       "<li>Price Expiration Date</li>" +
//                       "<li>Award Representative</li>" +
//                       "</ul>" +
//                       "</div>" +
//                       "<p><strong>Example:</strong> \"Create contract ABC123 named 'IT Services Contract' for customer C001 'Tech Corp' effective 2024-01-01 expiring 2024-12-31\"</p>" +
//                       "<p><em>Please provide the contract details and I'll help you create it.</em></p>" +
//                       "</div>";
//            default:
//                return "<div style='padding: 15px; border: 1px solid #ffc107; border-radius: 5px; background-color: #fff3cd;'>" +
//                       "<h4 style='color: #856404; margin-top: 0;'>Help Available</h4>" +
//                       "<p>I can help you with:</p>" +
//                       "<ul>" +
//                       "<li>Contract queries and information</li>" +
//                       "<li>Parts information and details</li>" +
//                       "<li>Failed parts analysis</li>" +
//                       "<li>Contract creation guidance</li>" +
//                       "</ul>" +
//                       "<p>Please ask me specific questions about contracts or parts.</p>" +
//                       "</div>";
//        }
//    }
//
//    public static String createCompleteResponseJSONWithHelpContent(Object queryResult, String helpContent) {
//        StringBuilder json = new StringBuilder();
//        json.append("{\n");
//        json.append("  \"success\": true,\n");
//        json.append("  \"message\": \"Help request processed successfully\",\n");
//        json.append("  \"isHelpQuery\": true,\n");
//        json.append("  \"nlpResponse\": {},\n");
//        json.append("  \"helpContent\": \"").append(escapeJson(helpContent)).append("\"\n");
//        json.append("}");
//        return json.toString();
//    }
//
//    public static String escapeJson(String input) {
//        if (input == null) return "";
//        return input.replace("\\", "\\\\")
//                   .replace("\"", "\\\"")
//                   .replace("\n", "\\n")
//                   .replace("\r", "\\r")
//                   .replace("\t", "\\t");
//    }
//
//    public static String extractDataProviderResponseFromJson(String jsonResponse) {
//        try {
//            String searchKey = "\"dataProviderResponse\": \"";
//            int startIndex = jsonResponse.indexOf(searchKey);
//            if (startIndex == -1) {
//                return "<div style='color: orange;'>No data content found in response</div>";
//            }
//            startIndex += searchKey.length();
//            int endIndex = findEndOfJsonStringValue(jsonResponse, startIndex);
//            if (endIndex == -1) {
//                return "<div style='color: red;'>Malformed JSON response</div>";
//            }
//            String htmlContent = jsonResponse.substring(startIndex, endIndex);
//            return unescapeJsonString(htmlContent);
//        } catch (Exception e) {
//            return "<div style='color: red;'>Error extracting data content: " + e.getMessage() + "</div>";
//        }
//    }
//
//    public static String extractHelpContentFromJson(String jsonResponse) {
//        try {
//            String searchKey = "\"helpContent\": \"";
//            int startIndex = jsonResponse.indexOf(searchKey);
//            if (startIndex == -1) {
//                return "<div style='color: orange;'>No help content found in response</div>";
//            }
//            startIndex += searchKey.length();
//            int endIndex = findEndOfJsonStringValue(jsonResponse, startIndex);
//            if (endIndex == -1) {
//                return "<div style='color: red;'>Malformed JSON response</div>";
//            }
//            String htmlContent = jsonResponse.substring(startIndex, endIndex);
//            return unescapeJsonString(htmlContent);
//        } catch (Exception e) {
//            return "<div style='color: red;'>Error extracting help content: " + e.getMessage() + "</div>";
//        }
//    }
//
//    public static int findEndOfJsonStringValue(String json, int startIndex) {
//        boolean escape = false;
//        for (int i = startIndex; i < json.length(); i++) {
//            char c = json.charAt(i);
//            if (c == '\\' && !escape) {
//                escape = true;
//            } else if (c == '"' && !escape) {
//                return i;
//            } else {
//                escape = false;
//            }
//        }
//        return -1;
//    }
//
//    public static String unescapeJsonString(String escapedString) {
//        if (escapedString == null) return null;
//        return escapedString.replace("\\\"", "\"")
//                            .replace("\\n", "\n")
//                            .replace("\\r", "\r")
//                            .replace("\\t", "\t")
//                            .replace("\\\\", "\\");
//    }
//
//    public static String extractHtmlContentFromJsonResponse(String jsonResponse) {
//        String dataContent = extractDataProviderResponseFromJson(jsonResponse);
//        if (dataContent != null && !dataContent.contains("Error")) {
//            return dataContent;
//        }
//        String helpContent = extractHelpContentFromJson(jsonResponse);
//        if (helpContent != null && !helpContent.contains("Error")) {
//            return helpContent;
//        }
//        return "<div style='color: orange;'>No HTML content found in response</div>";
//    }
//
//
//}
//
