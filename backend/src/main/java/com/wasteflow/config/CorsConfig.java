package com.wasteflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;

@Configuration
public class CorsConfig {

    @Bean
    WebMvcConfigurer corsConfigurer(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
    URI frontendUri = URI.create(frontendUrl);
    String host = frontendUri.getHost();
    String alternateHost = "localhost".equalsIgnoreCase(host) ? "127.0.0.1"
        : "127.0.0.1".equals(host) ? "localhost" : null;
    String[] allowedOrigins = alternateHost == null
        ? new String[]{frontendUrl}
        : new String[]{frontendUrl, frontendUri.getScheme() + "://" + alternateHost
            + (frontendUri.getPort() == -1 ? "" : ":" + frontendUri.getPort())};

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}