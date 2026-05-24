package com.rentpro.backend.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "property_photos")
public class PropertyPhoto {

    @Id
    @Column(name = "photo_id")
    private UUID photoId = UUID.randomUUID();

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public UUID getPhotoId() { return photoId; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
