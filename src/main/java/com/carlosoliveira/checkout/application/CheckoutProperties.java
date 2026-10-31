package com.carlosoliveira.checkout.application;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "checkout")
public record CheckoutProperties(@NotNull Duration cartTtl) {
}
