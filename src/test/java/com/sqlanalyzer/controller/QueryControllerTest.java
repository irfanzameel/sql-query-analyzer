package com.sqlanalyzer.controller;

import com.sqlanalyzer.model.QueryAnalysis;
import com.sqlanalyzer.service.QueryExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryExecutorService service;

    @Test
    void analyze_validRequest_returnsSuccess() throws Exception {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.setExecutionTimeMs(5.0);
        analysis.setPlanningTimeMs(0.1);
        analysis.getScanTypes().add("Index Scan");

        when(service.explainAnalyze(eq("SELECT 1"), eq(false)))
                .thenReturn(analysis);

        mockMvc.perform(post("/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\": \"SELECT 1\", \"verbose\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executionTimeMs").value(5.0))
                .andExpect(jsonPath("$.data.scanTypes[0]").value("Index Scan"));
    }

    @Test
    void analyze_invalidSql_returnsBadRequest() throws Exception {
        when(service.explainAnalyze(anyString(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("Only SELECT queries are allowed"));

        mockMvc.perform(post("/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\": \"DELETE FROM users\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Only SELECT queries are allowed"));
    }

    @Test
    void analyze_verboseMode_includesRawPlan() throws Exception {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.setExecutionTimeMs(2.0);
        analysis.setRawPlan("[{\"Plan\": {\"Node Type\": \"Seq Scan\"}}]");

        when(service.explainAnalyze(eq("SELECT * FROM users"), eq(true)))
                .thenReturn(analysis);

        mockMvc.perform(post("/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\": \"SELECT * FROM users\", \"verbose\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rawPlan").isNotEmpty());
    }
}
