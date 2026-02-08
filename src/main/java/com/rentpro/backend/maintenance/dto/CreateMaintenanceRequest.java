package com.rentpro.backend.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMaintenanceRequest(
        @NotNull Long unitId,
        @NotBlank String title,
        String description,
        String priority // LOW/MEDIUM/HIGH (optional)
) {}
