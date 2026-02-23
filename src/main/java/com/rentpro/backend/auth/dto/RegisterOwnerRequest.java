package com.rentpro.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO with strict validation following OWASP best practices
 */
public record RegisterOwnerRequest(
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']*$", message = "Full name can only contain letters, spaces, hyphens, and apostrophes")
    String fullName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must be less than 254 characters")
    String email,
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    String phone,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]*$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    String password
) {}
