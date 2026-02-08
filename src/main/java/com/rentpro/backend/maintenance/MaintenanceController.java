package com.rentpro.backend.maintenance;

import com.rentpro.backend.maintenance.dto.CreateMaintenanceRequest;
import com.rentpro.backend.maintenance.dto.MaintenanceResponse;
import com.rentpro.backend.maintenance.dto.UpdateMaintenanceStatusRequest;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceService service;
    private final UserRepository userRepo;

    public MaintenanceController(MaintenanceService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // TENANT creates request
    @PostMapping
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<MaintenanceResponse> create(@Valid @RequestBody CreateMaintenanceRequest req, Authentication auth) {
        User tenant = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return ResponseEntity.ok(MaintenanceResponse.from(service.createAsTenant(tenant.getId(), req)));
    }

    // TENANT views own requests
    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<List<MaintenanceResponse>> my(Authentication auth) {
        User tenant = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return ResponseEntity.ok(
                service.listMineAsTenant(tenant.getId()).stream().map(MaintenanceResponse::from).toList()
        );
    }

    // OWNER views all requests for all their properties
    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<MaintenanceResponse>> ownerAll(Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(
                service.listAllForOwner(owner.getId()).stream().map(MaintenanceResponse::from).toList()
        );
    }

    // OWNER updates status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<MaintenanceResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest req,
            Authentication auth
    ) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(MaintenanceResponse.from(service.updateStatusAsOwner(owner.getId(), id, req)));
    }
}
