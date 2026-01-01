package com.sqlanalyzer.service;
import com.sqlanalyzer.model.QueryAnalysis;
import com.sqlanalyzer.mock.MockExplainProvider;
import com.sqlanalyzer.parser.ExplainParser;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class QueryExecutorService {

    private final JdbcTemplate jdbcTemplate;

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER",
            "TRUNCATE", "CREATE", "GRANT", "REVOKE"
    );

    public QueryExecutorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public QueryAnalysis explainAnalyze(String sql) {
        validateSql(sql);

        String explainOutput = MockExplainProvider.getExplainOutput();

        return ExplainParser.parse(explainOutput);
    }


    private void validateSql(String sql) {


        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }
        if (sql.length() > 5000) {
            throw new IllegalArgumentException("SQL query too long");
        }
        String normalized = sql.trim().toUpperCase();

        // 1️⃣ Must start with SELECT
        if (!normalized.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        // 2️⃣ Block multiple statements
        if (normalized.contains(";")) {
            throw new IllegalArgumentException("Multiple statements are not allowed");
        }

        // 3️⃣ Block SQL comments
        if (normalized.contains("--") || normalized.contains("/*")) {
            throw new IllegalArgumentException("SQL comments are not allowed");
        }

        // 4️⃣ Block dangerous keywords
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (normalized.contains(keyword)) {
                throw new IllegalArgumentException("Forbidden keyword detected: " + keyword);
            }
        }
    }
}
