package com.carlosoliveira.checkout.application.checkout;

import com.carlosoliveira.checkout.domain.payment.CardData;

import java.util.UUID;

public record CheckoutCommand(UUID cartId, String paymentMethodName, CardData card) {
}
