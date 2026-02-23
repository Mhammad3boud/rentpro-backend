package com.rentpro.backend.payment;

import com.rentpro.backend.activity.ActivityService;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
// import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPayment.PaymentStatus;
import com.rentpro.backend.payment.dto.CreateOrUpdatePaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        // Owner must own the property in that lease
        if (!lease.getProperty().getOwner().getUserId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: lease does not belong to owner");
        }

        RentPayment payment = rentPaymentRepository
                .findByLease_LeaseIdAndMonthYear(request.leaseId(), request.monthYear())
                .orElseGet(RentPayment::new);

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
        if (request.amountPaid() != null && request.amountPaid().compareTo(BigDecimal.ZERO) > 0) {
            activityService.logPaymentReceived(ownerId, propertyName, unitNumber, 
                    request.monthYear(), request.amountPaid().doubleValue());
        } else {
            activityService.logPaymentUpdated(ownerId, propertyName, unitNumber, request.monthYear());
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
}
