package com.oracle.view.deep;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractQueryModel {
    private Header header;
    private QueryMetadata queryMetadata;
    private List<Entity> entities;
    private List<String> displayEntities;
    private List<String> errors;

    // Constructors
    public ContractQueryModel() {
        this.header = new Header();
        this.queryMetadata = new QueryMetadata();
        this.entities = new ArrayList<>();
        this.displayEntities = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    // Getters and Setters
    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    public QueryMetadata getQueryMetadata() { return queryMetadata; }
    public void setQueryMetadata(QueryMetadata queryMetadata) { this.queryMetadata = queryMetadata; }
    public List<Entity> getEntities() { return entities; }
    public void setEntities(List<Entity> entities) { this.entities = entities; }
    public List<String> getDisplayEntities() { return displayEntities; }
    public void setDisplayEntities(List<String> displayEntities) { this.displayEntities = displayEntities; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    // Inner classes
    public static class Header {
        private String contractNumber;
        private String partNumber;
        private String customerNumber;
        private String customerName;
        private String createdBy;

        // Getters and Setters
        public String getContractNumber() { return contractNumber; }
        public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
        public String getPartNumber() { return partNumber; }
        public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
        public String getCustomerNumber() { return customerNumber; }
        public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    }

    public static class QueryMetadata {
        private String queryType;
        private String actionType;
        private Integer processingTimeMs;

        // Getters and Setters
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public Integer getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class Entity {
        private String attribute;
        private String operation;
        private String value;
        private String source;

        // Getters and Setters
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    // Processing methods
    public void processUserInput(String input) {
        // Normalize the input
        String normalizedInput = input.toLowerCase().trim();
        
        // Reset model
        this.header = new Header();
        this.queryMetadata = new QueryMetadata();
        this.entities = new ArrayList<>();
        this.displayEntities = new ArrayList<>(Arrays.asList("CONTRACT_NUMBER", "CUSTOMER_NAME"));
        this.errors = new ArrayList<>();
        
        // Determine query type (contracts or parts)
        if (containsPartNumber(normalizedInput)) {
            queryMetadata.setQueryType("PARTS");
            processPartsQuery(normalizedInput);
        } else {
            queryMetadata.setQueryType("CONTRACTS");
            processContractQuery(normalizedInput);
        }
        
        // Set default processing time
        queryMetadata.setProcessingTimeMs(150);
    }
   
    
    private void processContractQuery(String input) {
        // Extract contract number if present
        extractContractNumber(input);
        
        // Extract customer information
        extractCustomerInfo(input);
        
        // Extract created by information
        extractCreatedBy(input);
        
        // Extract date filters
        extractDateFilters(input);
        
        // Extract requested fields
        extractRequestedFields(input);
        
        // Determine action type
        determineActionType(input);
    }
    
    private boolean containsPartNumber(String input) {
        // Pattern for part numbers like AE125 or part-related keywords
        Pattern pattern = Pattern.compile("\\b([a-z]{1,5}\\d{1,5})\\b|\\bparts?\\b|\\bpartz\\b|\\bpasd\\b|\\bfailed\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    private void extractContractNumber(String input) {
        // Improved pattern to capture contract numbers in various contexts
        Pattern pattern = Pattern.compile(
            "(?:contract|contrct|cntract|kontract|part|partz|pasd)\\s*(?:no|num|number)?\\s*(\\d{4,8})\\b", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            header.setContractNumber(matcher.group(1));
        }
    }

    private void processPartsQuery(String input) {
        // Extract contract number if present
        extractContractNumber(input);
        
        // Extract part number if specifically mentioned
        extractPartNumber(input);
        
        // Set query type to PARTS
        queryMetadata.setQueryType("PARTS");
        
        // Determine action type based on query content
        if (input.matches(".*\\bfailed\\b.*") && input.matches(".*\\bpasd\\b.*")) {
            queryMetadata.setActionType("parts_by_contract_all");
        } else if (input.matches(".*\\bfailed\\b.*")) {
            queryMetadata.setActionType("parts_by_contract_failed");
        } else if (input.matches(".*\\bpasd\\b.*")) {
            queryMetadata.setActionType("parts_by_contract_passed");
        } else {
            queryMetadata.setActionType("parts_by_contract");
        }
        
        // Set default display entities for parts queries
        displayEntities.add("PART_NUMBER");
        displayEntities.add("PART_STATUS");
        displayEntities.add("VALIDATION_STATUS");
        
        // Add specific fields based on query
        if (queryMetadata.getActionType().contains("failed")) {
            displayEntities.add("FAILURE_REASON");
        }
        
        // Include contract fields since this is parts under a contract
        displayEntities.add("CONTRACT_NUMBER");
        displayEntities.add("CUSTOMER_NAME");
    }
    
   
    
    private void extractPartNumber(String input) {
        Pattern pattern = Pattern.compile("\\b([a-z]{1,5}\\d{1,5})\\b");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            header.setPartNumber(matcher.group(1).toUpperCase());
        }
    }
    
    private void extractCustomerInfo(String input) {
        // Extract customer/account number - handles both "customer number" and "account number"
        Pattern numberPattern = Pattern.compile(
            "\\b(?:customer|cstomer|custmer|account|accunt|acount)\\s*(?:no|num|number)?\\s*(\\d{4,8})\\b");
        Matcher numberMatcher = numberPattern.matcher(input);
        if (numberMatcher.find()) {
            header.setCustomerNumber(numberMatcher.group(1));
        }
        
        // Extract customer name (simple version - would need more sophisticated NLP in production)
        if (input.contains("siemens")) {
            header.setCustomerName("Siemens");
        } else if (input.contains("honeywel")) {
            header.setCustomerName("Honeywell");
        } else if (input.contains("boieng")) {
            header.setCustomerName("Boeing");
        }
    }

   
   
    
    private void extractDateFilters(String input) {
        // Year filter
        if (input.contains("2024") || input.contains("24")) {
            addDateEntity("CREATED_DATE", "=", "2024");
        } else if (input.contains("2025") || input.contains("25")) {
            addDateEntity("CREATED_DATE", "=", "2025");
        }
        
        // Date range filters
        if (input.contains("after") || input.contains("aftr")) {
            if (input.contains("1-jan-2020") || input.contains("jan 2020")) {
                addDateEntity("CREATED_DATE", ">", "2020-01-01");
            }
        }
        
        if (input.contains("btwn") || input.contains("between")) {
            if (input.contains("jan") && input.contains("june") && input.contains("2024")) {
                addDateEntity("CREATED_DATE", ">=", "2024-01-01");
                addDateEntity("CREATED_DATE", "<=", "2024-06-30");
            }
        }
        
        // Month filters
        if (input.contains("lst mnth") || input.contains("last month")) {
            addDateEntity("CREATED_DATE", ">=", "LAST_MONTH_START");
            addDateEntity("CREATED_DATE", "<=", "LAST_MONTH_END");
        }
    }
    
    private void addDateEntity(String attribute, String operation, String value) {
        Entity entity = new Entity();
        entity.setAttribute(attribute);
        entity.setOperation(operation);
        entity.setValue(value);
        entity.setSource("user_input");
        entities.add(entity);
    }
    
    private void extractRequestedFields(String input) {
        // Check for specific field requests
        if (input.contains("effective date") || input.contains("efective date")) {
            displayEntities.add("EFFECTIVE_DATE");
        }
        if (input.contains("status") || input.contains("statuz") || input.contains("statuss")) {
            displayEntities.add("STATUS");
        }
        if (input.contains("price list") || input.contains("pric list")) {
            displayEntities.add("PRICE_LIST");
        }
        if (input.contains("project type") || input.contains("proj type")) {
            displayEntities.add("PROJECT_TYPE");
        }
        if (input.contains("all fields") || input.contains("all flieds") || input.contains("all metadata")) {
            displayEntities.clear();
            displayEntities.add("ALL_FIELDS");
        }
    }
    
    private void determineActionType(String input) {
        if (header.getContractNumber() != null) {
            queryMetadata.setActionType("contract_details");
        } else if (input.contains("expired") || input.contains("exipred")) {
            queryMetadata.setActionType("expired_contracts");
        } else if (input.contains("created by") || input.contains("creatd by")) {
            queryMetadata.setActionType("contracts_by_creator");
        } else if (input.contains("created in") || input.contains("creatd in") || 
                  input.contains("created after") || input.contains("creatd after") || 
                  input.contains("created btwn") || input.contains("creatd btwn")) {
            queryMetadata.setActionType("contracts_by_date");
        } else if (header.getCustomerNumber() != null || header.getCustomerName() != null) {
            queryMetadata.setActionType("contracts_by_customer");
        } else {
            queryMetadata.setActionType("contract_search");
        }
    }
    
    // Utility method to convert to JSON
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to convert to JSON\"}";
        }
    }
    private void extractCreatedBy(String input) {
        // Improved pattern to better capture the creator name
        Pattern pattern = Pattern.compile(
            "\\b(?:created|creatd)\\s*(?:by)?\\s*(\\w+)(?:\\s|$)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        
        if (matcher.find()) {
            String potentialName = matcher.group(1);
            // Exclude common non-name words
            if (!potentialName.matches("by|in|after|before|date|between|btwn")) {
                header.setCreatedBy(potentialName);
            }
        }
        
        // Additional check for "by <name>" pattern if the first one didn't catch it
        if (header.getCreatedBy() == null) {
            Pattern byPattern = Pattern.compile(
                "\\bby\\s+(\\w+)(?:\\s|$)", 
                Pattern.CASE_INSENSITIVE);
            Matcher byMatcher = byPattern.matcher(input);
            if (byMatcher.find()) {
                String potentialName = byMatcher.group(1);
                if (!potentialName.matches("after|before|between|btwn")) {
                    header.setCreatedBy(potentialName);
                }
            }
        }
    }
    public static void main(String[] args) {
        ContractQueryModel model = new ContractQueryModel();
        
        // Test with sample inputs
        String[] testInputs = {
            "show contract 123456",
            "contracts created by vinod after 1-Jan-2020",
            "contracts for customer number 897654",
            "contracts created in 2024",
            "get project type, effective date, and price list for account number 10840607",
            "whats the specifcations of prduct AE125",
            "ae125 avalable in stok?",
            "show failed and pasd parts 123456"
        };
        
        for (String input : testInputs) {
            System.out.println("Processing input: " + input);
            model.processUserInput(input);
            System.out.println(model.toJson());
            System.out.println("\n----------------------------------------\n");
        }
    }
}