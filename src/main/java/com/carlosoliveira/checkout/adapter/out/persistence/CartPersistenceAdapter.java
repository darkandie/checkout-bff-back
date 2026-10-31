package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.application.port.out.CartRepositoryPort;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartPersistenceAdapter implements CartRepositoryPort {

    private final CartJpaRepository repository;
    private final Clock clock;

    public CartPersistenceAdapter(CartJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Cart save(Cart cart) {
        CartJpaEntity saved = repository.save(CartJpaMapper.toEntity(cart, Instant.now(clock)));
        return CartJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findById(UUID id) {
        return repository.findById(id).map(CartJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean claimForProcessing(UUID id, Instant now) {
        return repository.transitionStatus(id, CartStatus.payable(), CartStatus.PROCESSING, now) == 1;
    }
}
