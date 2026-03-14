package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class StatsResponse {
    private Double totalAmount;
    private Map<String, Double> categoryBreakdown;
}
