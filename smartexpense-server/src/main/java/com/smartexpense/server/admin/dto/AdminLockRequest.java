package com.smartexpense.server.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminLockRequest {
    @NotNull
    private Boolean locked;
}
