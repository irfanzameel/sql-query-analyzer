package com.sqlanalyzer.model;

public class AnalyzeRequest {

    private String sql;
    private boolean verbose;

    public AnalyzeRequest() {}

    public AnalyzeRequest(String sql, boolean verbose) {
        this.sql = sql;
        this.verbose = verbose;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
