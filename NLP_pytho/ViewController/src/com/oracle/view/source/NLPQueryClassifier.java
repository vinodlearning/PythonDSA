// REQUIRED OPENNLP MODELS (place in models/ directory):
// English:
//   en-token.bin              https://opennlp.sourceforge.net/models-1.5/en-token.bin
//   en-pos-maxent.bin         https://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin
//   en-ner-person.bin         https://opennlp.sourceforge.net/models-1.5/en-ner-person.bin
//   en-ner-organization.bin   https://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin
// Spanish:
//   es-token.bin              https://opennlp.sourceforge.net/models-1.5/es-token.bin
//   es-pos-perceptron.bin     https://opennlp.sourceforge.net/models-1.5/es-pos-perceptron.bin
//   es-ner-person.bin         https://opennlp.sourceforge.net/models-1.5/es-ner-person.bin
//   es-ner-organization.bin   https://opennlp.sourceforge.net/models-1.5/es-ner-organization.bin
//
// Place all these files in: models/
//
// The code will auto-detect language and use the correct models for English or Spanish.
package com.oracle.view.source;

import java.util.*;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Modular NLP Query Classifier
 * Routes queries to specialized processors based on content analysis
 * 
 * Architecture:
 * 1. NLPQueryClassifier - Main router
 * 2. FailedPartsProcessor - Handles failed parts queries
 * 3. PartsProcessor - Handles regular parts queries  
 * 4. ContractProcessor - Handles contract queries
 * 5. HelpProcessor - Handles help/creation queries
 */
public class NLPQueryClassifier {
    
    private final FailedPartsProcessor failedPartsProcessor;
    private final PartsProcessor partsProcessor;
    private final ContractProcessor contractProcessor;
    private final HelpProcessor helpProcessor;
    private final SpellCorrector spellCorrector;
    private final Lemmatizer lemmatizer;
    private final CustomerProcessor customerProcessor = new CustomerProcessor();
    private final OpportunitiesProcessor opportunitiesProcessor = new OpportunitiesProcessor();
    
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private NameFinderME nameFinder;
    private NameFinderME orgNameFinder;
    private boolean nlpModelsLoaded = false;

    // Spanish models
    private TokenizerME esTokenizer;
    private POSTaggerME esPosTagger;
    private NameFinderME esPersonFinder;
    private NameFinderME esOrgFinder;
    private boolean esModelsLoaded = false;

