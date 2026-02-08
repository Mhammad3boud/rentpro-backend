package com.rentpro.backend.property.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdatePropertyRequest(

                @NotBlank String title,
                String address,
                Double latitude,
                Double longitude,
                String category,
                String structureType,
                @Min(1) Integer unitCount,
                String notes,
                Map<String, Object> meta) {
}
