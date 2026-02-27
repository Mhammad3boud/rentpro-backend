package com.rentpro.backend.lease.dto;

import java.time.LocalDate;

public record TerminateLeaseRequest(
        String reason,
        String notes,
        LocalDate terminationDate
) {}
