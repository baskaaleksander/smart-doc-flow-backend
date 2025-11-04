package com.baskaaleksander.smartdocflowbackend.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Smart Doc Flow API",
                version = "1.0.0",
                description = "API documentation for document ingestion and AI review workflow",
                contact = @Contact(
                        name = "Aleksander Baska",
                        url = "https://baskaaleksander.com",
                        email = "me@baskaaleksander.com"
                ),
                license = @License(
                        name = "MIT",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        description = "Main production server",
                        url = "https://smartdocflowapi.baskaaleksander.com/api"
                )
        }
)
@Configuration
public class OpenApiConfig {
}