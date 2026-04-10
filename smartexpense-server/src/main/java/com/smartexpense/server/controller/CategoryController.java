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

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(Authentication auth) {
        return ResponseEntity.ok(categoryService.getAllCategories(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            Authentication auth,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(Authentication auth, @PathVariable Long id) {
        categoryService.deleteCategory(auth.getName(), id);
        return ResponseEntity.ok().build();
    }
}
