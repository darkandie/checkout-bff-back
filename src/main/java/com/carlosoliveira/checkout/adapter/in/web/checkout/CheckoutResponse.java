package com.carlosoliveira.checkout.adapter.in.web.checkout;

import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartItem;
import com.carlosoliveira.checkout.domain.cart.PaymentDetails;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(
        UUID cartId,
        String status,
        Instant paidAt,
        PaymentView payment,
        List<ProductView> products
) {

    private static final String APPROVED_STATUS = "APROVADO";

    public static CheckoutResponse from(Cart cart) {
        PaymentDetails payment = cart.getPayment()
                .orElseThrow(() -> new IllegalStateException("approved cart " + cart.getId() + " has no payment"));
        return new CheckoutResponse(
                cart.getId(),
                APPROVED_STATUS,
                payment.paidAt(),
                PaymentView.from(payment),
                cart.getItems().stream().map(ProductView::from).toList()
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentView(
            String method,
            String lastDigits,
            String pixCode,
            BigDecimal amount,
            String currency
    ) {

        static PaymentView from(PaymentDetails payment) {
            return new PaymentView(
                    payment.methodName(),
                    payment.lastDigits(),
                    payment.pixCode(),
                    payment.amount(),
                    payment.currency()
            );
        }
    }

    public record ProductView(String slug, String name, BigDecimal price) {

        static ProductView from(CartItem item) {
            return new ProductView(item.slug(), item.name(), item.price());
        }
    }
}
