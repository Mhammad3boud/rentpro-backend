package com.rentpro.backend.user.dto;

import java.time.Instant;

public record TenantResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Instant createdAt
) {}
