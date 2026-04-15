package com.smartexpense.server.insights.service.impl;

import com.smartexpense.server.insights.algorithm.AnomalyDetector;
import com.smartexpense.server.insights.algorithm.BudgetCalculator;
import com.smartexpense.server.insights.algorithm.ForecastEngine;
import com.smartexpense.server.insights.algorithm.SpendingClassifier;
import com.smartexpense.server.insights.service.InsightsComputeService;
import com.smartexpense.server.model.*;
import com.smartexpense.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightsComputeServiceImpl implements InsightsComputeService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private final TransactionRepository transactionRepository;
    private final AiPredictionRepository predictionRepo;
    private final AiAnomalyRepository anomalyRepo;
    private final AiClassificationRepository classificationRepo;
    private final AiBudgetSuggestionRepository budgetRepo;

    private final ForecastEngine forecastEngine;
    private final AnomalyDetector anomalyDetector;
    private final SpendingClassifier spendingClassifier;
    private final BudgetCalculator budgetCalculator;

    @Override
    public void computeCurrentMonthForUser(Long userId) {
        LocalDate today = LocalDate.now(ZONE);
        computeAllForUser(userId, today.getMonthValue(), today.getYear());
    }

    @Override
    @Transactional
    public void computeAllForUser(Long userId, int month, int year) {
        log.info("Computing insights for user={} month={}/{}", userId, month, year);

        // Load 4 months of history (current + 3 previous) for context
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate from = firstOfMonth.minusMonths(3);
        long fromMillis = from.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long toMillis = firstOfMonth.plusMonths(1).atStartOfDay(ZONE).toInstant().toEpochMilli();

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTimeStampBetween(userId, fromMillis, toMillis);

        try {
            computePrediction(userId, month, year, transactions);
        } catch (Exception e) {
            log.error("Prediction failed for user={}: {}", userId, e.getMessage(), e);
        }
        try {
            computeAnomalies(userId, month, year, transactions);
        } catch (Exception e) {
            log.error("Anomaly detection failed for user={}: {}", userId, e.getMessage(), e);
        }
        try {
            computeClassification(userId, month, year, transactions);
        } catch (Exception e) {
            log.error("Classification failed for user={}: {}", userId, e.getMessage(), e);
        }
        try {
            computeBudget(userId, month, year, transactions);
        } catch (Exception e) {
            log.error("Budget suggestion failed for user={}: {}", userId, e.getMessage(), e);
        }
    }

    // ===== PREDICTION =====
    private void computePrediction(Long userId, int month, int year, List<Transaction> txs) {
        BigDecimal current = forecastEngine.currentMonthTotal(txs, month, year);
        ForecastEngine.ForecastResult result = forecastEngine.predictMonthlyTotalResult(txs, month, year);
        BigDecimal predicted = result.predictedAmount;

        AiPrediction entity = predictionRepo
                .findByUserIdAndMonthAndYear(userId, month, year)
                .orElseGet(() -> AiPrediction.builder()
                        .userId(userId)
                        .month(month)
                        .year(year)
                        .build());

        entity.setCurrentAmount(current);
        entity.setPredictedAmount(predicted);
        entity.setStatus("COMPLETED_" + result.provider);
        predictionRepo.save(entity);
    }

    // ===== ANOMALIES =====
    private void computeAnomalies(Long userId, int month, int year, List<Transaction> txs) {
        // Wipe previous results for this month then re-insert
        anomalyRepo.deleteByUserIdAndMonthAndYear(userId, month, year);

        List<AnomalyDetector.Anomaly> anomalies = anomalyDetector.detect(txs, month, year);
        for (AnomalyDetector.Anomaly a : anomalies) {
            AiAnomaly entity = AiAnomaly.builder()
                    .userId(userId)
                    .month(month)
                    .year(year)
                    .categoryId(a.categoryId)
                    .amountDiff(a.amountDiff)
                    .percentIncrease(a.percentIncrease)
                    .status("COMPLETED")
                    .build();
            anomalyRepo.save(entity);
        }
    }

    // ===== CLASSIFICATION =====
    private void computeClassification(Long userId, int month, int year, List<Transaction> txs) {
        SpendingClassifier.Result r = spendingClassifier.classify(txs, month, year);

        AiClassification entity = classificationRepo
                .findByUserIdAndMonthAndYear(userId, month, year)
                .orElseGet(() -> AiClassification.builder()
                        .userId(userId)
                        .month(month)
                        .year(year)
                        .build());

        entity.setLevel(r.level);
        entity.setNote(r.note);
        entity.setStatus("COMPLETED");
        classificationRepo.save(entity);
    }

    // ===== BUDGET SUGGESTION =====
    private void computeBudget(Long userId, int month, int year, List<Transaction> txs) {
        // Wipe previous results for this month then re-insert
        budgetRepo.deleteByUserIdAndMonthAndYear(userId, month, year);

        List<BudgetCalculator.Suggestion> suggestions = budgetCalculator.suggest(txs, month, year);
        for (BudgetCalculator.Suggestion s : suggestions) {
            AiBudgetSuggestion entity = AiBudgetSuggestion.builder()
                    .userId(userId)
                    .categoryId(s.categoryId)
                    .month(month)
                    .year(year)
                    .suggestedAmount(s.suggestedAmount)
                    .status("COMPLETED")
                    .build();
            budgetRepo.save(entity);
        }
    }
}
