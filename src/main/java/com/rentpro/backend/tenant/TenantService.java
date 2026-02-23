package com.rentpro.backend.tenant;

import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.lease.LeaseStatus;
import com.rentpro.backend.tenant.dto.CreateTenantRequest;
import com.rentpro.backend.tenant.dto.UpdateTenantProfileRequest;
import com.rentpro.backend.tenant.dto.UpdateTenantRequest;
import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         LeaseRepository leaseRepository,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // OWNER creates tenant (User + Tenant profile)
    public Tenant createTenant(UUID ownerId, CreateTenantRequest request) {

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // Prevent duplicate emails
        userRepository.findByEmail(request.email())
                .ifPresent(u -> { throw new RuntimeException("Email already exists"); });

        // Create tenant login user
        User tenantUser = new User();
        tenantUser.setEmail(request.email());
        tenantUser.setPassword(passwordEncoder.encode(request.password()));
        tenantUser.setRole(Role.TENANT);
        tenantUser = userRepository.save(tenantUser);

        // Create tenant profile
        Tenant tenant = new Tenant();
        tenant.setOwner(owner);
        tenant.setUser(tenantUser);
        tenant.setFullName(request.fullName());
        tenant.setPhone(request.phone());
        tenant.setEmergencyContact(request.emergencyContact());
        tenant.setAddress(request.address());

        return tenantRepository.save(tenant);
    }

    // OWNER lists only their tenants
    public List<Tenant> getOwnerTenants(UUID ownerId) {
        return tenantRepository.findByOwner_UserId(ownerId);
    }

    // TENANT updates profile fields (if allowed)
    public Tenant updateTenantProfile(UUID tenantId, UUID tenantUserId, UpdateTenantProfileRequest request) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Security check: tenant can only update their own profile
        if (!tenant.getUser().getUserId().equals(tenantUserId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (request.phone() != null) tenant.setPhone(request.phone());
        if (request.emergencyContact() != null) tenant.setEmergencyContact(request.emergencyContact());
        if (request.address() != null) tenant.setAddress(request.address());

        return tenantRepository.save(tenant);
    }

    // OWNER updates tenant
    public Tenant updateTenant(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (request.fullName() != null) tenant.setFullName(request.fullName());
        if (request.phone() != null) tenant.setPhone(request.phone());
        if (request.emergencyContact() != null) tenant.setEmergencyContact(request.emergencyContact());
        if (request.address() != null) tenant.setAddress(request.address());

        return tenantRepository.save(tenant);
    }

    // OWNER deletes tenant
    @Transactional
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Check for active leases
        List<Lease> leases = leaseRepository.findByTenant_TenantId(tenantId);
        boolean hasActiveLeases = leases.stream()
                .anyMatch(l -> l.getLeaseStatus() == LeaseStatus.ACTIVE);

        if (hasActiveLeases) {
            throw new RuntimeException("Cannot delete tenant with active leases. Please terminate or expire the leases first.");
        }

        // Delete all associated leases (expired/terminated ones) - cascades to rent_payments and maintenance_requests
        leaseRepository.deleteAll(leases);
        leaseRepository.flush(); // Ensure leases are deleted before tenant

        // Get the user account associated with this tenant
        User tenantUser = tenant.getUser();

        // Delete tenant profile
        tenantRepository.delete(tenant);
        tenantRepository.flush(); // Ensure tenant is deleted before user

        // Delete user account
        if (tenantUser != null) {
            userRepository.delete(tenantUser);
        }
    }
}
