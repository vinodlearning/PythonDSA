package com.oracle.view.source;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Enhanced Date Extractor for handling various date patterns in contract queries
 * Supports month ranges, year ranges, and flexible date formats
 */
public class EnhancedDateExtractor {
    
    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern MONTH_PATTERN = Pattern.compile("\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\w*\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("between\\s+(.+?)\\s+(?:and|to)\\s+(.+?)(?:\\s|$)", Pattern.CASE_INSENSITIVE);
    
    static {
        MONTH_MAP.put("jan", 1);
        MONTH_MAP.put("january", 1);
        MONTH_MAP.put("feb", 2);
        MONTH_MAP.put("february", 2);
        MONTH_MAP.put("mar", 3);
        MONTH_MAP.put("march", 3);
        MONTH_MAP.put("apr", 4);
        MONTH_MAP.put("april", 4);
        MONTH_MAP.put("may", 5);
        MONTH_MAP.put("jun", 6);
        MONTH_MAP.put("june", 6);
        MONTH_MAP.put("jul", 7);
        MONTH_MAP.put("july", 7);
        MONTH_MAP.put("aug", 8);
        MONTH_MAP.put("august", 8);
        MONTH_MAP.put("sep", 9);
        MONTH_MAP.put("september", 9);
        MONTH_MAP.put("oct", 10);
        MONTH_MAP.put("october", 10);
        MONTH_MAP.put("nov", 11);
        MONTH_MAP.put("november", 11);
        MONTH_MAP.put("dec", 12);
        MONTH_MAP.put("december", 12);
    }
    
    /**
     * Extract date information from input string
     */
    public static DateExtractionResult extractDateInfo(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new DateExtractionResult();
        }
        
        String lowerInput = input.toLowerCase();
        DateExtractionResult result = new DateExtractionResult();
        
        // Extract year information
        extractYearInfo(lowerInput, result);
        
        // Extract month information
        extractMonthInfo(lowerInput, result);
        
        // Extract date ranges
        extractDateRanges(lowerInput, result);
        
        // Extract specific dates
        extractSpecificDates(lowerInput, result);
        
        // Extract relative dates
        extractRelativeDates(lowerInput, result);
        
        return result;
    }
    
    /**
     * Extract year information
     */
    private static void extractYearInfo(String lowerInput, DateExtractionResult result) {
        Matcher yearMatcher = YEAR_PATTERN.matcher(lowerInput);
        while (yearMatcher.find()) {
            String year = yearMatcher.group();
            int yearValue = Integer.parseInt(year);
            
            if (lowerInput.contains("after") || lowerInput.contains("since")) {
                result.setAfterYear(yearValue);
                result.setTemporalOperation("AFTER");
            } else if (lowerInput.contains("before") || lowerInput.contains("until")) {
                result.setBeforeYear(yearValue);
                result.setTemporalOperation("BEFORE");
            } else if (lowerInput.contains("in") || lowerInput.contains("during") || lowerInput.contains("created")) {
                result.setInYear(yearValue);
                result.setTemporalOperation("IN");
            }
        }
    }
    
    /**
     * Extract month information
     */
    private static void extractMonthInfo(String lowerInput, DateExtractionResult result) {
        Matcher monthMatcher = MONTH_PATTERN.matcher(lowerInput);
        List<String> months = new ArrayList<>();
        
        while (monthMatcher.find()) {
            String month = monthMatcher.group().toLowerCase();
            months.add(month);
        }
        
        if (months.size() >= 2) {
            // Month range detected
            String startMonth = months.get(0);
            String endMonth = months.get(1);
            result.setStartMonth(MONTH_MAP.get(startMonth));
            result.setEndMonth(MONTH_MAP.get(endMonth));
            result.setTemporalOperation("BETWEEN");
        } else if (months.size() == 1) {
            // Single month detected
            String month = months.get(0);
            result.setStartMonth(MONTH_MAP.get(month));
            result.setEndMonth(MONTH_MAP.get(month));
        }
    }
    
