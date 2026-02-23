package com.rentpro.backend.tenant.dto;

public record CreateTenantRequest(
        String fullName,
        String email,
        String password,
        String phone,
        String emergencyContact,
        String address
) {}
