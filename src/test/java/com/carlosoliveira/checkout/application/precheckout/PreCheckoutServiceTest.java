package com.carlosoliveira.checkout.application.precheckout;

import com.carlosoliveira.checkout.application.CheckoutProperties;
import com.carlosoliveira.checkout.application.port.out.CartRepositoryPort;
import com.carlosoliveira.checkout.application.port.out.PaymentMethodsPort;
import com.carlosoliveira.checkout.application.port.out.ProductCatalogPort;
import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartItem;
import com.carlosoliveira.checkout.domain.cart.CartStatus;
import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.carlosoliveira.checkout.support.Errors.checkoutFailure;
import static com.carlosoliveira.checkout.support.Errors.errorCodeOf;
import static com.carlosoliveira.checkout.support.Fixtures.CART_TTL;
import static com.carlosoliveira.checkout.support.Fixtures.CATALOG;
import static com.carlosoliveira.checkout.support.Fixtures.FIXED_CLOCK;
import static com.carlosoliveira.checkout.support.Fixtures.NOW;
import static com.carlosoliveira.checkout.support.Fixtures.PAYMENT_METHODS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PreCheckoutServiceTest {

    private ProductCatalogPort productCatalog;
    private PaymentMethodsPort paymentMethods;
    private CartRepositoryPort cartRepository;
    private PreCheckoutService service;

    @BeforeEach
    void setUp() {
        productCatalog = mock(ProductCatalogPort.class);
        paymentMethods = mock(PaymentMethodsPort.class);
        cartRepository = mock(CartRepositoryPort.class);
        service = new PreCheckoutService(
                productCatalog, paymentMethods, cartRepository, new CheckoutProperties(CART_TTL), FIXED_CLOCK);

        when(productCatalog.findAll()).thenReturn(CATALOG);
        when(paymentMethods.findAvailable()).thenReturn(PAYMENT_METHODS);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("creates and persists a cart with the selected products and available payment methods")
    void createsCart() {
        Cart cart = service.start(new PreCheckoutCommand(List.of("premium")));

        ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(cart);

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CREATED);
        assertThat(cart.getItems()).hasSize(1);
        CartItem item = cart.getItems().get(0);
        assertThat(item.slug()).isEqualTo("premium");
        assertThat(item.name()).isEqualTo("Plano Premium");
        assertThat(item.price()).isEqualByComparingTo("39.90");
        assertThat(cart.getPaymentMethods()).isEqualTo(PAYMENT_METHODS);
        assertThat(cart.getExpiresAt()).isEqualTo(NOW.plus(CART_TTL));
    }

    @Test
    @DisplayName("normalizes slugs and ignores duplicates while keeping the selection order")
    void normalizesAndDeduplicatesSlugs() {
        Cart cart = service.start(new PreCheckoutCommand(List.of(" Familia ", "premium", "FAMILIA")));

        assertThat(cart.getItems().stream().map(CartItem::slug).toList()).containsExactly("familia", "premium");
        assertThat(cart.total()).isEqualByComparingTo("99.80");
    }

    @Test
    @DisplayName("rejects unknown products listing every missing slug without creating a cart")
    void rejectsUnknownProducts() {
        CheckoutException failure = checkoutFailure(() ->
                service.start(new PreCheckoutCommand(List.of("premium", "ultra", "gold"))));

        assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        assertThat(failure.getCartId()).isNull();
        assertThat(failure.getMessage()).contains("ultra", "gold");
        verifyNoInteractions(paymentMethods, cartRepository);
    }

    @Test
    @DisplayName("fails as an upstream outage when no payment method is available")
    void failsWithoutPaymentMethods() {
        when(paymentMethods.findAvailable()).thenReturn(List.of());

        assertThat(errorCodeOf(() -> service.start(new PreCheckoutCommand(List.of("premium")))))
                .isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        verifyNoInteractions(cartRepository);
    }
}
