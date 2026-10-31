package com.carlosoliveira.checkout.domain.cart;

import com.carlosoliveira.checkout.domain.catalog.Product;

import java.math.BigDecimal;

public record CartItem(
        String slug,
        String name,
        String description,
        BigDecimal price,
        String currency
) {

    public static CartItem from(Product product) {
        return new CartItem(
                product.slug(),
                product.name(),
                product.description(),
                product.price(),
                product.currency()
        );
    }
}
