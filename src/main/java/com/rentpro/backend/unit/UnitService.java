package com.rentpro.backend.unit;

import com.rentpro.backend.property.Property;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.unit.dto.CreateUnitRequest;
import com.rentpro.backend.unit.dto.CreateUnitsBulkRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UnitService {

    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;

    public UnitService(UnitRepository unitRepository, PropertyRepository propertyRepository) {
        this.unitRepository = unitRepository;
        this.propertyRepository = propertyRepository;
    }

    public Unit createUnit(UUID propertyId, CreateUnitRequest request) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Prevent duplicate unit numbers per property (extra safety)
        unitRepository.findByProperty_PropertyIdAndUnitNumber(propertyId, request.unitNumber())
                .ifPresent(u -> { throw new RuntimeException("Unit number already exists for this property"); });

        Unit unit = new Unit();
        unit.setProperty(property);
        unit.setUnitNumber(request.unitNumber());

        return unitRepository.save(unit);
    }

    public List<Unit> createUnitsBulk(UUID propertyId, CreateUnitsBulkRequest request) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        List<Unit> created = new ArrayList<>();

        for (String unitNumber : request.unitNumbers()) {
            unitRepository.findByProperty_PropertyIdAndUnitNumber(propertyId, unitNumber)
                    .ifPresent(u -> { throw new RuntimeException("Duplicate unit: " + unitNumber); });

            Unit unit = new Unit();
            unit.setProperty(property);
            unit.setUnitNumber(unitNumber);
            created.add(unit);
        }

        return unitRepository.saveAll(created);
    }

    public List<Unit> getUnitsByProperty(UUID propertyId) {
        return unitRepository.findByProperty_PropertyId(propertyId);
    }
}
