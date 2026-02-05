package com.rentpro.backend.property.dto;

import java.time.Instant;
import java.util.Map;

public record PropertyResponse(
        Long id,
        String title,
        String address,
        Double latitude,
        Double longitude,
        String category,
        String notes,
        Map<String, Object> meta,
        String structureType,
        Integer unitCount,
        Instant createdAt) {
}
