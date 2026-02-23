package com.rentpro.backend.maintenance;

import com.rentpro.backend.maintenance.dto.CreateMaintenanceRequest;
import com.rentpro.backend.maintenance.dto.UpdateMaintenanceStatusRequest;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceService service;

    public MaintenanceController(MaintenanceService service) {
        this.service = service;
    }

    // Create maintenance request
    @PostMapping("/requests")
    public MaintenanceRequest createRequest(Authentication auth,
                                           @RequestBody CreateMaintenanceRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantId = UUID.fromString(ctx.userId());
        return service.createRequest(tenantId, request);
    }

    // Get current user's maintenance requests (for tenants)
    @GetMapping("/my-requests")
    public List<MaintenanceRequest> getCurrentUserRequests(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantId = UUID.fromString(ctx.userId());
        return service.getTenantRequests(tenantId);
    }

    // Get all maintenance requests for properties owned by current user (for owners)
    @GetMapping("/property-requests")
    public List<MaintenanceRequest> getPropertyRequests(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return service.getOwnerRequests(ownerId);
    }

    // Update maintenance request status
    @PutMapping("/requests/{requestId}/status")
    public MaintenanceRequest updateRequestStatus(Authentication auth,
                                                 @PathVariable UUID requestId,
                                                 @RequestBody UpdateMaintenanceStatusRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return service.updateStatus(ownerId, requestId, request);
    }

    // Get specific maintenance request by ID
    @GetMapping("/requests/{requestId}")
    public MaintenanceRequest getRequestById(@PathVariable UUID requestId) {
        return service.getRequestById(requestId);
    }

    // Delete maintenance request
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> deleteRequest(Authentication auth,
                                              @PathVariable UUID requestId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        service.deleteRequest(ownerId, requestId);
        return ResponseEntity.noContent().build();
    }

    // Upload photo for maintenance request
    @PostMapping(value = "/requests/{requestId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MaintenancePhoto uploadPhoto(@PathVariable UUID requestId,
                                        @RequestParam("file") MultipartFile file) throws IOException {
        return service.uploadPhoto(requestId, file);
    }

    // Get photos for maintenance request
    @GetMapping("/requests/{requestId}/photos")
    public List<MaintenancePhoto> getPhotos(@PathVariable UUID requestId) {
        return service.getPhotos(requestId);
    }

    // Delete photo
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(Authentication auth,
                                            @PathVariable UUID photoId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        service.deletePhoto(ownerId, photoId);
        return ResponseEntity.noContent().build();
    }
}
