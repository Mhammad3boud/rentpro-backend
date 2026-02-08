package com.rentpro.backend.payment.allocation;

import com.rentpro.backend.payment.allocation.dto.AllocatePaymentRequest;
import com.rentpro.backend.payment.allocation.dto.PaymentAllocationResponse;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@PreAuthorize("hasRole('OWNER')")
public class PaymentAllocationController {

    private final PaymentAllocationService service;
    private final UserRepository userRepo;

    public PaymentAllocationController(PaymentAllocationService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    @PostMapping("/{paymentId}/allocate")
    public ResponseEntity<?> allocate(
            @PathVariable Long paymentId,
            @Valid @RequestBody AllocatePaymentRequest req,
            Authentication auth
    ) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(service.allocate(owner.getId(), paymentId, req).stream().map(PaymentAllocationResponse::from).toList());
    }
}
