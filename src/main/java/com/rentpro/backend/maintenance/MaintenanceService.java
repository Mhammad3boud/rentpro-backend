package com.rentpro.backend.maintenance;

import com.rentpro.backend.activity.ActivityService;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.maintenance.dto.CreateMaintenanceRequest;
import com.rentpro.backend.maintenance.dto.UpdateMaintenanceRequest;
import com.rentpro.backend.maintenance.dto.UpdateMaintenanceStatusRequest;
import com.rentpro.backend.notification.NotificationService;
import com.rentpro.backend.notification.NotificationType;
import com.rentpro.backend.property.Property;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.storage.StorageService;
import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.tenant.TenantRepository;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.lease.LeaseStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final NotificationService notificationService;
    private final StorageService storageService;

    public MaintenanceService(MaintenanceRepository repository,
                              MaintenancePhotoRepository photoRepository,
                              TenantRepository tenantRepository,
                              PropertyRepository propertyRepository,
                              UnitRepository unitRepository,
                              LeaseRepository leaseRepository,
                              ActivityService activityService,
                              NotificationService notificationService,
                              StorageService storageService) {
        this.repository = repository;
        this.photoRepository = photoRepository;
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.leaseRepository = leaseRepository;
        this.activityService = activityService;
        this.notificationService = notificationService;
        this.storageService = storageService;
    }

    public MaintenanceRequest createRequest(UUID currentUserId, String role, CreateMaintenanceRequest request) {
        if ("OWNER".equalsIgnoreCase(role)) {
            return createRequestAsOwner(currentUserId, request);
        }
        return createRequestAsTenant(currentUserId, request);
    }

    // Tenant creates request
    private MaintenanceRequest createRequestAsTenant(UUID tenantUserId, CreateMaintenanceRequest request) {
        if (request.leaseId() == null) {
            throw new RuntimeException("Lease is required for tenant maintenance requests");
        }

        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        Tenant tenant = tenantRepository.findByUser_UserId(tenantUserId)
                .orElseThrow(() -> new RuntimeException("Tenant not found for current user"));

        // Security check: tenant can only create requests for their own lease
        if (!lease.getTenant().getTenantId().equals(tenant.getTenantId())) {
            throw new RuntimeException("Unauthorized");
        }
        if (lease.getLeaseStatus() != LeaseStatus.ACTIVE) {
            throw new RuntimeException("Cannot create maintenance request for non-active lease");
        }

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
        UUID activityTenantUserId = tenant.getUser() != null ? tenant.getUser().getUserId() : null;
        String unitNumber = maintenance.getUnit() != null ? maintenance.getUnit().getUnitNumber() : null;
        activityService.logMaintenanceCreated(ownerId, property.getPropertyName(), unitNumber, request.title());
        if (activityTenantUserId != null && !activityTenantUserId.equals(ownerId)) {
            activityService.logMaintenanceCreated(activityTenantUserId, property.getPropertyName(), unitNumber, request.title());
        }

        // Notify owner when tenant raises a new maintenance request.
        String location = unitNumber != null
                ? property.getPropertyName() + " - Unit " + unitNumber
                : property.getPropertyName();
        String tenantName = tenant.getFullName() != null ? tenant.getFullName() : "Tenant";
        String title = "New maintenance request";
        String message = tenantName + " reported \"" + request.title() + "\" at " + location + ".";
        notificationService.createNotification(
                ownerId,
                NotificationType.MAINTENANCE_UPDATE,
                title,
                message,
                "MAINTENANCE_REQUEST",
                saved.getRequestId()
        );
        
        return saved;
    }

    // Owner can create request even without a lease/tenant.
    private MaintenanceRequest createRequestAsOwner(UUID ownerId, CreateMaintenanceRequest request) {
        Lease lease = null;
        if (request.leaseId() != null) {
            lease = leaseRepository.findById(request.leaseId())
                    .orElseThrow(() -> new RuntimeException("Lease not found"));
            if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized");
            }
        }

        Property property;
        if (request.propertyId() != null) {
            property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new RuntimeException("Property not found"));
            if (!property.getOwner().getUserId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized");
            }
            if (lease != null && !lease.getProperty().getPropertyId().equals(property.getPropertyId())) {
                throw new RuntimeException("Lease does not belong to selected property");
            }
        } else if (lease != null) {
            property = lease.getProperty();
        } else {
            throw new RuntimeException("Property is required");
        }

        Unit unit = null;
        if (request.unitId() != null) {
            unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            if (!unit.getProperty().getPropertyId().equals(property.getPropertyId())) {
                throw new RuntimeException("Unit does not belong to selected property");
            }
        } else if (lease != null && lease.getUnit() != null) {
            unit = lease.getUnit();
        }

        Tenant tenant = lease != null ? lease.getTenant() : null;

        MaintenanceRequest maintenance = new MaintenanceRequest();
        maintenance.setTenant(tenant);
        maintenance.setLease(lease);
        maintenance.setProperty(property);
        maintenance.setUnit(unit);
        maintenance.setTitle(request.title());
        maintenance.setDescription(request.description());
        maintenance.setPriority(request.priority() != null ? request.priority() : MaintenancePriority.MEDIUM);
        maintenance.setImageUrl(request.imageUrl());
        maintenance.setAssignedTechnician(request.assignedTechnician());
        maintenance.setMaintenanceCost(request.maintenanceCost());

        MaintenanceRequest saved = repository.save(maintenance);

        String unitNumber = unit != null ? unit.getUnitNumber() : null;
        activityService.logMaintenanceCreated(ownerId, property.getPropertyName(), unitNumber, request.title());

        UUID tenantUserId = tenant != null && tenant.getUser() != null ? tenant.getUser().getUserId() : null;
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            String tenantTitle = "Maintenance request created";
            String tenantMessage = "A maintenance request \"" + request.title() + "\" was created for your property.";
            notificationService.createNotification(
                    tenantUserId,
                    NotificationType.MAINTENANCE_UPDATE,
                    tenantTitle,
                    tenantMessage,
                    "MAINTENANCE_REQUEST",
                    saved.getRequestId()
            );
        }

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
        UUID tenantUserId = maintenance.getTenant() != null && maintenance.getTenant().getUser() != null
                ? maintenance.getTenant().getUser().getUserId()
                : null;
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logMaintenanceStatusChanged(tenantUserId, maintenance.getTitle(),
                    oldStatus.name(), request.status().name());

            String tenantTitle = "Maintenance status updated";
            String tenantMessage = "\"" + maintenance.getTitle() + "\" is now "
                    + request.status().name().replace('_', ' ') + ".";
            notificationService.createNotification(
                    tenantUserId,
                    NotificationType.MAINTENANCE_UPDATE,
                    tenantTitle,
                    tenantMessage,
                    "MAINTENANCE_REQUEST",
                    saved.getRequestId()
            );
        }
        
        return saved;
    }

    public MaintenanceRequest updateRequest(UUID ownerId,
                                            UUID requestId,
                                            UpdateMaintenanceRequest request) {
        MaintenanceRequest maintenance = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!maintenance.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        Lease lease = null;
        if (request.leaseId() != null) {
            lease = leaseRepository.findById(request.leaseId())
                    .orElseThrow(() -> new RuntimeException("Lease not found"));
            if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized");
            }
        }

        Property property = maintenance.getProperty();
        if (request.propertyId() != null) {
            property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new RuntimeException("Property not found"));
            if (!property.getOwner().getUserId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized");
            }
        } else if (lease != null) {
            property = lease.getProperty();
        }

        if (lease != null && !lease.getProperty().getPropertyId().equals(property.getPropertyId())) {
            throw new RuntimeException("Lease does not belong to selected property");
        }

        Unit unit = null;
        if (request.unitId() != null) {
            unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            if (!unit.getProperty().getPropertyId().equals(property.getPropertyId())) {
                throw new RuntimeException("Unit does not belong to selected property");
            }
        } else if (lease != null && lease.getUnit() != null) {
            unit = lease.getUnit();
        }

        if (request.title() != null) maintenance.setTitle(request.title());
        if (request.description() != null) maintenance.setDescription(request.description());
        if (request.priority() != null) maintenance.setPriority(request.priority());
        maintenance.setAssignedTechnician(request.assignedTechnician());
        maintenance.setMaintenanceCost(request.maintenanceCost());
        maintenance.setLease(lease);
        maintenance.setProperty(property);
        maintenance.setUnit(unit);
        maintenance.setTenant(lease != null ? lease.getTenant() : null);

        if (request.status() != null) {
            maintenance.setStatus(request.status());
            if (request.status() == MaintenanceStatus.RESOLVED) {
                maintenance.setResolvedAt(LocalDateTime.now());
            } else {
                maintenance.setResolvedAt(null);
            }
        }

        return repository.save(maintenance);
    }

    public List<MaintenanceRequest> getOwnerRequests(UUID ownerId) {
        return repository.findByProperty_Owner_UserId(ownerId);
    }

    public List<MaintenanceRequest> getTenantRequests(UUID tenantUserId) {
        return repository.findByTenant_User_UserId(tenantUserId);
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

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = requestId + "_" + UUID.randomUUID() + extension;

        // Save photo record
        MaintenancePhoto photo = new MaintenancePhoto();
        photo.setMaintenanceRequest(maintenance);
        photo.setImageUrl(storageService.uploadMaintenancePhoto(filename, file));

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

        storageService.deleteByUrl(photo.getImageUrl());

        photoRepository.delete(photo);
    }
}
