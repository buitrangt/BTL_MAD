package com.smartexpense.server.controller;

import com.smartexpense.server.dto.StatsResponse;
import com.smartexpense.server.dto.WeeklyOverviewResponse;
import com.smartexpense.server.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/weekly-overview")
    public ResponseEntity<WeeklyOverviewResponse> getWeeklyOverview(Authentication auth) {
        return ResponseEntity.ok(statsService.getWeeklyOverview(auth.getName()));
    }

    @GetMapping("/daily")
    public ResponseEntity<StatsResponse> getDailyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getDailyStats(auth.getName()));
    }

    @GetMapping("/weekly")
    public ResponseEntity<StatsResponse> getWeeklyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getWeeklyStats(auth.getName()));
    }

    @GetMapping("/monthly")
    public ResponseEntity<StatsResponse> getMonthlyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getMonthlyStats(auth.getName()));
    }

    @GetMapping("/by-category")
    public ResponseEntity<StatsResponse> getByCategoryStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getByCategoryStats(auth.getName()));
    }
}
