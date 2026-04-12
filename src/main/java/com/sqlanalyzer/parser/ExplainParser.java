package com.sqlanalyzer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlanalyzer.model.QueryAnalysis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExplainParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Patterns for text-format fallback ───
    private static final Pattern EXEC_TIME =
            Pattern.compile("Execution Time: ([0-9.]+) ms");
    private static final Pattern PLAN_TIME =
            Pattern.compile("Planning Time: ([0-9.]+) ms");
    private static final Pattern ROWS =
            Pattern.compile("rows=([0-9]+)");

    /**
     * Parse PostgreSQL EXPLAIN (FORMAT JSON) output.
     * This is the primary parser — JSON is structured and reliable.
     */
    public static QueryAnalysis parseJson(String json) {
        QueryAnalysis analysis = new QueryAnalysis();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode plan;

            // PostgreSQL wraps JSON explain in an array
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                plan = first.has("Plan") ? first.get("Plan") : first;

                // Extract top-level timing
                if (first.has("Planning Time")) {
                    analysis.setPlanningTimeMs(first.get("Planning Time").asDouble());
                }
                if (first.has("Execution Time")) {
                    double execTime = first.get("Execution Time").asDouble();
                    analysis.setExecutionTimeMs(execTime);
                    if (execTime > 100) {
                        analysis.setSlowQuery(true);
                        analysis.getIssues().add("Query execution time is high (" + execTime + " ms)");
                        analysis.getSuggestions().add("Optimize query filters or add indexes");
                    }
                }
            } else {
                plan = root;
            }

            // Walk the plan tree recursively
            walkPlanNode(plan, analysis);

        } catch (Exception e) {
            // If JSON parsing fails, fall back to text parsing
            return parseText(json);
        }

        return analysis;
    }

    /**
     * Recursively walk the EXPLAIN JSON plan tree to detect node types and issues.
     */
    private static void walkPlanNode(JsonNode node, QueryAnalysis analysis) {
        if (node == null || node.isMissingNode()) return;

        String nodeType = node.has("Node Type") ? node.get("Node Type").asText() : "";

        switch (nodeType) {
            case "Seq Scan" -> {
                analysis.getScanTypes().add("Seq Scan");
                long rows = node.has("Plan Rows") ? node.get("Plan Rows").asLong() : 0;
                if (rows > 1000) {
                    analysis.getIssues().add("Sequential scan on large table (" + rows + " estimated rows)");
                    analysis.getSuggestions().add("Consider adding an index on filtered columns");
                }
            }
            case "Index Scan" -> analysis.getScanTypes().add("Index Scan");
            case "Index Only Scan" -> analysis.getScanTypes().add("Index Only Scan");
            case "Bitmap Heap Scan" -> {
                analysis.getScanTypes().add("Bitmap Heap Scan");
                analysis.getSuggestions().add("Bitmap scan indicates multiple index conditions — review index strategy");
            }
            case "Bitmap Index Scan" -> analysis.getScanTypes().add("Bitmap Index Scan");
            case "CTE Scan" -> analysis.getScanTypes().add("CTE Scan");
            case "Function Scan" -> analysis.getScanTypes().add("Function Scan");
            case "Subquery Scan" -> analysis.getScanTypes().add("Subquery Scan");

            // Join types
            case "Nested Loop" -> {
                analysis.getIssues().add("Nested Loop join detected");
                long innerRows = node.has("Plan Rows") ? node.get("Plan Rows").asLong() : 0;
                if (innerRows > 5000) {
                    analysis.getSuggestions().add(
                            "Nested Loop on large dataset (" + innerRows + " rows) — consider indexes or hash join");
                }
            }
            case "Hash Join" -> analysis.getScanTypes().add("Hash Join");
            case "Merge Join" -> analysis.getScanTypes().add("Merge Join");

            // Aggregation and sorting
            case "Sort" -> {
                if (node.has("Sort Method") && node.get("Sort Method").asText().contains("external")) {
                    analysis.getIssues().add("Sort spilled to disk — insufficient work_mem");
                    analysis.getSuggestions().add("Increase work_mem or reduce sort data volume");
                }
            }
            case "HashAggregate", "GroupAggregate" ->
                    analysis.getScanTypes().add(nodeType);

            // Parallelism
            case "Gather", "Gather Merge" ->
                    analysis.getScanTypes().add(nodeType);

            // Materialization
            case "Materialize" -> {
                analysis.getScanTypes().add("Materialize");
                analysis.getSuggestions().add("Materialize node found — subquery result is being cached");
            }
        }

        // Check for actual vs estimated row mismatch (bad planner estimates)
        if (node.has("Plan Rows") && node.has("Actual Rows")) {
            long planned = node.get("Plan Rows").asLong();
            long actual = node.get("Actual Rows").asLong();
            if (planned > 0 && actual > 0) {
                double ratio = (double) actual / planned;
                if (ratio > 10 || ratio < 0.1) {
                    analysis.getIssues().add("Planner estimate mismatch for " + nodeType
                            + " (estimated " + planned + ", actual " + actual + ")");
                    analysis.getSuggestions().add("Run ANALYZE on the table to update statistics");
                }
            }
        }

        // Check shared buffer hits vs reads
        if (node.has("Shared Read Blocks")) {
            long reads = node.get("Shared Read Blocks").asLong();
            long hits = node.has("Shared Hit Blocks") ? node.get("Shared Hit Blocks").asLong() : 0;
            if (reads > 0 && hits > 0) {
                double hitRatio = (double) hits / (hits + reads);
                if (hitRatio < 0.9) {
                    analysis.getIssues().add("Low buffer cache hit ratio (" +
                            String.format("%.1f%%", hitRatio * 100) + ") for " + nodeType);
                    analysis.getSuggestions().add("Consider increasing shared_buffers or adding indexes to reduce I/O");
                }
            }
        }

        // Recurse into child plan nodes
        if (node.has("Plans")) {
            for (JsonNode child : node.get("Plans")) {
                walkPlanNode(child, analysis);
            }
        }
    }

    /**
     * Fallback: parse text-format EXPLAIN output using regex.
     * Used when JSON parsing fails or for backwards compatibility.
     */
    public static QueryAnalysis parseText(String explain) {
        QueryAnalysis analysis = new QueryAnalysis();

        // Scan type detection
        if (explain.contains("Seq Scan")) {
            analysis.getScanTypes().add("Seq Scan");
            analysis.getIssues().add("Sequential scan detected");
            analysis.getSuggestions().add("Consider adding an index on filtered columns");
        }
        if (explain.contains("Index Scan")) {
            analysis.getScanTypes().add("Index Scan");
        }
        if (explain.contains("Index Only Scan")) {
            analysis.getScanTypes().add("Index Only Scan");
        }
        if (explain.contains("Bitmap Heap Scan")) {
            analysis.getScanTypes().add("Bitmap Heap Scan");
            analysis.getSuggestions().add("Bitmap scan indicates multiple index conditions");
        }
        if (explain.contains("Hash Join")) {
            analysis.getScanTypes().add("Hash Join");
        }
        if (explain.contains("Merge Join")) {
            analysis.getScanTypes().add("Merge Join");
        }

        // Join strategy detection
        if (explain.contains("Nested Loop")) {
            analysis.getIssues().add("Nested Loop join detected");
            analysis.getSuggestions().add(
                    "Nested Loop on large datasets can be slow — consider indexes or hash join");
        }

        // Execution time
        Matcher execMatcher = EXEC_TIME.matcher(explain);
        if (execMatcher.find()) {
            double execTime = Double.parseDouble(execMatcher.group(1));
            analysis.setExecutionTimeMs(execTime);
            if (execTime > 100) {
                analysis.setSlowQuery(true);
                analysis.getIssues().add("Query execution time is high");
                analysis.getSuggestions().add("Optimize query filters or add indexes");
            }
        }

        // Planning time
        Matcher planMatcher = PLAN_TIME.matcher(explain);
        if (planMatcher.find()) {
            analysis.setPlanningTimeMs(Double.parseDouble(planMatcher.group(1)));
        }

        // Bad planner estimates
        Matcher rowsMatcher = ROWS.matcher(explain);
        if (rowsMatcher.find()) {
            int rows = Integer.parseInt(rowsMatcher.group(1));
            if (rows > 5000) {
                analysis.getIssues().add("High row count detected");
                analysis.getSuggestions().add("Check table statistics or consider ANALYZE");
            }
        }

        return analysis;
    }
}
