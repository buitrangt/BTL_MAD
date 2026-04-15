package com.smartexpense.server.admin.service;

import com.smartexpense.server.admin.dto.AdminCategoryCreateRequest;
import com.smartexpense.server.admin.dto.AdminCategoryDto;
import com.smartexpense.server.admin.dto.AdminOverviewResponse;
import com.smartexpense.server.admin.dto.AdminUserDto;
import com.smartexpense.server.admin.dto.AdminUsersPageResponse;

import java.util.List;

public interface AdminService {
    AdminOverviewResponse getOverview();

    AdminUsersPageResponse listUsers(String search, int page, int size);
    AdminUserDto setUserLocked(Long userId, boolean locked);

    List<AdminCategoryDto> listDefaultCategories();
    AdminCategoryDto createDefaultCategory(AdminCategoryCreateRequest req);
    void deleteDefaultCategory(Long id);
}
