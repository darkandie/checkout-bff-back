package com.carlosoliveira.checkout.support;

import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import com.carlosoliveira.checkout.domain.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class Errors {

    private Errors() {
    }

    public static CheckoutException checkoutFailure(ThrowingCallable call) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(CheckoutException.class);
        return (CheckoutException) thrown;
    }

    public static ErrorCode errorCodeOf(ThrowingCallable call) {
        return checkoutFailure(call).getErrorCode();
    }
}
