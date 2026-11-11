package com.sharebill.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Chỉ cho phép các địa chỉ trong CORS_ORIGINS gọi API từ trình duyệt.
 * App web chạy cùng địa chỉ với backend nên không cần CORS; danh sách này chỉ để phát triển bằng Expo web.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origins;

    public CorsConfig(@Value("${sharebill.cors-origins:}") String origins) {
        this.origins = origins.isBlank() ? new String[0] : origins.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", PasswordFilter.HEADER);
    }
}
