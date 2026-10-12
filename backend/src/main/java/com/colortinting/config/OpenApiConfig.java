package com.colortinting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI colorTintingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Color Tinting Machine API")
                .version("v1")
                .description("""
                        Local control API for a single color-tinting machine.
                        ESP32 paths, telemetry fields, and command semantics in the adapter are provisional
                        until the hardware developer confirms them. Command acceptance is not command completion.
                        """));
    }
}
