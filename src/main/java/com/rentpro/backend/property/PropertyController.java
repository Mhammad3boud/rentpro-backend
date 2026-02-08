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

import java.util.List;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final PropertyService propertyService;

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

        List<PropertyResponse> list = propertyRepo.findAllByOwner_Id(owner.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest req,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return propertyRepo.findByIdAndOwner_Id(id, owner.getId())
                .map(p -> {
                    // ✅ put rules in service (same validation rules as create)
                    Property updated = propertyService.updateProperty(p, req);
                    return ResponseEntity.ok(toResponse(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable Long id,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return propertyRepo.findByIdAndOwner_Id(id, owner.getId())
                .map(p -> {
                    propertyRepo.delete(p);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.<Void>notFound().build());
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
                p.getMeta(),
                p.getStructureType(),
                p.getUnitCount(),
                p.getCreatedAt());
    }
}
