package com.smartexpense.server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class AdminCategoryDto {
    private Long id;
    private String name;
    private String note;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
