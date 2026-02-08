package com.rentpro.backend.dashboard.dto;

import java.util.List;

public record OwnerDashboardResponse(
        OwnerDashboardSummaryResponse summary,
        List<OverdueInvoiceItem> attentionInvoices,
        MaintenanceCountsResponse maintenance
) {}
