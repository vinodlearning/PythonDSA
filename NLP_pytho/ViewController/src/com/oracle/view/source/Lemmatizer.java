package com.oracle.view.source;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Lightweight Offline Lemmatizer for Business Domain
 * 
 * Provides lemmatization (word normalization) without internet dependency.
 * Integrates with existing WordDatabase and SpellCorrector for comprehensive text processing.
 * 
 * Features:
 * - Offline operation (no internet required)
 * - Business domain specific rules
 * - Handles common English inflections
 * - Integrates with existing NLP pipeline
 * - Maintains backward compatibility
 */
public class Lemmatizer {
    
    // Singleton instance
    private static volatile Lemmatizer instance;
    
    // Business domain specific lemmatization rules
    private final Map<String, String> businessLemmatizationRules = new HashMap<>();
    
    // Common English lemmatization patterns
    private final Map<String, String> commonLemmatizationRules = new HashMap<>();
    
    // Irregular verb forms
    private final Map<String, String> irregularVerbs = new HashMap<>();
    
    // Plural to singular mappings for business terms
    private final Map<String, String> businessPlurals = new HashMap<>();
    
    // Suffix patterns for lemmatization
    private final List<LemmatizationRule> suffixRules = new ArrayList<>();
    
    private Lemmatizer() {
        initializeBusinessLemmatizationRules();
        initializeCommonLemmatizationRules();
        initializeIrregularVerbs();
        initializeBusinessPlurals();
        initializeSuffixRules();
    }
    