    /**
     * Extract date ranges
     */
    private static void extractDateRanges(String lowerInput, DateExtractionResult result) {
        Matcher rangeMatcher = DATE_RANGE_PATTERN.matcher(lowerInput);
        if (rangeMatcher.find()) {
            String startPart = rangeMatcher.group(1).trim();
            String endPart = rangeMatcher.group(2).trim();
            
            // Handle year ranges: "between 2024, 2025" or "between 2024 to 2025"
            if (startPart.matches("\\d{4}") && endPart.matches("\\d{4}")) {
                result.setStartYear(Integer.parseInt(startPart));
                result.setEndYear(Integer.parseInt(endPart));
                result.setTemporalOperation("BETWEEN");
            }
            
            // Handle month ranges: "between jan to june"
            Matcher startMonthMatcher = MONTH_PATTERN.matcher(startPart);
            Matcher endMonthMatcher = MONTH_PATTERN.matcher(endPart);
            
            if (startMonthMatcher.find() && endMonthMatcher.find()) {
                String startMonth = startMonthMatcher.group().toLowerCase();
                String endMonth = endMonthMatcher.group().toLowerCase();
                
                result.setStartMonth(MONTH_MAP.get(startMonth));
                result.setEndMonth(MONTH_MAP.get(endMonth));
                result.setTemporalOperation("BETWEEN");
                
                // If no year specified, use current year
                if (result.getStartYear() == null) {
                    int currentYear = LocalDate.now().getYear();
                    result.setStartYear(currentYear);
                    result.setEndYear(currentYear);
                }
            }
        }
    }
    
    /**
     * Extract specific dates
     */
    private static void extractSpecificDates(String lowerInput, DateExtractionResult result) {
        // Handle specific date formats
        Pattern specificDatePattern = Pattern.compile("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{2,4})");
        Matcher dateMatcher = specificDatePattern.matcher(lowerInput);
        
        if (dateMatcher.find()) {
            int day = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            int year = Integer.parseInt(dateMatcher.group(3));
            
            // Normalize 2-digit years
            if (year < 100) {
                year += 2000;
            }
            
            result.setSpecificDate(LocalDate.of(year, month, day));
        }
    }
    
    /**
     * Extract relative dates
     */
    private static void extractRelativeDates(String lowerInput, DateExtractionResult result) {
        LocalDate now = LocalDate.now();
        
        if (lowerInput.contains("today") || lowerInput.contains("now")) {
            result.setSpecificDate(now);
        } else if (lowerInput.contains("yesterday")) {
            result.setSpecificDate(now.minusDays(1));
        } else if (lowerInput.contains("last month")) {
            result.setSpecificDate(now.minusMonths(1));
        } else if (lowerInput.contains("this month")) {
            result.setStartMonth(now.getMonthValue());
            result.setEndMonth(now.getMonthValue());
            result.setStartYear(now.getYear());
            result.setEndYear(now.getYear());
        } else if (lowerInput.contains("this year")) {
            result.setStartYear(now.getYear());
            result.setEndYear(now.getYear());
        } else if (lowerInput.contains("last year")) {
            result.setStartYear(now.getYear() - 1);
            result.setEndYear(now.getYear() - 1);
        }
        
        // Handle "till date" and "current date" scenarios
        if (lowerInput.contains("till date") || lowerInput.contains("current date") || lowerInput.contains("till now")) {
            // If we have a start date but no end date, set end date to current date
            if (result.getStartYear() != null && result.getEndYear() == null) {
                result.setEndYear(now.getYear());
            }
            if (result.getStartMonth() != null && result.getEndMonth() == null) {
                result.setEndMonth(now.getMonthValue());
            }
            
            // For "after" scenarios without end date, set end date to current date
            if (result.getAfterYear() != null && result.getEndYear() == null) {
                result.setEndYear(now.getYear());
                result.setTemporalOperation("AFTER_TO_CURRENT");
            }
        }
    }
    
