package com.flowpilot.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "FlowPilot AI API",
                version = "v1",
                description = "Backend API for the FlowPilot AI enterprise operations copilot."
        )
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter the JWT returned by the login or registration endpoint."
)
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
