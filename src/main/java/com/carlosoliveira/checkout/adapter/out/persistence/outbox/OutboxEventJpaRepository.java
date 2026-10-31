package com.carlosoliveira.checkout.adapter.out.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = """
            select *
              from outbox_events
             where published_at is null
             order by occurred_at
             limit :limit
               for update skip locked
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockPending(@Param("limit") int limit);

    @Modifying
    @Query("update OutboxEventJpaEntity e set e.publishedAt = :publishedAt where e.id = :id")
    int markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("update OutboxEventJpaEntity e set e.attempts = e.attempts + 1, e.lastError = :error where e.id = :id")
    int markFailed(@Param("id") UUID id, @Param("error") String error);
}
