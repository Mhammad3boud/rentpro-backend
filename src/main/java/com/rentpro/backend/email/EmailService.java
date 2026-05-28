package com.rentpro.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.base-url:http://localhost:8104}")
    private String frontendBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCrashAlert(String toEmail, String errorType, String endpoint, String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(toEmail);
        msg.setSubject("[RentPro] Server Error — " + errorType);
        msg.setText(
            "A server error occurred in RentPro.\n\n" +
            "Error type : " + errorType + "\n" +
            "Endpoint   : " + endpoint + "\n" +
            "Message    : " + (message != null ? message : "(none)") + "\n" +
            "Time       : " + java.time.LocalDateTime.now() + "\n\n" +
            "Check the server logs for the full stack trace."
        );
        mailSender.send(msg);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        String resetLink = base + "/reset-password?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(toEmail);
        msg.setSubject("RentPro – Password Reset Request");
        msg.setText(
            "Hello,\n\n" +
            "We received a request to reset your RentPro password.\n\n" +
            "Click the link below to set a new password (expires in 1 hour):\n\n" +
            resetLink + "\n\n" +
            "If you didn't request this, you can safely ignore this email.\n\n" +
            "— The RentPro Team"
        );

        mailSender.send(msg);
    }
}
