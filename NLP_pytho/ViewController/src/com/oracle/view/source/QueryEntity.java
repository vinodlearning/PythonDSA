package com.oracle.view.source;
import com.fasterxml.jackson.annotation.JsonProperty;
public class QueryEntity {
    public final String attribute;  // Change from private to public
        public final String operation;  // Change from private to public  
        public final String value;      // Change from private to public
        public final String source;     // Change from private to public
    
        public QueryEntity(String attribute, String operation, String value, String source) {
               this.attribute = attribute;
               this.operation = operation;
               this.value = value;
               this.source = source;
           }
    
    // Add missing getter methods
    public String getAttribute() {
        return attribute;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getSource() {
        return source;
    }
    
}