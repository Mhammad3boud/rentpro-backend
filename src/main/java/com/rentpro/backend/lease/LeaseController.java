package com.rentpro.backend.lease;

import com.rentpro.backend.contract.ContractPdfService;
import com.rentpro.backend.lease.dto.AssignTenantRequest;
import com.rentpro.backend.lease.dto.CheckInLeaseRequest;
import com.rentpro.backend.lease.dto.CheckOutLeaseRequest;
import com.rentpro.backend.lease.dto.CreateLeaseRequest;
import com.rentpro.backend.lease.dto.RenewLeaseRequest;
import com.rentpro.backend.lease.dto.TerminateLeaseRequest;
import com.rentpro.backend.lease.dto.UpdateLeaseRequest;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leases")
public class LeaseController {

    private final LeaseService leaseService;
    private final ContractPdfService contractPdfService;

    public LeaseController(LeaseService leaseService, ContractPdfService contractPdfService) {
        this.leaseService = leaseService;
        this.contractPdfService = contractPdfService;
    }

    @PostMapping
    public Lease createLease(Authentication auth, @RequestBody CreateLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.createLease(UUID.fromString(ctx.userId()), request);
    }

    @GetMapping("/my-leases")
    public List<Lease> getCurrentUserLeases(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.getOwnerLeases(UUID.fromString(ctx.userId()));
    }

    @GetMapping("/tenant-leases")
    public List<Lease> getTenantLeases(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.getTenantLeases(UUID.fromString(ctx.userId()));
    }

    @GetMapping("/{leaseId}")
    public Lease getLeaseById(@PathVariable UUID leaseId) {
        return leaseService.getLeaseById(leaseId);
    }

    @PutMapping("/{leaseId}")
    public Lease updateLease(@PathVariable UUID leaseId, @RequestBody UpdateLeaseRequest request) {
        return leaseService.updateLease(leaseId, request);
    }

    @GetMapping("/tenant/{tenantId}")
    public Lease getLeaseByTenantId(@PathVariable UUID tenantId) {
        return leaseService.getLeaseByTenantId(tenantId);
    }

    @PostMapping("/assign")
    public Lease assignTenant(Authentication auth, @RequestBody AssignTenantRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.assignTenant(UUID.fromString(ctx.userId()), request);
    }

    // Renew an active lease — terminates the old one and creates a new lease + generates PDF
    @PostMapping("/{leaseId}/renew")
    public ResponseEntity<byte[]> renewLease(Authentication auth,
                                             @PathVariable UUID leaseId,
                                             @RequestBody RenewLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        Lease renewed = leaseService.renewLease(ownerId, leaseId, request);

        byte[] pdf = contractPdfService.generateContractPdf(renewed.getLeaseId(), request.templateId());
        String filename = "renewal-" + renewed.getLeaseId().toString().substring(0, 8) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        headers.set("X-New-Lease-Id", renewed.getLeaseId().toString());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PutMapping("/{leaseId}/terminate")
    public Lease terminateLease(Authentication auth,
                                @PathVariable UUID leaseId,
                                @RequestBody(required = false) TerminateLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.terminateLease(UUID.fromString(ctx.userId()), ctx.role(), leaseId, request);
    }

    @PutMapping("/{leaseId}/check-in")
    public Lease checkInLease(Authentication auth,
                              @PathVariable UUID leaseId,
                              @RequestBody(required = false) CheckInLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.checkInLease(UUID.fromString(ctx.userId()), ctx.role(), leaseId, request);
    }

    @PutMapping("/{leaseId}/check-out")
    public Lease checkOutLease(Authentication auth,
                               @PathVariable UUID leaseId,
                               @RequestBody CheckOutLeaseRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return leaseService.checkOutLease(UUID.fromString(ctx.userId()), ctx.role(), leaseId, request);
    }

    @DeleteMapping("/{leaseId}")
    public void deleteLease(@PathVariable UUID leaseId) {
        leaseService.deleteLease(leaseId);
    }
}
