package com.rentpro.backend.payment.profile;

import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment-profile")
public class OwnerPaymentProfileController {

    private final OwnerPaymentProfileService profileService;
    private final LeaseRepository leaseRepository;

    public OwnerPaymentProfileController(OwnerPaymentProfileService profileService,
                                         LeaseRepository leaseRepository) {
        this.profileService = profileService;
        this.leaseRepository = leaseRepository;
    }

    /** Owner: get their own payment profile. */
    @GetMapping
    public ResponseEntity<OwnerPaymentProfileResponse> getMyProfile(Authentication auth) {
        UUID userId = resolveUserId(auth);
        return ResponseEntity.ok(profileService.getOrEmpty(userId));
    }

    /** Owner: save/update their payment profile. */
    @PutMapping
    public ResponseEntity<OwnerPaymentProfileResponse> upsertMyProfile(
            Authentication auth,
            @RequestBody OwnerPaymentProfileRequest request) {
        UUID userId = resolveUserId(auth);
        return ResponseEntity.ok(profileService.upsert(userId, request));
    }

    /**
     * Tenant: get the owner's payment profile for a specific lease.
     * The tenant must be the lessee on that lease.
     */
    @GetMapping("/for-lease/{leaseId}")
    public ResponseEntity<OwnerPaymentProfileResponse> getProfileForLease(
            Authentication auth,
            @PathVariable UUID leaseId) {
        UUID tenantUserId = resolveUserId(auth);

        var lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        UUID leaseTenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;
        if (leaseTenantUserId == null || !leaseTenantUserId.equals(tenantUserId)) {
            return ResponseEntity.status(403).build();
        }

        UUID ownerUserId = lease.getProperty().getOwner().getUserId();
        return ResponseEntity.ok(profileService.getOrEmpty(ownerUserId));
    }

    private UUID resolveUserId(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return UUID.fromString(ctx.userId());
    }
}
