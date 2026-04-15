package com.smartexpense.server.admin.service.impl;

import com.smartexpense.server.admin.dto.AdminCategoryCreateRequest;
import com.smartexpense.server.admin.dto.AdminCategoryDto;
import com.smartexpense.server.admin.dto.AdminOverviewResponse;
import com.smartexpense.server.admin.dto.AdminUserDto;
import com.smartexpense.server.admin.dto.AdminUsersPageResponse;
import com.smartexpense.server.admin.service.AdminService;
import com.smartexpense.server.exception.ResourceNotFoundException;
import com.smartexpense.server.model.Category;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.CategoryRepository;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final CategoryRepository categoryRepository;

    @Override
    public AdminOverviewResponse getOverview() {
        long totalUsers = userRepository.count();

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

        long newToday = userRepository.countByCreatedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        long newYesterday = userRepository.countByCreatedAtBetween(
                today.minusDays(1).atStartOfDay(),
                today.atStartOfDay()
        );
        double newChange = computePercent(newToday, newYesterday);

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

        List<AdminUserDto> recent = userRepository.findRecent().stream()
                .limit(5)
                .map(this::toUserDto)
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

    @Override
    public AdminUsersPageResponse listUsers(String search, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchUsers(
                search == null ? "" : search.trim(), pageable);

        List<AdminUserDto> items = result.getContent().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());

        return AdminUsersPageResponse.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public AdminUserDto setUserLocked(Long userId, boolean locked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLocked(locked);
        userRepository.save(user);
        return toUserDto(user);
    }

    @Override
    public List<AdminCategoryDto> listDefaultCategories() {
        return categoryRepository.findAllDefault().stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminCategoryDto createDefaultCategory(AdminCategoryCreateRequest req) {
        String name = req.getName() == null ? "" : req.getName().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Tên danh mục không được để trống");
        }
        if (categoryRepository.existsDefaultByName(name)) {
            throw new RuntimeException("Danh mục mặc định đã tồn tại");
        }
        Category c = Category.builder()
                .name(name)
                .note(req.getNote())
                .isDefault(true)
                .user(null)
                .build();
        c = categoryRepository.save(c);
        return toCategoryDto(c);
    }

    @Override
    @Transactional
    public void deleteDefaultCategory(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (c.getUser() != null) {
            throw new RuntimeException("Không thể xóa danh mục của người dùng");
        }
        categoryRepository.delete(c);
    }

    private AdminUserDto toUserDto(User u) {
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

    private AdminCategoryDto toCategoryDto(Category c) {
        return AdminCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .note(c.getNote())
                .isDefault(c.getIsDefault())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private double computePercent(long current, long previous) {
        if (previous == 0) return current == 0 ? 0.0 : 100.0;
        return ((double) (current - previous) / previous) * 100.0;
    }
}
