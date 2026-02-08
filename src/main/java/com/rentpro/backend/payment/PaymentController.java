package com.rentpro.backend.payment;

import com.rentpro.backend.payment.dto.CreatePaymentRequest;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import com.rentpro.backend.payment.dto.LeasePaymentStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/payments")
@PreAuthorize("hasRole('OWNER')")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepo;

    public PaymentController(PaymentService paymentService, UserRepository userRepo) {
        this.paymentService = paymentService;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest req, Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Payment p = paymentService.createPayment(owner.getId(), req);
        return ResponseEntity.ok(PaymentResponse.from(p));
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<List<PaymentResponse>> list(@PathVariable Long leaseId, Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(
                paymentService.listLeasePayments(owner.getId(), leaseId)
                        .stream().map(PaymentResponse::from).toList());
    }

    @GetMapping("/lease/{leaseId}/status")
    public ResponseEntity<LeasePaymentStatusResponse> status(
            @PathVariable Long leaseId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(paymentService.getLeaseMonthlyStatus(owner.getId(), leaseId, from, to));
    }

}
