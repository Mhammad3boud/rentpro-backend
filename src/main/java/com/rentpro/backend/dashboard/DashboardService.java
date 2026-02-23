package com.rentpro.backend.dashboard;

import com.rentpro.backend.dashboard.dto.OwnerDashboardResponse;
import com.rentpro.backend.dashboard.dto.TenantDashboardResponse;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.maintenance.MaintenanceRepository;
import com.rentpro.backend.maintenance.MaintenanceStatus;
import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPaymentRepository;
import com.rentpro.backend.property.PropertyRepository;
import com.rentpro.backend.tenant.TenantRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class DashboardService {

    private final RentPaymentRepository rentPaymentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final LeaseRepository leaseRepository;

    public DashboardService(RentPaymentRepository rentPaymentRepository,
                            MaintenanceRepository maintenanceRepository,
                            PropertyRepository propertyRepository,
                            TenantRepository tenantRepository,
                            LeaseRepository leaseRepository) {
        this.rentPaymentRepository = rentPaymentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.propertyRepository = propertyRepository;
        this.tenantRepository = tenantRepository;
        this.leaseRepository = leaseRepository;
    }

    public OwnerDashboardResponse getOwnerDashboard(UUID ownerId) {

        BigDecimal totalExpected = rentPaymentRepository.sumExpectedByOwner(ownerId);
        BigDecimal totalCollected = rentPaymentRepository.sumPaidByOwner(ownerId);
        BigDecimal totalOutstanding = totalExpected.subtract(totalCollected);
        BigDecimal monthlyRevenue = rentPaymentRepository.sumCurrentMonthRevenueByOwner(ownerId);

        long overdueCount = rentPaymentRepository.countByOwnerAndStatus(ownerId, RentPayment.PaymentStatus.OVERDUE);
        long partialCount = rentPaymentRepository.countByOwnerAndStatus(ownerId, RentPayment.PaymentStatus.PARTIAL);
        long pendingCount = rentPaymentRepository.countByOwnerAndStatus(ownerId, RentPayment.PaymentStatus.PENDING);

        long pending = maintenanceRepository.countByOwnerAndStatus(ownerId, MaintenanceStatus.PENDING);
        long inProgress = maintenanceRepository.countByOwnerAndStatus(ownerId, MaintenanceStatus.IN_PROGRESS);
        long resolved = maintenanceRepository.countByOwnerAndStatus(ownerId, MaintenanceStatus.RESOLVED);

        // Count properties and tenants
        long totalProperties = propertyRepository.findByOwner_UserId(ownerId).size();
        long totalTenants = tenantRepository.findByOwner_UserId(ownerId).size();
        long activeLeases = leaseRepository.countActiveLeasesByOwner(ownerId, LocalDate.now());

        return new OwnerDashboardResponse(
                totalExpected,
                totalCollected,
                totalOutstanding,
                monthlyRevenue,
                overdueCount,
                partialCount,
                pendingCount,
                pending,
                inProgress,
                resolved,
                0L, // rejected - removed from enum
                totalProperties,
                totalTenants,
                activeLeases
        );
    }

    public TenantDashboardResponse getTenantDashboard(UUID tenantUserId) {

        BigDecimal expected = rentPaymentRepository.sumExpectedByTenant(tenantUserId);
        BigDecimal paid = rentPaymentRepository.sumPaidByTenant(tenantUserId);
        BigDecimal outstanding = expected.subtract(paid);

        long overdueCount = rentPaymentRepository.countOverdueByTenant(tenantUserId);

        LocalDate nextDueDate = rentPaymentRepository.nextDueDate(tenantUserId, LocalDate.now());

        long maintenanceCount = maintenanceRepository.countByTenant_User_UserId(tenantUserId);

        return new TenantDashboardResponse(
                outstanding,
                overdueCount,
                nextDueDate,
                maintenanceCount
        );
    }
}
