package com.smartexpense.server.insights.service;

public interface InsightsComputeService {
    /**
     * Run all 4 AI insights computations for the given user and target month.
     * Upserts results into the ai_* tables with status = COMPLETED.
     */
    void computeAllForUser(Long userId, int month, int year);

    /**
     * Convenience: compute for the current month in Asia/Bangkok timezone.
     */
    void computeCurrentMonthForUser(Long userId);
}