    // Simple lemmatizer (dictionary-based, can be extended)
    private static final Map<String, String> enLemmaDict = new HashMap<>();
    private static final Map<String, String> esLemmaDict = new HashMap<>();
    static {
        enLemmaDict.put("creating", "create"); enLemmaDict.put("created", "create"); enLemmaDict.put("contracts", "contract");
        esLemmaDict.put("creando", "crear"); esLemmaDict.put("creado", "crear"); esLemmaDict.put("contratos", "contrato");
        // Add more as needed
    }
public static final String MODEL_PATH="F:\\GitHub_VinodLearning\\NLPTEST\\NLP\\NLPMachineDesignApp\\models\\";
    // Regex-based date extraction (English and Spanish)
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b|" + // 12/05/2024 or 12-05-2024
        "\\b(\\d{1,2}\\s+de\\s+[a-zA-Z]+\\s+de\\s+\\d{4})\\b", // 12 de mayo de 2024
        Pattern.CASE_INSENSITIVE);

    public NLPQueryClassifier() {
        try {
            tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(MODEL_PATH+"en-token.bin")));
            posTagger = new POSTaggerME(new POSModel(new FileInputStream(MODEL_PATH+"en-pos-maxent.bin")));
            nameFinder = new NameFinderME(new TokenNameFinderModel(new FileInputStream(MODEL_PATH+"en-ner-person.bin")));
            orgNameFinder = new NameFinderME(new TokenNameFinderModel(new FileInputStream(MODEL_PATH+"en-ner-organization.bin")));
            nlpModelsLoaded = true;
            // Commented out date NER model loading as per user request
            // dateFinder = new NameFinderME(new TokenNameFinderModel(new FileInputStream(MODEL_PATH+"en-ner-date.bin")));
        } catch (IOException e) {
            System.err.println("[OpenNLP] Failed to load English models: " + e.getMessage());
            nlpModelsLoaded = false;
        }
        // Spanish model loading is disabled due to unavailability of classic models
        /*
        try {
            esTokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(MODEL_PATH+"es-token.bin")));
            esPosTagger = new POSTaggerME(new POSModel(new FileInputStream(MODEL_PATH+"es-pos-perceptron.bin")));
            esPersonFinder = new NameFinderME(new TokenNameFinderModel(new FileInputStream(MODEL_PATH+"es-ner-person.bin")));
            esOrgFinder = new NameFinderME(new TokenNameFinderModel(new FileInputStream(MODEL_PATH+"es-ner-organization.bin")));
            esModelsLoaded = true;
        } catch (IOException e) {
            System.err.println("[OpenNLP] Spanish models not found or failed to load: " + e.getMessage());
            esModelsLoaded = false;
        }
        */
        this.failedPartsProcessor = new FailedPartsProcessor();
        this.partsProcessor = new PartsProcessor();
        this.contractProcessor = new ContractProcessor();
        this.helpProcessor = new HelpProcessor();
        this.spellCorrector = new SpellCorrector();
        this.lemmatizer = Lemmatizer.getInstance();
    }
    
    /**
     * Enhanced query classification with modular processor architecture
     */
    public QueryResult processQuery(String userInput) {
        // Delegate to classifyWithDisambiguation to ensure latest NLP logic is always used
        return classifyWithDisambiguation(userInput);
    }

    // Language detection (very simple, can be replaced with a library)
    private String detectLanguage(String input) {
        if (input.matches(".*\\b(de|contrato|crear|por|para|con|cliente|empresa|organización)\\b.*")) {
            return "es";
        }
        return "en";
    }

    // Simple lemmatizer
    private String lemmatize(String word, String lang) {
        if (lang.equals("es")) return esLemmaDict.getOrDefault(word.toLowerCase(), word);
        return enLemmaDict.getOrDefault(word.toLowerCase(), word);
    }

    // Date extraction utility
    public List<String> extractDates(String input) {
        List<String> dates = new ArrayList<>();
        Matcher matcher = DATE_PATTERN.matcher(input);
        while (matcher.find()) {
            dates.add(matcher.group());
        }
        return dates;
    }

    // New method: OpenNLP-based intent/entity extraction and disambiguation
    public QueryResult classifyWithDisambiguation(String input) {
        QueryResult result = new QueryResult();
        String lang = detectLanguage(input);
        String[] tokens;
        String[] tags;
        Span[] nameSpans;
        Span[] orgSpans;
        if (lang.equals("es") && esModelsLoaded) {
            tokens = esTokenizer.tokenize(input);
            tags = esPosTagger.tag(tokens);
            nameSpans = esPersonFinder.find(tokens);
            orgSpans = esOrgFinder.find(tokens);
        } else if (nlpModelsLoaded) {
            tokens = tokenizer.tokenize(input);
            tags = posTagger.tag(tokens);
            nameSpans = nameFinder.find(tokens);
            orgSpans = orgNameFinder.find(tokens);
        } else {
            // Fallback: return a default QueryResult if models are not loaded
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            result.metadata = new QueryMetadata("ERROR", "MODEL_NOT_LOADED", 0.0);
            result.entities = new ArrayList<>();
            result.displayEntities = new ArrayList<>();
            result.errors = new ArrayList<>();
            return result;
        }
        // Lemmatize tokens
        List<String> lemmas = new ArrayList<>();
        for (String token : tokens) {
            lemmas.add(lemmatize(token, lang));
        }
        // NER extraction
        StringBuilder persons = new StringBuilder();
        for (Span span : nameSpans) {
            for (int i = span.getStart(); i < span.getEnd(); i++) {
                persons.append(tokens[i]).append(" ");
            }
        }
        StringBuilder orgs = new StringBuilder();
        for (Span span : orgSpans) {
            for (int i = span.getStart(); i < span.getEnd(); i++) {
                orgs.append(tokens[i]).append(" ");
            }
        }
        String personEntity = persons.toString().trim();
        String orgEntity = orgs.toString().trim();
        // Date extraction
        List<String> dates = extractDates(input);
        if (!dates.isEmpty()) result.entitiesMap.put("DATE", String.join(", ", dates));
        String lowerInput = input.toLowerCase();
        // --- Detect help/guide/steps to create contract ---
        String[] helpPatterns = {
            "steps to create", "how to create", "show me create", "show me how to create",
            "help me to create", "list the steps to create", "guide me to create",
            // Expanded patterns:
            "how to make", "show me how to make", "show me how to set up", "show me how to generate",
            "process for contract creation", "process to create contract", "process of contract creation",
            "walk me through contract creation", "explain how to set up a contract", "instructions for making a contract",
            "need help understanding contract creation", "can you show me how to make a contract", "walk me through contract creation",
            "explain contract creation", "help with contract creation", "help creating a contract", "help me create a contract",
            "how do i create a contract", "how can i create a contract", "how to set up a contract", "how to generate a contract",
            "how to make a contract", "how to build a contract", "how to start a contract", "how to initiate a contract",
            "guide to contract creation", "guide for contract creation", "guide on contract creation",
            "instructions for contract creation", "instructions to create contract", "instructions on creating a contract",
            "contract creation guide", "contract creation help", "contract creation instructions",
            "help with making a contract", "help with generating a contract", "help with setting up a contract",
            "help with building a contract", "help with starting a contract", "help with initiating a contract",
            "make a contract", "generate a contract", "set up a contract", "create a contract"
        };
        boolean matchedHelp = false;
        for (String pattern : helpPatterns) {
            if (lowerInput.contains(pattern)) {
                matchedHelp = true;
                break;
            }
        }
        // Additional robust check: if input contains both 'contract' and any of ['how', 'process', 'guide', 'instructions', 'walk', 'explain', 'help', 'make', 'generate', 'set up', 'create']
        String[] helpKeywords = {"how", "process", "guide", "instructions", "walk", "explain", "help", "make", "generate", "set up", "create"};
        if (!matchedHelp && lowerInput.contains("contract")) {
            for (String kw : helpKeywords) {
                if (lowerInput.contains(kw)) {
                    matchedHelp = true;
                    break;
                }
            }
        }
        if (matchedHelp) {
            // Heuristic: if input contains 'make', 'generate', 'set up', treat as BOT, else USER
            String actionType = "HELP_CONTRACT_CREATE_USER";
            if (lowerInput.contains("make") || lowerInput.contains("generate") || lowerInput.contains("set up")) {
                actionType = "HELP_CONTRACT_CREATE_BOT";
            }
            result.intent = actionType;
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            result.metadata = new QueryMetadata("HELP", actionType, 0.0);
            result.entities = new ArrayList<>();
            result.displayEntities = Arrays.asList(
                "CONTRACT_NAME", "CUSTOMER_NAME", "ACCOUNT_NUMBER", "DESCRIPTION", "COMMENTS", "TITLE", "IS_PRICELIST"
            );
            result.errors = new ArrayList<>();
            return result;
        }
        // Fuzzy/abbreviation/typo support for contract creation help
        String[] fuzzyHelp = {
            "contract", "contrakt", "contrct", "ctrct", "contractcreation", "contract; creation", "creation", "mk", "make", "creat", "create", "how", "steps", "step"
        };
        for (String kw : fuzzyHelp) {
            if (lowerInput.contains(kw)) {
                // Heuristic: if input is very short or ambiguous, still treat as HELP
                String actionType = "HELP_CONTRACT_CREATE_USER";
                if (lowerInput.contains("make") || lowerInput.contains("mk") || lowerInput.contains("creat") || lowerInput.contains("create")) {
                    actionType = "HELP_CONTRACT_CREATE_BOT";
                }
                result.intent = actionType;
                result.inputTracking = new InputTrackingResult(input, input, 1.0);
                result.header = new Header();
                result.metadata = new QueryMetadata("HELP", actionType, 0.0);
                result.entities = new ArrayList<>();
                result.displayEntities = Arrays.asList(
                    "CONTRACT_NAME", "CUSTOMER_NAME", "ACCOUNT_NUMBER", "DESCRIPTION", "COMMENTS", "TITLE", "IS_PRICELIST"
                );
                result.errors = new ArrayList<>();
                return result;
            }
        }
        // --- Use RobustDataExtractor for field-value extraction ---
        Map<String, String> extractedFields = RobustDataExtractor.extractData(input);
        if (extractedFields != null && !extractedFields.isEmpty()) {
            QueryResult directResult = new QueryResult();
            directResult.inputTracking = new InputTrackingResult(input, input, 1.0);
            directResult.header = new Header();
            directResult.metadata = new QueryMetadata("CONTRACTS", "contracts_by_filter", 0.0);
            directResult.entities = new ArrayList<>();
            for (Map.Entry<String, String> entry : extractedFields.entrySet()) {
                directResult.entities.add(new EntityFilter(entry.getKey().toUpperCase(), "=", entry.getValue(), "extracted"));
            }
            // Use default columns from TableColumnConfig
            try {
                Set<String> displayCols = TableColumnConfig.getInstance().getDisplayableColumns(TableColumnConfig.TABLE_CONTRACTS);
                directResult.displayEntities = new ArrayList<>(displayCols);
            } catch (Exception e) {
                directResult.displayEntities = new ArrayList<>();
            }
            directResult.errors = new ArrayList<>();
            return directResult;
        }
        // 1. If 'created by' (past tense) is present, always treat as SEARCH_CONTRACTS
        if (lowerInput.contains("created by")) {
            String[] parts = lowerInput.split("created by");
            String name = (parts.length > 1) ? parts[1].trim() : "";
            result.intent = "SEARCH_CONTRACTS";
            // --- Begin: Populate all QueryResult fields for downstream JSON ---
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            if (!name.isEmpty()) result.header.createdBy = name;
            else if (!personEntity.isEmpty()) result.header.createdBy = personEntity;
            else if (!orgEntity.isEmpty()) result.header.createdBy = orgEntity;
            result.metadata = new QueryMetadata("CONTRACTS", "contracts_by_user", 0.0);
            result.entities = new ArrayList<>();
            if (result.header.createdBy != null && !result.header.createdBy.isEmpty()) {
                result.entities.add(new EntityFilter("CREATED_BY", "=", result.header.createdBy, "extracted"));
            }
            result.displayEntities = Arrays.asList(
                "CONTRACT_NAME", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "CREATE_DATE", "EXPIRATION_DATE", "STATUS"
            );
            result.errors = new ArrayList<>();
            // --- End: Populate all QueryResult fields ---
            return result;
        }
        // 2. Only treat 'create contract' (present tense) as CREATE_CONTRACT
        boolean hasCreate = false;
        boolean hasBy = false;
        for (int i = 0; i < tokens.length; i++) {
            String lemma = lemmas.get(i);
            if (lemma.equalsIgnoreCase("by") || lemma.equalsIgnoreCase("por")) hasBy = true;
            // Only match 'create' if not 'created'
            if (tokens[i].equalsIgnoreCase("create")) hasCreate = true;
        }
        if (hasCreate && !lowerInput.contains("created by")) {
            result.intent = "CREATE_CONTRACT";
            if (!orgEntity.isEmpty()) result.entitiesMap.put("ORGANIZATION", orgEntity);
            return result;
        }
        // 3. If 'by' and a person/org is present, treat as SEARCH_CONTRACTS
        if (hasBy && (!personEntity.isEmpty() || !orgEntity.isEmpty())) {
            result.intent = "SEARCH_CONTRACTS";
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            if (!personEntity.isEmpty()) result.header.createdBy = personEntity;
            else if (!orgEntity.isEmpty()) result.header.createdBy = orgEntity;
            result.metadata = new QueryMetadata("CONTRACTS", "contracts_by_user", 0.0);
            result.entities = new ArrayList<>();
            if (result.header.createdBy != null && !result.header.createdBy.isEmpty()) {
                result.entities.add(new EntityFilter("CREATED_BY", "=", result.header.createdBy, "extracted"));
            }
            result.displayEntities = Arrays.asList(
                "CONTRACT_NAME", "CUSTOMER_NAME", "CUSTOMER_NUMBER", "CREATE_DATE", "EXPIRATION_DATE", "STATUS"
            );
            result.errors = new ArrayList<>();
            return result;
        }
        // --- Contract, Parts, Failed Parts, Customer Info Query Handling with TableColumnConfig Fuzzy Matching ---
        TableColumnConfig tcc = TableColumnConfig.getInstance();
        // Prepare user keywords for fuzzy matching
        String[] userKeywords = lowerInput.split("[^a-zA-Z0-9]+");
        // Contract Info
        String[] contractInfoKeywords = {
            "contract", "contarct", "contrat", "contrct", "ctrct", "contractcreation", "contract; creation", "creation", "info", "information", "effective", "efective", "expiration", "experation", "expiry", "begin", "start", "customer", "name", "date", "end", "details", "brief", "show"
        };
        boolean isContractInfo = false;
        for (String kw : contractInfoKeywords) {
            if (lowerInput.contains(kw)) {
                isContractInfo = true;
                break;
            }
        }
        java.util.regex.Matcher contractNumMatcher = java.util.regex.Pattern.compile("\\b\\d{6,}\\b").matcher(lowerInput);
        String contractNumber = null;
        if (contractNumMatcher.find()) {
            contractNumber = contractNumMatcher.group();
        }
        if (isContractInfo && contractNumber != null) {
            result.intent = "CONTRACTS";
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            result.header.contractNumber = contractNumber;
            result.metadata = new QueryMetadata("CONTRACTS", "contracts_by_contractnumber", 0.0);
            result.entities = new ArrayList<>();
            // CRITICAL: Use AWARD_NUMBER for contracts table
            result.entities.add(new EntityFilter("AWARD_NUMBER", "=", contractNumber, "extracted"));
            Set<String> foundColumns = new HashSet<>();
            for (String kw : userKeywords) {
                String col = tcc.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_CONTRACTS, kw);
                if (col != null) foundColumns.add(col);
            }
            if (foundColumns.isEmpty()) {
                foundColumns.addAll(TableColumnConfig.DEFAULT_CONTRACTS_COLMS);
            }
            result.displayEntities = new ArrayList<>(foundColumns);
            result.errors = new ArrayList<>();
            return result;
        }
        // Parts Info
        String[] partsKeywords = {"part", "parts", "item", "component", "spare", "number", "no", "pn"};
        boolean isPartsInfo = false;
        for (String kw : partsKeywords) {
            if (lowerInput.contains(kw)) {
                isPartsInfo = true;
                break;
            }
        }
        java.util.regex.Matcher partNumMatcher = java.util.regex.Pattern.compile("[a-zA-Z0-9]{3,}").matcher(lowerInput);
        String partNumber = null;
        if (partNumMatcher.find()) {
            partNumber = partNumMatcher.group();
        }
        if (isPartsInfo && partNumber != null && contractNumber != null) {
            result.intent = "PARTS";
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            result.header.partNumber = partNumber;
            result.header.contractNumber = contractNumber;
            result.metadata = new QueryMetadata("PARTS", "parts_by_partnumber", 0.0);
            result.entities = new ArrayList<>();
            // CRITICAL: Use LOADED_CP_NUMBER for parts table
            result.entities.add(new EntityFilter("LOADED_CP_NUMBER", "=", contractNumber, "extracted"));
            result.entities.add(new EntityFilter("PART_NUMBER", "=", partNumber, "extracted"));
            Set<String> foundColumns = new HashSet<>();
            for (String kw : userKeywords) {
                String col = tcc.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_PARTS, kw);
                if (col != null) foundColumns.add(col);
            }
            if (foundColumns.isEmpty()) {
                foundColumns.addAll(TableColumnConfig.DEFAULT_PARTS_COLMS);
            }
            result.displayEntities = new ArrayList<>(foundColumns);
            result.errors = new ArrayList<>();
            return result;
        }
        // Failed Parts Info
        String[] failedPartsKeywords = {"failed", "error", "missing", "not loaded", "fail", "problem", "issue"};
        boolean isFailedParts = false;
        for (String kw : failedPartsKeywords) {
            if (lowerInput.contains(kw)) {
                isFailedParts = true;
                break;
            }
        }
        if (isFailedParts && partNumber != null && contractNumber != null) {
            result.intent = "FAILED_PARTS";
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            result.header.partNumber = partNumber;
            result.header.contractNumber = contractNumber;
            result.metadata = new QueryMetadata("FAILED_PARTS", "failed_parts_by_partnumber", 0.0);
            result.entities = new ArrayList<>();
            // CRITICAL: Use CONTRACT_NO for failed parts table
            result.entities.add(new EntityFilter("CONTRACT_NO", "=", contractNumber, "extracted"));
            result.entities.add(new EntityFilter("PART_NUMBER", "=", partNumber, "extracted"));
            Set<String> foundColumns = new HashSet<>();
            for (String kw : userKeywords) {
                String col = tcc.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_FAILED_PARTS, kw);
                if (col != null) foundColumns.add(col);
            }
            if (foundColumns.isEmpty()) {
                foundColumns.addAll(TableColumnConfig.DEFAULT_PARTS_FAILED_COLMS);
            }
            result.displayEntities = new ArrayList<>(foundColumns);
            result.errors = new ArrayList<>();
            return result;
        }
        // Customer Info
        String[] customerKeywords = {"customer", "client", "account", "buyer", "organization", "company", "name", "number"};
        boolean isCustomerInfo = false;
        for (String kw : customerKeywords) {
            if (lowerInput.contains(kw)) {
                isCustomerInfo = true;
                break;
            }
        }
        java.util.regex.Matcher custNumMatcher = java.util.regex.Pattern.compile("\\b\\d{7,}\\b").matcher(lowerInput);
        String customerNumber = null;
        if (custNumMatcher.find()) {
            customerNumber = custNumMatcher.group();
        }
        if (isCustomerInfo && customerNumber != null) {
            result.intent = "CUSTOMERS";
            result.inputTracking = new InputTrackingResult(input, input, 1.0);
            result.header = new Header();
            result.header.customerNumber = customerNumber;
            result.metadata = new QueryMetadata("CUSTOMERS", "customers_by_number", 0.0);
            result.entities = new ArrayList<>();
            result.entities.add(new EntityFilter("CUSTOMER_NUMBER", "=", customerNumber, "extracted"));
            Set<String> foundColumns = new HashSet<>();
            for (String kw : userKeywords) {
                String col = tcc.getColumnForSynonymFuzzy(TableColumnConfig.TABLE_CUSTOMERS, kw);
                if (col != null) foundColumns.add(col);
            }
            if (foundColumns.isEmpty()) {
                foundColumns.addAll(tcc.getColumns(TableColumnConfig.TABLE_CUSTOMERS));
            }
            result.displayEntities = new ArrayList<>(foundColumns);
            result.errors = new ArrayList<>();
            return result;
        }
        // --- Main Routing Logic ---
        // 1. ContractProcessor: 6+ digit number or 'contract'
        if (lowerInput.matches(".*\\b\\d{6,}\\b.*") || lowerInput.contains("contract")) {
            return contractProcessor.process(input, input, input);
        }
        // 2. FailedPartsProcessor: 'failed' or 'error' + 'part(s)'
        if ((lowerInput.contains("failed") || lowerInput.contains("error")) && (lowerInput.contains("parts") || lowerInput.contains("part"))) {
            return failedPartsProcessor.process(input, input, input);
        }
        // 3. PartsProcessor: 'parts' or 'part'
        if (lowerInput.contains("parts") || lowerInput.contains("part")) {
            return partsProcessor.process(input, input, input);
        }
        // 4. CustomerProcessor: 'customer'
        if (lowerInput.contains("customer")) {
            return customerProcessor.process(input, input, input);
        }
        // 5. HelpProcessor: fallback
        return helpProcessor.process(input, input, input);
    }
    
    /**
     * Preprocess input using lemmatization
     */
    private String preprocessInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return userInput;
        }
        return lemmatizer.lemmatizeTextPreserveTense(userInput);
    }
    
    /**
     * Enhanced query type detection with HELP classification
     */
    private String determineQueryType(String userInput) {
        String input = userInput.toLowerCase();
        
        // Check for failed parts queries
        if (isFailedPartsQuery(input)) {
            return "FAILED_PARTS";
        }
        
        // Check for parts queries
        if (isPartsQuery(input)) {
            return "PARTS";
        }
        
        // Check for customer queries
        if (input.contains("customer") || input.contains("account number") || input.contains("customer number") || input.contains("customer name")) {
            return "CUSTOMERS";
        }
        // Check for opportunities queries
        if (input.contains("opportunity") || input.contains("opportunities")) {
            return "OPPORTUNITIES";
        }
        // Check for help queries
        if (isHelpQuery(input)) {
            return "HELP";
        }
        
        // Default to contracts
        return "CONTRACTS";
    }
    
    /**
     * Get display entities for quick actions
     */
    private List<String> getQuickActionDisplayEntities(String actionType) {
        List<String> displayEntities = new ArrayList<>();
        
        switch (actionType) {
            case "QUICK_ACTION_RECENT_CONTRACTS":
                displayEntities.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "CREATE_DATE", "STATUS"));
                break;
            case "QUICK_ACTION_PARTS_COUNT":
                displayEntities.add("TOTAL_PARTS");
                break;
            case "QUICK_ACTION_FAILED_CONTRACTS":
                displayEntities.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "FAILED_PARTS_COUNT"));
                break;
            case "QUICK_ACTION_EXPIRING_SOON":
                displayEntities.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "EXPIRATION_DATE", "DAYS_TO_EXPIRE"));
                break;
            case "QUICK_ACTION_AWARD_REPS":
                displayEntities.addAll(Arrays.asList("AWARD_REP", "CONTRACT_COUNT"));
                break;
            case "QUICK_ACTION_HELP":
            case "QUICK_ACTION_CREATE_CONTRACT":
                // These return formatted HTML, no specific display entities needed
                break;
        }
        
        return displayEntities;
    }
    
    /**
     * Action type determination is now handled by specialized processors
     * This method is kept for backward compatibility but delegates to processors
     */
    private String determineActionType(String userInput, String queryType) {
        // This method is deprecated - action types are now determined by specialized processors
        System.out.println("WARNING: determineActionType called - should use specialized processors");
        return "contracts_by_contractnumber"; // Default fallback
    }
    
    /**
     * Entity extraction is now handled by specialized processors
     * This method is kept for backward compatibility but delegates to processors
     */
    private List<EntityFilter> extractEntities(String userInput, String queryType) {
        // This method is deprecated - entity extraction is now handled by specialized processors
        System.out.println("WARNING: extractEntities called - should use specialized processors");
        return new ArrayList<>();
    }
    
    /**
     * Enhanced display entities extraction
     */
    private List<String> extractDisplayEntities(String userInput, String queryType) {
        List<String> displayEntities = new ArrayList<>();
        String input = userInput.toLowerCase();
        
        // Contract display entities
        if ("CONTRACTS".equals(queryType) || "HELP".equals(queryType)) {
            if (containsDateKeywords(input)) {
                displayEntities.add("EFFECTIVE_DATE");
                displayEntities.add("EXPIRATION_DATE");
                displayEntities.add("CREATED_DATE");
            }
            if (containsCustomerKeywords(input)) {
                displayEntities.add("CUSTOMER_NAME");
                displayEntities.add("CUSTOMER_NUMBER");
            }
            if (containsPaymentKeywords(input)) {
                displayEntities.add("PAYMENT_TERMS");
                displayEntities.add("INCOTERMS");
            }
            if (containsStatusKeywords(input)) {
                displayEntities.add("STATUS");
            }
            // Default contract entities
            if (displayEntities.isEmpty()) {
                // Use default contract columns
                displayEntities.add("CONTRACT_NAME");
                displayEntities.add("CUSTOMER_NAME");
                displayEntities.add("EFFECTIVE_DATE");
                displayEntities.add("EXPIRATION_DATE");
                displayEntities.add("CUSTOMER_NUMBER");
                displayEntities.add("AWARD_NUMBER");
            }
        }
        
        // Parts display entities
        if ("PARTS".equals(queryType)) {
            if (containsPriceKeywords(input)) {
                displayEntities.add("PRICE");
            }
            if (containsLeadTimeKeywords(input)) {
                displayEntities.add("LEAD_TIME");
            }
            if (containsMOQKeywords(input)) {
                displayEntities.add("MOQ");
            }
            if (containsUOMKeywords(input)) {
                displayEntities.add("UOM");
            }
            if (containsStatusKeywords(input)) {
                displayEntities.add("STATUS");
            }
            // Default parts entities
            if (displayEntities.isEmpty()) {
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("PRICE");
                displayEntities.add("LEAD_TIME");
                displayEntities.add("MOQ");
                displayEntities.add("UOM");
            }
        }
        
        // Failed parts display entities
        if ("FAILED_PARTS".equals(queryType)) {
            displayEntities.add("PART_NUMBER");
            displayEntities.add("REASON");
            displayEntities.add("ERROR_DATE");
            displayEntities.add("ERROR_TYPE");
        }
        
        return displayEntities;
    }
    
    // Helper methods for query detection
    private boolean isHelpQuery(String input) {
        // Exclude "created by" queries from HELP classification
        if (isCreatedByQuery(input)) {
            return false;
        }
        
        String[] helpKeywords = {
            "how to", "how do", "steps", "process", "guide", "instructions", 
            "help", "assist", "create", "make", "generate", "build", "set up",
            "walk me through", "explain", "tell me", "show me how", "need guidance",
            "need help", "want to create", "would like to create", "can you create",
            "please create", "help me create", "assist me", "guide me"
        };
        
        for (String keyword : helpKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isContractCreationQuery(String input) {
        String[] creationKeywords = {
            "create contract", "make contract", "generate contract", "build contract",
            "set up contract", "new contract", "start contract", "initiate contract",
            "draft contract", "establish contract", "form contract", "develop contract"
        };
        
        for (String keyword : creationKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isContractCreationHelpQuery(String input) {
        String[] userHelpKeywords = {
            "how to create", "how do i create", "steps to create", "process for creating",
            "guide me", "walk me through", "explain how", "tell me how", "show me how",
            "need guidance", "need help", "want to know", "would like to know"
        };
        
        for (String keyword : userHelpKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isContractCreationBotQuery(String input) {
        String[] botKeywords = {
            "create for me", "make for me", "generate for me", "build for me",
            "set up for me", "do it for me", "can you create", "please create",
            "create contract", "make contract", "generate contract", "build contract"
        };
        
        for (String keyword : botKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isUserInitiatedCreation(String input) {
        return input.contains("how") || input.contains("steps") || input.contains("process") ||
               input.contains("guide") || input.contains("explain") || input.contains("tell me");
    }
    
    private boolean isFailedPartsQuery(String input) {
        String[] failedKeywords = {
            "failed parts", "failed part", "parts failed", "part failed",
            "error parts", "error part", "parts error", "part error",
            "failed", "errors", "failures", "problems", "issues"
        };
        
        for (String keyword : failedKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isPartsQuery(String input) {
        String[] partsKeywords = {
            "part", "parts", "lead time", "price", "cost", "moq", "uom",
            "unit of measure", "minimum order", "leadtime", "pricing"
        };
        
        for (String keyword : partsKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isContractQuery(String input) {
        String[] contractKeywords = {
            "contract", "agreement", "customer", "effective date", "expiration",
            "payment terms", "incoterms", "status", "active", "expired"
        };
        
        for (String keyword : contractKeywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Detect "created by" queries to exclude them from HELP classification
     */
    private boolean isCreatedByQuery(String input) {
        String[] createdByKeywords = {
            "created by", "createdby", "created in", "createdin", "by", "user"
        };
        
        // Check if input contains "created by" pattern
        if (input.contains("created by") || input.contains("createdby")) {
            return true;
        }
        
        // Check if input contains "by" and "created" in close proximity
        if (input.contains("by") && input.contains("created")) {
            return true;
        }
        
        return false;
    }
    
    // Entity extraction helper methods
    /**
     * Contract number extraction moved to ContractProcessor
     * This method is deprecated
     */
    private List<String> extractContractNumbers(String input) {
        System.out.println("WARNING: extractContractNumbers called - should use ContractProcessor");
        return new ArrayList<>();
    }
    
    private List<String> extractPartNumbers(String input) {
        List<String> partNumbers = new ArrayList<>();
        // Pattern for part numbers (letters + numbers)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b[A-Z]{2}\\d{5}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(input.toUpperCase());
        while (matcher.find()) {
            partNumbers.add(matcher.group());
        }
        return partNumbers;
    }
    
    private boolean containsPartNumber(String input) {
        return !extractPartNumbers(input).isEmpty();
    }
    
    /**
     * Contract number detection moved to ContractProcessor
     * This method is deprecated
     */
    private boolean containsContractNumber(String input) {
        System.out.println("WARNING: containsContractNumber called - should use ContractProcessor");
        return false;
    }
    
    private boolean containsDateKeywords(String input) {
        String[] dateKeywords = {"date", "effective", "expiration", "expiry", "created", "start", "end"};
        for (String keyword : dateKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsCustomerKeywords(String input) {
        String[] customerKeywords = {"customer", "client", "who", "name"};
        for (String keyword : customerKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsPaymentKeywords(String input) {
        String[] paymentKeywords = {"payment", "terms", "incoterms", "price"};
        for (String keyword : paymentKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsStatusKeywords(String input) {
        String[] statusKeywords = {"status", "active", "inactive", "expired"};
        for (String keyword : statusKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsPriceKeywords(String input) {
        String[] priceKeywords = {"price", "cost", "pricing", "prise"};
        for (String keyword : priceKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsLeadTimeKeywords(String input) {
        String[] leadTimeKeywords = {"lead time", "leadtime", "delivery", "time"};
        for (String keyword : leadTimeKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsMOQKeywords(String input) {
        String[] moqKeywords = {"moq", "minimum order", "minimum quantity"};
        for (String keyword : moqKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean containsUOMKeywords(String input) {
        String[] uomKeywords = {"uom", "unit", "measure", "unit of measure"};
        for (String keyword : uomKeywords) {
            if (input.contains(keyword)) return true;
        }
        return false;
    }
    
    /**
     * Query Type Enum
     */
    public enum QueryType {
        FAILED_PARTS,
        PARTS,
        CONTRACTS,
        HELP,
        ERROR
    }
    
    /**
     * Query Result Class
     */
    public static class QueryResult {
        public InputTrackingResult inputTracking;
        public Header header;
        public QueryMetadata metadata;
        public List<EntityFilter> entities;
        public List<String> displayEntities;
        public List<ValidationError> errors;
        public String intent;
        public Map<String, String> entitiesMap = new HashMap<>();
        public String clarificationPrompt;
        
        public QueryResult() {
            this.entities = new ArrayList<>();
            this.displayEntities = new ArrayList<>();
            this.errors = new ArrayList<>();
        }
    }
    
    /**
     * Input Tracking Result Class
     */
    public static class InputTrackingResult {
        public final String originalInput;
        public final String correctedInput;
        public final double correctionConfidence;
        
        public InputTrackingResult(String originalInput, String correctedInput, double correctionConfidence) {
            this.originalInput = originalInput;
            this.correctedInput = correctedInput;
            this.correctionConfidence = correctionConfidence;
        }
    }
    
    /**
     * Header Class
     */
    public static class Header {
        public String contractNumber;
        public String partNumber;
        public String customerNumber;
        public String customerName;
        public String createdBy;
        // --- Added for OpportunitiesProcessor ---
        public String opportunityNumber;
        public String opportunityName;
    }
    
    /**
     * Query Metadata Class
     */
    public static class QueryMetadata {
        public String queryType;
        public String actionType;
        public double processingTimeMs;
        
        public QueryMetadata(String queryType, String actionType, double processingTimeMs) {
            this.queryType = queryType;
            this.actionType = actionType;
            this.processingTimeMs = processingTimeMs;
        }
    }
    
    /**
     * Entity Filter Class
     */
    public static class EntityFilter {
        public final String attribute;
        public final String operation;
        public final String value;
        public final String source;
        
        public EntityFilter(String attribute, String operation, String value, String source) {
            this.attribute = attribute;
            this.operation = operation;
            this.value = value;
            this.source = source;
        }
    }
    
    /**
     * Validation Error Class
     */
    public static class ValidationError {
        public final String code;
        public final String message;
        public final String severity;
        
        public ValidationError(String code, String message, String severity) {
            this.code = code;
            this.message = message;
            this.severity = severity;
        }
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
     */
    public static String normalizeYesNo(String input) {
        if (input == null) return null;
        String norm = input.trim().toLowerCase();
        // Remove punctuation and extra whitespace
        norm = norm.replaceAll("[!?.]", "").replaceAll("\\s+", " ");
        if (YES_WORDS.contains(norm)) return "YES";
        if (NO_WORDS.contains(norm)) return "NO";
        // Try partial match for multi-word phrases
        for (String yes : YES_WORDS) {
            if (norm.equals(yes) || norm.startsWith(yes + " ") || norm.endsWith(" " + yes)) return "YES";
        }
        for (String no : NO_WORDS) {
            if (norm.equals(no) || norm.startsWith(no + " ") || norm.endsWith(" " + no)) return "NO";
        }
        return null;
    }
    // --- End YES/NO Normalization Utility ---

    // Add this helper method to extract contract columns from user input
    private List<String> extractContractColumns(String input) {
        List<String> columns = new ArrayList<>();
        String lower = input.toLowerCase();
        // Match patterns like 'show effective date, contract name for contract 100476'
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("show\\s+([a-zA-Z0-9_, ]+)\\s+for\\s+contract\\s*\\d+").matcher(lower);
        if (m.find()) {
            String columnsPart = m.group(1);
            String[] cols = columnsPart.split(",| and | or | ");
            for (String col : cols) {
                String colTrim = col.trim().toUpperCase();
                // Validate against known contract columns (hardcoded or via TableColumnConfig if accessible)
                if (colTrim.matches("EFFECTIVE_DATE|CONTRACT_NAME|CUSTOMER_NAME|CUSTOMER_NUMBER|EXPIRATION_DATE|PRICE_EXPIRATION_DATE|STATUS|PAYMENT_TERMS|INCOTERMS|CONTRACT_TYPE|AWARD_NUMBER|CREATED_DATE")) {
                    columns.add(colTrim);
                }
            }
        }
        return columns;
    }
} 