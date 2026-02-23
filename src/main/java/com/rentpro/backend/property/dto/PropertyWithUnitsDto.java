package com.rentpro.backend.property.dto;

import com.rentpro.backend.property.Property;
import com.rentpro.backend.property.PropertyType;
import com.rentpro.backend.property.UsageType;
import com.rentpro.backend.unit.Unit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PropertyWithUnitsDto(
        UUID propertyId,
        String propertyName,
        PropertyType propertyType,
        UsageType usageType,
        String address,
        String region,
        String postcode,
        Double latitude,
        Double longitude,
        String waterMeterNo,
        String electricityMeterNo,
        LocalDateTime createdAt,
        List<UnitDto> units
) {
    public record UnitDto(
            UUID unitId,
            String unitNumber,
            LocalDateTime createdAt
    ) {
        public static UnitDto fromEntity(Unit unit) {
            return new UnitDto(
                    unit.getUnitId(),
                    unit.getUnitNumber(),
                    unit.getCreatedAt()
            );
        }
    }

    public static PropertyWithUnitsDto fromEntity(Property property, List<Unit> units) {
        return new PropertyWithUnitsDto(
                property.getPropertyId(),
                property.getPropertyName(),
                property.getPropertyType(),
                property.getUsageType(),
                property.getAddress(),
                property.getRegion(),
                property.getPostcode(),
                property.getLatitude(),
                property.getLongitude(),
                property.getWaterMeterNo(),
                property.getElectricityMeterNo(),
                null, // createdAt not exposed in Property entity getter
                units.stream().map(UnitDto::fromEntity).toList()
        );
    }
}
