package com.sqlanalyzer.service;

import com.sqlanalyzer.model.QueryAnalysis;
import com.sqlanalyzer.parser.ExplainParser;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class QueryExecutorService {

    private final JdbcTemplate jdbcTemplate;

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER",
            "TRUNCATE", "CREATE", "GRANT", "REVOKE",
            "UNION", "INTO", "EXEC", "COPY", "CALL"
    );

    public QueryExecutorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public QueryAnalysis explainAnalyze(String sql, boolean verbose) {
        validateSql(sql);

        String explainSql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql;
        List<String> lines = jdbcTemplate.queryForList(explainSql, String.class);
        String explainOutput = String.join("\n", lines);

        QueryAnalysis analysis = ExplainParser.parseJson(explainOutput);

        if (verbose) {
            analysis.setRawPlan(explainOutput);
        }

        return analysis;
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }
        if (sql.length() > 5000) {
            throw new IllegalArgumentException("SQL query too long (max 5000 characters)");
        }

        String normalized = sql.trim().toUpperCase();

        // Must start with SELECT
        if (!normalized.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        // Block multiple statements
        if (normalized.contains(";")) {
            throw new IllegalArgumentException("Multiple statements are not allowed");
        }

        // Block SQL comments
        if (normalized.contains("--") || normalized.contains("/*")) {
            throw new IllegalArgumentException("SQL comments are not allowed");
        }

        // Block dangerous keywords
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (normalized.contains(keyword)) {
                throw new IllegalArgumentException("Forbidden keyword detected: " + keyword);
            }
        }
    }
}
