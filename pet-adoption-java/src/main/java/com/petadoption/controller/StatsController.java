package com.petadoption.controller;

import com.petadoption.dto.response.ChartDataResponse;
import com.petadoption.dto.response.StatsResponse;
import com.petadoption.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "统计接口")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/stats")
    @Operation(summary = "获取统计数据")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    @GetMapping("/stats/charts")
    @Operation(summary = "获取图表数据")
    public ResponseEntity<ChartDataResponse> getChartData() {
        return ResponseEntity.ok(statsService.getChartData());
    }
}
