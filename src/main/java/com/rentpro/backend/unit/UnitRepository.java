package com.rentpro.backend.unit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findAllByProperty_Id(Long propertyId);
}
