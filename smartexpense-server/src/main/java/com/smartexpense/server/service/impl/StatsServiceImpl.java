package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.StatsResponse;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
