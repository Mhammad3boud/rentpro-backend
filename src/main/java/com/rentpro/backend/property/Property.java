package com.rentpro.backend.property;

import com.rentpro.backend.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @Column(name = "property_id")
    private UUID propertyId = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "property_name", nullable = false)
    private String propertyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type")
    private UsageType usageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_category")
    private AssetCategory assetCategory;

    private String address;
    private String region;
    private String postcode;

    private Double latitude;
    private Double longitude;

    @Column(name = "water_meter_no")
    private String waterMeterNo;

    @Column(name = "electricity_meter_no")
    private String electricityMeterNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getPropertyId() { return propertyId; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public PropertyType getPropertyType() { return propertyType; }
    public void setPropertyType(PropertyType propertyType) { this.propertyType = propertyType; }
    public UsageType getUsageType() { return usageType; }
    public void setUsageType(UsageType usageType) { this.usageType = usageType; }
    public AssetCategory getAssetCategory() { return assetCategory; }
    public void setAssetCategory(AssetCategory assetCategory) { this.assetCategory = assetCategory; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getWaterMeterNo() { return waterMeterNo; }
    public void setWaterMeterNo(String waterMeterNo) { this.waterMeterNo = waterMeterNo; }
    public String getElectricityMeterNo() { return electricityMeterNo; }
    public void setElectricityMeterNo(String electricityMeterNo) { this.electricityMeterNo = electricityMeterNo; }
}
