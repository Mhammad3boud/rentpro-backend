package com.rentpro.backend.property.dto;

import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;

public record PropertyResponse(
        Long id,
        String title,
        String address,
        Double latitude,
        Double longitude,
        String category,
        String notes,
        JsonNode meta,
        String structureType,
        Integer unitCount,
        Instant createdAt) {
}
