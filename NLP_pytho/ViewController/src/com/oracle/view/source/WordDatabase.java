package com.oracle.view.source;

import java.util.*;

/**
 * Comprehensive word database for NLP processing
 * Contains built-in dictionaries for verbs, nouns, and common variations
 * No internet connectivity required - all data is embedded
 */
public class WordDatabase {
    
    // Verb databases - all tenses and forms
    private static final Set<String> CREATION_VERBS = new HashSet<>(Arrays.asList(
        // Base forms
        "create", "make", "generate", "build", "draft", "initiate", "start", "produce", 
        "prepare", "compose", "write", "construct", "form", "develop", "assemble", 
        "manufacture", "fabricate", "establish", "setup", "do", "draw", "put", "get", 
        "give", "send", "provide", "help", "assist", "need", "want", "require", 
        "request", "order", "ask", "demand", "wish", "like", "could", "can", "will", 
        "shall", "may", "might", "should", "would", "please",
        
        // Present progressive (-ing forms)
        "creating", "making", "generating", "building", "drafting", "initiating", 
        "starting", "producing", "preparing", "composing", "writing", "constructing", 
        "forming", "developing", "assembling", "manufacturing", "fabricating", 
        "establishing", "setting up", "doing", "drawing", "putting", "getting", 
        "giving", "sending", "providing", "helping", "assisting", "needing", 
        "wanting", "requiring", "requesting", "ordering", "asking", "demanding", 
        "wishing", "liking", "coulding", "canning", "willing", "shalling", "maying", 
        "mighting", "shoulding", "woulding", "pleasing",
        
        // Past tense forms
        "made", "generated", "created", "built", "drafted", "initiated", "started", 
        "produced", "prepared", "composed", "wrote", "constructed", "formed", 
        "developed", "assembled", "manufactured", "fabricated", "established", 
        "set up", "did", "drew", "put", "got", "gave", "sent", "provided", "helped", 
        "assisted", "needed", "wanted", "required", "requested", "ordered", "asked", 
        "demanded", "wished", "liked", "could", "can", "will", "shall", "may", 
        "might", "should", "would", "pleased",
        
        // Past participle forms
        "generated", "created", "made", "built", "drafted", "initiated", "started", 
        "produced", "prepared", "composed", "written", "constructed", "formed", 
        "developed", "assembled", "manufactured", "fabricated", "established", 
        "set up", "done", "drawn", "put", "gotten", "given", "sent", "provided", 
        "helped", "assisted", "needed", "wanted", "required", "requested", "ordered", 
        "asked", "demanded", "wished", "liked",
        
        // Third person singular (-s forms)
        "creates", "makes", "generates", "builds", "drafts", "initiates", "starts", 
        "produces", "prepares", "composes", "writes", "constructs", "forms", 
        "develops", "assembles", "manufactures", "fabricates", "establishes", 
        "setups", "does", "draws", "puts", "gets", "gives", "sends", "provides", 
        "helps", "assists", "needs", "wants", "requires", "requests", "orders", 
        "asks", "demands", "wishes", "likes"
    ));
    
    // Noun forms of creation verbs
    private static final Set<String> CREATION_NOUNS = new HashSet<>(Arrays.asList(
        "creation", "generation", "making", "building", "drafting", "initiation", 
        "starting", "production", "preparation", "composition", "writing", 
        "construction", "formation", "development", "assembly", "manufacturing", 
        "fabrication", "establishment", "setup", "doing", "drawing", "putting", 
        "getting", "giving", "sending", "providing", "helping", "assisting", 
        "needing", "wanting", "requiring", "requesting", "ordering", "asking", 
        "demanding", "wishing", "liking", "steps", "guide", "process", "method", 
        "procedure", "approach", "technique", "strategy", "plan", "scheme", 
        "design", "layout", "structure", "framework", "system", "mechanism", 
        "workflow", "pipeline", "sequence", "series", "chain", "line", "path", 
        "route", "way", "means", "manner", "mode", "style", "format", "pattern", 
        "template", "model", "example", "sample", "instance", "case", "scenario", 
        "situation", "circumstance", "context", "environment", "setting", "background"
    ));
    
