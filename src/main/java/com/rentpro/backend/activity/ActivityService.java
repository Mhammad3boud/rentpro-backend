package com.rentpro.backend.activity;

import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public ActivityService(ActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    public List<Activity> getRecentActivities(UUID userId) {
        return activityRepository.findTop10ByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    public List<Activity> getAllActivities(UUID userId) {
        return activityRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    public Activity logActivity(UUID userId, ActivityType type, String title, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Activity activity = new Activity(user, type, title, description);
        return activityRepository.save(activity);
    }

    public Activity logActivity(UUID userId, ActivityType type, String title, String description, 
                                String entityType, UUID entityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Activity activity = new Activity(user, type, title, description, entityType, entityId);
        return activityRepository.save(activity);
    }

    // Convenience methods for specific activities
    public void logMaintenanceCreated(UUID userId, String propertyName, String unitNumber, String title) {
        String desc = unitNumber != null 
            ? String.format("New maintenance request '%s' for %s Unit %s", title, propertyName, unitNumber)
            : String.format("New maintenance request '%s' for %s", title, propertyName);
        logActivity(userId, ActivityType.MAINTENANCE_CREATED, "Maintenance Request Created", desc);
    }

    public void logMaintenanceStatusChanged(UUID userId, String title, String oldStatus, String newStatus) {
        String desc = String.format("Maintenance '%s' status changed from %s to %s", title, oldStatus, newStatus);
        logActivity(userId, ActivityType.MAINTENANCE_STATUS_CHANGED, "Maintenance Status Updated", desc);
    }

    public void logPaymentReceived(UUID userId, String propertyName, String unitNumber, String monthYear, double amount) {
        String location = unitNumber != null 
            ? String.format("%s Unit %s", propertyName, unitNumber) 
            : propertyName;
        String desc = String.format("Payment of TZS %.0f received for %s (%s)", amount, location, monthYear);
        logActivity(userId, ActivityType.PAYMENT_RECEIVED, "Payment Received", desc);
    }

    public void logPaymentUpdated(UUID userId, String propertyName, String unitNumber, String monthYear) {
        String location = unitNumber != null 
            ? String.format("%s Unit %s", propertyName, unitNumber) 
            : propertyName;
        String desc = String.format("Payment updated for %s (%s)", location, monthYear);
        logActivity(userId, ActivityType.PAYMENT_UPDATED, "Payment Updated", desc);
    }

    public void logLeaseCreated(UUID userId, String tenantName, String propertyName, String unitNumber) {
        String location = unitNumber != null 
            ? String.format("%s Unit %s", propertyName, unitNumber) 
            : propertyName;
        String desc = String.format("New lease created for %s at %s", tenantName, location);
        logActivity(userId, ActivityType.LEASE_CREATED, "Lease Created", desc);
    }

    public void logLeaseUpdated(UUID userId, String propertyName, String unitNumber, String changes) {
        String location = unitNumber != null 
            ? String.format("%s Unit %s", propertyName, unitNumber) 
            : propertyName;
        String desc = String.format("Lease updated for %s: %s", location, changes);
        logActivity(userId, ActivityType.LEASE_UPDATED, "Lease Updated", desc);
    }

    public void logPropertyCreated(UUID userId, String propertyName) {
        String desc = String.format("Property '%s' has been added", propertyName);
        logActivity(userId, ActivityType.PROPERTY_CREATED, "Property Created", desc);
    }

    public void logTenantCreated(UUID userId, String tenantName) {
        String desc = String.format("Tenant '%s' has been added", tenantName);
        logActivity(userId, ActivityType.TENANT_CREATED, "Tenant Created", desc);
    }

    public void logTenantAssigned(UUID userId, String tenantName, String propertyName, String unitNumber) {
        String location = unitNumber != null 
            ? String.format("%s Unit %s", propertyName, unitNumber) 
            : propertyName;
        String desc = String.format("Tenant '%s' assigned to %s", tenantName, location);
        logActivity(userId, ActivityType.TENANT_ASSIGNED, "Tenant Assigned", desc);
    }
}
