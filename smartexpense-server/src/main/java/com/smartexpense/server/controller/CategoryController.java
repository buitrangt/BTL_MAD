package com.smartexpense.server.controller;

import com.smartexpense.server.dto.CategoryRequest;
import com.smartexpense.server.dto.CategoryResponse;
import com.smartexpense.server.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các request liên quan đến danh mục.
 * Thuộc luồng chức năng: Quản lý danh mục (Tìm kiếm danh mục theo từ khóa, Thêm/Sửa/Xóa danh mục).
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    // 1. Lấy danh sách tất cả danh mục (Hỗ trợ cho tính năng Tìm kiếm danh mục trên client)
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(Authentication auth) {
        return ResponseEntity.ok(categoryService.getAllCategories(auth.getName()));
    }

    // 2. Thêm mới một danh mục
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            Authentication auth,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(auth.getName(), request));
    }

    // 3. Cập nhật thông tin danh mục (Sửa danh mục)
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(auth.getName(), id, request));
    }

    // 4. Xóa một danh mục
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(Authentication auth, @PathVariable Long id) {
        categoryService.deleteCategory(auth.getName(), id);
        return ResponseEntity.ok().build();
    }
}
