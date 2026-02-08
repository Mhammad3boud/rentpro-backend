package com.rentpro.backend.lease;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import com.rentpro.backend.lease.dto.CreateLeaseRequest;
// import com.rentpro.backend.lease.dto.EndLeaseRequest;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leases")
@PreAuthorize("hasRole('OWNER')")
public class LeaseController {

    private final LeaseService leaseService;
    private final UserRepository userRepo;

    public LeaseController(LeaseService leaseService, UserRepository userRepo) {
        this.leaseService = leaseService;
        this.userRepo = userRepo;
    }

    // Create a new lease (assign tenant to unit)
    @PostMapping
    public ResponseEntity<LeaseResponse> create(
            @Valid @RequestBody CreateLeaseRequest req,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Lease lease = leaseService.createLease(
                owner.getId(),
                req.unitId(),
                req.tenantId(),
                req.startDate(),
                req.endDate(),
                req.rentAmount(),
                req.depositAmount(),
                req.notes());

        return ResponseEntity.ok(LeaseResponse.from(lease));
    }

    // End an active lease
    @PostMapping("/{leaseId}/end")
    public ResponseEntity<LeaseResponse> end(
            @PathVariable Long leaseId,
            @RequestParam LocalDate endDate,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Lease lease = leaseService.endLease(owner.getId(), leaseId, endDate);
        return ResponseEntity.ok(LeaseResponse.from(lease));
    }

    // Get active lease for a unit
    @GetMapping("/unit/{unitId}/active")
    public ResponseEntity<LeaseResponse> active(
            @PathVariable Long unitId,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Lease lease = leaseService.getActiveLease(owner.getId(), unitId);

        return lease == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(LeaseResponse.from(lease));
    }

    // Full lease history for a unit
    @GetMapping("/unit/{unitId}/history")
    public ResponseEntity<List<LeaseResponse>> history(@PathVariable Long unitId, Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(
                leaseService.getUnitHistory(owner.getId(), unitId)
                        .stream()
                        .map(LeaseResponse::from)
                        .toList());
    }
}
