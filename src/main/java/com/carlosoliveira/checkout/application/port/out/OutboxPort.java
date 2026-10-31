package com.carlosoliveira.checkout.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxPort {

    List<OutboxMessage> lockPending(int limit);

    void markPublished(UUID id, Instant publishedAt);

    void markFailed(UUID id, String error);
}
