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
}
