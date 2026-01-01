package com.sqlanalyzer.service;

import com.sqlanalyzer.mock.MockExplainProvider;
import com.sqlanalyzer.model.QueryAnalysis;
import com.sqlanalyzer.parser.ExplainParser;
import org.springframework.stereotype.Service;

@Service
public class QueryAnalyzerService {

    public QueryAnalysis analyze(String sql) {

        // 🔒 later we’ll validate SQL (SELECT only)
        String explainOutput = MockExplainProvider.getExplainOutput();

        return ExplainParser.parse(explainOutput);
    }
}
