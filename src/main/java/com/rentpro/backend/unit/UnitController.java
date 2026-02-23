package com.rentpro.backend.unit;

import com.rentpro.backend.unit.dto.CreateUnitRequest;
import com.rentpro.backend.unit.dto.CreateUnitsBulkRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties/{propertyId}/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @PostMapping
    public Unit createUnit(@PathVariable UUID propertyId,
                           @RequestBody CreateUnitRequest request) {
        return unitService.createUnit(propertyId, request);
    }

    @PostMapping("/bulk")
    public List<Unit> createUnitsBulk(@PathVariable UUID propertyId,
                                      @RequestBody CreateUnitsBulkRequest request) {
        return unitService.createUnitsBulk(propertyId, request);
    }

    @GetMapping
    public List<Unit> getUnits(@PathVariable UUID propertyId) {
        return unitService.getUnitsByProperty(propertyId);
    }
}
