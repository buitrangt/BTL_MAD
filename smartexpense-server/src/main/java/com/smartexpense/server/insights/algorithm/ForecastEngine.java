package com.smartexpense.server.insights.algorithm;

import com.smartexpense.server.model.Transaction;
import com.smartexpense.server.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
@Slf4j
@Component
@RequiredArgsConstructor
public class ForecastEngine {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Forecast provider:
     * - gemini: call Gemini using daily totals (recommended when key exists)
     * - local: use built-in linear regression + proportional fallback
     */
    @Value("${insights.forecast.provider:gemini}")
    private String provider;

    private final GeminiService geminiService;

    public static class ForecastResult {
        public final BigDecimal predictedAmount;
        public final String provider; // "GEMINI" | "LOCAL"

        public ForecastResult(BigDecimal predictedAmount, String provider) {
            this.predictedAmount = predictedAmount;
            this.provider = provider;
        }
    }

    /**
     * @param transactions      ALL expense transactions of user (any time range, will be filtered).
     * @param month             1..12
     * @param year              e.g. 2026
     * @return                  predicted total spending for the full month.
     */
    public BigDecimal predictMonthlyTotal(List<Transaction> transactions, int month, int year) {
        return predictMonthlyTotalResult(transactions, month, year).predictedAmount;
    }

    public ForecastResult predictMonthlyTotalResult(List<Transaction> transactions, int month, int year) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastOfMonth = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        LocalDate today = LocalDate.now(ZONE);
        LocalDate cutoff = today.isBefore(lastOfMonth) ? today : lastOfMonth;

        // Build daily totals (day -> sum)
        TreeMap<Integer, BigDecimal> daily = new TreeMap<>();
        for (int d = 1; d <= cutoff.getDayOfMonth(); d++) {
            daily.put(d, BigDecimal.ZERO);
        }

