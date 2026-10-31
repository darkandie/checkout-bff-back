package com.carlosoliveira.checkout.adapter.out.persistence.outbox;

import com.carlosoliveira.checkout.domain.event.DomainEvent;
import com.carlosoliveira.checkout.domain.event.PaymentApproved;
import com.carlosoliveira.checkout.domain.event.PaymentDeclined;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxEventMapper {

    public static final String AGGREGATE_TYPE = "cart";
    public static final String PAYMENT_APPROVED = "payment.approved";
    public static final String PAYMENT_DECLINED = "payment.declined";

    private final ObjectMapper objectMapper;

    public OutboxEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEventJpaEntity toEntity(DomainEvent event) {
        return new OutboxEventJpaEntity(
                event.eventId(),
                AGGREGATE_TYPE,
                event.cartId(),
                eventType(event),
                serialize(payload(event)),
                event.occurredAt()
        );
    }

    static String eventType(DomainEvent event) {
        return switch (event) {
            case PaymentApproved approved -> PAYMENT_APPROVED;
            case PaymentDeclined declined -> PAYMENT_DECLINED;
        };
    }

    private static Object payload(DomainEvent event) {
        return switch (event) {
            case PaymentApproved approved -> new PaymentApprovedPayload(
                    approved.eventId(),
                    approved.cartId(),
                    approved.attemptId(),
                    approved.transactionId(),
                    approved.paymentMethod(),
                    approved.amount(),
                    approved.currency(),
                    approved.productSlugs(),
                    approved.occurredAt());
            case PaymentDeclined declined -> new PaymentDeclinedPayload(
                    declined.eventId(),
                    declined.cartId(),
                    declined.attemptId(),
                    declined.transactionId(),
                    declined.paymentMethod(),
                    declined.occurredAt());
        };
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize outbox payload", e);
        }
    }

    record PaymentApprovedPayload(
            UUID eventId,
            UUID cartId,
            UUID attemptId,
            String transactionId,
            PaymentMethodType paymentMethod,
            BigDecimal amount,
            String currency,
            List<String> products,
            Instant occurredAt
    ) {
    }

    record PaymentDeclinedPayload(
            UUID eventId,
            UUID cartId,
            UUID attemptId,
            String transactionId,
            PaymentMethodType paymentMethod,
            Instant occurredAt
    ) {
    }
}
