package com.sqlanalyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class QueryExecutorServiceTest {

    private QueryExecutorService service;

    @BeforeEach
    void setUp() {
        JdbcTemplate mockJdbc = mock(JdbcTemplate.class);
        service = new QueryExecutorService(mockJdbc);
    }

    // ─── Validation tests (via reflection or by triggering explainAnalyze) ───

    @Test
    void rejectsNullSql() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze(null, false));
    }

    @Test
    void rejectsEmptySql() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("", false));
    }

    @Test
    void rejectsBlankSql() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("   ", false));
    }

    @Test
    void rejectsTooLongSql() {
        String longSql = "SELECT " + "a".repeat(5000);
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze(longSql, false));
    }

    @Test
    void rejectsNonSelectQuery() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("INSERT INTO users VALUES (1)", false));
    }

    @Test
    void rejectsMultipleStatements() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT 1; DROP TABLE users", false));
    }

    @Test
    void rejectsSqlWithLineComments() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT 1 -- comment", false));
    }

    @Test
    void rejectsSqlWithBlockComments() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT /* comment */ 1", false));
    }

    @Test
    void rejectsDeleteKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT * FROM (DELETE FROM users RETURNING *)", false));
    }

    @Test
    void rejectsDropKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT * FROM users WHERE DROP", false));
    }

    @Test
    void rejectsUnionKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT 1 UNION SELECT 2", false));
    }

    @Test
    void rejectsCopyKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT * FROM COPY", false));
    }

    @Test
    void rejectsInsertKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT * FROM (INSERT INTO users VALUES(1))", false));
    }

    @Test
    void rejectsUpdateKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT * FROM (UPDATE users SET name='x')", false));
    }

    @Test
    void rejectsTruncateKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT TRUNCATE FROM users", false));
    }

    @Test
    void rejectsAlterKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT ALTER FROM users", false));
    }

    @Test
    void rejectsCreateKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT CREATE FROM users", false));
    }

    @Test
    void rejectsGrantKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT GRANT FROM users", false));
    }

    @Test
    void rejectsRevokeKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.explainAnalyze("SELECT REVOKE FROM users", false));
    }
}
