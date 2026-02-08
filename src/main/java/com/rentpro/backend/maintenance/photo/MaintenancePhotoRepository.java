package com.rentpro.backend.maintenance.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenancePhotoRepository extends JpaRepository<MaintenancePhoto, Long> {

    List<MaintenancePhoto> findAllByRequest_Id(Long requestId);
}
