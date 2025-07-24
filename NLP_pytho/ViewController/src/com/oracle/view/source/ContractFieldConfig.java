package com.oracle.view.source;

import java.util.*;

public class ContractFieldConfig {
    // List of required contract creation fields
    public static final List<String> CONTRACT_CREATION_FIELDS = Arrays.asList(
        "ACCOUNT_NUMBER",
        "CONTRACT_NAME",
        "TITLE",
        "DESCRIPTION",
        "COMMENTS",
        "IS_PRICELIST",
        "HPP_REQUIRED" // Example for extensibility
    );

    // Map of field key to display name (contract creation)
    public static final Map<String, String> FIELD_DISPLAY_NAMES = new HashMap<>();
    static {
        FIELD_DISPLAY_NAMES.put("ACCOUNT_NUMBER", "Account Number (6+ digits)");
        FIELD_DISPLAY_NAMES.put("CONTRACT_NAME", "Contract Name");
        FIELD_DISPLAY_NAMES.put("TITLE", "Title");
        FIELD_DISPLAY_NAMES.put("DESCRIPTION", "Description");
        FIELD_DISPLAY_NAMES.put("COMMENTS", "Comments");
        FIELD_DISPLAY_NAMES.put("IS_PRICELIST", "Price List Required (Yes/No)");
        FIELD_DISPLAY_NAMES.put("HPP_REQUIRED", "Hpp status");
        FIELD_DISPLAY_NAMES.put("EXPIRATION_DATE", "Expiration Date");
        FIELD_DISPLAY_NAMES.put("EFFECTIVE_DATE", "Effective Date");
        FIELD_DISPLAY_NAMES.put("FLOW_DOWN_DATE", "Flow down Date");
        FIELD_DISPLAY_NAMES.put("PRICE_EXPIRATION_DATE", "Price Expiration Date");
        FIELD_DISPLAY_NAMES.put("SYSTEM_LOADED_DATE", "System Loaded Date");
        FIELD_DISPLAY_NAMES.put("QUATAR", "Quatar");
        FIELD_DISPLAY_NAMES.put("DATE_OF_SIGNATURE", "Signature Date");
        FIELD_DISPLAY_NAMES.put("ACCOUNT_NUMBER", "Customer Number");
        FIELD_DISPLAY_NAMES.put("CONTRACT_NAME", "Contract Name");
        FIELD_DISPLAY_NAMES.put("TITLE", "Title");
        FIELD_DISPLAY_NAMES.put("DESCRIPTION", "Description");
        FIELD_DISPLAY_NAMES.put("COMMENTS", "Comments");
        FIELD_DISPLAY_NAMES.put("IS_PRICELIST", "Is it a Price List Contract");
    }

    public static String getDisplayName(String key) {
        return FIELD_DISPLAY_NAMES.getOrDefault(key, key.replace("_", " "));
    }

    // List of required checklist fields
    public static final List<String> CHECKLIST_FIELDS = Arrays.asList(
        "DATE_OF_SIGNATURE",
        "EFFECTIVE_DATE",
        "EXPIRATION_DATE",
        "FLOW_DOWN_DATE",
        "PRICE_EXPIRATION_DATE"
        // Add more as needed
    );

    // Map of checklist field key to display name
    public static final Map<String, String> CHECKLIST_DISPLAY_NAMES = new HashMap<>();
    static {
        CHECKLIST_DISPLAY_NAMES.put("DATE_OF_SIGNATURE", "Date of Signature");
        CHECKLIST_DISPLAY_NAMES.put("EFFECTIVE_DATE", "Effective Date");
        CHECKLIST_DISPLAY_NAMES.put("EXPIRATION_DATE", "Expiration Date");
        CHECKLIST_DISPLAY_NAMES.put("FLOW_DOWN_DATE", "Flow Down Date");
        CHECKLIST_DISPLAY_NAMES.put("PRICE_EXPIRATION_DATE", "Price Expiration Date");
        // Add more as needed
    }

    public static String getChecklistDisplayName(String key) {
        return CHECKLIST_DISPLAY_NAMES.getOrDefault(key, key.replace("_", " "));
    }
} 