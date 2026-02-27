package com.rentpro.backend.lease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentpro.backend.property.Property;
import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.unit.Unit;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "leases")
public class Lease {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @Column(name = "termination_reason")
    private String terminationReason;

    @Column(name = "termination_notes")
    private String terminationNotes;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "check_in_notes")
    private String checkInNotes;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "check_out_reason")
    private String checkOutReason;

    @Column(name = "check_out_notes")
    private String checkOutNotes;

    @Column(name = "check_in_checklist_json", columnDefinition = "TEXT")
    @JsonIgnore
    private String checkInChecklistJson;

    @Column(name = "check_out_checklist_json", columnDefinition = "TEXT")
    @JsonIgnore
    private String checkOutChecklistJson;

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

    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }

    public String getTerminationNotes() { return terminationNotes; }
    public void setTerminationNotes(String terminationNotes) { this.terminationNotes = terminationNotes; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }

    public LocalDateTime getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(LocalDateTime terminatedAt) { this.terminatedAt = terminatedAt; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }

    public String getCheckInNotes() { return checkInNotes; }
    public void setCheckInNotes(String checkInNotes) { this.checkInNotes = checkInNotes; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public String getCheckOutReason() { return checkOutReason; }
    public void setCheckOutReason(String checkOutReason) { this.checkOutReason = checkOutReason; }

    public String getCheckOutNotes() { return checkOutNotes; }
    public void setCheckOutNotes(String checkOutNotes) { this.checkOutNotes = checkOutNotes; }

    public String getCheckInChecklistJson() { return checkInChecklistJson; }
    public void setCheckInChecklistJson(String checkInChecklistJson) { this.checkInChecklistJson = checkInChecklistJson; }

    public String getCheckOutChecklistJson() { return checkOutChecklistJson; }
    public void setCheckOutChecklistJson(String checkOutChecklistJson) { this.checkOutChecklistJson = checkOutChecklistJson; }

    @JsonProperty("checkInChecklist")
    public List<LeaseChecklistItem> getCheckInChecklist() {
        return parseChecklist(checkInChecklistJson);
    }

    @JsonProperty("checkOutChecklist")
    public List<LeaseChecklistItem> getCheckOutChecklist() {
        return parseChecklist(checkOutChecklistJson);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    private List<LeaseChecklistItem> parseChecklist(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<LeaseChecklistItem>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
