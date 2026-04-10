package com.smartexpense.server.insights.algorithm;

import com.smartexpense.server.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Detects per-category overspending anomalies for the current month
 * by comparing against the average of previous N months.
 *
 * Algorithm:
 *   1. For each category in current month, compute current spending.
 *   2. Compute average spending of that category in previous 3 months.
 *   3. percent_increase = (current - avg) / avg * 100
 *   4. Flag as anomaly if percent_increase >= ANOMALY_THRESHOLD (default 30%)
 *      AND amount_diff is meaningful (>= MIN_AMOUNT_DIFF)
 *   5. Cap percent_increase at 999.99 to fit decimal(5,2).
 */
@Component
public class AnomalyDetector {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int LOOKBACK_MONTHS = 3;
    private static final BigDecimal ANOMALY_THRESHOLD_PERCENT = new BigDecimal("30");
    private static final BigDecimal MIN_AMOUNT_DIFF = new BigDecimal("50000");
    private static final BigDecimal MAX_PERCENT = new BigDecimal("999.99");

    public static class Anomaly {
        public final Long categoryId;
        public final BigDecimal currentAmount;
        public final BigDecimal avgAmount;
        public final BigDecimal amountDiff;
        public final BigDecimal percentIncrease;

        public Anomaly(Long categoryId, BigDecimal currentAmount, BigDecimal avgAmount,
                       BigDecimal amountDiff, BigDecimal percentIncrease) {
            this.categoryId = categoryId;
            this.currentAmount = currentAmount;
            this.avgAmount = avgAmount;
            this.amountDiff = amountDiff;
            this.percentIncrease = percentIncrease;
        }
    }

    /**
     * @param transactions   ALL expense transactions of user (last 4+ months ideally).
     * @param month          Current month (1..12)
     * @param year           Current year
     */
    public List<Anomaly> detect(List<Transaction> transactions, int month, int year) {
        // Group by category × monthKey
        // monthKey = year*100 + month (e.g. 202604)
        int currentKey = year * 100 + month;

        Map<Long, Map<Integer, BigDecimal>> grouped = new HashMap<>();

        for (Transaction tx : transactions) {
            if (!"expense".equalsIgnoreCase(tx.getType())) continue;
            if (tx.getCategory() == null) continue;
            LocalDate d = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            int key = d.getYear() * 100 + d.getMonthValue();
            Long catId = tx.getCategory().getId();
            grouped.computeIfAbsent(catId, k -> new HashMap<>())
                    .merge(key, tx.getAmount(), BigDecimal::add);
        }

        // Build list of previous month keys (3 months back)
        List<Integer> prevKeys = new ArrayList<>();
        LocalDate cur = LocalDate.of(year, month, 1);
        for (int i = 1; i <= LOOKBACK_MONTHS; i++) {
            LocalDate prev = cur.minusMonths(i);
            prevKeys.add(prev.getYear() * 100 + prev.getMonthValue());
        }

        List<Anomaly> anomalies = new ArrayList<>();

        for (var catEntry : grouped.entrySet()) {
            Long catId = catEntry.getKey();
            Map<Integer, BigDecimal> byMonth = catEntry.getValue();
            BigDecimal current = byMonth.getOrDefault(currentKey, BigDecimal.ZERO);
            if (current.compareTo(BigDecimal.ZERO) == 0) continue;

            // Compute average of previous months (only count months that have data)
            BigDecimal sumPrev = BigDecimal.ZERO;
            int countPrev = 0;
            for (Integer pk : prevKeys) {
                BigDecimal v = byMonth.get(pk);
                if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                    sumPrev = sumPrev.add(v);
                    countPrev++;
                }
            }

            if (countPrev == 0) {
                // Brand new category — flag as anomaly with 999.99% if current is significant
                if (current.compareTo(MIN_AMOUNT_DIFF) >= 0) {
                    anomalies.add(new Anomaly(catId, current, BigDecimal.ZERO,
                            current, MAX_PERCENT));
                }
                continue;
            }

            BigDecimal avg = sumPrev.divide(BigDecimal.valueOf(countPrev), 2, RoundingMode.HALF_UP);
            BigDecimal diff = current.subtract(avg);
            if (diff.compareTo(MIN_AMOUNT_DIFF) < 0) continue;

            BigDecimal percent = diff.multiply(BigDecimal.valueOf(100))
                    .divide(avg, 2, RoundingMode.HALF_UP);

            if (percent.compareTo(ANOMALY_THRESHOLD_PERCENT) < 0) continue;

            if (percent.compareTo(MAX_PERCENT) > 0) percent = MAX_PERCENT;

            anomalies.add(new Anomaly(catId, current, avg, diff, percent));
        }

        // Sort by percent desc
        anomalies.sort((a, b) -> b.percentIncrease.compareTo(a.percentIncrease));
        return anomalies;
    }
}
