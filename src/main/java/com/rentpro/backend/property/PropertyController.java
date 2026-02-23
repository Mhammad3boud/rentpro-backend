package com.rentpro.backend.property;

import com.rentpro.backend.property.dto.CreatePropertyRequest;
import com.rentpro.backend.property.dto.PropertyWithUnitsDto;
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
}
