package com.rentpro.backend.maintenance;

import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "maintenance_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 20)
    private String priority; // LOW / MEDIUM / HIGH

    @Column(nullable = false, length = 20)
    private String status;   // OPEN / IN_PROGRESS / RESOLVED

    @Column(name = "owner_notes", columnDefinition = "text")
    private String ownerNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
