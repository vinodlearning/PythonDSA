package com.oracle.view.source;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Calculates confidence scores for query parsing results
 */
public class ConfidenceCalculator {
    
    private static final Pattern CONTRACT_NUMBER_PATTERN = Pattern.compile("\\b\\d{6}\\b");
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("\\b[A-Z]{2}\\d{3}\\b");
    private static final Pattern CUSTOMER_NUMBER_PATTERN = Pattern.compile("\\b\\d{6,8}\\b");
    
    public double calculateConfidence(String input, ContractQueryResponse response) {
        if (input == null || input.trim().isEmpty()) {
            return 0.0;
        }
        
        double confidence = 0.0;
        int factors = 0;
        
        // Factor 1: Entity extraction confidence
        double entityConfidence = calculateEntityConfidence(input, response.getFilters());
        confidence += entityConfidence;
        factors++;
        
        // Factor 2: Query type consistency
        double typeConsistency = calculateTypeConsistency(input, response.getQueryMetadata().getQueryType());
        confidence += typeConsistency;
        factors++;
        
        // Factor 3: Action type appropriateness
        double actionAppropriate = calculateActionAppropriate(input, response.getQueryMetadata().getActionType());
        confidence += actionAppropriate;
        factors++;
        
        // Factor 4: Display fields relevance
        double fieldsRelevance = calculateFieldsRelevance(input, response.getDisplayEntities());
        confidence += fieldsRelevance;
        factors++;
        
        // Factor 5: Error presence (negative factor)
        double errorPenalty = response.getErrors().isEmpty() ? 1.0 : 0.5;
        confidence += errorPenalty;
        factors++;
        
        return factors > 0 ? confidence / factors : 0.0;
    }
    
    private double calculateEntityConfidence(String input, List<QueryEntity> entities) {
        if (entities.isEmpty()) {
            return 0.2; // Low confidence if no entities found
        }
        
        double confidence = 0.0;
        String lowerInput = input.toLowerCase();
        
        for (QueryEntity entity : entities) {
            switch (entity.getAttribute()) {
                case "AWARD_NUMBER":
                case "CONTRACT_NUMBER":
                    if (CONTRACT_NUMBER_PATTERN.matcher(input).find()) {
                        confidence += 0.9; // High confidence for exact pattern match
                    } else if (lowerInput.contains("contract")) {
                        confidence += 0.7; // Medium confidence for keyword match
                    }
                    break;
                    
                case "PART_NUMBER":
                    if (PART_NUMBER_PATTERN.matcher(input).find()) {
                        confidence += 0.9;
                    } else if (lowerInput.contains("part")) {
                        confidence += 0.7;
                    }
                    break;
 case "CUSTOMER_NUMBER":
                    if (CUSTOMER_NUMBER_PATTERN.matcher(input).find()) {
                        confidence += 0.9;
                    } else if (lowerInput.contains("customer") || lowerInput.contains("account")) {
                        confidence += 0.7;
                    }
                    break;
                    
                case "CUSTOMER_NAME":
                    if (input.contains("\"") || input.contains("'")) {
                        confidence += 0.8; // High confidence for quoted names
                    } else if (lowerInput.contains("customer") || lowerInput.contains("account")) {
                        confidence += 0.6;
                    }
                    break;
                    
                case "CREATE_DATE":
                    if (Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(input).find()) {
                        confidence += 0.8;
                    } else if (lowerInput.contains("date") || lowerInput.contains("created")) {
                        confidence += 0.6;
                    }
                    break;
                    
                case "STATUS":
                    if (lowerInput.contains("active") || lowerInput.contains("expired") || 
                        lowerInput.contains("failed") || lowerInput.contains("status")) {
                        confidence += 0.7;
                    }
                    break;
                    
                default:
                    confidence += 0.5; // Default confidence for other entities
                    break;
            }
        }
        
        return Math.min(1.0, confidence / entities.size());
    }
    
    private double calculateTypeConsistency(String input, String queryType) {
        String lowerInput = input.toLowerCase();
        
        switch (queryType) {
            case "CONTRACTS":
                if (lowerInput.contains("contract") || lowerInput.contains("award")) {
                    return 0.9;
                } else if (lowerInput.contains("customer") || lowerInput.contains("account")) {
                    return 0.7; // Customer queries often relate to contracts
                } else {
                    return 0.4;
                }
                
            case "PARTS":
                if (lowerInput.contains("part") || lowerInput.contains("component")) {
                    return 0.9;
                } else {
                    return 0.3;
                }
                
            case "FAILED_PARTS":
                if (lowerInput.contains("failed") && lowerInput.contains("part")) {
                    return 0.95;
                } else if (lowerInput.contains("failed") || lowerInput.contains("error")) {
                    return 0.7;
                } else {
                    return 0.2;
                }
                
            case "GENERAL":
                return 0.5; // Neutral confidence for general queries
                
            default:
                return 0.3;
        }
    }
    
