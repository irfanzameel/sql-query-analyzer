package com.sqlanalyzer.parser;

import com.sqlanalyzer.model.QueryAnalysis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplainParserTest {

    // ─── JSON Format Tests ───

    @Test
    void parseJson_seqScan_detectsIssue() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Seq Scan",
                  "Relation Name": "users",
                  "Plan Rows": 10000,
                  "Actual Rows": 10000
                },
                "Planning Time": 0.123,
                "Execution Time": 12.456
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.getScanTypes().contains("Seq Scan"));
        assertTrue(result.getIssues().stream().anyMatch(i -> i.contains("Sequential scan")));
        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("index")));
        assertEquals(0.123, result.getPlanningTimeMs(), 0.001);
        assertEquals(12.456, result.getExecutionTimeMs(), 0.001);
        assertFalse(result.isSlowQuery());
    }

    @Test
    void parseJson_indexScan_noIssues() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Index Scan",
                  "Index Name": "idx_users_email",
                  "Plan Rows": 1
                },
                "Planning Time": 0.05,
                "Execution Time": 0.1
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.getScanTypes().contains("Index Scan"));
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void parseJson_slowQuery_flagged() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Seq Scan",
                  "Plan Rows": 500
                },
                "Planning Time": 1.0,
                "Execution Time": 250.0
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.isSlowQuery());
        assertTrue(result.getIssues().stream().anyMatch(i -> i.contains("execution time is high")));
    }

    @Test
    void parseJson_nestedLoop_detectsJoin() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Nested Loop",
                  "Plan Rows": 10000,
                  "Plans": [
                    {"Node Type": "Index Scan", "Plan Rows": 1},
                    {"Node Type": "Seq Scan", "Plan Rows": 5000}
                  ]
                },
                "Execution Time": 50.0
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.getIssues().stream().anyMatch(i -> i.contains("Nested Loop")));
        assertTrue(result.getScanTypes().contains("Index Scan"));
        assertTrue(result.getScanTypes().contains("Seq Scan"));
    }

    @Test
    void parseJson_plannerEstimateMismatch_detected() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Seq Scan",
                  "Plan Rows": 10,
                  "Actual Rows": 50000
                },
                "Execution Time": 30.0
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.getIssues().stream().anyMatch(i -> i.contains("estimate mismatch")));
        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("ANALYZE")));
    }

    @Test
    void parseJson_bitmapScan_detected() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Bitmap Heap Scan",
                  "Plan Rows": 100,
                  "Plans": [
                    {"Node Type": "Bitmap Index Scan", "Plan Rows": 100}
                  ]
                },
                "Execution Time": 5.0
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.getScanTypes().contains("Bitmap Heap Scan"));
        assertTrue(result.getScanTypes().contains("Bitmap Index Scan"));
    }

    @Test
    void parseJson_hashJoin_detected() {
        String json = """
            [
              {
                "Plan": {
                  "Node Type": "Hash Join",
                  "Plan Rows": 500,
                  "Plans": [
                    {"Node Type": "Seq Scan", "Plan Rows": 500},
                    {"Node Type": "Hash", "Plan Rows": 100, "Plans": [
                      {"Node Type": "Seq Scan", "Plan Rows": 100}
                    ]}
                  ]
                },
                "Execution Time": 10.0
              }
            ]
            """;

        QueryAnalysis result = ExplainParser.parseJson(json);

        assertTrue(result.getScanTypes().contains("Hash Join"));
    }

    @Test
    void parseJson_invalidJson_fallsBackToText() {
        String text = """
            Seq Scan on users  (cost=0.00..431.00 rows=10000 width=32)
              Filter: (email = 'test@gmail.com')
            Planning Time: 0.123 ms
            Execution Time: 120.456 ms
            """;

        QueryAnalysis result = ExplainParser.parseJson(text);

        // Should fall back to text parsing
        assertTrue(result.getScanTypes().contains("Seq Scan"));
        assertEquals(120.456, result.getExecutionTimeMs(), 0.001);
        assertTrue(result.isSlowQuery());
    }

    // ─── Text Format Tests ───

    @Test
    void parseText_seqScan() {
        String explain = """
            Seq Scan on users  (cost=0.00..431.00 rows=10000 width=32)
              Filter: (email = 'test@gmail.com')
            Planning Time: 0.123 ms
            Execution Time: 5.456 ms
            """;

        QueryAnalysis result = ExplainParser.parseText(explain);

        assertTrue(result.getScanTypes().contains("Seq Scan"));
        assertEquals(0.123, result.getPlanningTimeMs(), 0.001);
        assertEquals(5.456, result.getExecutionTimeMs(), 0.001);
        assertFalse(result.isSlowQuery());
    }

    @Test
    void parseText_indexScan() {
        String explain = """
            Index Scan using idx_users_email on users
            Planning Time: 0.05 ms
            Execution Time: 0.1 ms
            """;

        QueryAnalysis result = ExplainParser.parseText(explain);

        assertTrue(result.getScanTypes().contains("Index Scan"));
    }

    @Test
    void parseText_highRowCount() {
        String explain = """
            Seq Scan on orders  (cost=0.00..1000.00 rows=50000 width=64)
            Execution Time: 50.0 ms
            """;

        QueryAnalysis result = ExplainParser.parseText(explain);

        assertTrue(result.getIssues().stream().anyMatch(i -> i.contains("High row count")));
    }

    @Test
    void parseText_nestedLoop() {
        String explain = """
            Nested Loop  (cost=0.00..500.00 rows=100 width=32)
            Execution Time: 10.0 ms
            """;

        QueryAnalysis result = ExplainParser.parseText(explain);

        assertTrue(result.getIssues().stream().anyMatch(i -> i.contains("Nested Loop")));
    }
}
