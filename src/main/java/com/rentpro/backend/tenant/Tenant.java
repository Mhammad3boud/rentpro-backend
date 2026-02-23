package com.rentpro.backend.tenant;

import com.rentpro.backend.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId = UUID.randomUUID();

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name="full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name="emergency_contact")
    private String emergencyContact;

    private String address;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getTenantId() { return tenantId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
