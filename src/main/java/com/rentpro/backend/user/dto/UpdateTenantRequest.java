package com.rentpro.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRequest(
        @NotBlank String fullName,
        String phone
) {}
