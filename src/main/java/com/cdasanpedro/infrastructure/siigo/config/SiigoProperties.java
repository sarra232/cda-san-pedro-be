package com.cdasanpedro.infrastructure.siigo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "siigo")
public class SiigoProperties {

    private String apiUrl = "https://api.siigo.com/v1";
    private String username;
    private String accessKey;
    private String environment = "SANDBOX";
    private Integer documentTypeId = 24581;
    private Integer sellerId;
    private String costCenterCode;

    public boolean isConfigured() {
        return username != null && !username.isBlank() &&
               accessKey != null && !accessKey.isBlank();
    }

    public boolean isSandbox() {
        return "SANDBOX".equalsIgnoreCase(environment);
    }
}
