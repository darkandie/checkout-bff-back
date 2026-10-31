package com.carlosoliveira.checkout.adapter.out.persistence.outbox;

import com.carlosoliveira.checkout.domain.event.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPersistenceAdapter {

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
}
