package com.rentpro.backend.lease;

import com.rentpro.backend.activity.ActivityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentpro.backend.notification.NotificationService;
import com.rentpro.backend.notification.NotificationType;
import com.rentpro.backend.property.Property;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.property.PropertyType;
import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.tenant.TenantRepository;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.lease.dto.AssignTenantRequest;
import com.rentpro.backend.lease.dto.CheckInLeaseRequest;
import com.rentpro.backend.lease.dto.CheckOutLeaseRequest;
import com.rentpro.backend.lease.dto.CreateLeaseRequest;
import com.rentpro.backend.lease.dto.TerminateLeaseRequest;
import com.rentpro.backend.lease.dto.UpdateLeaseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LeaseService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantRepository tenantRepository;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    public LeaseService(LeaseRepository leaseRepository,
                        PropertyRepository propertyRepository,
                        UnitRepository unitRepository,
                        TenantRepository tenantRepository,
                        ActivityService activityService,
                        NotificationService notificationService) {
        this.leaseRepository = leaseRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.tenantRepository = tenantRepository;
        this.activityService = activityService;
        this.notificationService = notificationService;
    }

    public Lease createLease(UUID ownerId, CreateLeaseRequest request) {

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Owner must own this property
        if (!property.getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: property does not belong to owner");
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Tenant must belong to the same owner
        if (!tenant.getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: tenant does not belong to owner");
        }

        Unit unit = null;

        // Validate unit based on property type
        if (property.getPropertyType() == PropertyType.MULTI_UNIT) {
            if (request.unitId() == null) {
                throw new RuntimeException("Unit is required for multi-unit property");
            }
            unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));

            if (!unit.getProperty().getPropertyId().equals(property.getPropertyId())) {
                throw new RuntimeException("Unit does not belong to selected property");
            }
        } else {
            // Standalone property: unit must be null
            unit = null;
        }

        Lease lease = new Lease();
        lease.setProperty(property);
        lease.setUnit(unit);
        lease.setTenant(tenant);
        lease.setMonthlyRent(request.monthlyRent());
        lease.setStartDate(request.startDate());
        lease.setEndDate(request.endDate());
        lease.setLeaseStatus(LeaseStatus.ACTIVE);

        // Auto-generate lease name:
        // Standalone: "Property Name Lease"
        // Multi-unit: "Property Name - Unit X"
        if (unit == null) {
            lease.setLeaseName(property.getPropertyName() + " Lease");
        } else {
            lease.setLeaseName(property.getPropertyName() + " - Unit " + unit.getUnitNumber());
        }

        Lease saved = leaseRepository.save(lease);
        
        // Log activity
        String unitNumber = unit != null ? unit.getUnitNumber() : null;
        activityService.logLeaseCreated(ownerId, tenant.getFullName(), property.getPropertyName(), unitNumber);
        UUID tenantUserId = tenant.getUser() != null ? tenant.getUser().getUserId() : null;
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logLeaseCreated(tenantUserId, tenant.getFullName(), property.getPropertyName(), unitNumber);
        }
        
        return saved;
    }

    public List<Lease> getOwnerLeases(UUID ownerId) {
        return leaseRepository.findByProperty_Owner_UserId(ownerId);
    }

    public List<Lease> getTenantLeases(UUID tenantUserId) {
        return leaseRepository.findByTenant_User_UserId(tenantUserId);
    }

    public Lease getLeaseById(UUID leaseId) {
        return leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));
    }

    @Transactional
    public Lease assignTenant(UUID ownerId, AssignTenantRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Owner must own this property
        if (!property.getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: property does not belong to owner");
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Tenant must belong to the same owner
        if (!tenant.getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: tenant does not belong to owner");
        }

        Unit unit = null;

        // Validate unit based on property type
        if (property.getPropertyType() == PropertyType.MULTI_UNIT) {
            if (request.unitId() == null) {
                throw new RuntimeException("Unit is required for multi-unit property");
            }
            unit = unitRepository.findById(request.unitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));

            if (!unit.getProperty().getPropertyId().equals(property.getPropertyId())) {
                throw new RuntimeException("Unit does not belong to selected property");
            }
        }

        // Check for existing active lease on this property/unit and terminate it
        java.util.Optional<Lease> existingLeaseOpt;
        if (unit != null) {
            existingLeaseOpt = leaseRepository.findFirstByProperty_PropertyIdAndUnit_UnitIdAndLeaseStatus(
                    request.propertyId(), request.unitId(), LeaseStatus.ACTIVE);
        } else {
            existingLeaseOpt = leaseRepository.findFirstByProperty_PropertyIdAndUnitIsNullAndLeaseStatus(
                    request.propertyId(), LeaseStatus.ACTIVE);
        }
        
        existingLeaseOpt.ifPresent(existingLease -> {
            // Terminate the existing lease
            applyTermination(existingLease, "Reassigned to another tenant", null, LocalDate.now());
            leaseRepository.save(existingLease);
        });

        Lease lease = new Lease();
        lease.setProperty(property);
        lease.setUnit(unit);
        lease.setTenant(tenant);
        lease.setMonthlyRent(request.monthlyRent());
        lease.setStartDate(request.startDate());
        lease.setEndDate(request.endDate());
        lease.setLeaseStatus(LeaseStatus.ACTIVE);

        // Auto-generate lease name
        if (unit == null) {
            lease.setLeaseName(property.getPropertyName() + " Lease");
        } else {
            lease.setLeaseName(property.getPropertyName() + " - Unit " + unit.getUnitNumber());
        }

        Lease saved = leaseRepository.save(lease);
        
        // Log activity
        String unitNumber = unit != null ? unit.getUnitNumber() : null;
        activityService.logTenantAssigned(ownerId, tenant.getFullName(), property.getPropertyName(), unitNumber);
        UUID tenantUserId = tenant.getUser() != null ? tenant.getUser().getUserId() : null;
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logTenantAssigned(tenantUserId, tenant.getFullName(), property.getPropertyName(), unitNumber);
        }
        
        return saved;
    }

    // Update existing lease
    @Transactional
    public Lease updateLease(UUID leaseId, UpdateLeaseRequest request) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        if (request.monthlyRent() != null) {
            lease.setMonthlyRent(request.monthlyRent());
        }
        if (request.securityDeposit() != null) {
            lease.setSecurityDeposit(request.securityDeposit());
        }
        if (request.startDate() != null) {
            lease.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            lease.setEndDate(request.endDate());
        }

        Lease saved = leaseRepository.save(lease);
        
        // Log activity - build changes description
        StringBuilder changes = new StringBuilder();
        if (request.monthlyRent() != null) {
            changes.append("Monthly rent updated to TZS ").append(request.monthlyRent()).append(". ");
        }
        if (request.securityDeposit() != null) {
            changes.append("Security deposit updated to TZS ").append(request.securityDeposit()).append(". ");
        }
        if (changes.length() > 0) {
            String propertyName = lease.getProperty().getPropertyName();
            String unitNumber = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
            UUID ownerId = lease.getProperty().getOwner().getUserId();
            activityService.logLeaseUpdated(ownerId, propertyName, unitNumber, changes.toString().trim());
            UUID tenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                    ? lease.getTenant().getUser().getUserId()
                    : null;
            if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
                activityService.logLeaseUpdated(tenantUserId, propertyName, unitNumber, changes.toString().trim());
            }
        }
        
        return saved;
    }

    // Get lease by tenant ID
    public Lease getLeaseByTenantId(UUID tenantId) {
        List<Lease> leases = leaseRepository.findByTenant_TenantId(tenantId);
        // Return the active lease, or the most recent one if no active lease
        return leases.stream()
                .filter(l -> l.getLeaseStatus() == LeaseStatus.ACTIVE)
                .findFirst()
                .orElse(leases.isEmpty() ? null : leases.get(0));
    }

    // Terminate a lease
    @Transactional
    public Lease terminateLease(UUID ownerId, String role, UUID leaseId, TerminateLeaseRequest request) {
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only owners can terminate leases");
        }

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        String reason = request != null ? request.reason() : null;
        String notes = request != null ? request.notes() : null;
        LocalDate terminationDate = request != null && request.terminationDate() != null
                ? request.terminationDate()
                : LocalDate.now();

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Termination reason is required");
        }

        applyTermination(lease, reason, notes, terminationDate);
        Lease saved = leaseRepository.save(lease);

        String propertyName = lease.getProperty().getPropertyName();
        String unitNumber = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
        String location = unitNumber != null
                ? String.format("%s Unit %s", propertyName, unitNumber)
                : propertyName;
        String desc = String.format("Lease terminated for %s. Reason: %s", location, reason.trim());
        activityService.logActivity(ownerId, com.rentpro.backend.activity.ActivityType.LEASE_TERMINATED, "Lease Terminated", desc);
        notificationService.createNotification(
                ownerId,
                NotificationType.LEASE_EXPIRY,
                "Lease terminated",
                desc,
                "LEASE",
                saved.getLeaseId()
        );
        UUID tenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logActivity(tenantUserId, com.rentpro.backend.activity.ActivityType.LEASE_TERMINATED, "Lease Terminated", desc);
            notificationService.createNotification(
                    tenantUserId,
                    NotificationType.LEASE_EXPIRY,
                    "Lease terminated",
                    desc,
                    "LEASE",
                    saved.getLeaseId()
            );
        }

        return saved;
    }

    @Transactional
    public Lease checkInLease(UUID ownerId, String role, UUID leaseId, CheckInLeaseRequest request) {
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only owners can check in leases");
        }

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }
        if (lease.getLeaseStatus() != LeaseStatus.ACTIVE) {
            throw new RuntimeException("Only active leases can be checked in");
        }
        if (lease.getCheckedInAt() != null) {
            throw new RuntimeException("Lease is already checked in");
        }

        LocalDate checkInDate = request != null && request.checkInDate() != null
                ? request.checkInDate()
                : LocalDate.now();

        String notes = request != null ? request.notes() : null;
        String cleanNotes = notes != null && !notes.trim().isEmpty() ? notes.trim() : null;

        lease.setCheckInDate(checkInDate);
        lease.setCheckedInAt(LocalDateTime.now());
        lease.setCheckInNotes(cleanNotes);
        lease.setCheckInChecklistJson(toChecklistJson(request != null ? request.checklist() : null));

        Lease saved = leaseRepository.save(lease);
        try {
            logCheckInOutActivity(saved, ownerId, "Check-In Completed",
                    "Tenant check-in confirmed", NotificationType.MAINTENANCE_UPDATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saved;
    }

    @Transactional
    public Lease checkOutLease(UUID ownerId, String role, UUID leaseId, CheckOutLeaseRequest request) {
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only owners can check out leases");
        }
        if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
            throw new RuntimeException("Check-out reason is required");
        }

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }
        if (lease.getLeaseStatus() != LeaseStatus.ACTIVE) {
            throw new RuntimeException("Only active leases can be checked out");
        }

        LocalDate checkOutDate = request.checkOutDate() != null
                ? request.checkOutDate()
                : LocalDate.now();
        String reason = request.reason().trim();
        String notes = request.notes() != null && !request.notes().trim().isEmpty()
                ? request.notes().trim()
                : null;

        applyTermination(lease, reason, notes, checkOutDate);
        lease.setCheckOutDate(checkOutDate);
        lease.setCheckedOutAt(LocalDateTime.now());
        lease.setCheckOutReason(reason);
        lease.setCheckOutNotes(notes);
        lease.setCheckOutChecklistJson(toChecklistJson(request.checklist()));

        Lease saved = leaseRepository.save(lease);
        try {
            logCheckInOutActivity(saved, ownerId, "Check-Out Completed",
                    "Tenant check-out confirmed", NotificationType.LEASE_EXPIRY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saved;
    }

    // Delete a lease (permanently)
    @Transactional
    public void deleteLease(UUID leaseId) {
        if (!leaseRepository.existsById(leaseId)) {
            throw new RuntimeException("Lease not found");
        }
        leaseRepository.deleteById(leaseId);
    }

    private void applyTermination(Lease lease, String reason, String notes, LocalDate terminationDate) {
        lease.setLeaseStatus(LeaseStatus.TERMINATED);
        lease.setTerminationReason(reason != null ? reason.trim() : null);
        lease.setTerminationNotes(notes != null && !notes.trim().isEmpty() ? notes.trim() : null);
        lease.setTerminationDate(terminationDate);
        lease.setTerminatedAt(LocalDateTime.now());

        if (lease.getEndDate() == null || lease.getEndDate().isAfter(terminationDate)) {
            lease.setEndDate(terminationDate);
        }
    }

    private void logCheckInOutActivity(Lease lease, UUID ownerId, String title, String action, NotificationType notificationType) {
        String propertyName = lease.getProperty().getPropertyName();
        String unitNumber = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
        String location = unitNumber != null
                ? String.format("%s Unit %s", propertyName, unitNumber)
                : propertyName;
        String desc = String.format("%s for %s", action, location);

        activityService.logActivity(ownerId, com.rentpro.backend.activity.ActivityType.LEASE_UPDATED, title, desc);
        notificationService.createNotification(
                ownerId,
                notificationType,
                title,
                desc,
                "LEASE",
                lease.getLeaseId()
        );

        UUID tenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logActivity(tenantUserId, com.rentpro.backend.activity.ActivityType.LEASE_UPDATED, title, desc);
            notificationService.createNotification(
                    tenantUserId,
                    notificationType,
                    title,
                    desc,
                    "LEASE",
                    lease.getLeaseId()
            );
        }
    }

    private String toChecklistJson(java.util.List<LeaseChecklistItem> checklist) {
        if (checklist == null || checklist.isEmpty()) {
            return null;
        }
        java.util.List<LeaseChecklistItem> cleaned = checklist.stream()
                .filter(item -> item != null && item.getItem() != null && !item.getItem().trim().isEmpty())
                .map(item -> {
                    LeaseChecklistItem x = new LeaseChecklistItem();
                    x.setItem(item.getItem().trim());
                    x.setCondition(item.getCondition() != null ? item.getCondition().trim() : null);
                    x.setChecked(Boolean.TRUE.equals(item.getChecked()));
                    x.setNotes(item.getNotes() != null && !item.getNotes().trim().isEmpty() ? item.getNotes().trim() : null);
                    return x;
                })
                .toList();

        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(cleaned);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
