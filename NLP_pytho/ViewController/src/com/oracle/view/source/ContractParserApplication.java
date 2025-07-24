package com.oracle.view.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Scanner;

public class ContractParserApplication {
    
    private final ContractQueryParser basicParser;
    private final EnhancedContractQueryParser enhancedParser;
    private final ObjectMapper objectMapper;
    
    public ContractParserApplication() {
        this.basicParser = new ContractQueryParser();
        this.enhancedParser = new EnhancedContractQueryParser();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    public static void main(String[] args) {
        String[] inputs={ "show contract 123456", "contract details 123456", "get contract info 123456",
            "contracts created by vinod after 1-Jan-2020", "status of contract 123456", "expired contracts",
            "contracts for customer number 897654", "account 10840607 contracts", "contracts created in 2024",
            "get all metadata for contract 123456", "contracts under account name 'Siemens'"};
        EnhancedContractQueryParser ob=new EnhancedContractQueryParser();
        
        for(String userInput:inputs){
       ContractQueryResponse res= ob.parseQuery(userInput);
            
//            String hearders="Contract Number: "+res.header.contractNumber+", Customer Number:"+res.header.customerNumber+" , Customer Name:"+res.header.customerName+", Created BY:"+res.header.createdBy+", Part NUmber:"+res.header.partNumber;
//            String qatype="Query Type:"+res.queryMetadata.queryType+", Action Type: "+res.queryMetadata.queryType;
//          
//        System.out.println(hearders);
//            System.out.println(qatype);
           new ContractParserApplication().displayResponse(res);
            
        }
       
    }
    
   
    
    private void processQuery(String query, boolean useEnhanced) {
        try {
            ContractQueryResponse response;
            
            if (useEnhanced) {
                System.out.println("Using Enhanced Parser with ML capabilities...");
                response = enhancedParser.parseQuery(query);
            } else {
                System.out.println("Using Basic Parser...");
                response = basicParser.parseQuery(query);
            }
            
            displayResponse(response);
            
        } catch (Exception e) {
            System.err.println("Error processing query: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void displayResponse(ContractQueryResponse response) {
        try {
            System.out.println("=== Query Response ===");
            String jsonResponse = objectMapper.writeValueAsString(response);
            System.out.println(jsonResponse);
            
            // Also display a human-readable summary
            System.out.println("\n=== Summary ===");
            System.out.println("Query Type: " + response.getQueryMetadata().queryType);
            System.out.println("Action: " + response.getQueryMetadata().actionType);
            System.out.println("Processing Time: " + response.getQueryMetadata().processingTimeMs + "ms");
            
            if (response.getHeader().getContractNumber() != null) {
                System.out.println("Contract Number: " + response.getHeader().getContractNumber() );
            }
            if (response.getHeader().getCustomerNumber() != null) {
                System.out.println("Customer Number: " + response.getHeader().getCustomerNumber());
            }
            if (response.getHeader().getCustomerName() != null) {
                System.out.println("Customer Name: " + response.getHeader().getCustomerName() );
            }
            if (response.getHeader().getPartNumber()  != null) {
                System.out.println("Part Number: " + response.getHeader().getPartNumber());
            }
            
            if (!response.getFilters().isEmpty()) {
                System.out.println("Entities Found: " + response.getFilters().size());
                for (QueryEntity entity : response.getFilters()) {
                    System.out.println("  - " + entity.attribute + " " + entity.operation + " " + entity.value);
                }
            }
            
            if (!response.getDisplayEntities().isEmpty()) {
                System.out.println("Fields to Display: " + String.join(", ", response.getDisplayEntities()));
            }
            
            if (!response.getErrors().isEmpty()) {
                System.out.println("Errors: " + String.join(", ", response.getErrors()));
            }
            
        } catch (Exception e) {
            System.err.println("Error displaying response: " + e.getMessage());
        }
    }
    
    private void showHelp() {
        System.out.println("\n=== Contract Query Parser Help ===");
        System.out.println("This tool parses natural language queries about contracts and parts.");
        System.out.println();
        System.out.println("Example queries:");
        System.out.println("  show contract 123456");
        System.out.println("  contracts created in 2024");
        System.out.println("  contracts for customer number 897654");
        System.out.println("  get all metadata for contract 123456");
        System.out.println("  show part AE125 details");
        System.out.println("  failed parts in contract 123456");
        System.out.println("  get project type, effective date, and price list for account number 10840607");
        System.out.println();
        System.out.println("Parser modes:");
        System.out.println("  basic: <query>     - Use basic rule-based parser");
        System.out.println("  enhanced: <query>  - Use enhanced parser with ML (default)");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  help  - Show this help message");
        System.out.println("  exit  - Exit the application");
        System.out.println();
    }
}