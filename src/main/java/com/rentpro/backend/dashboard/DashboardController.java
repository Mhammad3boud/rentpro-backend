package com.rentpro.backend.dashboard;

import com.rentpro.backend.dashboard.dto.OwnerDashboardResponse;
import com.rentpro.backend.dashboard.dto.TenantDashboardResponse;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/owner")
    public OwnerDashboardResponse ownerDashboard(Authentication auth) {
        try {
            JwtUserContext ctx = (JwtUserContext) auth.getDetails();
            UUID ownerUserId = UUID.fromString(ctx.userId());
            return dashboardService.getOwnerDashboard(ownerUserId);
        } catch (Exception e) {
            System.err.println("Dashboard error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load owner dashboard: " + e.getMessage(), e);
        }
    }

    @GetMapping("/tenant")
    public TenantDashboardResponse tenantDashboard(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantUserId = UUID.fromString(ctx.userId());
        return dashboardService.getTenantDashboard(tenantUserId);
    }
}
