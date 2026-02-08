package com.rentpro.backend.property.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreatePropertyRequest(

        @NotBlank
        String title,

        @NotBlank
        String category,

        String notes,

        @NotBlank
        String structureType,

        @NotNull
        @Min(1)
        Integer unitCount,

        String address,

        Double latitude,
        Double longitude,

        Map<String, Object> meta
) {}
