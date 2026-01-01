package com.sqlanalyzer.controller;
import com.sqlanalyzer.api.ApiResponse;
import com.sqlanalyzer.model.QueryAnalysis;
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
    public ApiResponse<QueryAnalysis> analyze(@RequestBody String sql) {
        QueryAnalysis result = service.explainAnalyze(sql);
        return ApiResponse.success(result);
    }
}
