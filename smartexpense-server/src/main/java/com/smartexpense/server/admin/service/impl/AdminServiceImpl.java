package com.smartexpense.server.admin.service.impl;

import com.smartexpense.server.admin.dto.AdminOverviewResponse;
import com.smartexpense.server.admin.dto.AdminUserDto;
import com.smartexpense.server.admin.service.AdminService;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public AdminOverviewResponse getOverview() {
        // ===== Total users =====
        long totalUsers = userRepository.count();

        // ===== Created this month / last month =====
        LocalDate today = LocalDate.now(ZONE);
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate firstOfLastMonth = firstOfMonth.minusMonths(1);

        long createdThisMonth = userRepository.countByCreatedAtBetween(
                firstOfMonth.atStartOfDay(),
                firstOfMonth.plusMonths(1).atStartOfDay()
        );
        long createdLastMonth = userRepository.countByCreatedAtBetween(
                firstOfLastMonth.atStartOfDay(),
                firstOfMonth.atStartOfDay()
        );

        double totalChange = computePercent(createdThisMonth, createdLastMonth);

        // ===== New users today / yesterday =====
        long newToday = userRepository.countByCreatedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        long newYesterday = userRepository.countByCreatedAtBetween(
                today.minusDays(1).atStartOfDay(),
                today.atStartOfDay()
        );
        double newChange = computePercent(newToday, newYesterday);

        // ===== Active users today =====
        // Definition: distinct users who have at least 1 transaction created today
        long startOfDayMs = today.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long endOfDayMs = today.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
        long startYesterdayMs = today.minusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();

        long activeToday = transactionRepository.findAll().stream()
                .filter(t -> t.getTimeStamp() != null
                        && t.getTimeStamp() >= startOfDayMs
                        && t.getTimeStamp() < endOfDayMs)
                .map(t -> t.getUser() != null ? t.getUser().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        long activeYesterday = transactionRepository.findAll().stream()
                .filter(t -> t.getTimeStamp() != null
                        && t.getTimeStamp() >= startYesterdayMs
                        && t.getTimeStamp() < startOfDayMs)
                .map(t -> t.getUser() != null ? t.getUser().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        double activeChange = computePercent(activeToday, activeYesterday);

        // ===== Weekly registrations (Mon..Sun of current week) =====
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sundayNext = monday.plusDays(7);

        List<User> weekUsers = userRepository.findByCreatedAtBetween(
                monday.atStartOfDay(),
                sundayNext.atStartOfDay()
        );

        Map<String, Long> weekly = new LinkedHashMap<>();
        weekly.put("Mon", 0L);
        weekly.put("Tue", 0L);
        weekly.put("Wed", 0L);
        weekly.put("Thu", 0L);
        weekly.put("Fri", 0L);
        weekly.put("Sat", 0L);
        weekly.put("Sun", 0L);

        for (User u : weekUsers) {
            if (u.getCreatedAt() == null) continue;
            DayOfWeek dow = u.getCreatedAt().getDayOfWeek();
            String key = switch (dow) {
                case MONDAY -> "Mon";
                case TUESDAY -> "Tue";
                case WEDNESDAY -> "Wed";
                case THURSDAY -> "Thu";
                case FRIDAY -> "Fri";
                case SATURDAY -> "Sat";
                case SUNDAY -> "Sun";
            };
            weekly.merge(key, 1L, Long::sum);
        }

        // ===== Recent users (top 5) =====
        List<AdminUserDto> recent = userRepository.findRecent().stream()
                .limit(5)
                .map(this::toDto)
                .collect(Collectors.toList());

        return AdminOverviewResponse.builder()
                .totalUsers(totalUsers)
                .percentChangeVsLastMonth(totalChange)
                .newUsersToday(newToday)
                .newUsersChangePercent(newChange)
                .activeUsersToday(activeToday)
                .activeUsersChangePercent(activeChange)
                .weeklyRegistrations(weekly)
                .recentUsers(recent)
                .build();
    }

    private AdminUserDto toDto(User u) {
        return AdminUserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .locked(u.getLocked())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private double computePercent(long current, long previous) {
        if (previous == 0) return current == 0 ? 0.0 : 100.0;
        return ((double) (current - previous) / previous) * 100.0;
    }
}
