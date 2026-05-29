package com.rentpro.backend.ai;

import com.rentpro.backend.dashboard.DashboardService;
import com.rentpro.backend.dashboard.dto.OwnerDashboardResponse;
import com.rentpro.backend.dashboard.dto.TenantDashboardResponse;
import com.rentpro.backend.maintenance.MaintenanceRepository;
import com.rentpro.backend.maintenance.MaintenanceRequest;
import com.rentpro.backend.maintenance.MaintenanceStatus;
import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiContextService {

    private final DashboardService dashboardService;
    private final RentPaymentRepository rentPaymentRepository;
    private final MaintenanceRepository maintenanceRepository;

    public AiContextService(DashboardService dashboardService,
                             RentPaymentRepository rentPaymentRepository,
                             MaintenanceRepository maintenanceRepository) {
        this.dashboardService = dashboardService;
        this.rentPaymentRepository = rentPaymentRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    public String buildOwnerContext(UUID ownerId) {
        OwnerDashboardResponse dash = dashboardService.getOwnerDashboard(ownerId);

        List<RentPayment> unpaid = rentPaymentRepository
                .findByLease_Property_Owner_UserId(ownerId).stream()
                .filter(p -> p.getPaymentStatus() == RentPayment.PaymentStatus.OVERDUE
                          || p.getPaymentStatus() == RentPayment.PaymentStatus.PENDING
                          || p.getPaymentStatus() == RentPayment.PaymentStatus.PARTIAL)
                .collect(Collectors.toList());

        List<MaintenanceRequest> openMaintenance = maintenanceRepository
                .findByProperty_Owner_UserId(ownerId).stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.PENDING
                          || m.getStatus() == MaintenanceStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        StringBuilder ctx = new StringBuilder();
        ctx.append("=== REAL DATA FROM DATABASE (today: ").append(LocalDate.now()).append(") ===\n\n");

        ctx.append("PORTFOLIO SUMMARY:\n");
        ctx.append("- Properties: ").append(dash.totalProperties()).append("\n");
        ctx.append("- Tenants: ").append(dash.totalTenants()).append("\n");
        ctx.append("- Active leases: ").append(dash.activeLeases()).append("\n");
        ctx.append("- Total outstanding: ").append(dash.totalOutstanding()).append(" (see per-payment currency below)\n");
        ctx.append("- This month's revenue: ").append(dash.monthlyRevenue()).append(" (see per-payment currency below)\n");
        ctx.append("- Overdue: ").append(dash.overdueCount())
           .append(", Partial: ").append(dash.partialCount())
           .append(", Pending: ").append(dash.pendingCount()).append("\n\n");

        if (!unpaid.isEmpty()) {
            ctx.append("UNPAID / OVERDUE PAYMENTS:\n");
            for (RentPayment p : unpaid) {
                String tenantName = tenantName(p);
                String unit = unitRef(p);
                ctx.append("- ").append(tenantName).append(" (").append(unit).append(")")
                   .append(": ").append(p.getCurrency() != null ? p.getCurrency() : "MYR")
                   .append(" ").append(p.getAmountExpected())
                   .append(", due ").append(p.getDueDate())
                   .append(", status: ").append(p.getPaymentStatus())
                   .append(", month: ").append(p.getMonthYear()).append("\n");
            }
            ctx.append("\n");
        } else {
            ctx.append("UNPAID / OVERDUE PAYMENTS: None\n\n");
        }

        if (!openMaintenance.isEmpty()) {
            ctx.append("OPEN MAINTENANCE REQUESTS:\n");
            for (MaintenanceRequest m : openMaintenance) {
                String tenantName = m.getTenant() != null && m.getTenant().getUser() != null
                        ? m.getTenant().getUser().getFullName() : "Unknown";
                String unitRef = m.getUnit() != null ? m.getUnit().getUnitNumber() : "—";
                ctx.append("- ").append(m.getTitle())
                   .append(" (").append(unitRef).append(", ").append(tenantName).append(")")
                   .append(": ").append(m.getPriority()).append(" priority, ")
                   .append(m.getStatus()).append("\n");
            }
        } else {
            ctx.append("OPEN MAINTENANCE REQUESTS: None\n");
        }

        return ctx.toString();
    }

    public String buildTenantContext(UUID tenantUserId) {
        TenantDashboardResponse dash = dashboardService.getTenantDashboard(tenantUserId);

        List<RentPayment> payments = rentPaymentRepository
                .findByLease_Tenant_User_UserId(tenantUserId);

        List<MaintenanceRequest> myRequests = maintenanceRepository
                .findByTenant_User_UserId(tenantUserId);

        StringBuilder ctx = new StringBuilder();
        ctx.append("=== REAL DATA FROM DATABASE (today: ").append(LocalDate.now()).append(") ===\n\n");

        ctx.append("YOUR PAYMENT SUMMARY:\n");
        ctx.append("- Outstanding balance: ").append(dash.totalOutstanding()).append("\n");
        ctx.append("- Overdue payments: ").append(dash.overdueCount()).append("\n");
        ctx.append("- Next due date: ").append(dash.nextDueDate() != null ? dash.nextDueDate() : "none").append("\n\n");

        if (!payments.isEmpty()) {
            ctx.append("YOUR RECENT PAYMENTS:\n");
            payments.stream()
                    .sorted((a, b) -> b.getMonthYear().compareTo(a.getMonthYear()))
                    .limit(6)
                    .forEach(p -> {
                        String cur = p.getCurrency() != null ? p.getCurrency() : "MYR";
                        ctx.append("- ").append(p.getMonthYear())
                           .append(": ").append(cur).append(" ").append(p.getAmountPaid())
                           .append(" / ").append(cur).append(" ").append(p.getAmountExpected())
                           .append(" (").append(p.getPaymentStatus()).append(")\n");
                    });
            ctx.append("\n");
        }

        if (!myRequests.isEmpty()) {
            ctx.append("YOUR MAINTENANCE REQUESTS:\n");
            myRequests.forEach(m -> ctx.append("- ").append(m.getTitle())
                    .append(": ").append(m.getStatus()).append("\n"));
        }

        return ctx.toString();
    }

    private String tenantName(RentPayment p) {
        try {
            return p.getLease().getTenant().getUser().getFullName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String unitRef(RentPayment p) {
        try {
            if (p.getLease().getUnit() != null) {
                return p.getLease().getUnit().getUnitNumber();
            }
            return p.getLease().getProperty().getPropertyName();
        } catch (Exception e) {
            return "—";
        }
    }
}
