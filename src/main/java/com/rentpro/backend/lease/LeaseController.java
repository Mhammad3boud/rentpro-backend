package com.rentpro.backend.lease;

import com.rentpro.backend.lease.dto.AssignTenantRequest;
import com.rentpro.backend.lease.dto.CheckInLeaseRequest;
import com.rentpro.backend.lease.dto.CheckOutLeaseRequest;
import com.rentpro.backend.lease.dto.CreateLeaseRequest;
import com.rentpro.backend.lease.dto.TerminateLeaseRequest;
import com.rentpro.backend.lease.dto.UpdateLeaseRequest;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leases")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    // Create new lease
    @PostMapping
    public Lease createLease(Authentication auth,
                             @RequestBody CreateLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return leaseService.createLease(ownerId, request);
    }

    // Get current user's leases (for owners)
    @GetMapping("/my-leases")
    public List<Lease> getCurrentUserLeases(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID userId = UUID.fromString(ctx.userId());
        return leaseService.getOwnerLeases(userId);
    }

    // Get tenant's leases
    @GetMapping("/tenant-leases")
    public List<Lease> getTenantLeases(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantUserId = UUID.fromString(ctx.userId());
        return leaseService.getTenantLeases(tenantUserId);
    }

    // Get specific lease by ID
    @GetMapping("/{leaseId}")
    public Lease getLeaseById(@PathVariable UUID leaseId) {
        return leaseService.getLeaseById(leaseId);
    }

    // Update existing lease
    @PutMapping("/{leaseId}")
    public Lease updateLease(@PathVariable UUID leaseId,
                             @RequestBody UpdateLeaseRequest request) {
        return leaseService.updateLease(leaseId, request);
    }

    // Get lease by tenant ID
    @GetMapping("/tenant/{tenantId}")
    public Lease getLeaseByTenantId(@PathVariable UUID tenantId) {
        return leaseService.getLeaseByTenantId(tenantId);
    }

    // Assign a tenant to a property/unit (creates a new lease)
    @PostMapping("/assign")
    public Lease assignTenant(Authentication auth,
                              @RequestBody AssignTenantRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return leaseService.assignTenant(ownerId, request);
    }

    // Terminate a lease (sets status to TERMINATED)
    @PutMapping("/{leaseId}/terminate")
    public Lease terminateLease(Authentication auth,
                               @PathVariable UUID leaseId,
                               @RequestBody(required = false) TerminateLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return leaseService.terminateLease(ownerId, ctx.role(), leaseId, request);
    }

    // Check in a lease (owner confirms move-in)
    @PutMapping("/{leaseId}/check-in")
    public Lease checkInLease(Authentication auth,
                              @PathVariable UUID leaseId,
                              @RequestBody(required = false) CheckInLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return leaseService.checkInLease(ownerId, ctx.role(), leaseId, request);
    }

    // Check out a lease (owner confirms move-out)
    @PutMapping("/{leaseId}/check-out")
    public Lease checkOutLease(Authentication auth,
                               @PathVariable UUID leaseId,
                               @RequestBody CheckOutLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return leaseService.checkOutLease(ownerId, ctx.role(), leaseId, request);
    }

    // Delete a lease permanently
    @DeleteMapping("/{leaseId}")
    public void deleteLease(@PathVariable UUID leaseId) {
        leaseService.deleteLease(leaseId);
    }
}
