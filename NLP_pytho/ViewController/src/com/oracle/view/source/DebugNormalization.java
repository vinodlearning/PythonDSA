package com.oracle.view.source;

public class DebugNormalization {
    public static void main(String[] args) {
        System.out.println("Testing normalization process");
        System.out.println("============================");
        
        String userInput = "Create a contract";
        
        System.out.println("Original input: '" + userInput + "'");
        
        // Test normalizePrompt using reflection
        String normalized = null;
        try {
            java.lang.reflect.Method normalizeMethod = NLPEntityProcessor.class.getDeclaredMethod("normalizePrompt", String.class);
            normalizeMethod.setAccessible(true);
            normalized = (String) normalizeMethod.invoke(null, userInput);
            System.out.println("After normalizePrompt: '" + normalized + "'");
        } catch (Exception e) {
            System.out.println("Error calling normalizePrompt: " + e.getMessage());
            return;
        }
        
        // Test findoutTheActionType with normalized input
        try {
            NLPEntityProcessor processor = new NLPEntityProcessor();
            java.lang.reflect.Method method = NLPEntityProcessor.class.getDeclaredMethod("findoutTheActionType", String.class);
            method.setAccessible(true);
            
            String result = (String) method.invoke(processor, normalized);
            System.out.println("findoutTheActionType result: '" + result + "'");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
} 