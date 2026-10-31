package com.carlosoliveira.checkout.adapter.out.persistence;

import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class CartPaymentMethodEmbeddable {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PaymentMethodType type;

    protected CartPaymentMethodEmbeddable() {
    }

    public CartPaymentMethodEmbeddable(String name, PaymentMethodType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public PaymentMethodType getType() {
        return type;
    }
}
