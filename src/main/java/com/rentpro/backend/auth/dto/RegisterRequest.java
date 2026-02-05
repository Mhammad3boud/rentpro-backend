package com.rentpro.backend.auth.dto;

// import com.rentpro.backend.user.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        @NotBlank @Size(min = 6) String password
        // @NotNull Role role
) {}
