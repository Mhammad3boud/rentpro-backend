package com.rentpro.backend.unit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnitPhotoRepository extends JpaRepository<UnitPhoto, UUID> {
    List<UnitPhoto> findByUnit_UnitId(UUID unitId);
}
