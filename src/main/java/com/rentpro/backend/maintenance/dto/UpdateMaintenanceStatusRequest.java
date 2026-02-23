package com.rentpro.backend.maintenance.dto;

import com.rentpro.backend.maintenance.MaintenanceStatus;
import java.math.BigDecimal;

public record UpdateMaintenanceStatusRequest(
        MaintenanceStatus status,
        String assignedTechnician,
        BigDecimal maintenanceCost
) {}
