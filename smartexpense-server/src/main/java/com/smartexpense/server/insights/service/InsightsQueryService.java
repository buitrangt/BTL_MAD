package com.smartexpense.server.insights.service;

import com.smartexpense.server.insights.dto.InsightsSummaryDto;

public interface InsightsQueryService {
    /**
     * Returns full insights summary for the user's CURRENT month.
     * If cached results are missing, runs compute on-the-fly.
     */
    InsightsSummaryDto getSummary(String userEmail);

    /**
     * Force recompute insights for the user's current month.
     */
    InsightsSummaryDto refresh(String userEmail);
}
