package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartItem;
import com.carlosoliveira.checkout.domain.cart.PaymentDetails;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;

import java.time.Instant;

final class CartJpaMapper {

    private CartJpaMapper() {
    }

    static CartJpaEntity toEntity(Cart cart, Instant now) {
        return new CartJpaEntity(
                cart.getId(),
                cart.getStatus(),
                cart.currency(),
                cart.total(),
                cart.getCreatedAt(),
                cart.getExpiresAt(),
                now,
                cart.getItems().stream()
                        .map(item -> new CartItemEmbeddable(
                                item.slug(), item.name(), item.description(), item.price(), item.currency()))
                        .toList(),
                cart.getPaymentMethods().stream()
                        .map(method -> new CartPaymentMethodEmbeddable(method.name(), method.type()))
                        .toList(),
                cart.getPayment().map(CartJpaMapper::toEmbeddable).orElse(null)
        );
    }

    static Cart toDomain(CartJpaEntity entity) {
        return Cart.restore(
                entity.getId(),
                entity.getItems().stream()
                        .map(item -> new CartItem(
                                item.getSlug(), item.getName(), item.getDescription(), item.getPrice(), item.getCurrency()))
                        .toList(),
                entity.getPaymentMethods().stream()
                        .map(method -> new PaymentMethod(method.getName(), method.getType()))
                        .toList(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                toDomain(entity.getPayment())
        );
    }

    private static PaymentEmbeddable toEmbeddable(PaymentDetails payment) {
        return new PaymentEmbeddable(
                payment.methodName(),
                payment.methodType(),
                payment.transactionId(),
                payment.lastDigits(),
                payment.pixCode(),
                payment.amount(),
                payment.currency(),
                payment.paidAt()
        );
    }

    private static PaymentDetails toDomain(PaymentEmbeddable payment) {
        if (payment == null || payment.getMethodName() == null) {
            return null;
        }
        return new PaymentDetails(
                payment.getMethodName(),
                payment.getMethodType(),
                payment.getTransactionId(),
                payment.getLastDigits(),
                payment.getPixCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt()
        );
    }
}
