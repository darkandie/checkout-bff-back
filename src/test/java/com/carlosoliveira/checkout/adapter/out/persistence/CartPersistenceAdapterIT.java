package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import com.carlosoliveira.checkout.domain.cart.PaymentDetails;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import com.carlosoliveira.checkout.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.carlosoliveira.checkout.support.Fixtures.CREDIT_CARD;
import static com.carlosoliveira.checkout.support.Fixtures.FAMILIA;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PAYMENT_METHODS;
import static com.carlosoliveira.checkout.support.Fixtures.PREMIUM;
import static com.carlosoliveira.checkout.support.Fixtures.newCart;
import static org.assertj.core.api.Assertions.assertThat;

class CartPersistenceAdapterIT extends IntegrationTestSupport {

    @Autowired
    private CartPersistenceAdapter adapter;

    @Test
    @DisplayName("persists and restores a cart with items, payment methods and expiration")
    void roundTrip() {
        Cart cart = newCart(FAMILIA, PREMIUM);

        adapter.save(cart);
        Cart restored = adapter.findById(cart.getId()).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(CartStatus.CREATED);
        assertThat(restored.getItems()).isEqualTo(cart.getItems());
        assertThat(restored.getPaymentMethods()).isEqualTo(PAYMENT_METHODS);
        assertThat(restored.getCreatedAt()).isEqualTo(cart.getCreatedAt());
        assertThat(restored.getExpiresAt()).isEqualTo(cart.getExpiresAt());
        assertThat(restored.total()).isEqualByComparingTo("99.80");
        assertThat(restored.getPayment()).isEmpty();
    }

    @Test
    @DisplayName("persists the payment details of an approved cart")
    void persistsApprovedPayment() {
        Cart cart = newCart(PREMIUM);
        adapter.save(cart);
        cart.startProcessing();
        cart.approve(CREDIT_CARD, PaymentResult.approvedCard("tx-42", "4242", NOW));

        adapter.save(cart);
        Cart restored = adapter.findById(cart.getId()).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(CartStatus.APPROVED);
        PaymentDetails payment = restored.getPayment().orElseThrow();
        assertThat(payment.methodType()).isEqualTo(PaymentMethodType.CREDIT_CARD);
        assertThat(payment.transactionId()).isEqualTo("tx-42");
        assertThat(payment.lastDigits()).isEqualTo("4242");
        assertThat(payment.amount()).isEqualByComparingTo("39.90");
        assertThat(payment.paidAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("lets only one caller claim a payable cart for processing")
    void claimsOnlyOnce() {
        Cart cart = newCart(PREMIUM);
        adapter.save(cart);

        assertThat(adapter.claimForProcessing(cart.getId(), NOW)).isTrue();
        assertThat(adapter.claimForProcessing(cart.getId(), NOW)).isFalse();
        assertThat(adapter.findById(cart.getId()).orElseThrow().getStatus()).isEqualTo(CartStatus.PROCESSING);
    }

    @Test
    @DisplayName("allows claiming a declined cart again")
    void claimsDeclinedCart() {
        Cart cart = newCart(PREMIUM);
        adapter.save(cart);
        adapter.claimForProcessing(cart.getId(), NOW);
        cart.startProcessing();
        cart.decline();
        adapter.save(cart);

        assertThat(adapter.claimForProcessing(cart.getId(), NOW)).isTrue();
    }

    @Test
    @DisplayName("does not claim unknown carts")
    void doesNotClaimUnknownCart() {
        assertThat(adapter.claimForProcessing(UUID.randomUUID(), NOW)).isFalse();
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }
}
