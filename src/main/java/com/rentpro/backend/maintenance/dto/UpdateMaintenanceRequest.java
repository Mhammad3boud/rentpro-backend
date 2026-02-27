package com.rentpro.backend.maintenance.dto;

import com.rentpro.backend.maintenance.MaintenancePriority;
import com.rentpro.backend.maintenance.MaintenanceStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateMaintenanceRequest(
        UUID leaseId,
        UUID propertyId,
        UUID unitId,
        String title,
        String description,
        MaintenancePriority priority,
        String assignedTechnician,
        BigDecimal maintenanceCost,
        MaintenanceStatus status
) {}
