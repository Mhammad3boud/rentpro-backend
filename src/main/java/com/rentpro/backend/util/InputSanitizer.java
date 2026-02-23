package com.rentpro.backend.util;

import org.springframework.web.util.HtmlUtils;
import java.util.regex.Pattern;

/**
 * Input sanitization utility following OWASP best practices
 * Provides methods to sanitize and validate user inputs
 */
public class InputSanitizer {

    // Common XSS patterns
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    // SQL injection patterns
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        Pattern.compile("(?i)(union|select|insert|update|delete|drop|alter|exec|execute)\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(--|#|/\\*|\\*/|;|')", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(or|and)\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(or|and)\\s+'[^']*'\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Sanitize input to prevent XSS attacks
     * @param input User input string
     * @return Sanitized string
     */
    public static String sanitizeForXss(String input) {
        if (input == null) {
            return null;
        }

        // HTML escape first
        String sanitized = HtmlUtils.htmlEscape(input);

        // Remove known XSS patterns
        for (Pattern pattern : XSS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }

        return sanitized.trim();
    }

    /**
     * Sanitize input to prevent SQL injection
     * @param input User input string
     * @return Sanitized string
     */
    public static String sanitizeForSql(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input;

        // Remove known SQL injection patterns
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }

        return sanitized.trim();
    }

    /**
     * Comprehensive sanitization for both XSS and SQL injection
     * @param input User input string
     * @return Fully sanitized string
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        // Apply both XSS and SQL injection sanitization
        String sanitized = sanitizeForXss(input);
        sanitized = sanitizeForSql(sanitized);

        return sanitized;
    }

    /**
     * Validate email format with additional security checks
     * @param email Email address to validate
     * @return true if email is valid and safe
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Basic email format check
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(emailRegex)) {
            return false;
        }

        // Check for suspicious patterns
        String sanitized = sanitize(email);
        return !sanitized.contains("<") && !sanitized.contains(">") && !sanitized.contains("javascript:");
    }

    /**
     * Validate phone number format
     * @param phone Phone number to validate
     * @return true if phone number is valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Remove common formatting characters
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Check if it's a valid international or local number
        return cleanPhone.matches("^[+]?[1-9]\\d{1,14}$");
    }

    /**
     * Sanitize filename to prevent directory traversal
     * @param filename Original filename
     * @return Sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return null;
        }

        // Remove path traversal characters
        String sanitized = filename.replaceAll("[/\\\\:*?\"<>|]", "_");
        
        // Remove leading dots and slashes
        sanitized = sanitized.replaceAll("^[./\\\\]+", "");
        
        // Limit filename length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized.trim();
    }

    /**
     * Validate and sanitize search queries
     * @param query Search query string
     * @return Sanitized query
     */
    public static String sanitizeSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        String sanitized = sanitize(query);
        
        // Limit search query length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }
}