    // Command words to filter out from queries
    private static final Set<String> COMMAND_WORDS = new HashSet<>(Arrays.asList(
        "show", "get", "list", "find", "display", "fetch", "retrieve", "give", "provide",
        "what", "how", "why", "when", "where", "which", "who", "is", "are", "can", "will",
        "the", "of", "for", "in", "on", "at", "by", "with", "from", "to", "and", "or",
        "contract", "contracts", "part", "parts", "customer", "account", "info", "details",
        "status", "data", "all", "any", "some", "many", "much", "more", "most", "less",
        "created", "expired", "active", "inactive", "failed", "passed", "loaded", "missing",
        "under", "name", "number", "after", "before", "between", "during", "within"
    ));
    
    // Customer context words
    private static final Set<String> CUSTOMER_CONTEXT_WORDS = new HashSet<>(Arrays.asList(
        "customer", "customers", "client", "clients", "account", "accounts"
    ));
    
    // Creator context words
    private static final Set<String> CREATOR_CONTEXT_WORDS = new HashSet<>(Arrays.asList(
        "created", "by", "author", "maker", "developer", "owner"
    ));
    
    // Date context words
    private static final Set<String> DATE_CONTEXT_WORDS = new HashSet<>(Arrays.asList(
        "date", "time", "when", "day", "month", "year", "period", "duration", "timeline",
        "schedule", "dat", "tim", "whn", "dy", "mnth", "yr", "perid", "duratin", "timelin",
        "schedul"
    ));
    
    // Price context words
    private static final Set<String> PRICE_CONTEXT_WORDS = new HashSet<>(Arrays.asList(
        "price", "cost", "amount", "value", "rate", "fee", "charge", "expense", "pricing",
        "costing", "pric", "cst", "amnt", "valu", "rat", "fe", "charg", "expens", "pricng",
        "costng"
    ));
    
    // Status context words
    private static final Set<String> STATUS_CONTEXT_WORDS = new HashSet<>(Arrays.asList(
        "status", "state", "condition", "situation", "position", "standing", "stage",
        "phase", "level", "statu", "stat", "conditn", "situatn", "positn", "standng",
        "stag", "phas", "levl"
    ));
    
