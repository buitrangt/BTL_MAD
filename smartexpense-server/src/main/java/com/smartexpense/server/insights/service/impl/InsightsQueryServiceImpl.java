package com.smartexpense.server.insights.service.impl;

import com.smartexpense.server.insights.dto.*;
import com.smartexpense.server.insights.service.InsightsComputeService;
import com.smartexpense.server.insights.service.InsightsQueryService;
import com.smartexpense.server.model.*;
import com.smartexpense.server.repository.*;
import com.smartexpense.server.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final TransactionRepository transactionRepository;

    private final InsightsComputeService computeService;
    private final GeminiService geminiService;

    @Override
    public InsightsSummaryDto getSummary(String userEmail) {
        User user = findUser(userEmail);
        LocalDate today = LocalDate.now(ZONE);
        int month = today.getMonthValue();
        int year = today.getYear();

        // Cache check:
        // - if prediction missing → compute
        // - if prediction exists but legacy status (e.g. "COMPLETED") → recompute once to stamp provider
        AiPrediction prediction = predictionRepo
                .findByUserIdAndMonthAndYear(user.getId(), month, year)
                .orElse(null);

        boolean hasAnyExpenseThisMonth = hasAnyExpenseThisMonth(user.getId());
        boolean isZeroPrediction = prediction != null
                && prediction.getPredictedAmount() != null
                && prediction.getPredictedAmount().compareTo(BigDecimal.ZERO) == 0
                && hasAnyExpenseThisMonth;

        boolean needsCompute = prediction == null
                || prediction.getStatus() == null
                || "COMPLETED".equalsIgnoreCase(prediction.getStatus())
                || "PROCESSING".equalsIgnoreCase(prediction.getStatus())
                || isZeroPrediction;
        if (needsCompute) {
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

        String aiNarrative = null;
        try {
            String prompt = buildAiNarrativePrompt(month, year, predictionDto, classificationDto, anomalies, budgets);
            String reply = geminiService.chat(null, null, prompt);
            if (reply != null) {
                String trimmed = reply.trim();
                // GeminiService returns Vietnamese error strings when misconfigured; do not show as narrative.
                if (!trimmed.isBlank()
                        && !trimmed.startsWith("[Lỗi cấu hình]")
                        && !trimmed.startsWith("Lỗi gọi AI:")
                        && !trimmed.startsWith("Lỗi kết nối AI:")
                        && !trimmed.startsWith("Không nhận được phản hồi")) {
                    aiNarrative = trimmed;
                }
            }
        } catch (Exception e) {
            log.debug("AI narrative generation skipped: {}", e.getMessage());
        }

        return InsightsSummaryDto.builder()
                .prediction(predictionDto)
                .classification(classificationDto)
                .anomalies(anomalies)
                .budgetSuggestions(budgets)
                .aiNarrative(aiNarrative)
                .build();
    }

    private String buildAiNarrativePrompt(
            int month,
            int year,
            PredictionDto prediction,
            ClassificationDto classification,
            List<AnomalyDto> anomalies,
            List<BudgetSuggestionDto> budgets
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý tài chính cá nhân. Viết bằng tiếng Việt.\n");
        sb.append("Nhiệm vụ: tạo 1 đoạn nhận xét ngắn cho màn \"Phân tích thông minh\".\n");
        sb.append("Yêu cầu format:\n");
        sb.append("- Tối đa 5 dòng.\n");
        sb.append("- Không dùng emoji.\n");
        sb.append("- Nêu 1 nhận xét chính + 2-3 gợi ý hành động cụ thể.\n");
        sb.append("- Không bịa số. Chỉ dùng dữ liệu được cung cấp.\n");
        sb.append("- Nếu thiếu dữ liệu thì nói ngắn gọn là chưa đủ dữ liệu.\n\n");

        sb.append("Tháng/năm: ").append(month).append("/").append(year).append("\n\n");

        if (classification != null) {
            sb.append("Phân loại: ").append(classification.getLabel())
                    .append(" (").append(classification.getLevel()).append(")\n");
            if (classification.getNote() != null && !classification.getNote().isBlank()) {
                sb.append("Ghi chú phân loại: ").append(classification.getNote()).append("\n");
            }
            sb.append("\n");
        }

        if (prediction != null) {
            sb.append("Dự đoán: hiện tại ").append(prediction.getCurrentAmount())
                    .append(", dự đoán cuối tháng ").append(prediction.getPredictedAmount()).append("\n\n");
        }

        if (anomalies != null && !anomalies.isEmpty()) {
            sb.append("Bất thường (tối đa 3 mục):\n");
            for (int i = 0; i < Math.min(3, anomalies.size()); i++) {
                AnomalyDto a = anomalies.get(i);
                sb.append("- ").append(a.getCategoryName())
                        .append(": +").append(a.getAmountDiff())
                        .append(" (").append(a.getPercentIncrease()).append("%)\n");
            }
            sb.append("\n");
        } else {
            sb.append("Bất thường: không có.\n\n");
        }

        if (budgets != null && !budgets.isEmpty()) {
            sb.append("Ngân sách đề xuất (tối đa 5 mục):\n");
            for (int i = 0; i < Math.min(5, budgets.size()); i++) {
                BudgetSuggestionDto b = budgets.get(i);
                sb.append("- ").append(b.getCategoryName())
                        .append(": ").append(b.getSuggestedAmount()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Viết câu trả lời ngay dưới đây:");
        return sb.toString();
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

    private boolean hasAnyExpenseThisMonth(Long userId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate startOfMonth = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
        long start = startOfMonth.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long end = startOfMonth.plusMonths(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
        return !transactionRepository.findByUserIdAndTypeAndTimeStampBetween(userId, "expense", start, end).isEmpty();
    }
}
