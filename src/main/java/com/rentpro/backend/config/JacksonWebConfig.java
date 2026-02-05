package com.rentpro.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class JacksonWebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public JacksonWebConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // remove any existing Jackson converters (if any)
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);

        // add Jackson FIRST so it wins
        converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));

        // (optional) print what converters you have
        System.out.println("=== HTTP Message Converters ===");
        converters.forEach(c -> System.out.println(" - " + c.getClass().getName()));
    }
}
