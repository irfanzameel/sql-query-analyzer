package com.sqlanalyzer.model;

import java.util.ArrayList;
import java.util.List;

public class QueryAnalysis {

    private double planningTimeMs;
    private double executionTimeMs;
    private boolean slowQuery;
    private List<String> scanTypes = new ArrayList<>();
    private List<String> issues = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private String rawPlan;

    // --- Getters ---

    public double getPlanningTimeMs() {
        return planningTimeMs;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean isSlowQuery() {
        return slowQuery;
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

    public String getRawPlan() {
        return rawPlan;
    }

    // --- Setters ---

    public void setPlanningTimeMs(double planningTimeMs) {
        this.planningTimeMs = planningTimeMs;
    }

    public void setExecutionTimeMs(double executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public void setSlowQuery(boolean slowQuery) {
        this.slowQuery = slowQuery;
    }

    public void setRawPlan(String rawPlan) {
        this.rawPlan = rawPlan;
    }
}
