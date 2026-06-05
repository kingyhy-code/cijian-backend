package com.cijian.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.PostConstruct;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class SwaggerAggregationConfig {

    private final SwaggerUiConfigParameters swaggerUiConfig;

    public SwaggerAggregationConfig(SwaggerUiConfigParameters swaggerUiConfig) {
        this.swaggerUiConfig = swaggerUiConfig;
    }

    @Bean
    public OpenAPI aggregatedOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("辞间 API 文档")
                        .description("辞间 · 短篇叙事创作平台 — 统一 API 入口 (8080)")
                        .version("1.0.0"));
    }

    @PostConstruct
    void initSwaggerUrls() {
        Set<SwaggerUrl> urls = new LinkedHashSet<>();
        String[][] services = {
                {"user-service", "用户服务 (8081)"},
                {"cijian-content", "内容服务 (8082)"},
                {"cijian-interaction", "互动服务 (8083)"},
                {"cijian-profile", "用户画像 (8084)"},
                {"cijian-ai", "AI 写作辅助 (8085)"},
                {"cijian-operation", "运营管理 (8086)"},
                {"cijian-search", "搜索服务 (8087)"},
        };
        for (String[] svc : services) {
            SwaggerUrl url = new SwaggerUrl();
            url.setName(svc[1]);
            url.setUrl("/v3/api-docs/" + svc[0]);
            urls.add(url);
        }
        swaggerUiConfig.setUrls(urls);
    }
}
