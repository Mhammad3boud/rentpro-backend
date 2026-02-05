package com.rentpro.backend.user.dto;

import jakarta.validation.constraints.*;

public record CreateTenantRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        @NotBlank @Size(min = 6) String password
) {}
