package com.oracle.view.source;

import java.lang.reflect.Method;

public class DebugFindoutActionType {
    public static void main(String[] args) {
        System.out.println("Testing findoutTheActionType method directly");
        System.out.println("===========================================");
        
        try {
            NLPEntityProcessor processor = new NLPEntityProcessor();
            String userInput = "Create a contract";
            
            // Use reflection to call the private method
            Method method = NLPEntityProcessor.class.getDeclaredMethod("findoutTheActionType", String.class);
            method.setAccessible(true);
            
            String result = (String) method.invoke(processor, userInput);
            
            System.out.println("Input: " + userInput);
            System.out.println("Result: " + result);
            System.out.println();
            
            if ("HELP_CONTRACT_CREATE_BOT".equals(result)) {
                System.out.println("SUCCESS: findoutTheActionType returns HELP_CONTRACT_CREATE_BOT");
            } else {
                System.out.println("FAILED: findoutTheActionType returns " + result + " instead of HELP_CONTRACT_CREATE_BOT");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 