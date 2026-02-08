package com.rentpro.backend.dashboard;

import com.rentpro.backend.dashboard.dto.MaintenanceCountsResponse;
import com.rentpro.backend.dashboard.dto.OwnerDashboardSummaryResponse;
import com.rentpro.backend.dashboard.dto.OverdueInvoiceItem;
import com.rentpro.backend.dashboard.dto.OwnerDashboardResponse;
import com.rentpro.backend.invoice.Invoice;
import com.rentpro.backend.invoice.InvoiceRepository;
import com.rentpro.backend.maintenance.MaintenanceRepository;
import com.rentpro.backend.payment.allocation.PaymentAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepo;
    private final PaymentAllocationRepository allocRepo;
    private final MaintenanceRepository maintenanceRepo;

    public DashboardService(
            InvoiceRepository invoiceRepo,
            PaymentAllocationRepository allocRepo,
            MaintenanceRepository maintenanceRepo) {
        this.invoiceRepo = invoiceRepo;
        this.allocRepo = allocRepo;
        this.maintenanceRepo = maintenanceRepo;
    }

    @Transactional(readOnly = true)
    public OwnerDashboardSummaryResponse summary(Long ownerId, YearMonth from, YearMonth to) {
        if (to.isBefore(from))
            throw new RuntimeException("to must be >= from");

        List<Invoice> all = invoiceRepo.findAllByLease_Unit_Property_Owner_Id(ownerId);

        Map<YearMonth, BigDecimal> expectedByMonth = new HashMap<>();
        Map<YearMonth, BigDecimal> paidByMonth = new HashMap<>();

        for (Invoice inv : all) {
            YearMonth ym = YearMonth.of(inv.getPeriodYear(), inv.getPeriodMonth());
            if (ym.isBefore(from) || ym.isAfter(to))
                continue;

            expectedByMonth.put(ym, expectedByMonth.getOrDefault(ym, BigDecimal.ZERO).add(inv.getAmount()));

            BigDecimal paid = allocRepo.sumAppliedByInvoiceId(inv.getId());
            paidByMonth.put(ym, paidByMonth.getOrDefault(ym, BigDecimal.ZERO).add(paid));
        }

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        List<OwnerDashboardSummaryResponse.MonthlyCollectionItem> items = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {
            BigDecimal expected = expectedByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal paid = paidByMonth.getOrDefault(ym, BigDecimal.ZERO);

            BigDecimal outstanding = expected.subtract(paid);
            BigDecimal rate = BigDecimal.ZERO;
            if (expected.compareTo(BigDecimal.ZERO) > 0) {
                rate = paid.divide(expected, 4, RoundingMode.HALF_UP);
            }

            totalExpected = totalExpected.add(expected);
            totalPaid = totalPaid.add(paid);

            items.add(new OwnerDashboardSummaryResponse.MonthlyCollectionItem(
                    ym.format(fmt),
                    expected,
                    paid,
                    outstanding,
                    rate));
        }

        BigDecimal outstanding = totalExpected.subtract(totalPaid);
        BigDecimal collectionRate = BigDecimal.ZERO;
        if (totalExpected.compareTo(BigDecimal.ZERO) > 0) {
            collectionRate = totalPaid.divide(totalExpected, 4, RoundingMode.HALF_UP);
        }

        return new OwnerDashboardSummaryResponse(
                from.format(fmt),
                to.format(fmt),
                totalExpected,
                totalPaid,
                outstanding,
                collectionRate,
                items);
    }

    @Transactional(readOnly = true)
    public List<OverdueInvoiceItem> overdue(Long ownerId) {
        List<Invoice> all = invoiceRepo.findAllByLease_Unit_Property_Owner_Id(ownerId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        List<OverdueInvoiceItem> out = new ArrayList<>();
        for (Invoice inv : all) {
            BigDecimal paid = allocRepo.sumAppliedByInvoiceId(inv.getId());
            BigDecimal remaining = inv.getAmount().subtract(paid);

            boolean isPaid = paid.compareTo(inv.getAmount()) >= 0;
            if (isPaid)
                continue;

            String status = null;
            if (paid.compareTo(BigDecimal.ZERO) > 0)
                status = "PARTIAL";
            else if (inv.getDueDate().isBefore(today))
                status = "OVERDUE";

            if (status != null) {
                String period = String.format("%04d-%02d", inv.getPeriodYear(), inv.getPeriodMonth());
                out.add(new OverdueInvoiceItem(
                        inv.getId(),
                        inv.getLease().getId(),
                        period,
                        inv.getDueDate(),
                        inv.getAmount(),
                        paid,
                        remaining,
                        status));
            }
        }

        // Sort: OVERDUE first, then earliest due date
        out.sort(Comparator
                .comparing(OverdueInvoiceItem::status).reversed()
                .thenComparing(OverdueInvoiceItem::dueDate));

        return out;
    }

    @Transactional(readOnly = true)
    public MaintenanceCountsResponse maintenanceCounts(Long ownerId) {
        long open = maintenanceRepo.countByUnit_Property_Owner_IdAndStatus(ownerId, "OPEN");
        long inProgress = maintenanceRepo.countByUnit_Property_Owner_IdAndStatus(ownerId, "IN_PROGRESS");
        long resolved = maintenanceRepo.countByUnit_Property_Owner_IdAndStatus(ownerId, "RESOLVED");
        long highOpen = maintenanceRepo.countByUnit_Property_Owner_IdAndStatusAndPriority(ownerId, "OPEN", "HIGH");
        return new MaintenanceCountsResponse(open, inProgress, resolved, highOpen);
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse fullDashboard(Long ownerId, YearMonth from, YearMonth to) {
        return new OwnerDashboardResponse(
                summary(ownerId, from, to),
                overdue(ownerId),
                maintenanceCounts(ownerId));
    }

}