    private double calculateActionAppropriate(String input, String actionType) {
        String lowerInput = input.toLowerCase();
        
        // Check for metadata requests
        if (actionType.contains("metadata")) {
            if (lowerInput.contains("metadata") || lowerInput.contains("all information") || 
                lowerInput.contains("get all")) {
                return 0.9;
            } else {
                return 0.4;
            }
        }
        
        // Check for summary requests
        if (actionType.contains("summary")) {
            if (lowerInput.contains("summary") || lowerInput.contains("overview")) {
                return 0.9;
            } else {
                return 0.5;
            }
        }
        
        // Check for detailed requests
        if (actionType.contains("detailed")) {
            if (lowerInput.contains("detail") || lowerInput.contains("detailed") || 
                lowerInput.contains("full information")) {
                return 0.9;
            } else {
                return 0.5;
            }
        }
        
        // Check for specific entity-based actions
        if (actionType.contains("contract_by_contractNumber")) {
            if (CONTRACT_NUMBER_PATTERN.matcher(input).find()) {
                return 0.95;
            } else {
                return 0.3;
            }
        }
        
        if (actionType.contains("contracts_by_customer")) {
            if (lowerInput.contains("customer") || lowerInput.contains("account")) {
                return 0.8;
            } else {
                return 0.4;
            }
        }
        
        if (actionType.contains("failed_parts")) {
            if (lowerInput.contains("failed") && lowerInput.contains("part")) {
                return 0.95;
            } else {
                return 0.3;
            }
        }
        
        // Check for action verbs
        if (actionType.contains("show") || actionType.contains("display")) {
            if (lowerInput.contains("show") || lowerInput.contains("display") || 
                lowerInput.contains("list")) {
                return 0.8;
            }
        }
        
        if (actionType.contains("get") || actionType.contains("retrieve")) {
            if (lowerInput.contains("get") || lowerInput.contains("retrieve") || 
                lowerInput.contains("fetch")) {
                return 0.8;
            }
        }
        
        return 0.6; // Default confidence
    }
    
    private double calculateFieldsRelevance(String input, List<String> displayFields) {
        if (displayFields.isEmpty()) {
            return 0.2;
        }
        
        String lowerInput = input.toLowerCase();
        double relevanceScore = 0.0;
        int relevantFields = 0;
        
        for (String field : displayFields) {
            boolean isRelevant = false;
            
            switch (field) {
                case "CONTRACT_NUMBER":
                    if (lowerInput.contains("contract") || lowerInput.contains("award") || 
                        CONTRACT_NUMBER_PATTERN.matcher(input).find()) {
                        isRelevant = true;
                    }
                    break;
                    
                case "CUSTOMER_NAME":
                case "CUSTOMER_NUMBER":
                    if (lowerInput.contains("customer") || lowerInput.contains("account")) {
                        isRelevant = true;
                    }
                    break;
                    
                case "PART_NUMBER":
                    if (lowerInput.contains("part") || lowerInput.contains("component")) {
                        isRelevant = true;
                    }
                    break;
                    
                case "CREATE_DATE":
                case "EFFECTIVE_DATE":
                case "EXPIRY_DATE":
                    if (lowerInput.contains("date") || lowerInput.contains("created") || 
                        lowerInput.contains("effective") || lowerInput.contains("expiry") ||
                        Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(input).find()) {
                        isRelevant = true;
                    }
                    break;
                    
                case "STATUS":
                    if (lowerInput.contains("status") || lowerInput.contains("active") || 
                        lowerInput.contains("expired") || lowerInput.contains("failed")) {
                        isRelevant = true;
                    }
                    break;
                    
                case "PROJECT_TYPE":
                    if (lowerInput.contains("project") || lowerInput.contains("type")) {
                        isRelevant = true;
                    }
                    break;
                    
                case "CREATED_BY":
                    if (lowerInput.contains("created by") || lowerInput.contains("author") || 
                        lowerInput.contains("who")) {
                        isRelevant = true;
                    }
                    break;
                    
                case "PRICE_LIST":
                    if (lowerInput.contains("price") || lowerInput.contains("cost") || 
                        lowerInput.contains("financial")) {
                        isRelevant = true;
                    }
                    break;
                    
                default:
                    // For unknown fields, assume moderate relevance
                    isRelevant = true;
                    break;
            }
            
            if (isRelevant) {
                relevantFields++;
                relevanceScore += 1.0;
            }
        }
        
        // Calculate relevance ratio
        double relevanceRatio = (double) relevantFields / displayFields.size();
        
        // Penalize if too many irrelevant fields are included
        if (relevanceRatio < 0.3) {
            return 0.2;
        } else if (relevanceRatio < 0.5) {
            return 0.5;
        } else if (relevanceRatio < 0.7) {
            return 0.7;
        } else {
            return 0.9;
        }
    }
    
