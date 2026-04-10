package com.smartexpense.server.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class InsightsSummaryDto {
    private PredictionDto prediction;
    private ClassificationDto classification;
    private List<AnomalyDto> anomalies;
    private List<BudgetSuggestionDto> budgetSuggestions;
}
