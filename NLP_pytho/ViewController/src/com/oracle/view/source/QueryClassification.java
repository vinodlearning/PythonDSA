package com.oracle.view.source;
public class QueryClassification {
    public final String queryType;
    public final String actionType;
    
    public QueryClassification(String queryType, String actionType) {
        this.queryType = queryType;
        this.actionType = actionType;
    }
    
    @Override
    public String toString() {
        return String.format("QueryClassification{queryType='%s', actionType='%s'}", 
                           queryType, actionType);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        QueryClassification that = (QueryClassification) obj;
        return queryType.equals(that.queryType) && actionType.equals(that.actionType);
    }
    
    @Override
    public int hashCode() {
        return queryType.hashCode() * 31 + actionType.hashCode();
    }
}