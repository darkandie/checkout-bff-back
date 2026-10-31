package com.carlosoliveira.checkout.domain.payment;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentRequest(
        UUID cartId,
        UUID idempotencyKey,
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        CardData card
) {

    public PaymentRequest {
        Objects.requireNonNull(cartId, "cartId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(method, "method must not be null");
    }
}
