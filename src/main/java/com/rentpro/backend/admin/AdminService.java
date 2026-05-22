package com.rentpro.backend.admin;

import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.lease.LeaseStatus;
import com.rentpro.backend.maintenance.MaintenanceRepository;
import com.rentpro.backend.maintenance.MaintenanceStatus;
import com.rentpro.backend.payment.RentPaymentRepository;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.unit.UnitRepository;
import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final LeaseRepository leaseRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public AdminService(UserRepository userRepository,
                        PropertyRepository propertyRepository,
                        UnitRepository unitRepository,
                        LeaseRepository leaseRepository,
                        MaintenanceRepository maintenanceRepository,
                        RentPaymentRepository rentPaymentRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.leaseRepository = leaseRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.rentPaymentRepository = rentPaymentRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public AdminStats getStats() {
        long total = userRepository.count();
        long owners = userRepository.countByRole(Role.OWNER);
        long tenants = userRepository.countByRole(Role.TENANT);
        long active = userRepository.countByStatus(true);
        long inactive = userRepository.countByStatus(false);
        return new AdminStats(total, owners, tenants, active, inactive);
    }

    public AdminAnalytics getAnalytics() {
        long totalProperties = propertyRepository.count();
        long totalUnits = unitRepository.count();
        long activeLeases = leaseRepository.countByLeaseStatus(LeaseStatus.ACTIVE);
        long expiredLeases = leaseRepository.countByLeaseStatus(LeaseStatus.EXPIRED);
        BigDecimal revenueThisMonth = rentPaymentRepository.sumPlatformRevenueThisMonth();
        BigDecimal totalOutstanding = rentPaymentRepository.sumPlatformOutstanding();
        long maintenancePending = maintenanceRepository.countByStatus(MaintenanceStatus.PENDING);
        long maintenanceInProgress = maintenanceRepository.countByStatus(MaintenanceStatus.IN_PROGRESS);
        long maintenanceResolved = maintenanceRepository.countByStatus(MaintenanceStatus.RESOLVED);
        return new AdminAnalytics(
                totalProperties, totalUnits,
                activeLeases, expiredLeases,
                revenueThisMonth, totalOutstanding,
                maintenancePending, maintenanceInProgress, maintenanceResolved
        );
    }

    public void setUserStatus(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot modify SUPER_ADMIN account");
        }
        user.setStatus(active);
        userRepository.save(user);
    }

    public void setUserRole(UUID userId, Role newRole) {
        if (newRole == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot assign SUPER_ADMIN role via API");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Cannot modify SUPER_ADMIN account");
        }
        user.setRole(newRole);
        userRepository.save(user);
    }

    public record AdminStats(long totalUsers, long ownerCount, long tenantCount, long activeCount, long inactiveCount) {}

    public record AdminAnalytics(
            long totalProperties,
            long totalUnits,
            long activeLeases,
            long expiredLeases,
            BigDecimal revenueThisMonth,
            BigDecimal totalOutstanding,
            long maintenancePending,
            long maintenanceInProgress,
            long maintenanceResolved
    ) {}
}
