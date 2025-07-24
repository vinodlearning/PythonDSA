package com.oracle.view.source;

/**
 * EntityFilter - Represents a filter condition for database queries
 * Extracted from StandardJSONProcessor for standalone use
 */
public class EntityFilter {
    public final String attribute;
    public final String operation;
    public final String value;
    public final String source;

    public EntityFilter(String attribute, String operation, String value, String source) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
        this.source = source;
    }

    @Override
    public String toString() {
        return String.format("EntityFilter{attribute='%s', operation='%s', value='%s', source='%s'}", 
                           attribute, operation, value, source);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EntityFilter that = (EntityFilter) obj;
        return attribute.equals(that.attribute) && 
               operation.equals(that.operation) && 
               value.equals(that.value) && 
               source.equals(that.source);
    }

    @Override
    public int hashCode() {
        int result = attribute.hashCode();
        result = 31 * result + operation.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + source.hashCode();
        return result;
    }
} 