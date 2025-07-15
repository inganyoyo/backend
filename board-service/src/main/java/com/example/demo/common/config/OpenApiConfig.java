package com.example.demo.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 설정
 * Swagger UI를 통해 API 문서를 제공한다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Board Service API")
                        .description("게시판 서비스 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("개발팀")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("개발 서버"),
                        new Server().url("https://api.example.com").description("운영 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요.")));
    }
}
