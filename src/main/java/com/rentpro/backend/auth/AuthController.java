package com.rentpro.backend.auth;

import com.rentpro.backend.auth.dto.*;
import com.rentpro.backend.security.JwtService;
import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder, JwtService jwt) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().toLowerCase().trim();

        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User u = User.builder()
                .fullName(req.fullName())
                .email(email)
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.OWNER)
                .createdAt(Instant.now())
                .build();

        userRepo.save(u);

        String token = jwt.generateToken(u.getEmail(), u.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, u.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String email = req.email().toLowerCase().trim();

        var userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        var user = userOpt.get();
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwt.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name()));
    }
}
