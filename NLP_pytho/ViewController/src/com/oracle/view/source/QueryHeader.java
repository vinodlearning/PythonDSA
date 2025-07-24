package com.oracle.view.source;
public class QueryHeader {
    private String contractNumber;
    private String partNumber;
    private String customerNumber;
    private String customerName;
    private String createdBy;
    
    // Add missing getter methods
    public String getContractNumber() {
        return contractNumber;
    }
    
    public String getPartNumber() {
        return partNumber;
    }
    
    public String getCustomerNumber() {
        return customerNumber;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    // Add missing setter methods
    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }
    
    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }
    
    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}