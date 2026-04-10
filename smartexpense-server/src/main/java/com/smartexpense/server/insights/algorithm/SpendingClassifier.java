package com.smartexpense.server.insights.algorithm;

import com.smartexpense.server.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Classifies the user's spending level for the current month based on saving rate.
 *
 * Levels:
 *   SAVING      → savingRate ≥ 30%
 *   NORMAL      → 10% ≤ savingRate < 30%
 *   WASTEFUL    → 0% ≤ savingRate < 10%
 *   OVERSPENT   → savingRate < 0% (chi > thu)
 *   NO_INCOME   → income == 0
 */
@Component
public class SpendingClassifier {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    public static class Result {
        public final String level;
        public final String note;
        public final BigDecimal income;
        public final BigDecimal expense;
        public final BigDecimal savings;
        public final BigDecimal savingRatePercent;

        public Result(String level, String note, BigDecimal income, BigDecimal expense,
                      BigDecimal savings, BigDecimal savingRatePercent) {
            this.level = level;
            this.note = note;
            this.income = income;
            this.expense = expense;
            this.savings = savings;
            this.savingRatePercent = savingRatePercent;
        }
    }

    public Result classify(List<Transaction> transactions, int month, int year) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            LocalDate d = java.time.Instant.ofEpochMilli(tx.getTimeStamp())
                    .atZone(ZONE).toLocalDate();
            if (d.getMonthValue() != month || d.getYear() != year) continue;

            String type = tx.getType() == null ? "" : tx.getType().toLowerCase();
            if ("income".equals(type)) {
                income = income.add(tx.getAmount());
            } else if ("expense".equals(type)) {
                expense = expense.add(tx.getAmount());
            }
        }

        BigDecimal savings = income.subtract(expense);

        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return new Result(
                    "NO_INCOME",
                    "Tháng này chưa ghi nhận thu nhập, chưa thể đánh giá mức tiết kiệm.",
                    income, expense, savings, BigDecimal.ZERO
            );
        }

        BigDecimal rate = savings.multiply(BigDecimal.valueOf(100))
                .divide(income, 2, RoundingMode.HALF_UP);

        String level;
        String note;
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            level = "OVERSPENT";
            note = "Bạn đang chi tiêu vượt thu nhập " + rate.abs() + "%. " +
                    "Cần xem lại các khoản chi lớn để cân đối.";
        } else if (rate.compareTo(BigDecimal.valueOf(10)) < 0) {
            level = "WASTEFUL";
            note = "Tỷ lệ tiết kiệm chỉ " + rate + "%, ở mức rủi ro. " +
                    "Cố gắng đưa lên ít nhất 10–20%.";
        } else if (rate.compareTo(BigDecimal.valueOf(30)) < 0) {
            level = "NORMAL";
            note = "Bạn đang tiết kiệm " + rate + "% thu nhập – mức ổn. " +
                    "Hướng tới 30% để có quỹ dự phòng tốt hơn.";
        } else {
            level = "SAVING";
            note = "Tuyệt vời! Tỷ lệ tiết kiệm " + rate + "% – bạn đang quản lý tài chính rất tốt.";
        }

        return new Result(level, note, income, expense, savings, rate);
    }
}
