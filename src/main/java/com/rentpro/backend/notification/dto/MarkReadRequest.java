package com.rentpro.backend.notification.dto;

import jakarta.validation.constraints.NotNull;

public record MarkReadRequest(@NotNull Boolean read) {
}
