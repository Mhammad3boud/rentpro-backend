package com.rentpro.backend.invoice;

import com.rentpro.backend.invoice.dto.GenerateInvoicesRequest;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.payment.allocation.PaymentAllocationRepository;
import com.rentpro.backend.unit.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final LeaseRepository leaseRepo;
    private final UnitRepository unitRepo;
    private final PaymentAllocationRepository allocRepo;

    public InvoiceService(
            InvoiceRepository invoiceRepo,
            LeaseRepository leaseRepo,
            UnitRepository unitRepo,
            PaymentAllocationRepository allocRepo) {
        this.invoiceRepo = invoiceRepo;
        this.leaseRepo = leaseRepo;
        this.unitRepo = unitRepo;
        this.allocRepo = allocRepo;
    }

    @Transactional
    public List<Invoice> generate(Long ownerId, GenerateInvoicesRequest req) {

        Lease lease = leaseRepo.findById(req.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        unitRepo.findByIdAndProperty_Owner_Id(lease.getUnit().getId(), ownerId)
                .orElseThrow(() -> new RuntimeException("Lease not found or not yours"));

        YearMonth from = YearMonth.parse(req.from());
        YearMonth to = YearMonth.parse(req.to());
        if (to.isBefore(from))
            throw new RuntimeException("to must be >= from");

        int dueDay = (req.dueDay() == null) ? 5 : req.dueDay();
        if (dueDay < 1 || dueDay > 28)
            throw new RuntimeException("dueDay must be 1..28");

        BigDecimal amount = lease.getRentAmount() == null ? BigDecimal.ZERO : lease.getRentAmount();

        List<Invoice> created = new ArrayList<>();
        for (YearMonth ym = from; !ym.isAfter(to); ym = ym.plusMonths(1)) {

            if (invoiceRepo.findByLease_IdAndPeriodYearAndPeriodMonth(
                    lease.getId(), ym.getYear(), ym.getMonthValue()).isPresent())
                continue;

            LocalDate dueDate = LocalDate.of(ym.getYear(), ym.getMonthValue(), dueDay);

            Invoice inv = Invoice.builder()
                    .lease(lease)
                    .periodYear(ym.getYear())
                    .periodMonth(ym.getMonthValue())
                    .amount(amount)
                    .dueDate(dueDate)
                    .status("UNPAID")
                    .createdAt(Instant.now())
                    .build();

            created.add(invoiceRepo.save(inv));
        }

        return created;
    }

    @Transactional(readOnly = true)
    public List<Invoice> listForLease(Long ownerId, Long leaseId) {

        Lease lease = leaseRepo.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        unitRepo.findByIdAndProperty_Owner_Id(lease.getUnit().getId(), ownerId)
                .orElseThrow(() -> new RuntimeException("Lease not found or not yours"));

        List<Invoice> invoices = invoiceRepo.findAllByLease_IdOrderByPeriodYearDescPeriodMonthDesc(leaseId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (Invoice inv : invoices) {
            BigDecimal paidTotal = allocRepo.sumAppliedByInvoiceId(inv.getId());

            if (paidTotal.compareTo(inv.getAmount()) >= 0)
                inv.setStatus("PAID");
            else if (paidTotal.compareTo(BigDecimal.ZERO) > 0)
                inv.setStatus("PARTIAL");
            else if (inv.getDueDate().isBefore(today))
                inv.setStatus("OVERDUE");
            else
                inv.setStatus("UNPAID");
        }

        return invoices;
    }
}
