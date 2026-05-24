package com.rentpro.backend.tenant;

import com.rentpro.backend.storage.StorageService;
import com.rentpro.backend.tenant.dto.CreateTenantRequest;
import com.rentpro.backend.tenant.dto.UpdateTenantProfileRequest;
import com.rentpro.backend.tenant.dto.UpdateTenantRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final StorageService storageService;

    public TenantController(TenantService tenantService,
                            TenantRepository tenantRepository,
                            StorageService storageService) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.storageService = storageService;
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

    // Upload IC / passport photo for a tenant
    @PostMapping(value = "/{tenantId}/ic-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadIdPhoto(@PathVariable UUID tenantId,
                                           @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        try {
            String ext = resolveExtension(file.getOriginalFilename());
            String filename = tenantId + "_ic_" + System.currentTimeMillis() + ext;

            if (tenant.getIcPhotoUrl() != null) {
                storageService.deleteByUrl(tenant.getIcPhotoUrl());
            }

            String url = storageService.uploadTenantIdPhoto(filename, file);
            tenant.setIcPhotoUrl(url);
            tenantRepository.save(tenant);

            return ResponseEntity.ok(Map.of("icPhotoUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
