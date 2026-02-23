package com.rentpro.backend.activity;

import com.rentpro.backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @Column(name = "activity_id")
    private UUID activityId = UUID.randomUUID();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Activity() {}

    public Activity(User user, ActivityType activityType, String title, String description) {
        this.user = user;
        this.activityType = activityType;
        this.title = title;
        this.description = description;
    }

    public Activity(User user, ActivityType activityType, String title, String description, String entityType, UUID entityId) {
        this.user = user;
        this.activityType = activityType;
        this.title = title;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // Getters & Setters
    public UUID getActivityId() { return activityId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
