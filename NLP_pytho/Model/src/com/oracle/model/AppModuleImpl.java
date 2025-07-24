
package com.oracle.model;

import com.oracle.model.common.AppModule;

import java.math.BigDecimal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.text.SimpleDateFormat;

import java.util.*;

import oracle.jbo.server.ApplicationModuleImpl;
import oracle.jbo.server.ViewObjectImpl;

public class AppModuleImpl extends ApplicationModuleImpl implements AppModule {
    public AppModuleImpl() {
    }

    public Map<String, Object> executeDynamicQuery(String sqlQuery, String[] paramValues, String[] paramTypes) {
        System.out.println("===============executeDynamicQuery=============" + sqlQuery);
        System.out.println("Parameters--->");
        for (String s : paramValues)
            System.out.println(s);
        System.out.println("Paramtypes--->");
        for (String s : paramTypes)
            System.out.println(s);

        Map<String, Object> result = new HashMap<>();
        PreparedStatement stmt = null;
        ResultSet resultset = null;

        try {
            stmt = getDBTransaction().createPreparedStatement(sqlQuery, 0);

            // Set parameters based on types
            if (paramValues != null && paramTypes != null) {
                for (int i = 0; i < paramValues.length && i < paramTypes.length; i++) {
                    setParameterByType(stmt, i + 1, paramValues[i], paramTypes[i]);
                    // Print debug info for each parameter
                    System.out.println("DEBUG: Setting parameter " + (i + 1) + " | Type: " + paramTypes[i] +
                                       " | Value: " + paramValues[i]);
                }
            }

            // Build debug SQL with parameters substituted
            String debugSql = sqlQuery;
            if (paramValues != null && paramTypes != null) {
                for (int i = 0; i < paramValues.length && i < paramTypes.length; i++) {
                    String replacement =
                        "String".equalsIgnoreCase(paramTypes[i]) ? "'" + paramValues[i] + "'" : paramValues[i];
                    debugSql = debugSql.replaceFirst("\\?", replacement);
                }
            }
            System.out.println("DEBUG: Final SQL to execute: " + debugSql);

            resultset = stmt.executeQuery();

            // Convert ResultSet to List of Maps
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metaData = resultset.getMetaData();
            int columnCount = metaData.getColumnCount();
System.out.println("Count of rows==============>"+columnCount);
            while (resultset.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultset.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }

            result.put("success", true);
            result.put("rows", rows);
            result.put("rowCount", rows.size());
            result.put("columnCount", columnCount);

            // Add column metadata
            List<String> columnNames = new ArrayList<>();
            List<String> columnTypes = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
                columnTypes.add(metaData.getColumnTypeName(i));
            }
            result.put("columnNames", columnNames);
            result.put("columnTypes", columnTypes);

        } catch (SQLException ex) {
            result.put("success", false);
            result.put("error", ex.getMessage());
            result.put("rows", new ArrayList<>());
            result.put("rowCount", 0);
        } finally {
            try {
                if (resultset != null)
                    resultset.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                // Log error
            }
        }
        return result;
    }

    private void setParameterByType(PreparedStatement stmt, int index, String value, String type) throws SQLException {
        if (value == null || "null".equalsIgnoreCase(value)) {
            stmt.setNull(index, java.sql
                                    .Types
                                    .VARCHAR);
            return;
        }
        switch (type.toUpperCase()) {
        case "NUMBER":
        case "INTEGER":
            try {
                stmt.setBigDecimal(index, new BigDecimal(value));
            } catch (NumberFormatException e) {
                stmt.setString(index, value);
            }
            break;
        case "DATE":
            try {
                stmt.setDate(index, java.sql
                                        .Date
                                        .valueOf(value));
            } catch (Exception e) {
                stmt.setString(index, value);
            }
            break;
        default: // STRING
            stmt.setString(index, value);
        }
    }

    public Map<String, Object> buildDynamicSQL(String actionType, String filterAttributes, String filterValues,
                                               String filterOperations, String displayColumns) {
        System.out.println("starting of buildDynamicSQL=======>");
        Map<String, Object> result = new HashMap<>();

        try {
            StringBuilder sql = new StringBuilder();

            // SELECT clause
            sql.append("SELECT ");
            if (displayColumns != null && !displayColumns.trim().isEmpty()) {
                sql.append(displayColumns);
            } else {
                sql.append("*");
            }

            // FROM clause based on action type
            sql.append(" FROM ");
            String tableName = getTableNameByActionType(actionType);
            sql.append(tableName);

            // WHERE clause
            String[] attributes = filterAttributes != null ? filterAttributes.split(",") : null;
            String[] values = filterValues != null ? filterValues.split(",") : null;
            String[] operations = filterOperations != null ? filterOperations.split(",") : null;

            if (attributes != null && attributes.length > 0) {
                sql.append(" WHERE ");
                for (int i = 0; i < attributes.length; i++) {
                    if (i > 0)
                        sql.append(" AND ");
                    String operation = (operations != null && i < operations.length) ? operations[i] : "=";
                    sql.append(attributes[i].trim())
                       .append(" ")
                       .append(operation)
                       .append(" ?");
                }
            }

            result.put("success", true);
            result.put("sqlQuery", sql.toString());
            result.put("tableName", tableName);
            result.put("parameterCount", attributes != null ? attributes.length : 0);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("sqlQuery", "");
        }

        return result;
    }

    private String getTableNameByActionType(String actionType) {
        switch (actionType) {
        case "contracts_by_contractnumber":
        case "contracts_by_filter":
        case "create_contract":
        case "update_contract":
            return "CCT_CONTRACTS_TMG";
        case "parts_by_contract_number":
        case "parts_by_part_number":
        case "parts_by_filter":
            return "CCT_PARTS_TMG";
        case "parts_failed_by_contract_number":
            return "CCT_FAILED_PARTS_TMG";
        default:
            return "CCT_CONTRACTS_TMG";
        }
    }

    public Map<String, Object> executeNLPQuery(String actionType, String filterAttributes, String filterValues,
                                               String filterOperations, String displayColumns) {
        System.out.println("===============executeDynamicQuery============= actionType" + actionType +
                           "===========filterAttributes" + filterAttributes + "============filterValues" +
                           filterValues);
        Map<String, Object> result = new HashMap<>();
        PreparedStatement stmt = null;
        ResultSet resultset = null;

        try {
            // Build SQL using existing method
            Map<String, Object> sqlResult =
                buildDynamicSQL(actionType, filterAttributes, filterValues, filterOperations, displayColumns);

            if (!(Boolean) sqlResult.get("success")) {
                result.put("success", false);
                result.put("error", "SQL building failed: " + sqlResult.get("error"));
                return result;
            }

            String sqlQuery = (String) sqlResult.get("sqlQuery");
            System.out.println("Generated SQL Query: " + sqlQuery);

            // Execute query
            stmt = getDBTransaction().createPreparedStatement(sqlQuery, 0);

            // Set parameters
            if (filterValues != null && !filterValues.trim().isEmpty()) {
                String[] values = filterValues.split(",");
                for (int i = 0; i < values.length; i++) {
                    stmt.setString(i + 1, values[i].trim());
                }
            }

            resultset = stmt.executeQuery();

            // Convert ResultSet to List of Maps (raw data - no formatting)
            List<Map<String, Object>> dataRows = new ArrayList<>();

            // Get column names
            java.sql.ResultSetMetaData metaData = resultset.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columnNames = new ArrayList<>();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            // Get data rows
            while (resultset.next()) {
                Map<String, Object> row = new HashMap<>();
                for (String columnName : columnNames) {
                    Object value = resultset.getObject(columnName);
                    row.put(columnName, value != null ? value.toString() : "N/A");
                }
                dataRows.add(row);
            }

            result.put("success", true);
            result.put("data", dataRows);
            result.put("columnNames", columnNames);
            result.put("rowCount", dataRows.size());
            result.put("sqlQuery", sqlQuery);

        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("success", false);
            result.put("error", ex.getMessage());
            result.put("data", new ArrayList<>());
            result.put("columnNames", new ArrayList<>());
            result.put("rowCount", 0);
        } finally {
            try {
                if (resultset != null)
                    resultset.close();
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public Map pullContractDatesByAwardNumber(String awardNumber) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> dates = new HashMap<>();
        String query =
            "SELECT award_number, date_of_signature, effective_date, expiration_date, price_expiration_date, flow_down_date " +
            "FROM cct_award_date_management " + "WHERE award_number = ?";

        PreparedStatement stmt = null;
        ResultSet resultset = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");

        try {
            stmt = getDBTransaction().createPreparedStatement(query, 0);
            stmt.setString(1, awardNumber);
            resultset = stmt.executeQuery();

            if (resultset.next()) {
                // Get each date column and format as MM-dd-yy
                String awardNumberDate = resultset.getString("award_number");
                if (awardNumberDate != null) {
                    dates.put("AWARD_NUMBER", awardNumberDate);
                }

                Date dateOfSignature = resultset.getDate("date_of_signature");
                if (dateOfSignature != null) {
                    dates.put("DATE_OF_SIGNATURE", dateFormat.format(dateOfSignature));
                }

                Date effectiveDate = resultset.getDate("effective_date");
                if (effectiveDate != null) {
                    dates.put("EFFECTIVE_DATE", dateFormat.format(effectiveDate));
                }

                Date expirationDate = resultset.getDate("expiration_date");
                if (expirationDate != null) {
                    dates.put("EXPIRATION_DATE", dateFormat.format(expirationDate));
                }

                Date priceExpirationDate = resultset.getDate("price_expiration_date");
                if (priceExpirationDate != null) {
                    dates.put("PRICE_EXPIRATION_DATE", dateFormat.format(priceExpirationDate));
                }

                Date flowDownDate = resultset.getDate("flow_down_date");
                if (flowDownDate != null) {
                    dates.put("FLOW_DOWN_DATE", dateFormat.format(flowDownDate));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (resultset != null)
                    resultset.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
                response.put("success", false);
                response.put("error",
                             "exception while pulling the data management data for the contarct #" + awardNumber);
            }
        }
        response.put("success", true);
        response.put("dates", dates);
        return response;
    }

    public ViewObjectImpl getCctContractsTmgView1() {
        return (ViewObjectImpl) findViewObject("CctContractsTmgView1");
    }

    public Map pullContractsByFilters(String userName, List dateFilters, String query) {
        Map<String, List> result = new HashMap<>();
        PreparedStatement stmt = null;
        ResultSet resultset = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");

        try {
            stmt = getDBTransaction().createPreparedStatement(query, 0);
            resultset = stmt.executeQuery();

            while (resultset.next()) { // Changed from 'if' to 'while'
                Map data = new HashMap();
                String awardRep = resultset.getString("AWARD_REP");

                String awardNumberDate = resultset.getString("award_number");
                if (awardNumberDate != null) {
                    data.put("AWARD_NUMBER", awardNumberDate);
                }

                String contractName = resultset.getString("CONTRACT_NAME");
                if (contractName != null) {
                    data.put("CONTRACT_NAME", contractName);
                }

                String customerName = resultset.getString("CUSTOMER_NAME");
                if (customerName != null) { // Fixed: was contractName
                    data.put("CUSTOMER_NAME", customerName);
                }

                Date effectiveDate = resultset.getDate("EFFECTIVE_DATE");
                if (effectiveDate != null) {
                    data.put("EFFECTIVE_DATE", dateFormat.format(effectiveDate));
                }

                Date expirationDate = resultset.getDate("expiration_date");
                if (expirationDate != null) {
                    data.put("EXPIRATION_DATE", dateFormat.format(expirationDate));
                }

                Date CREATE_DATE = resultset.getDate("CREATE_DATE");
                if (CREATE_DATE != null) {
                    data.put("CREATE_DATE", dateFormat.format(CREATE_DATE));
                }

                if (result.containsKey(awardRep)) {
                    List dataLst = result.get(awardRep);
                    dataLst.add(data);
                } else {
                    List awardData = new ArrayList();
                    awardData.add(data);
                    result.put(awardRep, awardData);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (resultset != null)
                    resultset.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public Map<String, Object> pullCustomerDetails(String customerNumber) {
        System.out.println("pullCustomerDetails===================>"+customerNumber);
        Map<String, Object> result = new HashMap<>();
        PreparedStatement stmt = null;
        ResultSet resultset = null;

        String query =
            "SELECT CUST_ID, CUSTOMER_NO, CUSTOMER_NAME, ACCOUNT_TYPE, " +
            "SALES_REP_ID, SALES_OWNER, SALES_TEAM, SALES_MANAGER, " +
            "CURRENCY_CODE, PAYMENT_TERMS, IS_ACTIVE, AWARDREP " + "FROM HR.CCT_CUTSOMERS_TGM " +
            "WHERE CUSTOMER_NO = ? AND IS_ACTIVE = 'Y'";

        try {
            stmt = getDBTransaction().createPreparedStatement(query, 0);
            stmt.setString(1, customerNumber);
            resultset = stmt.executeQuery();

            if (resultset.next()) {
                result.put("exists", true);
                result.put("CUST_ID", resultset.getString("CUST_ID"));
                result.put("CUSTOMER_NO", resultset.getString("CUSTOMER_NO"));
                result.put("CUSTOMER_NAME", resultset.getString("CUSTOMER_NAME"));
                result.put("ACCOUNT_TYPE", resultset.getString("ACCOUNT_TYPE"));
                result.put("SALES_REP_ID", resultset.getString("SALES_REP_ID"));
                result.put("SALES_OWNER", resultset.getString("SALES_OWNER"));
                result.put("SALES_TEAM", resultset.getString("SALES_TEAM"));
                result.put("SALES_MANAGER", resultset.getString("SALES_MANAGER"));
                result.put("CURRENCY_CODE", resultset.getString("CURRENCY_CODE"));
                result.put("PAYMENT_TERMS", resultset.getString("PAYMENT_TERMS"));
                result.put("AWARDREP", resultset.getString("AWARDREP"));
            } else {
                result.put("exists", false);
                result.put("message", "Customer number " + customerNumber + " not found or inactive");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            result.put("exists", false);
            result.put("error", "Database error: " + ex.getMessage());
        } finally {
            try {
                if (resultset != null)
                    resultset.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }


    public Map<String, Object> createContractByBOT(Map<String, Object> contractData, String createdBy) {
        System.out.println("createContractByBOT==============>");
        System.out.println(contractData);
        System.out.println("createContractByBOT================>");
        Map<String, Object> result = new HashMap<>();
        PreparedStatement stmt = null;

        // Define allowed columns
        List<String> allowedColumns =
            Arrays.asList("CUSTOMER_NUMBER", "CONTRACT_NAME", "CUSTOMER_NAME", "PRICE_LIST", "TITLE", "COMMENTS",
                          "DESCRIPTION");

        // Build dynamic insert query based on provided data
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        // Add provided contract data (only allowed columns)
        for (String key : contractData.keySet()) {
            if (allowedColumns.contains(key)) {
                if (columns.length() > 0) {
                    columns.append(", ");
                    values.append(", ");
                }
                columns.append(key);
                values.append("?");
                parameters.add(contractData.get(key));
            }
        }

        // Add system fields with AWARD_SEQ for AWARD_NUMBER
        if (columns.length() > 0) {
            columns.append(", ");
            values.append(", ");
        }
        columns.append("AWARD_NUMBER, CREATE_DATE, CREATED_BY, UPDATED_BY, UPDATED_DATE, STATUS");
        values.append("AWARD_SEQ.NEXTVAL, SYSDATE, ?, ?, SYSDATE, 'IN-PROGRESS'");
        parameters.add(createdBy);
        parameters.add(createdBy);

        String insertQuery =
            "INSERT INTO HR.CCT_CONTRACTS_TMG (" + columns.toString() + ") VALUES (" + values.toString() + ")";

        try {
            stmt = getDBTransaction().createPreparedStatement(insertQuery, 0);

            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param != null && param.equals("nocomments")) {
                    stmt.setNull(i + 1, java.sql
                                            .Types
                                            .VARCHAR);
                } else {
                    stmt.setObject(i + 1, param);
                }
            }

            int rowsInserted = stmt.executeUpdate();
            System.out.println("AMIMPL CREATED BY BOT method -----> rows count :"+rowsInserted);

            if (rowsInserted > 0) {
                getDBTransaction().commit();
                result.put("success", true);
                result.put("message", "Contract created successfully");
                result.put("contract", "100475");
            } else {
                result.put("success", false);
                result.put("message", "Failed to create contract");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            getDBTransaction().rollback();            
            result.put("success", false);
            result.put("error", "Database error: " + ex.getMessage());
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }


}
