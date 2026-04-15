package com.smartexpense.server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class AdminUserDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private Boolean locked;
    private LocalDateTime createdAt;
}
