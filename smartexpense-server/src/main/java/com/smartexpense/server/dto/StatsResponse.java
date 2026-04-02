package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class StatsResponse {
    private BigDecimal totalAmount;
    private Map<String, BigDecimal> categoryBreakdown;
}
