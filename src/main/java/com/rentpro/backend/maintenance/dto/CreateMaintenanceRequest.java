package com.rentpro.backend.maintenance.dto;

import com.rentpro.backend.maintenance.MaintenancePriority;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateMaintenanceRequest(
        UUID leaseId,
        UUID propertyId,
        UUID unitId,
        String title,
        String description,
        MaintenancePriority priority,
        String imageUrl,
        String assignedTechnician,
        BigDecimal maintenanceCost
) {}
