package com.rentpro.backend.contract.dto;

public record UpdateTemplateRequest(
    String name,
    String content
) {}
