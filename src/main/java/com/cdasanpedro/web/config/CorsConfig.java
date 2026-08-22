package com.cdasanpedro.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:80,http://localhost,https://*.ngrok-free.app,https://*.ngrok.app,https://*.ngrok-free.dev,https://*.vercel.app}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                config.addAllowedOriginPattern(trimmed);
            }
        }
        // Permitir comodín para desarrollo y túneles si se habilita
        config.addAllowedOriginPattern("https://*.ngrok*");
        config.addAllowedOriginPattern("http://*.ngrok*");
        config.addAllowedOriginPattern("https://*.loca.lt");
        config.addAllowedOriginPattern("http://192.168.*.*:*");
        config.addAllowedOriginPattern("http://10.*.*.*:*");
        config.addAllowedOriginPattern("http://localhost:*");
        
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