    public static Lemmatizer getInstance() {
        if (instance == null) {
            synchronized (Lemmatizer.class) {
                if (instance == null) {
                    instance = new Lemmatizer();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize business domain specific lemmatization rules
     */
    private void initializeBusinessLemmatizationRules() {
        // Business process terms
        businessLemmatizationRules.put("processing", "process");
        businessLemmatizationRules.put("processed", "process");
        businessLemmatizationRules.put("processes", "process");
        
        // Error and failure terms
        businessLemmatizationRules.put("errors", "error");
        businessLemmatizationRules.put("failures", "failure");
        businessLemmatizationRules.put("failed", "fail");
        businessLemmatizationRules.put("failing", "fail");
        businessLemmatizationRules.put("fails", "fail");
        
        // Validation terms
        businessLemmatizationRules.put("validations", "validation");
        businessLemmatizationRules.put("validated", "validate");
        businessLemmatizationRules.put("validating", "validate");
        businessLemmatizationRules.put("validates", "validate");
        
        // Business rule terms
        businessLemmatizationRules.put("violations", "violation");
        businessLemmatizationRules.put("violated", "violate");
        businessLemmatizationRules.put("violating", "violate");
        businessLemmatizationRules.put("violates", "violate");
        
        // Loading terms
        businessLemmatizationRules.put("loading", "load");
        businessLemmatizationRules.put("loaded", "load");
        businessLemmatizationRules.put("loads", "load");
        
        // Part terms
        businessLemmatizationRules.put("parts", "part");
        businessLemmatizationRules.put("parting", "part");
        businessLemmatizationRules.put("parted", "part");
        
        // Contract terms
        businessLemmatizationRules.put("contracts", "contract");
        businessLemmatizationRules.put("contracting", "contract");
        businessLemmatizationRules.put("contracted", "contract");
        
        // Customer terms
        businessLemmatizationRules.put("customers", "customer");
        businessLemmatizationRules.put("customizing", "customize");
        businessLemmatizationRules.put("customized", "customize");
        
        // Price terms
        businessLemmatizationRules.put("pricing", "price");
        businessLemmatizationRules.put("priced", "price");
        businessLemmatizationRules.put("prices", "price");
        
        // Status terms
        businessLemmatizationRules.put("statuses", "status");
        businessLemmatizationRules.put("statusing", "status");
        businessLemmatizationRules.put("statused", "status");
        
        // Information terms
        businessLemmatizationRules.put("informations", "information");
        businessLemmatizationRules.put("informing", "inform");
        businessLemmatizationRules.put("informed", "inform");
        businessLemmatizationRules.put("informs", "inform");
        
        // Detail terms
        businessLemmatizationRules.put("detailing", "detail");
        businessLemmatizationRules.put("detailed", "detail");
        businessLemmatizationRules.put("details", "detail");
        
        // Issue terms
        businessLemmatizationRules.put("issues", "issue");
        businessLemmatizationRules.put("issuing", "issue");
        businessLemmatizationRules.put("issued", "issue");
        
        // Problem terms
        businessLemmatizationRules.put("problems", "problem");
        businessLemmatizationRules.put("problematic", "problem");
        
        // Reason terms
        businessLemmatizationRules.put("reasoning", "reason");
        businessLemmatizationRules.put("reasoned", "reason");
        businessLemmatizationRules.put("reasons", "reason");
        
        // Cause terms
        businessLemmatizationRules.put("causing", "cause");
        businessLemmatizationRules.put("caused", "cause");
        businessLemmatizationRules.put("causes", "cause");
        
        // Line terms
        businessLemmatizationRules.put("lines", "line");
        businessLemmatizationRules.put("lining", "line");
        businessLemmatizationRules.put("lined", "line");
        
        // Column terms
        businessLemmatizationRules.put("columns", "column");
        businessLemmatizationRules.put("columnar", "column");
        
        // Order terms
        businessLemmatizationRules.put("ordering", "order");
        businessLemmatizationRules.put("ordered", "order");
        businessLemmatizationRules.put("orders", "order");
        
        // Quantity terms
        businessLemmatizationRules.put("quantities", "quantity");
        businessLemmatizationRules.put("quantifying", "quantify");
        businessLemmatizationRules.put("quantified", "quantify");
        
        // Lead time terms
        businessLemmatizationRules.put("leading", "lead");
        businessLemmatizationRules.put("led", "lead");
        businessLemmatizationRules.put("leads", "lead");
        
        // Minimum/Maximum terms
        businessLemmatizationRules.put("minimizing", "minimize");
        businessLemmatizationRules.put("minimized", "minimize");
        businessLemmatizationRules.put("minimizes", "minimize");
        businessLemmatizationRules.put("maximizing", "maximize");
        businessLemmatizationRules.put("maximized", "maximize");
        businessLemmatizationRules.put("maximizes", "maximize");
    }
    
    /**
     * Initialize common English lemmatization rules
     */
    private void initializeCommonLemmatizationRules() {
        // Common verb forms
        commonLemmatizationRules.put("showing", "show");
        commonLemmatizationRules.put("showed", "show");
        commonLemmatizationRules.put("shown", "show");
        commonLemmatizationRules.put("shows", "show");
        
        commonLemmatizationRules.put("listing", "list");
        commonLemmatizationRules.put("listed", "list");
        commonLemmatizationRules.put("lists", "list");
        
        commonLemmatizationRules.put("getting", "get");
        commonLemmatizationRules.put("got", "get");
        commonLemmatizationRules.put("gotten", "get");
        commonLemmatizationRules.put("gets", "get");
        
        commonLemmatizationRules.put("finding", "find");
        commonLemmatizationRules.put("found", "find");
        commonLemmatizationRules.put("finds", "find");
        
        commonLemmatizationRules.put("searching", "search");
        commonLemmatizationRules.put("searched", "search");
        commonLemmatizationRules.put("searches", "search");
        
        commonLemmatizationRules.put("displaying", "display");
        commonLemmatizationRules.put("displayed", "display");
        commonLemmatizationRules.put("displays", "display");
        
        commonLemmatizationRules.put("retrieving", "retrieve");
        commonLemmatizationRules.put("retrieved", "retrieve");
        commonLemmatizationRules.put("retrieves", "retrieve");
        
        commonLemmatizationRules.put("fetching", "fetch");
        commonLemmatizationRules.put("fetched", "fetch");
        commonLemmatizationRules.put("fetches", "fetch");
        
        commonLemmatizationRules.put("checking", "check");
        commonLemmatizationRules.put("checked", "check");
        commonLemmatizationRules.put("checks", "check");
        
        commonLemmatizationRules.put("verifying", "verify");
        commonLemmatizationRules.put("verified", "verify");
        commonLemmatizationRules.put("verifies", "verify");
        
        commonLemmatizationRules.put("updating", "update");
        commonLemmatizationRules.put("updated", "update");
        commonLemmatizationRules.put("updates", "update");
        
        commonLemmatizationRules.put("creating", "create");
        commonLemmatizationRules.put("created", "create");
        commonLemmatizationRules.put("creates", "create");
        
        commonLemmatizationRules.put("deleting", "delete");
        commonLemmatizationRules.put("deleted", "delete");
        commonLemmatizationRules.put("deletes", "delete");
        
        commonLemmatizationRules.put("modifying", "modify");
        commonLemmatizationRules.put("modified", "modify");
        commonLemmatizationRules.put("modifies", "modify");
        
        commonLemmatizationRules.put("editing", "edit");
        commonLemmatizationRules.put("edited", "edit");
        commonLemmatizationRules.put("edits", "edit");
        
        commonLemmatizationRules.put("adding", "add");
        commonLemmatizationRules.put("added", "add");
        commonLemmatizationRules.put("adds", "add");
        
        commonLemmatizationRules.put("removing", "remove");
        commonLemmatizationRules.put("removed", "remove");
        commonLemmatizationRules.put("removes", "remove");
        
        commonLemmatizationRules.put("filtering", "filter");
        commonLemmatizationRules.put("filtered", "filter");
        commonLemmatizationRules.put("filters", "filter");
        
        commonLemmatizationRules.put("sorting", "sort");
        commonLemmatizationRules.put("sorted", "sort");
        commonLemmatizationRules.put("sorts", "sort");
        
        commonLemmatizationRules.put("grouping", "group");
        commonLemmatizationRules.put("grouped", "group");
        commonLemmatizationRules.put("groups", "group");
        
        commonLemmatizationRules.put("counting", "count");
        commonLemmatizationRules.put("counted", "count");
        commonLemmatizationRules.put("counts", "count");
        
        commonLemmatizationRules.put("calculating", "calculate");
        commonLemmatizationRules.put("calculated", "calculate");
        commonLemmatizationRules.put("calculates", "calculate");
        
        commonLemmatizationRules.put("computing", "compute");
        commonLemmatizationRules.put("computed", "compute");
        commonLemmatizationRules.put("computes", "compute");
    }
    
    /**
     * Initialize irregular verb forms
     */
    private void initializeIrregularVerbs() {
        // Common irregular verbs
        irregularVerbs.put("went", "go");
        irregularVerbs.put("gone", "go");
        irregularVerbs.put("going", "go");
        irregularVerbs.put("goes", "go");
        
        irregularVerbs.put("came", "come");
        irregularVerbs.put("coming", "come");
        irregularVerbs.put("comes", "come");
        
        irregularVerbs.put("saw", "see");
        irregularVerbs.put("seen", "see");
        irregularVerbs.put("seeing", "see");
        irregularVerbs.put("sees", "see");
        
        irregularVerbs.put("did", "do");
        irregularVerbs.put("done", "do");
        irregularVerbs.put("doing", "do");
        irregularVerbs.put("does", "do");
        
        irregularVerbs.put("had", "have");
        irregularVerbs.put("having", "have");
        irregularVerbs.put("has", "have");
        
        irregularVerbs.put("was", "be");
        irregularVerbs.put("were", "be");
        irregularVerbs.put("been", "be");
        irregularVerbs.put("being", "be");
        irregularVerbs.put("am", "be");
        irregularVerbs.put("is", "be");
        irregularVerbs.put("are", "be");
    }
    
    /**
     * Initialize business plural to singular mappings
     */
    private void initializeBusinessPlurals() {
        // Business specific plurals
        businessPlurals.put("specifications", "specification");
        businessPlurals.put("descriptions", "description");
        businessPlurals.put("summaries", "summary");
        businessPlurals.put("categories", "category");
        businessPlurals.put("classifications", "classification");
        businessPlurals.put("suppliers", "supplier");
        businessPlurals.put("invoices", "invoice");
        businessPlurals.put("deliveries", "delivery");
        businessPlurals.put("shipments", "shipment");
        businessPlurals.put("inventories", "inventory");
        businessPlurals.put("warranties", "warranty");
        businessPlurals.put("requirements", "requirement");
        businessPlurals.put("performances", "performance");
        businessPlurals.put("maintenances", "maintenance");
        businessPlurals.put("managements", "management");
        businessPlurals.put("businesses", "business");
    }
    
    /**
     * Initialize suffix-based lemmatization rules
     */
    private void initializeSuffixRules() {
        // Verb suffixes
        suffixRules.add(new LemmatizationRule("ing$", "", "VB")); // running -> run
        suffixRules.add(new LemmatizationRule("ed$", "", "VB"));   // walked -> walk
        suffixRules.add(new LemmatizationRule("s$", "", "VB"));    // walks -> walk
        
        // Noun suffixes
        suffixRules.add(new LemmatizationRule("ies$", "y", "NN")); // categories -> category
        suffixRules.add(new LemmatizationRule("s$", "", "NN"));    // parts -> part
        
        // Adjective suffixes
        suffixRules.add(new LemmatizationRule("er$", "", "JJ"));   // faster -> fast
        suffixRules.add(new LemmatizationRule("est$", "", "JJ"));  // fastest -> fast
        suffixRules.add(new LemmatizationRule("al$", "", "JJ"));   // technical -> technical
        suffixRules.add(new LemmatizationRule("ic$", "", "JJ"));   // specific -> specific
        suffixRules.add(new LemmatizationRule("ous$", "", "JJ"));  // various -> various
        suffixRules.add(new LemmatizationRule("ive$", "", "JJ"));  // active -> active
        suffixRules.add(new LemmatizationRule("able$", "", "JJ")); // available -> available
        suffixRules.add(new LemmatizationRule("ible$", "", "JJ")); // possible -> possible
    }
    
    /**
     * Lemmatize a single word
     */
    public String lemmatize(String word) {
        if (word == null || word.trim().isEmpty()) {
            return word;
        }
        
        String lowerWord = word.toLowerCase().trim();
        
        // Check business-specific rules first (highest priority)
        if (businessLemmatizationRules.containsKey(lowerWord)) {
            return businessLemmatizationRules.get(lowerWord);
        }
        
        // Check irregular verbs
        if (irregularVerbs.containsKey(lowerWord)) {
            return irregularVerbs.get(lowerWord);
        }
        
        // Check business plurals
        if (businessPlurals.containsKey(lowerWord)) {
            return businessPlurals.get(lowerWord);
        }
        
        // Check common lemmatization rules
        if (commonLemmatizationRules.containsKey(lowerWord)) {
            return commonLemmatizationRules.get(lowerWord);
        }
        
        // Apply suffix rules
        String lemmatized = applySuffixRules(lowerWord);
        if (!lemmatized.equals(lowerWord)) {
            return lemmatized;
        }
        
        // If no rules apply, return the original word
        return word;
    }
    
    /**
     * Apply suffix-based lemmatization rules
     */
    private String applySuffixRules(String word) {
        for (LemmatizationRule rule : suffixRules) {
            if (rule.matches(word)) {
                return rule.apply(word);
            }
        }
        return word;
    }
    
    /**
     * Lemmatize a text (multiple words)
     */
    public String lemmatizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            result.append(lemmatize(words[i]));
        }
        
        return result.toString();
    }
    
    /**
     * Lemmatize text while preserving tense information for critical words
     * This method is specifically designed to handle "created" vs "create" distinction
     */
    public String lemmatizeTextPreserveTense(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            String lowerWord = word.toLowerCase();
            
            // CRITICAL: Preserve "created" (past tense) to distinguish from "create" (present tense)
            if (lowerWord.equals("created")) {
                result.append(word); // Keep "created" as is
            } else {
                result.append(lemmatize(word));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Check if a word is in past tense
     */
    public boolean isPastTense(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        String lowerWord = word.toLowerCase();
        
        // Common past tense patterns
        if (lowerWord.endsWith("ed")) {
            return true;
        }
        
        // Irregular past tense verbs
        String[] irregularPastTense = {
            "went", "came", "saw", "did", "had", "was", "were", "been", "got", "found", "made", "took", "gave", "wrote", "drove", "flew", "ate", "drank", "slept", "ran", "swam", "bought", "sold", "told", "said", "thought", "brought", "caught", "taught", "fought", "sought", "bought", "sold", "told", "said", "thought", "brought", "caught", "taught", "fought", "sought"
        };
        
        for (String pastTense : irregularPastTense) {
            if (lowerWord.equals(pastTense)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Lemmatize words while preserving case
     */
    public String lemmatizePreserveCase(String word) {
        if (word == null || word.trim().isEmpty()) {
            return word;
        }
        
        boolean isCapitalized = Character.isUpperCase(word.charAt(0));
        String lemmatized = lemmatize(word);
        
        if (isCapitalized && lemmatized.length() > 0) {
            return Character.toUpperCase(lemmatized.charAt(0)) + lemmatized.substring(1);
        }
        
        return lemmatized;
    }
    
    /**
     * Add custom business lemmatization rule
     */
    public void addBusinessRule(String word, String lemma) {
        businessLemmatizationRules.put(word.toLowerCase(), lemma.toLowerCase());
    }
    
    /**
     * Remove custom business lemmatization rule
     */
    public void removeBusinessRule(String word) {
        businessLemmatizationRules.remove(word.toLowerCase());
    }
    
    /**
     * Get all business lemmatization rules
     */
    public Map<String, String> getBusinessRules() {
        return new HashMap<>(businessLemmatizationRules);
    }
    
    /**
     * Check if a word has been lemmatized
     */
    public boolean isLemmatized(String original, String lemmatized) {
        return !original.equalsIgnoreCase(lemmatized);
    }
    
    /**
     * Get lemmatization statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("businessRules", businessLemmatizationRules.size());
        stats.put("commonRules", commonLemmatizationRules.size());
        stats.put("irregularVerbs", irregularVerbs.size());
        stats.put("businessPlurals", businessPlurals.size());
        stats.put("suffixRules", suffixRules.size());
        return stats;
    }
    
    /**
     * Lemmatization rule for suffix-based transformations
     */
    private static class LemmatizationRule {
        private final Pattern pattern;
        private final String replacement;
        private final String posTag;
        
        public LemmatizationRule(String regex, String replacement, String posTag) {
            this.pattern = Pattern.compile(regex);
            this.replacement = replacement;
            this.posTag = posTag;
        }
        
        public boolean matches(String word) {
            return pattern.matcher(word).find();
        }
        
        public String apply(String word) {
            return pattern.matcher(word).replaceAll(replacement);
        }
        
        public String getPosTag() {
            return posTag;
        }
    }
} 