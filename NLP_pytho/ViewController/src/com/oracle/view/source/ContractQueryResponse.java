package com.oracle.view.source;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ContractQueryResponse {
    
    @JsonProperty("originalInput")
    private final String originalInput;
    
    @JsonProperty("correctedInput")
    private final String correctedInput;
    
    @JsonProperty("header")
    private final QueryHeader header; // Changed from ContractHeader to QueryHeader
    
    @JsonProperty("queryMetadata")
    private final QueryMetadata queryMetadata;
    
    @JsonProperty("filters")
    private final List<QueryEntity> filters;
    
    @JsonProperty("displayEntities")
    private final List<String> displayEntities;
    
    @JsonProperty("errors")
    private final List<String> errors;
    
    public ContractQueryResponse(String originalInput, String correctedInput, QueryHeader header, 
                                   QueryMetadata queryMetadata, List<QueryEntity> filters, 
                                   List<String> displayEntities, List<String> errors) {
        this.originalInput = originalInput;
        this.correctedInput = correctedInput;
        this.header = header;
        this.queryMetadata = queryMetadata;
        this.filters = filters;
        this.displayEntities = displayEntities;
        this.errors = errors;
    }
    
    // Getters
    public String getOriginalInput() {
        return originalInput;
    }
    
    public String getCorrectedInput() {
        return correctedInput;
    }
    
    public QueryHeader getHeader() {
        return header;
    }
    
    public QueryMetadata getQueryMetadata() {
        return queryMetadata;
    }
    
    public List<QueryEntity> getFilters() {
        return filters;
    }
    
    public List<String> getDisplayEntities() {
        return displayEntities;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    @Override
    public String toString() {
        return String.format("ContractQueryResponse{originalInput='%s', correctedInput='%s', header=%s, queryMetadata=%s, filters=%s, displayEntities=%s, errors=%s}",
            originalInput, correctedInput, header, queryMetadata, filters, displayEntities, errors);
    }
}