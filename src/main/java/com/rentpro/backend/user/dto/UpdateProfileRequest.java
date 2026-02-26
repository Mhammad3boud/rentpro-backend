package com.rentpro.backend.user.dto;

public record UpdateProfileRequest(
        String fullName,
        String phone,
        String address,
        Boolean notificationEmail,
        Boolean notificationPush,
        String themePreference
) {}
