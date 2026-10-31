package com.carlosoliveira.checkout.domain.catalog;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Product(
        String slug,
        String name,
        String description,
        BigDecimal price,
        String currency
) implements Serializable {

    public Product {
        Objects.requireNonNull(slug, "slug must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        price = price.setScale(2, RoundingMode.HALF_EVEN);
    }
}
