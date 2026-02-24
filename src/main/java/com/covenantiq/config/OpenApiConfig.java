package com.covenantiq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI covenantIqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CovenantIQ API")
                        .description("Commercial Loan Risk Surveillance APIs")
                        .version("v1"));
    }
}
