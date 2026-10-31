package com.carlosoliveira.checkout.adapter.out.catalog;

import com.carlosoliveira.checkout.domain.catalog.Product;

import java.math.BigDecimal;

record ProductResponse(
        String slug,
        String name,
        String description,
        BigDecimal price,
        String currency,
        boolean active
) {

    Product toDomain() {
        return new Product(slug, name, description, price, currency);
    }
}
