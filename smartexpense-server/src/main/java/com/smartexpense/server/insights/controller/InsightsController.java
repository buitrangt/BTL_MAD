package com.smartexpense.server.insights.controller;

import com.smartexpense.server.insights.dto.InsightsSummaryDto;
import com.smartexpense.server.insights.service.InsightsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsQueryService insightsQueryService;

    @GetMapping("/summary")
    public ResponseEntity<InsightsSummaryDto> getSummary(Authentication auth) {
        return ResponseEntity.ok(insightsQueryService.getSummary(auth.getName()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<InsightsSummaryDto> refresh(Authentication auth) {
        return ResponseEntity.ok(insightsQueryService.refresh(auth.getName()));
    }
}
