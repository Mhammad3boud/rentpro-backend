package com.rentpro.backend.contract;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class ContractPdfService {

    private final LeaseService leaseService;
    private final ContractTemplateService templateService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy");

    public ContractPdfService(LeaseService leaseService, ContractTemplateService templateService) {
        this.leaseService = leaseService;
        this.templateService = templateService;
    }

    public byte[] generateContractPdf(UUID leaseId) {
        return generateContractPdf(leaseId, null);
    }

    public byte[] generateContractPdf(UUID leaseId, UUID templateId) {
        Lease lease = leaseService.getLeaseById(leaseId);
        UUID ownerId = lease.getProperty().getOwner().getUserId();

        ContractTemplate template = templateId != null
                ? templateService.getTemplateById(templateId)
                : templateService.getDefaultTemplate(ownerId);

        String content = buildContent(template.getTemplateContent(), lease);
        return renderToPdf(content);
    }

    private String buildContent(String templateContent, Lease lease) {
        String ownerName = lease.getProperty().getOwner() != null
                ? lease.getProperty().getOwner().getFullName() : "Property Owner";
        String ownerAddress = lease.getProperty().getAddress() != null
                ? lease.getProperty().getAddress() : "";
        String tenantName = lease.getTenant() != null ? lease.getTenant().getFullName() : "";
        String propertyName = lease.getProperty().getPropertyName();
        String propertyAddress = lease.getProperty().getAddress() != null ? lease.getProperty().getAddress() : "";
        String unitNumber = lease.getUnit() != null ? "Unit " + lease.getUnit().getUnitNumber() : "";
        String startDate = lease.getStartDate() != null ? lease.getStartDate().format(DATE_FMT) : "";
        String endDate = lease.getEndDate() != null ? lease.getEndDate().format(DATE_FMT) : "Open-ended";
        String monthlyRent = lease.getMonthlyRent() != null
                ? String.format("MYR %,.2f", lease.getMonthlyRent().doubleValue()) : "";
        double depositAmt = lease.getSecurityDeposit() != null
                ? lease.getSecurityDeposit().doubleValue()
                : (lease.getMonthlyRent() != null ? lease.getMonthlyRent().doubleValue() * 2 : 0);
        String securityDeposit = String.format("MYR %,.2f", depositAmt);
        String contractId = "CNT-" + lease.getLeaseId().toString().substring(0, 8).toUpperCase();
        String generatedDate = LocalDate.now().format(DATE_FMT);

        Map<String, String> tokens = Map.ofEntries(
                Map.entry("{{CONTRACT_ID}}", contractId),
                Map.entry("{{GENERATED_DATE}}", generatedDate),
                Map.entry("{{LANDLORD_NAME}}", ownerName),
                Map.entry("{{LANDLORD_ADDRESS}}", ownerAddress),
                Map.entry("{{TENANT_NAME}}", tenantName),
                Map.entry("{{PROPERTY_NAME}}", propertyName),
                Map.entry("{{PROPERTY_ADDRESS}}", propertyAddress),
                Map.entry("{{UNIT_NUMBER}}", unitNumber),
                Map.entry("{{START_DATE}}", startDate),
                Map.entry("{{END_DATE}}", endDate),
                Map.entry("{{MONTHLY_RENT}}", monthlyRent),
                Map.entry("{{SECURITY_DEPOSIT}}", securityDeposit),
                Map.entry("{{UTILITIES_TERMS}}", "As per local utility provider"),
                Map.entry("{{PET_POLICY}}", "No pets allowed unless agreed in writing"),
                Map.entry("{{SPECIAL_TERMS}}", "")
        );

        String result = templateContent;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private byte[] renderToPdf(String content) {
        // Strip HTML tags if this is Quill HTML output, preserve line structure
        String text = stripHtml(content);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 60, 60, 60, 60);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    document.add(Chunk.NEWLINE);
                    continue;
                }
                // Heuristic: all-uppercase short lines are headings
                boolean isHeading = trimmed.length() < 60
                        && trimmed.equals(trimmed.toUpperCase())
                        && trimmed.matches(".*[A-Z].*");
                Font font = isHeading ? headingFont : (trimmed.startsWith("•") ? smallFont : normalFont);
                Paragraph para = new Paragraph(trimmed, font);
                para.setSpacingAfter(isHeading ? 6 : 2);
                document.add(para);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        // Convert block-closing tags to newlines before stripping
        String result = html
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("[ \\t]+", " ")         // collapse spaces/tabs
                .replaceAll("\n{3,}", "\n\n");        // max 2 consecutive blank lines
        return result.trim();
    }
}
