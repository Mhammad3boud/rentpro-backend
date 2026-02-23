package com.rentpro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/maintenance}")
    private String uploadDir;

    @Value("${app.upload.profiles.dir:uploads/profiles}")
    private String profileUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve maintenance photos from the uploads directory
        registry.addResourceHandler("/uploads/maintenance/**")
                .addResourceLocations("file:" + uploadDir + "/");
        
        // Serve profile pictures from the uploads/profiles directory
        registry.addResourceHandler("/api/uploads/profiles/**")
                .addResourceLocations("file:" + profileUploadDir + "/");
    }
}
