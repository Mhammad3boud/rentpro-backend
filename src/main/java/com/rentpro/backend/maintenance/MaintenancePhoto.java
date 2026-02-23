package com.rentpro.backend.maintenance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "maintenance_photos")
public class MaintenancePhoto {

    @Id
    @Column(name = "photo_id")
    private UUID photoId = UUID.randomUUID();

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private MaintenanceRequest maintenanceRequest;

    @Column(nullable = false)
    private String imageUrl; // stored file path or cloud link

    public UUID getPhotoId() { return photoId; }

    public MaintenanceRequest getMaintenanceRequest() { return maintenanceRequest; }
    public void setMaintenanceRequest(MaintenanceRequest maintenanceRequest) {
        this.maintenanceRequest = maintenanceRequest;
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
