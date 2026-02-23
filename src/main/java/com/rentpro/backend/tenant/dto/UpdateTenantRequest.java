package com.rentpro.backend.tenant.dto;

/**
 * Request DTO for owner to update a tenant's details
 */
public record UpdateTenantRequest(
        String fullName,
        String phone,
        String emergencyContact,
        String address
) {}
