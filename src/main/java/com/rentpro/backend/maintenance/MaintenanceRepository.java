package com.rentpro.backend.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long> {

    // Tenant view (my tickets)
    List<MaintenanceRequest> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    // Owner view (tickets for my property via unit->property->owner)
    List<MaintenanceRequest> findAllByUnit_Property_IdOrderByCreatedAtDesc(Long propertyId);

    // Owner view (all tickets for all my properties)
    List<MaintenanceRequest> findAllByUnit_Property_Owner_IdOrderByCreatedAtDesc(Long ownerId);

    // Count methods for dashboard
    long countByUnit_Property_Owner_IdAndStatus(Long ownerId, String status);

    long countByUnit_Property_Owner_IdAndStatusAndPriority(Long ownerId, String status, String priority);

}
