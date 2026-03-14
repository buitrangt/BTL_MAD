package com.smartexpense.server.service;

import com.smartexpense.server.dto.StatsResponse;

public interface StatsService {
    StatsResponse getDailyStats(String userEmail);
    StatsResponse getWeeklyStats(String userEmail);
    StatsResponse getMonthlyStats(String userEmail);
    StatsResponse getByCategoryStats(String userEmail);
}
