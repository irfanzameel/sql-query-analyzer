package com.sqlanalyzer.model;

import java.util.ArrayList;
import java.util.List;

public class QueryAnalysis {
    private double planningTimeMs;
    private boolean slowQuery;

    private double executionTimeMs;
    private List<String> scanTypes = new ArrayList<>();
    private List<String> issues = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(double executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getScanTypes() {
        return scanTypes;
    }

    public List<String> getIssues() {
        return issues;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }


    public boolean isSlowQuery() {
        return slowQuery;
    }

    public void setSlowQuery(boolean slowQuery) {
        this.slowQuery = slowQuery;
    }

    public double getPlanningTimeMs() {
        return planningTimeMs;
    }

    public void setPlanningTimeMs(double planningTimeMs) {
        this.planningTimeMs = planningTimeMs;
    }
}
