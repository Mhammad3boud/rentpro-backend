package com.rentpro.backend.property;

import com.rentpro.backend.property.dto.CreatePropertyRequest;
import com.rentpro.backend.property.dto.PropertyWithUnitsDto;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping("/{ownerId}")
    public Property createProperty(@PathVariable UUID ownerId,
                                   @RequestBody CreatePropertyRequest request) {
        return propertyService.createProperty(ownerId, request);
    }

    @GetMapping("/{ownerId}")
    public List<PropertyWithUnitsDto> getOwnerProperties(@PathVariable UUID ownerId) {
        return propertyService.getOwnerPropertiesWithUnits(ownerId);
    }

    @PutMapping("/{propertyId}")
    public Property updateProperty(Authentication auth,
                                   @PathVariable UUID propertyId,
                                   @RequestBody CreatePropertyRequest request) {
        UUID ownerId = getOwnerId(auth);
        return propertyService.updateProperty(ownerId, propertyId, request);
    }

    @DeleteMapping("/{propertyId}")
    public void deleteProperty(Authentication auth, @PathVariable UUID propertyId) {
        UUID ownerId = getOwnerId(auth);
        propertyService.deleteProperty(ownerId, propertyId);
    }

    private UUID getOwnerId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof JwtUserContext ctx)) {
            throw new RuntimeException("Unauthorized");
        }
        return UUID.fromString(ctx.userId());
    }
}
