package com.smartexpense.server.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class AnomalyDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amountDiff;
    private BigDecimal percentIncrease;
}
