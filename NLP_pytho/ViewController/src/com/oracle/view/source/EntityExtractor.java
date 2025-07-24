package com.oracle.view.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class EntityExtractor {
    
    // Business Rules from StandardJSONProcessor
    private static final Pattern CONTRACT_NUMBER_PATTERN = Pattern.compile("\\b\\d{6}\\b");
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("\\b[A-Z]{2}\\d{3}\\b");
    private static final Pattern CUSTOMER_NUMBER_PATTERN = Pattern.compile("\\b\\d{6,8}\\b");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    
    // Spell correction dictionary from StandardJSONProcessor
    private static final Map<String, String> SPELL_CORRECTIONS = new HashMap<String, String>() {{
        put("contarct", "contract");
        put("custoemr", "customer");
        put("numbr", "number");
        put("detials", "details");
        put("summry", "summary");
        put("metadat", "metadata");
        put("creted", "created");
        put("effectiv", "effective");
        put("expird", "expired");
        put("activ", "active");
        put("statu", "status");
        put("projct", "project");
        put("pric", "price");
        put("lst", "list");
        put("mnth", "month");
        put("yr", "year");
        put("shw", "show");
        put("gt", "get");
        put("al", "all");
        put("fr", "for");
        put("wth", "with");
        put("nd", "and");
        put("bt", "but");
        put("nt", "not");
        put("hw", "how");
        put("whn", "when");
        put("whr", "where");
        put("wht", "what");
        put("wy", "why");
        put("hw", "how");
        put("cn", "can");
        put("cld", "could");
        put("shld", "should");
        put("wld", "would");
        put("mght", "might");
        put("mst", "must");
        put("hv", "have");
        put("hd", "had");
        put("hs", "has");
        put("r", "are");
        put("s", "is");
        put("ws", "was");
        put("wr", "were");
        put("bn", "been");
        put("bng", "being");
        put("d", "do");
        put("ds", "does");
        put("dd", "did");
        put("dn", "done");
        put("dng", "doing");
        put("g", "go");
        put("gs", "goes");
        put("wnt", "went");
        put("gn", "gone");
        put("gng", "going");
        put("cm", "come");
        put("cms", "comes");
        put("cm", "came");
        put("cmng", "coming");
        put("tk", "take");
        put("tks", "takes");
        put("tk", "took");
        put("tkn", "taken");
        put("tkng", "taking");
        put("mk", "make");
        put("mks", "makes");
        put("md", "made");
        put("mkng", "making");
        put("knw", "know");
        put("knws", "knows");
        put("knw", "knew");
        put("knwn", "known");
        put("knwng", "knowing");
        put("thnk", "think");
        put("thnks", "thinks");
        put("thght", "thought");
        put("thnkng", "thinking");
        put("wrk", "work");
        put("wrks", "works");
        put("wrkd", "worked");
        put("wrkng", "working");
        put("us", "use");
        put("uss", "uses");
        put("usd", "used");
        put("usng", "using");
        put("gt", "get");
        put("gts", "gets");
        put("gt", "got");
        put("gttng", "getting");
        put("gv", "give");
        put("gvs", "gives");
        put("gv", "gave");
        put("gvn", "given");
        put("gvng", "giving");
        put("fnd", "find");
        put("fnds", "finds");
        put("fnd", "found");
        put("fndng", "finding");
        put("lk", "look");
        put("lks", "looks");
        put("lkd", "looked");
        put("lkng", "looking");
        put("s", "see");
        put("ss", "sees");
        put("sw", "saw");
        put("sn", "seen");
        put("sng", "seeing");
        put("hr", "hear");
        put("hrs", "hears");
        put("hrd", "heard");
        put("hrng", "hearing");
        put("fl", "feel");
        put("fls", "feels");
        put("flt", "felt");
        put("flng", "feeling");
        put("lv", "leave");
        put("lvs", "leaves");
        put("lft", "left");
        put("lvng", "leaving");
        put("pt", "put");
        put("pts", "puts");
        put("pttng", "putting");
        put("kp", "keep");
        put("kps", "keeps");
        put("kpt", "kept");
        put("kpng", "keeping");
        put("lt", "let");
        put("lts", "lets");
        put("lttng", "letting");
        put("bg", "begin");
        put("bgs", "begins");
        put("bgn", "began");
        put("bgn", "begun");
        put("bgnng", "beginning");
        put("hlp", "help");
        put("hlps", "helps");
        put("hlpd", "helped");
        put("hlpng", "helping");
        put("ply", "play");
        put("plys", "plays");
        put("plyd", "played");
        put("plyng", "playing");
        put("rn", "run");
        put("rns", "runs");
        put("rn", "ran");
        put("rnng", "running");
        put("mv", "move");
        put("mvs", "moves");
        put("mvd", "moved");
        put("mvng", "moving");
        put("lv", "live");
        put("lvs", "lives");
        put("lvd", "lived");
        put("lvng", "living");
        put("blv", "believe");
        put("blvs", "believes");
        put("blvd", "believed");
        put("blvng", "believing");
        put("hld", "hold");
        put("hlds", "holds");
        put("hld", "held");
        put("hldng", "holding");
        put("brng", "bring");
        put("brngs", "brings");
        put("brght", "brought");
        put("brngng", "bringing");
        put("hppn", "happen");
        put("hppns", "happens");
        put("hppnd", "happened");
        put("hppnng", "happening");
        put("wrt", "write");
        put("wrts", "writes");
        put("wrt", "wrote");
        put("wrttn", "written");
        put("wrtng", "writing");
        put("prvd", "provide");
        put("prvds", "provides");
        put("prvdd", "provided");
        put("prvdng", "providing");
        put("st", "sit");
        put("sts", "sits");
        put("st", "sat");
        put("sttng", "sitting");
        put("stnd", "stand");
        put("stnds", "stands");
        put("std", "stood");
        put("stndng", "standing");
        put("ls", "lose");
        put("lss", "loses");
        put("lst", "lost");
        put("lsng", "losing");
        put("py", "pay");
        put("pys", "pays");
        put("pd", "paid");
        put("pyng", "paying");
        put("mt", "meet");
        put("mts", "meets");
        put("mt", "met");
        put("mtng", "meeting");
        put("ncld", "include");
        put("nclds", "includes");
        put("ncldd", "included");
        put("ncldng", "including");
        put("cntnu", "continue");
        put("cntnus", "continues");
        put("cntnud", "continued");
        put("cntnng", "continuing");
        put("st", "set");
        put("sts", "sets");
        put("sttng", "setting");
        put("lrn", "learn");
        put("lrns", "learns");
        put("lrnd", "learned");
        put("lrnng", "learning");
        put("chng", "change");
        put("chngs", "changes");
        put("chngd", "changed");
        put("chngng", "changing");
        put("ld", "lead");
        put("lds", "leads");
        put("ld", "led");
        put("ldng", "leading");
        put("ndrstnd", "understand");
        put("ndrstnds", "understands");
        put("ndrstd", "understood");
        put("ndrstndng", "understanding");
        put("wth", "watch");
        put("wths", "watches");
        put("wthd", "watched");
        put("wthng", "watching");
        put("fllw", "follow");
        put("fllws", "follows");
        put("fllwd", "followed");
        put("fllwng", "following");
        put("stp", "stop");
        put("stps", "stops");
        put("stpd", "stopped");
        put("stpng", "stopping");
        put("crt", "create");
        put("crts", "creates");
        put("crtd", "created");
        put("crtng", "creating");
        put("spk", "speak");
        put("spks", "speaks");
        put("spk", "spoke");
        put("spkn", "spoken");
        put("spkng", "speaking");
        put("rd", "read");
        put("rds", "reads");
        put("rdng", "reading");
        put("spnd", "spend");
        put("spnds", "spends");
        put("spnt", "spent");
        put("spndng", "spending");
        put("grw", "grow");
        put("grws", "grows");
        put("grw", "grew");
        put("grwn", "grown");
        put("grwng", "growing");
        put("pn", "open");
        put("pns", "opens");
        put("pnd", "opened");
        put("pnng", "opening");
        put("wlk", "walk");
        put("wlks", "walks");
        put("wlkd", "walked");
        put("wlkng", "walking");
        put("wn", "win");
        put("wns", "wins");
        put("wn", "won");
        put("wnng", "winning");
        put("ffr", "offer");
        put("ffrs", "offers");
        put("ffrd", "offered");
        put("ffrng", "offering");
        put("rmmbr", "remember");
        put("rmmbrs", "remembers");
        put("rmmbrd", "remembered");
        put("rmmbrng", "remembering");
        put("cnsdr", "consider");
        put("cnsdrs", "considers");
        put("cnsdrd", "considered");
        put("cnsdrng", "considering");
        put("ppr", "appear");
        put("pprs", "appears");
        put("pprd", "appeared");
        put("pprng", "appearing");
        put("by", "buy");
        put("bys", "buys");
        put("bght", "bought");
        put("byng", "buying");
        put("srv", "serve");
        put("srvs", "serves");
        put("srvd", "served");
        put("srvng", "serving");
        put("s", "send");
        put("snds", "sends");
        put("snt", "sent");
        put("sndng", "sending");
        put("xpct", "expect");
        put("xpcts", "expects");
        put("xpctd", "expected");
        put("xpctng", "expecting");
        put("bld", "build");
        put("blds", "builds");
        put("blt", "built");
        put("bldng", "building");
        put("sty", "stay");
        put("stys", "stays");
        put("styd", "stayed");
        put("styng", "staying");
        put("fll", "fall");
        put("flls", "falls");
        put("fll", "fell");
        put("fllng", "falling");
        put("ct", "cut");
        put("cts", "cuts");
        put("cttng", "cutting");
        put("rch", "reach");
        put("rchs", "reaches");
        put("rchd", "reached");
        put("rchng", "reaching");
        put("kll", "kill");
        put("klls", "kills");
        put("klld", "killed");
        put("kllng", "killing");
        put("rmn", "remain");
        put("rmns", "remains");
        put("rmnd", "remained");
        put("rmnng", "remaining");
        put("sgst", "suggest");
        put("sgsts", "suggests");
        put("sgstd", "suggested");
        put("sgstng", "suggesting");
        put("rs", "raise");
        put("rss", "raises");
        put("rsd", "raised");
        put("rsng", "raising");
        put("pss", "pass");
        put("psss", "passes");
        put("pssd", "passed");
        put("pssng", "passing");
        put("sll", "sell");
                put("sll", "sell");
        put("slls", "sells");
        put("sld", "sold");
        put("sllng", "selling");
        put("rqr", "require");
        put("rqrs", "requires");
        put("rqrd", "required");
        put("rqrng", "requiring");
        put("fgr", "figure");
        put("fgrs", "figures");
        put("fgrd", "figured");
        put("fgrng", "figuring");
        put("brkng", "breaking");
        put("brk", "break");
        put("brks", "breaks");
        put("brk", "broke");
        put("brkn", "broken");
    }};
    
    private final Map<Pattern, String> entityPatterns;
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private NameFinderME personFinder;
    private NameFinderME organizationFinder;
    private NameFinderME contractNumberFinder;
    private NameFinderME customerNameFinder;
    private NameFinderME dateFinder;
    private static boolean modelsInitialized = false;
    public static final String MODEL_PATH = "F:\\GitHub_VinodLearning\\NLPTEST\\NLP\\NLPMachineDesignApp\\models\\";
    
    public EntityExtractor() {
        this.entityPatterns = initializeEntityPatterns();
        initializeNLPModelsOnce();
    }
    
    /**
     * Spell correction from StandardJSONProcessor
     */
    public String correctSpelling(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder corrected = new StringBuilder();
        boolean hasCorrections = false;
        
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (SPELL_CORRECTIONS.containsKey(cleanWord)) {
                corrected.append(SPELL_CORRECTIONS.get(cleanWord));
                hasCorrections = true;
            } else {
                corrected.append(word);
            }
            corrected.append(" ");
        }
        
        return hasCorrections ? corrected.toString().trim() : input;
    }
    
    /**
     * Enhanced tokenization from StandardJSONProcessor
     */
    public List<String> enhancedTokenize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // First apply spell correction
        String correctedInput = correctSpelling(input);
        
        // Use OpenNLP tokenizer if available
        if (tokenizer != null) {
            String[] tokens = tokenizer.tokenize(correctedInput);
            return Arrays.asList(tokens);
        } else {
            // Fallback tokenization
            return Arrays.asList(correctedInput.trim().split("\\s+"));
        }
    }
    
    /**
     * Business rules extraction from StandardJSONProcessor
     */
    public ContractQueryResponse extractWithBusinessRules(String input) {
        String correctedInput = correctSpelling(input);
        List<String> tokens = enhancedTokenize(correctedInput);
        
        // Initialize response components
        QueryHeader header = new QueryHeader();
        List<QueryEntity> filters = new ArrayList<>();
        List<String> displayEntities = new ArrayList<>();
        QueryMetadata metadata = new QueryMetadata();
        List<String> errors = new ArrayList<>();
        
        // Extract entities using business rules
        extractContractNumbers(tokens, header, filters);
        extractPartNumbers(tokens, header, filters);
        extractCustomerInfo(tokens, header, filters);
        extractDateInfo(tokens, filters);
        extractStatusInfo(tokens, filters);
        
        // Determine query type and action
        determineQueryTypeAndAction(input, tokens, metadata);
        
        // Determine display entities
        displayEntities = determineDisplayEntities(input, filters);
        
        // Validate and add errors if needed
        validateQuery(input, filters, errors);
        
        return new ContractQueryResponse(
            input,
            correctedInput,
            header,
            metadata,
            filters,
            displayEntities,
            errors
        );
    }
    
    private void extractContractNumbers(List<String> tokens, QueryHeader header, List<QueryEntity> filters) {
        for (String token : tokens) {
            Matcher matcher = CONTRACT_NUMBER_PATTERN.matcher(token);
            if (matcher.find()) {
                String contractNumber = matcher.group();
                header.setContractNumber(contractNumber);
                filters.add(new QueryEntity("AWARD_NUMBER", "=", contractNumber, "business_rule"));
                System.out.println("Contract number extracted: " + contractNumber);
            }
        }
    }
    
    private void extractPartNumbers(List<String> tokens, QueryHeader header, List<QueryEntity> filters) {
        for (String token : tokens) {
            Matcher matcher = PART_NUMBER_PATTERN.matcher(token.toUpperCase());
            if (matcher.find()) {
                String partNumber = matcher.group();
                header.setPartNumber(partNumber);
                filters.add(new QueryEntity("PART_NUMBER", "=", partNumber, "business_rule"));
                System.out.println("Part number extracted: " + partNumber);
            }
        }
    }
    
    private void extractCustomerInfo(List<String> tokens, QueryHeader header, List<QueryEntity> filters) {
        String joinedTokens = String.join(" ", tokens).toLowerCase();
        
        // Extract customer numbers
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            // Check for customer number context
            if (i > 0 && (tokens.get(i-1).toLowerCase().contains("customer") || 
                         tokens.get(i-1).toLowerCase().contains("account"))) {
                Matcher matcher = CUSTOMER_NUMBER_PATTERN.matcher(token);
                if (matcher.find() && token.length() >= 6) {
                    String customerNumber = matcher.group();
                    header.setCustomerNumber(customerNumber);
                    filters.add(new QueryEntity("CUSTOMER_NUMBER", "=", customerNumber, "business_rule"));
                    System.out.println("Customer number extracted: " + customerNumber);
                }
            }
        }
        
        // Extract customer names from quoted strings
        Pattern customerNamePattern = Pattern.compile("(?:customer|account)\\s+name\\s+['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = customerNamePattern.matcher(joinedTokens);
        if (nameMatcher.find()) {
            String customerName = nameMatcher.group(1).trim();
            header.setCustomerName(customerName);
            filters.add(new QueryEntity("CUSTOMER_NAME", "=", customerName, "business_rule"));
            System.out.println("Customer name extracted: " + customerName);
        }
    }
    
    private void extractDateInfo(List<String> tokens, List<QueryEntity> filters) {
        String joinedTokens = String.join(" ", tokens).toLowerCase();
        
        // Extract years
        for (String token : tokens) {
            Matcher matcher = YEAR_PATTERN.matcher(token);
            if (matcher.find()) {
                String year = matcher.group();
                if (joinedTokens.contains("created") || joinedTokens.contains("in " + year)) {
                    filters.add(new QueryEntity("CREATE_DATE", "=", year, "business_rule"));
                    System.out.println("Year extracted: " + year);
                }
            }
        }
        
        // Extract relative dates
        if (joinedTokens.contains("last month")) {
            filters.add(new QueryEntity("CREATE_DATE", ">=", "LAST_MONTH", "business_rule"));
        }
        if (joinedTokens.contains("this year")) {
            filters.add(new QueryEntity("CREATE_DATE", ">=", "THIS_YEAR", "business_rule"));
        }
    }
    
    private void extractStatusInfo(List<String> tokens, List<QueryEntity> filters) {
        String joinedTokens = String.join(" ", tokens).toLowerCase();
        
        if (joinedTokens.contains("active")) {
            filters.add(new QueryEntity("STATUS", "=", "ACTIVE", "business_rule"));
        }
        if (joinedTokens.contains("expired")) {
            filters.add(new QueryEntity("STATUS", "=", "EXPIRED", "business_rule"));
        }
        if (joinedTokens.contains("failed")) {
            filters.add(new QueryEntity("STATUS", "=", "FAILED", "business_rule"));
        }
    }
    
    private void determineQueryTypeAndAction(String input, List<String> tokens, QueryMetadata metadata) {
        String lowerInput = input.toLowerCase();
        
        // Determine query type
        if (lowerInput.contains("part") || lowerInput.contains("component")) {
            metadata.setQueryType("PARTS");
            if (lowerInput.contains("failed")) {
                metadata.setActionType("failed_parts_report");
            } else {
                metadata.setActionType("parts_query");
            }
        } else if (lowerInput.contains("contract") || lowerInput.contains("award")) {
            metadata.setQueryType("CONTRACTS");
            
            // Determine specific action
            if (lowerInput.contains("metadata")) {
                metadata.setActionType("contract_metadata");
            } else if (lowerInput.contains("customer")) {
                metadata.setActionType("contracts_by_customer");
            } else if (lowerInput.contains("created")) {
                metadata.setActionType("contracts_by_date");
            } else {
                metadata.setActionType("contract_details");
            }
        } else {
            metadata.setQueryType("GENERAL");
            metadata.setActionType("general_query");
        }
    }
    
    private List<String> determineDisplayEntities(String input, List<QueryEntity> filters) {
        Set<String> displayFields = new LinkedHashSet<>();
        String lowerInput = input.toLowerCase();
        
        // Always include primary identifier
        displayFields.add("CONTRACT_NUMBER");
        
        // Add fields based on query intent
        if (lowerInput.contains("metadata") || lowerInput.contains("all")) {
            displayFields.addAll(Arrays.asList(
                "CONTRACT_NUMBER", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "PART_NUMBER",
                "CREATE_DATE", "EFFECTIVE_DATE", "EXPIRY_DATE", "STATUS", "PROJECT_TYPE",
                "PRICE_LIST", "CREATED_BY"
            ));
        } else if (lowerInput.contains("customer")) {
            displayFields.addAll(Arrays.asList("CUSTOMER_NAME", "CUSTOMER_NUMBER"));
        } else if (lowerInput.contains("part")) {
            displayFields.addAll(Arrays.asList("PART_NUMBER", "PART_STATUS"));
        } else if (lowerInput.contains("date")) {
            displayFields.addAll(Arrays.asList("CREATE_DATE", "EFFECTIVE_DATE", "EXPIRY_DATE"));
        } else if (lowerInput.contains("status")) {
            displayFields.addAll(Arrays.asList("STATUS", "EFFECTIVE_DATE"));
        } else {
            // Default fields
            displayFields.addAll(Arrays.asList("CONTRACT_NUMBER", "CUSTOMER_NAME", "STATUS"));
        }
        
        return new ArrayList<>(displayFields);
    }
    
    private void validateQuery(String input, List<QueryEntity> filters, List<String> errors) {
        if (input == null || input.trim().isEmpty()) {
            errors.add("Input cannot be empty");
            return;
        }
        
        if (input.trim().length() < 3) {
            errors.add("Input too short - please provide more details");
        }
        
        if (filters.isEmpty()) {
            errors.add("No valid entities found in query - please check your input");
        }
        
        // Validate specific patterns
        String lowerInput = input.toLowerCase();
        if (lowerInput.contains("contract") && !hasContractRelatedFilter(filters)) {
            errors.add("Contract query detected but no contract-related filters found");
        }
        
        if (lowerInput.contains("customer") && !hasCustomerRelatedFilter(filters)) {
            errors.add("Customer query detected but no customer-related filters found");
        }
    }
    
    private boolean hasContractRelatedFilter(List<QueryEntity> filters) {
        return filters.stream().anyMatch(f -> 
            "AWARD_NUMBER".equals(f.getAttribute()) || 
            "CONTRACT_NUMBER".equals(f.getAttribute())
        );
    }
    
    private boolean hasCustomerRelatedFilter(List<QueryEntity> filters) {
        return filters.stream().anyMatch(f -> 
            "CUSTOMER_NUMBER".equals(f.getAttribute()) || 
            "CUSTOMER_NAME".equals(f.getAttribute())
        );
    }
    
    // Keep existing methods for backward compatibility
    private Map<Pattern, String> initializeEntityPatterns() {
        Map<Pattern, String> patterns = new HashMap<>();
        
        // Contract number patterns
        patterns.put(Pattern.compile("(?:get\\s+all\\s+metadata\\s+for\\s+)?contract\\s+(\\d{6})", Pattern.CASE_INSENSITIVE), "CONTRACT_NUMBER");
        patterns.put(Pattern.compile("award\\s+(\\d{6})", Pattern.CASE_INSENSITIVE), "CONTRACT_NUMBER");   
        
        // Customer patterns
        patterns.put(Pattern.compile("customer\\s+number\\s+(\\d{6,8})", Pattern.CASE_INSENSITIVE), "CUSTOMER_NUMBER");
        patterns.put(Pattern.compile("account\\s+(\\d{7,8})(?:\\s+contracts)?", Pattern.CASE_INSENSITIVE), "CUSTOMER_NUMBER");
        
        // Date patterns
        patterns.put(Pattern.compile("created\\s+in\\s+(\\d{4})", Pattern.CASE_INSENSITIVE), "CREATE_DATE");
        patterns.put(Pattern.compile("in\\s+(\\d{4})", Pattern.CASE_INSENSITIVE), "CREATE_DATE");
        
        return patterns;
    }
    
    public void populateHeaderFromFilters(ContractQueryResponse response) {
        if (response.getFilters() != null) {
            for (QueryEntity filter : response.getFilters()) {
                switch (filter.getAttribute()) {
                    case "AWARD_NUMBER":
                    case "CONTRACT_NUMBER":
                        if (response.getHeader().getContractNumber() == null) {
                            response.getHeader().setContractNumber(filter.getValue());
                        }
                        break;
                    case "CUSTOMER_NUMBER":
                        if (response.getHeader().getCustomerNumber() == null) {
                            response.getHeader().setCustomerNumber(filter.getValue());
                        }
                        break;
                    case "CUSTOMER_NAME":
                        if (response.getHeader().getCustomerName() == null) {
                            response.getHeader().setCustomerName(filter.getValue());
                        }
                        break;
                    case "CREATED_BY":
                        if (response.getHeader().getCreatedBy() == null) {
                            response.getHeader().setCreatedBy(filter.getValue());
                        }
                        break;
                }
            }
        }
    }
