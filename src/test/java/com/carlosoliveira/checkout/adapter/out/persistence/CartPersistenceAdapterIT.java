package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import com.carlosoliveira.checkout.domain.cart.PaymentAttempt;
import com.carlosoliveira.checkout.domain.cart.PaymentDetails;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import com.carlosoliveira.checkout.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static com.carlosoliveira.checkout.support.Fixtures.CREDIT_CARD;
import static com.carlosoliveira.checkout.support.Fixtures.FAMILIA;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PAYMENT_METHODS;
import static com.carlosoliveira.checkout.support.Fixtures.PREMIUM;
import static com.carlosoliveira.checkout.support.Fixtures.PROCESSING_TIMEOUT;
import static com.carlosoliveira.checkout.support.Fixtures.newCart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
        cart.startProcessing(NOW, PROCESSING_TIMEOUT);
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
        Cart first = adapter.findById(cart.getId()).orElseThrow();
        Cart second = adapter.findById(cart.getId()).orElseThrow();
        first.startProcessing(NOW, PROCESSING_TIMEOUT);
        second.startProcessing(NOW, PROCESSING_TIMEOUT);

        assertThat(adapter.claimForProcessing(first, NOW.minus(PROCESSING_TIMEOUT))).isTrue();
        assertThat(adapter.claimForProcessing(second, NOW.minus(PROCESSING_TIMEOUT))).isFalse();

        Cart claimed = adapter.findById(cart.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(CartStatus.PROCESSING);
        assertThat(claimed.getAttempt()).contains(first.getAttempt().orElseThrow());
    }

    @Test
    @DisplayName("lets a caller recover a cart stuck in processing after the timeout")
    void recoversStaleClaim() {
        Cart cart = newCart(PREMIUM);
        adapter.save(cart);
        cart.startProcessing(NOW, PROCESSING_TIMEOUT);
        adapter.claimForProcessing(cart, NOW.minus(PROCESSING_TIMEOUT));
        Instant later = NOW.plus(PROCESSING_TIMEOUT).plusSeconds(1);

        Cart stuck = adapter.findById(cart.getId()).orElseThrow();
        Cart concurrent = adapter.findById(cart.getId()).orElseThrow();
        stuck.startProcessing(later, PROCESSING_TIMEOUT);
        concurrent.startProcessing(later, PROCESSING_TIMEOUT);

        assertThat(adapter.claimForProcessing(stuck, later.minus(PROCESSING_TIMEOUT))).isTrue();
        assertThat(adapter.claimForProcessing(concurrent, later.minus(PROCESSING_TIMEOUT))).isFalse();
        PaymentAttempt attempt = adapter.findById(cart.getId()).orElseThrow().getAttempt().orElseThrow();
        assertThat(attempt.id()).isEqualTo(cart.getAttempt().orElseThrow().id());
        assertThat(attempt.startedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("does not let a caller take over a cart that is still within the processing timeout")
    void keepsFreshClaim() {
        Cart cart = newCart(PREMIUM);
        adapter.save(cart);
        cart.startProcessing(NOW, PROCESSING_TIMEOUT);
        adapter.claimForProcessing(cart, NOW.minus(PROCESSING_TIMEOUT));
        Instant soon = NOW.plusSeconds(30);

        Cart other = adapter.findById(cart.getId()).orElseThrow();
        Throwable thrown = catchThrowable(() -> other.startProcessing(soon, PROCESSING_TIMEOUT));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(adapter.claimForProcessing(cart, soon.minus(PROCESSING_TIMEOUT))).isFalse();
    }

    @Test
    @DisplayName("allows claiming a declined cart again")
    void claimsDeclinedCart() {
        Cart cart = newCart(PREMIUM);
        adapter.save(cart);
        cart.startProcessing(NOW, PROCESSING_TIMEOUT);
        adapter.claimForProcessing(cart, NOW.minus(PROCESSING_TIMEOUT));
        cart.decline();
        adapter.save(cart);

        Cart retry = adapter.findById(cart.getId()).orElseThrow();
        retry.startProcessing(NOW.plusSeconds(5), PROCESSING_TIMEOUT);

        assertThat(adapter.claimForProcessing(retry, NOW.plusSeconds(5).minus(PROCESSING_TIMEOUT))).isTrue();
    }

    @Test
    @DisplayName("does not claim unknown carts")
    void doesNotClaimUnknownCart() {
        Cart neverSaved = newCart(PREMIUM);
        neverSaved.startProcessing(NOW, PROCESSING_TIMEOUT);

        assertThat(adapter.claimForProcessing(neverSaved, NOW.minus(PROCESSING_TIMEOUT))).isFalse();
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }
}
