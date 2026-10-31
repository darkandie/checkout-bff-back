package com.carlosoliveira.checkout.application.port.out;

import com.carlosoliveira.checkout.domain.payment.PaymentRequest;
import com.carlosoliveira.checkout.domain.payment.PaymentResult;

public interface PaymentGatewayPort {

    PaymentResult charge(PaymentRequest request);
}
