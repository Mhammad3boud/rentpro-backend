package com.rentpro.backend.lease;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rentpro.backend.property.Property;
import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.unit.Unit;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leases")
public class Lease {

    @Id
    @Column(name = "lease_id")
    private UUID leaseId = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "owner", "units"})
    private Property property;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unit_id") // nullable for standalone
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "property"})
    private Unit unit;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
    private Tenant tenant;

    @Column(name = "lease_name")
    private String leaseName;

    @Column(name = "monthly_rent", nullable = false)
    private BigDecimal monthlyRent;

    @Column(name = "security_deposit")
    private BigDecimal securityDeposit;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "lease_status", nullable = false)
    private LeaseStatus leaseStatus = LeaseStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getLeaseId() { return leaseId; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getLeaseName() { return leaseName; }
    public void setLeaseName(String leaseName) { this.leaseName = leaseName; }

    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }

    public BigDecimal getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(BigDecimal securityDeposit) { this.securityDeposit = securityDeposit; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LeaseStatus getLeaseStatus() { return leaseStatus; }
    public void setLeaseStatus(LeaseStatus leaseStatus) { this.leaseStatus = leaseStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
