package com.carlosoliveira.checkout.adapter.out.messaging;

import com.carlosoliveira.checkout.application.port.out.EventPublisherPort;
import com.carlosoliveira.checkout.application.port.out.OutboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "checkout.events", name = "publisher", havingValue = "log")
public class LoggingEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(OutboxMessage message) {
        log.info("Domain event id={} type={} aggregateId={} payload={}",
                message.id(), message.eventType(), message.aggregateId(), message.payload());
    }
}
