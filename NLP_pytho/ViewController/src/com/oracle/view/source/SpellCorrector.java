package com.oracle.view.source;

import java.util.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class SpellCorrector {
    
    private final Map<String, String> corrections;
    
    public SpellCorrector() {
        this.corrections = initializeCorrections();
    }
    
    /**
     * Enhanced spell correction with confidence scoring
     */
    public String correct(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String corrected = input;
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String correctedWord = correctWord(word);
            result.append(correctedWord);
            
            if (i < words.length - 1) {
                result.append(" ");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Correct individual word
     */
    private String correctWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return word;
        }
        
        // Common contract-related corrections
        String corrected = word.toLowerCase();
        
        // Contract corrections
        if (corrected.equals("contrct") || corrected.equals("contarct") || corrected.equals("contart")) {
            return "contract";
        }
        if (corrected.equals("detials") || corrected.equals("detalis")) {
            return "details";
        }
        if (corrected.equals("experation") || corrected.equals("expiration")) {
            return "expiration";
        }
        if (corrected.equals("efective") || corrected.equals("efectiv")) {
            return "effective";
        }
        if (corrected.equals("custmer") || corrected.equals("custommer")) {
            return "customer";
        }
        if (corrected.equals("paymet") || corrected.equals("payement")) {
            return "payment";
        }
        if (corrected.equals("incotems") || corrected.equals("incoterm")) {
            return "incoterms";
        }
        if (corrected.equals("staus") || corrected.equals("status")) {
            return "status";
        }
        if (corrected.equals("lenght") || corrected.equals("length")) {
            return "length";
        }
        if (corrected.equals("creat") || corrected.equals("create")) {
            return "create";
        }
        if (corrected.equals("mak") || corrected.equals("make")) {
            return "make";
        }
        if (corrected.equals("generat") || corrected.equals("generate")) {
            return "generate";
        }
        if (corrected.equals("build") || corrected.equals("built")) {
            return "build";
        }
        if (corrected.equals("set") || corrected.equals("setup")) {
            return "set up";
        }
        
        // Parts corrections
        if (corrected.equals("leed") || corrected.equals("lead")) {
            return "lead";
        }
        if (corrected.equals("tim") || corrected.equals("time")) {
            return "time";
        }
        if (corrected.equals("pric") || corrected.equals("prise")) {
            return "price";
        }
        if (corrected.equals("cost") || corrected.equals("costs")) {
            return "cost";
        }
        if (corrected.equals("moq") || corrected.equals("minimum")) {
            return "moq";
        }
        if (corrected.equals("uom") || corrected.equals("unit")) {
            return "uom";
        }
        if (corrected.equals("classificaton") || corrected.equals("classification")) {
            return "classification";
        }
        
        // Failed parts corrections
        if (corrected.equals("faild") || corrected.equals("failed")) {
            return "failed";
        }
        if (corrected.equals("error") || corrected.equals("errors")) {
            return "error";
        }
        if (corrected.equals("reasn") || corrected.equals("reason")) {
            return "reason";
        }
        if (corrected.equals("caus") || corrected.equals("cause")) {
            return "cause";
        }
        if (corrected.equals("problem") || corrected.equals("problems")) {
            return "problem";
        }
        if (corrected.equals("issu") || corrected.equals("issue")) {
            return "issue";
        }
        
        // Help corrections
        if (corrected.equals("how") || corrected.equals("howto")) {
            return "how";
        }
        if (corrected.equals("step") || corrected.equals("steps")) {
            return "steps";
        }
        if (corrected.equals("proces") || corrected.equals("process")) {
            return "process";
        }
        if (corrected.equals("guid") || corrected.equals("guide")) {
            return "guide";
        }
        if (corrected.equals("instructon") || corrected.equals("instruction")) {
            return "instruction";
        }
        if (corrected.equals("assist") || corrected.equals("assistance")) {
            return "assist";
        }
        if (corrected.equals("explain") || corrected.equals("explanation")) {
            return "explain";
        }
        if (corrected.equals("tell") || corrected.equals("told")) {
            return "tell";
        }
        if (corrected.equals("show") || corrected.equals("showed")) {
            return "show";
        }
        if (corrected.equals("need") || corrected.equals("needed")) {
            return "need";
        }
        if (corrected.equals("want") || corrected.equals("wanted")) {
            return "want";
        }
        if (corrected.equals("would") || corrected.equals("could")) {
            return "would";
        }
        if (corrected.equals("can") || corrected.equals("could")) {
            return "can";
        }
        if (corrected.equals("please") || corrected.equals("pls") || corrected.equals("plz")) {
            return "please";
        }
        if (corrected.equals("help") || corrected.equals("helped")) {
            return "help";
        }
        
        // Date corrections
        if (corrected.equals("date") || corrected.equals("dates")) {
            return "date";
        }
        if (corrected.equals("year") || corrected.equals("years")) {
            return "year";
        }
        if (corrected.equals("month") || corrected.equals("months")) {
            return "month";
        }
        if (corrected.equals("day") || corrected.equals("days")) {
            return "day";
        }
        if (corrected.equals("week") || corrected.equals("weeks")) {
            return "week";
        }
        
        // Return original word if no correction found
        return word;
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Get correction confidence between original and corrected input
     */
    public double getCorrectionConfidence(String original, String corrected) {
        if (original == null || corrected == null) {
            return 0.0;
        }
        
        if (original.equals(corrected)) {
            return 1.0; // Perfect match
        }
        
        // Calculate similarity based on character differences
        int maxLength = Math.max(original.length(), corrected.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(original.toLowerCase(), corrected.toLowerCase());
        double similarity = 1.0 - ((double) distance / maxLength);
        
        return Math.max(0.0, Math.min(1.0, similarity));
    }
    
    private Map<String, String> initializeCorrections() {
        Map<String, String> corrections = new HashMap<>();
        
        // Contract misspellings
        corrections.put("contrct", "contract");
        corrections.put("contarct", "contract");
        corrections.put("contarcts", "contracts");
        corrections.put("contracs", "contracts");
        corrections.put("contrcts", "contracts");
        corrections.put("kontrakt", "contract");
        corrections.put("kontract", "contract");
        corrections.put("ctrct", "contract");
        corrections.put("contrat", "contract");
        corrections.put("conract", "contract");
        corrections.put("cntrct", "contract");
        corrections.put("contrato", "contract");
        
        // Create misspellings (CRITICAL for contract creation)
        corrections.put("creat", "create");
        corrections.put("creates", "create");
        corrections.put("creating", "create");
        // REMOVED: corrections.put("created", "create"); - This was incorrectly converting past tense to present tense
        corrections.put("creatd", "created");
        
        // Make misspellings
        corrections.put("mak", "make");
        corrections.put("maek", "make");
        corrections.put("makes", "make");
        corrections.put("making", "make");
        corrections.put("made", "make");
        corrections.put("makeing", "making");
        
        // Generate misspellings
        corrections.put("genrate", "generate");
        corrections.put("genert", "generate");
        corrections.put("generates", "generate");
        corrections.put("generating", "generate");
        corrections.put("generated", "generate");
        
        // Show misspellings
        corrections.put("shwo", "show");
        corrections.put("shw", "show");
        
        // Info/Information misspellings
        corrections.put("infro", "info");
        corrections.put("detials", "details");
        corrections.put("detalis", "details");
        corrections.put("summry", "summary");
        corrections.put("informaton", "information");
        
        // Customer misspellings
        corrections.put("custmor", "customer");
        corrections.put("cstomer", "customer");
        corrections.put("custmer", "customer");
        corrections.put("custommer", "customer");
        
        // Number misspellings
        corrections.put("numer", "number");
        corrections.put("numbr", "number");
        corrections.put("no", "number");
        
        // Status misspellings
        corrections.put("statuz", "status");
        corrections.put("statuss", "status");
        corrections.put("staus", "status");
        
        // Date/Time misspellings
        corrections.put("aftr", "after");
        corrections.put("btwn", "between");
        corrections.put("mnth", "month");
        corrections.put("lst", "last");
        corrections.put("efective", "effective");
        corrections.put("tim", "time");
        
        // Parts misspellings
        corrections.put("prts", "parts");
        corrections.put("parst", "parts");
        corrections.put("partz", "parts");
        corrections.put("prduct", "product");
        
        // Account misspellings
        corrections.put("accunt", "account");
        corrections.put("acount", "account");
        corrections.put("acc", "account");
        
        // Action misspellings
        corrections.put("provid", "provide");
        corrections.put("avalable", "available");
        corrections.put("actve", "active");
        corrections.put("discontnud", "discontinued");
        corrections.put("discntinued", "discontinued");
        corrections.put("exipred", "expired");
        corrections.put("activ", "active");
        
        // Price misspellings
        corrections.put("pric", "price");
        corrections.put("prise", "price");
        corrections.put("pricng", "pricing");
        
        // Lead misspellings
        corrections.put("leed", "lead");
        corrections.put("lede", "lead");
        
        // Invoice misspellings
        corrections.put("invoce", "invoice");
        corrections.put("invoic", "invoice");
        
        // Expiration misspellings
        corrections.put("expir", "expire");
        corrections.put("expiry", "expiration");
        corrections.put("experation", "expiration");
        
        // Payment misspellings
        corrections.put("paymet", "payment");
        
        // Length misspellings
        corrections.put("lenght", "length");
        
        // Type misspellings
        corrections.put("typ", "type");
        
        // Failed misspellings
        corrections.put("faild", "failed");
        corrections.put("filde", "failed");
        corrections.put("faield", "failed");
        
        // Common abbreviations and chat-style corrections
        corrections.put("pls", "please");
        corrections.put("plz", "please");
        corrections.put("thx", "thanks");
        corrections.put("ty", "thank you");
        corrections.put("tnx", "thanks");
        corrections.put("u", "you");
        corrections.put("ur", "your");
        corrections.put("yr", "your");
        corrections.put("hw", "how");
        corrections.put("wat", "what");
        corrections.put("mee", "me");
        corrections.put("yu", "you");
        corrections.put("chek", "check");
        corrections.put("warrenty", "warranty");
        corrections.put("priod", "period");
        corrections.put("isses", "issues");
        corrections.put("defect", "defects");
        corrections.put("manufacterer", "manufacturer");
        corrections.put("specificatons", "specifications");
        corrections.put("compatble", "compatible");
        corrections.put("stok", "stock");
        corrections.put("validdation", "validation");
        corrections.put("loadded", "loaded");
        corrections.put("misssing", "missing");
        corrections.put("addedd", "added");
        corrections.put("mastr", "master");
        corrections.put("successfull", "successful");
        corrections.put("passd", "passed");
        corrections.put("becasue", "because");
        corrections.put("skipped", "skipped");
        corrections.put("arnt", "aren't");
        corrections.put("pasd", "passed");
        corrections.put("loadding", "loading");
        corrections.put("happen", "happened");
        corrections.put("oppurtunity", "opportunity");
        corrections.put("flieds", "fields");
        corrections.put("boieng", "boeing");
        corrections.put("corprate", "corporate");
        corrections.put("tomorr", "tomorrow");
        corrections.put("tmrw", "tomorrow");
        corrections.put("todai", "today");
        corrections.put("ystrday", "yesterday");
        corrections.put("recieve", "receive");
        corrections.put("adress", "address");
        corrections.put("definately", "definitely");
        corrections.put("seperately", "separately");
        corrections.put("occured", "occurred");
        corrections.put("becuase", "because");
        corrections.put("teh", "the");
        corrections.put("thier", "their");
        corrections.put("enviroment", "environment");
        
        // Number substitutions for common abbreviations
        corrections.put("4", "for");
        corrections.put("2", "to");
        corrections.put("8", "ate");
        
        // --- AUTO-GENERATE BUSINESS TERM TYPOS ---
        TableColumnConfig config = TableColumnConfig.getInstance();
        List<String> allColumns = new ArrayList<>();
        for (String table : Arrays.asList(
                TableColumnConfig.TABLE_CONTRACTS,
                TableColumnConfig.TABLE_PARTS,
                TableColumnConfig.TABLE_FAILED_PARTS)) {
            Set<String> cols = config.getColumns(table);
            if (cols != null) allColumns.addAll(cols);
        }
        for (String col : allColumns) {
            String colLower = col.toLowerCase();
            String colNoUnderscore = colLower.replace("_", "");
            String colWithSpace = colLower.replace("_", " ");
            String colNoVowels = colLower.replaceAll("[aeiou]", "");
            // Add lowercase, no-underscore, and with-space variants
            corrections.put(colLower, col);
            corrections.put(colNoUnderscore, col);
            corrections.put(colWithSpace, col);
            corrections.put(colNoVowels, col);
            // Add swapped adjacent letters (simple human typo)
            for (int i = 0; i < colLower.length() - 1; i++) {
                char[] chars = colLower.toCharArray();
                char tmp = chars[i];
                chars[i] = chars[i+1];
                chars[i+1] = tmp;
                corrections.put(new String(chars), col);
            }
            // Add missing one letter variants
            for (int i = 0; i < colLower.length(); i++) {
                String missing = colLower.substring(0, i) + colLower.substring(i+1);
                corrections.put(missing, col);
            }
        }
        
        // Opportunity misspellings
        corrections.put("opportunity", "opportunity");
        corrections.put("opportnity", "opportunity");
        corrections.put("opportuntiy", "opportunity");
        corrections.put("oppty", "opportunity");
        corrections.put("opp", "opportunity");
        corrections.put("opportinity", "opportunity");
        corrections.put("opportuity", "opportunity");
        corrections.put("opportnity_name", "opportunity_name");
        corrections.put("oppty_name", "opportunity_name");
        corrections.put("opp_name", "opportunity_name");
        corrections.put("opportunitynumber", "opportunity_number");
        corrections.put("oppty_number", "opportunity_number");
        corrections.put("opp_number", "opportunity_number");
        corrections.put("opportunityid", "opportunity_id");
        corrections.put("oppty_id", "opportunity_id");
        corrections.put("opp_id", "opportunity_id");
        // Customer misspellings
        corrections.put("customer", "customer");
        corrections.put("custmer", "customer");
        corrections.put("custommer", "customer");
        corrections.put("costumer", "customer");
        corrections.put("cust", "customer");
        corrections.put("cstmr", "customer");
        corrections.put("cstm", "customer");
        corrections.put("customername", "customer_name");
        corrections.put("custname", "customer_name");
        corrections.put("clientname", "customer_name");
        corrections.put("buyername", "customer_name");
        corrections.put("customer_no", "customer_number");
        corrections.put("cust_no", "customer_number");
        corrections.put("accountnumber", "account_number");
        corrections.put("account_no", "account_number");
        corrections.put("accountid", "account_id");
        corrections.put("account_id", "account_id");
        
        return corrections;
    }

    // --- YES/NO Normalization Utility ---
    private static final Set<String> YES_WORDS = new HashSet<>(Arrays.asList(
        "yes", "yeah", "yep", "sure", "absolutely", "definitely", "of course", "indeed", "agreed", "ok", "okay", "roger", "aye", "by all means", "certainly", "yup"
    ));
    private static final Set<String> NO_WORDS = new HashSet<>(Arrays.asList(
        "no", "nope", "nah", "not really", "no way", "absolutely not", "never", "nix", "nada", "denied", "hard no", "no chance", "not a chance", "forget it", "i disagree"
    ));
    /**
     * Normalize user input to YES/NO/null for NLP confirmation/intent flows.
     * Returns "YES" for any affirmative, "NO" for any negative, or null if not recognized.
     * Usage: SpellCorrector.normalizeYesNo(userInput)
     */
    public static String normalizeYesNo(String input) {
        if (input == null) return null;
        String norm = input.trim().toLowerCase();
        // Remove punctuation and extra whitespace
       // norm = norm.replaceAll("[!?.]", "").replaceAll("\\s+", " ");
        if (YES_WORDS.contains(norm)) return "YES";
        if (NO_WORDS.contains(norm)) return "NO";
        // Try partial match for multi-word phrases
        for (String yes : YES_WORDS) {
            if (norm.equalsIgnoreCase(yes) || norm.startsWith(yes + " ") || norm.endsWith(" " + yes)) return "YES";
        }
        for (String no : NO_WORDS) {
            if (norm.equalsIgnoreCase(no) || norm.startsWith(no + " ") || norm.endsWith(" " + no)) return "NO";
        }
        return null;
    }

    // Add static sets for field synonyms
    private static final Set<String> EFFECTIVE_DATE_KEYWORDS = new HashSet<>(Arrays.asList(
        "effective", "effect", "eff", "effectivedate", "effectdate", "effectivedt", 
        "effectdt", "effectivedte", "effectdte", "start", "startdate", "startdt", 
        "startdte", "effdt", "effdte", "eff date", "eff dt"
    ));
    private static final Set<String> EXPIRATION_DATE_KEYWORDS = new HashSet<>(Arrays.asList(
        "expiration", "expire", "exp", "expirationdate", "expiredate", "expdate",
        "expirationdt", "expiredt", "expdt", "expirationdte", "expiredte", "expdte",
        "end", "enddate", "enddt", "enddte", "expire date", "exp date"
    ));
    private static final Set<String> CONTRACT_NUMBER_KEYWORDS = new HashSet<>(Arrays.asList(
        "contract", "contractnumber", "contractno", "contractnum", "award", 
        "awardnumber", "awardno", "awardnum", "contractaward", "contractawardnumber",
        "contractawardno", "contractawardnum", "contr", "contrno", "contrnum",
        "awrd", "awrdno", "awrdnum", "contr num", "contract num"
    ));
    private static final Set<String> TITLE_KEYWORDS = new HashSet<>(Arrays.asList(
        "title", "ttl", "contracttitle", "contractttl"
    ));
    private static final Set<String> CONTRACT_NAME_KEYWORDS = new HashSet<>(Arrays.asList(
        "contractname", "name", "contractname", "contrname", "contrtitle",
        "contractnm", "contractttl", "conname", "contitle", "nm", "ttl", "contract name"
    ));
    private static final Set<String> CUSTOMER_KEYWORDS = new HashSet<>(Arrays.asList(
        "customer", "customernumber", "customerno", "customernum", "customername", 
        "customernam", "customernm", "client", "clientnumber", "clientno", "clientnum",
        "clientname", "clientnam", "clientnm", "cust", "custno", "custnum", 
        "custname", "custnam", "custnm", "customer name",
        // Add account synonyms
        "account", "accountnumber", "accountno", "accountnum", "acct", "acctno", "acctnum", "accnt", "accntno", "accntnum", "account name", "acct name"
    ));
    private static final Set<String> AWARD_REP_KEYWORDS = new HashSet<>(Arrays.asList(
        "representative", "rep", "awardrepresentative", "awardrep", "contact",
        "awardrepresentativ", "awardrepr", "contactrep", "contactrepresentative",
        "contactrepresentativ", "contactrepr", "repr", "awrdrep", "awrdrepresentative",
        "awrdrepresentativ", "awrdrepr", "award rep"
    ));
    private static final Set<String> SYSTEM_DATE_KEYWORDS = new HashSet<>(Arrays.asList(
        "system", "sys", "sysdate", "systemdate", "sysd", "sysdt", "systemdt", "systemdte"
    ));
    private static final Set<String> FLOW_DOWN_DATE_KEYWORDS = new HashSet<>(Arrays.asList(
        "flowdown", "flow", "flowdowndate", "flowdate", "flowdt", "flowdowndt",
        "flowdte", "flowdowndte", "flwdown", "flwdt", "flwdte"
    ));
    private static final Set<String> PRICE_EXPIRATION_DATE_KEYWORDS = new HashSet<>(Arrays.asList(
        "priceexpiration", "priceexp", "priceexpirationdate", "priceexpdate", 
        "priceexpirationdt", "priceexpdt", "priceexpirationdte", "priceexpdte",
        "pricingexpiration", "pricingexp", "pricingexpirationdate", "pricingexpdate",
        "pricingexpirationdt", "pricingexpdt", "pricingexpirationdte", "pricingexpdte",
        "prexp", "prexpdt", "prexpdte"
    ));
    private static final Set<String> COMMENTS_KEYWORDS = new HashSet<>(Arrays.asList(
        "comments", "comment", "commenst", "commnts", "cmnts", "cmnt", "commnt", "comm", "remarks", "remark", "notes", "note"
    ));

    public static String normalizeFieldLabel(String label) {
        if (label == null) return null;
        String clean = label.replaceAll("[^a-zA-Z]", "").toLowerCase();
        if (SYSTEM_DATE_KEYWORDS.contains(clean)) return "DATE_OF_SIGNATURE";
        if (EFFECTIVE_DATE_KEYWORDS.contains(clean)) return "EFFECTIVE_DATE";
        if (EXPIRATION_DATE_KEYWORDS.contains(clean)) return "EXPIRATION_DATE";
        if (FLOW_DOWN_DATE_KEYWORDS.contains(clean)) return "FLOW_DOWN_DATE";
        if (PRICE_EXPIRATION_DATE_KEYWORDS.contains(clean)) return "PRICE_EXPIRATION_DATE";
        if (COMMENTS_KEYWORDS.contains(clean)) return "COMMENTS";
        if (TITLE_KEYWORDS.contains(clean)) return "TITLE";
        if (CONTRACT_NUMBER_KEYWORDS.contains(clean)) return "CONTRACT_NUMBER";
        if (CONTRACT_NAME_KEYWORDS.contains(clean)) return "CONTRACT_NAME";
        if (CUSTOMER_KEYWORDS.contains(clean)) return "CUSTOMER";
        if (AWARD_REP_KEYWORDS.contains(clean)) return "AWARD_REPRESENTATIVE";
        if (CUSTOMER_KEYWORDS.contains(clean))
            return "CUSTOMER_NUMBER";
        if (FIELD_SYNONYMS.containsKey(clean))
            return FIELD_SYNONYMS.get(clean);
        // Optionally, try TableColumnConfig business term mapping
        String mapped = TableColumnConfig.getInstance().getColumnForBusinessTerm(TableColumnConfig.TABLE_CONTRACTS, clean);
        if (mapped != null) return mapped;
        // Fallback: uppercase, underscores
        return label.trim().toUpperCase().replace(" ", "_");
    }

    public static String normalizeField(String label) {
        return normalizeFieldLabel(label);
    }

    // Field label synonyms for extraction
    public static final Map<String, String> FIELD_SYNONYMS = new HashMap<>();
    static {
        FIELD_SYNONYMS.put("hpp", "HPP_REQUIRED");
        FIELD_SYNONYMS.put("hpp status", "HPP_REQUIRED");
        FIELD_SYNONYMS.put("price list", "IS_PRICELIST");
        FIELD_SYNONYMS.put("is price", "IS_PRICELIST");
        FIELD_SYNONYMS.put("isprice", "IS_PRICELIST");
        FIELD_SYNONYMS.put("ispricelist", "IS_PRICELIST");
        FIELD_SYNONYMS.put("comments", "COMMENTS");

        FIELD_SYNONYMS.put("Signature Date","DATE_OF_SIGNATURE");
         FIELD_SYNONYMS.put("Signature","DATE_OF_SIGNATURE");
        FIELD_SYNONYMS.put("SignatureDate","DATE_OF_SIGNATURE");
        FIELD_SYNONYMS.put("Sign Date","DATE_OF_SIGNATURE");
        FIELD_SYNONYMS.put("Sign","DATE_OF_SIGNATURE");
        
        

        FIELD_SYNONYMS.put("Expiration","EXPIRATION_DATE");
        FIELD_SYNONYMS.put("Expiration Date","EXPIRATION_DATE");
        FIELD_SYNONYMS.put("Ex Date","EXPIRATION_DATE");
        FIELD_SYNONYMS.put("Expiration","EXPIRATION_DATE");


        FIELD_SYNONYMS.put("Price","PRICE_EXPIRATION_DATE");
        FIELD_SYNONYMS.put("Price Date","PRICE_EXPIRATION_DATE");
        FIELD_SYNONYMS.put("Price", "Expiration Date");
        FIELD_SYNONYMS.put("Price","PRICE_EXPIRATION_DATE");
        FIELD_SYNONYMS.put("Price exp date","PRICE_EXPIRATION_DATE");




        FIELD_SYNONYMS.put("Flow down Date","FLOW_DOWN_DATE");
        FIELD_SYNONYMS.put("Flow down","FLOW_DOWN_DATE");
        FIELD_SYNONYMS.put("Flow  Date","FLOW_DOWN_DATE");
        // Add more as needed
    }
    public static final Map<String, String> DISPLAYNAMES = new HashMap<>();
    static {
        DISPLAYNAMES.put("DATE_OF_SIGNATURE", "Date of Signature Date");
        DISPLAYNAMES.put("FLOW_DOWN_DATE", "Flow Down  Date");
        DISPLAYNAMES.put("EFFECTIVE_DATE", "Effective  Date");
        DISPLAYNAMES.put("EXPIRATION_DATE", "Expiration Date");
        DISPLAYNAMES.put("PRICE_EXPIRATION_DATE", "Price Expiration Date");
        DISPLAYNAMES.put("SYSTEM_LOADED_DATE", "System Loaded Date");
        DISPLAYNAMES.put("QUATAR", "Quatra ");        
        DISPLAYNAMES.put("ACCOUNT_NUMBER","Customer Number");
        DISPLAYNAMES.put("CONTRACT_NAME", "Contract Name");
        DISPLAYNAMES.put("TITLE","Title");
        DISPLAYNAMES.put("DESCRIPTION","Description");
        DISPLAYNAMES.put("COMMENTS","Comments");
        DISPLAYNAMES.put("IS_PRICELIST","Is it a Price List Contract");
        // Add more as needed
    }
    
    public static final String userFriedlyDisplayName(String columenName){
         return DISPLAYNAMES.get(columenName);
    }
    public static void main(String v[]){
        String in="yup";
       String _test= SpellCorrector.normalizeYesNo(in);
       System.out.println("Before :"+in);
       System.out.println("After : "+_test);
    }
}