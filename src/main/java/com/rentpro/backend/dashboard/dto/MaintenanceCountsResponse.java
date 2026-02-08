package com.rentpro.backend.dashboard.dto;

public record MaintenanceCountsResponse(
        long open,
        long inProgress,
        long resolved,
        long highPriorityOpen) {
}
