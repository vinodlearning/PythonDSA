package com.oracle.view;

/**
 * Tracks input processing including spell corrections
 */
public class InputTracking {
    private String originalInput;
    private String correctedInput;
    private double correctionConfidence;
    
    // Private constructor for builder pattern
    private InputTracking() {}
    
    // Getters
    public String getOriginalInput() { return originalInput; }
    public String getCorrectedInput() { return correctedInput; }
    public double getCorrectionConfidence() { return correctionConfidence; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private InputTracking tracking = new InputTracking();
        
        public Builder originalInput(String originalInput) {
            tracking.originalInput = originalInput;
            return this;
        }
        
        public Builder correctedInput(String correctedInput) {
            tracking.correctedInput = correctedInput;
            return this;
        }
        
        public Builder correctionConfidence(double correctionConfidence) {
            tracking.correctionConfidence = correctionConfidence;
            return this;
        }
        
        public InputTracking build() {
            return tracking;
        }
    }
}