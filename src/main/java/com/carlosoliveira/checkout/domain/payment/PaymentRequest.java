package com.carlosoliveira.checkout.domain.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID cartId,
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        CardData card
) {
}
