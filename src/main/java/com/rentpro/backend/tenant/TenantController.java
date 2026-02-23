package com.rentpro.backend.tenant;

import com.rentpro.backend.tenant.dto.CreateTenantRequest;
import com.rentpro.backend.tenant.dto.UpdateTenantProfileRequest;
import com.rentpro.backend.tenant.dto.UpdateTenantRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    // OWNER creates tenant
    @PostMapping("/owner/{ownerId}")
    public Tenant createTenant(@PathVariable UUID ownerId,
                               @RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(ownerId, request);
    }

    // OWNER lists tenants
    @GetMapping("/owner/{ownerId}")
    public List<Tenant> getOwnerTenants(@PathVariable UUID ownerId) {
        return tenantService.getOwnerTenants(ownerId);
    }

    // OWNER updates tenant
    @PutMapping("/{tenantId}")
    public Tenant updateTenant(@PathVariable UUID tenantId,
                               @RequestBody UpdateTenantRequest request) {
        return tenantService.updateTenant(tenantId, request);
    }

    // TENANT updates their profile (if enabled)
    @PutMapping("/{tenantId}/profile/{tenantUserId}")
    public Tenant updateProfile(@PathVariable UUID tenantId,
                                @PathVariable UUID tenantUserId,
                                @RequestBody UpdateTenantProfileRequest request) {
        return tenantService.updateTenantProfile(tenantId, tenantUserId, request);
    }

    // OWNER deletes tenant
    @DeleteMapping("/{tenantId}")
    public void deleteTenant(@PathVariable UUID tenantId) {
        tenantService.deleteTenant(tenantId);
    }
}
