package com.smartexpense.server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class AdminUsersPageResponse {
    private List<AdminUserDto> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}
