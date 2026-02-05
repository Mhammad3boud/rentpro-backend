package com.rentpro.backend.lease.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EndLeaseRequest(
        @NotNull LocalDate endDate
) {}
