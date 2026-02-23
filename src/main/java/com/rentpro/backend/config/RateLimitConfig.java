package com.rentpro.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class RateLimitConfig {

    // Rate limits following OWASP recommendations
    // Authentication endpoints: 5 requests per minute (strict)
    private static final Bandwidth AUTH_BANDWIDTH = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
    
    // General API endpoints: 100 requests per minute
    private static final Bandwidth API_BANDWIDTH = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    
    // File upload endpoints: 10 requests per minute
    private static final Bandwidth UPLOAD_BANDWIDTH = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));

    // In-memory bucket storage for rate limiting
    private final ConcurrentMap<String, Bucket> cache = new ConcurrentHashMap<>();

    @Bean
    public Bucket authBucket() {
        return Bucket.builder()
                .addLimit(AUTH_BANDWIDTH)
                .build();
    }

    @Bean
    public Bucket apiBucket() {
        return Bucket.builder()
                .addLimit(API_BANDWIDTH)
                .build();
    }

    @Bean
    public Bucket uploadBucket() {
        return Bucket.builder()
                .addLimit(UPLOAD_BANDWIDTH)
                .build();
    }

    /**
     * Resolve rate limit bucket by IP address and endpoint type
     * @param key Client IP address or unique identifier
     * @param type Type of endpoint (auth, api, upload)
     * @return Bucket for rate limiting
     */
    public Bucket resolveBucket(String key, String type) {
        return cache.computeIfAbsent(key + ":" + type, k -> {
            switch (type) {
                case "auth":
                    return Bucket.builder()
                            .addLimit(AUTH_BANDWIDTH)
                            .build();
                case "upload":
                    return Bucket.builder()
                            .addLimit(UPLOAD_BANDWIDTH)
                            .build();
                default:
                    return Bucket.builder()
                            .addLimit(API_BANDWIDTH)
                            .build();
            }
        });
    }
}
