package com.carlosoliveira.checkout.application.outbox;

import com.carlosoliveira.checkout.application.port.out.EventPublisherPort;
import com.carlosoliveira.checkout.application.port.out.OutboxMessage;
import com.carlosoliveira.checkout.application.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final OutboxPort outbox;
    private final EventPublisherPort publisher;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxRelayService(OutboxPort outbox,
                              EventPublisherPort publisher,
                              OutboxProperties properties,
                              Clock clock) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int relayPending() {
        List<OutboxMessage> pending = outbox.lockPending(properties.batchSize());
        int published = 0;
        for (OutboxMessage message : pending) {
            try {
                publisher.publish(message);
                outbox.markPublished(message.id(), Instant.now(clock));
                published++;
            } catch (RuntimeException e) {
                log.warn("Failed to publish outbox event id={} type={}", message.id(), message.eventType(), e);
                outbox.markFailed(message.id(), truncate(e.getMessage()));
                break;
            }
        }
        if (published > 0) {
            log.info("Published {} outbox event(s)", published);
        }
        return published;
    }

    private static String truncate(String error) {
        if (error == null) {
            return "unknown error";
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