// Keep existing OpenNLP methods
    private synchronized void initializeNLPModelsOnce() {
        if (!modelsInitialized) {
            initializeNLPModels();
            modelsInitialized = true;
        }
    }
    
    private void initializeNLPModels() {
        System.out.println("=== Initializing OpenNLP Models (One Time) ===");
        
        try {
            // Initialize tokenizer
            InputStream tokenModelIn = getModelInputStream("en-token.bin");
            if (tokenModelIn != null) {
                TokenizerModel tokenModel = new TokenizerModel(tokenModelIn);
                tokenizer = new TokenizerME(tokenModel);
                tokenModelIn.close();
                System.out.println("? Tokenizer model loaded successfully");
            }
            
            // Initialize POS tagger
            InputStream posModelIn = getModelInputStream("en-pos-maxent.bin");
            if (posModelIn != null) {
                POSModel posModel = new POSModel(posModelIn);
                posTagger = new POSTaggerME(posModel);
                posModelIn.close();
                System.out.println("? POS Tagger model loaded successfully");
            }
            
            // Initialize Named Entity Recognition
            InputStream personModelIn = getModelInputStream("en-ner-person.bin");
            if (personModelIn != null) {
                TokenNameFinderModel personModel = new TokenNameFinderModel(personModelIn);
                personFinder = new NameFinderME(personModel);
                personModelIn.close();
                System.out.println("? Person NER model loaded successfully");
            }
            
            InputStream orgModelIn = getModelInputStream("en-ner-organization.bin");
            if (orgModelIn != null) {
                TokenNameFinderModel orgModel = new TokenNameFinderModel(orgModelIn);
                organizationFinder = new NameFinderME(orgModel);
                orgModelIn.close();
                System.out.println("? Organization NER model loaded successfully");
            }
            
            // Load custom NER models for business entities
            InputStream contractModelIn = getModelInputStream("en-ner-contractnumber.bin");
            if (contractModelIn != null) {
                TokenNameFinderModel contractModel = new TokenNameFinderModel(contractModelIn);
                contractNumberFinder = new NameFinderME(contractModel);
                contractModelIn.close();
            }
            InputStream customerModelIn = getModelInputStream("en-ner-customername.bin");
            if (customerModelIn != null) {
                TokenNameFinderModel customerModel = new TokenNameFinderModel(customerModelIn);
                customerNameFinder = new NameFinderME(customerModel);
                customerModelIn.close();
            }
//            InputStream dateModelIn = getModelInputStream("en-ner-date.bin");
//            if (dateModelIn != null) {
//                TokenNameFinderModel dateModel = new TokenNameFinderModel(dateModelIn);
//                dateFinder = new NameFinderME(dateModel);
//                dateModelIn.close();
//            }
            
            System.out.println("=== OpenNLP Models Initialization Complete ===");
            
        } catch (Exception e) {
            System.err.println("Warning: Could not load OpenNLP models, falling back to basic tokenization");
            e.printStackTrace();
        }
    }
    
    private InputStream getModelInputStream(String fileName) {
        if (fileName != null) {
            try {
                String fullPath = MODEL_PATH + fileName;
                File modelFile = new File(fullPath);
                
                if (!modelFile.exists()) {
                    System.err.println("Model file not found: " + fullPath);
                    return null;
                }
                if (!modelFile.canRead()) {
                    System.err.println("Cannot read model file: " + fullPath);
                    return null;
                }
                if (modelFile.length() == 0) {
                    System.err.println("Model file is empty: " + fullPath);
                    return null;
                }

                return new FileInputStream(modelFile);
            } catch (Exception e) {
                System.err.println("Error loading model " + fileName + ": " + e.getMessage());
                return null;
            }
        }
        return null;
    }
    
    // Legacy methods for backward compatibility
    public List<QueryEntity> extractEntities(String input, QueryClassification classification) {
        // Use the new business rules method but extract only filters
        ContractQueryResponse response = extractWithBusinessRules(input);
        return response.getFilters();
    }
    
    public QueryHeader extractHeaderInformation(String input, List<QueryEntity> entities) {
        QueryHeader header = new QueryHeader();
        
        for (QueryEntity entity : entities) {
            switch (entity.getAttribute()) {
                case "AWARD_NUMBER":
                case "CONTRACT_NUMBER":
                    header.setContractNumber(entity.getValue());
                    break;
                case "CUSTOMER_NUMBER":
                    header.setCustomerNumber(entity.getValue());
                    break;
                case "CUSTOMER_NAME":
                    header.setCustomerName(entity.getValue());
                    break;
                case "CREATED_BY":
                    header.setCreatedBy(entity.getValue());
                    break;
                case "PART_NUMBER":
                    header.setPartNumber(entity.getValue());
                    break;
            }
        }
        
        return header;
    }
    
    private String mapToTableColumn(String entityType) {
        Map<String, String> columnMapping = new HashMap<>();
        columnMapping.put("CONTRACT_NUMBER", "AWARD_NUMBER");
        columnMapping.put("CUSTOMER_NUMBER", "CUSTOMER_NUMBER");
        columnMapping.put("CUSTOMER_NAME", "CUSTOMER_NAME");
        columnMapping.put("CREATED_DATE", "CREATE_DATE");
        columnMapping.put("CREATE_DATE", "CREATE_DATE");
        columnMapping.put("CREATED_BY", "CREATED_BY");
        columnMapping.put("EFFECTIVE_DATE", "EFFECTIVE_DATE");
        columnMapping.put("STATUS", "STATUS");
        columnMapping.put("PROJECT_TYPE", "PROJECT_TYPE");
        
        return columnMapping.getOrDefault(entityType, entityType);
    }

    // Advanced Entity Extraction Method
    public Map<String, String> extractAllEntities(String input) {
        Map<String, String> entities = new HashMap<>();
        String[] tokens = tokenizer != null ? tokenizer.tokenize(input) : input.split("\\s+");

        // Run all custom NER models
        if (contractNumberFinder != null) {
            for (Span span : contractNumberFinder.find(tokens)) {
                String value = String.join(" ", Arrays.copyOfRange(tokens, span.getStart(), span.getEnd()));
                entities.put("CONTRACT_NUMBER", value);
            }
        }
        if (customerNameFinder != null) {
            for (Span span : customerNameFinder.find(tokens)) {
                String value = String.join(" ", Arrays.copyOfRange(tokens, span.getStart(), span.getEnd()));
                entities.put("CUSTOMER_NAME", value);
            }
        }
        if (dateFinder != null) {
            for (Span span : dateFinder.find(tokens)) {
                String value = String.join(" ", Arrays.copyOfRange(tokens, span.getStart(), span.getEnd()));
                entities.put("DATE", value);
            }
        }
        // ... repeat for other NER models as needed

        // Run regexes for patterns not covered by NER
        Matcher contractMatcher = CONTRACT_NUMBER_PATTERN.matcher(input);
        if (contractMatcher.find()) {
            entities.put("CONTRACT_NUMBER", contractMatcher.group());
        }
        Matcher yearMatcher = YEAR_PATTERN.matcher(input);
        if (yearMatcher.find()) {
            entities.put("YEAR", yearMatcher.group());
        }
        // ... add more regexes as needed

        // Normalize and map entities with fuzzy matching
        Map<String, String> normalized = new HashMap<>();
        TableColumnConfig tableConfig = TableColumnConfig.getInstance();
        for (Map.Entry<String, String> entry : entities.entrySet()) {
            String key = SpellCorrector.normalizeFieldLabel(entry.getKey());
            String mapped = tableConfig.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_CONTRACTS, key);
            if (mapped == null) {
                mapped = tableConfig.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_PARTS, key);
            }
            if (mapped == null) {
                mapped = tableConfig.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_CUSTOMERS, key);
            }
            if (mapped == null) {
                mapped = tableConfig.getColumnForSynonymFuzzy("HR.CCT_OPPORTUNITIES_TMG", key);
            }
            normalized.put(mapped != null ? mapped : key, entry.getValue());
        }
        return normalized;
    }
}