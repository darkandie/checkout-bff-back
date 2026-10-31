package com.carlosoliveira.checkout.domain.exception;

import java.util.Collection;
import java.util.UUID;

public class CheckoutException extends RuntimeException {

    private final ErrorCode errorCode;
    private final UUID cartId;

    public CheckoutException(ErrorCode errorCode, UUID cartId, String message) {
        this(errorCode, cartId, message, null);
    }

    protected CheckoutException(ErrorCode errorCode, UUID cartId, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.cartId = cartId;
    }

    public static CheckoutException productNotFound(Collection<String> slugs) {
        return new CheckoutException(ErrorCode.PRODUCT_NOT_FOUND, null, "Products not found: " + slugs);
    }

    public static CheckoutException cartNotFound(UUID cartId) {
        return new CheckoutException(ErrorCode.CART_NOT_FOUND, cartId, "Cart not found");
    }

    public static CheckoutException cartExpired(UUID cartId) {
        return new CheckoutException(ErrorCode.CART_EXPIRED, cartId, "Cart expired");
    }

    public static CheckoutException paymentMethodNotAllowed(UUID cartId, String methodName) {
        return new CheckoutException(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED, cartId,
                "Payment method not allowed for cart: " + methodName);
    }

    public static CheckoutException invalidPaymentData(UUID cartId, String reason) {
        return new CheckoutException(ErrorCode.INVALID_PAYMENT_DATA, cartId, reason);
    }

    public static CheckoutException paymentDeclined(UUID cartId) {
        return new CheckoutException(ErrorCode.PAYMENT_DECLINED, cartId, "Payment declined");
    }

    public static CheckoutException alreadyApproved(UUID cartId) {
        return new CheckoutException(ErrorCode.PAYMENT_ALREADY_APPROVED, cartId, "Cart already paid");
    }

    public static CheckoutException paymentInProgress(UUID cartId) {
        return new CheckoutException(ErrorCode.PAYMENT_IN_PROGRESS, cartId, "Payment already in progress");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public UUID getCartId() {
        return cartId;
    }
}
