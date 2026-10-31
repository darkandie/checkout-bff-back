package com.carlosoliveira.checkout.adapter.out.messaging;

import com.carlosoliveira.checkout.application.outbox.OutboxRelayService;
import com.carlosoliveira.checkout.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OutboxRelayIT extends IntegrationTestSupport {

    private static final long RECEIVE_TIMEOUT_MS = 5_000;

    @Autowired
    private OutboxRelayService relay;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void drainPreviousEvents() {
        int published;
        do {
            published = relay.relayPending();
        } while (published > 0);
        amqpAdmin.purgeQueue(RabbitTopology.PAYMENT_APPROVED_QUEUE, false);
        amqpAdmin.purgeQueue(RabbitTopology.PAYMENT_DECLINED_QUEUE, false);
    }

    @Test
    @DisplayName("stores PaymentApproved in the outbox and publishes it to RabbitMQ when relayed")
    void publishesApprovedPayment() throws Exception {
        UUID cartId = preCheckout();
        checkout(cartId, "{\"name\": \"Pix\"}");

        assertThat(publishedAt(cartId, "payment.approved")).isNull();

        assertThat(relay.relayPending()).isEqualTo(1);

        assertThat(publishedAt(cartId, "payment.approved")).isNotNull();
        Message message = rabbitTemplate.receive(RabbitTopology.PAYMENT_APPROVED_QUEUE, RECEIVE_TIMEOUT_MS);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getType()).isEqualTo("payment.approved");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(message.getMessageProperties().getHeaders().get(RabbitEventPublisher.AGGREGATE_ID_HEADER))
                .isEqualTo(cartId.toString());

        JsonNode payload = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(payload.get("eventId").asText());
        assertThat(payload.get("cartId").asText()).isEqualTo(cartId.toString());
        assertThat(payload.get("paymentMethod").asText()).isEqualTo("PIX");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("39.90");
        assertThat(payload.get("products").get(0).asText()).isEqualTo("premium");
    }

    @Test
    @DisplayName("publishes PaymentDeclined to the declined queue")
    void publishesDeclinedPayment() throws Exception {
        UUID cartId = preCheckout();
        checkout(cartId, """
                {"name": "Cartão de Crédito",
                 "card": {"holderName": "Ana Souza", "number": "4000000000000002", "expiry": "%s", "cvv": "123"}}
                """.formatted(futureExpiry()));

        relay.relayPending();

        Message message = rabbitTemplate.receive(RabbitTopology.PAYMENT_DECLINED_QUEUE, RECEIVE_TIMEOUT_MS);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getType()).isEqualTo("payment.declined");
        assertThat(rabbitTemplate.receive(RabbitTopology.PAYMENT_APPROVED_QUEUE, 500)).isNull();
    }

    @Test
    @DisplayName("does not publish an event twice")
    void publishesOnce() throws Exception {
        UUID cartId = preCheckout();
        checkout(cartId, "{\"name\": \"Pix\"}");

        relay.relayPending();

        assertThat(relay.relayPending()).isEqualTo(0);
        assertThat(rabbitTemplate.receive(RabbitTopology.PAYMENT_APPROVED_QUEUE, RECEIVE_TIMEOUT_MS)).isNotNull();
        assertThat(rabbitTemplate.receive(RabbitTopology.PAYMENT_APPROVED_QUEUE, 500)).isNull();
    }

    private Timestamp publishedAt(UUID cartId, String eventType) {
        return jdbcTemplate.queryForObject(
                "select published_at from outbox_events where aggregate_id = ? and event_type = ?",
                Timestamp.class, cartId, eventType);
    }

    private UUID preCheckout() throws Exception {
        String body = mockMvc.perform(post("/operations/pre-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"products\": [{\"slug\": \"premium\"}]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.cartId"));
    }

    private void checkout(UUID cartId, String paymentMethod) throws Exception {
        mockMvc.perform(post("/operations/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartId\": \"%s\", \"paymentMethod\": %s}".formatted(cartId, paymentMethod)));
    }

    private static String futureExpiry() {
        return YearMonth.now(ZoneOffset.UTC).plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy"));
    }
}
