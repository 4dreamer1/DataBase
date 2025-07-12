package dev.usr.database.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

//AI 配置类
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.config")
public class AiConfig {
    private String apiKey;
    private String apiUrl;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String systemMessage;
}