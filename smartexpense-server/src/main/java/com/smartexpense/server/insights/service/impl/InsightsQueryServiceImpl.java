package com.smartexpense.server.insights.service.impl;

import com.smartexpense.server.insights.dto.*;
import com.smartexpense.server.insights.service.InsightsComputeService;
import com.smartexpense.server.insights.service.InsightsQueryService;
import com.smartexpense.server.model.*;
import com.smartexpense.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightsQueryServiceImpl implements InsightsQueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AiPredictionRepository predictionRepo;
    private final AiAnomalyRepository anomalyRepo;
    private final AiClassificationRepository classificationRepo;
    private final AiBudgetSuggestionRepository budgetRepo;

    private final InsightsComputeService computeService;

    @Override
    public InsightsSummaryDto getSummary(String userEmail) {
        User user = findUser(userEmail);
        LocalDate today = LocalDate.now(ZONE);
        int month = today.getMonthValue();
        int year = today.getYear();

        // Cache check: if prediction missing → trigger compute synchronously
        boolean hasPrediction = predictionRepo
                .findByUserIdAndMonthAndYear(user.getId(), month, year)
                .isPresent();
        if (!hasPrediction) {
            computeService.computeAllForUser(user.getId(), month, year);
        }

        return assembleSummary(user.getId(), month, year);
    }

    @Override
    public InsightsSummaryDto refresh(String userEmail) {
        User user = findUser(userEmail);
        LocalDate today = LocalDate.now(ZONE);
        int month = today.getMonthValue();
        int year = today.getYear();

        computeService.computeAllForUser(user.getId(), month, year);
        return assembleSummary(user.getId(), month, year);
    }

    private InsightsSummaryDto assembleSummary(Long userId, int month, int year) {
        // ===== Prediction =====
        PredictionDto predictionDto = predictionRepo
                .findByUserIdAndMonthAndYear(userId, month, year)
                .map(p -> PredictionDto.builder()
                        .month(p.getMonth())
                        .year(p.getYear())
                        .currentAmount(p.getCurrentAmount())
                        .predictedAmount(p.getPredictedAmount())
                        .status(p.getStatus())
                        .build())
                .orElse(null);

        // ===== Classification =====
        ClassificationDto classificationDto = classificationRepo
                .findByUserIdAndMonthAndYear(userId, month, year)
                .map(c -> ClassificationDto.builder()
                        .month(c.getMonth())
                        .year(c.getYear())
                        .level(c.getLevel())
                        .label(humanLabel(c.getLevel()))
                        .note(c.getNote())
                        .build())
                .orElse(null);

        // ===== Anomalies =====
        List<AiAnomaly> anomalyEntities = anomalyRepo
                .findByUserIdAndMonthAndYear(userId, month, year);
        List<AnomalyDto> anomalies = anomalyEntities.stream()
                .map(a -> AnomalyDto.builder()
                        .categoryId(a.getCategoryId())
                        .categoryName(getCategoryName(a.getCategoryId()))
                        .amountDiff(a.getAmountDiff())
                        .percentIncrease(a.getPercentIncrease())
                        .build())
                .collect(Collectors.toList());

        // ===== Budget suggestions =====
        List<AiBudgetSuggestion> budgetEntities = budgetRepo
                .findByUserIdAndMonthAndYear(userId, month, year);
        List<BudgetSuggestionDto> budgets = budgetEntities.stream()
                .map(b -> BudgetSuggestionDto.builder()
                        .categoryId(b.getCategoryId())
                        .categoryName(getCategoryName(b.getCategoryId()))
                        .suggestedAmount(b.getSuggestedAmount())
                        .build())
                .collect(Collectors.toList());

        return InsightsSummaryDto.builder()
                .prediction(predictionDto)
                .classification(classificationDto)
                .anomalies(anomalies)
                .budgetSuggestions(budgets)
                .build();
    }

    private String humanLabel(String level) {
        if (level == null) return "Chưa xác định";
        return switch (level) {
            case "SAVING" -> "Tiết kiệm tốt";
            case "NORMAL" -> "Bình thường";
            case "WASTEFUL" -> "Cần lưu ý";
            case "OVERSPENT" -> "Hoang phí";
            case "NO_INCOME" -> "Chưa có thu nhập";
            default -> level;
        };
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) return "Khác";
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("Khác");
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
