package com.sqlanalyzer.parser;

import com.sqlanalyzer.model.QueryAnalysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExplainParser {

    private static final Pattern EXEC_TIME =
            Pattern.compile("Execution Time: ([0-9.]+) ms");

    private static final Pattern PLAN_TIME =
            Pattern.compile("Planning Time: ([0-9.]+) ms");

    private static final Pattern ROWS =
            Pattern.compile("rows=([0-9]+)");

    public static QueryAnalysis parse(String explain) {

        QueryAnalysis analysis = new QueryAnalysis();

        /* -------------------------
           Scan type detection
        -------------------------- */
        if (explain.contains("Seq Scan")) {
            analysis.getScanTypes().add("Seq Scan");
            analysis.getIssues().add("Sequential scan detected");
            analysis.getSuggestions().add(
                    "Consider adding an index on filtered columns"
            );
        }

        if (explain.contains("Index Scan")) {
            analysis.getScanTypes().add("Index Scan");
        }

        if (explain.contains("Bitmap Heap Scan")) {
            analysis.getScanTypes().add("Bitmap Heap Scan");
            analysis.getSuggestions().add(
                    "Bitmap scan indicates multiple index conditions"
            );
        }

        /* -------------------------
           Join strategy detection
        -------------------------- */
        if (explain.contains("Nested Loop")) {
            analysis.getIssues().add("Nested Loop join detected");

            if (explain.contains("rows=10000")) {
                analysis.getSuggestions().add(
                        "Nested Loop on large datasets can be slow; consider indexes or hash join"
                );
            }
        }

        /* -------------------------
           Execution time
        -------------------------- */
        Matcher execMatcher = EXEC_TIME.matcher(explain);
        if (execMatcher.find()) {
            double execTime = Double.parseDouble(execMatcher.group(1));
            analysis.setExecutionTimeMs(execTime);

            if (execTime > 100) {
                analysis.setSlowQuery(true);
                analysis.getIssues().add("Query execution time is high");
                analysis.getSuggestions().add(
                        "Optimize query filters or add indexes"
                );
            }
        }

        /* -------------------------
           Planning time
        -------------------------- */
        Matcher planMatcher = PLAN_TIME.matcher(explain);
        if (planMatcher.find()) {
            analysis.setPlanningTimeMs(
                    Double.parseDouble(planMatcher.group(1))
            );
        }

        /* -------------------------
           Bad planner estimates
        -------------------------- */
        Matcher rowsMatcher = ROWS.matcher(explain);
        if (rowsMatcher.find()) {
            int rows = Integer.parseInt(rowsMatcher.group(1));

            if (rows > 5000) {
                analysis.getIssues().add("High row count detected");
                analysis.getSuggestions().add(
                        "Check table statistics or consider ANALYZE"
                );
            }
        }

        return analysis;
    }
}
