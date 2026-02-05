package com.rentpro.backend.property;

import com.rentpro.backend.property.dto.CreatePropertyRequest;
import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class PropertyService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "RENTAL", "FARM", "LAND", "WAREHOUSE", "OFFICE", "OTHER");

    private static final Set<String> ALLOWED_STRUCTURE = Set.of(
            "STANDALONE", "MULTI_UNIT");

    private final PropertyRepository propertyRepo;
    private final UnitRepository unitRepo;

    public PropertyService(PropertyRepository propertyRepo, UnitRepository unitRepo) {
        this.propertyRepo = propertyRepo;
        this.unitRepo = unitRepo;
    }

    @Transactional
    public Property createProperty(User owner, CreatePropertyRequest req) {
        String category = normalizeCategory(req.category());
        String structureType = normalizeStructure(req.structureType());
        Integer unitCount = normalizeUnitCount(structureType, req.unitCount());

        Property property = Property.builder()
                .owner(owner)
                .title(req.title())
                .address(req.address())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .category(category)
                .notes(req.notes())
                .meta(req.meta())
                .structureType(structureType)
                .unitCount(unitCount)
                .createdAt(Instant.now())
                .build();

        propertyRepo.save(property);

        // Auto-create units
        if ("STANDALONE".equals(structureType)) {
            unitRepo.save(Unit.builder()
                    .property(property)
                    .unitNo("MAIN")
                    .createdAt(Instant.now())
                    .build());
        } else {
            for (int i = 1; i <= unitCount; i++) {
                unitRepo.save(Unit.builder()
                        .property(property)
                        .unitNo("Unit " + i)
                        .createdAt(Instant.now())
                        .build());
            }
        }

        return property;
    }

    private String normalizeCategory(String raw) {
        String c = (raw == null || raw.isBlank()) ? "RENTAL" : raw.trim().toUpperCase();
        if (!ALLOWED_CATEGORIES.contains(c)) {
            throw new RuntimeException("Invalid category. Allowed: " + ALLOWED_CATEGORIES);
        }
        return c;
    }

    private String normalizeStructure(String raw) {
        String s = (raw == null || raw.isBlank()) ? "STANDALONE" : raw.trim().toUpperCase();
        if (!ALLOWED_STRUCTURE.contains(s)) {
            throw new RuntimeException("Invalid structureType. Allowed: " + ALLOWED_STRUCTURE);
        }
        return s;
    }

    private Integer normalizeUnitCount(String structureType, Integer unitCount) {
        if ("STANDALONE".equals(structureType))
            return 1;
        if (unitCount == null || unitCount < 1) {
            throw new RuntimeException("unitCount must be >= 1 for MULTI_UNIT");
        }
        return unitCount;
    }
}
