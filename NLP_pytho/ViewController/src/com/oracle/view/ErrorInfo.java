package com.oracle.view;

/**
 * Error information class representing error objects
 * Used in the errors array of the JSON response
 */
public class ErrorInfo {
    private String code;        // Error type/code
    private String message;     // Human-readable message
    private String severity;    // BLOCKER or WARNING
    
    // Constructors
    public ErrorInfo() {}
    
    public ErrorInfo(String code, String message, String severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
    
    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    @Override
    public String toString() {
        return String.format("ErrorInfo{code='%s', message='%s', severity='%s'}", 
                           code, message, severity);
    }
}