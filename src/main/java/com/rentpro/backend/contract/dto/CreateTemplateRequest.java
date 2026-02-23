package com.rentpro.backend.contract.dto;

public record CreateTemplateRequest(
    String name,
    String content
) {}
