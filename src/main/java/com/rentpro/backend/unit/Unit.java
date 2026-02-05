package com.rentpro.backend.unit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rentpro.backend.property.Property;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnore
    private Property property;

    @Column(name = "unit_no", nullable = false, length = 50)
    private String unitNo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
