package com.smartexpense.server.admin.controller;

import com.smartexpense.server.admin.dto.AdminOverviewResponse;
import com.smartexpense.server.admin.service.AdminService;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok(adminService.getOverview());
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }
}
