package com.rentpro.backend.contract;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ContractPdfService {

    private final LeaseService leaseService;

    public ContractPdfService(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    public byte[] generateContractPdf(UUID leaseId) {
        Lease lease = leaseService.getLeaseById(leaseId);
        return generatePdf(lease);
    }

    private byte[] generatePdf(Lease lease) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("RESIDENTIAL LEASE AGREEMENT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Contract details
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

            String contractId = "CNT-" + lease.getLeaseId().toString().substring(0, 8).toUpperCase();
            document.add(new Paragraph("Contract Number: " + contractId, normalFont));
            document.add(new Paragraph("Generated Date: " + LocalDate.now().format(dateFormatter), normalFont));
            document.add(Chunk.NEWLINE);

            // Landlord
            document.add(new Paragraph("LANDLORD:", boldFont));
            String ownerName = lease.getProperty().getOwner() != null 
                ? lease.getProperty().getOwner().getFullName() 
                : "Property Owner";
            document.add(new Paragraph(ownerName, normalFont));
            document.add(Chunk.NEWLINE);

            // Tenant
            document.add(new Paragraph("TENANT:", boldFont));
            String tenantName = lease.getTenant() != null 
                ? lease.getTenant().getFullName() 
                : "Tenant Name";
            document.add(new Paragraph(tenantName, normalFont));
            document.add(Chunk.NEWLINE);

            // Property
            document.add(new Paragraph("PROPERTY:", boldFont));
            String propertyName = lease.getProperty().getPropertyName();
            String unitInfo = lease.getUnit() != null 
                ? propertyName + " - Unit " + lease.getUnit().getUnitNumber()
                : propertyName;
            document.add(new Paragraph(unitInfo, normalFont));
            String address = lease.getProperty().getAddress();
            if (address != null && !address.isEmpty()) {
                document.add(new Paragraph("Address: " + address, normalFont));
            }
            document.add(Chunk.NEWLINE);

            // Lease Terms
            document.add(new Paragraph("LEASE TERMS:", boldFont));
            document.add(new Paragraph("• Lease Start Date: " + 
                (lease.getStartDate() != null ? lease.getStartDate().format(dateFormatter) : "N/A"), normalFont));
            document.add(new Paragraph("• Lease End Date: " + 
                (lease.getEndDate() != null ? lease.getEndDate().format(dateFormatter) : "N/A"), normalFont));
            document.add(new Paragraph("• Monthly Rent: TZS " + 
                String.format("%,.0f", lease.getMonthlyRent().doubleValue()), normalFont));
            double depositAmount = lease.getSecurityDeposit() != null 
                ? lease.getSecurityDeposit().doubleValue() 
                : lease.getMonthlyRent().doubleValue() * 2;
            document.add(new Paragraph("• Security Deposit: TZS " + 
                String.format("%,.0f", depositAmount), normalFont));
            document.add(Chunk.NEWLINE);

            // Additional Terms
            document.add(new Paragraph("ADDITIONAL TERMS:", boldFont));
            document.add(new Paragraph("Standard lease terms and conditions apply. This agreement is subject to the laws and regulations of the jurisdiction in which the property is located.", normalFont));
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Signature lines
            document.add(new Paragraph("___________________________           ___________________________", normalFont));
            document.add(new Paragraph("Landlord Signature                           Tenant Signature", normalFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Date: ____________                           Date: ____________", normalFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }
}
