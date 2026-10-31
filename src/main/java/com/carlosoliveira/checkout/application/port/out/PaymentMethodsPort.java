package com.carlosoliveira.checkout.application.port.out;

import com.carlosoliveira.checkout.domain.payment.PaymentMethod;

import java.util.List;

public interface PaymentMethodsPort {

    List<PaymentMethod> findAvailable();
}
