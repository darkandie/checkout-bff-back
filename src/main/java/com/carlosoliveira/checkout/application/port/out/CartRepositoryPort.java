package com.carlosoliveira.checkout.application.port.out;

import com.carlosoliveira.checkout.domain.cart.Cart;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findById(UUID id);

    boolean claimForProcessing(UUID id, Instant now);
}