    // Stop words to avoid as filter values
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "for", "and", "of", "is", "the", "a", "an", "to", "in", "on", "by", "with", "at",
        "from", "what", "who", "which"
    ));
    
    // Common misspellings and variations
    private static final Map<String, String> SPELL_CORRECTIONS = new HashMap<>();
    static {
        // Contract variations
        SPELL_CORRECTIONS.put("ctrct", "contract");
        SPELL_CORRECTIONS.put("contarct", "contract");
        SPELL_CORRECTIONS.put("contrat", "contract");
        SPELL_CORRECTIONS.put("conract", "contract");
        SPELL_CORRECTIONS.put("cntrct", "contract");
        SPELL_CORRECTIONS.put("kontract", "contract");
        SPELL_CORRECTIONS.put("contrato", "contract");
        SPELL_CORRECTIONS.put("contracts", "contract");
        SPELL_CORRECTIONS.put("contracting", "contract");
        SPELL_CORRECTIONS.put("contracted", "contract");
        
        // Create variations
        SPELL_CORRECTIONS.put("creat", "create");
        SPELL_CORRECTIONS.put("creates", "create");
        SPELL_CORRECTIONS.put("creating", "create");
        // REMOVED: SPELL_CORRECTIONS.put("created", "create"); - This was incorrectly converting past tense to present tense
        
        // Make variations
        SPELL_CORRECTIONS.put("mak", "make");
        SPELL_CORRECTIONS.put("maek", "make");
        SPELL_CORRECTIONS.put("makes", "make");
        SPELL_CORRECTIONS.put("making", "make");
        SPELL_CORRECTIONS.put("made", "make");
        
        // Generate variations
        SPELL_CORRECTIONS.put("genrate", "generate");
        SPELL_CORRECTIONS.put("genert", "generate");
        SPELL_CORRECTIONS.put("generates", "generate");
        SPELL_CORRECTIONS.put("generating", "generate");
        SPELL_CORRECTIONS.put("generated", "generate");
        
        // FIXED: Add missing spell corrections identified in analysis
        SPELL_CORRECTIONS.put("tim", "time");
        SPELL_CORRECTIONS.put("informaton", "information");
        SPELL_CORRECTIONS.put("staus", "status");
        SPELL_CORRECTIONS.put("detials", "details");
        SPELL_CORRECTIONS.put("pric", "price");
        SPELL_CORRECTIONS.put("prise", "price");
        SPELL_CORRECTIONS.put("leed", "lead");
        SPELL_CORRECTIONS.put("invoce", "invoice");
        SPELL_CORRECTIONS.put("invoic", "invoice");
        SPELL_CORRECTIONS.put("efective", "effective");
        SPELL_CORRECTIONS.put("expir", "expire");
        SPELL_CORRECTIONS.put("expiry", "expiration");
        SPELL_CORRECTIONS.put("experation", "expiration");
        
        // NEW: Additional spell corrections for 13 cosmetic improvement cases
        SPELL_CORRECTIONS.put("custommer", "customer");
        SPELL_CORRECTIONS.put("paymet", "payment");
        SPELL_CORRECTIONS.put("lenght", "length");
        SPELL_CORRECTIONS.put("typ", "type");
        SPELL_CORRECTIONS.put("experation", "expiration");
        SPELL_CORRECTIONS.put("expire", "expire"); // Preserve "expire" when grammatically correct
        
        // CRITICAL FIX: Failed parts spell corrections
        SPELL_CORRECTIONS.put("faild", "failed");
        SPELL_CORRECTIONS.put("faild parts", "failed parts");
        SPELL_CORRECTIONS.put("faild part", "failed part");
        
        // CRITICAL FIX: Preserve "no data" - don't change to "number data"
        // This is handled in contextual logic, not simple replacement
        
        // Common abbreviations
        SPELL_CORRECTIONS.put("pls", "please");
        SPELL_CORRECTIONS.put("plz", "please");
        SPELL_CORRECTIONS.put("thx", "thanks");
        SPELL_CORRECTIONS.put("ty", "thank you");
        SPELL_CORRECTIONS.put("tnx", "thanks");
        SPELL_CORRECTIONS.put("u", "you");
        SPELL_CORRECTIONS.put("ur", "your");
        SPELL_CORRECTIONS.put("yr", "your");
        
        // Number substitutions
        SPELL_CORRECTIONS.put("4", "for");
        SPELL_CORRECTIONS.put("2", "to");
        SPELL_CORRECTIONS.put("too", "to");
        SPELL_CORRECTIONS.put("two", "to");
        
        // Common words
        SPELL_CORRECTIONS.put("asap", "as soon as possible");
        SPELL_CORRECTIONS.put("immediately", "now");
        SPELL_CORRECTIONS.put("quick", "fast");
        SPELL_CORRECTIONS.put("urgent", "important");
        SPELL_CORRECTIONS.put("could you", "please");
        SPELL_CORRECTIONS.put("can you", "please");
        SPELL_CORRECTIONS.put("will you", "please");
        SPELL_CORRECTIONS.put("would you", "please");
        SPELL_CORRECTIONS.put("for me", "please");
    }
    
    // Word boundary patterns for concatenated words
    private static final Map<String, String> WORD_BOUNDARY_CORRECTIONS = new HashMap<>();
    static {
        // Contract + action patterns
        WORD_BOUNDARY_CORRECTIONS.put("contractcreation", "contract creation");
        WORD_BOUNDARY_CORRECTIONS.put("contractcreate", "contract create");
        WORD_BOUNDARY_CORRECTIONS.put("contractmake", "contract make");
        WORD_BOUNDARY_CORRECTIONS.put("contractgenerate", "contract generate");
        WORD_BOUNDARY_CORRECTIONS.put("contractdraft", "contract draft");
        WORD_BOUNDARY_CORRECTIONS.put("contractinitiate", "contract initiate");
        WORD_BOUNDARY_CORRECTIONS.put("contractstart", "contract start");
        WORD_BOUNDARY_CORRECTIONS.put("contractproduce", "contract produce");
        WORD_BOUNDARY_CORRECTIONS.put("contractbuild", "contract build");
        WORD_BOUNDARY_CORRECTIONS.put("contractprepare", "contract prepare");
        WORD_BOUNDARY_CORRECTIONS.put("contractcompose", "contract compose");
        WORD_BOUNDARY_CORRECTIONS.put("contractwrite", "contract write");
        WORD_BOUNDARY_CORRECTIONS.put("contractconstruct", "contract construct");
        WORD_BOUNDARY_CORRECTIONS.put("contractform", "contract form");
        WORD_BOUNDARY_CORRECTIONS.put("contractdevelop", "contract develop");
        WORD_BOUNDARY_CORRECTIONS.put("contractassemble", "contract assemble");
        WORD_BOUNDARY_CORRECTIONS.put("contractmanufacture", "contract manufacture");
        WORD_BOUNDARY_CORRECTIONS.put("contractfabricate", "contract fabricate");
        WORD_BOUNDARY_CORRECTIONS.put("contractestablish", "contract establish");
        WORD_BOUNDARY_CORRECTIONS.put("contractsetup", "contract setup");
        WORD_BOUNDARY_CORRECTIONS.put("contractdo", "contract do");
        WORD_BOUNDARY_CORRECTIONS.put("contractdraw", "contract draw");
        WORD_BOUNDARY_CORRECTIONS.put("contractput", "contract put");
        WORD_BOUNDARY_CORRECTIONS.put("contractget", "contract get");
        WORD_BOUNDARY_CORRECTIONS.put("contractgive", "contract give");
        WORD_BOUNDARY_CORRECTIONS.put("contractsend", "contract send");
        WORD_BOUNDARY_CORRECTIONS.put("contractprovide", "contract provide");
        WORD_BOUNDARY_CORRECTIONS.put("contracthelp", "contract help");
        WORD_BOUNDARY_CORRECTIONS.put("contractassist", "contract assist");
        WORD_BOUNDARY_CORRECTIONS.put("contractneed", "contract need");
        WORD_BOUNDARY_CORRECTIONS.put("contractwant", "contract want");
        WORD_BOUNDARY_CORRECTIONS.put("contractrequire", "contract require");
        WORD_BOUNDARY_CORRECTIONS.put("contractrequest", "contract request");
        WORD_BOUNDARY_CORRECTIONS.put("contractorder", "contract order");
        WORD_BOUNDARY_CORRECTIONS.put("contractask", "contract ask");
        WORD_BOUNDARY_CORRECTIONS.put("contractdemand", "contract demand");
        WORD_BOUNDARY_CORRECTIONS.put("contractwish", "contract wish");
        WORD_BOUNDARY_CORRECTIONS.put("contractlike", "contract like");
        
        // Action + contract patterns
        WORD_BOUNDARY_CORRECTIONS.put("createcontract", "create contract");
        WORD_BOUNDARY_CORRECTIONS.put("makecontract", "make contract");
        WORD_BOUNDARY_CORRECTIONS.put("generatecontract", "generate contract");
        WORD_BOUNDARY_CORRECTIONS.put("draftcontract", "draft contract");
        WORD_BOUNDARY_CORRECTIONS.put("initiatecontract", "initiate contract");
        WORD_BOUNDARY_CORRECTIONS.put("startcontract", "start contract");
        WORD_BOUNDARY_CORRECTIONS.put("producecontract", "produce contract");
        WORD_BOUNDARY_CORRECTIONS.put("buildcontract", "build contract");
        WORD_BOUNDARY_CORRECTIONS.put("preparecontract", "prepare contract");
        WORD_BOUNDARY_CORRECTIONS.put("composecontract", "compose contract");
        WORD_BOUNDARY_CORRECTIONS.put("writecontract", "write contract");
        WORD_BOUNDARY_CORRECTIONS.put("constructcontract", "construct contract");
        WORD_BOUNDARY_CORRECTIONS.put("formcontract", "form contract");
        WORD_BOUNDARY_CORRECTIONS.put("developcontract", "develop contract");
        WORD_BOUNDARY_CORRECTIONS.put("assemblecontract", "assemble contract");
        WORD_BOUNDARY_CORRECTIONS.put("manufacturecontract", "manufacture contract");
        WORD_BOUNDARY_CORRECTIONS.put("fabricatecontract", "fabricate contract");
        WORD_BOUNDARY_CORRECTIONS.put("establishcontract", "establish contract");
        WORD_BOUNDARY_CORRECTIONS.put("setupcontract", "setup contract");
        WORD_BOUNDARY_CORRECTIONS.put("docontract", "do contract");
        WORD_BOUNDARY_CORRECTIONS.put("drawcontract", "draw contract");
        WORD_BOUNDARY_CORRECTIONS.put("putcontract", "put contract");
        WORD_BOUNDARY_CORRECTIONS.put("getcontract", "get contract");
        WORD_BOUNDARY_CORRECTIONS.put("givecontract", "give contract");
        WORD_BOUNDARY_CORRECTIONS.put("sendcontract", "send contract");
        WORD_BOUNDARY_CORRECTIONS.put("providecontract", "provide contract");
        WORD_BOUNDARY_CORRECTIONS.put("helpcontract", "help contract");
        WORD_BOUNDARY_CORRECTIONS.put("assistcontract", "assist contract");
        WORD_BOUNDARY_CORRECTIONS.put("needcontract", "need contract");
        WORD_BOUNDARY_CORRECTIONS.put("wantcontract", "want contract");
        WORD_BOUNDARY_CORRECTIONS.put("requirecontract", "require contract");
        WORD_BOUNDARY_CORRECTIONS.put("requestcontract", "request contract");
        WORD_BOUNDARY_CORRECTIONS.put("ordercontract", "order contract");
        WORD_BOUNDARY_CORRECTIONS.put("askcontract", "ask contract");
        WORD_BOUNDARY_CORRECTIONS.put("demandcontract", "demand contract");
        WORD_BOUNDARY_CORRECTIONS.put("wishcontract", "wish contract");
        WORD_BOUNDARY_CORRECTIONS.put("likecontract", "like contract");
    }
    
    // Imperative indicators
    private static final Set<String> IMPERATIVE_INDICATORS = new HashSet<>(Arrays.asList(
        "now", "asap", "immediately", "quick", "fast", "urgent", "please", 
        "could you", "can you", "will you", "would you", "for me", "me", 
        "my", "mine", "myself", "i need", "i want", "i require", "i request",
        "i would like", "i would love", "i would appreciate", "i would be grateful",
        "i would be thankful", "i would be obliged", "i would be indebted",
        "i would be in your debt", "i would be grateful if", "i would appreciate if",
        "i would be thankful if", "i would be obliged if", "i would be indebted if"
    ));
    
    // Question words that indicate help requests
    private static final Set<String> QUESTION_WORDS = new HashSet<>(Arrays.asList(
        "how", "what", "when", "where", "why", "which", "who", "whom", "whose",
        "how to", "what is", "what are", "when is", "where is", "why is",
        "which is", "who is", "whom is", "whose is", "how do", "what do",
        "when do", "where do", "why do", "which do", "who do", "whom do", "whose do"
    ));
    
    /**
     * Check if a word is a creation verb (any tense)
     */
    public static boolean isCreationVerb(String word) {
        if (word == null) return false;
        return CREATION_VERBS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a creation noun
     */
    public static boolean isCreationNoun(String word) {
        if (word == null) return false;
        return CREATION_NOUNS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a command word (should be filtered out)
     */
    public static boolean isCommandWord(String word) {
        if (word == null) return false;
        return COMMAND_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a customer context word
     */
    public static boolean isCustomerContextWord(String word) {
        if (word == null) return false;
        return CUSTOMER_CONTEXT_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a creator context word
     */
    public static boolean isCreatorContextWord(String word) {
        if (word == null) return false;
        return CREATOR_CONTEXT_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a date context word
     */
    public static boolean isDateContextWord(String word) {
        if (word == null) return false;
        return DATE_CONTEXT_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a price context word
     */
    public static boolean isPriceContextWord(String word) {
        if (word == null) return false;
        return PRICE_CONTEXT_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a status context word
     */
    public static boolean isStatusContextWord(String word) {
        if (word == null) return false;
        return STATUS_CONTEXT_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a stop word
     */
    public static boolean isStopWord(String word) {
        if (word == null) return false;
        return STOP_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Get spell correction for a word
     */
    public static String getSpellCorrection(String word) {
        if (word == null) return null;
        return SPELL_CORRECTIONS.get(word.toLowerCase());
    }
    
    /**
     * Get all spell corrections map
     */
    public static Map<String, String> getSpellCorrections() {
        return new HashMap<>(SPELL_CORRECTIONS);
    }
    
    /**
     * Get word boundary correction for concatenated words
     */
    public static String getWordBoundaryCorrection(String word) {
        if (word == null) return null;
        return WORD_BOUNDARY_CORRECTIONS.get(word.toLowerCase());
    }
    
    /**
     * Check if a word is an imperative indicator
     */
    public static boolean isImperativeIndicator(String word) {
        if (word == null) return false;
        return IMPERATIVE_INDICATORS.contains(word.toLowerCase());
    }
    
    /**
     * Check if a word is a question word
     */
    public static boolean isQuestionWord(String word) {
        if (word == null) return false;
        return QUESTION_WORDS.contains(word.toLowerCase());
    }
    
    /**
     * Normalize a word by applying spell corrections and boundary corrections
     */
    public static String normalizeWord(String word) {
        if (word == null) return null;
        
        String normalized = word.toLowerCase();
        
        // Apply word boundary correction first
        String boundaryCorrection = getWordBoundaryCorrection(normalized);
        if (boundaryCorrection != null) {
            normalized = boundaryCorrection;
        }
        
        // Apply spell correction
        String spellCorrection = getSpellCorrection(normalized);
        if (spellCorrection != null) {
            normalized = spellCorrection;
        }
        
        return normalized;
    }
    
    /**
     * Check if text contains creation words (verbs or nouns)
     */
    public static boolean containsCreationWords(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");
        
        for (String word : words) {
            if (isCreationVerb(word) || isCreationNoun(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if text contains imperative indicators
     */
    public static boolean containsImperativeIndicators(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");
        
        for (String word : words) {
            if (isImperativeIndicator(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if text contains question words
     */
    public static boolean containsQuestionWords(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");
        
        for (String word : words) {
            if (isQuestionWord(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get all command words
     */
    public static Set<String> getCommandWords() {
        return new HashSet<>(COMMAND_WORDS);
    }
    
    /**
     * Get all customer context words
     */
    public static Set<String> getCustomerContextWords() {
        return new HashSet<>(CUSTOMER_CONTEXT_WORDS);
    }
    
    /**
     * Get all creator context words
     */
    public static Set<String> getCreatorContextWords() {
        return new HashSet<>(CREATOR_CONTEXT_WORDS);
    }
    
    /**
     * Get all date context words
     */
    public static Set<String> getDateContextWords() {
        return new HashSet<>(DATE_CONTEXT_WORDS);
    }
    
    /**
     * Get all price context words
     */
    public static Set<String> getPriceContextWords() {
        return new HashSet<>(PRICE_CONTEXT_WORDS);
    }
    
    /**
     * Get all status context words
     */
    public static Set<String> getStatusContextWords() {
        return new HashSet<>(STATUS_CONTEXT_WORDS);
    }
    
    /**
     * Get all stop words
     */
    public static Set<String> getStopWords() {
        return new HashSet<>(STOP_WORDS);
    }
} 