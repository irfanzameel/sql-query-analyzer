package com.sqlanalyzer.mock;

public class MockExplainProvider {
    public static String getExplainOutput() {
        return """
        Seq Scan on users  (cost=0.00..431.00 rows=10000 width=32)
          Filter: (email = 'test@gmail.com')
        Planning Time: 0.123 ms
        Execution Time: 120.456 ms
        """;
    }
}
