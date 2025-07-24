package com.oracle.view.source;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the context and understanding of a query
 */
public class QueryContext {
    public boolean hasCustomerFocus;
    public boolean hasDateFocus;
    public boolean hasPartFocus;
    public boolean hasStatusFocus;
    public boolean hasMetadataRequest;
    public boolean hasDetailRequest;
    public boolean hasSummaryRequest;
    public List<String> explicitFields;
    public String queryComplexity;
    
    public QueryContext() {
        this.explicitFields = new ArrayList<>();
        this.queryComplexity = "LOW";
    }
    
    @Override
    public String toString() {
        return String.format("QueryContext{customerFocus=%s, dateFocus=%s, partFocus=%s, " +
                           "statusFocus=%s, metadataRequest=%s, detailRequest=%s, " +
                           "summaryRequest=%s, explicitFields=%s, complexity=%s}",
                           hasCustomerFocus, hasDateFocus, hasPartFocus, hasStatusFocus,
                           hasMetadataRequest, hasDetailRequest, hasSummaryRequest,
                           explicitFields, queryComplexity);
    }
}