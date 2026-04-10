package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class WeeklyOverviewResponse {
    private BigDecimal totalAmount;
    private double percentChange;
    private Map<String, BigDecimal> dailyBreakdown;
    private List<CategoryStat> categoryBreakdown;

    @Data
    @AllArgsConstructor
    @Builder
    public static class CategoryStat {
        private String category;
        private BigDecimal amount;
        private double percent;
    }
}
