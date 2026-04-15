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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(Authentication auth) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.getOverview());
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.listUsers(search, page, size));
    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<?> setLock(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody AdminLockRequest body) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.setUserLocked(id, body.getLocked()));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> listCategories(Authentication auth) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.listDefaultCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            Authentication auth,
            @Valid @RequestBody AdminCategoryCreateRequest body) {
        if (!isAdmin(auth)) return forbidden();
        return ResponseEntity.ok(adminService.createDefaultCategory(body));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(Authentication auth, @PathVariable Long id) {
        if (!isAdmin(auth)) return forbidden();
        adminService.deleteDefaultCategory(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body("Forbidden");
    }
}
