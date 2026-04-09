package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.CategoryRequest;
import com.smartexpense.server.dto.CategoryResponse;
import com.smartexpense.server.model.Category;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.CategoryRepository;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public List<CategoryResponse> getAllCategories(String userEmail) {
        User user = findUser(userEmail);
        return categoryRepository.findByUserIdOrDefault(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse createCategory(String userEmail, CategoryRequest request) {
        User user = findUser(userEmail);

        categoryRepository.findByNameAndUserId(request.getName().trim(), user.getId())
                .ifPresent(c -> {
                    throw new RuntimeException("Category already exists");
                });

        Category category = Category.builder()
                .name(request.getName().trim())
                .note(request.getNote())
                .isDefault(false)
                .user(user)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(String userEmail, Long categoryId, CategoryRequest request) {
        User user = findUser(userEmail);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String newName = request.getName().trim();
        categoryRepository.findByNameAndUserId(newName, user.getId())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(categoryId)) {
                        throw new RuntimeException("Category already exists");
                    }
                });

        category.setName(newName);
        category.setNote(request.getNote());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(String userEmail, Long categoryId) {
        User user = findUser(userEmail);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        long txCount = transactionRepository.countByUserIdAndCategoryId(user.getId(), categoryId);
        if (txCount > 0) {
            throw new RuntimeException("Cannot delete category in use by transactions");
        }

        categoryRepository.delete(category);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .note(category.getNote())
                .isDefault(category.getIsDefault())
                .build();
    }
}
