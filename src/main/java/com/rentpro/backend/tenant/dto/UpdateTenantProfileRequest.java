package com.rentpro.backend.tenant.dto;

public record UpdateTenantProfileRequest(
        String phone,
        String emergencyName,
        String emergencyPhone,
        String address
) {}
