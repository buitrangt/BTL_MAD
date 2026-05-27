package com.smartexpense.server.insights.algorithm;

import com.smartexpense.server.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Thuật toán tính toán và gợi ý ngân sách cho từng danh mục.
 * Thuộc luồng chức năng: AI phân tích (Gợi ý ngân sách).
 *
 * Suggests a budget per category for the upcoming month based on the average
 * spending of the previous N months (default 3), with simple smoothing.
 *
 * Strategy:
 *   1. For each category, compute average spending in the past 3 full months
 *      (excluding current month, which is incomplete).
 *   2. Suggested = round-up to nearest 10k of (average × 1.05) — small buffer.
 *   3. If avg == 0 (new category) → skip; nothing to suggest yet.
 */
@Component
public class BudgetCalculator {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int LOOKBACK_MONTHS = 3;
    private static final BigDecimal BUFFER = new BigDecimal("1.05");
    private static final BigDecimal ROUND_TO = new BigDecimal("10000");

    public static class Suggestion {
        public final Long categoryId;
        public final BigDecimal suggestedAmount;

        public Suggestion(Long categoryId, BigDecimal suggestedAmount) {
            this.categoryId = categoryId;
            this.suggestedAmount = suggestedAmount;
        }
    }

    /**
     * @param transactions ALL expense transactions (last 3+ months ideally).
     * @param month        Target month (1..12) – the month the suggestion applies to.
     * @param year         Target year.
     */
    // 1. Logic phân tích chi tiêu các tháng trước để tính trung bình và đưa ra gợi ý ngân sách cho từng danh mục
    public List<Suggestion> suggest(List<Transaction> transactions, int month, int year) {
        // Build the list of previous month keys (year*100 + month)
        LocalDate target = LocalDate.of(year, month, 1);
        Set<Integer> prevKeys = new HashSet<>();
        for (int i = 1; i <= LOOKBACK_MONTHS; i++) {
            LocalDate prev = target.minusMonths(i);
            prevKeys.add(prev.getYear() * 100 + prev.getMonthValue());
        }

        // Group: category × monthKey → sum
        Map<Long, Map<Integer, BigDecimal>> grouped = new HashMap<>();

        for (Transaction tx : transactions) {
            if (!"expense".equalsIgnoreCase(tx.getType())) continue;
            if (tx.getCategory() == null) continue;
            LocalDate d = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            int key = d.getYear() * 100 + d.getMonthValue();
            if (!prevKeys.contains(key)) continue;
            Long catId = tx.getCategory().getId();
            grouped.computeIfAbsent(catId, k -> new HashMap<>())
                    .merge(key, tx.getAmount(), BigDecimal::add);
        }

        List<Suggestion> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Long catId = entry.getKey();
            Map<Integer, BigDecimal> byMonth = entry.getValue();
            if (byMonth.isEmpty()) continue;

            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (BigDecimal v : byMonth.values()) {
                if (v.compareTo(BigDecimal.ZERO) > 0) {
                    sum = sum.add(v);
                    count++;
                }
            }
            if (count == 0) continue;

            BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            BigDecimal withBuffer = avg.multiply(BUFFER);

            // Round up to nearest 10000
            BigDecimal rounded = withBuffer
                    .divide(ROUND_TO, 0, RoundingMode.UP)
                    .multiply(ROUND_TO);

            result.add(new Suggestion(catId, rounded));
        }

        // Sort by amount desc
        result.sort((a, b) -> b.suggestedAmount.compareTo(a.suggestedAmount));
        return result;
    }
}
