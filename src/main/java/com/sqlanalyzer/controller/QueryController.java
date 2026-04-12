package com.sqlanalyzer.controller;

import com.sqlanalyzer.api.ApiResponse;
import com.sqlanalyzer.model.AnalyzeRequest;
import com.sqlanalyzer.model.QueryAnalysis;
import com.sqlanalyzer.service.QueryExecutorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analyze")
public class QueryController {

    private final QueryExecutorService service;

    public QueryController(QueryExecutorService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<QueryAnalysis> analyze(@RequestBody AnalyzeRequest request) {
        QueryAnalysis result = service.explainAnalyze(request.getSql(), request.isVerbose());
        return ApiResponse.success(result);
    }
}