    /**
     * Calculate confidence for a specific aspect of the response
     */
    public double calculateAspectConfidence(String input, ContractQueryResponse response, String aspect) {
        switch (aspect.toLowerCase()) {
            case "entities":
                return calculateEntityConfidence(input, response.getFilters());
            case "query_type":
                return calculateTypeConsistency(input, response.getQueryMetadata().getQueryType());
            case "action_type":
                return calculateActionAppropriate(input, response.getQueryMetadata().getActionType());
            case "display_fields":
                return calculateFieldsRelevance(input, response.getDisplayEntities());
            default:
                return calculateConfidence(input, response);
        }
    }
    
    /**
     * Get detailed confidence breakdown
     */
    public ConfidenceBreakdown getConfidenceBreakdown(String input, ContractQueryResponse response) {
        double entityConfidence = calculateEntityConfidence(input, response.getFilters());
        double typeConsistency = calculateTypeConsistency(input, response.getQueryMetadata().getQueryType());
        double actionAppropriate = calculateActionAppropriate(input, response.getQueryMetadata().getActionType());
        double fieldsRelevance = calculateFieldsRelevance(input, response.getDisplayEntities());
        double errorPenalty = response.getErrors().isEmpty() ? 1.0 : 0.5;
        
        double overall = (entityConfidence + typeConsistency + actionAppropriate + fieldsRelevance + errorPenalty) / 5.0;
        
        return new ConfidenceBreakdown(overall, entityConfidence, typeConsistency, 
                                     actionAppropriate, fieldsRelevance, errorPenalty);
    }
    
    /**
     * Confidence breakdown details
     */
    public static class ConfidenceBreakdown {
        public final double overall;
        public final double entityConfidence;
        public final double typeConsistency;
        public final double actionAppropriate;
        public final double fieldsRelevance;
        public final double errorPenalty;
        
        public ConfidenceBreakdown(double overall, double entityConfidence, double typeConsistency,
                                 double actionAppropriate, double fieldsRelevance, double errorPenalty) {
            this.overall = overall;
            this.entityConfidence = entityConfidence;
            this.typeConsistency = typeConsistency;
            this.actionAppropriate = actionAppropriate;
            this.fieldsRelevance = fieldsRelevance;
            this.errorPenalty = errorPenalty;
        }
        
        @Override
        public String toString() {
            return String.format("ConfidenceBreakdown{overall=%.2f, entities=%.2f, type=%.2f, action=%.2f, fields=%.2f, errors=%.2f}",
                overall, entityConfidence, typeConsistency, actionAppropriate, fieldsRelevance, errorPenalty);
        }
        
        public String getDetailedReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Confidence Analysis ===\n");
            report.append(String.format("Overall Confidence: %.1f%%\n", overall * 100));
            report.append("\nBreakdown:\n");
            report.append(String.format("  Entity Extraction: %.1f%%\n", entityConfidence * 100));
            report.append(String.format("  Query Type Match: %.1f%%\n", typeConsistency * 100));
            report.append(String.format("  Action Appropriateness: %.1f%%\n", actionAppropriate * 100));
            report.append(String.format("  Field Relevance: %.1f%%\n", fieldsRelevance * 100));
            report.append(String.format("  Error Penalty: %.1f%%\n", errorPenalty * 100));
            
            // Add recommendations
            report.append("\nRecommendations:\n");
            if (entityConfidence < 0.6) {
                report.append("  - Consider providing more specific entity information\n");
            }
            if (typeConsistency < 0.6) {
                report.append("  - Query type may not match your intent - try rephrasing\n");
            }
            if (actionAppropriate < 0.6) {
                report.append("  - Action type may be incorrect - be more specific about what you want\n");
            }
            if (fieldsRelevance < 0.6) {
                report.append("  - Display fields may not match your needs - specify required fields\n");
            }
            if (errorPenalty < 1.0) {
                report.append("  - There are validation errors - check your input format\n");
            }
            
            return report.toString();
        }
    }
}
                    
   