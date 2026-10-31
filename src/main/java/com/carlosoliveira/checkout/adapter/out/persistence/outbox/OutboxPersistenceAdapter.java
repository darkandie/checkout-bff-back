package com.carlosoliveira.checkout.adapter.out.persistence.outbox;

import com.carlosoliveira.checkout.application.port.out.OutboxMessage;
import com.carlosoliveira.checkout.application.port.out.OutboxPort;
import com.carlosoliveira.checkout.domain.event.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxPersistenceAdapter implements OutboxPort {

    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;

    public OutboxPersistenceAdapter(OutboxEventJpaRepository repository, OutboxEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(List<DomainEvent> events) {
        if (!events.isEmpty()) {
            repository.saveAll(events.stream().map(mapper::toEntity).toList());
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<OutboxMessage> lockPending(int limit) {
        return repository.lockPending(limit).stream()
                .map(entity -> new OutboxMessage(
                        entity.getId(),
                        entity.getEventType(),
                        entity.getAggregateId(),
                        entity.getPayload(),
                        entity.getOccurredAt()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markPublished(UUID id, Instant publishedAt) {
        repository.markPublished(id, publishedAt);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markFailed(UUID id, String error) {
        repository.markFailed(id, error);
    }
}
