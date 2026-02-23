package com.rentpro.backend.activity;

import com.rentpro.backend.security.JwtUserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityDto>> getRecentActivities(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID userId = UUID.fromString(ctx.userId());
        
        List<Activity> activities = activityService.getRecentActivities(userId);
        List<ActivityDto> dtos = activities.stream()
                .map(this::toDto)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ActivityDto>> getAllActivities(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID userId = UUID.fromString(ctx.userId());
        
        List<Activity> activities = activityService.getAllActivities(userId);
        List<ActivityDto> dtos = activities.stream()
                .map(this::toDto)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }

    private ActivityDto toDto(Activity activity) {
        return new ActivityDto(
                activity.getActivityId().toString(),
                activity.getActivityType().name(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getEntityType(),
                activity.getEntityId() != null ? activity.getEntityId().toString() : null,
                activity.getCreatedAt().toString()
        );
    }

    public record ActivityDto(
            String activityId,
            String type,
            String title,
            String description,
            String entityType,
            String entityId,
            String createdAt
    ) {}
}
