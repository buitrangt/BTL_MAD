package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.HomeOverviewResponse;
import com.smartexpense.server.dto.StatsResponse;
import com.smartexpense.server.dto.TransactionResponse;
import com.smartexpense.server.dto.WeeklyOverviewResponse;
import com.smartexpense.server.model.Transaction;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    public StatsResponse getDailyStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = getStartOfDay();
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public StatsResponse getWeeklyStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"));
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public StatsResponse getMonthlyStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public StatsResponse getByCategoryStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public HomeOverviewResponse getHomeOverview(String userEmail) {
        User user = findUser(userEmail);
        TimeZone tz = TimeZone.getTimeZone("Asia/Bangkok");

        // Time boundaries
        Calendar cal = Calendar.getInstance(tz);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long endOfDay = startOfDay + 24L * 60 * 60 * 1000;

        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long startOfWeek = cal.getTimeInMillis();
        long endOfWeek = startOfWeek + 7L * 24 * 60 * 60 * 1000;

        cal.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long endOfMonth = cal.getTimeInMillis();

        // Fetch all month transactions once
        List<Transaction> monthTx = transactionRepository.findByUserIdAndTimeStampBetween(
                user.getId(), startOfMonth, endOfMonth);

        // Today = full day (00:00 → 24:00), Week = full week (Mon → next Mon),
        // Month = full month (1st → 1st next month). This includes future-dated
        // transactions within those boundaries.
        BigDecimal todayAmt = sumExpense(monthTx, startOfDay, endOfDay);
        BigDecimal weekAmt = sumExpense(monthTx, startOfWeek, endOfWeek);
        BigDecimal monthAmt = sumExpense(monthTx, startOfMonth, endOfMonth);
        BigDecimal monthIncome = sumIncome(monthTx, startOfMonth, endOfMonth);
        BigDecimal monthSavings = monthIncome.subtract(monthAmt);

        // Recent 8 transactions (any type)
        List<Transaction> allTx = transactionRepository.findByUserIdOrderByTimeStampDesc(user.getId());
        List<TransactionResponse> recent = allTx.stream()
                .limit(8)
                .map(this::toTxResponse)
                .collect(java.util.stream.Collectors.toList());

        return HomeOverviewResponse.builder()
                .todayAmount(todayAmt)
                .weekAmount(weekAmt)
                .monthAmount(monthAmt)
                .monthIncome(monthIncome)
                .monthExpense(monthAmt)
                .monthSavings(monthSavings)
                .recentTransactions(recent)
                .build();
    }

    private BigDecimal sumExpense(List<Transaction> txs, long start, long end) {
        return txs.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .filter(t -> t.getTimeStamp() >= start && t.getTimeStamp() < end)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumIncome(List<Transaction> txs, long start, long end) {
        return txs.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .filter(t -> t.getTimeStamp() >= start && t.getTimeStamp() < end)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TransactionResponse toTxResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .amount(t.getAmount())
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .type(t.getType())
                .note(t.getNote())
                .timeStamp(t.getTimeStamp())
                .build();
    }

    @Override
    public WeeklyOverviewResponse getWeeklyOverview(String userEmail) {
        User user = findUser(userEmail);
        TimeZone tz = TimeZone.getTimeZone("Asia/Bangkok");

        // Current week: Monday 00:00 → now
        Calendar cal = Calendar.getInstance(tz);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long thisWeekStart = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        // Previous week
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long prevWeekStart = cal.getTimeInMillis();
        long prevWeekEnd = thisWeekStart - 1;

        // Fetch transactions
        List<Transaction> thisWeekTx = transactionRepository.findByUserIdAndTypeAndTimeStampBetween(
                user.getId(), "expense", thisWeekStart, now);
        List<Transaction> prevWeekTx = transactionRepository.findByUserIdAndTypeAndTimeStampBetween(
                user.getId(), "expense", prevWeekStart, prevWeekEnd);

        // Total this week
        BigDecimal totalThisWeek = thisWeekTx.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total previous week
        BigDecimal totalPrevWeek = prevWeekTx.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Percent change
        double percentChange = 0.0;
        if (totalPrevWeek.compareTo(BigDecimal.ZERO) > 0) {
            percentChange = totalThisWeek.subtract(totalPrevWeek)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalPrevWeek, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Daily breakdown (Mon → Sun)
        String[] dayKeys = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Map<String, BigDecimal> dailyBreakdown = new LinkedHashMap<>();
        for (String day : dayKeys) {
            dailyBreakdown.put(day, BigDecimal.ZERO);
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.ENGLISH);
        dayFormat.setTimeZone(tz);
        for (Transaction tx : thisWeekTx) {
            String dayLabel = dayFormat.format(new Date(tx.getTimeStamp()));
            dailyBreakdown.merge(dayLabel, tx.getAmount(), BigDecimal::add);
        }

        // Category breakdown with percent
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        for (Transaction tx : thisWeekTx) {
            String catName = tx.getCategory() != null ? tx.getCategory().getName() : "Khác";
            categoryTotals.merge(catName, tx.getAmount(), BigDecimal::add);
        }

        List<WeeklyOverviewResponse.CategoryStat> categoryStats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            double pct = totalThisWeek.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(totalThisWeek, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            categoryStats.add(WeeklyOverviewResponse.CategoryStat.builder()
                    .category(entry.getKey())
                    .amount(entry.getValue())
                    .percent(pct)
                    .build());
        }

        // Sort by amount descending
        categoryStats.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        return WeeklyOverviewResponse.builder()
                .totalAmount(totalThisWeek)
                .percentChange(percentChange)
                .dailyBreakdown(dailyBreakdown)
                .categoryBreakdown(categoryStats)
                .build();
    }

    private StatsResponse buildStats(Long userId, long start, long end) {
        var transactions = transactionRepository.findByUserIdAndTypeAndTimeStampBetween(userId, "expense", start, end);
        BigDecimal total = transactions.stream()
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categoryBreakdown = new LinkedHashMap<>();
        var categoryResults = transactionRepository.sumByCategoryAndType(userId, "expense", start, end);
        for (Object[] row : categoryResults) {
            categoryBreakdown.put((String) row[0], (BigDecimal) row[1]);
        }

        return StatsResponse.builder()
                .totalAmount(total)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    private Calendar getStartOfDay() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
