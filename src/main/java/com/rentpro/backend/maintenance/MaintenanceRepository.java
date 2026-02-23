package com.rentpro.backend.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, UUID> {

    List<MaintenanceRequest> findByTenant_TenantId(UUID tenantId);

    List<MaintenanceRequest> findByProperty_Owner_UserId(UUID ownerId);

    long countByTenant_User_UserId(UUID tenantUserId);

    @Query("""
        select count(m)
        from MaintenanceRequest m
        where m.property.owner.userId = :ownerId
          and m.status = :status
    """)
    long countByOwnerAndStatus(@Param("ownerId") UUID ownerId,
                               @Param("status") MaintenanceStatus status);
}
