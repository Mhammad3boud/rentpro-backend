package com.rentpro.backend.lease.dto;

import com.rentpro.backend.lease.LeaseChecklistItem;

import java.time.LocalDate;
import java.util.List;

public record CheckOutLeaseRequest(
        LocalDate checkOutDate,
        String reason,
        String notes,
        List<LeaseChecklistItem> checklist
) {
}
