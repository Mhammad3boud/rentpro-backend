package com.rentpro.backend.lease;

import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepo;
    private final UnitRepository unitRepo;
    private final UserRepository userRepo;

    public LeaseService(LeaseRepository leaseRepo, UnitRepository unitRepo, UserRepository userRepo) {
        this.leaseRepo = leaseRepo;
        this.unitRepo = unitRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Lease createLease(Long ownerId, Long unitId, Long tenantId,
            LocalDate startDate, LocalDate endDate,
            java.math.BigDecimal rentAmount, java.math.BigDecimal depositAmount, String notes) {

        Unit unit = unitRepo.findByIdAndProperty_Owner_Id(unitId, ownerId)
                .orElseThrow(() -> new RuntimeException("Unit not found or not yours"));

        User tenant = userRepo.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        leaseRepo.findFirstByUnit_IdAndEndDateIsNull(unitId)
                .ifPresent(l -> {
                    throw new RuntimeException("Unit already has an active lease");
                });

        if (endDate != null && endDate.isBefore(startDate)) {
            throw new RuntimeException("endDate must be >= startDate");
        }

        Lease lease = Lease.builder()
                .unit(unit)
                .tenant(tenant)
                .startDate(startDate)
                .endDate(endDate)
                .rentAmount(rentAmount)
                .depositAmount(depositAmount)
                .notes(notes)
                .createdAt(Instant.now())
                .build();

        return leaseRepo.save(lease);
    }

    @Transactional
    public Lease endLease(Long ownerId, Long leaseId, LocalDate endDate) {

        Lease lease = leaseRepo.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        // ownership check: this lease's unit must belong to this owner
        unitRepo.findByIdAndProperty_Owner_Id(lease.getUnit().getId(), ownerId)
                .orElseThrow(() -> new RuntimeException("Lease not found or not yours"));

        if (lease.getEndDate() != null) {
            throw new RuntimeException("Lease already ended");
        }
        if (endDate.isBefore(lease.getStartDate())) {
            throw new RuntimeException("endDate must be >= startDate");
        }

        lease.setEndDate(endDate);
        return leaseRepo.save(lease);
    }

    @Transactional(readOnly = true)
    public Lease getActiveLease(Long ownerId, Long unitId) {

        unitRepo.findByIdAndProperty_Owner_Id(unitId, ownerId)
                .orElseThrow(() -> new RuntimeException("Unit not found or not yours"));

        return leaseRepo.findFirstByUnit_IdAndEndDateIsNull(unitId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Lease> getUnitHistory(Long ownerId, Long unitId) {

        unitRepo.findByIdAndProperty_Owner_Id(unitId, ownerId)
                .orElseThrow(() -> new RuntimeException("Unit not found or not yours"));

        return leaseRepo.findAllByUnit_IdOrderByStartDateDesc(unitId);
    }

}
