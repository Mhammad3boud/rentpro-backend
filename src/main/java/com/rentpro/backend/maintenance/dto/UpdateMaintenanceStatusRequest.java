package com.rentpro.backend.maintenance.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMaintenanceStatusRequest(
        @NotBlank String status,   // OPEN/IN_PROGRESS/RESOLVED
        String ownerNotes
) {}
