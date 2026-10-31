package com.carlosoliveira.checkout.domain.payment;

import java.time.Instant;

public record PaymentResult(
        PaymentStatus status,
        String transactionId,
        String lastDigits,
        String pixCode,
        Instant processedAt
) {

    public static PaymentResult approvedCard(String transactionId, String lastDigits, Instant processedAt) {
        return new PaymentResult(PaymentStatus.APPROVED, transactionId, lastDigits, null, processedAt);
    }

    public static PaymentResult approvedPix(String transactionId, String pixCode, Instant processedAt) {
        return new PaymentResult(PaymentStatus.APPROVED, transactionId, null, pixCode, processedAt);
    }

    public static PaymentResult declined(String transactionId, Instant processedAt) {
        return new PaymentResult(PaymentStatus.DECLINED, transactionId, null, null, processedAt);
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }
}
