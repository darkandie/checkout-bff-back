package com.carlosoliveira.checkout.application.outbox;

import com.carlosoliveira.checkout.application.port.out.EventPublisherPort;
import com.carlosoliveira.checkout.application.port.out.OutboxMessage;
import com.carlosoliveira.checkout.application.port.out.OutboxPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.carlosoliveira.checkout.support.Fixtures.FIXED_CLOCK;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayServiceTest {

    private OutboxPort outbox;
    private EventPublisherPort publisher;
    private OutboxRelayService relay;

    @BeforeEach
    void setUp() {
        outbox = mock(OutboxPort.class);
        publisher = mock(EventPublisherPort.class);
        relay = new OutboxRelayService(outbox, publisher, new OutboxProperties(10), FIXED_CLOCK);
    }

    @Test
    @DisplayName("publishes pending events in order and marks each one as published")
    void publishesPendingEvents() {
        OutboxMessage first = message("payment.declined");
        OutboxMessage second = message("payment.approved");
        when(outbox.lockPending(10)).thenReturn(List.of(first, second));

        int published = relay.relayPending();

        assertThat(published).isEqualTo(2);
        verify(publisher).publish(first);
        verify(publisher).publish(second);
        verify(outbox).markPublished(first.id(), NOW);
        verify(outbox).markPublished(second.id(), NOW);
    }

    @Test
    @DisplayName("stops at the first failure, records it and keeps the remaining events pending")
    void stopsAtFirstFailure() {
        OutboxMessage failing = message("payment.approved");
        OutboxMessage next = message("payment.approved");
        when(outbox.lockPending(10)).thenReturn(List.of(failing, next));
        doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(failing);

        int published = relay.relayPending();

        assertThat(published).isEqualTo(0);
        verify(outbox).markFailed(failing.id(), "broker unavailable");
        verify(outbox, never()).markPublished(any(), any());
        verify(publisher, never()).publish(next);
    }

    @Test
    @DisplayName("does nothing when there are no pending events")
    void nothingPending() {
        when(outbox.lockPending(10)).thenReturn(List.of());

        assertThat(relay.relayPending()).isEqualTo(0);
        verify(publisher, never()).publish(any());
        verify(outbox, never()).markFailed(any(), any());
    }

    private static OutboxMessage message(String type) {
        return new OutboxMessage(UUID.randomUUID(), type, UUID.randomUUID(), "{}", NOW);
    }
}
