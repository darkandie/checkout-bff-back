package com.carlosoliveira.checkout.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
        UUID id,
        String eventType,
        UUID aggregateId,
        String payload,
        Instant occurredAt
) {
}
