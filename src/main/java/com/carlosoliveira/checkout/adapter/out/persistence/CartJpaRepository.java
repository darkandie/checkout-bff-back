package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.cart.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update CartJpaEntity c
               set c.status = :processing,
                   c.paymentAttemptId = :attemptId,
                   c.processingStartedAt = :startedAt,
                   c.updatedAt = :startedAt
             where c.id = :id
               and (c.status in :payable
                    or (c.status = :processing and c.processingStartedAt <= :staleBefore))
            """)
    int claimForProcessing(@Param("id") UUID id,
                           @Param("attemptId") UUID attemptId,
                           @Param("startedAt") Instant startedAt,
                           @Param("payable") Collection<CartStatus> payable,
                           @Param("processing") CartStatus processing,
                           @Param("staleBefore") Instant staleBefore);
}
