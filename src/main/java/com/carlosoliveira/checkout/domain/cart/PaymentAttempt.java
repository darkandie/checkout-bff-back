package com.carlosoliveira.checkout.domain.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentAttempt(UUID id, Instant startedAt) {

    public PaymentAttempt {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
    }

    public static PaymentAttempt start(Instant now) {
        return new PaymentAttempt(UUID.randomUUID(), now);
    }

    public PaymentAttempt renew(Instant now) {
        return new PaymentAttempt(id, now);
    }

    public boolean isStaleAt(Instant now, Duration timeout) {
        return !now.isBefore(startedAt.plus(timeout));
    }
}
