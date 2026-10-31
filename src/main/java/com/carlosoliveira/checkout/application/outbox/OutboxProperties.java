package com.carlosoliveira.checkout.application.outbox;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "checkout.outbox")
public record OutboxProperties(@Positive int batchSize) {
}