        // 90-day history (date -> sum), ending at cutoff
        Map<String, BigDecimal> last90Daily = buildLastNDaysDailyTotals(transactions, cutoff, 90);
        Map<String, BigDecimal> last3MonthsTotals = buildLastNMonthsTotals(transactions, firstOfMonth.minusMonths(3), firstOfMonth.minusDays(1));

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
            return new ForecastResult(BigDecimal.ZERO, "LOCAL");
        }

        BigDecimal localForecast = localForecast(cumulative, currentTotal, daysSoFar, totalDays);

        if (!"gemini".equalsIgnoreCase(provider)) {
            return new ForecastResult(localForecast, "LOCAL");
        }

        BigDecimal geminiForecast = geminiForecast(
                daily,
                last90Daily,
                last3MonthsTotals,
                currentTotal,
                daysSoFar,
                totalDays,
                month,
                year
        );
        if (geminiForecast != null) {
            return new ForecastResult(geminiForecast, "GEMINI");
        }
        return new ForecastResult(localForecast, "LOCAL");
    }

    private BigDecimal localForecast(
            TreeMap<Integer, BigDecimal> cumulative,
            BigDecimal currentTotal,
            int daysSoFar,
            int totalDays
    ) {
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
     * Calls Gemini using daily totals only (Cách A).
     * Returns null if Gemini fails or response cannot be parsed safely.
     */
    private BigDecimal geminiForecast(
            TreeMap<Integer, BigDecimal> dailyTotals,
            Map<String, BigDecimal> last90DailyTotals,
            Map<String, BigDecimal> last3MonthsTotals,
            BigDecimal currentTotal,
            int daysSoFar,
            int totalDays,
            int month,
            int year
    ) {
        try {
            String prompt = buildGeminiForecastPrompt(
                    dailyTotals,
                    last90DailyTotals,
                    last3MonthsTotals,
                    currentTotal,
                    daysSoFar,
                    totalDays,
                    month,
                    year
            );
            String reply = geminiService.chat(null, null, prompt);
            if (reply == null) return null;

            String trimmed = reply.trim();
            if (trimmed.isBlank()
                    || trimmed.startsWith("[Lỗi cấu hình]")
                    || trimmed.startsWith("Lỗi gọi AI:")
                    || trimmed.startsWith("Lỗi kết nối AI:")
                    || trimmed.startsWith("Không nhận được phản hồi")) {
                return null;
            }

            // Gemini sometimes wraps JSON in markdown fences; strip them.
            String jsonText = trimmed
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JSONObject obj = new JSONObject(jsonText);
            if (!obj.has("predictedAmount")) return null;

            BigDecimal predicted = new BigDecimal(String.valueOf(obj.get("predictedAmount")))
                    .setScale(2, RoundingMode.HALF_UP);

            // Guardrails: never below current total.
            if (predicted.compareTo(currentTotal) < 0) {
                predicted = currentTotal;
            }
            return predicted;
        } catch (Exception e) {
            log.debug("Gemini forecast skipped: {}", e.getMessage());
            return null;
        }
    }

    private String buildGeminiForecastPrompt(
            TreeMap<Integer, BigDecimal> dailyTotals,
            Map<String, BigDecimal> last90DailyTotals,
            Map<String, BigDecimal> last3MonthsTotals,
            BigDecimal currentTotal,
            int daysSoFar,
            int totalDays,
            int month,
            int year
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a forecasting engine for monthly personal spending.\n");
        sb.append("Task: predict total expense for the full month.\n");
        sb.append("Rules:\n");
        sb.append("- Use ONLY the provided data. Do not invent numbers.\n");
        sb.append("- Return ONLY valid JSON, no extra text.\n");
        sb.append("- Output schema: {\"predictedAmount\": number}\n");
        sb.append("- The predictedAmount must be >= currentTotal.\n\n");

        sb.append("Context:\n");
        sb.append("- month: ").append(month).append("\n");
        sb.append("- year: ").append(year).append("\n");
        sb.append("- daysSoFar: ").append(daysSoFar).append("\n");
        sb.append("- totalDaysInMonth: ").append(totalDays).append("\n");
        sb.append("- currentTotal: ").append(currentTotal).append("\n\n");

        sb.append("Current-month daily totals so far (dayOfMonth -> totalExpenseThatDay):\n");
        sb.append("{");
        boolean first = true;
        for (var e : dailyTotals.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append("}\n");

        sb.append("\nLast 90 days daily totals (ISO date -> totalExpenseThatDay):\n");
        sb.append("{");
        first = true;
        for (var e : last90DailyTotals.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append("}\n");

        sb.append("\nLast 3 full months totals (YYYY-MM -> totalExpenseThatMonth):\n");
        sb.append("{");
        first = true;
        for (var e : last3MonthsTotals.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append("}\n");

        return sb.toString();
    }

    private Map<String, BigDecimal> buildLastNDaysDailyTotals(List<Transaction> transactions, LocalDate endInclusive, int days) {
        LocalDate start = endInclusive.minusDays(days - 1L);
        // keep stable order for prompt readability
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            out.put(d.format(ISO_DATE), BigDecimal.ZERO);
        }

        for (Transaction tx : transactions) {
            if (!"expense".equalsIgnoreCase(tx.getType())) continue;
            LocalDate txDate = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            if (txDate.isBefore(start) || txDate.isAfter(endInclusive)) continue;
            String key = txDate.format(ISO_DATE);
            out.put(key, out.get(key).add(tx.getAmount()));
        }
        return out;
    }

    /**
     * Builds total expenses for each month between start..end (inclusive), keyed by YYYY-MM.
     * Intended to provide a compact summary to the model.
     */
    private Map<String, BigDecimal> buildLastNMonthsTotals(List<Transaction> transactions, LocalDate startInclusive, LocalDate endInclusive) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (Transaction tx : transactions) {
            if (!"expense".equalsIgnoreCase(tx.getType())) continue;
            LocalDate txDate = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            if (txDate.isBefore(startInclusive) || txDate.isAfter(endInclusive)) continue;
            String ym = txDate.getYear() + "-" + String.format("%02d", txDate.getMonthValue());
            totals.merge(ym, tx.getAmount(), BigDecimal::add);
        }
        return totals;
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
