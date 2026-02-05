package com.rentpro.backend.property;

import com.rentpro.backend.property.dto.CreatePropertyRequest;
import com.rentpro.backend.property.dto.PropertyResponse;
import com.rentpro.backend.property.dto.UpdatePropertyRequest;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/properties")
public class PropertyController {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "RENTAL", "FARM", "LAND", "WAREHOUSE", "OFFICE", "OTHER");

    private static final Set<String> ALLOWED_STRUCTURE = Set.of(
            "STANDALONE", "MULTI_UNIT");

    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final PropertyService propertyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PropertyController(PropertyRepository propertyRepo, UserRepository userRepo,
            PropertyService propertyService) {
        this.propertyRepo = propertyRepo;
        this.userRepo = userRepo;
        this.propertyService = propertyService;
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(
            @Valid @RequestBody CreatePropertyRequest req,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Property created = propertyService.createProperty(owner, req);
        return ResponseEntity.ok(toResponse(created));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<List<PropertyResponse>> myProperties(Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(
                propertyRepo.findAllByOwner_Id(owner.getId())
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest req,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        String category = normalizeCategory(req.category());
        String structureType = normalizeStructure(req.structureType());
        Integer unitCount = normalizeUnitCount(structureType, req.unitCount());

        return propertyRepo.findByIdAndOwner_Id(id, owner.getId())
                .map(p -> {
                    p.setTitle(req.title());
                    p.setAddress(req.address());
                    p.setLatitude(req.latitude());
                    p.setLongitude(req.longitude());
                    p.setCategory(category);
                    p.setNotes(req.notes());
                    p.setMeta(req.meta());
                    p.setStructureType(structureType);
                    p.setUnitCount(unitCount);
                    propertyRepo.save(p);
                    return ResponseEntity.ok(toResponse(p));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private PropertyResponse toResponse(Property p) {
        return new PropertyResponse(
                p.getId(),
                p.getTitle(),
                p.getAddress(),
                p.getLatitude(),
                p.getLongitude(),
                p.getCategory(),
                p.getNotes(),
                p.getMeta() == null ? null : objectMapper.valueToTree(p.getMeta()),
                p.getStructureType(),
                p.getUnitCount(),
                p.getCreatedAt());
    }

    private String normalizeCategory(String raw) {
        String c = (raw == null || raw.isBlank()) ? "RENTAL" : raw.trim().toUpperCase();
        if (!ALLOWED_CATEGORIES.contains(c))
            throw new RuntimeException("Invalid category: " + c);
        return c;
    }

    private String normalizeStructure(String raw) {
        String s = (raw == null || raw.isBlank()) ? "STANDALONE" : raw.trim().toUpperCase();
        if (!ALLOWED_STRUCTURE.contains(s))
            throw new RuntimeException("Invalid structureType: " + s);
        return s;
    }

    private Integer normalizeUnitCount(String structureType, Integer unitCount) {
        if ("STANDALONE".equals(structureType))
            return 1;
        if (unitCount == null || unitCount < 1)
            throw new RuntimeException("unitCount must be >= 1 for MULTI_UNIT");
        return unitCount;
    }
}
