package com.carlosoliveira.checkout.adapter.in.web.error;

import com.carlosoliveira.checkout.domain.exception.CheckoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CheckoutException.class)
    ResponseEntity<FrontendErrorResponse> handleCheckout(CheckoutException ex) {
        ErrorCatalog error = ErrorCatalog.from(ex.getErrorCode());
        if (error.status().is5xxServerError()) {
            log.error("Checkout failure errorCode={} cartId={}", ex.getErrorCode(), ex.getCartId(), ex);
        } else {
            log.info("Checkout rejected errorCode={} cartId={} reason={}",
                    ex.getErrorCode(), ex.getCartId(), ex.getMessage());
        }
        return respond(error, ex.getCartId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<FrontendErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.info("Invalid request fields={}", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        UUID cartId = ex.getBindingResult().getTarget() instanceof CartScoped scoped ? scoped.cartId() : null;
        return respond(ErrorCatalog.INVALID_REQUEST, cartId);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class
    })
    ResponseEntity<FrontendErrorResponse> handleBadRequest(Exception ex) {
        log.info("Malformed request: {}", ex.getMessage());
        return respond(ErrorCatalog.INVALID_REQUEST, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<FrontendErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return respond(ErrorCatalog.RESOURCE_NOT_FOUND, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<FrontendErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(ErrorCatalog.METHOD_NOT_SUPPORTED, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<FrontendErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(ErrorCatalog.UNSUPPORTED_MEDIA_TYPE, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<FrontendErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorCatalog.UNEXPECTED_ERROR, null);
    }

    private static ResponseEntity<FrontendErrorResponse> respond(ErrorCatalog error, UUID cartId) {
        return ResponseEntity.status(error.status()).body(error.toResponse(cartId));
    }
}
