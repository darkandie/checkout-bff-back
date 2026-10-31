package com.carlosoliveira.checkout.domain.event;

import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;

import java.time.Instant;
import java.util.UUID;

public record PaymentDeclined(
        UUID eventId,
        UUID cartId,
        Instant occurredAt,
        UUID attemptId,
        String transactionId,
        PaymentMethodType paymentMethod
) implements DomainEvent {
}
