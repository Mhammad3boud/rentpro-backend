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

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

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
