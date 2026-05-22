package com.rentpro.backend.admin;

import com.rentpro.backend.user.Role;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AdminBootstrapController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * One-time endpoint to create the first SUPER_ADMIN account.
     * Returns 409 on every subsequent call — cannot be used to create a second admin.
     */
    @PostMapping("/admin-setup")
    public ResponseEntity<String> setupAdmin(@Valid @RequestBody AdminSetupRequest req) {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Admin account already exists");
        }

        User admin = new User();
        admin.setFullName(req.fullName());
        admin.setEmail(req.email().toLowerCase().trim());
        admin.setPassword(passwordEncoder.encode(req.password()));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setStatus(true);
        userRepository.save(admin);

        return ResponseEntity.status(HttpStatus.CREATED).body("Admin account created successfully");
    }

    public record AdminSetupRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password
    ) {}
}
