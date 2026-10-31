package com.carlosoliveira.checkout.adapter.in.web.precheckout;

import com.carlosoliveira.checkout.domain.cart.Cart;
import com.carlosoliveira.checkout.domain.cart.CartItem;
import com.carlosoliveira.checkout.domain.payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PreCheckoutResponse(
        UUID cartId,
        List<ProductView> products,
        List<PaymentMethodView> paymentMethods
) {

    public static PreCheckoutResponse from(Cart cart) {
        return new PreCheckoutResponse(
                cart.getId(),
                cart.getItems().stream().map(ProductView::from).toList(),
                cart.getPaymentMethods().stream().map(PaymentMethodView::from).toList()
        );
    }

    public record ProductView(String slug, String name, String description, BigDecimal price, String currency) {

        static ProductView from(CartItem item) {
            return new ProductView(item.slug(), item.name(), item.description(), item.price(), item.currency());
        }
    }

    public record PaymentMethodView(String name) {

        static PaymentMethodView from(PaymentMethod method) {
            return new PaymentMethodView(method.name());
        }
    }
}
