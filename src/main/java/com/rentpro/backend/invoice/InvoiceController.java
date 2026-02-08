package com.rentpro.backend.invoice;

import com.rentpro.backend.invoice.dto.GenerateInvoicesRequest;
import com.rentpro.backend.invoice.dto.InvoiceResponse;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@PreAuthorize("hasRole('OWNER')")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserRepository userRepo;

    public InvoiceController(InvoiceService invoiceService, UserRepository userRepo) {
        this.invoiceService = invoiceService;
        this.userRepo = userRepo;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<InvoiceResponse>> generate(@Valid @RequestBody GenerateInvoicesRequest req, Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Owner not found"));
        return ResponseEntity.ok(
                invoiceService.generate(owner.getId(), req).stream().map(InvoiceResponse::from).toList()
        );
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<List<InvoiceResponse>> list(@PathVariable Long leaseId, Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Owner not found"));
        return ResponseEntity.ok(
                invoiceService.listForLease(owner.getId(), leaseId).stream().map(InvoiceResponse::from).toList()
        );
    }
}
