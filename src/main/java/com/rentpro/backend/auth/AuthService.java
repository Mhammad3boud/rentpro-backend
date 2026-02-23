package com.rentpro.backend.auth;

import com.rentpro.backend.auth.dto.AuthResponse;
import com.rentpro.backend.auth.dto.LoginRequest;
import com.rentpro.backend.auth.dto.RegisterOwnerRequest;
import com.rentpro.backend.security.JwtService;
import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        // Return only token and token type - no sensitive user data
        return new AuthResponse(
                token,
                "Bearer",
                null,  // userId removed - should be in JWT token
                null,  // email removed - should be in JWT token
                null); // role removed - should be in JWT token
    }

}
