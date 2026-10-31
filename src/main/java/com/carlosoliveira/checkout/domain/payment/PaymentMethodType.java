package com.carlosoliveira.checkout.domain.payment;

public enum PaymentMethodType {

    CREDIT_CARD(true),
    DEBIT_CARD(true),
    PIX(false);

    private final boolean requiresCard;

    PaymentMethodType(boolean requiresCard) {
        this.requiresCard = requiresCard;
    }

    public boolean requiresCard() {
        return requiresCard;
    }
}
