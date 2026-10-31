package com.carlosoliveira.checkout.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface DomainEvent permits PaymentApproved, PaymentDeclined {

    UUID eventId();

    UUID cartId();

    Instant occurredAt();
}
