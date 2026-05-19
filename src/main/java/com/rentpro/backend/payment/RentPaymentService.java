package com.rentpro.backend.payment;

import com.rentpro.backend.activity.ActivityService;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
// import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPayment.PaymentStatus;
import com.rentpro.backend.payment.dto.CreateOrUpdatePaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rentpro.backend.lease.LeaseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RentPaymentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final LeaseRepository leaseRepository;
    private final ActivityService activityService;

    public RentPaymentService(RentPaymentRepository rentPaymentRepository,
                              LeaseRepository leaseRepository,
                              ActivityService activityService) {
        this.rentPaymentRepository = rentPaymentRepository;
        this.leaseRepository = leaseRepository;
        this.activityService = activityService;
    }

    // Owner records or edits payment for a lease/month
    public RentPayment upsertPayment(UUID ownerId, CreateOrUpdatePaymentRequest request) {
        return upsertPaymentInternal(ownerId, null, request, true);
    }

    // Tenant gateway completion path
    public RentPayment upsertPaymentForTenant(UUID tenantUserId, CreateOrUpdatePaymentRequest request) {
        return upsertPaymentInternal(null, tenantUserId, request, false);
    }

    private RentPayment upsertPaymentInternal(UUID ownerId, UUID tenantUserId, CreateOrUpdatePaymentRequest request, boolean ownerFlow) {
        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        UUID leaseTenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;

        if (ownerFlow) {
            if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized: lease does not belong to owner");
            }
        } else {
            if (leaseTenantUserId == null || !leaseTenantUserId.equals(tenantUserId)) {
                throw new RuntimeException("Unauthorized: lease does not belong to tenant");
            }
        }

        RentPayment payment = rentPaymentRepository
                .findByLease_LeaseIdAndMonthYear(request.leaseId(), request.monthYear())
                .orElseGet(RentPayment::new);

        validatePaymentWindow(lease, request.monthYear());

        payment.setLease(lease);
        payment.setMonthYear(request.monthYear());
        payment.setAmountExpected(request.amountExpected());
        payment.setAmountPaid(request.amountPaid() == null ? BigDecimal.ZERO : request.amountPaid());
        
        // Due date is always the 7th of the payment month
        LocalDate dueDate = calculateDueDate(request.monthYear());
        payment.setDueDate(dueDate);
        
        payment.setPaidDate(request.paidDate());
        payment.setPaymentMethod(RentPayment.PaymentMethod.valueOf(request.paymentMethod().name()));

        payment.setPaymentStatus(calculateStatus(payment.getAmountExpected(), payment.getAmountPaid(), payment.getDueDate()));

        RentPayment saved = rentPaymentRepository.save(payment);
        
        // Log activity
        String propertyName = lease.getProperty().getPropertyName();
        String unitNumber = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
        UUID actorId = ownerFlow ? ownerId : tenantUserId;
        if (request.amountPaid() != null && request.amountPaid().compareTo(BigDecimal.ZERO) > 0) {
            activityService.logPaymentReceived(actorId, propertyName, unitNumber,
                    request.monthYear(), request.amountPaid().doubleValue());
            if (leaseTenantUserId != null && !leaseTenantUserId.equals(actorId)) {
                activityService.logPaymentReceived(leaseTenantUserId, propertyName, unitNumber,
                        request.monthYear(), request.amountPaid().doubleValue());
            }
        } else {
            activityService.logPaymentUpdated(actorId, propertyName, unitNumber, request.monthYear());
            if (leaseTenantUserId != null && !leaseTenantUserId.equals(actorId)) {
                activityService.logPaymentUpdated(leaseTenantUserId, propertyName, unitNumber, request.monthYear());
            }
        }
        
        return saved;
    }

    // Calculate due date as 7th of the given month (format: "2026-02")
    private LocalDate calculateDueDate(String monthYear) {
        String[] parts = monthYear.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return LocalDate.of(year, month, 7);
    }

    private PaymentStatus calculateStatus(BigDecimal expected, BigDecimal paid, LocalDate dueDate) {

        if (expected == null) expected = BigDecimal.ZERO;
        if (paid == null) paid = BigDecimal.ZERO;

        // paid >= expected => PAID
        if (paid.compareTo(expected) >= 0 && expected.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PAID;
        }

        boolean isOverdue = dueDate != null && LocalDate.now().isAfter(dueDate);

        // paid > 0 but not full => PARTIAL (or OVERDUE if past due date)
        if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(expected) < 0) {
            return isOverdue ? PaymentStatus.OVERDUE : PaymentStatus.PARTIAL;
        }

        // paid == 0
        if (isOverdue) {
            return PaymentStatus.OVERDUE;
        }

        return PaymentStatus.PENDING;
    }

    // Called immediately after a new lease is created so the tenant sees the current month right away
    public void seedCurrentMonthForLease(Lease lease) {
        YearMonth currentMonth = YearMonth.now();
        // Don't seed if lease hasn't started yet this month
        if (lease.getStartDate() != null && currentMonth.isBefore(YearMonth.from(lease.getStartDate()))) {
            return;
        }
        // Don't seed if already past lease end date
        if (lease.getEndDate() != null && currentMonth.isAfter(YearMonth.from(lease.getEndDate()))) {
            return;
        }
        String monthYear = currentMonth.toString();
        boolean exists = rentPaymentRepository
                .findByLease_LeaseIdAndMonthYear(lease.getLeaseId(), monthYear)
                .isPresent();
        if (exists) return;

        RentPayment pending = new RentPayment();
        pending.setLease(lease);
        pending.setMonthYear(monthYear);
        pending.setAmountExpected(lease.getMonthlyRent() != null ? lease.getMonthlyRent() : BigDecimal.ZERO);
        pending.setAmountPaid(BigDecimal.ZERO);
        pending.setDueDate(LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 7));
        pending.setPaymentStatus(RentPayment.PaymentStatus.PENDING);
        rentPaymentRepository.save(pending);
    }

    // Scheduled task — runs on the 1st of each month to seed a Pending record for every active lease
    public void generateMonthlyPendingRecords() {
        YearMonth currentMonth = YearMonth.now();
        String monthYear = currentMonth.toString(); // "YYYY-MM"
        LocalDate dueDate = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 7);

        List<Lease> activeLeases = leaseRepository.findByLeaseStatus(LeaseStatus.ACTIVE);
        for (Lease lease : activeLeases) {
            // Skip if lease hasn't started yet
            if (lease.getStartDate() != null && currentMonth.isBefore(YearMonth.from(lease.getStartDate()))) {
                continue;
            }
            // Skip if current month is past lease end date
            if (lease.getEndDate() != null && currentMonth.isAfter(YearMonth.from(lease.getEndDate()))) {
                continue;
            }
            // Skip if record already exists for this lease + month
            boolean exists = rentPaymentRepository
                    .findByLease_LeaseIdAndMonthYear(lease.getLeaseId(), monthYear)
                    .isPresent();
            if (exists) continue;

            RentPayment pending = new RentPayment();
            pending.setLease(lease);
            pending.setMonthYear(monthYear);
            pending.setAmountExpected(lease.getMonthlyRent() != null ? lease.getMonthlyRent() : BigDecimal.ZERO);
            pending.setAmountPaid(BigDecimal.ZERO);
            pending.setDueDate(dueDate);
            pending.setPaymentStatus(RentPayment.PaymentStatus.PENDING);
            rentPaymentRepository.save(pending);
        }
    }

    // Scheduled task to update overdue payments - called daily
    public void updateOverduePayments() {
        List<RentPayment> payments = rentPaymentRepository.findAll();
        for (RentPayment payment : payments) {
            PaymentStatus newStatus = calculateStatus(
                payment.getAmountExpected(), 
                payment.getAmountPaid(), 
                payment.getDueDate()
            );
            if (payment.getPaymentStatus() != newStatus) {
                payment.setPaymentStatus(newStatus);
                rentPaymentRepository.save(payment);
            }
        }
    }

    public List<RentPayment> getLeasePayments(UUID ownerId, UUID leaseId) {

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        return rentPaymentRepository.findByLease_LeaseId(leaseId);
    }

    // Tenant read-only view
    public List<RentPayment> getTenantPayments(UUID tenantUserId) {
        return rentPaymentRepository.findByLease_Tenant_User_UserId(tenantUserId);
    }

    public RentPayment getPaymentById(UUID paymentId) {
        return rentPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public RentPayment markPaymentAsUnpaid(UUID ownerId, UUID paymentId) {
        RentPayment payment = rentPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Lease lease = payment.getLease();
        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        payment.setAmountPaid(BigDecimal.ZERO);
        payment.setPaidDate(null);
        payment.setPaymentMethod(null);
        payment.setPaymentStatus(calculateStatus(
                payment.getAmountExpected(),
                payment.getAmountPaid(),
                payment.getDueDate()
        ));

        RentPayment saved = rentPaymentRepository.save(payment);

        String propertyName = lease.getProperty().getPropertyName();
        String unitNumber = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
        UUID tenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;

        activityService.logPaymentUpdated(ownerId, propertyName, unitNumber, payment.getMonthYear());
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logPaymentUpdated(tenantUserId, propertyName, unitNumber, payment.getMonthYear());
        }

        return saved;
    }

    private void validatePaymentWindow(Lease lease, String monthYear) {
        YearMonth paymentMonth;
        try {
            paymentMonth = YearMonth.parse(monthYear);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid month format. Use YYYY-MM");
        }

        if (lease.getEndDate() != null) {
            YearMonth leaseEndMonth = YearMonth.from(lease.getEndDate());
            if (paymentMonth.isAfter(leaseEndMonth)) {
                throw new RuntimeException("Cannot record payment after lease end/termination date");
            }
        }

        if (lease.getLeaseStatus() != null && lease.getLeaseStatus().name().equals("TERMINATED") && lease.getEndDate() == null) {
            throw new RuntimeException("Cannot record payment for terminated lease");
        }
    }

    public void deletePayment(UUID ownerId, UUID paymentId) {
        RentPayment payment = rentPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Lease lease = payment.getLease();
        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }

        String monthYear = payment.getMonthYear();
        String propertyName = lease.getProperty().getPropertyName();
        String unitNumber = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
        UUID tenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;

        rentPaymentRepository.delete(payment);

        activityService.logPaymentUpdated(ownerId, propertyName, unitNumber, monthYear);
        if (tenantUserId != null && !tenantUserId.equals(ownerId)) {
            activityService.logPaymentUpdated(tenantUserId, propertyName, unitNumber, monthYear);
        }
    }
}
