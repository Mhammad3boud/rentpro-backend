package com.rentpro.backend.maintenance;

import com.rentpro.backend.activity.ActivityService;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.maintenance.dto.CreateMaintenanceRequest;
import com.rentpro.backend.maintenance.dto.UpdateMaintenanceStatusRequest;
import com.rentpro.backend.property.Property;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.tenant.TenantRepository;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final MaintenancePhotoRepository photoRepository;
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final LeaseRepository leaseRepository;
    private final ActivityService activityService;

    @Value("${app.upload.dir:uploads/maintenance}")
    private String uploadDir;

    public MaintenanceService(MaintenanceRepository repository,
                              MaintenancePhotoRepository photoRepository,
                              TenantRepository tenantRepository,
                              PropertyRepository propertyRepository,
                              UnitRepository unitRepository,
                              LeaseRepository leaseRepository,
                              ActivityService activityService) {
        this.repository = repository;
        this.photoRepository = photoRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.leaseRepository = leaseRepository;
        this.activityService = activityService;
    }

    // Tenant creates request
    public MaintenanceRequest createRequest(UUID userId, CreateMaintenanceRequest request) {

        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        // Try to find tenant by userId, or use the lease's tenant
        Tenant tenant = tenantRepository.findById(userId)
                .orElse(lease.getTenant());

        // Get property from request, or fall back to lease's property
        Property property;
        if (request.propertyId() != null) {
            property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new RuntimeException("Property not found"));
        } else {
            property = lease.getProperty();
        }

        MaintenanceRequest maintenance = new MaintenanceRequest();
        maintenance.setTenant(tenant);
        maintenance.setLease(lease);
        maintenance.setProperty(property);
        maintenance.setTitle(request.title());
        maintenance.setDescription(request.description());
        maintenance.setPriority(request.priority() != null ? request.priority() : MaintenancePriority.MEDIUM);
        maintenance.setImageUrl(request.imageUrl());
        maintenance.setAssignedTechnician(request.assignedTechnician());
        maintenance.setMaintenanceCost(request.maintenanceCost());

        if (request.unitId() != null) {
            Unit unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            maintenance.setUnit(unit);
        } else if (lease.getUnit() != null) {
            // Fall back to lease's unit if not provided
            maintenance.setUnit(lease.getUnit());
        }

        MaintenanceRequest saved = repository.save(maintenance);
        
        // Log activity
        UUID ownerId = property.getOwner().getUserId();
        String unitNumber = maintenance.getUnit() != null ? maintenance.getUnit().getUnitNumber() : null;
        activityService.logMaintenanceCreated(ownerId, property.getPropertyName(), unitNumber, request.title());
        
        return saved;
    }

    // Owner updates status
    public MaintenanceRequest updateStatus(UUID ownerId,
                                           UUID requestId,
                                           UpdateMaintenanceStatusRequest request) {

        MaintenanceRequest maintenance = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!maintenance.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        MaintenanceStatus oldStatus = maintenance.getStatus();
        maintenance.setStatus(request.status());
        maintenance.setAssignedTechnician(request.assignedTechnician());
        maintenance.setMaintenanceCost(request.maintenanceCost());

        if (request.status() == MaintenanceStatus.RESOLVED) {
            maintenance.setResolvedAt(LocalDateTime.now());
        }

        MaintenanceRequest saved = repository.save(maintenance);
        
        // Log activity
        activityService.logMaintenanceStatusChanged(ownerId, maintenance.getTitle(), 
                oldStatus.name(), request.status().name());
        
        return saved;
    }

    public List<MaintenanceRequest> getOwnerRequests(UUID ownerId) {
        return repository.findByProperty_Owner_UserId(ownerId);
    }

    public List<MaintenanceRequest> getTenantRequests(UUID tenantId) {
        return repository.findByTenant_TenantId(tenantId);
    }

    public MaintenanceRequest getRequestById(UUID requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found"));
    }

    // Delete maintenance request (owner only)
    public void deleteRequest(UUID ownerId, UUID requestId) {
        MaintenanceRequest maintenance = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!maintenance.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Delete associated photos first
        List<MaintenancePhoto> photos = photoRepository.findByMaintenanceRequest_RequestId(requestId);
        photoRepository.deleteAll(photos);

        repository.delete(maintenance);
    }

    // Upload photo for maintenance request
    public MaintenancePhoto uploadPhoto(UUID requestId, MultipartFile file) throws IOException {
        MaintenanceRequest maintenance = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = requestId + "_" + UUID.randomUUID() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Save photo record
        MaintenancePhoto photo = new MaintenancePhoto();
        photo.setMaintenanceRequest(maintenance);
        photo.setImageUrl("/uploads/maintenance/" + filename);

        return photoRepository.save(photo);
    }

    // Get photos for maintenance request
    public List<MaintenancePhoto> getPhotos(UUID requestId) {
        return photoRepository.findByMaintenanceRequest_RequestId(requestId);
    }

    // Delete photo
    public void deletePhoto(UUID ownerId, UUID photoId) {
        MaintenancePhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getMaintenanceRequest().getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Delete file from disk
        try {
            Path filePath = Paths.get(photo.getImageUrl().replace("/uploads/maintenance/", uploadDir + "/"));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail if file deletion fails
        }

        photoRepository.delete(photo);
    }
}
