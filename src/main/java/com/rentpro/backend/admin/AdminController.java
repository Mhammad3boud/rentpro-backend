package com.rentpro.backend.admin;

import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserSummary> listUsers() {
        return adminService.getAllUsers().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/stats")
    public AdminService.AdminStats getStats() {
        return adminService.getStats();
    }

    @GetMapping("/analytics")
    public AdminService.AdminAnalytics getAnalytics() {
        return adminService.getAnalytics();
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Void> setStatus(@PathVariable UUID id, @RequestBody StatusRequest req) {
        adminService.setUserStatus(id, req.active());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> setRole(@PathVariable UUID id, @RequestBody RoleRequest req) {
        try {
            adminService.setUserRole(id, Role.valueOf(req.role()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    private UserSummary toSummary(User u) {
        return new UserSummary(
                u.getUserId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole().name(),
                u.getStatus(),
                u.getCreatedAt()
        );
    }

    public record StatusRequest(boolean active) {}
    public record RoleRequest(String role) {}
    public record UserSummary(
            UUID userId,
            String fullName,
            String email,
            String role,
            Boolean status,
            LocalDateTime createdAt
    ) {}
}
