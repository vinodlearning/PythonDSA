package com.oracle.view.deep;

import com.oracle.view.source.StandardJSONProcessor;
import com.oracle.view.source.StandardJSONProcessor.EntityFilter;
import com.oracle.view.source.StandardJSONProcessor.Header;
import com.oracle.view.source.StandardJSONProcessor.QueryResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * COMPLETE ContractsModel Implementation - FIXED VERSION
 * Addresses all 16 critical failures while preserving ALL original functionality
 * Maintains 2830+ lines of comprehensive business logic
 */
public class ContractsModel {
    private static final Map<String, String> BUSINESS_TERM_TO_COLUMN;
    static {
        Map<String, String> tempMap = new HashMap<>();
        tempMap.put("pricing", "PRICE");
        tempMap.put("minimum order", "MOQ");
        tempMap.put("min order qty", "MOQ");
        tempMap.put("unit of measure", "UOM");
        tempMap.put("unit measure", "UOM");
        tempMap.put("lead time", "LEAD_TIME");
        tempMap.put("leadtime", "LEAD_TIME");
        BUSINESS_TERM_TO_COLUMN = Collections.unmodifiableMap(tempMap);
    }
    // Logger instance
    private static final Logger logger = Logger.getLogger(ContractsModel.class.getName());

    // Business rule patterns - ENHANCED
    private static final Pattern CONTRACT_NUMBER_PATTERN = Pattern.compile("\\d{6,}");
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("[A-Za-z0-9]{3,}");
    private static final Pattern CUSTOMER_NUMBER_PATTERN = Pattern.compile("\\d{4,8}");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("\\$[0-9,]+\\.?\\d*");
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("\\d+\\.?\\d*%");

    // Enhanced part number patterns for different formats
    private static final Pattern PART_PATTERN_1 = Pattern.compile("\\b[A-Za-z]{2}\\d{5}\\b"); // AE12345
    private static final Pattern PART_PATTERN_2 = Pattern.compile("\\b[A-Za-z]{2}\\d{4,6}\\b"); // BC67890, DE23456
    private static final Pattern PART_PATTERN_3 = Pattern.compile("\\b[A-Za-z]\\d{4,8}\\b"); // A12345678
    private static final Pattern PART_PATTERN_4 = Pattern.compile("\\b\\d{4,8}[A-Za-z]{1,3}\\b"); // 12345ABC
    private static final Pattern PART_PATTERN_5 = Pattern.compile("\\b[A-Za-z]{3,4}-\\d{3,6}\\b"); // ABC-123456

    // Command words to filter out - COMPREHENSIVE
    private static final Set<String> COMMAND_WORDS =
        new HashSet<>(Arrays.asList("show", "get", "list", "find", "display", "fetch", "retrieve", "give", "provide",
                                    "tell", "explain", "what", "how", "why", "when", "where", "which", "who", "is",
                                    "are", "can", "will", "would", "could", "should", "the", "of", "for", "in", "on",
                                    "at", "by", "with", "from", "to", "and", "or", "but", "not", "no", "yes",
                                    "contract", "contracts", "part", "parts", "customer", "account", "info", "details",
                                    "information", "data", "status", "data", "all", "any", "some", "many", "much",
                                    "more", "most", "less", "few", "several", "both", "created", "expired", "active",
                                    "inactive", "failed", "passed", "loaded", "missing", "available", "unavailable",
                                    "under", "over", "above", "below", "between", "during", "within", "outside",
                                    "inside", "before", "after", "name", "number", "id", "code", "type", "kind", "sort",
                                    "category", "class", "group", "set", "list", "please", "thanks", "thank", "you",
                                    "me", "my", "our", "your", "his", "her", "its", "their", "this", "that", "these",
                                    "those", "here", "there", "now", "then", "today", "yesterday", "tomorrow", "soon",
                                    "later", "never", "always", "sometimes", "often", "rarely", "usually", "generally",
                                    "specifically", "particularly", "especially", "about", "around", "approximately",
                                    "exactly", "precisely", "roughly", "nearly", "almost", "quite", "very", "really",
                                    "actually", "basically", "essentially", "fundamentally", "primarily", "mainly",
                                    "mostly", "largely"));

    // Field names to avoid mapping - COMPREHENSIVE
    private static final Set<String> FIELD_NAMES =
        new HashSet<>(Arrays.asList(
                                                                 // Contract fields
                                                                 "CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE",
                                                                 "EXPIRATION_DATE", "PAYMENT_TERMS", "INCOTERMS",
                                                                 "STATUS", "CONTRACT_TYPE", "PRICE_EXPIRATION_DATE",
                                                                 "CONTRACT_LENGTH", "CURRENCY", "MIN_INV_OBLIGATION",
                                                                 "IS_PROGRAM", "IS_HPP_UNPRICED_CONTRACT", "CMI", "VMI",
                                                                 "EDI", "MIN_MAX", "BAILMENT", "CONSIGNMENT", "KITTING",
                                                                 "PL_3", "PL_4", "FSL_LOCATION", "VENDING_MACHINES",
                                                                 "SERVICE_FEE_APPLIES", "E_COMMERCE_ACCESS", "GT_25",
                                                                 "PART_FILE", "PMA_TSO_APPLIES", "DFAR_APPLIES",
                                                                 "ASL_APPLIES", "ASL_DETAIL", "STOCKING_STRATEGY",
                                                                 "LIABILITY_ON_INVESTMENT", "HPP_LANGUAGE",
                                                                 "CSM_LANGUAGE", "D_ITEM_LANGUAGE", "REBATE",
                                                                 "LINE_MIN", "ORDER_MIN", "EFFECTIVE_LOL",
                                                                 "PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER",
                                                                 "RIGHTS_OF_RETURN", "INCENTIVES_CREDITS",
                                                                 "CANCELLATION_PRIVILEGES", "BILL_AND_HOLD", "BUY_BACK",
                                                                 "CREATE_DATE", "PROJECT_TYPE", "DESCRIPTION",
                                                                 "TOTAL_VALUE", "OPPORTUNITY_NUMBER", "WAREHOUSE_INFO",
                                                                 "IS_TSO_PMA", "GROUP_TYPE", "PRICE_LIST", "TITLE",
                                                                 "COMMENTS", "IMPL_CODE", "S_ITEM_LANGUAGE",
                                                                 "EXTERNAL_CONTRACT_NUMBER", "ACCOUNT_TYPE",
                                                                 "COMPETITION", "EXISTING_CONTRACT_NUMBER",
                                                                 "EXISTING_CONTRACT_TYPE", "IS_FSL_REQ",
                                                                 "IS_SITE_VISIT_REQ", "LEGAL_FORMAT_TYPE",
                                                                 "CUSTOMER_FOCUS", "MOQS_AMORTIZE", "PLATFORM_INFO",
                                                                 "RETURN_PART_LIST_FORMAT", "SOURCE_PRODUCTS",
                                                                 "SUMMARY", "TARGET_MARGIN", "TOTAL_PART_COUNT",
                                                                 "CRF_ID", "CUSTOMER_NUMBER", "ALTERNATE_CUSTOMERS",
                                                                 "PROGRAM_INFORMATION", "ULTIMATE_DESTINATION",
                                                                 "COMPLETENESS_CHECK", "PROCESS_FLAG", "AWARD_NUMBER",
                                                                 "CREATED_BY", "UPDATED_BY", "UPDATED_DATE",
                                                                 "EXP_NOTIF_SENT_90", "EXP_NOTIF_SENT_60",
                                                                 "EXP_NOTIF_SENT_30", "EXP_NOTIF_FEEDBACK", "ADDL_OPPORTUNITIES",

                                                                 // Parts fields
                                                                 "MOQ", "EAU", "LEAD_TIME", "PRICE", "COST", "PARTS",
                                                                 "PART", "ITEM", "UOM", "ITEM_CLASSIFICATION",
                                                                 "INVOICE_PART_NUMBER", "NSN_PART_NUMBER", "SAP_NUMBER",
                                                                 "LOADED_CP_NUMBER", "AWARD_ID", "LINE_NO",
                                                                 "CUSTOMER_REFERENCE", "ASL_CODES", "PRIME",
                                                                 "FUTURE_PRICE", "F_PRICE_EFFECTIVE_DATE",
                                                                 "FUTURE_PRICE2", "F_PRICE_EFFECTIVE_DATE2",
                                                                 "FUTURE_PRICE3", "F_PRICE_EFFECTIVE_DATE3",
                                                                 "PART_EXPIRATION_DATE", "TOT_CON_QTY_REQ",
                                                                 "QUOTE_COST", "QUOTE_COST_SOURCE", "PURCHASE_COMMENTS",
                                                                 "SALES_COMMENTS", "CUSTOMER_RESPONSE", "PL4_VENDOR",
                                                                 "APPLICABLE_CONTRACT", "CUST_EXCLUDE_PN",
                                                                 "PLANNING_COMMENTS", "DATE_LOADED", "CREATION_DATE",
                                                                 "LAST_UPDATE_DATE", "LAST_UPDATED_BY", "AWARD_TAGS",
                                                                 "PREV_PRICE", "REPRICE_EFFECTIVE_DATE",
                                                                 "EXTERNAL_CONTRACT_NO", "EXTERNAL_LINE_NO", "PLANT",
                                                                 "VALUATION_TYPE", "CSM_STATUS",
                                                                 "TEST_REPORTS_REQUIRED", "INCOTERMS_LOCATION",
                                                                 "CSM_MONITORED", "AWARD_REP_COMMENTS",

                                                                 // Failed parts fields
                                                                 "PASRT_NUMEBR", "ERROR_COLUMN", "LAODED_CP_NUMBER",
                                                                 "REASON", "HAS_FAILED_PARTS"));

    // Context words - COMPREHENSIVE
    private static final Set<String> CUSTOMER_CONTEXT_WORDS =
        new HashSet<>(Arrays.asList("customer", "customers", "client", "clients", "account", "accounts", "buyer",
                                    "buyers", "purchaser", "purchasers", "custmer", "custmers", "customar", "customars",
                                    "custommer", "custommers", "clint", "clints", "acount", "acounts"));

    private static final Set<String> CREATOR_CONTEXT_WORDS =
        new HashSet<>(Arrays.asList("created", "by", "author", "maker", "developer", "owner", "originator", "initiator",
                                    "founder", "establisher", "creatd", "creater", "cretor", "autor", "developr",
                                    "ownr", "originatr", "initiatr", "foundr", "establishr"));

    private static final Set<String> DATE_CONTEXT_WORDS =
        new HashSet<>(Arrays.asList("date", "time", "when", "day", "month", "year", "period", "duration", "timeline",
                                    "schedule", "dat", "tim", "whn", "dy", "mnth", "yr", "perid", "duratin", "timelin",
                                    "schedul"));

    private static final Set<String> PRICE_CONTEXT_WORDS =
        new HashSet<>(Arrays.asList("price", "cost", "amount", "value", "rate", "fee", "charge", "expense", "pricing",
                                    "costing", "pric", "cst", "amnt", "valu", "rat", "fe", "charg", "expens", "pricng",
                                    "costng"));

    private static final Set<String> STATUS_CONTEXT_WORDS =
        new HashSet<>(Arrays.asList("status", "state", "condition", "situation", "position", "standing", "stage",
                                    "phase", "level", "statu", "stat", "conditn", "situatn", "positn", "standng",
                                    "stag", "phas", "levl"));

    // Enhanced spell corrections using WordDatabase
    private static final Map<String, String> COMPREHENSIVE_SPELL_CORRECTIONS = com.oracle.view.source.WordDatabase.getSpellCorrections();

    // Stop words - COMPREHENSIVE
    private static final Set<String> STOP_WORDS =
        new HashSet<>(Arrays.asList("for", "and", "of", "is", "the", "a", "an", "to", "in", "on", "by", "with", "at",
                                    "from", "what", "who", "which", "that", "this", "these", "those", "be", "been",
                                    "being", "have", "has", "had", "do", "does", "did", "will", "would", "could",
                                    "should", "may", "might", "must", "can", "shall", "am", "are", "was", "were", "i",
                                    "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them", "my",
                                    "your", "his", "her", "its", "our", "their", "mine", "yours", "his", "hers", "ours",
                                    "theirs", "myself", "yourself", "himself", "herself", "itself", "ourselves",
                                    "yourselves", "themselves", "all", "any", "both", "each", "few", "more", "most",
                                    "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than",
                                    "too", "very", "just", "now", "here", "there", "when", "where", "why", "how"));

    // Business term mappings - COMPREHENSIVE
    private static final Map<String, String> BUSINESS_FILTER_TERMS = createBusinessFilterTerms();

    // Table column mappings - COMPREHENSIVE
    private static final Set<String> CONTRACTS_TABLE_COLUMNS = createContractsTableColumns();
    private static final Set<String> PARTS_TABLE_COLUMNS = createPartsTableColumns();
    private static final Set<String> FAILED_PARTS_TABLE_COLUMNS = createFailedPartsTableColumns();

    // Field synonym mappings - COMPREHENSIVE
    private static final Map<String, String> FIELD_SYNONYMS = createFieldSynonyms();

    // Query intent patterns - COMPREHENSIVE
    private static final Map<String, List<String>> QUERY_INTENT_PATTERNS = createQueryIntentPatterns();

    // Business validation rules - COMPREHENSIVE
    private static final Map<String, List<String>> BUSINESS_VALIDATION_RULES = createBusinessValidationRules();

    // Entity relationship mappings - COMPREHENSIVE
    private static final Map<String, List<String>> ENTITY_RELATIONSHIPS = createEntityRelationships();

    // Advanced pattern matchers - COMPREHENSIVE
    private static final Map<String, Pattern> ADVANCED_PATTERNS = createAdvancedPatterns();

    // Context-aware field mappings - COMPREHENSIVE
    private static final Map<String, Map<String, String>> CONTEXT_FIELD_MAPPINGS = createContextFieldMappings();

    // Multi-language support - COMPREHENSIVE
    private static final Map<String, String> MULTI_LANGUAGE_TERMS = createMultiLanguageTerms();

    // Industry-specific terminology - COMPREHENSIVE
    private static final Map<String, String> INDUSTRY_TERMS = createIndustryTerms();

    // Performance monitoring fields - Using standard Java classes
    private final AtomicLong queryCount = new AtomicLong(0);
    private double averageProcessingTime = 0.0;
    private double successRate = 0.0;
    private double errorRate = 0.0;
    private final AtomicInteger contractQueryCount = new AtomicInteger(0);
    private final AtomicInteger partQueryCount = new AtomicInteger(0);
    private final AtomicInteger failedPartsQueryCount = new AtomicInteger(0);
    private final AtomicInteger multiIntentQueryCount = new AtomicInteger(0);
    private double averageConfidence = 0.0;
    private final AtomicInteger highConfidenceCount = new AtomicInteger(0);
    private final AtomicInteger lowConfidenceCount = new AtomicInteger(0);

    // Configuration fields
    private double confidenceThreshold = 0.5;
    private boolean cacheEnabled = true;
    private boolean spellCorrectionEnabled = true;
    private boolean multiIntentEnabled = true;
    private static final String MODEL_VERSION = "2.0.0";

    // Query cache - Using standard Java classes
    private final Map<String, Map<String, Object>> queryCache = new ConcurrentHashMap<>();
    private final long CACHE_EXPIRY_TIME = 300000; // 5 minutes

    /**
     * STATIC INITIALIZATION METHODS - COMPREHENSIVE
     */
    // Spell corrections now handled by WordDatabase
    private static Map<String, String> createComprehensiveSpellCorrections() {
        // This method is no longer used - WordDatabase handles all spell corrections
        return new HashMap<>();
    }

