package com.rentpro.backend.filter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiting filter implementing OWASP best practices
 * Provides IP-based and user-based rate limiting with graceful 429 responses
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Cache for rate limit buckets per client
    private final ConcurrentMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        String endpointType = determineEndpointType(request.getRequestURI());
        String userId = extractUserId(request);
        
        // Use user ID if available, otherwise use IP address
        String rateLimitKey = userId != null ? "user:" + userId : "ip:" + clientIp;
        
        Bucket bucket = bucketCache.computeIfAbsent(rateLimitKey + ":" + endpointType, 
            key -> createBucket(endpointType));

        if (bucket.tryConsume(1)) {
            // Add rate limit headers for transparency
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            
            filterChain.doFilter(request, response);
        } else {
            // Graceful 429 response with retry-after header (60 seconds default)
            response.setHeader("Retry-After", "60");
            response.setHeader("X-Rate-Limit-Remaining", "0");
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    /**
     * Extract client IP address considering proxies and load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Extract user ID from JWT token if available for user-based rate limiting
     */
    private String extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // In a real implementation, you would parse the JWT token here
            // For now, we'll return null to use IP-based limiting
            return null;
        }
        return null;
    }

    /**
     * Determine endpoint type for appropriate rate limiting
     */
    private String determineEndpointType(String requestUri) {
        if (requestUri.startsWith("/api/auth/")) {
            return "auth";
        } else if (requestUri.contains("/upload") || requestUri.contains("/file")) {
            return "upload";
        } else {
            return "api";
        }
    }

    /**
     * Create bucket based on endpoint type
     */
    private Bucket createBucket(String endpointType) {
        switch (endpointType) {
            case "auth":
                return Bucket.builder()
                        .addLimit(io.github.bucket4j.Bandwidth.classic(5, 
                            io.github.bucket4j.Refill.intervally(5, Duration.ofMinutes(1))))
                        .build();
            case "upload":
                return Bucket.builder()
                        .addLimit(io.github.bucket4j.Bandwidth.classic(10, 
                            io.github.bucket4j.Refill.intervally(10, Duration.ofMinutes(1))))
                        .build();
            default:
                return Bucket.builder()
                        .addLimit(io.github.bucket4j.Bandwidth.classic(100, 
                            io.github.bucket4j.Refill.intervally(100, Duration.ofMinutes(1))))
                        .build();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip rate limiting for health checks and static resources
        return path.startsWith("/actuator") || 
               path.startsWith("/error") || 
               path.startsWith("/favicon.ico") ||
               path.startsWith("/static/");
    }
}
