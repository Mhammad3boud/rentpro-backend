package com.rentpro.backend.auth;

import com.rentpro.backend.auth.dto.AuthResponse;
import com.rentpro.backend.auth.dto.LoginRequest;
import com.rentpro.backend.auth.dto.RegisterOwnerRequest;
import com.rentpro.backend.email.EmailService;
import com.rentpro.backend.security.JwtService;
import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PasswordResetTokenRepository resetTokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resetTokenRepository = resetTokenRepository;
        this.emailService = emailService;
    }

    public void registerOwner(RegisterOwnerRequest req) {
        String email = req.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setFullName(req.fullName());
        user.setPhone(req.phone());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.OWNER);
        user.setStatus(true);

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new IllegalArgumentException("User is disabled");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer", null, null, null);
    }

    public void forgotPassword(String email) {
        String normalized = email.toLowerCase().trim();

        // Always return success — never reveal whether the email is registered
        userRepository.findByEmail(normalized).ifPresent(user -> {
            resetTokenRepository.deleteAllByUser(user);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUser(user);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            resetTokenRepository.save(resetToken);

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
                System.out.println("[EMAIL] Password reset email sent to: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("[EMAIL ERROR] Failed to send password reset email to " + user.getEmail());
                System.err.println("[EMAIL ERROR] Cause: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                Throwable cause = e.getCause();
                while (cause != null) {
                    System.err.println("[EMAIL ERROR] Caused by: " + cause.getClass().getSimpleName() + " — " + cause.getMessage());
                    cause = cause.getCause();
                }
            }
        });
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findById(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }
}