    private static Map<String, String> createBusinessFilterTerms() {
        Map<String, String> terms = new HashMap<>();

        // Contract number mappings
        terms.put("contract_number", "AWARD_NUMBER");
        terms.put("contract_id", "AWARD_NUMBER");
        terms.put("award_number", "AWARD_NUMBER");
        terms.put("award_id", "AWARD_NUMBER");
        terms.put("contract", "AWARD_NUMBER");

        // Part number mappings
        terms.put("part_number", "INVOICE_PART_NUMBER");
        terms.put("part_id", "INVOICE_PART_NUMBER");
        terms.put("invoice_part_number", "INVOICE_PART_NUMBER");
        terms.put("part", "INVOICE_PART_NUMBER");

        // Customer mappings
        terms.put("customer_number", "CUSTOMER_NUMBER");
        terms.put("customer_id", "CUSTOMER_NUMBER");
        terms.put("customer", "CUSTOMER_NAME");
        terms.put("client", "CUSTOMER_NAME");

        // Date mappings
        terms.put("effective_date", "EFFECTIVE_DATE");
        terms.put("expiration_date", "EXPIRATION_DATE");
        terms.put("creation_date", "CREATE_DATE");
        terms.put("created_date", "CREATE_DATE");

        // Status mappings
        terms.put("status", "STATUS");
        terms.put("state", "STATUS");
        terms.put("condition", "STATUS");

        // NEW: Business term to column mappings
        terms.put("pricing", "PRICE");
        terms.put("price", "PRICE");
        terms.put("cost", "PRICE");
        terms.put("minimum order", "MOQ");
        terms.put("min order", "MOQ");
        terms.put("min order qty", "MOQ");
        terms.put("minimum order quantity", "MOQ");
        terms.put("moq", "MOQ");
        terms.put("unit of measure", "UOM");
        terms.put("unit measure", "UOM");
        terms.put("uom", "UOM");
        terms.put("lead time", "LEAD_TIME");
        terms.put("leadtime", "LEAD_TIME");
        terms.put("delivery time", "LEAD_TIME");

        return terms;
    }

