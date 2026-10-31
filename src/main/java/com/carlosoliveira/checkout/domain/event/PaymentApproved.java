package com.carlosoliveira.checkout.domain.event;

import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentApproved(
        UUID eventId,
        UUID cartId,
        Instant occurredAt,
        UUID attemptId,
        String transactionId,
        PaymentMethodType paymentMethod,
        BigDecimal amount,
        String currency,
        List<String> productSlugs
) implements DomainEvent {

    public PaymentApproved {
        productSlugs = List.copyOf(productSlugs);
    }
}
