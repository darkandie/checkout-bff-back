package com.carlosoliveira.checkout.adapter.out.messaging;

import com.carlosoliveira.checkout.application.port.out.EventPublisherPort;
import com.carlosoliveira.checkout.application.port.out.OutboxMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@ConditionalOnProperty(prefix = "checkout.events", name = "publisher", havingValue = "rabbit")
public class RabbitEventPublisher implements EventPublisherPort {

    static final String AGGREGATE_ID_HEADER = "x-aggregate-id";
    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(OutboxMessage message) {
        Message amqpMessage = toAmqpMessage(message);
        rabbitTemplate.invoke(operations -> {
            operations.send(RabbitTopology.EXCHANGE, message.eventType(), amqpMessage);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
    }

    private static Message toAmqpMessage(OutboxMessage message) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(message.id().toString());
        properties.setType(message.eventType());
        properties.setTimestamp(Date.from(message.occurredAt()));
        properties.setHeader(AGGREGATE_ID_HEADER, message.aggregateId().toString());
        return new Message(message.payload().getBytes(StandardCharsets.UTF_8), properties);
    }
}
