package com.rentpro.backend.tenant.dto;

public record UpdateTenantProfileRequest(
        String phone,
        String emergencyContact,
        String address
) {}
