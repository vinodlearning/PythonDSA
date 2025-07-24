package com.oracle.view.source;
// Enhanced Robust Java Data Extraction Solution
import java.util.*;
import java.util.regex.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * RobustDataExtractor.java
 *
 * Dedicated utility for robust extraction of key-value pairs from complex, mixed-format user input.
 * - Handles comma, newline, colon, and equals-separated fields.
 * - Handles duplicates and inconsistent spacing.
 * - Does NOT normalize field names or perform business validation (e.g., account number validation).
 *   Use SpellCorrector or other logic after extraction to map to canonical column names and validate values.
 *
 * Usage:
 *   Map{@code <String, String>} extracted = RobustDataExtractor.extractData(userInput);
 *   // Normalize and validate fields after extraction:
 *   
 *   for (Map.Entry{@code <String, String>} entry : extracted.entrySet()) {
 *       String normKey = SpellCorrector.normalizeField(entry.getKey());
 *       normalized.put(normKey, entry.getValue());
 *   }
 *   // Now use normalized map for business logic, validation, or JSON output.
 *
 * See Technical_Design_Document.md for integration details.
 */
public class RobustDataExtractor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Enhanced method to extract data from complex text input
     * Handles mixed formats, duplicates, and inconsistent spacing
     * Supports: key:value, key=value, comma-separated, duplicates
     */
    public static Map<String, String> extractData(String input) {
        Map<String, String> data = new LinkedHashMap<>();
        Map<String, Integer> duplicateTracker = new HashMap<>();

        if (input == null || input.trim().isEmpty()) {
            return data;
        }

        String[] lines = input.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // Process comma-separated values with mixed separators first
            if (line.contains(",") && (line.contains(":") || line.contains("="))) {
                processCommaSeparatedLine(line, data, duplicateTracker);
            }
            // Handle single colon-separated pairs (avoid conflicts with equals)
            else if (line.contains(":") && !line.contains("=")) {
                processColonSeparatedLine(line, data, duplicateTracker);
            }
            // Handle single equals-separated pairs (avoid conflicts with colons)
            else if (line.contains("=") && !line.contains(":")) {
                processEqualsSeparatedLine(line, data, duplicateTracker);
            }
        }

        return data;
    }

    /**
     * Process comma-separated values with mixed separators
     */
    private static void processCommaSeparatedLine(String line, Map<String, String> data,
                                                  Map<String, Integer> duplicateTracker) {
        String[] pairs = line.split(",");

        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty())
                continue;

            String key = "";
            String value = "";

            if (pair.contains(":")) {
                int colonIndex = pair.indexOf(':');
                if (colonIndex > 0 && colonIndex < pair.length() - 1) {
                    key = pair.substring(0, colonIndex).trim();
                    value = pair.substring(colonIndex + 1).trim();
                }
            } else if (pair.contains("=")) {
                int equalsIndex = pair.indexOf('=');
                if (equalsIndex > 0 && equalsIndex < pair.length() - 1) {
                    key = pair.substring(0, equalsIndex).trim();
                    value = pair.substring(equalsIndex + 1).trim();
                }
            }

            if (!key.isEmpty() && !value.isEmpty()) {
                String finalKey = handleDuplicateKey(key, data, duplicateTracker);
                data.put(finalKey, value);
            }
        }
    }

    /**
     * Process colon-separated key-value pairs
     */
    private static void processColonSeparatedLine(String line, Map<String, String> data,
                                                  Map<String, Integer> duplicateTracker) {
        int colonIndex = line.indexOf(':');
        if (colonIndex > 0 && colonIndex < line.length() - 1) {
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            // Remove trailing comma if present
            if (value.endsWith(",")) {
                value = value.substring(0, value.length() - 1).trim();
            }

            if (!key.isEmpty() && !value.isEmpty()) {
                String finalKey = handleDuplicateKey(key, data, duplicateTracker);
                data.put(finalKey, value);
            }
        }
    }

    /**
     * Process equals-separated key-value pairs
     */
    private static void processEqualsSeparatedLine(String line, Map<String, String> data,
                                                   Map<String, Integer> duplicateTracker) {
        int equalsIndex = line.indexOf('=');
        if (equalsIndex > 0 && equalsIndex < line.length() - 1) {
            String key = line.substring(0, equalsIndex).trim();
            String value = line.substring(equalsIndex + 1).trim();

            if (!key.isEmpty() && !value.isEmpty()) {
                String finalKey = handleDuplicateKey(key, data, duplicateTracker);
                data.put(finalKey, value);
            }
        }
    }

    /**
     * Handle duplicate keys by adding numerical suffix
     */
    private static String handleDuplicateKey(String key, Map<String, String> data,
                                             Map<String, Integer> duplicateTracker) {
        if (!data.containsKey(key)) {
            return key;
        }

        // Key already exists, create numbered version
        int count = duplicateTracker.getOrDefault(key, 1) + 1;
        duplicateTracker.put(key, count);
        return key + "_" + count;
    }

    /**
     * Convert Map to JSON string using Jackson with enhanced formatting
     */
    public static String toJson(Map<String, String> data) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    /**
     * Convert Map to JSON string with manual formatting (no dependencies)
     */
    public static String toJsonManual(Map<String, String> data) {
        if (data.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        Iterator<Map.Entry<String, String>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            json.append("  \"")
                .append(escapeJsonString(entry.getKey()))
                .append("\": \"")
                .append(escapeJsonString(entry.getValue()))
                .append("\"");

            if (iterator.hasNext()) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Enhanced data validation with detailed reporting
     */
    public static ValidationResult validateData(Map<String, String> data) {
        ValidationResult result = new ValidationResult();

        if (data == null || data.isEmpty()) {
            result.valid = false;
            result.errors.add("No data extracted");
            return result;
        }

        int validFields = 0;
        int duplicates = 0;

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key == null || key.trim().isEmpty()) {
                result.errors.add("Empty key found");
                continue;
            }

            if (value == null || value.trim().isEmpty()) {
                result.errors.add("Empty value for key: " + key);
                continue;
            }

            if (key.matches(".*_\\d+$")) {
                duplicates++;
            }

            validFields++;
        }

        result.valid = result.errors.isEmpty();
        result.totalFields = data.size();
        result.validFields = validFields;
        result.duplicateFields = duplicates;

        return result;
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        public boolean valid = true;
        public int totalFields = 0;
        public int validFields = 0;
        public int duplicateFields = 0;
        public List<String> errors = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation Result:\n");
            sb.append("Valid: ")
              .append(valid)
              .append("\n");
            sb.append("Total Fields: ")
              .append(totalFields)
              .append("\n");
            sb.append("Valid Fields: ")
              .append(validFields)
              .append("\n");
            sb.append("Duplicate Fields: ")
              .append(duplicateFields)
              .append("\n");

            if (!errors.isEmpty()) {
                sb.append("Errors:\n");
                for (String error : errors) {
                    sb.append("- ")
                      .append(error)
                      .append("\n");
                }
            }

            return sb.toString();
        }
    }

    /**
     * Escape special characters for JSON
     */
    private static String escapeJsonString(String input) {
        if (input == null)
            return "";

        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    /**
     * Enhanced main method with complex data testing
     */
    public static void main(String[] args) {
        // Your complex contract data with mixed formats and duplicates
        String complexContractData ="contract name: Vinod Contract BY VINod,\n" + "account: 12345678,\n" + "Title : This is the title,\n" +
            "description: No description,\n" + "commenst : my comments\n" +
            "contract name= Vinod Contract BY VINod,\n" + "account= 12345678,\n" + "Title =This is the title,\n" +
            "description= No description,\n" + "commenst = my comments\n" + "Date of Signature=1/8/25\n" +
            "Effective Date=1/8/24\n" + "Expiration Date=1/8/25\n" + "Flow Down Date=1/8/25\n" +
            "Price Expiration Date=1/8/24\n" + "Expiration date :8/8/25\n" + "price expiration date :8/8/25\n" +
            "system loaded date :8/8/25\n" + "quatar :8/8/25\n" + "date of signature :8/8/25\n" + "is price list:no\n" +
            "is hpp: no";

        try {
            System.out.println("=== ROBUST DATA EXTRACTION TEST ===\n");

            // Extract data with enhanced algorithm
            Map<String, String> extractedData = extractData(complexContractData);

            // Normalize keys using SpellCorrector
            Map<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : extractedData.entrySet()) {
                String normKey = SpellCorrector.normalizeField(entry.getKey());
                normalized.put(normKey, entry.getValue());
            }

            // Detailed validation
            ValidationResult validation = validateData(normalized);
            System.out.println(validation.toString());

            if (validation.valid || validation.validFields > 0) {
                // Convert to JSON
                String jsonOutput = toJson(normalized);
                System.out.println("\n=== JSON OUTPUT ===");
                System.out.println(jsonOutput);

                // Show statistics
                System.out.println("\n=== EXTRACTION STATISTICS ===");
                System.out.println("Total fields extracted: " + normalized.size());
                System.out.println("Duplicate fields handled: " + validation.duplicateFields);
                System.out.println("Mixed format support: ?");

                // Show all extracted pairs
                System.out.println("\n=== EXTRACTED KEY-VALUE PAIRS ===");
                for (Map.Entry<String, String> entry : normalized.entrySet()) {
                    System.out.printf("%-25s -> %s%n", entry.getKey(), entry.getValue());
                }

            } else {
                System.out.println("\n? Data extraction failed validation!");
            }

        } catch (JsonProcessingException e) {
            System.err.println("JSON conversion error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
