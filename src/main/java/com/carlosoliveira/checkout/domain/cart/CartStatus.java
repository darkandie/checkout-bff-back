package com.carlosoliveira.checkout.domain.cart;

import java.util.EnumSet;
import java.util.Set;

public enum CartStatus {
    CREATED,
    PROCESSING,
    APPROVED,
    DECLINED;

    private static final Set<CartStatus> PAYABLE = EnumSet.of(CREATED, DECLINED);

    public static Set<CartStatus> payable() {
        return PAYABLE;
    }

    public boolean isPayable() {
        return PAYABLE.contains(this);
    }
}
