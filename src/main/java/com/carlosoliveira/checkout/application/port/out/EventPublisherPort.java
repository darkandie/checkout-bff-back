package com.carlosoliveira.checkout.application.port.out;

public interface EventPublisherPort {

    void publish(OutboxMessage message);
}
