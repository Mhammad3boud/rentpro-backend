package com.rentpro.backend.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenancePhotoRepository extends JpaRepository<MaintenancePhoto, UUID> {

    List<MaintenancePhoto> findByMaintenanceRequest_RequestId(UUID requestId);
}
