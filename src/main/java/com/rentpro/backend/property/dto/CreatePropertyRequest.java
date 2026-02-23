package com.rentpro.backend.property.dto;

import com.rentpro.backend.property.PropertyType;
import com.rentpro.backend.property.UsageType;
import java.util.List;

/**
 * Request to create a new property.
 * Supports both new format (propertyName, propertyType) and legacy format (title, meta).
 */
public record CreatePropertyRequest(
        // New format fields (matching database schema)
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
        Integer unitCount,
        List<String> unitNumbers,
        
        // Legacy format fields (for backwards compatibility)
        String title,
        String category,
        PropertyMeta meta,
        String status,
        List<String> units,
        String waterMeterNumber,
        String notes
) {
    public record PropertyMeta(
            String propertyType,
            String propertyUsage,
            String region,
            String postcode,
            String electricityMeterNumber
    ) {}
    
    // Helper to get property name from either format
    public String getEffectivePropertyName() {
        return propertyName != null ? propertyName : title;
    }
    
    // Helper to get property type from either format
    public PropertyType getEffectivePropertyType() {
        if (propertyType != null) return propertyType;
        if (meta != null && meta.propertyType() != null) {
            return PropertyType.valueOf(meta.propertyType().toUpperCase());
        }
        return PropertyType.STANDALONE;
    }
    
    // Helper to get usage type from either format
    public UsageType getEffectiveUsageType() {
        if (usageType != null) return usageType;
        if (meta != null && meta.propertyUsage() != null) {
            try {
                return UsageType.valueOf(meta.propertyUsage().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
    
    // Helper to get region from either format
    public String getEffectiveRegion() {
        return region != null ? region : (meta != null ? meta.region() : null);
    }
    
    // Helper to get postcode from either format
    public String getEffectivePostcode() {
        return postcode != null ? postcode : (meta != null ? meta.postcode() : null);
    }
    
    // Helper to get water meter number from either format
    public String getEffectiveWaterMeterNo() {
        return waterMeterNo != null ? waterMeterNo : waterMeterNumber;
    }
    
    // Helper to get electricity meter number from either format
    public String getEffectiveElectricityMeterNo() {
        return electricityMeterNo != null ? electricityMeterNo : (meta != null ? meta.electricityMeterNumber() : null);
    }
    
    // Helper to get unit numbers from either format
    public List<String> getEffectiveUnitNumbers() {
        return unitNumbers != null ? unitNumbers : units;
    }
}
