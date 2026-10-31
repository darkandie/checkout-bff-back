package com.carlosoliveira.checkout.domain.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProductTest {

    @Test
    @DisplayName("normalizes price to two decimal places")
    void normalizesPrice() {
        Product product = new Product("premium", "Plano Premium", "desc", new BigDecimal("39.9"), "BRL");

        assertThat(product.price()).isEqualTo(new BigDecimal("39.90"));
    }

    @Test
    @DisplayName("rejects negative prices")
    void rejectsNegativePrice() {
        Throwable thrown = catchThrowable(() ->
                new Product("premium", "Plano Premium", "desc", new BigDecimal("-1.00"), "BRL"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("requires slug, name, price and currency")
    void requiresMandatoryFields() {
        assertThat(catchThrowable(() -> new Product(null, "n", "d", BigDecimal.ONE, "BRL")))
                .isInstanceOf(NullPointerException.class);
        assertThat(catchThrowable(() -> new Product("s", null, "d", BigDecimal.ONE, "BRL")))
                .isInstanceOf(NullPointerException.class);
        assertThat(catchThrowable(() -> new Product("s", "n", "d", null, "BRL")))
                .isInstanceOf(NullPointerException.class);
        assertThat(catchThrowable(() -> new Product("s", "n", "d", BigDecimal.ONE, null)))
                .isInstanceOf(NullPointerException.class);
    }
}