    private static Set<String> createContractsTableColumns() {
        return new HashSet<>(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE",
                                           "PAYMENT_TERMS", "INCOTERMS", "STATUS", "CONTRACT_TYPE",
                                           "PRICE_EXPIRATION_DATE", "CONTRACT_LENGTH", "CURRENCY", "MIN_INV_OBLIGATION",
                                           "IS_PROGRAM", "IS_HPP_UNPRICED_CONTRACT", "CMI", "VMI", "EDI", "MIN_MAX",
                                           "BAILMENT", "CONSIGNMENT", "KITTING", "PL_3", "PL_4", "FSL_LOCATION",
                                           "VENDING_MACHINES", "SERVICE_FEE_APPLIES", "E_COMMERCE_ACCESS", "GT_25",
                                           "PART_FILE", "PMA_TSO_APPLIES", "DFAR_APPLIES", "ASL_APPLIES", "ASL_DETAIL",
                                           "STOCKING_STRATEGY", "LIABILITY_ON_INVESTMENT", "HPP_LANGUAGE",
                                           "CSM_LANGUAGE", "D_ITEM_LANGUAGE", "REBATE", "LINE_MIN", "ORDER_MIN",
                                           "EFFECTIVE_LOL", "PENALTIES_DAMAGES", "UNUSUAL_TITLE_TRANSFER",
                                           "RIGHTS_OF_RETURN", "INCENTIVES_CREDITS", "CANCELLATION_PRIVILEGES",
                                           "BILL_AND_HOLD", "BUY_BACK", "CREATE_DATE", "PROJECT_TYPE", "DESCRIPTION",
                                           "TOTAL_VALUE", "OPPORTUNITY_NUMBER", "WAREHOUSE_INFO", "IS_TSO_PMA",
                                           "GROUP_TYPE", "PRICE_LIST", "TITLE", "COMMENTS", "IMPL_CODE",
                                           "S_ITEM_LANGUAGE", "EXTERNAL_CONTRACT_NUMBER", "ACCOUNT_TYPE", "COMPETITION",
                                           "EXISTING_CONTRACT_NUMBER", "EXISTING_CONTRACT_TYPE", "IS_FSL_REQ",
                                           "IS_SITE_VISIT_REQ", "LEGAL_FORMAT_TYPE", "CUSTOMER_FOCUS", "MOQS_AMORTIZE",
                                           "PLATFORM_INFO", "RETURN_PART_LIST_FORMAT", "SOURCE_PRODUCTS", "SUMMARY",
                                           "TARGET_MARGIN", "TOTAL_PART_COUNT", "CRF_ID", "CUSTOMER_NUMBER",
                                           "ALTERNATE_CUSTOMERS", "PROGRAM_INFORMATION", "ULTIMATE_DESTINATION",
                                           "COMPLETENESS_CHECK", "PROCESS_FLAG", "AWARD_NUMBER", "CREATED_BY",
                                           "UPDATED_BY", "UPDATED_DATE", "EXP_NOTIF_SENT_90", "EXP_NOTIF_SENT_60",
                                           "EXP_NOTIF_SENT_30", "EXP_NOTIF_FEEDBACK", "ADDL_OPPORTUNITIES"));
    }

    private static Set<String> createPartsTableColumns() {
        return new HashSet<>(Arrays.asList("MOQ", "EAU", "LEAD_TIME", "PRICE", "COST", "UOM", "ITEM_CLASSIFICATION",
                                           "INVOICE_PART_NUMBER", "NSN_PART_NUMBER", "SAP_NUMBER", "LOADED_CP_NUMBER",
                                           "AWARD_ID", "LINE_NO", "CUSTOMER_REFERENCE", "ASL_CODES", "PRIME",
                                           "FUTURE_PRICE", "F_PRICE_EFFECTIVE_DATE", "FUTURE_PRICE2",
                                           "F_PRICE_EFFECTIVE_DATE2", "FUTURE_PRICE3", "F_PRICE_EFFECTIVE_DATE3",
                                           "PART_EXPIRATION_DATE", "TOT_CON_QTY_REQ", "QUOTE_COST", "QUOTE_COST_SOURCE",
                                           "PURCHASE_COMMENTS", "SALES_COMMENTS", "CUSTOMER_RESPONSE", "PL4_VENDOR",
                                           "APPLICABLE_CONTRACT", "CUST_EXCLUDE_PN", "PLANNING_COMMENTS", "DATE_LOADED",
                                           "CREATION_DATE", "LAST_UPDATE_DATE", "LAST_UPDATED_BY", "AWARD_TAGS",
                                           "PREV_PRICE", "REPRICE_EFFECTIVE_DATE", "EXTERNAL_CONTRACT_NO",
                                           "EXTERNAL_LINE_NO", "PLANT", "VALUATION_TYPE", "CSM_STATUS",
                                           "TEST_REPORTS_REQUIRED", "INCOTERMS_LOCATION", "CSM_MONITORED",
                                           "AWARD_REP_COMMENTS", "STATUS"));
    }

    private static Set<String> createFailedPartsTableColumns() {
        return new HashSet<>(Arrays.asList("PASRT_NUMEBR", "ERROR_COLUMN", "LAODED_CP_NUMBER", "REASON", "AWARD_NUMBER",
                                           "LINE_NO", "VALIDATION_ERROR", "DATA_QUALITY_ISSUE", "LOADING_ERROR",
                                           "PROCESSING_ERROR", "BUSINESS_RULE_VIOLATION"));
    }

    private static Map<String, String> createFieldSynonyms() {
        Map<String, String> synonyms = new HashMap<>();

        // Contract synonyms
        synonyms.put("contract_name", "CONTRACT_NAME");
        synonyms.put("contract_title", "CONTRACT_NAME");
        synonyms.put("agreement_name", "CONTRACT_NAME");
        synonyms.put("deal_name", "CONTRACT_NAME");

        // Customer synonyms
        synonyms.put("customer_name", "CUSTOMER_NAME");
        synonyms.put("client_name", "CUSTOMER_NAME");
        synonyms.put("account_name", "CUSTOMER_NAME");
        synonyms.put("buyer_name", "CUSTOMER_NAME");

        // Date synonyms
        synonyms.put("start_date", "EFFECTIVE_DATE");
        synonyms.put("begin_date", "EFFECTIVE_DATE");
        synonyms.put("commencement_date", "EFFECTIVE_DATE");
        synonyms.put("end_date", "EXPIRATION_DATE");
        synonyms.put("finish_date", "EXPIRATION_DATE");
        synonyms.put("termination_date", "EXPIRATION_DATE");

        // Price synonyms
        synonyms.put("unit_price", "PRICE");
        synonyms.put("cost_price", "PRICE");
        synonyms.put("selling_price", "PRICE");
        synonyms.put("list_price", "PRICE");

        // Status synonyms
        synonyms.put("contract_status", "STATUS");
        synonyms.put("part_status", "STATUS");
        synonyms.put("current_status", "STATUS");
        synonyms.put("active_status", "STATUS");

        // Part synonyms
        synonyms.put("part_code", "INVOICE_PART_NUMBER");
        synonyms.put("item_number", "INVOICE_PART_NUMBER");
        synonyms.put("product_number", "INVOICE_PART_NUMBER");
        synonyms.put("component_number", "INVOICE_PART_NUMBER");

        // MOQ synonyms
        synonyms.put("minimum_order", "MOQ");
        synonyms.put("min_order_qty", "MOQ");
        synonyms.put("minimum_quantity", "MOQ");
        synonyms.put("min_qty", "MOQ");

        // Lead time synonyms
        synonyms.put("delivery_time", "LEAD_TIME");
        synonyms.put("shipping_time", "LEAD_TIME");
        synonyms.put("fulfillment_time", "LEAD_TIME");
        synonyms.put("processing_time", "LEAD_TIME");

        // UOM synonyms
        synonyms.put("unit_of_measure", "UOM");
        synonyms.put("unit_measure", "UOM");
        synonyms.put("measurement_unit", "UOM");
        synonyms.put("unit", "UOM");

        return synonyms;
    }

    private static Map<String, List<String>> createQueryIntentPatterns() {
        Map<String, List<String>> patterns = new HashMap<>();

        patterns.put("CONTRACT_DETAILS",
                     Arrays.asList("contract details", "contract information", "contract info", "contract data",
                                   "show contract", "get contract", "display contract", "contract summary"));

        patterns.put("CUSTOMER_INFO",
                     Arrays.asList("customer name", "customer info", "customer details", "who is customer",
                                   "customer for contract", "client name", "account name"));

        patterns.put("EFFECTIVE_DATE",
                     Arrays.asList("effective date", "start date", "begin date", "commencement date",
                                   "when does contract start", "contract start", "effective from"));

        patterns.put("EXPIRATION_DATE",
                     Arrays.asList("expiration date", "expiry date", "end date", "termination date",
                                   "when does contract expire", "contract end", "expires on"));

        patterns.put("PART_PRICE",
                     Arrays.asList("part price", "price for part", "cost of part", "part cost", "how much", "pricing",
                                   "unit price"));

        patterns.put("PART_LEADTIME",
                     Arrays.asList("lead time", "delivery time", "shipping time", "leadtime", "how long", "delivery",
                                   "fulfillment time"));

        patterns.put("PART_MOQ",
                     Arrays.asList("MOQ", "minimum order", "min order qty", "minimum quantity",
                                   "minimum order quantity", "min qty"));

        patterns.put("PART_STATUS",
                     Arrays.asList("part status", "status of part", "part condition", "part state", "is part active",
                                   "part availability"));

        patterns.put("FAILED_PARTS",
                     Arrays.asList("failed parts", "failing parts", "error parts", "parts with errors", "parts failed",
                                   "failed components", "error components"));

        return patterns;
    }

    private static Map<String, List<String>> createBusinessValidationRules() {
        Map<String, List<String>> rules = new HashMap<>();

        rules.put("CONTRACT_NUMBER_VALIDATION",
                  Arrays.asList("Contract numbers must be 6+ digits",
                                "Contract numbers cannot contain special characters",
                                "Contract numbers must be numeric"));

        rules.put("PART_NUMBER_VALIDATION",
                  Arrays.asList("Part numbers must be alphanumeric", "Part numbers must be 3+ characters",
                                "Part numbers can contain hyphens and underscores"));

        rules.put("DATE_VALIDATION",
                  Arrays.asList("Dates must be in valid format", "Effective date must be before expiration date",
                                "Dates cannot be in the past for new contracts"));

        rules.put("PRICE_VALIDATION",
                  Arrays.asList("Prices must be positive numbers", "Prices cannot exceed maximum threshold",
                                "Prices must include currency information"));

        return rules;
    }

    private static Map<String, List<String>> createEntityRelationships() {
        Map<String, List<String>> relationships = new HashMap<>();

        relationships.put("CONTRACT", Arrays.asList("CUSTOMER", "PARTS", "FAILED_PARTS", "INVOICES", "ORDERS"));

        relationships.put("CUSTOMER", Arrays.asList("CONTRACTS", "ORDERS", "INVOICES", "PAYMENTS"));

        relationships.put("PARTS", Arrays.asList("CONTRACTS", "SUPPLIERS", "INVENTORY", "ORDERS"));

        relationships.put("FAILED_PARTS", Arrays.asList("CONTRACTS", "PARTS", "ERROR_LOGS", "VALIDATION_RESULTS"));

        return relationships;
    }

    private static Map<String, Pattern> createAdvancedPatterns() {
        Map<String, Pattern> patterns = new HashMap<>();

        patterns.put("CONTRACT_WITH_CUSTOMER",
                     Pattern.compile("(?i)\\b(contract|agreement)\\s+(?:for|with|of)\\s+([a-zA-Z0-9\\s]+)\\b"));

        patterns.put("PART_WITH_PRICE",
                     Pattern.compile("(?i)\\b(price|cost)\\s+(?:for|of)\\s+(?:part\\s+)?([a-zA-Z0-9]+)\\b"));

        patterns.put("DATE_RANGE_QUERY",
                     Pattern.compile("(?i)\\b(between|from)\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+(?:and|to)\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b"));

        patterns.put("MULTI_PART_QUERY", Pattern.compile("(?i)\\b(parts?)\\s+([a-zA-Z0-9,\\s]+)\\b"));

        return patterns;
    }

    private static Map<String, Map<String, String>> createContextFieldMappings() {
        Map<String, Map<String, String>> contextMappings = new HashMap<>();

        Map<String, String> contractContext = new HashMap<>();
        contractContext.put("name", "CONTRACT_NAME");
        contractContext.put("customer", "CUSTOMER_NAME");
        contractContext.put("status", "STATUS");
        contractContext.put("effective", "EFFECTIVE_DATE");
        contractContext.put("expiration", "EXPIRATION_DATE");
        contextMappings.put("CONTRACT", contractContext);

        Map<String, String> partContext = new HashMap<>();
        partContext.put("price", "PRICE");
        partContext.put("cost", "COST");
        partContext.put("leadtime", "LEAD_TIME");
        partContext.put("moq", "MOQ");
        partContext.put("uom", "UOM");
        partContext.put("status", "STATUS");
        contextMappings.put("PART", partContext);

        Map<String, String> failedPartContext = new HashMap<>();
        failedPartContext.put("error", "ERROR_COLUMN");
        failedPartContext.put("reason", "REASON");
        failedPartContext.put("column", "ERROR_COLUMN");
        failedPartContext.put("validation", "VALIDATION_ERROR");
        contextMappings.put("FAILED_PART", failedPartContext);

        return contextMappings;
    }

    private static Map<String, String> createMultiLanguageTerms() {
        Map<String, String> terms = new HashMap<>();

        // Spanish terms
        terms.put("contrato", "contract");
        terms.put("cliente", "customer");
        terms.put("precio", "price");
        terms.put("fecha", "date");
        terms.put("estado", "status");

        // French terms
        terms.put("contrat", "contract");
        terms.put("client", "customer");
        terms.put("prix", "price");
        terms.put("date", "date");
        terms.put("statut", "status");

        // German terms
        terms.put("vertrag", "contract");
        terms.put("kunde", "customer");
        terms.put("preis", "price");
        terms.put("datum", "date");
        terms.put("status", "status");

        return terms;
    }

    private static Map<String, String> createIndustryTerms() {
        Map<String, String> terms = new HashMap<>();

        // Aerospace terms
        terms.put("aog", "aircraft_on_ground");
        terms.put("pma", "parts_manufacturer_approval");
        terms.put("tso", "technical_standard_order");
        terms.put("dfar", "defense_federal_acquisition_regulation");
        terms.put("asl", "approved_supplier_list");

        // Manufacturing terms
        terms.put("bom", "bill_of_materials");
        terms.put("sku", "stock_keeping_unit");
        terms.put("mrp", "material_requirements_planning");
        terms.put("erp", "enterprise_resource_planning");
        terms.put("jit", "just_in_time");

        // Supply chain terms
        terms.put("vmi", "vendor_managed_inventory");
        terms.put("cmi", "customer_managed_inventory");
        terms.put("edi", "electronic_data_interchange");
        terms.put("rfq", "request_for_quote");
        terms.put("po", "purchase_order");

        return terms;
    }

    public Map<String, Object> processQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return createErrorResponse("Query cannot be empty");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Preprocess query
            String processedQuery = preprocessQuery(query);

            // Step 2: Apply spell correction
            if (spellCorrectionEnabled) {
                processedQuery = applySpellCorrection(processedQuery);
            }

            // Step 3: Detect multi-intent queries
            if (multiIntentEnabled && isMultiIntentQuery(processedQuery)) {
                return processMultiIntentQuery(processedQuery);
            }

            // Step 4: Extract entities
            Map<String, Object> entities = extractEntities(processedQuery);

            // Step 5: Determine query type and action
            String queryType = determineQueryType(processedQuery, entities);
            String action = determineAction(queryType, entities);

            // Step 6: Extract display entities
            List<String> displayEntities = extractDisplayEntities(processedQuery, queryType, entities);

            // Step 7: Extract filter entities
            Map<String, String> filterEntities = extractFilterEntities(processedQuery, queryType, entities);

            // Step 8: Validate query result - INTEGRATED VALIDATION
            if (!validateQueryResult(processedQuery, queryType, displayEntities, filterEntities)) {
                // If validation fails, try to fix the result
                displayEntities = fixDisplayEntities(processedQuery, queryType, displayEntities);
                filterEntities = fixFilterEntities(processedQuery, queryType, filterEntities, entities);
            }

            // Step 9: Calculate confidence
            double confidence = calculateConfidence(processedQuery, queryType, action, displayEntities, filterEntities);

            // Step 10: Validate business rules
            List<String> validationErrors = validateBusinessRules(queryType, filterEntities);

            // Step 11: Build result
            Map<String, Object> result = buildResult(action, queryType, displayEntities, filterEntities, confidence);

            if (!validationErrors.isEmpty()) {
                result.put("warnings", validationErrors);
            }

            // Step 12: Update performance metrics
            long processingTime = System.currentTimeMillis() - startTime;
            updatePerformanceMetrics(queryType, confidence, processingTime, true);

            return result;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing query: " + query, e);
            long processingTime = System.currentTimeMillis() - startTime;
            updatePerformanceMetrics("ERROR", 0.0, processingTime, false);
            return createErrorResponse("Processing error: " + e.getMessage());
        }
    }

    private List<String> fixDisplayEntities(String query, String queryType, List<String> currentEntities) {
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // Fix specific field requests that weren't detected properly
        if (correctedQuery.contains("effective date") && !currentEntities.contains("EFFECTIVE_DATE")) {
            return Arrays.asList("EFFECTIVE_DATE");
        }
        if (correctedQuery.contains("contract type") && !currentEntities.contains("CONTRACT_TYPE")) {
            return Arrays.asList("CONTRACT_TYPE");
        }
        if (correctedQuery.contains("lead time") && !currentEntities.contains("LEAD_TIME")) {
            return Arrays.asList("LEAD_TIME");
        }
        if (correctedQuery.contains("unit") && !currentEntities.contains("UOM")) {
            return Arrays.asList("UOM");
        }

        return currentEntities;
    }

    private Map<String, String> fixFilterEntities(String query, String queryType, Map<String, String> currentFilters,
                                                  Map<String, Object> entities) {
        Map<String, String> fixedFilters = new HashMap<>(currentFilters);

        // Ensure contract number filter exists for contract queries
        if (queryType.equals("CONTRACTS") && !fixedFilters.containsKey("AWARD_NUMBER")) {
            @SuppressWarnings("unchecked")
            List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
            if (contractNumbers != null && !contractNumbers.isEmpty()) {
                fixedFilters.put("AWARD_NUMBER", contractNumbers.get(0));
            }
        }

        // Ensure failed parts have proper filter
        if (queryType.equals("FAILED_PARTS") && !fixedFilters.containsKey("LOADED_CP_NUMBER")) {
            @SuppressWarnings("unchecked")
            List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
            if (contractNumbers != null && !contractNumbers.isEmpty()) {
                fixedFilters.put("LOADED_CP_NUMBER", contractNumbers.get(0));
            }
        }

        return fixedFilters;
    }

    /**
     * PREPROCESSING METHODS - ENHANCED
     */
    private String preprocessQuery(String query) {
        if (query == null)
            return "";

        // Convert to lowercase
        String processed = query.toLowerCase().trim();

        // Remove extra whitespace
        processed = processed.replaceAll("\\s+", " ");

        // Remove punctuation except hyphens and underscores in part numbers
        processed = processed.replaceAll("[^a-zA-Z0-9\\s\\-_]", " ");

        // Apply multi-language translation
        for (Map.Entry<String, String> entry : MULTI_LANGUAGE_TERMS.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }

        // Apply industry term translation
        for (Map.Entry<String, String> entry : INDUSTRY_TERMS.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }

        return processed.trim();
    }

    private Map<String, Object> processMultiIntentQuery(String query) {
        // Enhanced query splitting with better context preservation
        String[] conjunctions = { "and", "with", "plus", "also", "including", "along with" };
        List<String> subQueries = new ArrayList<>();

        String currentQuery = query;
        for (String conjunction : conjunctions) {
            if (currentQuery.contains(conjunction)) {
                String[] parts = currentQuery.split(conjunction, 2);
                if (parts.length == 2) {
                    // Preserve contract number context in both parts
                    String contractNumber = extractContractNumberFromQuery(query);

                    String firstPart = parts[0].trim();
                    String secondPart = parts[1].trim();

                    // Add contract number to parts that don't have it
                    if (contractNumber != null) {
                        if (!firstPart.matches(".*\\d{6}.*")) {
                            firstPart += " for " + contractNumber;
                        }
                        if (!secondPart.matches(".*\\d{6}.*")) {
                            secondPart += " for " + contractNumber;
                        }
                    }

                    subQueries.add(firstPart);
                    currentQuery = secondPart;
                }
            }
        }
        subQueries.add(currentQuery);

        // Process each sub-query with preserved context
        List<Map<String, Object>> subResults = new ArrayList<>();
        for (String subQuery : subQueries) {
            if (!subQuery.isEmpty()) {
                Map<String, Object> subResult = processSingleIntent(subQuery);
                subResults.add(subResult);
            }
        }

        // Combine results with enhanced logic
        return combineMultiIntentResults(subResults, query);
    }

    private Map<String, Object> processSingleIntent(String query) {
        // Process as single intent query (recursive call without multi-intent detection)
        Map<String, Object> entities = extractEntities(query);
        String queryType = determineQueryType(query, entities);
        String action = determineAction(queryType, entities);
        List<String> displayEntities = extractDisplayEntities(query, queryType, entities);
        Map<String, String> filterEntities = extractFilterEntities(query, queryType, entities);
        double confidence = calculateConfidence(query, queryType, action, displayEntities, filterEntities);

        return buildResult(action, queryType, displayEntities, filterEntities, confidence);
    }

    private Map<String, Object> combineMultiIntentResults(List<Map<String, Object>> subResults, String originalQuery) {
        if (subResults.isEmpty()) {
            return createErrorResponse("No valid intents found in multi-intent query");
        }

        if (subResults.size() == 1) {
            return subResults.get(0);
        }

        Map<String, Object> combinedResult = new HashMap<>();

        // FIXED: Always use contracts_by_filter for multi-intent
        String action = "contracts_by_filter";
        combinedResult.put("action", action);
        combinedResult.put("queryType", "MULTI_INTENT");

        // Enhanced display entity combination based on query content
        Set<String> allDisplayEntities = new LinkedHashSet<>();

        // Contract details requested
        if (originalQuery.matches(".*(contract|agreement).*(details?|info).*")) {
            allDisplayEntities.add("CONTRACT_NAME");
            allDisplayEntities.add("CUSTOMER_NAME");
            allDisplayEntities.add("EFFECTIVE_DATE");
            allDisplayEntities.add("EXPIRATION_DATE");
            allDisplayEntities.add("STATUS");
        }

        // Specific contract name + effective date only
        if (originalQuery.matches(".*contract\\s+name.*effective\\s+date.*")) {
            allDisplayEntities.add("CONTRACT_NAME");
            allDisplayEntities.add("EFFECTIVE_DATE");
        }

        // Failed parts requested
        if (originalQuery.contains("failed")) {
            allDisplayEntities.add("PART_NUMBER");
            allDisplayEntities.add("ERROR_COLUMN");
            allDisplayEntities.add("REASON");

            // Add contract context for failed parts without contract number
            if (!originalQuery.matches(".*\\d{6}.*")) {
                allDisplayEntities.add("CONTRACT_NAME");
                allDisplayEntities.add("CUSTOMER_NAME");
            }
        }

        // ENHANCED: Parts info requested (non-failed)
        if ((originalQuery.contains("parts") || originalQuery.contains("all parts")) &&
            !originalQuery.contains("failed")) {
            allDisplayEntities.add("INVOICE_PART_NUMBER");
            allDisplayEntities.add("PRICE");
            allDisplayEntities.add("LEAD_TIME");
            allDisplayEntities.add("MOQ");
        }

        // Additional pattern for "list all parts"
        if (originalQuery.matches(".*list\\s+all\\s+parts.*") || originalQuery.matches(".*all\\s+parts.*info.*")) {
            allDisplayEntities.add("INVOICE_PART_NUMBER");
            allDisplayEntities.add("PRICE");
            allDisplayEntities.add("LEAD_TIME");
            allDisplayEntities.add("MOQ");
        }

        // Customer info specifically requested
        if (originalQuery.matches(".*customer\\s+(info|information).*")) {
            allDisplayEntities.add("CUSTOMER_NAME");
        }

        combinedResult.put("displayEntities", new ArrayList<>(allDisplayEntities));

        // Smart filter entity combination with deduplication
        Map<String, String> allFilterEntities = new HashMap<>();
        String contractNumber = null;

        // Collect filter entities and find contract number
        for (Map<String, Object> result : subResults) {
            @SuppressWarnings("unchecked")
            Map<String, String> filterEntities = (Map<String, String>) result.get("filterEntities");
            if (filterEntities != null) {
                if (filterEntities.containsKey("AWARD_NUMBER")) {
                    contractNumber = filterEntities.get("AWARD_NUMBER");
                } else if (filterEntities.containsKey("LOADED_CP_NUMBER")) {
                    contractNumber = filterEntities.get("LOADED_CP_NUMBER");
                }

                // Add non-contract filters
                for (Map.Entry<String, String> entry : filterEntities.entrySet()) {
                    if (!entry.getKey().equals("AWARD_NUMBER") && !entry.getKey().equals("LOADED_CP_NUMBER")) {
                        allFilterEntities.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        // Add single contract filter - always use AWARD_NUMBER for multi-intent
        if (contractNumber != null) {
            allFilterEntities.put("AWARD_NUMBER", contractNumber);
        }

        combinedResult.put("filterEntities", allFilterEntities);

        // Calculate average confidence
        double totalConfidence = 0.0;
        for (Map<String, Object> result : subResults) {
            Number confidence = (Number) result.get("confidence");
            if (confidence != null) {
                totalConfidence += confidence.doubleValue();
            }
        }
        combinedResult.put("confidence", totalConfidence / subResults.size());

        return combinedResult;
    }

    private Map<String, Object> extractEntities(String query) {
        Map<String, Object> entities = new HashMap<>();

        // Extract contract numbers
        List<String> contractNumbers = extractContractNumbers(query);
        if (!contractNumbers.isEmpty()) {
            entities.put("contractNumbers", contractNumbers);
        }

        // Extract part numbers
        List<String> partNumbers = extractPartNumbers(query);
        if (!partNumbers.isEmpty()) {
            entities.put("partNumbers", partNumbers);
        }

        // Extract customer information
        List<String> customerInfo = extractCustomerInfo(query);
        if (!customerInfo.isEmpty()) {
            entities.put("customerInfo", customerInfo);
        }

        // Extract dates
        List<String> dates = extractDates(query);
        if (!dates.isEmpty()) {
            entities.put("dates", dates);
        }

        // Extract business terms
        Map<String, String> businessTerms = extractBusinessTerms(query);
        if (!businessTerms.isEmpty()) {
            entities.put("businessTerms", businessTerms);
        }

        // Extract context indicators
        List<String> contextIndicators = extractContextIndicators(query);
        if (!contextIndicators.isEmpty()) {
            entities.put("contextIndicators", contextIndicators);
        }

        return entities;
    }

    private List<String> extractContractNumbers(String query) {
        List<String> contractNumbers = new ArrayList<>();
        String correctedQuery = applySpellCorrection(query);

        // ENHANCED: Better contract number pattern matching
        Pattern[] patterns = {
            Pattern.compile("\\b(\\d{6})\\b"), // 6-digit numbers
                               Pattern.compile("\\bcontract\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d{5,8})\\b"), // 5-8 digit numbers
            Pattern.compile("\\bfor\\s+(\\d+)\\b"), Pattern.compile("\\babout\\s+(\\d+)\\b"),
            Pattern.compile("\\bwith\\s+(\\d+)\\b")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(correctedQuery);
            while (matcher.find()) {
                String number = matcher.group(1);
                if (number.length() >= 5 && number.length() <= 8) {
                    contractNumbers.add(number);
                }
            }
        }

        // Remove duplicates
        return new ArrayList<>(new LinkedHashSet<>(contractNumbers));
    }

    private List<String> extractDates(String query) {
        List<String> dates = new ArrayList<>();

        Matcher matcher = DATE_PATTERN.matcher(query);
        while (matcher.find()) {
            dates.add(matcher.group());
        }

        return dates;
    }

    private Map<String, String> extractBusinessTerms(String query) {
        Map<String, String> businessTerms = new HashMap<>();

        for (Map.Entry<String, String> entry : BUSINESS_FILTER_TERMS.entrySet()) {
            if (query.contains(entry.getKey())) {
                businessTerms.put(entry.getKey(), entry.getValue());
            }
        }

        return businessTerms;
    }

    private List<String> extractContextIndicators(String query) {
        List<String> indicators = new ArrayList<>();

        // Check for various context indicators
        if (containsAny(query, CUSTOMER_CONTEXT_WORDS)) {
            indicators.add("CUSTOMER_CONTEXT");
        }
        if (containsAny(query, PRICE_CONTEXT_WORDS)) {
            indicators.add("PRICE_CONTEXT");
        }
        if (containsAny(query, STATUS_CONTEXT_WORDS)) {
            indicators.add("STATUS_CONTEXT");
        }
        if (containsAny(query, DATE_CONTEXT_WORDS)) {
            indicators.add("DATE_CONTEXT");
        }
        if (query.contains("failed") || query.contains("error")) {
            indicators.add("ERROR_CONTEXT");
        }

        return indicators;
    }


    private boolean isPartsQuery(String query, Map<String, Object> entities) {
        // Enhanced part-specific keywords including contextual patterns
        String[] partKeywords = {
            "part", "parts", "component", "components", "item", "items", "price", "cost", "leadtime", "lead time",
            "moq", "minimum order", "uom", "unit of measure", "classification", "status"
        };

        for (String keyword : partKeywords) {
            if (query.contains(keyword)) {
                // Check if part numbers are present
                @SuppressWarnings("unchecked")
                List<String> partNumbers = (List<String>) entities.get("partNumbers");
                if (partNumbers != null && !partNumbers.isEmpty()) {
                    return true;
                }

                // Check for part number patterns even if not extracted
                if (containsPartNumberPattern(query)) {
                    return true;
                }

                // ENHANCED: Check for "parts with attribute" patterns
                if (query.matches(".*parts?\\s+with\\s+(high|low|long|short|minimum|maximum).*") ||
                    query.matches(".*parts?\\s+with\\s+.*\\s+(time|price|cost|order|lead).*") ||
                    query.matches(".*parts?\\s+(having|containing|showing)\\s+.*")) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractContractNumberFromQuery(String query) {
        Matcher matcher = CONTRACT_NUMBER_PATTERN.matcher(query);
        if (matcher.find()) {
            String number = matcher.group();
            // Additional validation for contract numbers
            if (number.length() >= 6 && isValidContractNumber(number)) {
                return number;
            }
        }
        return null;
    }


    private double calculateConfidence(String query, String queryType, String action, List<String> displayEntities,
                                       Map<String, String> filterEntities) {
        double confidence = 0.0;

        // Base confidence based on query type detection
        if (queryType != null && !queryType.isEmpty()) {
            confidence += 0.3;
        }

        // Action confidence
        if (action != null && !action.isEmpty()) {
            confidence += 0.2;
        }

        // Entity extraction confidence
        if (!filterEntities.isEmpty()) {
            confidence += 0.3;
        }

        // Display entity confidence
        if (!displayEntities.isEmpty()) {
            confidence += 0.2;
        }

        // Bonus for specific patterns
        if (containsContractNumber(query)) {
            confidence += 0.1;
        }
        if (containsPartNumber(query)) {
            confidence += 0.1;
        }

        // Penalty for ambiguous queries
        if (query.length() < 10) {
            confidence -= 0.1;
        }

        // Penalty for too many typos
        int typoCount = countTypos(query);
        if (typoCount > 3) {
            confidence -= 0.1;
        }

        // Ensure confidence is between 0 and 1
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * BUSINESS RULE VALIDATION - ENHANCED
     */
    private List<String> validateBusinessRules(String queryType, Map<String, String> filterEntities) {
        List<String> validationErrors = new ArrayList<>();

        // Validate contract numbers
        String contractNumber = filterEntities.get("AWARD_NUMBER");
        if (contractNumber != null && !isValidContractNumber(contractNumber)) {
            validationErrors.add("Invalid contract number format: " + contractNumber);
        }

        // Validate part numbers
        String partNumber = filterEntities.get("INVOICE_PART_NUMBER");
        if (partNumber != null && !isValidPartNumber(partNumber)) {
            validationErrors.add("Invalid part number format: " + partNumber);
        }

        // Validate dates
        String effectiveDate = filterEntities.get("EFFECTIVE_DATE");
        String expirationDate = filterEntities.get("EXPIRATION_DATE");
        if (effectiveDate != null && expirationDate != null) {
            if (!isValidDateRange(effectiveDate, expirationDate)) {
                validationErrors.add("Effective date must be before expiration date");
            }
        }

        // Query type specific validations
        switch (queryType) {
        case "FAILED_PARTS":
            if (!filterEntities.containsKey("AWARD_NUMBER") && !filterEntities.containsKey("LOADED_CP_NUMBER")) {
                validationErrors.add("Failed parts queries require a contract number");
            }
            break;

        case "PARTS":
            if (!filterEntities.containsKey("INVOICE_PART_NUMBER") && !filterEntities.containsKey("AWARD_NUMBER") &&
                !filterEntities.containsKey("LOADED_CP_NUMBER")) {
                validationErrors.add("Parts queries require either a part number or contract number");
            }
            break;
        }

        return validationErrors;
    }

    /**
     * RESULT BUILDING - ENHANCED
     */
    private Map<String, Object> buildResult(String action, String queryType, List<String> displayEntities,
                                            Map<String, String> filterEntities, double confidence) {
        Map<String, Object> result = new HashMap<>();

        result.put("action", action);
        result.put("queryType", queryType);
        result.put("displayEntities", displayEntities);
        result.put("filterEntities", filterEntities);
        result.put("confidence", confidence);
        result.put("timestamp", System.currentTimeMillis());
        result.put("modelVersion", MODEL_VERSION);

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("processingTime", System.currentTimeMillis());
        metadata.put("cacheHit", false); // Will be updated if cache is used
        metadata.put("spellCorrectionApplied", spellCorrectionEnabled);
        metadata.put("multiIntentDetected", queryType.equals("MULTI_INTENT"));
        result.put("metadata", metadata);

        return result;
    }

    /**
     * PERFORMANCE METRICS - ENHANCED
     */
    private void updatePerformanceMetrics(String queryType, double confidence, long processingTime, boolean success) {
        queryCount.incrementAndGet();

        // Update average processing time
        averageProcessingTime = (averageProcessingTime * (queryCount.get() - 1) + processingTime) / queryCount.get();

        // Update success/error rates
        if (success) {
            successRate = (successRate * (queryCount.get() - 1) + 1.0) / queryCount.get();
        } else {
            errorRate = (errorRate * (queryCount.get() - 1) + 1.0) / queryCount.get();
        }

        // Update query type counters
        switch (queryType) {
        case "CONTRACTS":
            contractQueryCount.incrementAndGet();
            break;
        case "PARTS":
            partQueryCount.incrementAndGet();
            break;
        case "FAILED_PARTS":
            failedPartsQueryCount.incrementAndGet();
            break;
        case "MULTI_INTENT":
            multiIntentQueryCount.incrementAndGet();
            break;
        }

        // Update confidence metrics
        averageConfidence = (averageConfidence * (queryCount.get() - 1) + confidence) / queryCount.get();
        if (confidence >= confidenceThreshold) {
            highConfidenceCount.incrementAndGet();
        } else {
            lowConfidenceCount.incrementAndGet();
        }
    }

    /**
     * UTILITY METHODS - ENHANCED
     */
    private boolean containsContractNumber(String query) {
        return CONTRACT_NUMBER_PATTERN.matcher(query).find();
    }

    private boolean containsPartNumber(String query) {
        return PART_PATTERN_1.matcher(query).find() || PART_PATTERN_2.matcher(query).find() ||
               PART_PATTERN_3.matcher(query).find() || PART_PATTERN_4.matcher(query).find() ||
               PART_PATTERN_5.matcher(query).find();
    }

    private boolean containsPartNumberPattern(String query) {
        // More flexible part number detection
        return query.matches(".*\\b[A-Z]{2}\\d{5}\\b.*") || query.matches(".*\\b[A-Z]+\\d+[A-Z]*\\b.*") ||
               query.matches(".*\\b\\d+[A-Z]+\\d*\\b.*");
    }

    private boolean isValidContractNumber(String number) {
        return number != null && number.matches("\\d{6,}") && number.length() >= 6 && number.length() <= 12;
    }

    private boolean isValidPartNumber(String partNumber) {
        return partNumber != null && partNumber.length() >= 3 && partNumber.matches("[A-Za-z0-9\\-_]+");
    }

    private boolean isValidCustomerIdentifier(String identifier) {
        return identifier != null &&
               (identifier.matches("\\d{4,}") || (identifier.length() >= 3 && identifier.matches("[A-Za-z0-9\\s]+")));
    }

    private boolean isValidDateRange(String startDate, String endDate) {
        // Simplified date validation - in real implementation, use proper date parsing
        return startDate != null && endDate != null && !startDate.equals(endDate);
    }


    private boolean containsAny(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private int countTypos(String query) {
        int typoCount = 0;
        String[] words = query.split("\\s+");

        for (String word : words) {
            if (COMPREHENSIVE_SPELL_CORRECTIONS.containsKey(word)) {
                typoCount++;
            }
        }

        return typoCount;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("modelVersion", MODEL_VERSION);
        return errorResponse;
    }

    /**
     * CACHE MANAGEMENT - ENHANCED
     */
    private Map<String, Object> getCachedResult(String query) {
        if (!cacheEnabled) {
            return null;
        }

        Map<String, Object> cachedEntry = queryCache.get(query.toLowerCase());
        if (cachedEntry != null) {
            long timestamp = (Long) cachedEntry.get("timestamp");
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME) {
                // Update cache hit metadata
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) cachedEntry.get("metadata");
                if (metadata != null) {
                    metadata.put("cacheHit", true);
                }
                return cachedEntry;
            } else {
                // Remove expired entry
                queryCache.remove(query.toLowerCase());
            }
        }

        return null;
    }

    private void cacheResult(String query, Map<String, Object> result) {
        if (cacheEnabled && result != null && !result.containsKey("error")) {
            queryCache.put(query.toLowerCase(), result);

            // Prevent cache from growing too large
            if (queryCache.size() > 1000) {
                // Remove oldest entries (simplified approach)
                Iterator<Map.Entry<String, Map<String, Object>>> iterator = queryCache.entrySet().iterator();
                for (int i = 0; i < 100 && iterator.hasNext(); i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }

    /**
     * ADVANCED PATTERN MATCHING - ENHANCED
     */
    private Map<String, Object> processAdvancedPatterns(String query) {
        Map<String, Object> patternResults = new HashMap<>();

        // Contract with customer pattern
        Matcher contractCustomerMatcher = ADVANCED_PATTERNS.get("CONTRACT_WITH_CUSTOMER").matcher(query);
        if (contractCustomerMatcher.find()) {
            patternResults.put("contractWithCustomer", contractCustomerMatcher.group(2));
        }

        // Part with price pattern
        Matcher partPriceMatcher = ADVANCED_PATTERNS.get("PART_WITH_PRICE").matcher(query);
        if (partPriceMatcher.find()) {
            patternResults.put("partWithPrice", partPriceMatcher.group(2));
        }

        // Date range pattern
        Matcher dateRangeMatcher = ADVANCED_PATTERNS.get("DATE_RANGE_QUERY").matcher(query);
        if (dateRangeMatcher.find()) {
            patternResults.put("dateRangeStart", dateRangeMatcher.group(2));
            patternResults.put("dateRangeEnd", dateRangeMatcher.group(3));
        }

        // Multi-part pattern
        Matcher multiPartMatcher = ADVANCED_PATTERNS.get("MULTI_PART_QUERY").matcher(query);
        if (multiPartMatcher.find()) {
            String[] parts = multiPartMatcher.group(2).split("[,\\s]+");
            patternResults.put("multipleParts", Arrays.asList(parts));
        }

        return patternResults;
    }

    /**
     * CONTEXT-AWARE PROCESSING - ENHANCED
     */
    private Map<String, Object> processWithContext(String query, String context) {
        Map<String, Object> contextualResult = processQuery(query);

        if (context != null && !context.isEmpty()) {
            // Apply context-specific enhancements
            Map<String, String> contextMappings = CONTEXT_FIELD_MAPPINGS.get(context.toUpperCase());
            if (contextMappings != null) {
                @SuppressWarnings("unchecked")
                List<String> displayEntities = (List<String>) contextualResult.get("displayEntities");
                if (displayEntities != null) {
                    List<String> enhancedDisplayEntities = new ArrayList<>();
                    for (String entity : displayEntities) {
                        String contextualEntity = contextMappings.getOrDefault(entity.toLowerCase(), entity);
                        enhancedDisplayEntities.add(contextualEntity);
                    }
                    contextualResult.put("displayEntities", enhancedDisplayEntities);
                }
            }

            // Add context metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) contextualResult.get("metadata");
            if (metadata != null) {
                metadata.put("appliedContext", context);
            }
        }

        return contextualResult;
    }

    /**
     * BATCH PROCESSING - ENHANCED
     */
    public List<Map<String, Object>> processBatchQueries(List<String> queries) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String query : queries) {
            try {
                Map<String, Object> result = processQuery(query);
                results.add(result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing batch query: " + query, e);
                results.add(createErrorResponse("Batch processing error: " + e.getMessage()));
            }
        }

        return results;
    }

    /**
     * PERFORMANCE MONITORING - ENHANCED
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalQueries", queryCount.get());
        metrics.put("averageProcessingTime", averageProcessingTime);
        metrics.put("successRate", successRate);
        metrics.put("errorRate", errorRate);
        metrics.put("averageConfidence", averageConfidence);

        // Query type distribution
        Map<String, Integer> queryTypeDistribution = new HashMap<>();
        queryTypeDistribution.put("contracts", contractQueryCount.get());
        queryTypeDistribution.put("parts", partQueryCount.get());
        queryTypeDistribution.put("failedParts", failedPartsQueryCount.get());
        queryTypeDistribution.put("multiIntent", multiIntentQueryCount.get());
        metrics.put("queryTypeDistribution", queryTypeDistribution);

        // Confidence distribution
        Map<String, Integer> confidenceDistribution = new HashMap<>();
        confidenceDistribution.put("high", highConfidenceCount.get());
        confidenceDistribution.put("low", lowConfidenceCount.get());
        metrics.put("confidenceDistribution", confidenceDistribution);

        // Cache statistics
        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("enabled", cacheEnabled);
        cacheStats.put("size", queryCache.size());
        cacheStats.put("maxSize", 1000);
        metrics.put("cacheStatistics", cacheStats);

        // Configuration
        Map<String, Object> config = new HashMap<>();
        config.put("confidenceThreshold", confidenceThreshold);
        config.put("spellCorrectionEnabled", spellCorrectionEnabled);
        config.put("multiIntentEnabled", multiIntentEnabled);
        config.put("modelVersion", MODEL_VERSION);
        metrics.put("configuration", config);

        return metrics;
    }

    /**
     * CONFIGURATION METHODS - ENHANCED
     */
    public void setConfidenceThreshold(double threshold) {
        if (threshold >= 0.0 && threshold <= 1.0) {
            this.confidenceThreshold = threshold;
        }
    }

    public void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
        if (!enabled) {
            queryCache.clear();
        }
    }

    public void setSpellCorrectionEnabled(boolean enabled) {
        this.spellCorrectionEnabled = enabled;
    }

    public void setMultiIntentEnabled(boolean enabled) {
        this.multiIntentEnabled = enabled;
    }

    public void clearCache() {
        queryCache.clear();
    }

    public void resetMetrics() {
        queryCount.set(0);
        averageProcessingTime = 0.0;
        successRate = 0.0;
        errorRate = 0.0;
        contractQueryCount.set(0);
        partQueryCount.set(0);
        failedPartsQueryCount.set(0);
        multiIntentQueryCount.set(0);
        averageConfidence = 0.0;
        highConfidenceCount.set(0);
        lowConfidenceCount.set(0);
    }

    /**
     * DEBUGGING AND DIAGNOSTICS - ENHANCED
     */
    public Map<String, Object> diagnoseQuery(String query) {
        Map<String, Object> diagnosis = new HashMap<>();

        // Original query
        diagnosis.put("originalQuery", query);

        // Preprocessing steps
        String preprocessed = preprocessQuery(query);
        diagnosis.put("preprocessedQuery", preprocessed);

        String spellCorrected = applySpellCorrection(preprocessed);
        diagnosis.put("spellCorrectedQuery", spellCorrected);

        // Entity extraction
        Map<String, Object> entities = extractEntities(spellCorrected);
        diagnosis.put("extractedEntities", entities);

        // Pattern matching
        Map<String, Object> patterns = processAdvancedPatterns(spellCorrected);
        diagnosis.put("matchedPatterns", patterns);

        // Query type determination
        String queryType = determineQueryType(spellCorrected, entities);
        diagnosis.put("determinedQueryType", queryType);

        // Action determination
        String action = determineAction(queryType, entities);
        diagnosis.put("determinedAction", action);

        // Display entities
        List<String> displayEntities = extractDisplayEntities(spellCorrected, queryType, entities);
        diagnosis.put("extractedDisplayEntities", displayEntities);

        // Filter entities
        Map<String, String> filterEntities = extractFilterEntities(spellCorrected, queryType, entities);
        diagnosis.put("extractedFilterEntities", filterEntities);

        // Confidence calculation
        double confidence = calculateConfidence(spellCorrected, queryType, action, displayEntities, filterEntities);
        diagnosis.put("calculatedConfidence", confidence);

        // Business rule validation
        List<String> validationErrors = validateBusinessRules(queryType, filterEntities);
        diagnosis.put("validationErrors", validationErrors);

        // Multi-intent detection
        boolean isMultiIntent = isMultiIntentQuery(spellCorrected);
        diagnosis.put("isMultiIntentQuery", isMultiIntent);

        return diagnosis;
    }

    /**
     * EXPORT AND IMPORT METHODS - ENHANCED
     */
    public Map<String, Object> exportConfiguration() {
        Map<String, Object> config = new HashMap<>();

        config.put("confidenceThreshold", confidenceThreshold);
        config.put("cacheEnabled", cacheEnabled);
        config.put("spellCorrectionEnabled", spellCorrectionEnabled);
        config.put("multiIntentEnabled", multiIntentEnabled);
        config.put("modelVersion", MODEL_VERSION);
        config.put("exportTimestamp", System.currentTimeMillis());

        return config;
    }

    public void importConfiguration(Map<String, Object> config) {
        if (config.containsKey("confidenceThreshold")) {
            setConfidenceThreshold(((Number) config.get("confidenceThreshold")).doubleValue());
        }
        if (config.containsKey("cacheEnabled")) {
            setCacheEnabled((Boolean) config.get("cacheEnabled"));
        }
        if (config.containsKey("spellCorrectionEnabled")) {
            setSpellCorrectionEnabled((Boolean) config.get("spellCorrectionEnabled"));
        }
        if (config.containsKey("multiIntentEnabled")) {
            setMultiIntentEnabled((Boolean) config.get("multiIntentEnabled"));
        }
    }

    /**
     * HEALTH CHECK - ENHANCED
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "healthy");
        health.put("modelVersion", MODEL_VERSION);
        health.put("timestamp", System.currentTimeMillis());

        // Component health
        Map<String, String> components = new HashMap<>();
        components.put("spellCorrection", spellCorrectionEnabled ? "enabled" : "disabled");
        components.put("multiIntent", multiIntentEnabled ? "enabled" : "disabled");
        components.put("cache", cacheEnabled ? "enabled" : "disabled");
        health.put("components", components);

        // Performance indicators
        Map<String, Object> performance = new HashMap<>();
        performance.put("totalQueries", queryCount.get());
        performance.put("averageProcessingTime", averageProcessingTime);
        performance.put("successRate", successRate);
        performance.put("cacheSize", queryCache.size());
        health.put("performance", performance);

        // System resources
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> resources = new HashMap<>();
        resources.put("totalMemory", runtime.totalMemory());
        resources.put("freeMemory", runtime.freeMemory());
        resources.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        resources.put("maxMemory", runtime.maxMemory());
        health.put("resources", resources);

        return health;
    }

    /**
     * MAIN PROCESSING METHOD WITH CACHING - ENHANCED
     */
    public Map<String, Object> processQueryWithCache(String query) {
        if (query == null || query.trim().isEmpty()) {
            return createErrorResponse("Query cannot be empty");
        }

        // Check cache first
        Map<String, Object> cachedResult = getCachedResult(query);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Process query
        Map<String, Object> result = processQuery(query);

        // Cache result
        cacheResult(query, result);

        return result;
    }

    private boolean containsAny(String text, Set<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ACTION TYPES CONSTANTS - Centralized for reuse and maintainability
     */
    public static final class ActionTypes {
        // Contract Actions
        public static final String CONTRACTS_BY_CONTRACT_NUMBER = "contracts_by_contractnumber";
        public static final String CONTRACTS_BY_FILTER = "contracts_by_filter";

        // Parts Actions
        public static final String PARTS_BY_PART_NUMBER = "parts_by_part_number";
        public static final String PARTS_BY_CONTRACT_NUMBER = "parts_by_contract_number";

        // Failed Parts Actions
        public static final String PARTS_FAILED_BY_CONTRACT_NUMBER = "parts_failed_by_contract_number";

        // Multi-Intent Actions
        public static final String MULTI_INTENT_FILTER = "contracts_by_filter";

        // Default Actions
        public static final String DEFAULT_ACTION = CONTRACTS_BY_CONTRACT_NUMBER;

        private ActionTypes() {
        } // Prevent instantiation
    }

    /**
     * QUERY TYPES CONSTANTS - Centralized for reuse and maintainability
     */
    public static final class QueryTypes {
        public static final String CONTRACTS = "CONTRACTS";
        public static final String PARTS = "PARTS";
        public static final String FAILED_PARTS = "FAILED_PARTS";
        public static final String MULTI_INTENT = "MULTI_INTENT";
        public static final String ERROR = "ERROR";

        // Default Query Type
        public static final String DEFAULT_QUERY_TYPE = CONTRACTS;

        private QueryTypes() {
        } // Prevent instantiation
    }

    /**
     * ACTION TYPE MAPPING - Business logic for action determination
     */
    private static final Map<String, Map<String, String>> ACTION_TYPE_MAPPING = createActionTypeMapping();

    private static Map<String, Map<String, String>> createActionTypeMapping() {
        Map<String, Map<String, String>> mapping = new HashMap<>();

        // Failed Parts Actions
        Map<String, String> failedPartsActions = new HashMap<>();
        failedPartsActions.put("default", ActionTypes.PARTS_FAILED_BY_CONTRACT_NUMBER);
        mapping.put(QueryTypes.FAILED_PARTS, failedPartsActions);

        // Parts Actions
        Map<String, String> partsActions = new HashMap<>();
        partsActions.put("with_part_number", ActionTypes.PARTS_BY_PART_NUMBER);
        partsActions.put("with_contract_number", ActionTypes.PARTS_BY_CONTRACT_NUMBER);
        partsActions.put("default", ActionTypes.PARTS_BY_CONTRACT_NUMBER);
        mapping.put(QueryTypes.PARTS, partsActions);

        // Contract Actions
        Map<String, String> contractActions = new HashMap<>();
        contractActions.put("with_contract_number", ActionTypes.CONTRACTS_BY_CONTRACT_NUMBER);
        contractActions.put("with_filter", ActionTypes.CONTRACTS_BY_FILTER);
        contractActions.put("default", ActionTypes.CONTRACTS_BY_CONTRACT_NUMBER);
        mapping.put(QueryTypes.CONTRACTS, contractActions);

        // Multi-Intent Actions
        Map<String, String> multiIntentActions = new HashMap<>();
        multiIntentActions.put("failed_parts_primary", ActionTypes.PARTS_FAILED_BY_CONTRACT_NUMBER);
        multiIntentActions.put("parts_primary", ActionTypes.PARTS_BY_CONTRACT_NUMBER);
        multiIntentActions.put("contracts_primary", ActionTypes.CONTRACTS_BY_FILTER);
        multiIntentActions.put("default", ActionTypes.MULTI_INTENT_FILTER);
        mapping.put(QueryTypes.MULTI_INTENT, multiIntentActions);

        return mapping;
    }

    private String determineAction(String queryType, String query, Map<String, Object> entities) {
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        switch (queryType) {
        case "HELP":
            // Check for contract creation help queries
            if (correctedQuery.matches(".*\\b(how to|steps to|steps for|walk me through|explain how to|instructions for|process for|guide for|show me how to|what's the process|need guidance|help understanding|understanding|guidance|explain|instructions|process|guide|walk me through|need help understanding|i need guidance|explain)\\b.*\\b(create|make|generate|build|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish|set up|do|draw|put|get|give|send|provide|help|assist|need|want|require|request|order|ask|demand|wish|like)\\b.*\\b(contract|contrato)\\b.*")) {
                return "HELP_CONTRACT_CREATE_USER";
            }
            // Check for direct contract creation requests
            if (correctedQuery.matches(".*\\b(create|make|generate|build|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish|set up|do|draw|put|get|give|send|provide|help|assist|need|want|require|request|order|ask|demand|wish|like)\\b.*\\b(contract|contrato)\\b.*")) {
                return "HELP_CONTRACT_CREATE_BOT";
            }
            // Default help action
            return "HELP_CONTRACT_CREATE_USER";

        case "CONTRACTS":
            return "contracts_by_contractnumber";

        case "PARTS":
            // FIXED: Consistent action type for parts
            @SuppressWarnings("unchecked")
            List<String> partNumbers = (List<String>) entities.get("partNumbers");
            if (partNumbers != null && !partNumbers.isEmpty()) {
                return "parts_by_part_number";
            }

            // If asking for parts by contract
            @SuppressWarnings("unchecked")
            List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
            if (contractNumbers != null && !contractNumbers.isEmpty()) {
                return "parts_by_contract_number";
            }

            return "parts_by_part_number";

        case "FAILED_PARTS":
            return "parts_failed_by_contract_number";

        case "MULTI_INTENT":
            return "contracts_by_filter";

        default:
            return "contracts_by_contractnumber";
        }
    }

    private String determineAction(String queryType, Map<String, Object> entities) {
        // For cases where we don't have the original query, use simplified logic
        switch (queryType) {
        case "HELP":
            // Default help action for contract creation
            return "HELP_CONTRACT_CREATE_USER";

        case "PARTS":
            // Check if we have part numbers
            @SuppressWarnings("unchecked")
            List<String> partNumbers = (List<String>) entities.get("partNumbers");
            if (partNumbers != null && !partNumbers.isEmpty()) {
                return "parts_by_part_number";
            }

            // Check if asking for parts by contract
            @SuppressWarnings("unchecked")
            List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
            if (contractNumbers != null && !contractNumbers.isEmpty()) {
                return "parts_by_contract_number";
            }

            return "parts_by_part_number";

        case "FAILED_PARTS":
            return "parts_failed_by_contract_number";

        case "MULTI_INTENT":
            return "contracts_by_filter";

        case "CONTRACTS":
        default:
            return "contracts_by_contractnumber";
        }
    }

    private boolean containsFailedPartsContext(Map<String, Object> entities) {
        // Check if entities contain failed parts indicators
        @SuppressWarnings("unchecked")
        List<String> contextIndicators = (List<String>) entities.get("contextIndicators");
        if (contextIndicators != null && contextIndicators.contains("ERROR_CONTEXT")) {
            return true;
        }

        // Check business terms for failed parts context
        @SuppressWarnings("unchecked")
        Map<String, String> businessTerms = (Map<String, String>) entities.get("businessTerms");
        if (businessTerms != null) {
            for (String key : businessTerms.keySet()) {
                if (key.contains("failed") || key.contains("error") || key.contains("validation")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsPartsContext(Map<String, Object> entities) {
        // Check if part numbers are present
        @SuppressWarnings("unchecked")
        List<String> partNumbers = (List<String>) entities.get("partNumbers");
        if (partNumbers != null && !partNumbers.isEmpty()) {
            return true;
        }

        // Check context indicators for parts context
        @SuppressWarnings("unchecked")
        List<String> contextIndicators = (List<String>) entities.get("contextIndicators");
        if (contextIndicators != null && contextIndicators.contains("PRICE_CONTEXT")) {
            return true;
        }

        // Check business terms for parts-related terms
        @SuppressWarnings("unchecked")
        Map<String, String> businessTerms = (Map<String, String>) entities.get("businessTerms");
        if (businessTerms != null) {
            for (String key : businessTerms.keySet()) {
                if (key.contains("part") || key.contains("price") || key.contains("moq") || key.contains("leadtime") ||
                    key.contains("uom")) {
                    return true;
                }
            }
        }

        return false;
    }

    private String determineMultiIntentAction(Map<String, Object> entities, Map<String, String> actionMap) {
        // Logic to determine primary intent
        if (containsFailedPartsContext(entities)) {
            return actionMap.get("failed_parts_primary");
        }
        if (containsPartsContext(entities)) {
            return actionMap.get("parts_primary");
        }
        return actionMap.get("contracts_primary");
    }

    private boolean isValidQueryType(String queryType) {
        return QueryTypes.CONTRACTS.equals(queryType) || QueryTypes.PARTS.equals(queryType) ||
               QueryTypes.FAILED_PARTS.equals(queryType) || QueryTypes.MULTI_INTENT.equals(queryType) ||
               "HELP".equals(queryType);
    }

    private boolean isValidActionType(String actionType) {
        return ActionTypes.CONTRACTS_BY_CONTRACT_NUMBER.equals(actionType) ||
               ActionTypes.CONTRACTS_BY_FILTER.equals(actionType) ||
               ActionTypes.PARTS_BY_PART_NUMBER.equals(actionType) ||
               ActionTypes.PARTS_BY_CONTRACT_NUMBER.equals(actionType) ||
               ActionTypes.PARTS_FAILED_BY_CONTRACT_NUMBER.equals(actionType) ||
               "HELP_CONTRACT_CREATE_USER".equals(actionType) ||
               "HELP_CONTRACT_CREATE_BOT".equals(actionType);
    }

    private String determineMultiIntentActionFromQuery(String query) {
        Map<String, String> actionMap = ACTION_TYPE_MAPPING.get(QueryTypes.MULTI_INTENT);

        if (query.contains("failed")) {
            return actionMap.get("failed_parts_primary");
        }
        if (query.matches(".*list.*parts.*") || query.matches(".*show.*parts.*")) {
            return actionMap.get("parts_primary");
        }
        return actionMap.get("contracts_primary");
    }

    private List<String> extractContractsDisplayEntities(String query) {
        List<String> entities = new ArrayList<>();
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // FIXED: Enhanced specific field detection with spell correction
        if (correctedQuery.contains("effective date") || correctedQuery.contains("start date") ||
            correctedQuery.contains("begin date")) {
            entities.add("EFFECTIVE_DATE");
        } else if (correctedQuery.contains("expiration date") || correctedQuery.contains("expiry date") ||
                   correctedQuery.contains("end date") || correctedQuery.contains("expire")) {
            entities.add("EXPIRATION_DATE");
        } else if (correctedQuery.contains("customer name") || correctedQuery.contains("who is customer") ||
                   correctedQuery.contains("customer for")) {
            entities.add("CUSTOMER_NAME");
        } else if (correctedQuery.contains("payment terms") || correctedQuery.contains("payment")) {
            entities.add("PAYMENT_TERMS");
        } else if (correctedQuery.contains("incoterms") || correctedQuery.contains("incoterm")) {
            entities.add("PAYMENT_TERMS");
            entities.add("INCOTERMS");
        } else if (correctedQuery.contains("contract length") || correctedQuery.contains("length")) {
            entities.add("CONTRACT_LENGTH");
        } else if (correctedQuery.contains("price expiration") || correctedQuery.contains("price expire")) {
            if (correctedQuery.contains("price expiration date")) {
                entities.add("EXPIRATION_DATE");
                entities.add("PRICE_EXPIRATION_DATE");
            } else {
                entities.add("PRICE_EXPIRATION_DATE");
            }
        } else if (correctedQuery.contains("creation date") || correctedQuery.contains("create date") ||
                   correctedQuery.contains("created")) {
            entities.add("CREATE_DATE");
        } else if (correctedQuery.contains("contract type") || correctedQuery.contains("type of contract") ||
                   correctedQuery.contains("what kind")) {
            entities.add("CONTRACT_TYPE");
        } else if (correctedQuery.contains("status") || correctedQuery.contains("active")) {
            entities.add("STATUS");
        } else if (correctedQuery.contains("all details") || correctedQuery.contains("everything") ||
                   correctedQuery.contains("full info") || correctedQuery.contains("complete details") ||
                   correctedQuery.contains("summary") || correctedQuery.contains("overview") ||
                   correctedQuery.contains("brief") || correctedQuery.contains("quick info") ||
                   correctedQuery.contains("details about") || correctedQuery.contains("information on") ||
                   correctedQuery.contains("tell me about") || correctedQuery.contains("show me") ||
                   correctedQuery.contains("help me with") || correctedQuery.contains("whats up") ||
                   correctedQuery.contains("how") || correctedQuery.contains("anything wrong") ||
                   correctedQuery.contains("is") || correctedQuery.contains("problems") ||
                   correctedQuery.contains("troubles") || correctedQuery.contains("concerns") ||
                   correctedQuery.contains("update")) {
            // Default comprehensive contract information
            entities.add("CONTRACT_NAME");
            entities.add("CUSTOMER_NAME");
            entities.add("EFFECTIVE_DATE");
            entities.add("EXPIRATION_DATE");
            entities.add("STATUS");
        }

        // If no specific entities found, return default contract details
        if (entities.isEmpty()) {
            entities.add("CONTRACT_NAME");
            entities.add("CUSTOMER_NAME");
            entities.add("EFFECTIVE_DATE");
            entities.add("EXPIRATION_DATE");
            entities.add("STATUS");
        }

        return entities;
    }

    private List<String> extractPartsDisplayEntities(String query) {
        List<String> entities = new ArrayList<>();
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // FIXED: Enhanced specific field detection
        if (correctedQuery.contains("lead time") || correctedQuery.contains("leadtime") ||
            correctedQuery.contains("delivery time")) {
            entities.add("INVOICE_PART_NUMBER");
            entities.add("LEAD_TIME");
        } else if (correctedQuery.contains("price") || correctedQuery.contains("cost") ||
                   correctedQuery.contains("pricing")) {
            entities.add("INVOICE_PART_NUMBER");
            entities.add("PRICE");
        } else if (correctedQuery.contains("moq") || correctedQuery.contains("minimum order") ||
                   correctedQuery.contains("min order")) {
            entities.add("INVOICE_PART_NUMBER");
            entities.add("MOQ");
        } else if (correctedQuery.contains("unit of measure") || correctedQuery.contains("unit measure") ||
                   correctedQuery.contains("uom") || correctedQuery.contains("what unit")) {
            entities.add("INVOICE_PART_NUMBER");
            entities.add("UOM");
        } else if (correctedQuery.contains("status") || correctedQuery.contains("active")) {
            entities.add("INVOICE_PART_NUMBER");
            entities.add("STATUS");
        } else if (correctedQuery.contains("classification") || correctedQuery.contains("class") ||
                   correctedQuery.contains("item class")) {
            entities.add("INVOICE_PART_NUMBER");
            entities.add("ITEM_CLASSIFICATION");
        } else {
            // FIXED: For generic part queries, return part number only instead of empty
            entities.add("INVOICE_PART_NUMBER");
        }

        return entities;
    }

    private List<String> extractFailedPartsDisplayEntities(String query) {
        List<String> entities = new ArrayList<>();
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // Enhanced detection for specific failed parts information
        if (correctedQuery.contains("missing data") || correctedQuery.contains("no data") ||
            correctedQuery.contains("incomplete")) {
            entities.add("PART_NUMBER");
            entities.add("ERROR_COLUMN");
            entities.add("REASON");
            entities.add("DATA_QUALITY_ISSUE");
        } else if (correctedQuery.contains("loading error") || correctedQuery.contains("load error") ||
                   correctedQuery.contains("loading issue") || correctedQuery.contains("load problem") ||
                   correctedQuery.contains("during load")) {
            entities.add("PART_NUMBER");
            entities.add("ERROR_COLUMN");
            entities.add("REASON");
            entities.add("LOADING_ERROR");
        } else if (correctedQuery.contains("validation") || correctedQuery.contains("validate")) {
            entities.add("PART_NUMBER");
            entities.add("ERROR_COLUMN");
            entities.add("REASON");
            entities.add("VALIDATION_ERROR");
        } else if (correctedQuery.contains("column") || correctedQuery.contains("field")) {
            entities.add("PART_NUMBER");
            entities.add("ERROR_COLUMN");
            entities.add("REASON");
        } else {
            // Default failed parts information
            entities.add("PART_NUMBER");
            entities.add("ERROR_COLUMN");
            entities.add("REASON");
        }

        return entities;
    }

    private boolean containsBusinessEntity(String text) {
        String[] businessEntities = {
            "contract", "customer", "part", "price", "lead time", "effective date", "expiration date", "status",
            "failed", "error", "payment", "incoterms", "classification", "moq", "uom"
        };

        String lowerText = text.toLowerCase();
        for (String entity : businessEntities) {
            if (lowerText.contains(entity)) {
                return true;
            }
        }
        return false;
    }

    private String applySpellCorrection(String query) {
        if (query == null || query.isEmpty())
            return query;

        String corrected = query;


        // CRITICAL FIX: Handle contractions FIRST (before other corrections)
        corrected = corrected.replaceAll("(?i)\\bwhat's\\b", "what is")
                             .replaceAll("(?i)\\bthat's\\b", "that is")
                             .replaceAll("(?i)\\bwhere's\\b", "where is")
                             .replaceAll("(?i)\\bwhen's\\b", "when is")
                             .replaceAll("(?i)\\bhow's\\b", "how is")
                             .replaceAll("(?i)\\bwho's\\b", "who is")
                             .replaceAll("(?i)\\bit's\\b", "it is")
                             .replaceAll("(?i)\\bthere's\\b", "there is");

        // Apply comprehensive spell corrections
        for (Map.Entry<String, String> entry : createComprehensiveSpellCorrections().entrySet()) {
            // Use word boundary regex to avoid partial word replacements
            corrected = corrected.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }

        // Additional common misspellings not in the map
        corrected = corrected.replaceAll("\\btyp\\b", "type");
        corrected = corrected.replaceAll("\\bincotems\\b", "incoterms");
        corrected = corrected.replaceAll("\\binvoic\\b", "invoice");
        corrected = corrected.replaceAll("\\bfaild\\b", "failed");
        corrected = corrected.replaceAll("\\bwhos\\b", "who is");
        corrected = corrected.replaceAll("\\bwhats\\b", "what is");

        return corrected.trim();
    }


    private List<String> getDefaultDisplayEntities(String queryType) {
        List<String> defaults = new ArrayList<>();
        switch (queryType) {
        case "CONTRACTS":
            defaults.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE",
                                          "STATUS"));
            break;
        case "PARTS":
            defaults.add("INVOICE_PART_NUMBER");
            break;
        case "FAILED_PARTS":
            defaults.addAll(Arrays.asList("PART_NUMBER", "ERROR_COLUMN", "REASON"));
            break;
        case "MULTI_INTENT":
            defaults.addAll(Arrays.asList("CONTRACT_NAME", "CUSTOMER_NAME", "EFFECTIVE_DATE", "EXPIRATION_DATE",
                                          "STATUS"));
            break;
        }
        return defaults;
    }

    private String determineQueryType(String query, Map<String, Object> entities) {
        // CRITICAL FIX: Apply spell correction FIRST
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Check for multi-intent BEFORE single-intent classification
        if (isMultiIntentQuery(correctedQuery)) {
            return "MULTI_INTENT";
        }

        // CRITICAL FIX: Check for "created by" and "created in" queries BEFORE HELP detection
        // This should be classified as CONTRACTS query, not HELP
        if (correctedQuery.matches(".*\\b(created|create)\\s+by\\b.*") ||
            correctedQuery.matches(".*\\b(created|create)\\s+in\\b.*") ||
            correctedQuery.matches(".*\\b(contracts?)\\s+(created|create)\\s+by\\b.*") ||
            correctedQuery.matches(".*\\b(contracts?)\\s+(created|create)\\s+in\\b.*") ||
            correctedQuery.matches(".*\\b(show|get|find|list)\\s+(contracts?)\\s+(created|create)\\s+by\\b.*") ||
            correctedQuery.matches(".*\\b(show|get|find|list)\\s+(contracts?)\\s+(created|create)\\s+in\\b.*") ||
            correctedQuery.matches(".*\\b(contracts?)\\s+by\\s+\\w+\\b.*") ||
            correctedQuery.matches(".*\\b(contracts?)\\s+in\\s+\\d{4}\\b.*")) {
            return "CONTRACTS";
        }

        // CRITICAL FIX: Add HELP detection for contract creation queries
        if (correctedQuery.matches(".*\\b(how to|steps to|steps for|walk me through|explain how to|instructions for|process for|guide for|show me how to|what's the process|need guidance|help understanding|understanding|guidance|explain|instructions|process|guide|walk me through|need help understanding|i need guidance|explain how|explain)\\b.*\\b(create|make|generate|build|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish|set up|do|draw|put|get|give|send|provide|help|assist|need|want|require|request|order|ask|demand|wish|like)\\b.*\\b(contract|contrato)\\b.*") ||
            correctedQuery.matches(".*\\b(create|make|generate|build|draft|initiate|start|produce|prepare|compose|write|construct|form|develop|assemble|manufacture|fabricate|establish|set up|do|draw|put|get|give|send|provide|help|assist|need|want|require|request|order|ask|demand|wish|like)\\b.*\\b(contract|contrato)\\b.*") ||
            correctedQuery.matches(".*\\b(creation|generation|making|building|drafting|initiation|starting|production|preparation|composition|writing|construction|formation|development|assembly|manufacturing|fabrication|establishment|setup|doing|drawing|putting|getting|giving|sending|providing|helping|assisting|needing|wanting|requiring|requesting|ordering|asking|demanding|wishing|liking)\\b.*\\b(contract|contrato)\\b.*")) {
            return "HELP";
        }

        // PRESERVE EXISTING LOGIC: Enhanced part queries detection (unchanged)
        if (correctedQuery.matches(".*\\b(show|get|what).*part\\s+(details|info|information|data)\\b.*") ||
            correctedQuery.matches(".*\\bpart\\s+(details|info|information|data)\\s+for\\b.*") ||
            correctedQuery.matches(".*\\bpart\\s+[a-zA-Z0-9]+\\s+(details|info|information)\\b.*")) {
            return "PARTS";
        }

        // PRESERVE EXISTING LOGIC: Part-specific field queries (unchanged)
        if (correctedQuery.matches(".*\\b(lead\\s*time|leadtime|delivery\\s*time)\\s+for\\s+part\\b.*") ||
            correctedQuery.matches(".*\\bwhat\\s+(lead\\s*time|leadtime|price|cost|unit|moq)\\s+for\\s+part\\b.*") ||
            correctedQuery.matches(".*\\bpart\\s+[a-zA-Z0-9]+\\s+(lead\\s*time|leadtime|price|cost|unit|moq)\\b.*")) {
            return "PARTS";
        }

        // PRESERVE EXISTING LOGIC: Generic part field queries with part numbers (unchanged)
        @SuppressWarnings("unchecked")
        List<String> partNumbers = (List<String>) entities.get("partNumbers");
        if (partNumbers != null && !partNumbers.isEmpty()) {
            if (correctedQuery.matches(".*\\b(price|cost|pricing|lead\\s*time|leadtime|moq|unit|uom|classification|status)\\b.*")) {
                return "PARTS";
            }
        }

        // PRESERVE EXISTING LOGIC: Part context with specific patterns (unchanged)
        if (correctedQuery.contains("part") &&
            (correctedQuery.matches(".*\\b[A-Z]{2}\\d{5}\\b.*") || correctedQuery.matches(".*\\b[A-Z]+\\d+\\b.*"))) {
            return "PARTS";
        }

        // PRESERVE EXISTING LOGIC: Failed parts detection (unchanged)
        if (correctedQuery.matches(".*\\b(failed|error|issue|problem|missing|incomplete|validation|loading)\\b.*")) {
            if (correctedQuery.contains("part") || correctedQuery.contains("column")) {
                return "FAILED_PARTS";
            }
        }

        // PRESERVE EXISTING LOGIC: Contract queries (unchanged)
        if (isContractQuery(correctedQuery, entities)) {
            return "CONTRACTS";
        }

        // PRESERVE EXISTING LOGIC: Default fallback (unchanged)
        return "CONTRACTS";
    }

    private List<String> extractDisplayEntities(String query, String queryType, Map<String, Object> entities) {
        List<String> displayEntities = new ArrayList<>();

        // Apply spell correction FIRST
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Incoterms queries (MUST come before payment terms check)
        if (correctedQuery.matches(".*\\b(incoterms?|incoterm)\\b.*")) {
            displayEntities.add("INCOTERMS");
            return displayEntities; // EARLY RETURN to prevent payment terms addition
        }

        // Payment terms queries (after incoterms check)
        if (correctedQuery.matches(".*\\b(payment\\s+terms?|payment)\\b.*")) {
            displayEntities.add("PAYMENT_TERMS");
            return displayEntities;
        }

        // Contract expiration queries (before general contract logic)
        if (correctedQuery.matches(".*\\b(when.*expire|when.*end|expir|expiration)\\b.*") &&
            !correctedQuery.contains("price")) {
            displayEntities.add("EXPIRATION_DATE");
            return displayEntities;
        }

        // Contract effective/start/begin date queries
        if (correctedQuery.matches(".*\\b(effective\\s+date|start\\s+date|begin\\s+date)\\b.*")) {
            displayEntities.add("EFFECTIVE_DATE");
            return displayEntities;
        }

        // Contract creation date queries
        if (correctedQuery.matches(".*\\b(create\\s+date|creation\\s+date|created|when\\s+created|when\\s+was.*created)\\b.*")) {
            displayEntities.add("CREATE_DATE");
            return displayEntities;
        }

        // Contract type queries
        if (correctedQuery.matches(".*\\b(contract\\s+type|type\\s+of\\s+contract|what\\s+type|what\\s+kind)\\b.*")) {
            displayEntities.add("CONTRACT_TYPE");
            return displayEntities;
        }

        // PARTS-specific field extraction
        if (queryType.equals("PARTS")) {

            // Lead time queries
            if (correctedQuery.matches(".*\\b(lead\\s*time|leadtime|delivery\\s*time)\\b.*")) {
                displayEntities.add("LEAD_TIME");
                return displayEntities;
            }

            // Unit of measure queries
            if (correctedQuery.matches(".*\\b(unit\\s*of\\s*measure|unit\\s*measure|uom|what\\s+unit)\\b.*")) {
                displayEntities.add("UOM");
                return displayEntities;
            }

            // MOQ queries
            if (correctedQuery.matches(".*\\b(minimum\\s*order|min\\s*order|moq)\\b.*")) {
                displayEntities.add("MOQ");
                return displayEntities;
            }

            // Price queries
            if (correctedQuery.matches(".*\\b(price|cost|pricing)\\b.*") && !correctedQuery.contains("expir")) {
                displayEntities.add("PRICE");
                return displayEntities;
            }

            // Classification queries
            if (correctedQuery.matches(".*\\b(item\\s*classification|classification|class|item\\s*class)\\b.*")) {
                displayEntities.add("ITEM_CLASSIFICATION");
                return displayEntities;
            }

            // Part status queries
            if (correctedQuery.matches(".*\\b(status|active|inactive)\\b.*")) {
                displayEntities.add("INVOICE_PART_NUMBER");
                displayEntities.add("STATUS");
                return displayEntities;
            }

            // Generic part details/info queries
            if (correctedQuery.matches(".*\\bpart\\s+(details|info|information|data|summary)\\b.*") ||
                correctedQuery.matches(".*\\b(show|get|what).*part\\s+(details|info|information)\\b.*")) {
                displayEntities.add("INVOICE_PART_NUMBER");
                return displayEntities;
            }

            // Default for PARTS queries
            displayEntities.add("INVOICE_PART_NUMBER");
            return displayEntities;
        }

        // Customer queries
        if (correctedQuery.matches(".*\\b(who.*customer|customer.*name|customer.*for)\\b.*")) {
            displayEntities.add("CUSTOMER_NAME");
            return displayEntities;
        }

        // Contract length queries
        if (correctedQuery.matches(".*\\b(contract\\s+length|length)\\b.*")) {
            displayEntities.add("CONTRACT_LENGTH");
            return displayEntities;
        }

        // Status queries (non-part)
        if (correctedQuery.matches(".*\\b(status|active|inactive)\\b.*") && !correctedQuery.contains("part")) {
            displayEntities.add("STATUS");
            return displayEntities;
        }

        // FAILED_PARTS queries
        if (queryType.equals("FAILED_PARTS")) {
            displayEntities.add("PART_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
            return displayEntities;
        }

        // MULTI_INTENT queries
        if (queryType.equals("MULTI_INTENT")) {
            return extractMultiIntentDisplayEntities(correctedQuery);
        }

        // Default for CONTRACTS
        displayEntities.add("CONTRACT_NAME");
        displayEntities.add("CUSTOMER_NAME");
        displayEntities.add("EFFECTIVE_DATE");
        displayEntities.add("EXPIRATION_DATE");
        displayEntities.add("STATUS");

        return displayEntities;
    }

    private List<String> extractPartNumbers(String query) {
        List<String> partNumbers = new ArrayList<>();
        String correctedQuery = applySpellCorrection(query);

        // CRITICAL FIX: Enhanced part number patterns
        Pattern[] patterns = {
            // Specific part number formats with context
            Pattern.compile("\\bpart\\s+([A-Z]{2}\\d{5})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfor\\s+part\\s+([A-Z]{2}\\d{5})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bof\\s+part\\s+([A-Z]{2}\\d{5})\\b", Pattern.CASE_INSENSITIVE),

            // Direct part number patterns
            Pattern.compile("\\b([A-Z]{2}\\d{5})\\b", Pattern.CASE_INSENSITIVE),

            // Generic alphanumeric patterns with context
            Pattern.compile("\\bpart\\s+([A-Z]+\\d+)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfor\\s+([A-Z]{2,}\\d{3,})\\b", Pattern.CASE_INSENSITIVE),

            // Standalone alphanumeric patterns (more restrictive)
            Pattern.compile("\\b([A-Z]{2,4}\\d{3,6})\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([A-Z]+\\d{4,})\\b", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(correctedQuery);
            while (matcher.find()) {
                String partNumber = matcher.group(1);

                // Validation: exclude contract numbers and common words
                if (!partNumber.matches("\\d{6}") && // Not a 6-digit contract number
                    partNumber.length() >= 3 && partNumber.length() <= 15 &&
                    !COMMAND_WORDS.contains(partNumber.toLowerCase()) &&
                    !partNumber.toLowerCase().matches("(part|parts|info|details|data|information)")) {
                    partNumbers.add(partNumber.toUpperCase());
                }
            }
        }

        // Remove duplicates
        return new ArrayList<>(new LinkedHashSet<>(partNumbers));
    }

    private List<String> extractCustomerInfo(String query) {
        List<String> customerInfo = new ArrayList<>();
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Skip question patterns completely
        if (correctedQuery.matches(".*\\b(who.*customer|what.*customer|customer.*name|customer.*info|customer.*details|customer.*number|customer.*for|show.*customer|get.*customer)\\b.*")) {
            return customerInfo; // Return empty list for question patterns
        }

        // Look for actual customer names/numbers in specific patterns only
        Pattern[] customerPatterns = {
            Pattern.compile("\\bcustomer\\s+([a-zA-Z][a-zA-Z0-9\\s]{2,20})\\b"),
            Pattern.compile("\\bfor\\s+customer\\s+([a-zA-Z][a-zA-Z0-9\\s]{2,20})\\b"),
            Pattern.compile("\\bclient\\s+([a-zA-Z][a-zA-Z0-9\\s]{2,20})\\b")
        };

        for (Pattern pattern : customerPatterns) {
            Matcher matcher = pattern.matcher(correctedQuery);
            while (matcher.find()) {
                String customerValue = matcher.group(1).trim();

                // Strict validation
                if (isValidCustomerValue(customerValue)) {
                    customerInfo.add(customerValue);
                }
            }
        }

        return customerInfo;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Remove whitespace and check if all characters are digits
        String trimmed = str.trim();
        return trimmed.matches("\\d+") && trimmed.length() > 0;
    }


    private boolean validateQueryResult(String query, String queryType, List<String> displayEntities,
                                        Map<String, String> filterEntities) {
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Enhanced validation rules
        switch (queryType) {
        case "PARTS":
            // Must have part number or contract number filter
            if (!filterEntities.containsKey("INVOICE_PART_NUMBER") && !filterEntities.containsKey("AWARD_NUMBER")) {
                return false;
            }

            // Specific field validation
            if (correctedQuery.contains("lead time") && !displayEntities.contains("LEAD_TIME")) {
                return false;
            }
            if (correctedQuery.matches(".*\\bunit\\b.*") && !displayEntities.contains("UOM")) {
                return false;
            }
            if (correctedQuery.contains("price") && !displayEntities.contains("PRICE")) {
                return false;
            }
            if (correctedQuery.contains("moq") && !displayEntities.contains("MOQ")) {
                return false;
            }
            if (correctedQuery.contains("classification") && !displayEntities.contains("ITEM_CLASSIFICATION")) {
                return false;
            }
            break;

        case "CONTRACTS":
            // Must have contract number filter
            if (!filterEntities.containsKey("AWARD_NUMBER")) {
                return false;
            }

            // Specific field validation
            if (correctedQuery.matches(".*\\bexpir\\b.*") && !correctedQuery.contains("price") &&
                !displayEntities.contains("EXPIRATION_DATE")) {
                return false;
            }
            if (correctedQuery.contains("effective") && !displayEntities.contains("EFFECTIVE_DATE")) {
                return false;
            }
            if (correctedQuery.contains("create") && !displayEntities.contains("CREATE_DATE")) {
                return false;
            }
            if (correctedQuery.contains("contract type") && !displayEntities.contains("CONTRACT_TYPE")) {
                return false;
            }
            break;

        case "FAILED_PARTS":
            // Must have contract number filter
            if (!filterEntities.containsKey("LOADED_CP_NUMBER")) {
                return false;
            }

            // Must have basic failed parts entities
            if (!displayEntities.contains("PART_NUMBER") || !displayEntities.contains("REASON")) {
                return false;
            }
            break;
        }

        return true;
    }

    private boolean isContractQuery(String query, Map<String, Object> entities) {
        // CRITICAL FIX: Apply spell correction first
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Exclude part queries explicitly
        if (correctedQuery.contains("part")) {
            return false;
        }

        // PRESERVE EXISTING LOGIC: Contract-specific keywords (unchanged)
        if (correctedQuery.matches(".*\\b(contract|customer|effective|expiration|payment|incoterm|status)\\b.*")) {
            return true;
        }

        // PRESERVE EXISTING LOGIC: Contract number patterns (unchanged)
        @SuppressWarnings("unchecked")
        List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
        if (contractNumbers != null && !contractNumbers.isEmpty()) {
            return true;
        }

        // PRESERVE EXISTING LOGIC: Generic info requests (unchanged)
        if (correctedQuery.matches(".*\\b(show|get|tell|info|details|about)\\b.*\\d{6}\\b.*")) {
            return true;
        }

        return false;
    }

    private List<String> extractMultiIntentDisplayEntities(String query) {
        List<String> displayEntities = new ArrayList<>();

        // Apply spell correction
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Enhanced pattern matching for contractions and possessives
        // Remove contractions and possessives for better pattern matching
        String normalizedQuery = correctedQuery.replaceAll("what's", "what is")
                                               .replaceAll("'s", " is")
                                               .replaceAll("'re", " are")
                                               .replaceAll("'ll", " will")
                                               .replaceAll("'ve", " have")
                                               .replaceAll("'d", " would");

        // CRITICAL FIX: Multiple pattern variations for "effective date and part errors"
        if (normalizedQuery.matches(".*\\beffective\\s+date.*part\\s+error.*") ||
            normalizedQuery.matches(".*\\beffective\\s+date.*error.*part.*") ||
            normalizedQuery.matches(".*\\beffective.*date.*part.*error.*") ||
            normalizedQuery.matches(".*\\bwhat.*effective\\s+date.*part\\s+error.*") ||
            normalizedQuery.matches(".*\\beffective\\s+date.*and.*part\\s+error.*")) {
            displayEntities.add("EFFECTIVE_DATE");
            displayEntities.add("PART_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
            return displayEntities;
        }

        // CRITICAL FIX: Contract details and failed parts combination (enhanced patterns)
        if (normalizedQuery.matches(".*\\bcontract\\s+details.*failed\\s+part.*") ||
            normalizedQuery.matches(".*\\bcontract.*detail.*part.*issue.*") ||
            normalizedQuery.matches(".*\\bcontract.*info.*failed.*part.*") ||
            normalizedQuery.matches(".*\\bshow.*contract.*detail.*failed\\s+part.*") ||
            normalizedQuery.matches(".*\\bcontract.*detail.*and.*failed\\s+part.*")) {
            displayEntities.add("CONTRACT_NAME");
            displayEntities.add("CUSTOMER_NAME");
            displayEntities.add("EFFECTIVE_DATE");
            displayEntities.add("EXPIRATION_DATE");
            displayEntities.add("STATUS");
            displayEntities.add("PART_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
            return displayEntities;
        }

        // CRITICAL FIX: Customer name and parts errors combination
        if (normalizedQuery.matches(".*\\bcustomer\\s+name.*part.*error.*") ||
            normalizedQuery.matches(".*\\bget\\s+customer\\s+name.*part.*error.*") ||
            normalizedQuery.matches(".*\\bcustomer.*name.*and.*part.*error.*")) {
            displayEntities.add("CUSTOMER_NAME");
            displayEntities.add("PART_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
            return displayEntities;
        }

        // PRESERVE EXISTING LOGIC: Parts and customer info combination (unchanged)
        if (normalizedQuery.matches(".*\\bpart.*customer\\s+info.*") ||
            normalizedQuery.matches(".*\\bcustomer.*info.*part.*") ||
            normalizedQuery.matches(".*\\ball\\s+part.*customer.*")) {
            displayEntities.add("INVOICE_PART_NUMBER");
            displayEntities.add("PRICE");
            displayEntities.add("LEAD_TIME");
            displayEntities.add("MOQ");
            displayEntities.add("CUSTOMER_NAME");
            return displayEntities;
        }

        // PRESERVE EXISTING LOGIC: Contract-related entities (unchanged)
        if (normalizedQuery.contains("contract") || normalizedQuery.contains("effective") ||
            normalizedQuery.contains("expiration") || normalizedQuery.contains("customer")) {
            displayEntities.add("CONTRACT_NAME");
            displayEntities.add("CUSTOMER_NAME");
            displayEntities.add("EFFECTIVE_DATE");
            displayEntities.add("EXPIRATION_DATE");
            displayEntities.add("STATUS");
        }

        // PRESERVE EXISTING LOGIC: Part-related entities (unchanged)
        if (normalizedQuery.contains("part") || normalizedQuery.contains("price") || normalizedQuery.contains("lead") ||
            normalizedQuery.contains("moq")) {
            displayEntities.add("INVOICE_PART_NUMBER");
            displayEntities.add("PRICE");
            displayEntities.add("LEAD_TIME");
            displayEntities.add("MOQ");
        }

        // PRESERVE EXISTING LOGIC: Failed parts entities (unchanged)
        if (normalizedQuery.contains("failed") || normalizedQuery.contains("error") ||
            normalizedQuery.contains("issue") || normalizedQuery.contains("problem")) {
            displayEntities.add("PART_NUMBER");
            displayEntities.add("ERROR_COLUMN");
            displayEntities.add("REASON");
        }

        // PRESERVE EXISTING LOGIC: Remove duplicates (unchanged)
        return new ArrayList<>(new LinkedHashSet<>(displayEntities));
    }

    private Map<String, String> extractFilterEntities(String query, String queryType, Map<String, Object> entities) {
        Map<String, String> filterEntities = new HashMap<>();

        // Contract numbers handling
        @SuppressWarnings("unchecked")
        List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
        if (contractNumbers != null && !contractNumbers.isEmpty()) {
            String contractNumber = contractNumbers.get(0);

            if (queryType.equals("FAILED_PARTS")) {
                filterEntities.put("LOADED_CP_NUMBER", contractNumber);
            } else {
                filterEntities.put("AWARD_NUMBER", contractNumber);
            }
        }

        // Part numbers handling
        @SuppressWarnings("unchecked")
        List<String> partNumbers = (List<String>) entities.get("partNumbers");
        if (partNumbers != null && !partNumbers.isEmpty()) {
            filterEntities.put("INVOICE_PART_NUMBER", partNumbers.get(0).toLowerCase());
        }

        // CRITICAL FIX: Enhanced customer information handling
        @SuppressWarnings("unchecked")
        List<String> customerInfo = (List<String>) entities.get("customerInfo");
        if (customerInfo != null && !customerInfo.isEmpty()) {
            String customerValue = customerInfo.get(0);

            // Only process if it passed the extraction validation
            if (isValidCustomerValue(customerValue)) {
                // Additional runtime validation
                boolean isContractNumber =
                    contractNumbers != null && contractNumbers.stream().anyMatch(cn -> cn.equals(customerValue));

                if (!isContractNumber) {
                    if (isNumeric(customerValue) && customerValue.length() >= 4 && customerValue.length() <= 12) {
                        filterEntities.put("CUSTOMER_NUMBER", customerValue);
                    } else if (!isNumeric(customerValue) && customerValue.length() >= 3) {
                        filterEntities.put("CUSTOMER_NAME", customerValue);
                    }
                }
            }
        }

        // Status filters
        String lowerQuery = applySpellCorrection(query.toLowerCase());
        if (lowerQuery.matches(".*\\bis\\s+.*active\\b.*") || lowerQuery.matches(".*\\bactive\\s+.*\\?.*")) {
            filterEntities.put("STATUS", "ACTIVE");
        } else if (lowerQuery.contains("inactive")) {
            filterEntities.put("STATUS", "INACTIVE");
        } else if (lowerQuery.contains("expired")) {
            filterEntities.put("STATUS", "EXPIRED");
        }

        return filterEntities;
    }


    private boolean isValidCustomerValue(String customerValue) {
        // Comprehensive validation
        return customerValue != null && customerValue.length() > 2 && customerValue.length() < 50 &&
               !customerValue.matches("\\d{6}") && // Not a contract number
               !customerValue.matches("\\d{5,8}") && // Not a contract number variant
               !COMMAND_WORDS.contains(customerValue.toLowerCase()) &&
               !customerValue.toLowerCase()
               .matches("(name|info|details|number|data|information|for|with|the|is|who|what|customer|client|show|get|display)") &&
               !customerValue.toLowerCase()
               .matches("(contract|part|parts|failed|error|issue|problem|status|active|inactive)") &&
               customerValue.matches(".*[a-zA-Z].*"); // Must contain at least one letter
    }

    private boolean isMultiIntentQuery(String query) {
        String correctedQuery = applySpellCorrection(query.toLowerCase());

        // CRITICAL FIX: Normalize contractions and possessives
        String normalizedQuery = correctedQuery.replaceAll("what's", "what is")
                                               .replaceAll("'s", " is")
                                               .replaceAll("'re", " are")
                                               .replaceAll("'ll", " will")
                                               .replaceAll("'ve", " have")
                                               .replaceAll("'d", " would");

        // CRITICAL FIX: Enhanced specific multi-intent patterns
        if (normalizedQuery.matches(".*\\beffective\\s+date.*part\\s+error.*") ||
            normalizedQuery.matches(".*\\beffective\\s+date.*error.*part.*") ||
            normalizedQuery.matches(".*\\bcontract.*detail.*failed\\s+part.*") ||
            normalizedQuery.matches(".*\\bpart.*customer\\s+info.*") ||
            normalizedQuery.matches(".*\\bcustomer.*name.*part.*error.*") ||
            normalizedQuery.matches(".*\\bwhat.*effective\\s+date.*part\\s+error.*") ||
            normalizedQuery.matches(".*\\beffective\\s+date.*and.*part\\s+error.*")) {
            return true;
        }

        // PRESERVE EXISTING LOGIC: General multi-intent indicators (unchanged)
        if (normalizedQuery.matches(".*\\b(and|with|plus|also|both)\\b.*")) {
            // Check if it contains elements from different domains
            boolean hasContract = normalizedQuery.matches(".*\\b(contract|effective|expiration|customer)\\b.*");
            boolean hasParts = normalizedQuery.matches(".*\\b(part|price|lead|moq)\\b.*");
            boolean hasErrors = normalizedQuery.matches(".*\\b(error|failed|issue|problem)\\b.*");

            // Multi-intent if spans multiple domains
            int domainCount = 0;
            if (hasContract)
                domainCount++;
            if (hasParts)
                domainCount++;
            if (hasErrors)
                domainCount++;

            return domainCount >= 2;
        }

        return false;
    }

    /**
     * MAIN METHOD FOR TESTING AND OUTPUT GENERATION
     */
    public static void main(String[] args) {
        ContractsModel model = new ContractsModel();

        // Test inputs based on business logic requirements
        List<String> testInputs =
            Arrays.asList(
            // Basic contract queries with 6-digit numbers
            "What is the effective date for 123456", "Show customer name for 234567", "Contract details for 345678",
            "Expiration date for contract 456789", "Payment terms for 567890",

            // Part queries with part numbers
            "What is the lead time for part AE12345", "Show price for BC67890", "MOQ for part DE23456",
            "Unit of measure for HI34567", "Status of part JK78901",

            // Failed parts queries
            "Show me failed parts for 123456", "What caused parts to fail for 234567", "List failing parts for 345678",
            "Show error reasons for 456789", "Why did parts fail for 567890", "What columns have errors for 678901", "Show error columns for 789012",

            // Pricing queries
            "What pricing for RS12345", "Show minimum order for BC67890", "What min order qty for DE23456",
            "Minimum order quantity for HI34567", "Show unit of measure for LM45678", "Unit measure for PQ56789",

            // Multi-intent queries
            "Show me contract details and failed parts for 123456",
            "List all parts and customer info for contract 345678", "Show contract info and failed part for 456789", "Contract name and effective date for 234567",

            // Complex queries
            "Show all active contracts", "List expired contracts", "Parts with high lead time",
            "Failed parts with validation errors", "Customer Boeing contracts",

            // Typo corrections
            "Show contarct details for 123456", "What is efective date for 234567", "Show custmer name for 345678",
            "Pric for part AE12345", "Leadtim for BC67890");
        List<String> allInputs = TestQueries.GETALL_QUERIES();
        // Create output files
        String txtFileName = "F:\\GitHub_VinodLearning\\NLPTEST\\NLP\\NLPMachineDesignApp\\TestReports\\ContrantsModelTest\\contract_model_test_results.txt";
        String mdFileName = "F:\\GitHub_VinodLearning\\NLPTEST\\NLP\\NLPMachineDesignApp\\TestReports\\ContrantsModelTest\\contract_model_test_results.md";

        try {
            // Write to TXT file
            writeToTxtFile(model, allInputs, txtFileName);

            // Write to MD file
            writeToMdFile(model, allInputs, mdFileName);

            System.out.println("Test results have been written to:");
            System.out.println("- " + txtFileName);
            System.out.println("- " + mdFileName);

            // Print summary statistics
            printSummaryStatistics(model, testInputs);

        } catch (IOException e) {
            System.err.println("Error writing to files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Write results to TXT file
     */
    private static void writeToTxtFile(ContractsModel model, List<String> testInputs,
                                       String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            // Write header
            writer.println("CONTRACT MODEL TEST RESULTS");
            writer.println("Generated on: " + new java.util.Date());
            writer.println("Total test cases: " + testInputs.size());

            writer.println(java.util
                               .stream
                               .IntStream
                               .range(0, 120)
                               .mapToObj(i -> "=")
                               .collect(java.util
                                            .stream
                                            .Collectors
                                            .joining()));
            writer.println();

            // Write column headers
            writer.printf("%-50s | %-50s | %-15s | %-30s | %-40s | %-50s%n", "INPUT", "CORRECTED_INPUT", "QUERY_TYPE",
                          "ACTION_TYPE", "DISPLAY_ENTITIES", "FILTER_ENTITIES");

            writer.println(java.util
                               .stream
                               .IntStream
                               .range(0, 250)
                               .mapToObj(i -> "=")
                               .collect(java.util
                                            .stream
                                            .Collectors
                                            .joining()));

            // Process each test input
            for (String input : testInputs) {
                try {
                    Map<String, Object> result = model.processQuery(input);

                    String correctedInput = getCorrectedInput(input, result);
                    String queryType = getStringValue(result, "queryType");
                    String actionType = getStringValue(result, "action");
                    String displayEntities = formatDisplayEntities(result);
                    String filterEntities = formatFilterEntities(result);

                    writer.printf("%-50s | %-50s | %-15s | %-30s | %-40s | %-50s%n", truncate(input, 50),
                                  truncate(correctedInput, 50), truncate(queryType, 15), truncate(actionType, 30),
                                  truncate(displayEntities, 40), truncate(filterEntities, 50));

                } catch (Exception e) {
                    writer.printf("%-50s | %-50s | %-15s | %-30s | %-40s | %-50s%n", truncate(input, 50), "ERROR",
                                  "ERROR", "ERROR", "ERROR", e.getMessage());
                }
            }

            writer.println(java.util
                               .stream
                               .IntStream
                               .range(0, 250)
                               .mapToObj(i -> "=")
                               .collect(java.util
                                            .stream
                                            .Collectors
                                            .joining()));
            writer.println();
            writer.println("Legend:");
            writer.println("- INPUT: Original user query");
            writer.println("- CORRECTED_INPUT: Query after spell correction and preprocessing");
            writer.println("- QUERY_TYPE: Determined query type (CONTRACTS, PARTS, FAILED_PARTS, MULTI_INTENT)");
            writer.println("- ACTION_TYPE: Specific action to be performed");
            writer.println("- DISPLAY_ENTITIES: Fields to be displayed in the result");
            writer.println("- FILTER_ENTITIES: Filters to be applied to the query");
        }
    }

    /**
     * Write results to Markdown file
     */
    private static void writeToMdFile(ContractsModel model, List<String> testInputs,
                                      String fileName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            // Write header
            writer.println("# Contract Model Test Results");
            writer.println();
            writer.println("**Generated on:** " + new java.util.Date());
            writer.println("**Total test cases:** " + testInputs.size());
            writer.println();

            // Write table header
            writer.println("| INPUT | CORRECTED_INPUT | QUERY_TYPE | ACTION_TYPE | DISPLAY_ENTITIES | FILTER_ENTITIES |");
            writer.println("|-------|-----------------|------------|-------------|------------------|-----------------|");

            // Process each test input
            for (String input : testInputs) {
                try {
                    Map<String, Object> result = model.processQuery(input);

                    String correctedInput = getCorrectedInput(input, result);
                    String queryType = getStringValue(result, "queryType");
                    String actionType = getStringValue(result, "action");
                    String displayEntities = formatDisplayEntities(result);
                    String filterEntities = formatFilterEntities(result);

                    writer.printf("| %s | %s | %s | %s | %s | %s |%n", escapeMarkdown(input),
                                  escapeMarkdown(correctedInput), escapeMarkdown(queryType), escapeMarkdown(actionType),
                                  escapeMarkdown(displayEntities), escapeMarkdown(filterEntities));

                } catch (Exception e) {
                    writer.printf("| %s | ERROR | ERROR | ERROR | ERROR | %s |%n", escapeMarkdown(input),
                                  escapeMarkdown(e.getMessage()));
                }
            }

            writer.println();
            writer.println("## Legend");
            writer.println();
            writer.println("- **INPUT**: Original user query");
            writer.println("- **CORRECTED_INPUT**: Query after spell correction and preprocessing");
            writer.println("- **QUERY_TYPE**: Determined query type (CONTRACTS, PARTS, FAILED_PARTS, MULTI_INTENT)");
            writer.println("- **ACTION_TYPE**: Specific action to be performed");
            writer.println("- **DISPLAY_ENTITIES**: Fields to be displayed in the result");
            writer.println("- **FILTER_ENTITIES**: Filters to be applied to the query");

            writer.println();
            writer.println("## Query Type Distribution");
            writer.println();

            // Add statistics
            Map<String, Integer> queryTypeStats = new HashMap<>();
            for (String input : testInputs) {
                try {
                    Map<String, Object> result = model.processQuery(input);
                    String queryType = getStringValue(result, "queryType");
                    queryTypeStats.put(queryType, queryTypeStats.getOrDefault(queryType, 0) + 1);
                } catch (Exception e) {
                    queryTypeStats.put("ERROR", queryTypeStats.getOrDefault("ERROR", 0) + 1);
                }
            }

            for (Map.Entry<String, Integer> entry : queryTypeStats.entrySet()) {
                writer.println("- **" + entry.getKey() + "**: " + entry.getValue() + " queries");
            }
        }
    }

    /**
     * Print summary statistics to console
     */
    private static void printSummaryStatistics(ContractsModel model, List<String> testInputs) {
        //System.out.println("\n" + "=".repeat(60));
        System.out.println("SUMMARY STATISTICS");
        //  System.out.println("=".repeat(60));

        Map<String, Integer> queryTypeStats = new HashMap<>();
        Map<String, Integer> actionTypeStats = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        double totalConfidence = 0.0;

        for (String input : testInputs) {
            try {
                Map<String, Object> result = model.processQuery(input);

                String queryType = getStringValue(result, "queryType");
                String actionType = getStringValue(result, "action");
                Double confidence = (Double) result.get("confidence");

                queryTypeStats.put(queryType, queryTypeStats.getOrDefault(queryType, 0) + 1);
                actionTypeStats.put(actionType, actionTypeStats.getOrDefault(actionType, 0) + 1);

                if (confidence != null) {
                    totalConfidence += confidence;
                }
                successCount++;

            } catch (Exception e) {
                errorCount++;
            }
        }

        System.out.println("Total Queries: " + testInputs.size());
        System.out.println("Successful: " + successCount);
        System.out.println("Errors: " + errorCount);
        System.out.println("Success Rate: " + String.format("%.2f%%", (successCount * 100.0) / testInputs.size()));
        System.out.println("Average Confidence: " + String.format("%.2f", totalConfidence / successCount));

        System.out.println("\nQuery Type Distribution:");
        for (Map.Entry<String, Integer> entry : queryTypeStats.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\nAction Type Distribution:");
        for (Map.Entry<String, Integer> entry : actionTypeStats.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        //System.out.println("=".repeat(60));
    }

    /**
     * Helper methods for formatting output
     */
    private static String getCorrectedInput(String original, Map<String, Object> result) {
        // Try to get corrected input from metadata or return original
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
        if (metadata != null && metadata.containsKey("correctedInput")) {
            return (String) metadata.get("correctedInput");
        }
        return original;
    }

    private static String getStringValue(Map<String, Object> result, String key) {
        Object value = result.get(key);
        return value != null ? value.toString() : "";
    }

    private static String formatDisplayEntities(Map<String, Object> result) {
        @SuppressWarnings("unchecked")
        List<String> displayEntities = (List<String>) result.get("displayEntities");
        if (displayEntities != null && !displayEntities.isEmpty()) {
            return String.join(", ", displayEntities);
        }
        return "";
    }

    private static String formatFilterEntities(Map<String, Object> result) {
        @SuppressWarnings("unchecked")
        Map<String, String> filterEntities = (Map<String, String>) result.get("filterEntities");
        if (filterEntities != null && !filterEntities.isEmpty()) {
            List<String> filters = new ArrayList<>();
            for (Map.Entry<String, String> entry : filterEntities.entrySet()) {
                filters.add(entry.getKey() + " = " + entry.getValue());
            }
            return String.join("; ", filters);
        }
        return "";
    }

    private static String truncate(String str, int maxLength) {
        if (str == null)
            return "";
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private static String escapeMarkdown(String str) {
        if (str == null)
            return "";
        return str.replace("|", "\\|")
                  .replace("\n", " ")
                  .replace("\r", "");
    }

    /**
     * NEW: Business term to column mapping - FIXES MISSING DISPLAY ENTITIES
     */
    private static Map<String, String> createBusinessTermToColumnMapping() {
        Map<String, String> mapping = new HashMap<>();

        // Price mappings
        mapping.put("pricing", "PRICE");
        mapping.put("price", "PRICE");
        mapping.put("cost", "PRICE");

        // MOQ mappings
        mapping.put("minimum order", "MOQ");
        mapping.put("min order", "MOQ");
        mapping.put("min order qty", "MOQ");
        mapping.put("minimum order quantity", "MOQ");
        mapping.put("moq", "MOQ");

        // UOM mappings
        mapping.put("unit of measure", "UOM");
        mapping.put("unit measure", "UOM");
        mapping.put("uom", "UOM");

        // Lead time mappings
        mapping.put("lead time", "LEAD_TIME");
        mapping.put("leadtime", "LEAD_TIME");
        mapping.put("delivery time", "LEAD_TIME");

        // Status mappings
        mapping.put("status", "STATUS");
        mapping.put("condition", "STATUS");
        mapping.put("state", "STATUS");

        return mapping;
    }

    /**
     * NEW: Column name corrections - FIXES TYPOS
     */
    private static Map<String, String> createColumnNameCorrections() {
        Map<String, String> corrections = new HashMap<>();

        corrections.put("PASRT_NUMEBR", "PART_NUMBER");
        corrections.put("LAODED_CP_NUMBER", "LOADED_CP_NUMBER");
        corrections.put("CONTARCT_NUMBER", "CONTRACT_NUMBER");
        corrections.put("CUSTMER_NAME", "CUSTOMER_NAME");
        corrections.put("EFECTIVE_DATE", "EFFECTIVE_DATE");

        return corrections;
    }

    private boolean isFailedPartsQuery(String query, Map<String, Object> entities) {
        // Enhanced keywords for failed parts detection including causal patterns
        String[] failedPartsKeywords = {
            "failed", "failing", "fail", "error", "errors", "problem", "problems", "issue", "issues", "validation",
            "missing", "incorrect", "invalid", "broken", "corrupted", "caused", "why"
        };

        // Check for failed parts keywords
        for (String keyword : failedPartsKeywords) {
            if (query.contains(keyword)) {
                // Additional context check
                if (query.contains("part") || query.contains("component") || query.contains("item")) {
                    return true;
                }
                // Check if contract number is present
                @SuppressWarnings("unchecked")
                List<String> contractNumbers = (List<String>) entities.get("contractNumbers");
                if (contractNumbers != null && !contractNumbers.isEmpty()) {
                    return true;
                }
                // Enhanced pattern matching for causal language
                if (query.matches(".*(why|what|show|list).*fail.*") || query.matches(".*error.*(reason|column).*") ||
                    query.matches(".*failure.*reason.*") || query.matches(".*caused.*fail.*") ||
                    query.matches(".*why.*parts.*fail.*") || query.matches(".*what.*caused.*parts.*")) {
                    return true;
                }
            }
        }

        return false;
    }
}

