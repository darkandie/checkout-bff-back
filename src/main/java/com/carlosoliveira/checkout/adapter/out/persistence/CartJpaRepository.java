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
               set c.status = :target, c.updatedAt = :now
             where c.id = :id
               and c.status in :allowed
            """)
    int transitionStatus(@Param("id") UUID id,
                         @Param("allowed") Collection<CartStatus> allowed,
                         @Param("target") CartStatus target,
                         @Param("now") Instant now);
}
