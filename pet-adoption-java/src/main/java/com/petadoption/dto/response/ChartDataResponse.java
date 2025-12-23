package com.petadoption.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataResponse {

    private ChartData petTypeDistribution;
    private ChartData monthlyAdoptions;
    private ApplicationTrendData applicationTrend;
    private ChartData ageDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private List<String> labels;
        private List<Long> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationTrendData {
        private List<String> labels;
        private List<Long> pending;
        private List<Long> approved;
        private List<Long> completed;
    }
}
