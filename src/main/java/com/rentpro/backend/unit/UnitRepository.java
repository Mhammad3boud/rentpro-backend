package com.rentpro.backend.unit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByProperty_PropertyId(UUID propertyId);

    Optional<Unit> findByProperty_PropertyIdAndUnitNumber(UUID propertyId, String unitNumber);
}
