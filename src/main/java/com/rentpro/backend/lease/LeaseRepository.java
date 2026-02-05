package com.rentpro.backend.lease;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    // Current (active) lease for a unit = endDate is NULL
    Optional<Lease> findFirstByUnit_IdAndEndDateIsNull(Long unitId);

    // Full history for a unit (latest first)
    List<Lease> findAllByUnit_IdOrderByStartDateDesc(Long unitId);

    // Full history for a tenant (latest first)
    List<Lease> findAllByTenant_IdOrderByStartDateDesc(Long tenantId);
}
