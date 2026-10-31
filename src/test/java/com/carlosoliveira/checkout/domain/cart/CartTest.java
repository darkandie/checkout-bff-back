package com.carlosoliveira.checkout.domain.cart;

import com.carlosoliveira.checkout.domain.catalog.Product;
import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.carlosoliveira.checkout.support.Errors.errorCodeOf;
import static com.carlosoliveira.checkout.support.Fixtures.CART_TTL;
import static com.carlosoliveira.checkout.support.Fixtures.CREDIT_CARD;
import static com.carlosoliveira.checkout.support.Fixtures.FAMILIA;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PAYMENT_METHODS;
import static com.carlosoliveira.checkout.support.Fixtures.PIX;
import static com.carlosoliveira.checkout.support.Fixtures.PREMIUM;
import static com.carlosoliveira.checkout.support.Fixtures.approvedCart;
import static com.carlosoliveira.checkout.support.Fixtures.newCart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CartTest {

    @Test
    @DisplayName("creates a cart snapshotting products, payment methods and expiration")
    void createsCart() {
        Cart cart = newCart(PREMIUM, FAMILIA);

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CREATED);
        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.getItems().get(0).slug()).isEqualTo("premium");
        assertThat(cart.getPaymentMethods()).isEqualTo(PAYMENT_METHODS);
        assertThat(cart.getCreatedAt()).isEqualTo(NOW);
        assertThat(cart.getExpiresAt()).isEqualTo(NOW.plus(CART_TTL));
        assertThat(cart.getPayment()).isEmpty();
    }

    @Test
    @DisplayName("sums item prices into the cart total")
    void sumsTotal() {
        Cart cart = newCart(PREMIUM, FAMILIA);

        assertThat(cart.total()).isEqualByComparingTo("99.80");
        assertThat(cart.currency()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("rejects a cart without items")
    void rejectsEmptyCart() {
        Throwable thrown = catchThrowable(() -> Cart.create(UUID.randomUUID(), List.of(), PAYMENT_METHODS, NOW, CART_TTL));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects items with different currencies")
    void rejectsMixedCurrencies() {
        Product dollarPlan = new Product("global", "Plano Global", "Plano em dólar", new BigDecimal("9.90"), "USD");

        Throwable thrown = catchThrowable(() ->
                Cart.create(UUID.randomUUID(), List.of(PREMIUM, dollarPlan), PAYMENT_METHODS, NOW, CART_TTL));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resolves an offered payment method ignoring case and surrounding spaces")
    void resolvesPaymentMethod() {
        Cart cart = newCart(PREMIUM);

        assertThat(cart.resolvePaymentMethod("  pix ")).isEqualTo(PIX);
        assertThat(cart.resolvePaymentMethod("cartão de crédito")).isEqualTo(CREDIT_CARD);
    }

    @Test
    @DisplayName("rejects a payment method that was not offered for the cart")
    void rejectsUnknownPaymentMethod() {
        Cart cart = newCart(PREMIUM);

        assertThat(errorCodeOf(() -> cart.resolvePaymentMethod("Boleto")))
                .isEqualTo(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("is payable while created and not expired")
    void payableWhenCreated() {
        Cart cart = newCart(PREMIUM);

        cart.ensurePayable(NOW.plus(CART_TTL).minusSeconds(1));

        assertThat(cart.getStatus().isPayable()).isTrue();
    }

    @Test
    @DisplayName("is not payable once expired")
    void notPayableWhenExpired() {
        Cart cart = newCart(PREMIUM);

        assertThat(errorCodeOf(() -> cart.ensurePayable(NOW.plus(CART_TTL))))
                .isEqualTo(ErrorCode.CART_EXPIRED);
    }

    @Test
    @DisplayName("is not payable once approved")
    void notPayableWhenApproved() {
        Cart cart = approvedCart(PREMIUM);

        assertThat(errorCodeOf(() -> cart.ensurePayable(NOW)))
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_APPROVED);
    }

    @Test
    @DisplayName("is not payable while another payment is processing")
    void notPayableWhenProcessing() {
        Cart cart = newCart(PREMIUM);
        cart.startProcessing();

        assertThat(errorCodeOf(() -> cart.ensurePayable(NOW)))
                .isEqualTo(ErrorCode.PAYMENT_IN_PROGRESS);
    }

    @Test
    @DisplayName("records payment details when approved")
    void approves() {
        Cart cart = newCart(PREMIUM);
        cart.startProcessing();

        cart.approve(CREDIT_CARD, PaymentResult.approvedCard("tx-1", "4242", NOW));

        assertThat(cart.getStatus()).isEqualTo(CartStatus.APPROVED);
        PaymentDetails payment = cart.getPayment().orElseThrow();
        assertThat(payment.methodName()).isEqualTo("Cartão de Crédito");
        assertThat(payment.methodType()).isEqualTo(PaymentMethodType.CREDIT_CARD);
        assertThat(payment.lastDigits()).isEqualTo("4242");
        assertThat(payment.pixCode()).isNull();
        assertThat(payment.amount()).isEqualByComparingTo("39.90");
        assertThat(payment.currency()).isEqualTo("BRL");
        assertThat(payment.paidAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("allows a new attempt after a declined payment")
    void retriesAfterDecline() {
        Cart cart = newCart(PREMIUM);
        cart.startProcessing();
        cart.decline();

        assertThat(cart.getStatus()).isEqualTo(CartStatus.DECLINED);
        cart.ensurePayable(NOW);
        assertThat(cart.startProcessing()).isEqualTo(CartStatus.DECLINED);
    }

    @Test
    @DisplayName("restores the previous status when processing is aborted")
    void abortsProcessing() {
        Cart cart = newCart(PREMIUM);
        CartStatus previous = cart.startProcessing();

        cart.abortProcessing(previous);

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CREATED);
    }

    @Test
    @DisplayName("refuses invalid status transitions")
    void refusesInvalidTransitions() {
        Cart approved = approvedCart(PREMIUM);
        Cart created = newCart(PREMIUM);

        assertThat(catchThrowable(approved::startProcessing)).isInstanceOf(IllegalStateException.class);
        assertThat(catchThrowable(created::decline)).isInstanceOf(IllegalStateException.class);
        assertThat(catchThrowable(() -> created.approve(PIX, PaymentResult.approvedPix("tx", "000201", NOW))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("refuses to approve with a declined payment result")
    void refusesDeclinedResultOnApprove() {
        Cart cart = newCart(PREMIUM);
        cart.startProcessing();

        Throwable thrown = catchThrowable(() -> cart.approve(CREDIT_CARD, PaymentResult.declined("tx", NOW)));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
