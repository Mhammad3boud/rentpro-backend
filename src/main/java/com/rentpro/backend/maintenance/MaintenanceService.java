package com.rentpro.backend.maintenance;

import com.rentpro.backend.maintenance.dto.CreateMaintenanceRequest;
import com.rentpro.backend.maintenance.dto.UpdateMaintenanceStatusRequest;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenanceRepository repo;
    private final UnitRepository unitRepo;
    private final UserRepository userRepo;

    public MaintenanceService(MaintenanceRepository repo, UnitRepository unitRepo, UserRepository userRepo) {
        this.repo = repo;
        this.unitRepo = unitRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public MaintenanceRequest createAsTenant(Long tenantId, CreateMaintenanceRequest req) {

        User tenant = userRepo.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Unit unit = unitRepo.findById(req.unitId())
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        String priority = (req.priority() == null || req.priority().isBlank()) ? "MEDIUM" : req.priority().toUpperCase();
        if (!priority.equals("LOW") && !priority.equals("MEDIUM") && !priority.equals("HIGH")) {
            throw new RuntimeException("Invalid priority");
        }

        MaintenanceRequest mr = MaintenanceRequest.builder()
                .unit(unit)
                .tenant(tenant)
                .title(req.title())
                .description(req.description())
                .priority(priority)
                .status("OPEN")
                .createdAt(Instant.now())
                .resolvedAt(null)
                .ownerNotes(null)
                .build();

        return repo.save(mr);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRequest> listMineAsTenant(Long tenantId) {
        return repo.findAllByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRequest> listAllForOwner(Long ownerId) {
        // owner-safe by query (unit->property->owner)
        return repo.findAllByUnit_Property_Owner_IdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional
    public MaintenanceRequest updateStatusAsOwner(Long ownerId, Long requestId, UpdateMaintenanceStatusRequest req) {

        MaintenanceRequest mr = repo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found"));

        // Owner check using unit->property->owner
        Long unitId = mr.getUnit().getId();
        unitRepo.findByIdAndProperty_Owner_Id(unitId, ownerId)
                .orElseThrow(() -> new RuntimeException("Request not found or not yours"));

        String status = req.status().toUpperCase();
        if (!status.equals("OPEN") && !status.equals("IN_PROGRESS") && !status.equals("RESOLVED")) {
            throw new RuntimeException("Invalid status");
        }

        mr.setStatus(status);
        if (req.ownerNotes() != null) mr.setOwnerNotes(req.ownerNotes());

        if (status.equals("RESOLVED")) mr.setResolvedAt(Instant.now());

        return repo.save(mr);
    }
}
