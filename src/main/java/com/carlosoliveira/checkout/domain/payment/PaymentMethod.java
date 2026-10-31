package com.carlosoliveira.checkout.domain.payment;

import java.io.Serializable;
import java.util.Objects;

public record PaymentMethod(String name, PaymentMethodType type) implements Serializable {

    public PaymentMethod {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    public boolean matches(String candidateName) {
        return candidateName != null && name.equalsIgnoreCase(candidateName.trim());
    }
}
