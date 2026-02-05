package com.rentpro.backend.lease;

import com.rentpro.backend.unit.Unit;
import com.rentpro.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "leases",
    indexes = {
        @Index(name = "ix_leases_unit_id", columnList = "unit_id"),
        @Index(name = "ix_leases_tenant_id", columnList = "tenant_id")
    }
)
public class Lease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Unit being leased
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    // 👤 Tenant
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    // 📅 Lease period
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate; // NULL = active lease

    // 💰 Financials
    @Column(name = "rent_amount", precision = 12, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    // 📝 Optional notes
    @Column(columnDefinition = "text")
    private String notes;

    // 🕒 Audit
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
