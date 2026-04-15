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
    /**
     * Optional AI-generated narrative/advice for the Insights screen.
     * Kept as plain text so the mobile UI can render it easily.
     */
    private String aiNarrative;
}
