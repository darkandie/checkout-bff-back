package com.carlosoliveira.checkout.domain.exception;

public class ExternalServiceException extends CheckoutException {

    private final String service;

    public ExternalServiceException(String service, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, null, "External service unavailable: " + service, cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
