package com.smartexpense.server.admin.controller;

import com.smartexpense.server.admin.dto.AdminCategoryCreateRequest;
import com.smartexpense.server.admin.dto.AdminLockRequest;
import com.smartexpense.server.admin.service.AdminService;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API cho phân hệ Admin: thống kê tổng quan, quản lý người dùng và danh mục mặc định.
 * Mọi endpoint đều kiểm tra quyền ADMIN trước khi xử lý.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    // GET /api/admin/overview - lấy số liệu tổng quan cho dashboard
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(Authentication auth) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.getOverview());
    }

    // GET /api/admin/users - danh sách người dùng (tìm kiếm + phân trang)
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.listUsers(search, page, size));
    }

    // PATCH /api/admin/users/{id}/lock - khóa/mở khóa 1 tài khoản
    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<?> setLock(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody AdminLockRequest body) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.setUserLocked(id, body.getLocked()));
    }

    // GET /api/admin/categories - danh sách danh mục mặc định
    @GetMapping("/categories")
    public ResponseEntity<?> listCategories(Authentication auth) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.listDefaultCategories());
    }

    // POST /api/admin/categories - thêm danh mục mặc định mới
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            Authentication auth,
            @Valid @RequestBody AdminCategoryCreateRequest body) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.createDefaultCategory(body));
    }

    // DELETE /api/admin/categories/{id} - xóa danh mục mặc định
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(Authentication auth, @PathVariable Long id) {
        if (!isAdmin(auth)) return forbidden();
        adminService.deleteDefaultCategory(id);
        return ResponseEntity.noContent().build();
    }

    // Kiểm tra người gọi có vai trò ADMIN hay không
    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // Trả về lỗi 403 khi không đủ quyền
    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body("Forbidden");
    }
}
