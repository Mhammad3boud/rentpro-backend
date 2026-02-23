package com.rentpro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Security headers configuration implementing OWASP best practices
 * Adds security headers to prevent common web vulnerabilities
 */
@Configuration
public class SecurityHeadersConfig {

    @Value("${security.headers.xss-protection:true}")
    private boolean xssProtection;

    @Value("${security.headers.content-type-options:true}")
    private boolean contentTypeOptions;

    @Value("${security.headers.frame-options:DENY}")
    private String frameOptions;

    @Value("${security.headers.strict-transport-security:max-age=31536000; includeSubDomains}")
    private String strictTransportSecurity;

    /**
     * Configure security headers
     */
    public void configureHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                // XSS Protection header
                .xssProtection(xss -> xss.headerValue(
                    xssProtection ? XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK 
                                 : XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                
                // Content Type Options header
                .contentTypeOptions(contentTypeOptions ? contentType -> {} : contentType -> contentType.disable())
                
                // Frame Options header to prevent clickjacking
                .frameOptions(frame -> frame.deny())
                
                // Referrer Policy header
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                
                // Strict Transport Security header (HTTPS only)
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000) // 1 year
                        .requestMatcher(request -> request.isSecure())) // Only apply to HTTPS requests
                
                // Content Security Policy header
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data: https:; " +
                                "font-src 'self'; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'; " +
                                "upgrade-insecure-requests;"
                        ))
        );
    }
}