    /**
     * Build SQL date filter based on extraction result
     */
    public static String buildDateFilter(DateExtractionResult result) {
        if (result == null) {
            return null;
        }
        
        StringBuilder filter = new StringBuilder();
        
        if (result.getSpecificDate() != null) {
            // Specific date filter
            filter.append("CREATE_DATE = DATE '").append(result.getSpecificDate()).append("'");
        } else if (result.getInYear() != null) {
            // Year filter
            filter.append("EXTRACT(YEAR FROM CREATE_DATE) = ").append(result.getInYear());
        } else if (result.getAfterYear() != null) {
            // After year filter
            filter.append("EXTRACT(YEAR FROM CREATE_DATE) > ").append(result.getAfterYear());
        } else if (result.getBeforeYear() != null) {
            // Before year filter
            filter.append("EXTRACT(YEAR FROM CREATE_DATE) < ").append(result.getBeforeYear());
        } else if (result.getStartYear() != null && result.getEndYear() != null) {
            // Year range filter
            filter.append("EXTRACT(YEAR FROM CREATE_DATE) BETWEEN ")
                  .append(result.getStartYear()).append(" AND ").append(result.getEndYear());
        } else if (result.getStartMonth() != null && result.getEndMonth() != null) {
            // Month range filter
            int currentYear = result.getStartYear() != null ? result.getStartYear() : LocalDate.now().getYear();
            
            LocalDate startDate = LocalDate.of(currentYear, result.getStartMonth(), 1);
            LocalDate endDate = LocalDate.of(currentYear, result.getEndMonth(), 
                                           LocalDate.of(currentYear, result.getEndMonth(), 1).lengthOfMonth());
            
            filter.append("CREATE_DATE BETWEEN DATE '").append(startDate).append("' AND DATE '").append(endDate).append("'");
        }
        
        return filter.length() > 0 ? filter.toString() : null;
    }
    
    /**
     * Result class for date extraction
     */
    public static class DateExtractionResult {
        private LocalDate specificDate;
        private Integer inYear;
        private Integer afterYear;
        private Integer beforeYear;
        private Integer startYear;
        private Integer endYear;
        private Integer startMonth;
        private Integer endMonth;
        private String temporalOperation;
        
        // Getters and setters
        public LocalDate getSpecificDate() { return specificDate; }
        public void setSpecificDate(LocalDate specificDate) { this.specificDate = specificDate; }
        
        public Integer getInYear() { return inYear; }
        public void setInYear(Integer inYear) { this.inYear = inYear; }
        
        public Integer getAfterYear() { return afterYear; }
        public void setAfterYear(Integer afterYear) { this.afterYear = afterYear; }
        
        public Integer getBeforeYear() { return beforeYear; }
        public void setBeforeYear(Integer beforeYear) { this.beforeYear = beforeYear; }
        
        public Integer getStartYear() { return startYear; }
        public void setStartYear(Integer startYear) { this.startYear = startYear; }
        
        public Integer getEndYear() { return endYear; }
        public void setEndYear(Integer endYear) { this.endYear = endYear; }
        
        public Integer getStartMonth() { return startMonth; }
        public void setStartMonth(Integer startMonth) { this.startMonth = startMonth; }
        
        public Integer getEndMonth() { return endMonth; }
        public void setEndMonth(Integer endMonth) { this.endMonth = endMonth; }
        
        public String getTemporalOperation() { return temporalOperation; }
        public void setTemporalOperation(String temporalOperation) { this.temporalOperation = temporalOperation; }
        
        public boolean hasDateInfo() {
            return specificDate != null || inYear != null || afterYear != null || 
                   beforeYear != null || startYear != null || endYear != null || 
                   startMonth != null || endMonth != null;
        }
        
        @Override
        public String toString() {
            return "DateExtractionResult{" +
                   "specificDate=" + specificDate +
                   ", inYear=" + inYear +
                   ", afterYear=" + afterYear +
                   ", beforeYear=" + beforeYear +
                   ", startYear=" + startYear +
                   ", endYear=" + endYear +
                   ", startMonth=" + startMonth +
                   ", endMonth=" + endMonth +
                   ", temporalOperation='" + temporalOperation + '\'' +
                   '}';
        }
    }
} 