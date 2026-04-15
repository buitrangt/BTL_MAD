package com.smartexpense.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCategoryCreateRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String note;
}
