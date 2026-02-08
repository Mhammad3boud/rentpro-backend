package com.rentpro.backend.maintenance.dto;

import com.rentpro.backend.maintenance.MaintenanceRequest;

import java.time.Instant;

public record MaintenanceResponse(
        Long id,
        Long unitId,
        Long tenantId,
        String title,
        String description,
        String priority,
        String status,
        String ownerNotes,
        Instant createdAt,
        Instant resolvedAt
) {
    public static MaintenanceResponse from(MaintenanceRequest mr) {
        return new MaintenanceResponse(
                mr.getId(),
                mr.getUnit().getId(),
                mr.getTenant().getId(),
                mr.getTitle(),
                mr.getDescription(),
                mr.getPriority(),
                mr.getStatus(),
                mr.getOwnerNotes(),
                mr.getCreatedAt(),
                mr.getResolvedAt()
        );
    }
}
