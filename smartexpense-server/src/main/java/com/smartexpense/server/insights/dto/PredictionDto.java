package com.smartexpense.server.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class PredictionDto {
    private Integer month;
    private Integer year;
    private BigDecimal currentAmount;
    private BigDecimal predictedAmount;
    private String status;
}
