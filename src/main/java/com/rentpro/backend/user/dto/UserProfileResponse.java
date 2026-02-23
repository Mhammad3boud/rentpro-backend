package com.rentpro.backend.user.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String role,
        String fullName,
        String phone,
        String address,
        String profilePicture,
        Boolean notificationEmail,
        Boolean notificationPush
) {}
