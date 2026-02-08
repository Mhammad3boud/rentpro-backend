package com.rentpro.backend.dashboard;

import com.rentpro.backend.dashboard.dto.MaintenanceCountsResponse;
import com.rentpro.backend.dashboard.dto.OwnerDashboardSummaryResponse;
import com.rentpro.backend.dashboard.dto.OverdueInvoiceItem;
import com.rentpro.backend.dashboard.dto.OwnerDashboardResponse;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('OWNER')")
public class DashboardController {

    private final DashboardService service;
    private final UserRepository userRepo;

    public DashboardController(DashboardService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    @GetMapping("/owner/summary")
    public ResponseEntity<OwnerDashboardSummaryResponse> summary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Owner not found"));
        return ResponseEntity.ok(service.summary(owner.getId(), from, to));
    }

    @GetMapping("/owner/attention")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<OverdueInvoiceItem>> attention(Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Owner not found"));
        return ResponseEntity.ok(service.overdue(owner.getId()));
    }

    @GetMapping("/owner/maintenance")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<MaintenanceCountsResponse> maintenance(Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Owner not found"));
        return ResponseEntity.ok(service.maintenanceCounts(owner.getId()));
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerDashboardResponse> fullDashboard(
            @RequestParam YearMonth from,
            @RequestParam YearMonth to,
            Authentication auth) {
        User owner = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        return ResponseEntity.ok(
                service.fullDashboard(owner.getId(), from, to));
    }

}
