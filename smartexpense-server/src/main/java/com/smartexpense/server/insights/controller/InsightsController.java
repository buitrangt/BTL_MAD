package com.smartexpense.server.insights.controller;

import com.smartexpense.server.insights.dto.InsightsSummaryDto;
import com.smartexpense.server.insights.service.InsightsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các request liên quan đến AI phân tích.
 * Thuộc luồng chức năng: AI phân tích (Phân loại mức chi tiêu, gợi ý thông minh, gợi ý ngân sách, Dự báo chi tiêu, Cảnh báo bất thường).
 */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsQueryService insightsQueryService;

    // 1. API lấy toàn bộ dữ liệu phân tích từ AI (Dự báo, cảnh báo, phân loại, gợi ý ngân sách, gợi ý thông minh)
    @GetMapping("/summary")
    public ResponseEntity<InsightsSummaryDto> getSummary(Authentication auth) {
        return ResponseEntity.ok(insightsQueryService.getSummary(auth.getName()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<InsightsSummaryDto> refresh(Authentication auth) {
        return ResponseEntity.ok(insightsQueryService.refresh(auth.getName()));
    }
}
