package com.carlosoliveira.checkout.adapter.out.paymentmethod;

import com.carlosoliveira.checkout.domain.payment.PaymentMethod;
import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;

import java.util.Arrays;
import java.util.Optional;

record PaymentMethodResponse(String code, String name, boolean enabled) {

    Optional<PaymentMethod> toDomain() {
        return Arrays.stream(PaymentMethodType.values())
                .filter(type -> type.name().equalsIgnoreCase(code))
                .findFirst()
                .map(type -> new PaymentMethod(name, type));
    }
}
