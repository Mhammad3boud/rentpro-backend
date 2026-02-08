package com.rentpro.backend.payment;

import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.payment.dto.CreatePaymentRequest;
import com.rentpro.backend.payment.dto.LeasePaymentStatusResponse;
import com.rentpro.backend.payment.dto.MonthlyPaymentStatusItem;
import com.rentpro.backend.unit.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final LeaseRepository leaseRepo;
    private final UnitRepository unitRepo;

    public PaymentService(PaymentRepository paymentRepo, LeaseRepository leaseRepo, UnitRepository unitRepo) {
        this.paymentRepo = paymentRepo;
        this.leaseRepo = leaseRepo;
        this.unitRepo = unitRepo;
    }

    @Transactional
    public Payment createPayment(Long ownerId, CreatePaymentRequest req) {

        Lease lease = leaseRepo.findById(req.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        // ownership check via unit -> property -> owner
        Long unitId = lease.getUnit().getId();
        unitRepo.findByIdAndProperty_Owner_Id(unitId, ownerId)
                .orElseThrow(() -> new RuntimeException("Lease not found or not yours"));

        Payment payment = Payment.builder()
                .lease(lease)
                .amount(req.amount())
                .paidAt(req.paidAt())
                .method(req.method())
                .reference(req.reference())
                .notes(req.notes())
                .createdAt(Instant.now())
                .build();

        return paymentRepo.save(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> listLeasePayments(Long ownerId, Long leaseId) {

        Lease lease = leaseRepo.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        unitRepo.findByIdAndProperty_Owner_Id(lease.getUnit().getId(), ownerId)
                .orElseThrow(() -> new RuntimeException("Lease not found or not yours"));

        return paymentRepo.findAllByLease_IdOrderByPaidAtDesc(leaseId);
    }

    @Transactional(readOnly = true)
    public LeasePaymentStatusResponse getLeaseMonthlyStatus(Long ownerId, Long leaseId, YearMonth from, YearMonth to) {

        Lease lease = leaseRepo.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        unitRepo.findByIdAndProperty_Owner_Id(lease.getUnit().getId(), ownerId)
                .orElseThrow(() -> new RuntimeException("Lease not found or not yours"));

        if (to.isBefore(from)) {
            throw new RuntimeException("to must be >= from");
        }

        BigDecimal expected = lease.getRentAmount() == null ? BigDecimal.ZERO : lease.getRentAmount();

        // Convert YearMonth range into Instants (UTC)
        LocalDate start = from.atDay(1);
        LocalDate end = to.atEndOfMonth();
        Instant fromInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = end.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        List<Payment> payments = paymentRepo.findAllByLease_IdAndPaidAtBetweenOrderByPaidAtAsc(
                leaseId, fromInstant, toInstant);

        // Sum paid per month (UTC)
        Map<YearMonth, BigDecimal> sums = new HashMap<>();
        for (Payment p : payments) {
            YearMonth ym = YearMonth.from(p.getPaidAt().atZone(ZoneOffset.UTC));
            sums.put(ym, sums.getOrDefault(ym, BigDecimal.ZERO).add(p.getAmount()));
        }

        // Lease period bounds (assuming Lease uses LocalDate startDate/endDate)
        YearMonth leaseStartYm = YearMonth.from(lease.getStartDate());
        YearMonth leaseEndYm = (lease.getEndDate() == null) ? null : YearMonth.from(lease.getEndDate());

        // “Today” month (UTC)
        YearMonth nowYm = YearMonth.now(ZoneOffset.UTC);

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        int paidCount = 0;
        int partialCount = 0;
        int unpaidCount = 0;

        List<MonthlyPaymentStatusItem> items = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {

            boolean afterStart = !ym.isBefore(leaseStartYm);
            boolean beforeEnd = (leaseEndYm == null) || !ym.isAfter(leaseEndYm);
            boolean inLeasePeriod = afterStart && beforeEnd;

            BigDecimal expectedForMonth = inLeasePeriod ? expected : BigDecimal.ZERO;
            BigDecimal paid = sums.getOrDefault(ym, BigDecimal.ZERO);

            String status;
            if (!inLeasePeriod) {
                status = "N/A";
            } else if (paid.compareTo(expected) >= 0) {
                status = "PAID";
                paidCount++;
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                status = "PARTIAL";
                partialCount++;
            } else {
                // paid == 0 and in lease period
                status = ym.isBefore(nowYm) ? "OVERDUE" : "UNPAID";
                unpaidCount++;
            }

            totalExpected = totalExpected.add(expectedForMonth);
            totalPaid = totalPaid.add(paid);

            // IMPORTANT: this assumes MonthlyPaymentStatusItem has (month, expectedRent,
            // paidTotal, status, inLeasePeriod)
            items.add(new MonthlyPaymentStatusItem(
                    ym.format(fmt),
                    expectedForMonth,
                    paid,
                    status,
                    inLeasePeriod));
        }

        BigDecimal outstanding = totalExpected.subtract(totalPaid);

        return new LeasePaymentStatusResponse(
                leaseId,
                expected,
                items,
                totalExpected,
                totalPaid,
                outstanding,
                paidCount,
                partialCount,
                unpaidCount);
    }
}
