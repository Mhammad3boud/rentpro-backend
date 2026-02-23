package com.rentpro.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        
        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword
) {}
