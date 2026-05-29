package com.rentpro.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${app.frontend.base-url:http://localhost:8104}")
    private String frontendBaseUrl;

    private static final String FROM = "RentPro <noreply@rentpro.dev>";
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private void send(String to, String subject, String text) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.err.println("[EMAIL] RESEND_API_KEY not set — skipping email to " + to);
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> body = Map.of(
            "from", FROM,
            "to", List.of(to),
            "subject", subject,
            "text", text
        );

        restTemplate.exchange(RESEND_URL, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    // ── Password reset ──────────────────────────────────────────────────

    public void sendPasswordResetEmail(String toEmail, String token) {
        String base = frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;
        String resetLink = base + "/reset-password?token=" + token;

        send(toEmail,
            "RentPro – Password Reset Request",
            "Hello,\n\n" +
            "We received a request to reset your RentPro password.\n\n" +
            "Click the link below to set a new password (expires in 5 minutes):\n\n" +
            resetLink + "\n\n" +
            "If you didn't request this, you can safely ignore this email.\n\n" +
            "— The RentPro Team"
        );
    }

    // ── Tenant welcome ──────────────────────────────────────────────────

    public void sendTenantWelcomeEmail(String toEmail, String tenantName, String ownerName,
                                       String propertyName, String tempPassword) {
        String base = frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;

        send(toEmail,
            "Welcome to RentPro — Your Tenant Account is Ready",
            "Hello " + tenantName + ",\n\n" +
            ownerName + " has added you as a tenant on RentPro for " + propertyName + ".\n\n" +
            "Your login details:\n" +
            "  Email    : " + toEmail + "\n" +
            "  Password : " + tempPassword + "\n\n" +
            "Sign in at: " + base + "\n\n" +
            "Please change your password after your first login.\n\n" +
            "— The RentPro Team"
        );
    }

    // ── Payment ─────────────────────────────────────────────────────────

    public void sendPaymentReceivedEmail(String toEmail, String tenantName,
                                          String propertyName, String monthYear, double amount, String currency) {
        send(toEmail,
            "RentPro – Payment Received for " + monthYear,
            "Hello " + tenantName + ",\n\n" +
            "Your rent payment has been recorded.\n\n" +
            "  Property : " + propertyName + "\n" +
            "  Month    : " + monthYear + "\n" +
            "  Amount   : " + currency + " " + String.format("%.2f", amount) + "\n\n" +
            "Thank you for your payment.\n\n" +
            "— The RentPro Team"
        );
    }

    public void sendPaymentOverdueEmail(String toEmail, String tenantName,
                                         String propertyName, String monthYear, double amountDue, String currency) {
        send(toEmail,
            "RentPro – Rent Overdue: " + monthYear,
            "Hello " + tenantName + ",\n\n" +
            "This is a reminder that your rent is overdue.\n\n" +
            "  Property   : " + propertyName + "\n" +
            "  Month      : " + monthYear + "\n" +
            "  Amount Due : " + currency + " " + String.format("%.2f", amountDue) + "\n\n" +
            "Please arrange payment as soon as possible.\n\n" +
            "— The RentPro Team"
        );
    }

    // ── Maintenance ─────────────────────────────────────────────────────

    public void sendMaintenanceCreatedEmail(String toEmail, String ownerName,
                                             String requestTitle, String propertyName,
                                             String submittedBy) {
        send(toEmail,
            "RentPro – New Maintenance Request: " + requestTitle,
            "Hello " + ownerName + ",\n\n" +
            "A new maintenance request has been submitted.\n\n" +
            "  Property   : " + propertyName + "\n" +
            "  Title      : " + requestTitle + "\n" +
            "  Submitted by: " + submittedBy + "\n\n" +
            "Log in to RentPro to review and action the request.\n\n" +
            "— The RentPro Team"
        );
    }

    public void sendMaintenanceStatusEmail(String toEmail, String tenantName,
                                            String requestTitle, String newStatus) {
        send(toEmail,
            "RentPro – Maintenance Update: " + requestTitle,
            "Hello " + tenantName + ",\n\n" +
            "Your maintenance request has been updated.\n\n" +
            "  Title  : " + requestTitle + "\n" +
            "  Status : " + newStatus + "\n\n" +
            "Log in to RentPro for more details.\n\n" +
            "— The RentPro Team"
        );
    }

    // ── Lease ────────────────────────────────────────────────────────────

    public void sendLeaseCreatedEmail(String toEmail, String tenantName,
                                       String propertyName, String startDate,
                                       String endDate, double monthlyRent, String currency) {
        send(toEmail,
            "RentPro – Your Lease Details",
            "Hello " + tenantName + ",\n\n" +
            "Your lease has been set up on RentPro.\n\n" +
            "  Property     : " + propertyName + "\n" +
            "  Start Date   : " + startDate + "\n" +
            "  End Date     : " + endDate + "\n" +
            "  Monthly Rent : " + currency + " " + String.format("%.2f", monthlyRent) + "\n\n" +
            "Log in to RentPro to view your full lease details.\n\n" +
            "— The RentPro Team"
        );
    }

    // ── Admin ────────────────────────────────────────────────────────────

    public void sendCrashAlert(String toEmail, String errorType, String endpoint, String message) {
        send(toEmail,
            "[RentPro] Server Error — " + errorType,
            "A server error occurred in RentPro.\n\n" +
            "Error type : " + errorType + "\n" +
            "Endpoint   : " + endpoint + "\n" +
            "Message    : " + (message != null ? message : "(none)") + "\n" +
            "Time       : " + java.time.LocalDateTime.now() + "\n\n" +
            "Check the server logs for the full stack trace."
        );
    }
}
