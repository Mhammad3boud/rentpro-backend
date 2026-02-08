package com.rentpro.backend.maintenance.photo.dto;

import com.rentpro.backend.maintenance.photo.MaintenancePhoto;

import java.time.Instant;

public record MaintenancePhotoResponse(
        Long id,
        Long requestId,
        String fileName,
        String contentType,
        Instant uploadedAt,
        String url
) {
    public static MaintenancePhotoResponse from(MaintenancePhoto p) {
        return new MaintenancePhotoResponse(
                p.getId(),
                p.getRequest().getId(),
                p.getFileName(),
                p.getContentType(),
                p.getUploadedAt(),
                "/maintenance/photos/" + p.getFileName()
        );
    }
}
