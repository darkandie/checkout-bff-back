package com.carlosoliveira.checkout.support;

import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.catalog.Product;
import com.carlosoliveira.checkout.domain.payment.CardData;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class Fixtures {

    public static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    public static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final Duration CART_TTL = Duration.ofMinutes(30);
    public static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(2);

    public static final Product BASICO =
            new Product("basico", "Plano Básico", "Assinatura mensal com catálogo essencial", new BigDecimal("19.90"), "BRL");
    public static final Product PREMIUM =
            new Product("premium", "Plano Premium", "Assinatura mensal com catálogo completo", new BigDecimal("39.90"), "BRL");
    public static final Product FAMILIA =
            new Product("familia", "Plano Família", "Assinatura mensal para até 4 perfis", new BigDecimal("59.90"), "BRL");

    public static final PaymentMethod CREDIT_CARD = new PaymentMethod("Cartão de Crédito", PaymentMethodType.CREDIT_CARD);
    public static final PaymentMethod DEBIT_CARD = new PaymentMethod("Cartão de Débito", PaymentMethodType.DEBIT_CARD);
    public static final PaymentMethod PIX = new PaymentMethod("Pix", PaymentMethodType.PIX);

    public static final List<Product> CATALOG = List.of(BASICO, PREMIUM, FAMILIA);
    public static final List<PaymentMethod> PAYMENT_METHODS = List.of(CREDIT_CARD, DEBIT_CARD, PIX);

    private Fixtures() {
    }

    public static Cart newCart(Product... products) {
        return Cart.create(UUID.randomUUID(), List.of(products), PAYMENT_METHODS, NOW, CART_TTL);
    }

    public static Cart approvedCart(Product... products) {
        Cart cart = newCart(products);
        cart.startProcessing(NOW, PROCESSING_TIMEOUT);
        cart.approve(CREDIT_CARD, PaymentResult.approvedCard("tx-approved", "4242", NOW));
        return cart;
    }

    public static CardData approvedCard() {
        return new CardData("Ana Souza", "4111111111114242", "12/28", "123");
    }

    public static CardData declinedCard() {
        return new CardData("Ana Souza", "4000000000000002", "12/28", "123");
    }

    public static CardData expiredCard() {
        return new CardData("Ana Souza", "4111111111114242", "09/26", "123");
    }
}
