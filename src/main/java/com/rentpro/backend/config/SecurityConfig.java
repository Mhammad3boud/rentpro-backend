package com.rentpro.backend.config;

import com.rentpro.backend.filter.RateLimitFilter;
import com.rentpro.backend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityHeadersConfig securityHeadersConfig;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter, SecurityHeadersConfig securityHeadersConfig) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.securityHeadersConfig = securityHeadersConfig;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .requestMatchers("/api/app-config/**").permitAll()
                        .requestMatchers("/predictions/**").permitAll()
                        .requestMatchers("/uploads/**", "/api/uploads/**").permitAll()
                        .requestMatchers("/api/tenants/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/api/properties/**").hasRole("OWNER")
                        .requestMatchers("/api/leases/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/payments/my-payments").hasRole("TENANT")
                        .requestMatchers("/payments/leases/**").hasRole("OWNER")
                        .requestMatchers("/payments/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/dashboard/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/maintenance/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/api/users/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/api/notifications/**").hasAnyRole("OWNER", "TENANT")
                        .requestMatchers("/api/activities/**").hasAnyRole("OWNER", "TENANT")
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Apply security headers
        securityHeadersConfig.configureHeaders(http);

        return http.build();
    }
}
