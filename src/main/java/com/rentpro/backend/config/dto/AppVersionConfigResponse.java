package com.rentpro.backend.config.dto;

public record AppVersionConfigResponse(
        String platform,
        String latestVersion,
        String minSupportedVersion,
        String updateUrl,
        String message
) {}
