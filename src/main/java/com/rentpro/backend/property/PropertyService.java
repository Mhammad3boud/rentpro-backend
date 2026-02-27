package com.rentpro.backend.property;

import com.rentpro.backend.property.dto.CreatePropertyRequest;
import com.rentpro.backend.property.dto.PropertyWithUnitsDto;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;

    public PropertyService(PropertyRepository propertyRepository,
                           UserRepository userRepository,
                           UnitRepository unitRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.unitRepository = unitRepository;
    }

    @Transactional
    public Property createProperty(UUID ownerId, CreatePropertyRequest request) {

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Property property = new Property();
        property.setOwner(owner);
        property.setPropertyName(request.getEffectivePropertyName());
        property.setPropertyType(request.getEffectivePropertyType());
        property.setUsageType(request.getEffectiveUsageType());
        property.setAssetCategory(request.getEffectiveAssetCategory());
        property.setAddress(request.address());
        property.setRegion(request.getEffectiveRegion());
        property.setPostcode(request.getEffectivePostcode());
        property.setLatitude(request.latitude());
        property.setLongitude(request.longitude());
        property.setWaterMeterNo(request.getEffectiveWaterMeterNo());
        property.setElectricityMeterNo(request.getEffectiveElectricityMeterNo());

        Property savedProperty = propertyRepository.save(property);

        // Create units for multi-unit properties
        if (request.getEffectivePropertyType() == PropertyType.MULTI_UNIT) {
            List<String> unitNames = request.getEffectiveUnitNumbers();
            
            if (unitNames != null && !unitNames.isEmpty()) {
                // Use provided unit names
                for (String unitName : unitNames) {
                    Unit unit = new Unit();
                    unit.setProperty(savedProperty);
                    unit.setUnitNumber(unitName);
                    unitRepository.save(unit);
                }
            } else if (request.unitCount() != null && request.unitCount() > 0) {
                // Generate default unit names based on count
                for (int i = 1; i <= request.unitCount(); i++) {
                    Unit unit = new Unit();
                    unit.setProperty(savedProperty);
                    unit.setUnitNumber("Unit " + i);
                    unitRepository.save(unit);
                }
            }
        }

        return savedProperty;
    }

    public List<Property> getOwnerProperties(UUID ownerId) {
        return propertyRepository.findByOwner_UserId(ownerId);
    }

    public List<PropertyWithUnitsDto> getOwnerPropertiesWithUnits(UUID ownerId) {
        List<Property> properties = propertyRepository.findByOwner_UserId(ownerId);
        return properties.stream()
                .map(property -> {
                    List<Unit> units = unitRepository.findByProperty_PropertyId(property.getPropertyId());
                    return PropertyWithUnitsDto.fromEntity(property, units);
                })
                .toList();
    }

    @Transactional
    public Property updateProperty(UUID ownerId, UUID propertyId, CreatePropertyRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (!property.getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        String propertyName = trimToNull(request.propertyName());
        if (propertyName == null) {
            propertyName = trimToNull(request.title());
        }
        if (propertyName != null) {
            property.setPropertyName(propertyName);
        }

        PropertyType propertyType = resolvePropertyType(request);
        if (propertyType != null) {
            property.setPropertyType(propertyType);
        }

        AssetCategory assetCategory = resolveAssetCategory(request);
        if (assetCategory != null) {
            property.setAssetCategory(assetCategory);
        }

        UsageType usageType = resolveUsageType(request);
        if (usageType != null) {
            property.setUsageType(usageType);
        }

        if (request.address() != null) property.setAddress(trimToNull(request.address()));
        if (request.region() != null || (request.meta() != null && request.meta().region() != null)) {
            property.setRegion(trimToNull(request.getEffectiveRegion()));
        }
        if (request.postcode() != null || (request.meta() != null && request.meta().postcode() != null)) {
            property.setPostcode(trimToNull(request.getEffectivePostcode()));
        }
        if (request.latitude() != null) property.setLatitude(request.latitude());
        if (request.longitude() != null) property.setLongitude(request.longitude());

        if (request.waterMeterNo() != null || request.waterMeterNumber() != null) {
            property.setWaterMeterNo(trimToNull(request.getEffectiveWaterMeterNo()));
        }
        if (request.electricityMeterNo() != null || (request.meta() != null && request.meta().electricityMeterNumber() != null)) {
            property.setElectricityMeterNo(trimToNull(request.getEffectiveElectricityMeterNo()));
        }

        return propertyRepository.save(property);
    }

    @Transactional
    public void deleteProperty(UUID ownerId, UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (!property.getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        propertyRepository.delete(property);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PropertyType resolvePropertyType(CreatePropertyRequest request) {
        if (request.propertyType() != null) return request.propertyType();
        return null;
    }

    private AssetCategory resolveAssetCategory(CreatePropertyRequest request) {
        if (request.assetCategory() != null) return request.assetCategory();
        if (request.meta() != null && request.meta().propertyType() != null) {
            try {
                return AssetCategory.valueOf(request.meta().propertyType().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private UsageType resolveUsageType(CreatePropertyRequest request) {
        if (request.usageType() != null) return request.usageType();
        if (request.meta() != null && request.meta().propertyUsage() != null) {
            String usage = request.meta().propertyUsage().trim().toUpperCase();
            if ("AGRICULTURAL".equals(usage) || "INDUSTRIAL".equals(usage)) {
                return UsageType.COMMERCIAL;
            }
            try {
                return UsageType.valueOf(usage);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (request.category() != null) {
            try {
                return UsageType.valueOf(request.category().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
