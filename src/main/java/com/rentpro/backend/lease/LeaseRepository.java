package com.rentpro.backend.lease;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    List<Lease> findByProperty_Owner_UserId(UUID ownerId);

    List<Lease> findByTenant_User_UserId(UUID tenantUserId);

    List<Lease> findByTenant_TenantId(UUID tenantId);

    // Find active lease for a property/unit combination
    @Query("""
        select l from Lease l
        where l.property.propertyId = :propertyId
          and ((:unitId is null and l.unit is null) or l.unit.unitId = :unitId)
          and l.leaseStatus = com.rentpro.backend.lease.LeaseStatus.ACTIVE
    """)
    java.util.Optional<Lease> findActiveLeaseByPropertyAndUnit(
            @Param("propertyId") UUID propertyId,
            @Param("unitId") UUID unitId);

    // Alternative: find by property+unit with any status for termination
    java.util.Optional<Lease> findFirstByProperty_PropertyIdAndUnit_UnitIdAndLeaseStatus(
            UUID propertyId, UUID unitId, LeaseStatus status);

    java.util.Optional<Lease> findFirstByProperty_PropertyIdAndUnitIsNullAndLeaseStatus(
            UUID propertyId, LeaseStatus status);

    @Query("""
        select count(l)
        from Lease l
        where l.property.owner.userId = :ownerId
          and l.leaseStatus = com.rentpro.backend.lease.LeaseStatus.ACTIVE
          and l.startDate <= :currentDate
          and (l.endDate is null or l.endDate >= :currentDate)
    """)
    long countActiveLeasesByOwner(@Param("ownerId") UUID ownerId, @Param("currentDate") LocalDate currentDate);
}
