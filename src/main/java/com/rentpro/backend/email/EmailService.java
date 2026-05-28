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

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${app.frontend.base-url:http://localhost:8104}")
    private String frontendBaseUrl;

    private static final String FROM = "RentPro <onboarding@resend.dev>";
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private void send(String to, String subject, String text) {
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
