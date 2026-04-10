package com.smartexpense.server.service;

import com.smartexpense.server.dto.HomeOverviewResponse;
import com.smartexpense.server.dto.StatsResponse;
import com.smartexpense.server.dto.WeeklyOverviewResponse;

public interface StatsService {
    StatsResponse getDailyStats(String userEmail);
    StatsResponse getWeeklyStats(String userEmail);
    StatsResponse getMonthlyStats(String userEmail);
    StatsResponse getByCategoryStats(String userEmail);
    WeeklyOverviewResponse getWeeklyOverview(String userEmail);
    HomeOverviewResponse getHomeOverview(String userEmail);
}
