package com.oracle.view.source;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContractHeader {
    
    @JsonProperty("contractNumber")
    public String contractNumber;
    
    @JsonProperty("partNumber")
    public String partNumber;
    
    @JsonProperty("customerNumber")
    public String customerNumber;
    
    @JsonProperty("customerName")
    public String customerName;
    
    @JsonProperty("createdBy")
    public String createdBy;
    
    public ContractHeader() {
        this.contractNumber = null;
        this.partNumber = null;
        this.customerNumber = null;
        this.customerName = null;
        this.createdBy = null;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
