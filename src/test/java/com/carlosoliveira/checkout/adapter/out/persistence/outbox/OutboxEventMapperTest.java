package com.carlosoliveira.checkout.adapter.out.persistence.outbox;

import com.carlosoliveira.checkout.domain.event.PaymentApproved;
import com.carlosoliveira.checkout.domain.event.PaymentDeclined;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventMapperTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private final OutboxEventMapper mapper = new OutboxEventMapper(objectMapper);

    @Test
    @DisplayName("maps PaymentApproved to an outbox row with a JSON payload")
    void mapsPaymentApproved() throws Exception {
        PaymentApproved event = new PaymentApproved(UUID.randomUUID(), UUID.randomUUID(), NOW, UUID.randomUUID(),
                "tx-1", PaymentMethodType.PIX, new BigDecimal("39.90"), "BRL", List.of("premium"));

        OutboxEventJpaEntity entity = mapper.toEntity(event);
        JsonNode payload = objectMapper.readTree(entity.getPayload());

        assertThat(entity.getId()).isEqualTo(event.eventId());
        assertThat(entity.getAggregateType()).isEqualTo("cart");
        assertThat(entity.getAggregateId()).isEqualTo(event.cartId());
        assertThat(entity.getEventType()).isEqualTo("payment.approved");
        assertThat(entity.getOccurredAt()).isEqualTo(NOW);
        assertThat(entity.getPublishedAt()).isNull();
        assertThat(payload.get("cartId").asText()).isEqualTo(event.cartId().toString());
        assertThat(payload.get("attemptId").asText()).isEqualTo(event.attemptId().toString());
        assertThat(payload.get("transactionId").asText()).isEqualTo("tx-1");
        assertThat(payload.get("paymentMethod").asText()).isEqualTo("PIX");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("39.90");
        assertThat(payload.get("currency").asText()).isEqualTo("BRL");
        assertThat(payload.get("products").get(0).asText()).isEqualTo("premium");
        assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-10-31T12:00:00Z");
    }

    @Test
    @DisplayName("maps PaymentDeclined to its own event type")
    void mapsPaymentDeclined() throws Exception {
        PaymentDeclined event = new PaymentDeclined(UUID.randomUUID(), UUID.randomUUID(), NOW, UUID.randomUUID(),
                "tx-2", PaymentMethodType.CREDIT_CARD);

        OutboxEventJpaEntity entity = mapper.toEntity(event);
        JsonNode payload = objectMapper.readTree(entity.getPayload());

        assertThat(entity.getEventType()).isEqualTo("payment.declined");
        assertThat(payload.get("paymentMethod").asText()).isEqualTo("CREDIT_CARD");
        assertThat(payload.has("amount")).isFalse();
    }
}
