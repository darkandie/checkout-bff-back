package com.carlosoliveira.checkout.domain.cart;

import com.carlosoliveira.checkout.domain.payment.PaymentMethodType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDetails(
        String methodName,
        PaymentMethodType methodType,
        String transactionId,
        String lastDigits,
        String pixCode,
        BigDecimal amount,
        String currency,
        Instant paidAt
) {
}
