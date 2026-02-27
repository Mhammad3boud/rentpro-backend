package com.rentpro.backend.user;

import com.rentpro.backend.tenant.Tenant;
import com.rentpro.backend.tenant.TenantRepository;
import com.rentpro.backend.user.dto.ChangePasswordRequest;
import com.rentpro.backend.user.dto.UpdateProfileRequest;
import com.rentpro.backend.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String UPLOAD_DIR = "uploads/profiles/";

    public UserController(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fullName = user.getFullName();
        String phone = user.getPhone();
        String address = user.getAddress();
        if (isBlank(fullName) || isBlank(phone) || isBlank(address)) {
            Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
            if (tenant != null) {
                if (isBlank(fullName)) fullName = tenant.getFullName();
                if (isBlank(phone)) phone = tenant.getPhone();
                if (isBlank(address)) address = tenant.getAddress();
            }
        }

        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                fullName,
                phone,
                address,
                user.getProfilePicture(),
                user.getNotificationEmail(),
                user.getNotificationPush(),
                user.getThemePreference()
        );
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.notificationEmail() != null) {
            user.setNotificationEmail(request.notificationEmail());
        }
        if (request.notificationPush() != null) {
            user.setNotificationPush(request.notificationPush());
        }
        if (request.themePreference() != null) {
            user.setThemePreference(request.themePreference());
        }

        userRepository.save(user);

        return ResponseEntity.ok(new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getProfilePicture(),
                user.getNotificationEmail(),
                user.getNotificationPush(),
                user.getThemePreference()
        ));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/me/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = user.getUserId().toString() + "_" + System.currentTimeMillis() + extension;
            
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());

            // Delete old profile picture if exists
            String oldPicture = user.getProfilePicture();
            if (oldPicture != null && !oldPicture.isEmpty()) {
                try {
                    Path oldPath = Paths.get(oldPicture.replace("/api/uploads/profiles/", UPLOAD_DIR));
                    Files.deleteIfExists(oldPath);
                } catch (Exception e) {
                    // Ignore delete errors
                }
            }

            // Save new profile picture path
            String pictureUrl = "/api/uploads/profiles/" + filename;
            user.setProfilePicture(pictureUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile picture updated successfully",
                    "profilePicture", pictureUrl
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldPicture = user.getProfilePicture();
        if (oldPicture != null && !oldPicture.isEmpty()) {
            try {
                Path oldPath = Paths.get(oldPicture.replace("/api/uploads/profiles/", UPLOAD_DIR));
                Files.deleteIfExists(oldPath);
            } catch (Exception e) {
                // Ignore delete errors
            }
        }

        user.setProfilePicture(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile picture removed"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
