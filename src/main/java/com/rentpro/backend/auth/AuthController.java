package com.rentpro.backend.auth;

import com.rentpro.backend.auth.dto.LoginRequest;
import com.rentpro.backend.auth.dto.RegisterOwnerRequest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterOwnerRequest req) {
        try {
            authService.registerOwner(req);
            return ResponseEntity.status(201).body(Map.of("message", "OWNER_REGISTERED"));
        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

}
