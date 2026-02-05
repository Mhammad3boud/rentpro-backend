package com.rentpro.backend.property.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record UpdatePropertyRequest(
        @NotBlank String title,
        String address,
        Double latitude,
        Double longitude,
        String category,
        String structureType,
        Integer unitCount,
        String notes,
        Map<String, Object> meta
) {}
