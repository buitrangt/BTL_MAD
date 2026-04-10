package com.smartexpense.server.insights.algorithm;

import com.smartexpense.server.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;

/**
 * Predicts total monthly spending using a linear regression on the daily series
 * built from the user's transactions in the current month.
 *
 * Algorithm:
 *   1. Build daily totals (day of month → sum) for the current month so far.
 *   2. Cumulative the daily totals → cumulative[d] = sum of expenses up to day d.
 *   3. Fit linear regression y = a*x + b on the cumulative series.
 *   4. Project to the last day of the month → predicted total.
 *   5. Floor predicted at currentTotal (cannot decrease) and at simple
 *      proportional projection in case regression is unstable.
 */
@Component
public class ForecastEngine {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    /**
     * @param transactions      ALL expense transactions of user (any time range, will be filtered).
     * @param month             1..12
     * @param year              e.g. 2026
     * @return                  predicted total spending for the full month.
     */
    public BigDecimal predictMonthlyTotal(List<Transaction> transactions, int month, int year) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastOfMonth = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        LocalDate today = LocalDate.now(ZONE);
        LocalDate cutoff = today.isBefore(lastOfMonth) ? today : lastOfMonth;

        // Build daily totals (day -> sum)
        TreeMap<Integer, BigDecimal> daily = new TreeMap<>();
        for (int d = 1; d <= cutoff.getDayOfMonth(); d++) {
            daily.put(d, BigDecimal.ZERO);
        }
        for (Transaction tx : transactions) {
            if (!"expense".equalsIgnoreCase(tx.getType())) continue;
            LocalDate txDate = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            if (txDate.getMonthValue() != month || txDate.getYear() != year) continue;
            if (txDate.isAfter(cutoff)) continue;
            int day = txDate.getDayOfMonth();
            daily.merge(day, tx.getAmount(), BigDecimal::add);
        }

        // Cumulative
        TreeMap<Integer, BigDecimal> cumulative = new TreeMap<>();
        BigDecimal running = BigDecimal.ZERO;
        for (var entry : daily.entrySet()) {
            running = running.add(entry.getValue());
            cumulative.put(entry.getKey(), running);
        }

        BigDecimal currentTotal = running;
        int daysSoFar = cutoff.getDayOfMonth();
        int totalDays = lastOfMonth.getDayOfMonth();

        if (daysSoFar == 0 || currentTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Simple proportional fallback (linear from origin)
        BigDecimal proportionalForecast = currentTotal
                .multiply(BigDecimal.valueOf(totalDays))
                .divide(BigDecimal.valueOf(daysSoFar), 2, RoundingMode.HALF_UP);

        // Linear regression: y = a*x + b on cumulative series
        if (daysSoFar < 3) {
            return proportionalForecast;
        }

        double n = daysSoFar;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (var entry : cumulative.entrySet()) {
            double x = entry.getKey();
            double y = entry.getValue().doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double meanX = sumX / n;
        double meanY = sumY / n;
        double denom = sumX2 - n * meanX * meanX;
        if (denom == 0) {
            return proportionalForecast;
        }
        double a = (sumXY - n * meanX * meanY) / denom;
        double b = meanY - a * meanX;
        double regressionForecast = a * totalDays + b;

        // Pick max of regression and proportional, but never below currentTotal
        BigDecimal result = BigDecimal.valueOf(regressionForecast)
                .setScale(2, RoundingMode.HALF_UP);
        if (result.compareTo(proportionalForecast) < 0) {
            result = proportionalForecast;
        }
        if (result.compareTo(currentTotal) < 0) {
            result = currentTotal;
        }
        return result;
    }

    /**
     * Helper: returns the current month's total expenses.
     */
    public BigDecimal currentMonthTotal(List<Transaction> transactions, int month, int year) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (!"expense".equalsIgnoreCase(tx.getType())) continue;
            LocalDate txDate = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            if (txDate.getMonthValue() == month && txDate.getYear() == year) {
                total = total.add(tx.getAmount());
            }
        }
        return total;
    }
}
