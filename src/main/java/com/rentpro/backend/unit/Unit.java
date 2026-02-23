package com.rentpro.backend.unit;

import com.rentpro.backend.property.Property;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "units",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "unit_number"})
)
public class Unit {

    @Id
    @Column(name = "unit_id")
    private UUID unitId = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getUnitId() { return unitId; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
