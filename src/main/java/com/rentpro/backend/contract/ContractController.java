package com.rentpro.backend.contract;

import com.rentpro.backend.contract.dto.CreateTemplateRequest;
import com.rentpro.backend.contract.dto.UpdateTemplateRequest;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractPdfService contractPdfService;
    private final ContractTemplateService templateService;

    public ContractController(ContractPdfService contractPdfService,
                              ContractTemplateService templateService) {
        this.contractPdfService = contractPdfService;
        this.templateService = templateService;
    }

    /**
     * Generate and download a PDF contract for a given lease
     */
    @GetMapping("/{leaseId}/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(@PathVariable UUID leaseId) {
        byte[] pdfBytes = contractPdfService.generateContractPdf(leaseId);
        
        String filename = "contract-" + leaseId.toString().substring(0, 8) + ".pdf";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ========== Template Endpoints ==========

    /**
     * Get all templates for current user
     */
    @GetMapping("/templates")
    public List<ContractTemplate> getTemplates(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return templateService.getTemplatesForOwner(ownerId);
    }

    /**
     * Get a specific template by ID
     */
    @GetMapping("/templates/{templateId}")
    public ContractTemplate getTemplate(@PathVariable UUID templateId) {
        return templateService.getTemplateById(templateId);
    }

    /**
     * Create a new template
     */
    @PostMapping("/templates")
    public ContractTemplate createTemplate(Authentication auth,
                                           @RequestBody CreateTemplateRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return templateService.createTemplate(ownerId, request.name(), request.content());
    }

    /**
     * Update an existing template
     */
    @PutMapping("/templates/{templateId}")
    public ContractTemplate updateTemplate(@PathVariable UUID templateId,
                                           @RequestBody UpdateTemplateRequest request) {
        return templateService.updateTemplate(templateId, request.name(), request.content());
    }

    /**
     * Delete a template
     */
    @DeleteMapping("/templates/{templateId}")
    public void deleteTemplate(@PathVariable UUID templateId) {
        templateService.deleteTemplate(templateId);
    }

    /**
     * Set a template as default
     */
    @PutMapping("/templates/{templateId}/default")
    public ContractTemplate setDefaultTemplate(Authentication auth,
                                               @PathVariable UUID templateId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return templateService.setAsDefault(ownerId, templateId);
    }
}
