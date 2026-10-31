package com.carlosoliveira.checkout.application.checkout;

import com.carlosoliveira.checkout.application.port.out.CartRepositoryPort;
import com.carlosoliveira.checkout.application.port.out.PaymentGatewayPort;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import com.carlosoliveira.checkout.domain.cart.PaymentDetails;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.carlosoliveira.checkout.domain.payment.PaymentRequest;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.carlosoliveira.checkout.support.Errors.checkoutFailure;
import static com.carlosoliveira.checkout.support.Errors.errorCodeOf;
import static com.carlosoliveira.checkout.support.Fixtures.CART_TTL;
import static com.carlosoliveira.checkout.support.Fixtures.FIXED_CLOCK;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PAYMENT_METHODS;
import static com.carlosoliveira.checkout.support.Fixtures.PREMIUM;
import static com.carlosoliveira.checkout.support.Fixtures.approvedCard;
import static com.carlosoliveira.checkout.support.Fixtures.approvedCart;
import static com.carlosoliveira.checkout.support.Fixtures.expiredCard;
import static com.carlosoliveira.checkout.support.Fixtures.newCart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutServiceTest {

    private CartRepositoryPort cartRepository;
    private PaymentGatewayPort paymentGateway;
    private CheckoutService service;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepositoryPort.class);
        paymentGateway = mock(PaymentGatewayPort.class);
        service = new CheckoutService(cartRepository, paymentGateway, FIXED_CLOCK);

        cart = newCart(PREMIUM);
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(cartRepository.claimForProcessing(eq(cart.getId()), any())).thenReturn(true);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("approves a card payment charging the cart total and storing only the last digits")
    void approvesCardPayment() {
        when(paymentGateway.charge(any())).thenReturn(PaymentResult.approvedCard("tx-1", "4242", NOW));

        Cart result = service.checkout(new CheckoutCommand(cart.getId(), "Cartão de Crédito", approvedCard()));

        ArgumentCaptor<PaymentRequest> charged = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentGateway).charge(charged.capture());
        assertThat(charged.getValue().cartId()).isEqualTo(cart.getId());
        assertThat(charged.getValue().amount()).isEqualByComparingTo("39.90");
        assertThat(charged.getValue().currency()).isEqualTo("BRL");
        assertThat(charged.getValue().method().type()).isEqualTo(PaymentMethodType.CREDIT_CARD);

        assertThat(result.getStatus()).isEqualTo(CartStatus.APPROVED);
        PaymentDetails payment = result.getPayment().orElseThrow();
        assertThat(payment.lastDigits()).isEqualTo("4242");
        assertThat(payment.transactionId()).isEqualTo("tx-1");
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("approves a Pix payment without card data")
    void approvesPixPayment() {
        when(paymentGateway.charge(any())).thenReturn(PaymentResult.approvedPix("tx-2", "000201PIX", NOW));

        Cart result = service.checkout(new CheckoutCommand(cart.getId(), "Pix", null));

        PaymentDetails payment = result.getPayment().orElseThrow();
        assertThat(result.getStatus()).isEqualTo(CartStatus.APPROVED);
        assertThat(payment.pixCode()).isEqualTo("000201PIX");
        assertThat(payment.lastDigits()).isNull();
    }

    @Test
    @DisplayName("marks the cart as declined and reports PAYMENT_DECLINED")
    void declinesPayment() {
        when(paymentGateway.charge(any())).thenReturn(PaymentResult.declined("tx-3", NOW));

        CheckoutException failure = checkoutFailure(() ->
                service.checkout(new CheckoutCommand(cart.getId(), "Cartão de Crédito", approvedCard())));

        assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_DECLINED);
        assertThat(failure.getCartId()).isEqualTo(cart.getId());
        assertThat(cart.getStatus()).isEqualTo(CartStatus.DECLINED);
        assertThat(cart.getPayment()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("rejects an unknown cart")
    void rejectsUnknownCart() {
        UUID unknown = UUID.randomUUID();
        when(cartRepository.findById(unknown)).thenReturn(Optional.empty());

        CheckoutException failure = checkoutFailure(() ->
                service.checkout(new CheckoutCommand(unknown, "Pix", null)));

        assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND);
        assertThat(failure.getCartId()).isEqualTo(unknown);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("rejects a cart that was already paid without charging it again")
    void rejectsApprovedCart() {
        Cart paid = approvedCart(PREMIUM);
        when(cartRepository.findById(paid.getId())).thenReturn(Optional.of(paid));

        assertThat(errorCodeOf(() -> service.checkout(new CheckoutCommand(paid.getId(), "Pix", null))))
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_APPROVED);
        verifyNoInteractions(paymentGateway);
        verify(cartRepository, never()).claimForProcessing(any(), any());
    }

    @Test
    @DisplayName("rejects an expired cart")
    void rejectsExpiredCart() {
        CheckoutService later = new CheckoutService(
                cartRepository, paymentGateway, Clock.fixed(NOW.plus(CART_TTL), ZoneOffset.UTC));

        assertThat(errorCodeOf(() -> later.checkout(new CheckoutCommand(cart.getId(), "Pix", null))))
                .isEqualTo(ErrorCode.CART_EXPIRED);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("rejects a payment method not offered for the cart")
    void rejectsPaymentMethodNotOffered() {
        assertThat(errorCodeOf(() -> service.checkout(new CheckoutCommand(cart.getId(), "Boleto", null))))
                .isEqualTo(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("rejects card data sent for Pix")
    void rejectsCardForPix() {
        assertThat(errorCodeOf(() -> service.checkout(new CheckoutCommand(cart.getId(), "Pix", approvedCard()))))
                .isEqualTo(ErrorCode.INVALID_PAYMENT_DATA);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("requires card data for card payment methods")
    void requiresCardForCardMethods() {
        assertThat(errorCodeOf(() -> service.checkout(new CheckoutCommand(cart.getId(), "Cartão de Débito", null))))
                .isEqualTo(ErrorCode.INVALID_PAYMENT_DATA);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("rejects an expired card before reaching the gateway")
    void rejectsExpiredCard() {
        assertThat(errorCodeOf(() ->
                service.checkout(new CheckoutCommand(cart.getId(), "Cartão de Crédito", expiredCard()))))
                .isEqualTo(ErrorCode.INVALID_PAYMENT_DATA);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("reports PAYMENT_ALREADY_APPROVED when a concurrent request paid the cart first")
    void concurrentRequestApprovedFirst() {
        Cart paidMeanwhile = Cart.restore(cart.getId(), cart.getItems(), PAYMENT_METHODS,
                CartStatus.APPROVED, cart.getCreatedAt(), cart.getExpiresAt(), null);
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart), Optional.of(paidMeanwhile));
        when(cartRepository.claimForProcessing(eq(cart.getId()), any())).thenReturn(false);

        assertThat(errorCodeOf(() -> service.checkout(new CheckoutCommand(cart.getId(), "Pix", null))))
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_APPROVED);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("reports PAYMENT_IN_PROGRESS when a concurrent request is still processing")
    void concurrentRequestStillProcessing() {
        Cart processing = Cart.restore(cart.getId(), cart.getItems(), PAYMENT_METHODS,
                CartStatus.PROCESSING, cart.getCreatedAt(), cart.getExpiresAt(), null);
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart), Optional.of(processing));
        when(cartRepository.claimForProcessing(eq(cart.getId()), any())).thenReturn(false);

        assertThat(errorCodeOf(() -> service.checkout(new CheckoutCommand(cart.getId(), "Pix", null))))
                .isEqualTo(ErrorCode.PAYMENT_IN_PROGRESS);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("restores the previous status when the gateway fails unexpectedly")
    void restoresStatusOnGatewayFailure() {
        IllegalStateException gatewayFailure = new IllegalStateException("gateway down");
        when(paymentGateway.charge(any())).thenThrow(gatewayFailure);

        Throwable thrown = catchThrowable(() ->
                service.checkout(new CheckoutCommand(cart.getId(), "Cartão de Crédito", approvedCard())));

        assertThat(thrown).isEqualTo(gatewayFailure);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CREATED);
        verify(cartRepository, times(1)).save(cart);
    }
}
