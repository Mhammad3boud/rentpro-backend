package com.rentpro.backend.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void safeSend(String to, String subject, String body) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) return; // mail not configured, skip

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception ignored) {
        }
    }
}
