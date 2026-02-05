package com.rentpro.backend.user;

import com.rentpro.backend.user.dto.CreateTenantRequest;
import com.rentpro.backend.user.dto.TenantResponse;
import com.rentpro.backend.user.dto.UpdateTenantRequest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public UserController(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // OWNER creates tenant
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/tenants")
    public ResponseEntity<?> createTenant(@Valid @RequestBody CreateTenantRequest req, Authentication auth) {

        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        String email = req.email().toLowerCase().trim();
        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User tenant = User.builder()
                .fullName(req.fullName())
                .email(email)
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.TENANT)
                .createdAt(Instant.now())
                .owner(owner) // sets owner_id
                .build();

        userRepo.save(tenant);
        return ResponseEntity.ok(
                new TenantResponse(tenant.getId(), tenant.getFullName(), tenant.getEmail(), tenant.getPhone(),
                        tenant.getCreatedAt()));
    }

    // OWNER gets tenants
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResponse>> myTenants(Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        List<TenantResponse> result = userRepo.findAllByOwner_Id(owner.getId())
                .stream()
                .map(t -> new TenantResponse(t.getId(), t.getFullName(), t.getEmail(), t.getPhone(), t.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(result);
    }

    // OWNER updates tenant
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/tenants/{id}")
    public ResponseEntity<?> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest req,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return userRepo.findByIdAndOwner_Id(id, owner.getId())
                .map(tenant -> {
                    tenant.setFullName(req.fullName());
                    tenant.setPhone(req.phone());
                    userRepo.save(tenant);
                    return ResponseEntity.ok(
                            new TenantResponse(tenant.getId(), tenant.getFullName(), tenant.getEmail(),
                                    tenant.getPhone(), tenant.getCreatedAt()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // OWNER deletes tenant
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<?> deleteTenant(@PathVariable Long id, Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return userRepo.findByIdAndOwner_Id(id, owner.getId())
                .map(t -> {
                    userRepo.delete(t);
                    return ResponseEntity.ok("Tenant deleted");
                })
                .orElseGet(() -> ResponseEntity.status(404).body("Tenant not found"));
    }

}
