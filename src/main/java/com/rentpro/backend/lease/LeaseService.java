package com.rentpro.backend.lease;

import com.rentpro.backend.activity.ActivityService;
import com.rentpro.backend.property.Property;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.property.PropertyType;
import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.tenant.TenantRepository;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.lease.dto.AssignTenantRequest;
import com.rentpro.backend.lease.dto.CreateLeaseRequest;
import com.rentpro.backend.lease.dto.UpdateLeaseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantRepository tenantRepository;
    private final ActivityService activityService;

    public LeaseService(LeaseRepository leaseRepository,
                        PropertyRepository propertyRepository,
                        UnitRepository unitRepository,
                        TenantRepository tenantRepository,
                        ActivityService activityService) {
        this.leaseRepository = leaseRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.tenantRepository = tenantRepository;
        this.activityService = activityService;
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
            existingLease.setLeaseStatus(LeaseStatus.TERMINATED);
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
    public Lease terminateLease(UUID leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));
        lease.setLeaseStatus(LeaseStatus.TERMINATED);
        return leaseRepository.save(lease);
    }

    // Delete a lease (permanently)
    @Transactional
    public void deleteLease(UUID leaseId) {
        if (!leaseRepository.existsById(leaseId)) {
            throw new RuntimeException("Lease not found");
        }
        leaseRepository.deleteById(leaseId);
    }
}
