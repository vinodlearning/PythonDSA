package com.oracle.view;

/**
 * Response header containing extracted entities and input tracking
 */
public class ResponseHeader {
    private String contractNumber;
    private String partNumber;
    private String customerNumber;
    private String customerName;
    private String createdBy;
    private InputTracking inputTracking;
    
    // Private constructor for builder pattern
    private ResponseHeader() {}
    
    // Getters
    public String getContractNumber() { return contractNumber; }
    public String getPartNumber() { return partNumber; }
    public String getCustomerNumber() { return customerNumber; }
    public String getCustomerName() { return customerName; }
    public String getCreatedBy() { return createdBy; }
    public InputTracking getInputTracking() { return inputTracking; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ResponseHeader header = new ResponseHeader();
        
        public Builder contractNumber(String contractNumber) {
            header.contractNumber = contractNumber;
            return this;
        }
        
        public Builder partNumber(String partNumber) {
            header.partNumber = partNumber;
            return this;
        }
        
        public Builder customerNumber(String customerNumber) {
            header.customerNumber = customerNumber;
            return this;
        }
        
        public Builder customerName(String customerName) {
            header.customerName = customerName;
            return this;
        }
        
        public Builder createdBy(String createdBy) {
            header.createdBy = createdBy;
            return this;
        }
        
        public Builder inputTracking(InputTracking inputTracking) {
            header.inputTracking = inputTracking;
            return this;
        }
        
        public ResponseHeader build() {
            return header;
        }
    }
}