package com.smartexpense.server.service;

import com.smartexpense.server.dto.CategoryRequest;
import com.smartexpense.server.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories(String userEmail);
    CategoryResponse createCategory(String userEmail, CategoryRequest request);
    CategoryResponse updateCategory(String userEmail, Long categoryId, CategoryRequest request);
    void deleteCategory(String userEmail, Long categoryId);
}
