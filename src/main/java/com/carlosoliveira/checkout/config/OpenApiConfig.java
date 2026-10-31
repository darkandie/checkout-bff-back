package com.carlosoliveira.checkout.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI checkoutOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Checkout BFF")
                .version("v1")
                .description("Backend for Frontend do checkout de assinaturas: pré-checkout e checkout."));
    }

    @Bean
    GroupedOpenApi checkoutApi() {
        return GroupedOpenApi.builder()
                .group("checkout")
                .pathsToMatch("/operations/**")
                .build();
    }

    @Bean
    GroupedOpenApi mockServicesApi() {
        return GroupedOpenApi.builder()
                .group("mock-services")
                .pathsToMatch("/mocks/**")
                .build();
    }
}
