package com.smartexpense.server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class AdminOverviewResponse {
    private Long totalUsers;
    private Double percentChangeVsLastMonth;

    private Long newUsersToday;
    private Double newUsersChangePercent;

    private Long activeUsersToday;
    private Double activeUsersChangePercent;

    /** Map keys: Mon..Sun, values: number of registrations */
    private Map<String, Long> weeklyRegistrations;

    /** Most recent N users */
    private List<AdminUserDto> recentUsers;
}
